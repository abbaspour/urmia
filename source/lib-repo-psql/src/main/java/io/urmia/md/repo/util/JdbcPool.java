package io.urmia.md.repo.util;

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

import com.google.common.util.concurrent.ListenableFuture;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

import java.sql.Connection;
import java.sql.SQLException;

public interface JdbcPool {

    Connection connection(boolean readOnly) throws SQLException;

    public static class BoneCPJdbcPool implements JdbcPool {

        private final BoneCP pool;

        public BoneCPJdbcPool(String url, String user, String pass, int min, int max) throws SQLException {
            BoneCPConfig config = new BoneCPConfig();
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(pass);
            config.setMinConnectionsPerPartition(min);
            config.setMaxConnectionsPerPartition(max);
            config.setPartitionCount(1);
            config.setLogStatementsEnabled(true);

            pool = new BoneCP(config); // setup the connection pool
        }

        public BoneCPJdbcPool(BoneCPConfig config) throws SQLException {
            pool = new BoneCP(config);
        }

        @Override
        public Connection connection(boolean readOnly) throws SQLException {
            Connection c = pool.getConnection();
            c.setReadOnly(readOnly);
            return c;
        }

        @SuppressWarnings("UnusedDeclaration")
        public ListenableFuture<Connection> asyncConnection() throws SQLException {
            return pool.getAsyncConnection();
        }

    }
}
