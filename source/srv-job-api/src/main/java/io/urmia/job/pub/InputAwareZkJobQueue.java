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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.*;
import io.urmia.md.model.job.*;
import io.urmia.md.model.storage.ObjectName;
import io.urmia.md.service.MetadataService;
import io.urmia.naming.model.NodeType;
import io.urmia.naming.service.NamingService;
import io.urmia.util.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.recipes.queue.DistributedQueue;
import org.apache.curator.framework.recipes.queue.QueueBuilder;
import org.apache.curator.framework.recipes.queue.QueueSerializer;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static io.urmia.md.model.job.JobInput.END;

/**
 * todo: review algorithm to reuse existing queues. not always first storage node.
 */
public class InputAwareZkJobQueue implements JobQueue {

    private static final Logger log = LoggerFactory.getLogger(InputAwareZkJobQueue.class);

    private final CuratorFramework client;
    private final MetadataService mds;
    private final JobDefinition jobDef;
    private final String jobId;
    private final JobStatusUpdateHandler statHandler;
    private final NamingService ns;

    private final Map<String, DistributedQueue<JobInput>> queueMap =
            new LinkedHashMap<String, DistributedQueue<JobInput>>();

    //private DistributedQueue<JobInput> reduceQueue = null;

    private final Map<String, Boolean> finishedMap = new LinkedHashMap<String, Boolean>();

    private final Map<String, AtomicInteger> inputCounter = new LinkedHashMap<String, AtomicInteger>();

    public InputAwareZkJobQueue(CuratorFramework client, MetadataService mds, NamingService ns,
                                JobCreateRequest request,
                                JobStatusUpdateHandler statHandler) throws Exception {
        this.client = client;
        this.mds = mds;
        this.jobDef = request.job;
        this.jobId = request.getId();
        this.statHandler = statHandler;
        this.ns = ns;
    }

    @Override
    public void put(JobInput input) throws Exception {

        final Map<String, JobInput> inputPerNode = split(input);

        log.info("split input {} to map: {}", input, inputPerNode);

        for (Map.Entry<String, JobInput> e : inputPerNode.entrySet()) {
            getQueue(e.getKey())
                    .put(e.getValue());
            inputCounter.get(e.getKey()).addAndGet(e.getValue().getCount());
        }

    }

    @Override
    public void close() throws Exception {
        statHandler.markInputDone();

        for (DistributedQueue<JobInput> q : queueMap.values()) {
            log.info("closing DistributedQueue: {}", q);
            q.put(END);
            q.close();
        }
    }

    private Map<String, JobInput> split(JobInput input) throws Exception {
        ImmutableMultimap.Builder<String, ObjectName> b = ImmutableMultimap.builder();

        for (/*ObjectName*/String in : input) {
            Optional<ObjectName> optOn = ObjectName.of(in);
            if (!optOn.isPresent()) {
                log.warn("no ObjectName for input: {}", in);
                continue;
            }
            ObjectName on = optOn.get();
            ServiceInstance<NodeType> s = whereIs(on);
            log.info("looking where is on: {} -> {}", on, s);
            if (s != null) b.put(s.getId(), on);
        }

        Map<String, Collection<ObjectName>> m = b.build().asMap();

        return Maps.transformValues(m, transformer());
    }

    private Function<Collection<ObjectName>, JobInput> transformer() {
        return new Function<Collection<ObjectName>, JobInput>() {
            public JobInput apply(Collection<ObjectName> input) {
                return new LineJobInput(input);
            }
        };
    }

    private ServiceInstance<NodeType> findRunnerInstanceUp(List<String> ids) throws Exception {
        for (String id : ids) {
            ServiceInstance<NodeType> si = ns.discover(NodeType.JRS, id);
            if (si != null) {
                log.debug("found running jrs node of id: {}", id);
                return si;
            } else {
                log.debug("jrs node is down: {}", id);
            }
        }
        return null;
    }

    private ServiceInstance<NodeType> whereIs(ObjectName on) throws Exception {
        List<String> storedNodes = mds.storedNodes(on);
        log.info("stored nodes on {} -> {}", on, storedNodes);
        return findRunnerInstanceUp(storedNodes);
    }

    private DistributedQueue<JobInput> getQueue(String instanceId) throws Exception {

        DistributedQueue<JobInput> cachedQ = queueMap.get(instanceId);
        if (cachedQ != null)
            return cachedQ;

        String json = jobDef.toString();
        byte[] bytes = json.getBytes();

        String jobs = "/urmia/1/" + instanceId + "/jobs";

        if (client.checkExists().forPath(jobs) == null)
            client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(jobs);


        String p = jobs + "/" + jobId;
        log.info("creating job: {} -> {}", p, json);

        client.create().withMode(CreateMode.PERSISTENT).forPath(p, bytes);

        String ip = p + "/live/in";

        QueueBuilder<JobInput> builder = QueueBuilder.builder(client, null, serializer, ip);

        client.checkExists().usingWatcher(new QueueExistsWatcher()).forPath(p);

        Thread.sleep(500); // todo: why?

        DistributedQueue<JobInput> q = builder.buildQueue();
        q.start();

        queueMap.put(instanceId, q);
        finishedMap.put(instanceId, Boolean.FALSE);
        inputCounter.put(instanceId, new AtomicInteger(0));
        statHandler.incTasks(1);

        return q;
    }

    private DistributedQueue<JobInput> getReduceQueue(String instanceId) throws Exception {

        String json = jobDef.toString();
        byte[] bytes = json.getBytes();

        String jobs = "/urmia/1/" + instanceId + "/jobs";

        if (client.checkExists().forPath(jobs) == null)
            client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(jobs);


        String p = jobs + "/" + jobId + "-reduce";
        log.info("creating job: {} -> {}", p, json);

        client.create().withMode(CreateMode.PERSISTENT).forPath(p, bytes);

        String ip = p + "/live/in";

        QueueBuilder<JobInput> builder = QueueBuilder.builder(client, null, serializer, ip);

        client.checkExists().usingWatcher(new QueueExistsWatcher()).forPath(p);

        Thread.sleep(500); // todo: why?

        DistributedQueue<JobInput> q = builder.buildQueue();
        q.start();

        return q;
    }

    private class QueueExistsWatcher implements CuratorWatcher {

        @Override
        public void process(WatchedEvent event) throws Exception {
            log.info("QueueExistsWatcher received event: {}", event);

            if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
                String qPath = event.getPath();
                String hostId = getNodeId(qPath);

                if (isReduceQueuePath(qPath)) {
                    log.info("reduce job finished on q: {}", qPath);
                    addToOutputs(hostId, false, true);

                } else {
                    log.info("map job finished on node: {}", hostId);

                    boolean allFinished = markFinished(hostId);

                    addToOutputs(hostId, jobDef.hasReduce(), false);

                    if (allFinished && jobDef.hasReduce()) {

                        if (mapOutputs.isEmpty()) {
                            log.warn("map outputs empty. skipping reduce phase.");
                            return;
                        }

                        String reduceHostId = getReduceNodeId();

                        log.info("process finished in all workers. best reduce node: {}", reduceHostId);

                        log.info("reduce input(s): {}", mapOutputs);

                        buildReduceQueue(reduceHostId);
                    }
                }
            }
        }
    }

    private boolean isReduceQueuePath(String path) {
        return path.endsWith("-reduce");
    }

    private void buildReduceQueue(String hostId) throws Exception {
        DistributedQueue<JobInput> reduceQ = getReduceQueue(hostId);

        Thread.sleep(500);

        JobInput reduceInput = new LineJobInput(mapOutputs);

        log.info("putting map results into reduce q: {}", reduceInput);
        reduceQ.put(reduceInput);
        reduceQ.put(JobInput.END);
    }

    private List<String> mapOutputs = new LinkedList<String>();

    private void addToOutputs(String hostId, boolean queue, boolean reducePhase) throws Exception {
        if (queue) {
            String path = '/' + jobDef.getOwner() + "/jobs/" + jobId + "/stor" + '/' + jobDef.getOwner() + '/' + ObjectName.Namespace.JOBS.path + '/' + jobId + '/' + hostId;
            mapOutputs.add(path);
            return;
        }
        addToOutputsZk(hostId, reducePhase);
    }

    private void addToOutputsZk(String hostId, boolean reducePhase) throws Exception {

        String outPath = "/urmia/1/jobs/" + jobDef.getOwner() + "/" + jobId + "/live/out.txt";

        byte[] existing = new byte[0];
        try {
            existing = client.getData().forPath(outPath);
        } catch (Exception ignored) {
        }
        String path = '/' + jobDef.getOwner() + "/jobs/" + jobId + "/stor" + '/' +
                jobDef.getOwner() + '/' + ObjectName.Namespace.JOBS.path + '/' + jobId + '/' + hostId;

        if(reducePhase)
            path += "-reduce";

        log.info("adding to outputs of job {}: {}", jobId, path);

        client.create().creatingParentsIfNeeded().inBackground().forPath(outPath, StringUtils.append(existing, path));
    }

    /*
    private String getMapPhaseOutputs() {
        StringBuilder b = new StringBuilder();

        for (String host : queueMap.keySet()) {
            try {
                String d = new String(client.getData().forPath("/urmia/1/" + host + "/jobs/" + jobId + "/live/out.txt")).trim();
                b.append(d).append('\n');
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return b.toString();
    }
    */

    private String getReduceNodeId() {
        int min = -1;
        String id = "";
        for (Map.Entry<String, AtomicInteger> e : inputCounter.entrySet())
            if (min < e.getValue().get()) {
                min = e.getValue().get();
                id = e.getKey();
            }
        return id;
    }

    private boolean markFinished(String id) {
        finishedMap.put(id, Boolean.TRUE);
        for (Boolean b : finishedMap.values())
            if (!b) return false;
        return true;
    }

    // /urmia/1/2c8a34b9-6bcb-4e8d-982c-35bdb9931ff1/jobs/c1481511-1870-4a28-9a09-9f31133b12fc(-reduce)
    private static String getNodeId(String qPath) {
        int index = qPath.indexOf("/urmia/1/") + 9;
        int end = qPath.indexOf('/', index);
        return qPath.substring(index, end);
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
