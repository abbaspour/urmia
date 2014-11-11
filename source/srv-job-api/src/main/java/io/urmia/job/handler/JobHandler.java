package io.urmia.job.handler;

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
import com.google.common.collect.Lists;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import io.urmia.md.model.storage.FullObjectName;
import io.urmia.proxy.HttpProxyFrontendHandler;
import io.urmia.job.pub.*;
import io.urmia.md.model.job.*;
import io.urmia.md.service.MetadataService;
import io.urmia.naming.model.NodeType;
import io.urmia.naming.service.NamingService;
import io.urmia.util.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.ACCEPTED;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class JobHandler extends SimpleChannelInboundHandler<JobRequest> {

    private static final Logger log = LoggerFactory.getLogger(JobHandler.class);

    // todo: weak reference or TTL cache
    private static final Map<String, JobQueue> jobQueueMap = new LinkedHashMap<String, JobQueue>();
    private static final Map<String, JobStatusUpdateHandler> statUpdaterMap = new LinkedHashMap<String, JobStatusUpdateHandler>();

    private final CuratorFramework client;
    private final MetadataService mds;
    private final JobStatusMonitor statusMon;
    private final NamingService ns;

    public JobHandler(CuratorFramework client, MetadataService mds, NamingService ns) {
        this.client = client;
        this.mds = mds;
        this.statusMon = new ZkJobStatusMonitor(client);
        this.ns = ns;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        JobRequest jobRequest = ctx.attr(jobAttributeKey).getAndRemove();
        log.info("channelInactive jobRequest: {} in ctx: {}", jobRequest, ctx);

        //if(jobRequest == null)
        //    return;
        //log.info("publishing EOT as channel inactive for job: {}", jobRequest);
        //input(new JobInputRequest(jobRequest, JobInput.end()));
    }

    public static final AttributeKey<JobRequest> jobAttributeKey = AttributeKey.valueOf("jobRequest");

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final JobRequest msg) throws Exception {

        log.info("channelRead0 received: {}", msg);

        String location = msg.objectName.owner + '/' + msg.objectName.ns.path + '/' + msg.getId();

        if (StringUtils.isBlank(location)) {
            final HttpResponse httpResp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
            ctx.writeAndFlush(httpResp);
            ctx.channel().close();
            return;
        }

        if (msg instanceof JobQueryRequest) {
            JobQueryRequest queryRequest = (JobQueryRequest) msg;
            Optional<String> status = query(queryRequest);
            final HttpResponse httpResp;

            if(status.isPresent()) {
                byte[] statusByes = status.get().getBytes();
                httpResp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, OK, Unpooled.wrappedBuffer(statusByes));
                httpResp.headers().set("Content-Type", "application/json");
                httpResp.headers().set("Content-Length", statusByes.length);
                httpResp.headers().set("Connection", "close");
            } else {
                String err = String.format("{\"code\":\"ResourceNotFound\",\"message\":\"%s was not found\"}", location);
                httpResp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, Unpooled.wrappedBuffer(err.getBytes()));
                httpResp.headers().set("Content-Length", err.length());
                httpResp.headers().set("Connection", "close");
            }
            ctx.writeAndFlush(httpResp);
            return;
        }

        if (msg instanceof JobCancelRequest) {
            JobCancelRequest cancelRequest = (JobCancelRequest) msg;

            cancel(cancelRequest);
            final HttpResponse httpResp;
            httpResp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, ACCEPTED);
            httpResp.headers().set("Content-Type", "application/json");
            httpResp.headers().set("Content-Length", 0);
            httpResp.headers().set("Connection", "close");

            ctx.writeAndFlush(httpResp);
            return;
        }

        if (msg instanceof JobCreateRequest) {
            JobCreateRequest createRequest = (JobCreateRequest) msg;
            register(createRequest);

            ctx.attr(jobAttributeKey).setIfAbsent(msg);
            log.info("added jobReq {} to ctx: {}", msg, ctx);

            DefaultFullHttpResponse httpResp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, CREATED);
            httpResp.headers().add("location", msg.getId());
            httpResp.headers().add("Content-Length", 0);

            ctx.writeAndFlush(httpResp);
            return;
        }

        if (msg instanceof JobInputRequest) {
            JobInputRequest inputRequest = (JobInputRequest) msg;
            input(inputRequest);

            boolean end = inputRequest.input.isEod();
            final HttpResponse httpResp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, CREATED);
            httpResp.headers().add("Content-Length", 0);
            ctx.writeAndFlush(httpResp);

            if (end)
                ctx.channel().close();
            return;

        }

        if(msg instanceof JobGetRequest) {
            JobGetRequest getRequest = (JobGetRequest) msg;
            setupProxyGet(ctx, getRequest);
            return;
        }

        log.error("invalid input");
        final HttpResponse httpResp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
        ctx.writeAndFlush(httpResp);
        ctx.channel().close();

    }

    private void register(JobCreateRequest msg) throws Exception {

        JobStatusUpdateHandler handler = statusMon.register(msg.getId(), msg.job);
        statUpdaterMap.put(msg.getId(), handler);

        JobQueue q = new InputAwareZkJobQueue(client, mds, ns, msg, handler);
        jobQueueMap.put(msg.getId(), q);

        log.info("registered queue against job: {}", msg.getId());

    }

    private void input(JobInputRequest msg) throws Exception {

        log.debug("publish msg of: {}", msg);

        JobQueue q = jobQueueMap.get(msg.getId());

        if (q == null) {
            log.warn("discarding input. no job queue found for id: {}", msg.getId());
            return;
        }

        if (msg.input.isEod()) {
                log.warn("EOT for job: {}", msg.getId());
                jobQueueMap.remove(msg.getId());
                q.close();
        } else {
            log.info("publishing to q: {}, input: {}", q, msg.input);
            addToInputs(msg);
            q.put(msg.input);
        }
    }

    private void addToInputs(JobInputRequest input) throws Exception {
        String insPath = "/urmia/1/jobs/" + input.objectName.owner + "/" + input.getId() + "/live/in.txt";

        byte[] existing = new byte[0];
        try {
            existing = client.getData().forPath(insPath);
        } catch (Exception e) {
            log.warn("unable to fetch from {}, msg: {}", insPath, e.getMessage());
        }

        client.create().creatingParentsIfNeeded().inBackground().forPath(insPath, StringUtils.append(existing, input.input.toBytes()));
    }

    private boolean cancel(JobCancelRequest q) {
        log.info("please implement cancel: {}", q);
        return true;
    }

    private Optional<String> query(JobQueryRequest q) {
        JobQueryRequest.Type type = q.type;

        if(type == JobQueryRequest.Type.Status)
            try {
                Optional<JobStatus> status = statusMon.getStatus(q.getId(), handler(q.getId()));
                return Optional.of(status.get().toString());
            } catch (Exception e) {
                log.error("exception in query {}, msg: {}", q, e.getMessage());
                return Optional.absent();
            }


        if(type == JobQueryRequest.Type.Outputs) {
            String outPath = "/urmia/1/jobs/" + q.objectName.owner + "/" + q.getId() + "/live/out.txt";

            log.debug("scanning path for query output: {}", outPath);

            try {
                String outputs = new String(client.getData().forPath(outPath)).trim();
                return Optional.of(outputs);
            } catch (Exception e) {
                log.error("exception in query {}, msg: {}", q, e.getMessage());
                return Optional.absent();
            }
        }

        if(type == JobQueryRequest.Type.Inputs) {
            String inPath = "/urmia/1/jobs/" + q.objectName.owner + "/" + q.getId() + "/live/in.txt";

            log.debug("scanning path for query inputs: {}", inPath);

            try {
                String inputs = new String(client.getData().forPath(inPath)).trim();
                return Optional.of(inputs);
            } catch (Exception e) {
                log.error("exception in query {}, msg: {}", q, e.getMessage());
                return Optional.absent();
            }
        }

        if(type == JobQueryRequest.Type.List) {
            String listPath = "/urmia/1/jobs/" + q.objectName.owner ;

            log.debug("scanning path for query list: {}", listPath);

            StringBuilder sb = new StringBuilder();
            try {
                List<String> children = client.getChildren().forPath(listPath);
                for(String child : children)
                    sb.append(String.format("{\"name\":\"%s\",\"type\":\"directory\",\"mtime\":\"2014-07-24T04:50:30.573Z\"}\n", child));

                return Optional.of(sb.toString());
            } catch (Exception e) {
                log.error("exception in query {}, msg: {}", q, e.getMessage());
                return Optional.absent();
            }
        }

        return Optional.absent();
    }

    private JobStatusUpdateHandler handler(String jobId) {
        JobStatusUpdateHandler handler = statUpdaterMap.get(jobId);
        if(handler != null)
            return handler;

        handler = new ZkJobStatusUpdaterHandler(client, jobId);
        statUpdaterMap.put(jobId, handler);
        return handler;
    }

    private void setupProxyGet(final ChannelHandlerContext ctx, JobGetRequest getRequest) throws Exception {
        ServiceInstance<NodeType> ods = ns.get(NodeType.ODS, getRequest.getStorageNodeId());

        List<ServiceInstance<NodeType>> l = Lists.newArrayList(ods);
        log.info("proxy storage node: {}", ods);
        HttpRequest initHttpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, getRequest.objectName.toString());
        HttpProxyFrontendHandler proxy = new HttpProxyFrontendHandler(l, mds, initHttpRequest, ctx, true, Optional.<FullObjectName>absent());

        ctx.pipeline().remove("encoder");
        ctx.pipeline().remove("aggregator");
        ctx.pipeline().remove("job-decoder");
        ctx.pipeline().remove("job");

        ctx.pipeline().addLast("proxy", proxy);

    }
}
