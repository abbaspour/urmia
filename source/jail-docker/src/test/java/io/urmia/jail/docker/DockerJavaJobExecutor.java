package io.urmia.jail.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientImpl;
import io.urmia.job.XargsJobExecutor;
import io.urmia.md.model.job.JobDefinition;

public class DockerJavaJobExecutor extends XargsJobExecutor {

    public static final String DOCKER_SCRIPT_PATH = "/opt/urmia/bin/job-runner.sh";
    public static final String DOCKER_MOUNT_POINT = "/urmia/";

    public DockerJavaJobExecutor(String hostId, JobDefinition jobDef, String jobId) {
        super(hostId, jobDef, jobId, DOCKER_MOUNT_POINT, JobDefinition.Phase.Type.MAP);

        DockerClient client = new DockerClientImpl("http://localhost:2375");

        String command = String.format("/opt/urmia/bin/job-runner.sh -j %s -c '%s'", jobId, jobDef.getPhases().get(0).exec.get(0).getCommand());
        System.err.println("command: " + command);

        String[] commands = new String[]{command};

        Volume volJobs = new Volume("/urmia/jobs");
        Volume volStor = new Volume("/urmia/stor");

        Bind bindJobs = new Bind("/var/tmp/urmia/tck/ods/1/tck/jobs", volJobs);
        Bind bindStor = new Bind("/var/tmp/urmia/tck/ods/1/tck/stor", volStor);

        CreateContainerResponse container = client
                .createContainerCmd("urmia/debian")
                .withCmd(commands)
                .exec();

        client.startContainerCmd(container.getId()).withBinds(bindJobs, bindStor);

        int exitCode = client.waitContainerCmd(container.getId()).exec();

        System.out.println("info = " + client.infoCmd().exec());
    }

    @Override
    protected String getScriptPath(String s) {
        return DOCKER_SCRIPT_PATH;
    }
}
