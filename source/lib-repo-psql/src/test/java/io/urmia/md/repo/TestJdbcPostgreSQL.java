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

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Properties;

public class TestJdbcPostgreSQL {

    private static final Logger log = LoggerFactory.getLogger(TestJdbcPostgreSQL.class);

    public static void main(String[] args) throws SQLException {
        //Connection conn = direct();
        Connection conn = pooled();

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("select * from event_public");

        while(rs.next())
            log.info("{} -> {}", rs.getLong("id"), rs.getString("summary"));

        rs.close();
        st.close();
        conn.close();

    }

    private static Connection pooled() throws SQLException {
        // setup the connection pool
        BoneCPConfig config = new BoneCPConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost/taghvim"); // jdbc url specific to your database, eg jdbc:mysql://127.0.0.1/yourdb
        config.setUsername("postgres");
        config.setPassword("");
        config.setMinConnectionsPerPartition(5);
        config.setMaxConnectionsPerPartition(10);
        config.setPartitionCount(1);

        BoneCP connectionPool = new BoneCP(config); // setup the connection pool

        return connectionPool.getConnection(); // fetch a connection

    }

    private static Connection direct() throws SQLException {
        String url = "jdbc:postgresql://localhost/taghvim";
        Properties props = new Properties();
        props.setProperty("user","postgres");
        props.setProperty("password","");
        //props.setProperty("ssl","false");
        Connection conn = DriverManager.getConnection(url, props);
        return conn;
    }
}
