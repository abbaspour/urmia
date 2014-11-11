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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;

public class HttpProxyBackendInitializer extends ChannelInitializer<SocketChannel> {

    private final ChannelHandlerContext inboundCtx;
    private final int index;
    private final boolean directWriteBack;

    public HttpProxyBackendInitializer(ChannelHandlerContext inboundCtx, int index, final boolean directWriteBack) {
        this.inboundCtx = inboundCtx;
        this.index = index;
        this.directWriteBack = directWriteBack;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();


        if(directWriteBack) {
            p.addLast("encoder", new HttpRequestEncoder());
            p.addLast(new DirectWriteBackHttpProxyBackendHandler(inboundCtx.channel()));
        } else {
            p.addLast("encoder", new HttpRequestEncoder());

            p.addLast("decoder", new HttpResponseDecoder());
            //p.addLast("aggregator", new HttpObjectAggregator(2048));
            p.addLast(new HttpProxyBackendHandler(inboundCtx, index));
        }

    }
}



