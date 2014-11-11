package io.urmia.md.repo.psql;

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
import io.urmia.md.model.ObjectRequest;
import io.urmia.md.repo.util.JdbcPool;
import io.urmia.md.repo.MetadataRepository;
import io.urmia.md.exception.MetadataException;
import io.urmia.md.model.storage.ExtendedObjectAttributes;
import io.urmia.md.model.storage.FullObjectName;
import io.urmia.md.model.storage.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class PsqlMdRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(PsqlMetadataRepositoryImpl.class);

    public static void main(String[] args) throws SQLException, MetadataException {
        JdbcPool pool = new JdbcPool.BoneCPJdbcPool("jdbc:postgresql://localhost/udb01", "uadmin", "uadmin", 1, 3);
        MetadataRepository reader = new PsqlMetadataRepositoryImpl(pool);

        // -- select
        Optional<ObjectName> on = ObjectName.of("/abbaspour/stor/sample.file");
        Optional<FullObjectName> fon = reader.selectByName(on.get());
        log.info("found: {}", fon);

        // -- insert
        Optional<ObjectName> on2 = ObjectName.of("/abbaspour/stor/some/other6.file");
        ExtendedObjectAttributes eoa = new ExtendedObjectAttributes(false, 1233, "SAuQ2FmZYFqgb4FA6AXWnw==", "--etag--",
                1, System.currentTimeMillis());
        FullObjectName fon2 = new FullObjectName(on2.get(), eoa);
        log.info("insert: {}", fon2);
        reader.insert(fon2);

        // -- delete
        //reader.delete(fon2);
        log.info("delete: {}", fon2);

        // -- list
        String path = "/abbaspour/stor/";
        Optional<ObjectName> root = ObjectName.of(path);
        List<FullObjectName> list = reader.listDir(root.get(), ObjectRequest.DEFAULT_LIMIT);

        log.info("list {} -> {}", path, list);

        // -- list2
        String path2 = "/abbaspour/stor/sample.file";
        Optional<ObjectName> root2 = ObjectName.of(path2);
        List<FullObjectName> list2 = reader.listDir(root2.get(), ObjectRequest.DEFAULT_LIMIT);

        log.info("list {} -> {}", path2, list2);
    }
}
