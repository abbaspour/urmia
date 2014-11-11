package io.urmia.job.codec;

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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.urmia.md.model.job.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ChannelHandler.Sharable
public class JobDecoder extends MessageToMessageDecoder<HttpObject> {

    private Logger log = LoggerFactory.getLogger(JobDecoder.class);

    public void decode(final ChannelHandlerContext ctx, HttpObject msg, List out){

        log.debug("decoding: {} on ctx: {}", msg, ctx);

        if(!(msg instanceof FullHttpRequest)) {
            log.warn("input to JobDecoder is not FullHttpReq: {}", msg);
            return;
        }

        final FullHttpRequest httpReq = (FullHttpRequest) msg;

        final JobRequest jobRequest = decode(httpReq);

        log.info("returning {} -> {}", httpReq.getUri(), jobRequest);

        //noinspection unchecked
        out.add(jobRequest);

        httpReq.retain();   // need the content
    }

    JobRequest decode(FullHttpRequest fullHttpRequest) {

        final String uri = fullHttpRequest.getUri();
        final HttpMethod method = fullHttpRequest.getMethod();

        if(HttpMethod.GET.equals(method)) {
            if(isGet(uri))
                return new JobGetRequest(fullHttpRequest);

            return new JobQueryRequest(fullHttpRequest);
        }

        if(HttpMethod.POST.equals(method)) {
            if (isInput(uri))
                return new JobInputRequest(fullHttpRequest);

            if (isCancel(uri))
                return new JobCancelRequest(fullHttpRequest);

            return new JobCreateRequest(fullHttpRequest);
        }

        throw new IllegalArgumentException("unknown job request: " + method + " " + uri);
    }

    private boolean isInput(String uri) {
        return uri.endsWith("/live/in") || isEOT(uri);
    }

    private boolean isEOT(String uri) {
        return uri.endsWith("/live/in/end");
    }

    private boolean isCancel(String uri) {
        return uri.endsWith("/live/cancel");
    }
    private boolean isGet(String uri) {
        return uri.contains("/stor/");
    }

}
