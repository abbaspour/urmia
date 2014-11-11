package io.urmia.job;

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

import io.urmia.md.model.job.JobDefinition;
import io.urmia.md.model.job.JobExec;
import io.urmia.md.model.job.JobInput;
import io.urmia.md.model.storage.ObjectName;
import io.urmia.util.UnixPathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static io.urmia.job.JobExecutor.Status.*;
import static io.urmia.md.model.job.JobDefinition.Phase.Type.MAP;
import static io.urmia.md.model.job.JobDefinition.Phase.Type.REDUCE;
import static io.urmia.md.model.job.JobInput.END;

public class XargsJobExecutor implements JobExecutor {

    private static final Logger log = LoggerFactory.getLogger(XargsJobExecutor.class);

    private Status status = INIT;
    private Process process = null;

    private final String[] commands;
    private final String[] envp;

    private static final String RUNNER_BASE = "src/main/bash";
    private static final String MODULE_NAME = "jail-docker";
    private static final String SCRIPT_RUNNER = "job-runner.sh";
    private static final String SCRIPT_COLLECT = "collect-outputs.sh";

    private final String hostId;
    private final String directory;
    private final String inputDirectory;
    private final String mountPoint;
    private final JobDefinition.Phase.Type type;

    private RandomAccessFile jobInput = null;

    public XargsJobExecutor(String hostId, JobDefinition jobDef, String jobId, String mountPoint,
                            JobDefinition.Phase.Type type) {

        this.directory = UnixPathUtils.appendTrailingSlash(mountPoint) + jobDef.getOwner() + '/' + ObjectName.Namespace.JOBS.path + '/' + jobId;
        this.inputDirectory = directory + "/input";
        this.mountPoint = UnixPathUtils.trimSlash(mountPoint);
        this.hostId = hostId;
        this.type = type;

        List<String> commands = new LinkedList<String>();

        final String parent = UnixPathUtils.normalize(mountPoint + "/" + jobDef.getOwner());
        log.info("job {} parent folder: {}", jobId + parent);

        commands.add(getScriptPath(SCRIPT_RUNNER));
        commands.add("-p");
        commands.add(parent);
        commands.add("-i");
        commands.add(type == MAP ? hostId : hostId + "-reduce");
        commands.add("-j");
        commands.add(jobId);

        List<JobDefinition.Phase> selectedPhases = jobDef.getPhases(type);

        log.info("number of phases type: {} in job {} is: {}/{}", type, jobId, selectedPhases.size(), jobDef.getPhases().size());

        // todo: for reduce job, add collect-outputs.sh as the first phase.
        if(type == REDUCE) {
            log.info("job is reduce. adding collect phase first");
            commands.add("-c");
            commands.add(getScriptPath(SCRIPT_COLLECT));
        }

        for (JobDefinition.Phase p : selectedPhases) {
            log.info("number of exec in phase {} is: {}", p, p.exec.size());
            for (JobExec exec : p.exec) {
                commands.add("-c");
                commands.add(exec.getCommand());
            }
        }

        this.commands = commands.toArray(new String[commands.size()]);

        envp = new String[]{
                "PATH=/bin:/usr/bin:/usr/local/bin"
        };

        boolean mkDirResult = new File(directory).mkdirs();
        log.info("mkdirs to {} was: {}", directory, mkDirResult);
    }

    protected String getScriptPath(String SCRIPT_NAME) {
        String pwd = System.getProperty("user.dir");

        String scriptPath = pwd + File.separatorChar + "source" + File.separatorChar + RUNNER_BASE + File.separatorChar + SCRIPT_NAME;

        File f = new File(scriptPath);

        if (f.exists()) {
            if (f.canExecute())
                return scriptPath;
            else throw new RuntimeException("script exists but not executable: " + scriptPath);
        }

        String scriptPath2 = pwd + File.separatorChar + "source" + File.separatorChar + MODULE_NAME + File.separatorChar + RUNNER_BASE + File.separatorChar + SCRIPT_NAME;

        File f2 = new File(scriptPath2);
        if (f2.exists()) {
            if (f2.canExecute())
                return scriptPath2;
            else throw new RuntimeException("script exists but not executable: " + scriptPath2);

        }

        String scriptPath3 = pwd + File.separatorChar + ".." + File.separatorChar + MODULE_NAME + File.separatorChar + RUNNER_BASE + File.separatorChar + SCRIPT_NAME;

        File f3 = new File(scriptPath3);
        if (f3.exists()) {
            if (f3.canExecute())
                return scriptPath3;
            else throw new RuntimeException("script exists but not executable: " + scriptPath3);

        }

        String scriptPath4 = pwd + File.separatorChar + "../.." + File.separatorChar + MODULE_NAME + File.separatorChar + RUNNER_BASE + File.separatorChar + SCRIPT_NAME;

        File f4 = new File(scriptPath4);
        if (f4.exists()) {
            if (f4.canExecute())
                return scriptPath4;
            else throw new RuntimeException("script exists but not executable: " + scriptPath4);

        }

        throw new RuntimeException("script (" + SCRIPT_NAME + ") not found in:\n" + scriptPath + "\n" + scriptPath2 + "\n" + scriptPath3 + "\n" + scriptPath4);

    }

    @Override
    public void addInput(JobInput input) throws IOException {
        if (jobInput == null)
            throw new RuntimeException("stream not available yet");

        if (status != SUCCESSFUL)
            throw new IllegalArgumentException("job not in successful mode: " + status);

        if (input.isEod()) {
            log.info("closing input. file: {}", inputDirectory);
            jobInput.close();
            jobInput = null;
            return;
        }

        log.info("passing job input to stream: {}", input);

        for (String on : input) {
            String line = normalizeInput(on);
            log.info("flushing input: {}", line);
            jobInput.writeBytes(line + "\n");
        }

    }

    /**
     * the format of input is different for map and reduce.
     * map is ON, reduce is URL (http://ODS:port/ON) for remote and disk path for local
     * @return normalized path (disk or remote http)
     * /var/tmp/urmia/tck/ods/1[/tck/jobs/e5d8714d-882c-41fe-89e3-788b74bd2bb0/stor/tck/jobs/e5d8714d-882c-41fe-89e3-788b74bd2bb0/2c8a34b9-6bcb-4e8d-982c-35bdb9931ff1]
     */
    private String normalizeInput(String on) {
        if(type == REDUCE && on.startsWith("http://"))
            return on;

        return mountPoint + on;
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
        log.info("waiting for '{}' process to exit", type );
        return process.waitFor();
    }

    @Override
    public void run() {

        log.info("running command: {}", Arrays.toString(commands));
        log.info("running env: {}", Arrays.toString(envp));

        try {
            process = Runtime.getRuntime().exec(commands, envp, new File(directory));
        } catch (IOException e) {
            log.error("error running job.", e);
            status = FAILED;
            return;
        }

        try {
            Thread.sleep(100); // todo: why
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
            return new XargsJobExecutor(hostId, jobDef, jobId, mountPoint, type);
        }
    }
}
