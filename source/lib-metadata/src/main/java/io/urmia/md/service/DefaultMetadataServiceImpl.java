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
import io.urmia.md.repo.MetadataRepository;
import io.urmia.md.exception.MetadataException;
import io.urmia.md.model.*;
import io.urmia.md.model.storage.Etag;
import io.urmia.md.model.storage.FullObjectName;
import io.urmia.md.model.storage.FullObjectRequest;
import io.urmia.md.model.storage.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;

import static com.google.common.base.Optional.absent;

public class DefaultMetadataServiceImpl implements MetadataService {

    private static final Logger log = LoggerFactory.getLogger(DefaultMetadataServiceImpl.class);

    private final MetadataRepository repository;

    public DefaultMetadataServiceImpl(MetadataRepository r) {
        this.repository = r;
    }

    @Override
    public Future<ObjectResponse> delete(EventExecutor executor, final ObjectRequest request) {
        return executor.submit(new Callable<ObjectResponse>() {
            @Override
            public ObjectResponse call() throws Exception {
                return syncDelete(request);
            }
        });
    }

    private ObjectResponse syncDelete(ObjectRequest request) {
        try {
            if (!repository.deletable(request.objectName)) // todo: make this two nested async calls inside delete
                return new ObjectResponse.Failure(ObjectResponse.FailureType.NotEmpty, request.objectName + " is not empty");
            repository.delete(request.objectName);
            return new ObjectResponse.EmptyResponse();
        } catch (MetadataException e) {
            log.error("MetadataException", e);
            return new ObjectResponse.Failure(ObjectResponse.FailureType.Internal, "invalid operation: " + request);
        }
    }

    @Override
    public Future<ObjectResponse> head(EventExecutor executor, final ObjectRequest request) {
        return executor.submit(new Callable<ObjectResponse>() {
            @Override
            public ObjectResponse call() throws Exception {
                return syncHead(request);
            }
        });
    }


    private ObjectResponse syncHead(ObjectRequest request) {

        log.info("listing: {}", request);

        try {
            Optional<FullObjectName> fon = repository.selectByName(request.objectName);
            if (!fon.isPresent())
                return new ObjectResponse.EmptyResponse();

            return new ObjectResponse.SingleObject(fon.get());

        } catch (MetadataException e) {
            log.error("MetadataException", e);
            return new ObjectResponse.Failure(ObjectResponse.FailureType.Internal, e.getMessage());
        }

    }

    @Override
    public Optional<FullObjectName> selectByName(ObjectName on) {

        try {
            return repository.selectByName(on);
        } catch (MetadataException e) {
            log.error("MetadataException", e);
            return absent();
        }
    }

    @Override
    public Future<ObjectResponse> list(EventExecutor executor, final ObjectRequest request) {
        return executor.submit(new Callable<ObjectResponse>() {
            @Override
            public ObjectResponse call() throws Exception {
                return syncList(request);
            }
        });
    }

    private ObjectResponse syncList(ObjectRequest request) {

        log.info("listing: {}", request);

        try {
            Optional<FullObjectName> fon = repository.selectByName(request.objectName);
            if (!fon.isPresent())
                return new ObjectResponse.EmptyResponse();

            if (!fon.get().attributes.dir)
                return new ObjectResponse.SingleObject(fon.get());

            List<FullObjectName> result = repository.listDir(request.objectName, request.getLimit());
            return new ObjectResponse.MultipleObjects(result);
        } catch (MetadataException e) {
            log.error("MetadataException", e);
            return new ObjectResponse.MultipleObjects(Collections.<FullObjectName>emptyList());
        }

    }


    @Override
    public Future<ObjectResponse> put(EventExecutor executor, final FullObjectRequest request) {
        return executor.submit(new Callable<ObjectResponse>() {
            @Override
            public ObjectResponse call() throws Exception {
                return syncPut(request);
            }
        });
    }


    private ObjectResponse syncPut(FullObjectRequest request) {
        try {
            repository.insert(request.fullObjectName);
            return new ObjectResponse.EmptyResponse();
        } catch (MetadataException e) {
            log.error("MetadataException", e);
            return new ObjectResponse.Failure(ObjectResponse.FailureType.Internal, "invalid operation: " + request);
        }

    }

    @Override
    public Future<List<String>> storedNodes(final EventExecutor executor, final String etag) {
        return executor.submit(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                return repository.findStorageNameByEtag(etag);
            }
        });
    }

    @Override
    public List<String> storedNodes(final ObjectName on) {
        try {
            return repository.findStorageByName(on);
        } catch (MetadataException e) {
            log.warn("error on find storedNodes for on: {}, err: {}", on, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> storedNodes(String etag) {
        try {
            return repository.findStorageNameByEtag(etag);
        } catch (MetadataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Future<Void> flagStored(EventExecutor executor, final String location, final Etag etag) {
        return executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                repository.insertStored(location, etag);
                return null;
            }
        });
    }

}
