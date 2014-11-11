package io.urmia.md.model;

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

import com.google.common.base.Joiner;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import io.urmia.md.model.storage.FullObjectName;

import java.util.List;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.urmia.util.StringUtils.isBlank;

public abstract class ObjectResponse {

    public String json() {
        return "";
    }

    public int resultSetSize() {
        return 0;
    }

    public String contentType() {
        return "application/x-json-stream; type=directory";
    }

    public final String toString() {
        return json();
    }

    public static final String RESULT_SET_SIZE = "result-set-size";

    // todo: need review
    public FullHttpResponse encode() {

        final HttpResponseStatus status;
        final ByteBuf content;

        if(this instanceof Failure) {
            status = ((Failure) this).type.status;
            content = Unpooled.copiedBuffer(json(), CharsetUtil.UTF_8);

        } else {
            final String json = this.json();

            if(isBlank(json)) {
                status = HttpResponseStatus.NO_CONTENT;
                content = Unpooled.EMPTY_BUFFER;
            } else {
                status = HttpResponseStatus.OK;
                content = Unpooled.copiedBuffer(json, CharsetUtil.UTF_8);
            }
        }

        FullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_1_1, status, content);

        httpResponse.headers().set(HttpHeaders.Names.CONTENT_TYPE, this.contentType());
        //httpResponse.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.json().length());
        httpResponse.headers().set("transfer-encoding", "chunked"); // this is a must for list response

        if(this.resultSetSize() > 0)
            httpResponse.headers().set(RESULT_SET_SIZE, this.resultSetSize());

        return httpResponse;

    }

    public static class EmptyResponse extends ObjectResponse {

        @Override
        public String json() {
            return "";
        }

        public HttpResponseStatus status() {
            return HttpResponseStatus.NO_CONTENT;
        }

    }

    public static class SingleObject extends ObjectResponse {

        public final FullObjectName fullObjectName;

        public SingleObject(FullObjectName fullObjectName) {
            this.fullObjectName = fullObjectName;
        }

        @Override
        public String json() {
            return fullObjectName.toString();
        }
    }

    private static final Joiner LINE_JOINER = Joiner.on('\n').skipNulls();

    public static class MultipleObjects extends ObjectResponse {

        public final List<FullObjectName> objects;

        public MultipleObjects(List<FullObjectName> objects) {
            this.objects = objects;
        }

        @Override
        public String json() {
            return LINE_JOINER.join(objects);
        }

        @Override
        public int resultSetSize() {
            return objects.size();
        }
    }

    public static enum FailureType {
        NotFound("NotFoundError", HttpResponseStatus.NOT_FOUND),
        Timeout("TimeoutError", HttpResponseStatus.GATEWAY_TIMEOUT),
        Internal("InternalError", HttpResponseStatus.BAD_GATEWAY),
        NotEmpty("DirectoryNotEmpty", HttpResponseStatus.BAD_REQUEST);

        public final String code;
        public final HttpResponseStatus status;

        FailureType(String code, HttpResponseStatus status) {
            this.code = code;
            this.status = status;
        }
    }

    public static class Failure extends ObjectResponse {

        public final FailureType type;
        public final String message;

        public Failure(FailureType type, String message) {
            this.type = type;
            this.message = message;
        }

        @Override
        public String json() {
            return "{" +
                    "\"code\":\"" + type.code + '\"' +
                    ", \"message\":\"" + message + '\"' +
                    '}';
        }

    }

}
