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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class JdbcUtil {
    public static void tryClose(Connection c) {
        if (c == null) return;
        try {
            if (c.isClosed()) return;
            c.close();
        } catch (SQLException ignored) {
        }
    }

    public static void tryClose(Statement s) {
        if (s == null) return;
        try {
            if (s.isClosed()) return;
            s.close();
        } catch (SQLException ignored) {
        }
    }

    public static void tryClose(ResultSet r) {
        if (r == null) return;
        try {
            if (r.isClosed()) return;
            r.close();
        } catch (SQLException ignored) {
        }
    }
}
