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

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.urmia.md.model.storage.ObjectName;
import io.urmia.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

public class ObjectRequest {

    private static final Splitter.MapSplitter HTTP_PARAM_SPLIT = Splitter.on('&').trimResults().withKeyValueSeparator('=');

    public final ObjectName objectName;

    private final boolean typeDirectory;
    private final boolean typeLink;
    private final int durability;
    private final String location;
    private final int limit;

    protected ObjectRequest(ObjectRequest o) {
        this.objectName = o.objectName;

        this.typeDirectory = o.typeDirectory;
        this.typeLink = o.typeLink;
        this.durability = o.durability;
        this.location = o.location;
        this.limit = o.limit;

    }

    public ObjectRequest(ObjectName on) throws ExceptionInInitializerError {
        this.objectName = on;
        this.typeDirectory = false;
        this.typeLink = false;
        this.durability = 1;
        this.location = null;
        this.limit = DEFAULT_LIMIT;
    }

    public ObjectRequest(HttpRequest httpRequest) throws ExceptionInInitializerError {

        final URI uri;
        try {
            uri = new URI(httpRequest.getUri());
        } catch (URISyntaxException e) {
            throw new ExceptionInInitializerError(e);
        }

        Optional<ObjectName> oon = ObjectName.of(uri);

        if (!oon.isPresent())
            throw new ExceptionInInitializerError("object name invalid: " + httpRequest.getUri());

        objectName = oon.get();

        HttpHeaders headers = httpRequest.headers();

        this.typeDirectory = isDirectory(headers);
        this.durability = getDurability(headers);
        this.typeLink = isLink(headers);
        this.location = getLinkLocation(headers);

        Map<String, String> params = StringUtils.isBlank(uri.getQuery()) ? Collections.<String, String>emptyMap() : HTTP_PARAM_SPLIT.split(uri.getQuery());
        this.limit = getLimit(params);
    }

    @Override
    public String toString() {
        return "ObjectRequest{" +
                "objectName=" + objectName +
                '}';
    }

    public static final int DEFAULT_DURABILITY = 2;

    public int getDurability() {
        return durability;
    }

    private static int getDurability(HttpHeaders headers) {

        final String ds = headers.get("x-durability-level");

        if (StringUtils.isBlank(ds))
            return DEFAULT_DURABILITY;

        try {
            return Integer.parseInt(ds);
        } catch (NumberFormatException nfe) {
            return DEFAULT_DURABILITY;
        }
    }

    public boolean isDirectory() {
        return typeDirectory;
    }

    public boolean isNotDirectory() {
        return !typeDirectory;
    }

    private boolean isDirectory(HttpHeaders headers) {
        String contentType = headers.get("content-type");
        return contentType != null && contentType.endsWith("; type=directory");
    }

    public boolean isLink() {
        return typeLink;
    }

    private static boolean isLink(HttpHeaders headers) {
        String contentType = headers.get("content-type");
        return contentType != null && contentType.endsWith("; type=link");
    }

    public String getLinkLocation() {
        return location;
    }

    private static String getLinkLocation(HttpHeaders headers) {
        return headers.get("location");
    }

    public static final int DEFAULT_LIMIT = 1024;
    public static final int MAX_LIMIT = DEFAULT_LIMIT * 10;

    public int getLimit() {
        return limit;
    }

    private static int getLimit(Map<String, String> params) {

        if (params == null || params.isEmpty()) return DEFAULT_LIMIT;
        String pLimit = params.get("limit");
        if (StringUtils.isBlank(pLimit)) return DEFAULT_LIMIT;

        int limit = DEFAULT_LIMIT;
        try {
            limit = Integer.parseInt(pLimit);
        } catch (NumberFormatException ignored) {
        }

        if (limit > MAX_LIMIT) return MAX_LIMIT;
        return limit;
    }
}
