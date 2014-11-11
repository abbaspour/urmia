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

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.urmia.proxy.ProxyUserEvent.Type.*;

public class HttpProxyBackendHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static final Logger log = LoggerFactory.getLogger(HttpProxyBackendHandler.class);

    private final ChannelHandlerContext inboundCtx;
    private final int index;

    public HttpProxyBackendHandler(ChannelHandlerContext inboundCtx, int index) {
        this.inboundCtx = inboundCtx;
        this.index = index;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("backend active: {}", ctx);
        ctx.read();
        ctx.write(Unpooled.EMPTY_BUFFER);
    }

    private volatile HttpResponse httpResponse;

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        //log.info("backend read. direct write back: {}. writing to inbound: {}", directWriteBack, msg);

        if(msg instanceof HttpResponse) {
            httpResponse = (HttpResponse) msg;
            log.info("backend http response: {}, status code: {}", httpResponse, httpResponse.getStatus().code());
            ctx.channel().read();
            return;
        }

        if(httpResponse == null) {
            log.error("httpResponse is null when received: {}", msg);
            inboundCtx.pipeline().fireUserEventTriggered(new ProxyUserEvent(OUTBOUND_ERROR, index));
            return;
        }

        if(!(msg instanceof LastHttpContent)) {
            log.error("input message is not supported: {}", msg);
            inboundCtx.pipeline().fireUserEventTriggered(new ProxyUserEvent(OUTBOUND_ERROR, index));
            return;
        }

        if(httpResponse.getStatus().equals(HttpResponseStatus.CONTINUE)) {
            log.info("continue from client.");
            ProxyUserEvent pevt = new ProxyUserEvent(OUTBOUND_CONTINUE, index);
            log.info("fire user event: {}", pevt);
            inboundCtx.pipeline().fireUserEventTriggered(pevt);
            ctx.read();

            return;
        }

        ProxyUserEvent pevt = new ProxyUserEvent(OUTBOUND_COMPLETED, index);
        log.info("fire user event: {}", pevt);
        inboundCtx.pipeline().fireUserEventTriggered(pevt);
        ctx.read();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("backend channelInactive: {}", ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn("exceptionCaught", cause);
        inboundCtx.pipeline().fireUserEventTriggered(new ProxyUserEvent(OUTBOUND_ERROR, index));
    }
}
