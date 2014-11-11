package io.urmia.api.handler;

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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.urmia.proxy.HttpProxyFrontendHandler;
import io.urmia.md.model.*;
import io.urmia.md.model.storage.ExtendedObjectAttributes;
import io.urmia.md.model.storage.FullObjectName;
import io.urmia.md.model.storage.FullObjectRequest;
import io.urmia.md.model.storage.ObjectName;
import io.urmia.md.service.MetadataService;
import io.urmia.naming.model.NodeType;
import io.urmia.naming.service.NamingService;
import io.urmia.naming.service.Uuid;
import org.apache.curator.x.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class RestApiHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static final Logger log = LoggerFactory.getLogger(RestApiHandler.class);

    private static FullHttpResponse ERROR_BAD_REQUEST = new DefaultFullHttpResponse(HTTP_1_1,
            HttpResponseStatus.BAD_REQUEST, errorBody("BadRequest", "request is not valid"));

    private static enum RequestState {
        EXPECT_REQUEST, // start
        EXPECT_CONTENT_OR_LAST, // PUT
        EXPECT_LAST // anything else
    }

    private final MetadataService mds;
    private final Uuid uuid;
    private final NamingService ns;

    private volatile RequestState requestState = RequestState.EXPECT_REQUEST;
    private volatile HttpRequest httpRequest = null;
    private volatile ObjectRequest objectRequest = null;
    private volatile boolean proxyMode = false;
    private volatile HttpRequest request;

    public RestApiHandler(MetadataService mds, Uuid uuid, NamingService ns) {
        super(false);
        this.mds = mds;
        this.uuid = uuid;
        this.ns = ns;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("channelActive: {}", ctx);
        super.channelActive(ctx);
        ctx.read();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {

        if (msg instanceof HttpRequest) {

            if (requestState != RequestState.EXPECT_REQUEST) {
                sendError(ctx, ERROR_BAD_REQUEST);
                return;
            }

            request = (HttpRequest) msg;

            log.info("received HttpRequest: {} {}", request.getMethod(), request.getUri());

            final ObjectRequest r;

            try {
                r = new ObjectRequest(request);
            } catch (ExceptionInInitializerError e) {
                log.warn("unable to parse the request into a command: {}", request.getUri());
                return;
            }

            objectRequest = r;
            httpRequest = request;

            if (HttpMethod.PUT.equals(request.getMethod())) {
                if (objectRequest.isNotDirectory()) // make dir request
                    setupProxyToPUT(ctx, objectRequest);

                requestState = RequestState.EXPECT_CONTENT_OR_LAST;
            } else {
                requestState = RequestState.EXPECT_LAST;
                ctx.read();
            }

            return;
        }

        if (objectRequest == null) {
            String uri = request == null ? "" : request.getUri();
            log.warn("not expecting an empty objectRequest. parse error maybe: {}", uri);
            ByteBuf body = request == null || request.getMethod().equals(HttpMethod.HEAD) ? null :
                    errorBody("ResourceNotFoundError", uri + " does not exist");
            sendError(ctx, HttpResponseStatus.NOT_FOUND, body);
            return;
        }

        if (msg instanceof HttpContent) {

            if (requestState != RequestState.EXPECT_LAST && requestState != RequestState.EXPECT_CONTENT_OR_LAST) {
                log.warn("not expecting LAST or CONTENT, requestState: {}", requestState);
                sendError(ctx, HttpResponseStatus.NOT_EXTENDED);
                return;
            }

            final boolean last = msg instanceof LastHttpContent;
            final boolean emptyLast = last && msg == LastHttpContent.EMPTY_LAST_CONTENT;

            if (proxyMode && !emptyLast) // todo: the emptyLast was added for mln
                ctx.fireChannelRead(msg);

            // example of reading only if at the end
            if (last) {

                log.debug("received LastHttpContent: {}", msg);
                requestState = RequestState.EXPECT_REQUEST;

                final HttpRequest request = httpRequest;

                if (HttpMethod.HEAD.equals(request.getMethod())) {
                    handleHEAD(ctx, objectRequest);
                    return;
                }

                if (HttpMethod.GET.equals(request.getMethod())) {
                    handleGET(ctx, objectRequest);
                    return;
                }

                if (HttpMethod.DELETE.equals(request.getMethod())) {
                    handleDELETE(ctx, objectRequest);
                    return;
                }


                if (HttpMethod.PUT.equals(request.getMethod())) {
                    if (proxyMode)
                        log.info("finished file upload: {}", objectRequest);
                    else
                        handlePUTmkdir(ctx, objectRequest);
                    return;
                }

                log.warn("unknown request: {}", request);
                sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            }

            return;
        }

        log.warn("unexpected msg type: {}", msg);
        sendError(ctx, HttpResponseStatus.BAD_REQUEST);
    }

    private void handleGET(final ChannelHandlerContext ctx, final ObjectRequest objectRequest) throws Exception {

        log.info("handleGET req: {}", httpRequest);

        mds.list(ctx.executor(), objectRequest).addListener(
                new GenericFutureListener<Future<ObjectResponse>>() {
                    @Override
                    public void operationComplete(Future<ObjectResponse> future) throws Exception {
                        if (!future.isSuccess()) {
                            log.error("future failed for handleGET: {}", objectRequest, future.cause());
                            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                            return;
                        }

                        ObjectResponse objectResponse = future.getNow();

                        log.info("result of mds.operate: {} -> {}", objectRequest, objectResponse);

                        if (objectResponse instanceof ObjectResponse.EmptyResponse) {
                            log.info("nothing found at: {}", objectRequest);
                            sendError(ctx, HttpResponseStatus.NOT_FOUND);
                            return;

                        }

                        if (objectResponse instanceof ObjectResponse.SingleObject) {
                            log.info("GET is for a file. proxy for downloading: {}", objectResponse);
                            setupProxyToGET(ctx, ((ObjectResponse.SingleObject) objectResponse).fullObjectName);
                            return;

                        }

                        if (!(objectResponse instanceof ObjectResponse.MultipleObjects)) {
                            log.info("multipleObjects.objects is not MultipleObjects: {}", objectResponse);
                            sendError(ctx, HttpResponseStatus.NOT_FOUND);
                            return;
                        }

                        log.info("returning result of mds.list: {}", objectResponse.json());
                        ctx.writeAndFlush(objectResponse.encode(), ctx.voidPromise());
                    }
                }
        );
    }

    private void handleDELETE(final ChannelHandlerContext ctx, final ObjectRequest request) throws IOException {
        log.info("handleDELETE req: {}", httpRequest);

        mds.delete(ctx.executor(), request).addListener(
                new GenericFutureListener<Future<ObjectResponse>>() {
                    @Override
                    public void operationComplete(Future<ObjectResponse> future) throws Exception {
                        if (future.isSuccess()) {
                            ObjectResponse response = future.get();
                            if (response instanceof ObjectResponse.Failure) {
                                log.warn("error on handleDELETE: {} -> {}", request, response);
                                sendError(ctx, response.encode());
                            } else {
                                log.info("successful handleDELETE req: {}, rsp: {}", request, response);
                                ctx.writeAndFlush(response.encode());
                            }
                        } else {
                            log.warn("error on handleDELETE: {}", request, future.cause());
                            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
                        }
                    }
                }
        );

    }

    private void handleHEAD(final ChannelHandlerContext ctx, final ObjectRequest objectRequest) {

        log.info("handleHEAD uri: {} ", httpRequest.getUri());

        mds.head(ctx.executor(), objectRequest).addListener(
                new GenericFutureListener<Future<ObjectResponse>>() {
                    @Override
                    public void operationComplete(Future<ObjectResponse> future) throws Exception {
                        if (future.isSuccess()) {
                            ObjectResponse objectResponse = future.get();

                            log.info("handleHEAD {} -> {}", objectRequest, objectResponse);

                            // todo: use .encode()
                            if (objectResponse instanceof ObjectResponse.SingleObject) {
                                ObjectResponse.SingleObject singleObject = (ObjectResponse.SingleObject) objectResponse;

                                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.NO_CONTENT);

                                if (singleObject.fullObjectName.attributes.dir) {
                                    response.headers().set(CONTENT_TYPE, "application/x-json-stream; type=directory");
                                } else {
                                    response.headers().set(CONTENT_MD5, singleObject.fullObjectName.attributes.md5);
                                }

                                ctx.writeAndFlush(response).addListener(
                                        new GenericFutureListener<ChannelFuture>() {

                                            @Override
                                            public void operationComplete(ChannelFuture future) throws Exception {
                                                if (future.isSuccess()) {
                                                    log.info("write success");
                                                } else {
                                                    log.info("write failed: {}", future.cause());
                                                }

                                            }
                                        }
                                );

                                return;
                            }

                            log.warn("error on handleHEAD: {}", objectRequest, future.cause());
                            sendError(ctx, ERROR_BAD_REQUEST);
                        }
                    }
                }
        );

    }

    private void handlePUTmkdir(final ChannelHandlerContext ctx, ObjectRequest request) throws Exception {
        if (request.isNotDirectory()) {
            log.warn("request is not mkdir: {}", request);
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }

        log.info("this is a mkdir: {}", request);
        ExtendedObjectAttributes eoa = new ExtendedObjectAttributes(true, 0, "", uuid.next(), 1, System.currentTimeMillis());
        final FullObjectRequest fullObjectRequest = new FullObjectRequest(request, eoa);
        mds.put(ctx.executor(), fullObjectRequest).addListener(new GenericFutureListener<Future<ObjectResponse>>() {
            @Override
            public void operationComplete(Future<ObjectResponse> future) throws Exception {
                if (future.isSuccess()) {
                    ObjectResponse response = future.getNow();
                    log.info("successful mkdir: {}", response);
                    ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT), ctx.voidPromise());
                    ctx.channel().close();
                } else {
                    log.warn("no success on mkdir: {}", future.cause());
                    sendError(ctx, HttpResponseStatus.BAD_REQUEST);
                }
            }
        });
    }

    private void setupProxyToPUT(final ChannelHandlerContext ctx, final ObjectRequest objectRequest) throws Exception {

        final List<ServiceInstance<NodeType>> st;
        final Optional<FullObjectName> fon;

        if(objectRequest.isLink()) {
            fon = findByName(objectRequest.getLinkLocation());

            if(fon.isPresent()) {
                st = findAllStorageInstanceUp(mds.storedNodes(fon.get().attributes.etag));
                log.info("LINK proxy storage node: {}, for {} -> {}", st, objectRequest, objectRequest.getLinkLocation());
            } else {
                st = Collections.emptyList();
            }
        } else {
            fon = Optional.absent();

            st = ns.suggestStorage(objectRequest.getDurability());
            log.info("PUT proxy storage node: {}, download mode: {}, durability: {}", st, false, objectRequest.getDurability());
        }

        if(st.isEmpty()) throw new Exception("not found instances for PUT: " + objectRequest);

        HttpProxyFrontendHandler proxy = new HttpProxyFrontendHandler(st, mds, httpRequest, ctx, false, fon);

        ctx.pipeline().addLast("proxy", proxy);

        proxyMode = true;

        //ctx.read(); // commented to mln

    }

    private Optional<FullObjectName> findByName(String name) {
        Optional<ObjectName> optOn = ObjectName.of(name);
        if(!optOn.isPresent()) return Optional.absent();
        return mds.selectByName(optOn.get());
    }

    private Optional<ServiceInstance<NodeType>> findStorageInstanceUp(List<String> ids) throws Exception {
        for(String id : ids) {
            ServiceInstance<NodeType> si = ns.discover(NodeType.ODS, id);
            if(si != null) return Optional.of(si);
        }
        return Optional.absent();
    }

    private List<ServiceInstance<NodeType>> findAllStorageInstanceUp(List<String> ids) throws Exception {
        List<ServiceInstance<NodeType>> s = new ArrayList<ServiceInstance<NodeType>>(ids.size());
        for(String id : ids) {
            ServiceInstance<NodeType> si = ns.discover(NodeType.ODS, id);
            if(si != null) s.add(si);
        }
        return s;
    }

    private void setupProxyToGET(final ChannelHandlerContext ctx, final FullObjectName fon) throws Exception {

        log.info("proxy GET storage node: download mode: {}", true);

        final Future<List<String>> futureStorageNodes = mds.storedNodes(ctx.executor(), fon.attributes.etag);

        futureStorageNodes.addListener(
                new GenericFutureListener<Future<List<String>>>() {
                    @Override
                    public void operationComplete(Future<List<String>> future) throws Exception {
                        if (future.isSuccess()) {
                            final List<String> st = future.getNow();

                            Optional<ServiceInstance<NodeType>> si = findStorageInstanceUp(st);

                            if(!si.isPresent()) {
                                log.error("failed to find running node, req: {}", objectRequest);
                                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                                return;
                            }

                            List<ServiceInstance<NodeType>> l = Lists.newArrayList(si.get());
                            log.info("proxy storage node: {}, download mode: {}, durability: {}", st, true, objectRequest.getDurability());
                            HttpProxyFrontendHandler proxy = new HttpProxyFrontendHandler(l, mds, httpRequest, ctx, true, Optional.<FullObjectName>absent());

                            ctx.pipeline().remove("encoder");

                            ctx.pipeline().addLast("proxy", proxy);

                            proxyMode = true;

                            ctx.read(); // todo: can ve removed?

                        } else {
                            log.error("failed to fetch futureStorageNodes, req: {}", objectRequest, future.cause());
                            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                        }
                    }
                }
        );

    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        sendError(ctx, new DefaultFullHttpResponse(HTTP_1_1, status));
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, ByteBuf body) {
        final FullHttpResponse response = body == null ?
                new DefaultFullHttpResponse(HTTP_1_1, status) :
                new DefaultFullHttpResponse(HTTP_1_1, status, body);
        sendError(ctx, response);
    }

    private void sendError(ChannelHandlerContext ctx, FullHttpResponse response) {
        log.info("sending error: {}", response);
        response.headers().set(CONTENT_TYPE, "application/json");
        response.headers().set(CONNECTION, "close");
        ctx.writeAndFlush(response);
        ctx.channel().close();
    }


    private static ByteBuf errorBody(String code, String message) {
        final String json = "{\"code\":\"" + code + "\", \"message\":\"" + message + "\"}";
        return Unpooled.copiedBuffer(json, CharsetUtil.UTF_8);
    }

}