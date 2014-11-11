package io.urmia.md.model.job;

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

import io.netty.handler.codec.http.FullHttpRequest;
import io.urmia.md.model.ObjectRequest;
import io.urmia.md.model.storage.ObjectName;

public abstract class JobRequest extends ObjectRequest {

    public final String uri;
    protected String id;

    protected JobRequest(ObjectRequest objectRequest, String uri) throws ExceptionInInitializerError {
        super(objectRequest);
        this.uri = uri;
    }

    protected JobRequest(FullHttpRequest fullHttpRequest) throws ExceptionInInitializerError {
        super(fullHttpRequest);

        if(objectName.ns != ObjectName.Namespace.JOBS)
            throw new IllegalArgumentException("namespace is not job: " + objectName);

        uri = fullHttpRequest.getUri();
    }

    public final String getId() {
        return id;
    }

    private static final String KEYWORD = "/jobs/";

    //jobs/506b260b-3e97-4f23-b175-66154889e8ad/live/in
    protected static String getId(String uri) {
        int start = uri.lastIndexOf(KEYWORD) + KEYWORD.length();
        int end = uri.indexOf('/', start);
        return uri.substring(start, end);
    }

}
