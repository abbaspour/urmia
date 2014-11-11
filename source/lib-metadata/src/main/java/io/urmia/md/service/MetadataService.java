package io.urmia.md.service;

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
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.urmia.md.model.*;
import io.urmia.md.model.storage.Etag;
import io.urmia.md.model.storage.FullObjectName;
import io.urmia.md.model.storage.FullObjectRequest;
import io.urmia.md.model.storage.ObjectName;

import java.util.List;

public interface MetadataService {

    // better to call these REST service
    Future<ObjectResponse> delete(EventExecutor executor, ObjectRequest request);

    Future<ObjectResponse> head(EventExecutor executor, ObjectRequest request);

    Future<ObjectResponse> list(EventExecutor executor, ObjectRequest request);

    Future<ObjectResponse> put(EventExecutor executor, FullObjectRequest request);

    // these (v) or those (^) don't belong here.
    Future<List<String>> storedNodes(EventExecutor executor, String etag);

    Future<Void> flagStored(EventExecutor executor, String location, Etag etag);

    // sync (for mln. remove them)
    Optional<FullObjectName> selectByName(ObjectName on);

    List<String> storedNodes(String etag);

    List<String> storedNodes(final ObjectName on);

}
