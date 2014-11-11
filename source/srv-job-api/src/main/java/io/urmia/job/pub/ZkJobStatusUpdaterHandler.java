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

import io.urmia.md.model.job.JobStatus;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicInteger;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkJobStatusUpdaterHandler implements JobStatusUpdateHandler {

    private static final Logger log = LoggerFactory.getLogger(ZkJobStatusUpdaterHandler.class);

    private static final RetryPolicy RETRY_POLICY = new ExponentialBackoffRetry(1000, 3);

    private final DistributedAtomicInteger inputDone;
    private final DistributedAtomicInteger cancelled;

    private final DistributedAtomicInteger errors;
    private final DistributedAtomicInteger outputs;
    private final DistributedAtomicInteger retries;
    private final DistributedAtomicInteger tasks;
    private final DistributedAtomicInteger tasksDone;

    public ZkJobStatusUpdaterHandler(CuratorFramework client, String jobId) {

        String path = getGlobalZkPath(jobId);


        inputDone = new DistributedAtomicInteger(client, path + "/stats/inputDone", RETRY_POLICY);
        cancelled = new DistributedAtomicInteger(client, path + "/stats/cancelled", RETRY_POLICY);

        errors = new DistributedAtomicInteger(client, path + "/stats/errors", RETRY_POLICY);
        outputs = new DistributedAtomicInteger(client, path + "/stats/outputs", RETRY_POLICY);
        retries = new DistributedAtomicInteger(client, path + "/stats/retries", RETRY_POLICY);
        tasks = new DistributedAtomicInteger(client, path + "/stats/tasks", RETRY_POLICY);
        tasksDone = new DistributedAtomicInteger(client, path + "/stats/tasksDone", RETRY_POLICY);
    }

    private String getGlobalZkPath(String jobId) {
        return "/urmia/1/jobs/" + jobId;
    }

    @Override
    public JobStatus.Stats getStats() throws Exception {

        AtomicValue<Integer> idv = inputDone.get();
        boolean inputDone = idv.succeeded() && idv.preValue() > 0;

        AtomicValue<Integer> cv = cancelled.get();
        boolean cancelled = cv.succeeded() && cv.preValue() > 0;

        final JobStatus.Counters counters = new JobStatus.Counters(
                errors.get().preValue(),
                outputs.get().postValue(),
                retries.get().preValue(),
                tasks.get().preValue(),
                tasksDone.get().preValue()
        );

        JobStatus.State state = JobStatus.State.created;

        return new JobStatus.Stats(state, cancelled, inputDone, counters);
    }

    @Override
    public void incErrors(int delta) throws Exception {
        errors.add(delta);
    }

    @Override
    public void incTasks(int delta) throws Exception {
        tasks.add(delta);
    }

    public void markCancelled() throws Exception {
        cancelled.increment();
    }

    public void markInputDone() throws Exception {
        inputDone.increment();
    }
}
