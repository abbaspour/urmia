package io.urmia.job.pub;

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
import io.urmia.md.model.job.JobDefinition;
import io.urmia.md.model.job.JobStatus;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkJobStatusMonitor implements  JobStatusMonitor {

    private static final Logger log = LoggerFactory.getLogger(ZkJobStatusMonitor.class);

    private final CuratorFramework client;

    public ZkJobStatusMonitor(CuratorFramework client) {
        this.client = client;
    }

    private String getGlobalZkPath(String jobId) {
        return "/urmia/1/jobs/" + jobId;
    }

    @Override
    public Optional<JobStatus> getStatus(String jobId, JobStatusUpdateHandler handler) throws Exception {
        String path = getGlobalZkPath(jobId);

        if(client.checkExists().forPath(path) == null)
            return Optional.absent();

        byte[] statusBytes = client.getData().forPath(path);
        String statusJobString = new String(statusBytes).trim();

        log.info("statusJobString: {}", statusJobString);

        JobStatus.Stats counters = handler.getStats();

        JobStatus jobStatus = new JobStatus(statusJobString, counters);

        return Optional.of(jobStatus);
    }

    @Override
    public JobStatusUpdateHandler register(String jobId, JobDefinition def) throws Exception {

        String path = getGlobalZkPath(jobId);

        if(client.checkExists().forPath(path) != null)
            throw new IllegalArgumentException("job with given id already exists: " + jobId);

        JobStatus status = new JobStatus(jobId, def);

        byte[] statusBytes = status.toString().getBytes();

        client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path, statusBytes);

        return new ZkJobStatusUpdaterHandler(client, jobId);

    }
}
