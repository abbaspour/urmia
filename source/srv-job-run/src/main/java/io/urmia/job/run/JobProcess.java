package io.urmia.job.run;

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

import com.google.common.base.Optional;
import io.urmia.job.JobExecutor;
import io.urmia.job.JobExecutorFactory;
import io.urmia.md.model.job.JobDefinition;
import io.urmia.md.model.job.JobInput;
import io.urmia.md.model.job.LineJobInput;
import io.urmia.naming.model.NodeType;
import io.urmia.naming.service.NamingService;
import io.urmia.util.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.queue.QueueBuilder;
import org.apache.curator.framework.recipes.queue.QueueConsumer;
import org.apache.curator.framework.recipes.queue.QueueSerializer;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.x.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import static io.urmia.md.model.job.JobDefinition.Phase.Type.*;
import static io.urmia.md.model.job.JobInput.END;

public class JobProcess implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(JobProcess.class);
    private static final JobExecutorFactory jobExecFactory = JobExecutorFactory.lookup();

    private final String zkPath;

    private final JobExecutor je;
    private final CuratorFramework client;
    private final JobDefinition jd;
    private final JobDefinition.Phase.Type type;
    private final String hostId;
    private final NamingService ns;

    public JobProcess(String hostId, CuratorFramework client, String zkPath, String mountPoint, NamingService ns) throws Exception {

        if (StringUtils.isBlank(zkPath))
            throw new IllegalArgumentException("zkPath is blank");

        this.zkPath = zkPath;
        this.client = client;
        this.hostId = hostId;
        this.ns = ns;

        type = isReduce(zkPath) ? REDUCE : MAP;
        String jobId = getId(zkPath);

        if (StringUtils.isBlank(jobId))
            throw new IllegalArgumentException("unable to find jobId from zkPath: " + zkPath);

        String jobDefJson = new String(client.getData().forPath(zkPath)).trim();
        log.info("jobDef at zk path {} => {}, type: {}", zkPath, jobDefJson, type);

        jd = new JobDefinition(jobDefJson);

        je = jobExecFactory.newInstance(hostId, jd, jobId, mountPoint, type);

        QueueBuilder<JobInput> builder = QueueBuilder.builder(client, new InputQueueConsumer(), serializer, zkPath + "/live/in");
        builder.buildQueue().start();
    }

    public static String getId(String path) {
        int index = path.lastIndexOf('/');
        return isReduce(path) ? path.substring(index + 1, path.lastIndexOf('-')) : path.substring(index + 1);
    }

    private static boolean isReduce(String path) {
        return path.endsWith("-reduce");
    }

    private static final QueueSerializer<JobInput> serializer = new JobInputQueueSerializer();

    private static class JobInputQueueSerializer implements QueueSerializer<JobInput> {

        @Override
        public byte[] serialize(JobInput s) {
            return s.toString().getBytes();
        }

        @Override
        public JobInput deserialize(byte[] bytes) {
            if (bytes == null || bytes.length == 0) return END;
            if (bytes.length == 1 && bytes[0] == 4) return END;

            String s = new String(bytes).trim();
            return new LineJobInput(s);
        }
    }

    private volatile boolean EOD = false;

    private final Object lock = new Object();

    // todo: move timeOut concept to JobExecutor? return as an Exception maybe
    private boolean timedOut() {
        //return System.currentTimeMillis() - startTime > TIMEOUT;
        return false;
    }

    @Override
    public void run() {

        log.info("running {}, job from node under: {}", type, zkPath);

        int exitCode = 0;

        je.run();

        try {
            exitCode = je.exitCode();
        } catch (InterruptedException e) {
            log.error("InterruptedException for exit code", e);
        }

        if (exitCode != 0)
            throw new IllegalArgumentException("unable to start job runner for job: " + jd + ", exitCode: " + exitCode);

        while (!(Thread.interrupted() || EOD || timedOut())) {
            synchronized (lock) {
                try {
                    lock.wait(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }

        log.info("process finished. EOD: {}, time out: {}", EOD, timedOut());
    }

    private class InputQueueConsumer implements QueueConsumer<JobInput> {
        @Override
        public void consumeMessage(JobInput input) throws Exception {
            log.info("queue {}/live/in --> message: {}", zkPath, input);


            if (input.isEod()) {
                EOD = true;
                je.terminateInput();

                log.info("end of data deleting job queue: {}", zkPath);
                client.delete().deletingChildrenIfNeeded().inBackground().forPath(zkPath);

                synchronized (lock) {
                    lock.notifyAll();
                }

            } else {
                je.addInput(normalize(input));
            }

        }

        private JobInput normalize(JobInput input) {
            if (type == MAP)
                return input;

            List<String> lines = new LinkedList<String>();

            for (String l : input) {
                String p = l.substring(l.indexOf("/stor/") + 5);
                String instanceId = p.substring(p.lastIndexOf('/') + 1);

                if (hostId.equals(instanceId)) {
                    log.info("reduce input is in my instance");
                    lines.add(p);
                } else {
                    log.info("reduce input is remote on host: {}", instanceId);
                    Optional<String> downloadURL = getDownloadURL(instanceId, p);
                    if (downloadURL.isPresent()) {
                        log.info("adding remote resource for reduce: {}", downloadURL.get());
                        lines.add(downloadURL.get());
                    }
                }
            }

            return new LineJobInput(lines);
        }

        private Optional<String> getDownloadURL(String hostId, String resource) {
            try {
                ServiceInstance<NodeType> ods = ns.discover(NodeType.ODS, hostId);
                String url = "http://" + ods.getAddress() + ":" + ods.getPort() + resource;
                return Optional.of(url);
            } catch (Exception e) {
                log.warn("fail to find ODS: {}. reduce is complete. msg:{}", hostId, e.getMessage());
                return Optional.absent();
            }
        }

        @Override
        public void stateChanged(CuratorFramework client, ConnectionState newState) {
            log.info("queue {} stateChanged: {}", zkPath, newState);
        }
    }

}
