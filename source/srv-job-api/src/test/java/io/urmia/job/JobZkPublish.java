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

import com.google.common.collect.Lists;
import io.urmia.md.model.job.JobDefinition;
import io.urmia.md.model.job.JobExec;
import io.urmia.md.model.job.JobInput;
import io.urmia.md.model.job.LineJobInput;
import io.urmia.naming.service.RandomUuidImpl;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.queue.DistributedQueue;
import org.apache.curator.framework.recipes.queue.QueueBuilder;
import org.apache.curator.framework.recipes.queue.QueueSerializer;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import static io.urmia.md.model.job.JobInput.END;

public class JobZkPublish {

    private static final Logger log = LoggerFactory.getLogger(JobZkPublish.class);
    private static final String ZK_SERVER = "localhost:2181";

    public static void main(String[] args) throws Exception {

        CuratorFramework client = CuratorFrameworkFactory.newClient(ZK_SERVER, new ExponentialBackoffRetry(1000, 3));
        client.start();

        // create my node
        String hostname = InetAddress.getLocalHost().getHostName();

        String jobs = "/urmia/1/" + hostname + "/jobs";

        JobExec lsJob = new JobExec.Shell("ls");
        JobDefinition.Phase lsJobPhase = new JobDefinition.Phase(lsJob);
        JobDefinition jobDef = new JobDefinition("tck", Lists.newArrayList(lsJobPhase));

        byte[] lsJobBytes = jobDef.toString().getBytes();

        if (client.checkExists().forPath(jobs) == null)
            client.create().withMode(CreateMode.PERSISTENT).forPath(jobs);

        String i = new RandomUuidImpl().next();

        log.info("creating job: {}", i);
        String p = jobs + "/" + i;

        client.create().withMode(CreateMode.PERSISTENT).forPath(p, lsJobBytes);

        String ip = p + "/live/in";

        QueueBuilder<JobInput> builder = QueueBuilder.builder(client, null, serializer, ip);

        Thread.sleep(500);

        DistributedQueue<JobInput> q = builder.buildQueue();

        q.start();

        q.put(new LineJobInput("/"));
        q.put(new LineJobInput("/tmp"));
        q.put(END);

        q.flushPuts(1, TimeUnit.SECONDS);

        Thread.sleep(1000);

    }

    private static final QueueSerializer<JobInput> serializer = new JobInputQueueSerializer();

    private static class JobInputQueueSerializer implements QueueSerializer<JobInput> {

        @Override
        public byte[] serialize(JobInput s) {
            log.info("serialize job: {}", s);
            return s.toBytes();
        }

        @Override
        public JobInput deserialize(byte[] bytes) {
            return new LineJobInput(new String(bytes).trim());
        }
    }

}
