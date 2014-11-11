package io.urmia.jail.docker;

/**
 *
 * Copyright 2014 by Amin Abbaspour
 *
 * This file is part of Urmia.io
 *
 * Urmia.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Urmia.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Urmia.io.  If not, see <http://www.gnu.org/licenses/>.
 */

import io.urmia.job.JobExecutor;
import io.urmia.job.JobExecutorFactory;
import io.urmia.md.model.job.JobDefinition;
import io.urmia.md.model.job.JobExec;
import io.urmia.md.model.job.JobInput;
import io.urmia.md.model.storage.ObjectName;
import io.urmia.util.UnixPathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import static io.urmia.job.JobExecutor.Status.*;
import static io.urmia.md.model.job.JobInput.END;

public class DockerCommandJobExecutor implements JobExecutor {

    private static final Logger log = LoggerFactory.getLogger(DockerCommandJobExecutor.class);

    private final static String[] envp = new String[]{
            "PATH=/bin:/usr/bin:/usr/local/bin",
            "DOCKER_HOST=tcp://192.168.59.103:2376",
            "DOCKER_TLS_VERIFY=0"
    };

    private Status status = INIT;
    private Process process = null;

    private final String[] commands;

    private final String hostId;
    private final String directory;
    private final String inputDirectory;
    private final String dockerStorMountPoint = "/urmia";
    private final JobDefinition.Phase.Type type;

    private RandomAccessFile jobInput = null;
    private final boolean boot2docker;

    public DockerCommandJobExecutor(String hostId, JobDefinition jobDef, String jobId, String mountPoint,
                                    JobDefinition.Phase.Type type) {

        this.directory = UnixPathUtils.appendTrailingSlash(mountPoint) + jobDef.getOwner() + '/' +  ObjectName.Namespace.JOBS.path + '/' + jobId;
        this.inputDirectory = directory + "/input";
        this.hostId = hostId;
        this.type = type;

        List<String> commands = new LinkedList<String>();

        String dockerBin = "/usr/bin/docker";

        if("Mac OS X".equalsIgnoreCase(System.getProperty("os.name"))) {
            dockerBin = "/usr/local/bin/docker";
            boot2docker = true;
        } else {
            boot2docker = false;
        }

        commands.add(dockerBin);
        commands.add("--debug=true");
        commands.add("run");
        commands.add("-v");
        commands.add(getAbsoluteScriptFolderPath() + ":/opt/urmia/bin:ro");
        commands.add("-v");
        commands.add(UnixPathUtils.trimSlash(mountPoint) + "/" + jobDef.getOwner() + ":/urmia/" + jobDef.getOwner());
        //commands.add("urmia/debian");
        commands.add("debian:7.6"); // <-- todo: read this from jobDef
        commands.add("/opt/urmia/bin/job-runner.sh");
        commands.add("-p");
        commands.add(dockerStorMountPoint + "/" + jobDef.getOwner());
        commands.add("-i");
        commands.add(hostId);
        commands.add("-j");
        commands.add(jobId);

        log.info("number of phases in job {} is: {}", jobId, jobDef.getPhases().size());

        for (JobDefinition.Phase p : jobDef.getPhases()) {
            log.info("number of exec in phase {} is: {}", p, p.exec.size());
            for (JobExec exec : p.exec) {
                commands.add("-c");
                commands.add(exec.getCommand());
            }
        }

        this.commands = commands.toArray(new String[commands.size()]);

        boolean mkDirResult = new File(directory).mkdirs();
        log.info("mkdirs to {} was: {}", directory, mkDirResult);
    }

    private static final String RUNNER_BASE = "src/main/bash";
    private static final String MODULE_NAME = "jail-docker";

    protected static String getAbsoluteScriptFolderPath() {
        String pwd = System.getProperty("user.dir");


        String scriptPath = pwd + File.separatorChar + RUNNER_BASE;
        File f = new File(scriptPath);
        if (f.exists() && f.isDirectory())
            return f.getAbsolutePath();

        String scriptPath2 = pwd + File.separatorChar + MODULE_NAME + File.separatorChar + RUNNER_BASE;
        File f2 = new File(scriptPath2);
        if (f2.exists() && f2.isDirectory())
            return f2.getAbsolutePath();

        String scriptPath21 = pwd +  "/source/" + MODULE_NAME + File.separatorChar + RUNNER_BASE;
        File f21 = new File(scriptPath21);
        if (f21.exists() && f21.isDirectory())
            return f21.getAbsolutePath();

        String scriptPath3 = pwd + File.separatorChar + ".." + File.separatorChar + MODULE_NAME + File.separatorChar + RUNNER_BASE;
        File f3 = new File(scriptPath3);
        if (f3.exists() && f3.isDirectory())
            return f3.getAbsolutePath();

        String scriptPath4 = pwd + File.separatorChar + "../.." + File.separatorChar + MODULE_NAME + File.separatorChar + RUNNER_BASE;
        File f4 = new File(scriptPath4);
        if (f4.exists() && f4.isDirectory())
            return f4.getAbsolutePath();

        throw new RuntimeException("script folder not found in:\n" + scriptPath + "\n" + scriptPath2 + "\n" + scriptPath3 + "\n" + scriptPath4);
    }

    @Override
    public void addInput(JobInput input) throws IOException {
        if (jobInput == null)
            throw new RuntimeException("stream not available yet");

        if (status != SUCCESSFUL)
            throw new IllegalArgumentException("job not in successful mode: " + status);

        if (input.isEod()) {
            log.info("closing input");
            jobInput.close();
            jobInput = null;

            //File f = new File(inputDirectory);
            //f.delete();

            return;
        }

        for (String on : input) {
            String line = dockerStorMountPoint + on;
            log.info("flushing input: {}", line);
            jobInput.writeBytes(line + "\n");
        }

    }

    @Override
    public void terminateInput() throws IOException {
        addInput(END);
    }

    @Override
    public String getOutputPath() {
        return directory + "/" + hostId;
    }

    @Override
    public int exitCode() throws InterruptedException {
        log.info("waiting for process to exit" );
        return process.waitFor();
    }

    @SuppressWarnings("UnusedDeclaration")
    private Runnable pipe(final InputStream in) {
        return new Runnable() {
            public void run() {
                Scanner s = new Scanner(in);
                while (!Thread.interrupted() && s.hasNext())
                    log.error(s.next());
                s.close();
            }
        };
    }

    @Override
    public void run() {

        log.info("running command: {}", Arrays.toString(commands));

        ProcessBuilder pb = new ProcessBuilder(commands)
                .directory(new File(directory))
                .redirectErrorStream(true);

        if(boot2docker) {
            Map<String, String> env = pb.environment();
            // todo: get these values from boot2docker shellinit output.
            env.put("PATH", "/bin:/usr/bin:/usr/local/bin");
            env.put("DOCKER_HOST", "tcp://192.168.59.103:2376");
            env.put("DOCKER_CERT_PATH", "/Users/amin/.boot2docker/certs/boot2docker-vm");
            env.put("DOCKER_TLS_VERIFY", "1");
            //log.info("boot2docker running env: {}", env);
        }

        try {
            //process = Runtime.getRuntime().exec(commands, envp, new File(directory));
            process = pb.start();
        } catch (IOException e) {
            log.error("error running job.", e);
            status = FAILED;
            return;
        }

        new Thread(pipe(process.getErrorStream())).start();
        new Thread(pipe(process.getInputStream())).start();

        try {
            Thread.sleep(100); // todo: why? give the process time to create input
            // todo: don't do this. move jobInput stream creation to receive on first input.
            // todo: check if enough time is past and file is there, then create it
        } catch (InterruptedException ignored) {
        }

        try {
            log.info("opening up jobInput to path: {}", inputDirectory);
            File f = new File(inputDirectory);
            if(!f.exists())
                log.warn("input file does not exist: {}", inputDirectory);
            else
                log.warn("input file exist: {}", inputDirectory);

            jobInput = new RandomAccessFile(inputDirectory, "rwd");

        } catch (FileNotFoundException e) {
            log.error("error opening input job.", e);
            status = FAILED;

            process.destroy();
            return;
        }

        status = SUCCESSFUL;
        log.info("opened up jobInput to path: {}", inputDirectory);
    }

    public static class Factory extends JobExecutorFactory {
        @Override
        public JobExecutor newInstance(String hostId, JobDefinition jobDef, String jobId, String mountPoint,
                                       JobDefinition.Phase.Type type) {
            return new DockerCommandJobExecutor(hostId, jobDef, jobId, mountPoint, type);
        }
    }
}
