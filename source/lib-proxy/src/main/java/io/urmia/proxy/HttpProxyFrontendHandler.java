package io.urmia.proxy;

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
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.urmia.dd.DirectDigest;
import io.urmia.md.model.*;
import io.urmia.md.model.storage.Etag;
import io.urmia.md.model.storage.ExtendedObjectAttributes;
import io.urmia.md.model.storage.FullObjectName;
import io.urmia.md.model.storage.FullObjectRequest;
import io.urmia.md.service.MetadataService;
import io.urmia.naming.model.NodeType;
import io.urmia.naming.service.RandomUuidImpl;
import io.urmia.naming.service.Uuid;
import org.apache.curator.x.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static io.urmia.proxy.ProxyUserEvent.Type.*;

public class HttpProxyFrontendHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static final Logger log = LoggerFactory.getLogger(HttpProxyFrontendHandler.class);

    private final List<ServiceInstance<NodeType>> storageNodes;
    private final int outboundCount;

    private final BitSet completedSet;
    private final BitSet writeSet;
    private final BitSet continueSet;

    private final List<Channel> outboundChannels; // has to be volatile cuz opened in channelActive with ctx

    private final MetadataService mds;

    private final long md5ctx;
    private final HttpRequest initHttpRequest;

    private final ObjectRequest objectRequest;

    private final AtomicLong receivedSize = new AtomicLong();

    private final AtomicLong[] writtenSizes;

    private final boolean directWriteBack;
    private final Etag etag;

    private static final Uuid uuid = new RandomUuidImpl();

    private final Optional<FullObjectName> fon;

    public HttpProxyFrontendHandler(List<ServiceInstance<NodeType>> storageNodes, MetadataService mds, HttpRequest initHttpRequest,
                                    ChannelHandlerContext initCtx, final boolean directWriteBack, Optional<FullObjectName> fon) {

        this.storageNodes = storageNodes;
        this.outboundCount = storageNodes.size();

        this.directWriteBack = directWriteBack;

        this.writeSet = new BitSet(outboundCount);
        writeSet.clear();

        this.continueSet = new BitSet(outboundCount);
        continueSet.clear();

        this.completedSet = new BitSet(outboundCount);
        completedSet.clear();

        outboundChannels = new ArrayList<Channel>(outboundCount);

        this.mds = mds;
        this.md5ctx = DirectDigest.md5_init();
        log.info("md5_init: {}", md5ctx);
        this.initHttpRequest = initHttpRequest;
        this.objectRequest = new ObjectRequest(initHttpRequest);

        etag = new Etag(uuid.next());
        log.info("etag: {}", etag);

        writtenSizes = new AtomicLong[outboundCount];
        this.fon = fon;

        int i = 0;
        for(ServiceInstance<NodeType> storageNode : storageNodes) {
            log.info("opening outbound to: {}", storageNode);
            outboundChannels.add(openOutboundChannel(initCtx, storageNode.getAddress(), storageNode.getPort(), i));
            writtenSizes[i] = new AtomicLong();
            i++;
        }
    }

    private void onSuccessfulWrite(final ChannelHandlerContext ctx, int index) {
        writeSet.set(index);
        if(writeSet.cardinality() != outboundCount) return;
        ctx.channel().read();
    }


    private Channel openOutboundChannel(final ChannelHandlerContext ctx,
                                        String remoteHost, int remotePort, final int index) {

        log.info("proxy opening outbound channel to({}): {}:{}", index, remoteHost, remotePort);

        Bootstrap b = new Bootstrap();
        b.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new HttpProxyBackendInitializer(ctx, index, directWriteBack))
                .option(ChannelOption.AUTO_READ, false);

        ChannelFuture f = b.connect(remoteHost, remotePort);
        Channel outboundChannel = f.channel();
        f.addListener(
                new GenericFutureListener<ChannelFuture>() {
                    @Override
                    public void operationComplete(ChannelFuture futureC) throws Exception {
                        if (futureC.isSuccess()) {
                            futureC.channel().writeAndFlush(initHttpRequest).addListener(new GenericFutureListener<ChannelFuture>() {
                                @Override
                                public void operationComplete(ChannelFuture futureW) throws Exception {
                                    if(futureW.isSuccess())
                                        onSuccessfulWrite(ctx, index);
                                    else {
                                        log.info("unable to write http request: {}", futureW.cause());
                                        ctx.fireUserEventTriggered(new ProxyUserEvent(OUTBOUND_ERROR, index));
                                    }
                                }
                            });
                        } else {
                            ctx.fireUserEventTriggered(new ProxyUserEvent(OUTBOUND_ERROR, index));
                        }
                    }
                }
        );

        return outboundChannel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {

        //log.info("frontend channelRead0: {}", msg);

        if (!(msg instanceof HttpContent)) {
            log.warn("not supposed to receive msg of type: " + msg);
            return;
        }

        HttpContent content = (HttpContent) msg;
        digestUpdate(content);

        content.retain(outboundCount); // will be write by outboundCount

        writeToAllOutbounds(ctx, content);

    }

    private void digestUpdate(HttpContent httpContent) {
        ByteBuf content = httpContent.content();
        receivedSize.addAndGet(content.readableBytes());

        for (ByteBuffer bb : content.nioBuffers()) {
            if (bb.isDirect())
                DirectDigest.md5_update(md5ctx, bb);
            else
                DirectDigest.md5_update(md5ctx, bb.array());
        }
    }

    private String digestFinal() {
        final byte[] md5 = DirectDigest.md5_final(md5ctx);
        return new String(Base64.encode(Unpooled.wrappedBuffer(md5)).array()).trim();
    }

    private void writeToAllOutbounds(final ChannelHandlerContext ctx, final HttpContent msg) {

        writeSet.clear();

        final int contentSize = msg.content().writableBytes();

        int i = 0;

        for(final Channel outboundChannel : outboundChannels) {

            final int chIdx = i++;

            outboundChannel.writeAndFlush(msg.duplicate()) // duplicate because different widx
                    .addListener(new GenericFutureListener<ChannelFuture>() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess()) {
                                writtenSizes[chIdx].addAndGet(contentSize);
                                onSuccessfulWrite(ctx, chIdx);
                            } else {
                                log.error("error to write to outbound", future.cause());
                                future.channel().close();
                            }

                        }
                    });
        }
    }

    private void onContinue(final ChannelHandlerContext ctx, int index) {
        log.info("onContinue: {}", index);
        continueSet.set(index);
        if(continueSet.cardinality() != outboundCount) return;
        log.info("all onContinue");
        ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
    }

    private void onComplete(final ChannelHandlerContext ctx, final int index) {
        log.info("onComplete: {}", index);

        completedSet.set(index);

        mds.flagStored(ctx.executor(), storageNodes.get(index).getId(), etag);

        if(completedSet.cardinality() != outboundCount) return;

        log.info("all onComplete");

        final String md5;
        final long size;

        if(fon.isPresent()) {
            md5 = fon.get().attributes.md5;
            size = fon.get().attributes.size;

            digestFinal();
        } else {
            // todo: for mln copy md5 and size from original object
            md5 = digestFinal();
            log.info("md5: ", md5);

            size = receivedSize.longValue();
            receivedSize.set(0);
            log.info("size: {}", size);
        }

        ExtendedObjectAttributes eoa = new ExtendedObjectAttributes(false, size, md5, etag.value,
                completedSet.cardinality(), System.currentTimeMillis());
        final FullObjectRequest req = new FullObjectRequest(objectRequest, eoa);

        log.info("mds.put: {}", req);

        mds.put(ctx.executor(), req).addListener(new GenericFutureListener<Future<ObjectResponse>>() {
            @Override
            public void operationComplete(Future<ObjectResponse> future) throws Exception {
                HttpResponseStatus status = future.isSuccess() ? HttpResponseStatus.OK : HttpResponseStatus.BAD_GATEWAY;

                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
                log.info("writing back to client: {}", response);
                ctx.writeAndFlush(response).addListener(new GenericFutureListener<ChannelFuture>(){
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        log.info("write back successful. closing channel");
                        ctx.channel().close();
                    }
                });
            }
        });
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, Object evtObject) throws Exception {
        log.info("received user event: {}", evtObject);

        if (!(evtObject instanceof ProxyUserEvent)) {
            log.error("evtObject in not of ProxyUserEvent type: {}", evtObject);
            closeOnFlush(ctx.channel());
            return;
        }

        ProxyUserEvent evt = (ProxyUserEvent) evtObject;

        switch (evt.type) {

            case OUTBOUND_ERROR:
                log.warn("outbound error: {}", evt);
                closeOnFlush(ctx.channel());
                return;

            case OUTBOUND_INACTIVE:
                log.info("outbound error: {}", evt);
                break;

            case OUTBOUND_CONTINUE:
                onContinue(ctx, evt.index);
                break;

            case OUTBOUND_COMPLETED: {
                onComplete(ctx, evt.index);
                break;
            }
            default:
                log.warn("unknown event: {}", evt);
                closeOnFlush(ctx.channel());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        log.info("proxy channelInactive: {}", ctx);

        for(Channel outboundChannel : outboundChannels)
            if (outboundChannel != null)
                closeOnFlush(outboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("exceptionCaught: {}", cause.getMessage());

        cause.printStackTrace();
        closeOnFlush(ctx.channel());
    }

    private static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            log.info("closeOnFlush active ch: {}", ch);
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        } else {
            log.info("closeOnFlush inactive ch: {}", ch);
            ch.closeFuture();
        }

    }

}
