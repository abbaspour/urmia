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

import com.google.common.base.Optional;
import io.netty.handler.codec.http.FullHttpRequest;
import io.urmia.md.model.ObjectRequest;
import io.urmia.md.model.storage.ObjectName;

/**
 *
 * get requests are of this form
 *
 * /user/jobs/[job-id]/stor/user/stor/[output-name]
 *
 * /abbaspour/jobs/d081adc6-c193-48ef-a99a-85c4e5934cfb/stor + /abbaspour/stor/treasure_island.txt.0.c8598f52-3c14-451a-9b2f-354579a2d5e8(-reduce)
 *
 */
public class JobGetRequest extends JobRequest {

    private final String storageId;

    public JobGetRequest(FullHttpRequest fullHttpRequest) throws ExceptionInInitializerError {
        super(fromJobGetHttpRequest(fullHttpRequest.getUri()), fullHttpRequest.getUri());
        this.id = getId(uri);
        this.storageId = getStorageNodeId(uri);
    }

    public String getStorageNodeId() {
        return storageId;
    }

    public static String getStorageNodeId(String uri) {
        int slash = uri.lastIndexOf('/');
        int dot = uri.lastIndexOf('.');

        int index = Math.max(slash, dot);

        String n = uri.substring(index + 1);
        int r = n.indexOf("-reduce");

        return r == -1 ? n : n.substring(0, r);
    }

    @Override
    public String toString() {
        return "JobGetRequest{" +
                "on='" + objectName + '\'' +
                '}';
    }

    public static ObjectRequest fromJobGetHttpRequest(String uri) {
        int loc = uri.indexOf("/stor/");
        if (loc <= 0) throw new IllegalArgumentException("invalid job get uri: " + uri);

        String path = uri.substring(loc + 5);
        Optional<ObjectName> on = ObjectName.of(path);
        if(! on.isPresent()) throw new IllegalArgumentException("invalid job get path: " + path);

        return new ObjectRequest(on.get());
    }
}
