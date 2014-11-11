package io.urmia.md.model.storage;

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
import io.urmia.util.UnixPathUtils;

import java.net.URI;
import java.net.URISyntaxException;

import static io.urmia.util.StringUtils.*;
import static io.urmia.util.UnixPathUtils.*;

public class ObjectName {

    public final String owner;
    public final Namespace ns;
    public final String parent;
    public final String name;
    public final String path;

    public ObjectName(ObjectName objectName) {
        this(objectName.owner, objectName.ns, objectName.parent, objectName.name);
    }

    public ObjectName(String owner, Namespace ns, String parent, String name) {
        assert isNotBlank(parent) : "parent is blank";
        assert parent.charAt(0) == '/' : "parent does not start with '/'";
        assert parent.equals("/") || isNotEmpty(name) : "name is empty";

        this.owner = owner;
        this.ns = ns;
        this.parent = parent;
        this.name = name;
        this.path = mkPath(parent, name);
    }

    @Override
    public String toString() {
        return '/' + owner + '/' + mkPath(ns.path, path);
    }

    public static Optional<ObjectName> of(String path) {
        if(path == null) return Optional.absent();
        try {
            return of(new URI(path));
        } catch (URISyntaxException e) {
            return Optional.absent();
        }
    }

    /**
     * root:        /owner/
     * namespace:   /owner/ns/path
     */
    public static Optional<ObjectName> of(URI uri) {

        if(uri == null) return Optional.absent();

        final String uriPath = appendTrailingSlash(uri.getPath());

        if(! startsWith('/', uriPath))
            return Optional.absent();

        int secondSlashPos = uriPath.indexOf('/', 1);

        if(secondSlashPos == -1)
            return Optional.absent();

        final String owner = uriPath.substring(1, secondSlashPos);

        if(isEmpty(owner)) // no owner
            return Optional.absent();

        // todo: only if mds.userExists(owner)
        if(secondSlashPos == uriPath.length() - 1) // special case of /user/ only. for listing root namespaces
            return Optional.of(new ObjectName(owner, Namespace.ROOT, "/", ""));


        String nsPath = uriPath.substring(secondSlashPos + 1);

        int thirdSlashPos = nsPath.indexOf('/');

        if(thirdSlashPos <= 0) // no namespace
            return Optional.absent();

        final String nsName = nsPath.substring(0, thirdSlashPos);

        final ObjectName.Namespace ns = ObjectName.Namespace.of(nsName);
        if (ns.unknown()) return Optional.absent(); // unknown ns

        final String normalPath = normalize(nsPath.substring(thirdSlashPos));
        final String parent = UnixPathUtils.parent(normalPath);
        final String name = UnixPathUtils.name(normalPath);

        final ObjectName on = new ObjectName(owner, ns, parent, name);

        return Optional.of(on);
    }

    public static enum Namespace {

        ROOT(""),
        JOBS("jobs"),
        PUBLIC("public"),
        REPORTS("reports"),
        STOR("stor"),
        INDEX("index"),
        UNKNOWN("?");

        public final String path;

        Namespace(String path) {
            this.path = path;
        }

        public static Namespace of(String path) {
            for (Namespace ns : Namespace.values())
                if (ns.path.equalsIgnoreCase(path))
                    return ns;
            return UNKNOWN;
        }

        public boolean unknown() {
            return UNKNOWN.equals(this);
        }

        public static Namespace[] list() { // public visibale ones
            return new Namespace[] {JOBS, PUBLIC, REPORTS, STOR };
        }
    }

    public static enum ObjectType {
        FILE,
        DIRECTORY
    }
}
