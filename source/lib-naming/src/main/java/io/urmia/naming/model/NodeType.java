package io.urmia.naming.model;

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

public enum NodeType {

    MDS(8085, false),    // metadata server      Storage HTTP REST API
    ODS(8081, true),    // object-data server   File storage

    MDB(5432, true),    // metadata database    PSQL

    JDS(8086, false),    // job data server      Job REST API
    JRS(0, false),    // job run server       job runner

    AAS(8443, false);    // auth/authorization server    sub-auth processor + CORS

    public final int defaultPort;
    public final boolean uriRequired;

    private NodeType(int port, boolean uriRequired) {
        this.defaultPort = port;
        this.uriRequired = uriRequired;
    }

}
