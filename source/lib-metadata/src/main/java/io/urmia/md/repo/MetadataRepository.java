package io.urmia.md.repo;

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
import io.urmia.md.exception.MetadataException;
import io.urmia.md.model.storage.Etag;
import io.urmia.md.model.storage.FullObjectName;
import io.urmia.md.model.storage.ObjectName;

import java.util.List;

public interface MetadataRepository {

    Optional<FullObjectName> selectByName(ObjectName on) throws MetadataException;

    void insert(FullObjectName fon) throws MetadataException;

    void insertStored(String location, Etag etag) throws MetadataException;

    void delete(ObjectName on) throws MetadataException;

    List<FullObjectName> listDir(ObjectName on, int limit) throws MetadataException;

    boolean deletable(ObjectName dir) throws MetadataException; // empty dir not roots or file

    List<String> findStorageNameByEtag(String etag) throws MetadataException;

    List<String> findStorageByName(ObjectName on) throws MetadataException;

    static final int TIMEOUT_READ = 10;
}
