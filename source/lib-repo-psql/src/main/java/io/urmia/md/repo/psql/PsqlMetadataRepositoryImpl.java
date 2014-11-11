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
import com.google.common.collect.ImmutableList;
import io.urmia.md.repo.util.JdbcPool;
import io.urmia.md.repo.MetadataRepository;
import io.urmia.md.exception.MetadataException;
import io.urmia.md.exception.MetadataTimeoutException;
import io.urmia.md.model.storage.Etag;
import io.urmia.md.model.storage.ExtendedObjectAttributes;
import io.urmia.md.model.storage.FullObjectName;
import io.urmia.md.model.storage.ObjectName;
import io.urmia.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.urmia.md.repo.util.JdbcUtil.*;

@SuppressWarnings("JpaQueryApiInspection")
public class PsqlMetadataRepositoryImpl implements MetadataRepository {

    private static final Logger log = LoggerFactory.getLogger(PsqlMetadataRepositoryImpl.class);

    private final JdbcPool pool;

    public PsqlMetadataRepositoryImpl(JdbcPool pool) {
        this.pool = pool;
    }

    @Override
    public Optional<FullObjectName> selectByName(ObjectName on) throws MetadataException {

        log.trace("findByName: {}", on);

        if(on.ns == ObjectName.Namespace.ROOT) // special kind of list, returning available namespaces
            return Optional.of(new FullObjectName(on, ROOT_EOA));

        Connection c = null;
        PreparedStatement s = null;
        ResultSet r = null;

        try {
             c = pool.connection(true);
             s = c.prepareStatement("SELECT type, size, md5, etag, durability, mtime FROM objects WHERE owner = ? AND ns = ?::namespace " +
                     "AND parent = ? AND name = ? AND deleted = FALSE LIMIT 1");

            s.setString(1, on.owner);
            s.setString(2, on.ns.path);
            s.setString(3, on.parent);
            s.setString(4, on.name);

            s.setQueryTimeout(TIMEOUT_READ);

            r = s.executeQuery();

            if (r.next())
                return Optional.fromNullable(mapResultSet(on, r));

            return Optional.absent();

        } catch (SQLTimeoutException e) {
            throw new MetadataTimeoutException("timeout fetching object: " + on, e);
        } catch (SQLException e) {
            throw new MetadataException("sql error fetching object: " + on, e);
        } finally {
            tryClose(r);
            tryClose(s);
            tryClose(c);
        }
    }

    //public static FileTime ROOT_MTIME = FileTime.fromMillis(System.currentTimeMillis()); // todo: fix this. use registration time
    public static ExtendedObjectAttributes ROOT_EOA = new ExtendedObjectAttributes(true, 0, "", "", 2, System.currentTimeMillis());

    private List<FullObjectName> listNamespaces(String owner) {

        ObjectName.Namespace[] namespaces = ObjectName.Namespace.list();
        List<FullObjectName> objectNames = new ArrayList<FullObjectName>(namespaces.length);

        for(ObjectName.Namespace ns : namespaces) {
            ObjectName on = new ObjectName(owner, ns, "/", ns.path);
            FullObjectName fon = new FullObjectName(on, ROOT_EOA);
            objectNames.add(fon);
        }

        return objectNames;
    }

    @Deprecated // duplicated with below
    private FullObjectName mapResultSet(ObjectName on, ResultSet r) throws SQLException {

        final long size = r.getLong("size");
        if (r.wasNull()) return null;

        final String type = r.getString("type");
        if (r.wasNull()) return null;

        final String md5 = r.getString("md5");
        if (r.wasNull()) return null;

        final String etag = r.getString("etag");
        if (r.wasNull()) return null;

        final int durability = r.getInt("durability");
        if (r.wasNull()) return null;

        final Timestamp mtimestamp = r.getTimestamp("mtime");
        if (r.wasNull()) return null;
        final long mtime = mtimestamp.getTime();

        final boolean dir = ObjectName.ObjectType.DIRECTORY.name().equalsIgnoreCase(type);

        final ExtendedObjectAttributes eoa = new ExtendedObjectAttributes(dir, size, md5, etag, durability, mtime);

        return new FullObjectName(on, eoa);
    }

    @Deprecated // duplicated with above
    private List<FullObjectName> mapResultSet(ResultSet r) throws SQLException {

        ImmutableList.Builder<FullObjectName> b = ImmutableList.builder();

        while (r.next()) {

            final String owner = r.getString("owner");
            if (r.wasNull()) continue;

            final String nss = r.getString("ns");
            if (r.wasNull()) continue;
            ObjectName.Namespace ns = ObjectName.Namespace.of(nss);
            if(ns.unknown()) continue;

            final String parent = r.getString("parent");
            if (r.wasNull()) continue;

            final String name = r.getString("name");
            if (r.wasNull()) continue;
            if(StringUtils.isBlank(name)) continue;

            final String type = r.getString("type");
            if (r.wasNull()) continue;
            boolean dir = ObjectName.ObjectType.DIRECTORY.name().equalsIgnoreCase(type);

            final long size = r.getLong("size");
            if (r.wasNull()) continue;

            final String md5 = r.getString("md5");
            if (r.wasNull()) continue;

            final String etag = r.getString("etag");
            if (r.wasNull()) continue;

            final int durability = r.getInt("durability");
            if (r.wasNull()) return null;

            final Timestamp mtimestamp = r.getTimestamp("mtime");
            if (r.wasNull()) return null;
            final long mtime = mtimestamp.getTime();

            ExtendedObjectAttributes eoa = new ExtendedObjectAttributes(dir, size, md5, etag, durability, mtime);

            b.add(new FullObjectName(owner, ns, parent, name, eoa));
        }

        return b.build();
    }

    private static final String PSQL_ERROR_CODE_unique_violation = "23505";

    private boolean entryExists(SQLException e) {
        return PSQL_ERROR_CODE_unique_violation.equals(e.getSQLState());
    }

    private void update(FullObjectName fon) throws MetadataException {
        log.trace("update: {}", fon);

        Connection c = null;
        PreparedStatement s = null;

        try {
            c = pool.connection(false);

            s = c.prepareStatement("UPDATE objects SET size=?, mtime=now(), md5=?, etag=? WHERE " +
                     "owner=? AND ns=?::namespace AND parent=? AND name=? AND type=?::object_type AND deleted=FALSE");

            s.setLong(1, fon.attributes.size);
            s.setString(2, fon.attributes.md5);
            s.setString(3, fon.attributes.etag);

            s.setString(4, fon.owner);
            s.setString(5, fon.ns.path);
            s.setString(6, fon.parent);
            s.setString(7, fon.name);
            s.setString(8, fon.attributes.dir ? "directory" : "file");

            s.setQueryTimeout(TIMEOUT_READ);

            final int row = s.executeUpdate();

            if (row != 1)
                throw new MetadataException("failed to update. affected row count does not match. for: " + fon + ", row: " + row);

            log.debug("updated success: {}", fon);

        } catch (SQLTimeoutException e) {
            throw new MetadataTimeoutException("timeout insert object: " + fon, e);
        } catch (SQLException e) {
            throw new MetadataException("sql error insert object: " + fon + ", error code: " + e.getErrorCode() + ", sql state: " + e.getSQLState(), e);
        } finally {
            tryClose(s);
            tryClose(c);
        }


    }

    @Override
    public void insert(FullObjectName fon) throws MetadataException {
        log.trace("insert: {}", fon);

        Connection c = null;
        PreparedStatement s = null;

        try  {
            c = pool.connection(false);
            s = c.prepareStatement("INSERT INTO objects(owner, ns, parent, name, type, size, mtime, md5, etag, durability, deleted) VALUES" +
                     "(?, ?::namespace, ?, ?, ?::object_type, ?, now(), ?, ?, ?, false)");

            s.setString(1, fon.owner);
            s.setString(2, fon.ns.path);
            s.setString(3, fon.parent);
            s.setString(4, fon.name);
            s.setString(5, fon.attributes.dir ? "directory" : "file");
            s.setLong(6, fon.attributes.size);
            s.setString(7, fon.attributes.md5);
            s.setString(8, fon.attributes.etag);
            s.setInt(9, fon.attributes.durability);

            s.setQueryTimeout(TIMEOUT_READ);

            final int row = s.executeUpdate();

            if (row != 1)
                throw new MetadataException("failed to insert. affected row count does not match. for: " + fon + ", row: " + row);

            log.debug("insert success: {}", fon);

        } catch (SQLTimeoutException e) {
            throw new MetadataTimeoutException("timeout insert object: " + fon, e);
        } catch (SQLException e) {
            if(entryExists(e)) {
                log.info("entry exits. updating: {}", fon);
                update(fon);
                return;
            }
            throw new MetadataException("sql error insert object: " + fon + ", error code: " + e.getErrorCode() + ", sql state: " + e.getSQLState(), e);
        } finally {
            tryClose(s);
            tryClose(c);
        }

    }

    @Override
    public void insertStored(String location, Etag etag) throws MetadataException {
        log.trace("insertStored: {} in {}", etag, location);

        Connection c = null;
        PreparedStatement s = null;

        try {
            c = pool.connection(false);
            s = c.prepareStatement("INSERT INTO storage(location, etag, mtime) VALUES(?, ?, now())");

            s.setString(1, location);
            s.setString(2, etag.value);

            s.setQueryTimeout(TIMEOUT_READ);

            final int row = s.executeUpdate();

            if (row != 1)
                throw new MetadataException("failed to insertStored. affected row count does not match: " + row);

            log.debug("insertStored success: {} in {}", etag, location);

        } catch (SQLTimeoutException e) {
            throw new MetadataTimeoutException("timeout insertStored", e);
        } catch (SQLException e) {
            throw new MetadataException("sql error insertStored", e);
        } finally {
            tryClose(s);
            tryClose(c);
        }

    }

    @Override
    public void delete(ObjectName on) throws MetadataException {
        log.trace("delete: {}", on);

        Connection c = null;
        PreparedStatement s = null;

        try {
             c = pool.connection(false);
             s = c.prepareStatement("UPDATE objects SET deleted = TRUE WHERE owner = ? AND ns = ?::namespace " +
                     "AND parent = ? AND name = ? AND deleted = FALSE");

            s.setString(1, on.owner);
            s.setString(2, on.ns.path);
            s.setString(3, on.parent);
            s.setString(4, on.name);

            s.setQueryTimeout(TIMEOUT_READ);

            final int row = s.executeUpdate();

            if (row != 1)
                throw new MetadataException("failed to flag delete. affected row count does not match. for: " + on + ", row: " + row);

            log.debug("delete success: {}", on);

        } catch (SQLTimeoutException e) {
            throw new MetadataTimeoutException("timeout delete object: " + on, e);
        } catch (SQLException e) {
            throw new MetadataException("sql error delete object: " + on, e);
        } finally {
            tryClose(s);
            tryClose(c);
        }

    }

    @Override
    public List<FullObjectName> listDir(ObjectName on, int limit) throws MetadataException {

        log.debug("listDir: {}", on);

        if(on.ns == ObjectName.Namespace.ROOT) // special kind of list, returning available namespaces
            return listNamespaces(on.owner);

        Connection c = null;
        PreparedStatement s = null;
        ResultSet r = null;

        try {
            c = pool.connection(true);
            s = c.prepareStatement("SELECT * FROM objects WHERE owner = ? AND ns = ?::namespace AND deleted = FALSE AND parent=? LIMIT ?");

            s.setString(1, on.owner);
            s.setString(2, on.ns.path);
            s.setString(3, on.path);
            s.setInt(4, limit);

            s.setQueryTimeout(TIMEOUT_READ);

            r = s.executeQuery();
            return mapResultSet(r);

        } catch (SQLTimeoutException e) {
            throw new MetadataTimeoutException("timeout list object: " + on, e);
        } catch (SQLException e) {
            throw new MetadataException("sql error list object: " + on, e);
        } finally {
            tryClose(r);
            tryClose(s);
            tryClose(c);
        }
    }

    @Override
    public List<String> findStorageByName(ObjectName on) throws MetadataException {
        Optional<FullObjectName> fon = selectByName(on);
        return fon.isPresent() ? findStorageNameByEtag(fon.get().attributes.etag) : Collections.<String>emptyList();
    }

    @Override
    public List<String> findStorageNameByEtag(String etag) throws MetadataException {
        log.debug("findStorageNameByEtag: {}", etag);

        Connection c = null;
        PreparedStatement s = null;
        ResultSet r = null;

        try {
            c = pool.connection(true);
            s = c.prepareStatement("SELECT location FROM storage WHERE etag=?");

            s.setString(1, etag);

            s.setQueryTimeout(TIMEOUT_READ);

            r = s.executeQuery();
            return mapStorageName(r);

        } catch (SQLTimeoutException e) {
            throw new MetadataTimeoutException("timeout list all storage for etag: " + etag, e);
        } catch (SQLException e) {
            throw new MetadataException("sql error list all storage for etag: " + etag, e);
        } finally {
            tryClose(r);
            tryClose(s);
            tryClose(c);
        }

    }

    private List<String> mapStorageName(ResultSet r) throws SQLException {

        ImmutableList.Builder<String> b = ImmutableList.builder();

        while (r.next()) {
            final String location = r.getString("location");
            if (r.wasNull()) continue;
            b.add(location);
        }

        return b.build();
    }

    @Override
    public boolean deletable(ObjectName on) throws MetadataException {
        if(on.ns == ObjectName.Namespace.ROOT || StringUtils.isBlank(on.name))
            return false;

        log.trace("deletable: {}", on);

        Connection c = null;
        PreparedStatement s = null;
        ResultSet r = null;

        try {
             c = pool.connection(true);
             s = c.prepareStatement("SELECT count(*) FROM objects WHERE owner = ? AND ns = ?::namespace " +
                     "AND parent = ? AND deleted = FALSE LIMIT 1");

            s.setString(1, on.owner);
            s.setString(2, on.ns.path);
            s.setString(3, on.path);

            s.setQueryTimeout(TIMEOUT_READ);

            r = s.executeQuery();
            return r.next() && r.getInt(1) == 0;

        } catch (SQLTimeoutException e) {
            throw new MetadataTimeoutException("timeout fetching object: " + on, e);
        } catch (SQLException e) {
            throw new MetadataException("sql error fetching object: " + on, e);
        } finally {
            tryClose(r);
            tryClose(s);
            tryClose(c);
        }

    }
}
