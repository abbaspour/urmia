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

import io.urmia.util.FileTime;

public class FullObjectName extends ObjectName {

    public final ExtendedObjectAttributes attributes;

    public FullObjectName(ObjectName objectName, ExtendedObjectAttributes eoa) {
        super(objectName);
        this.attributes = eoa;
    }

    public FullObjectName(String owner, Namespace ns, String parent, String name, ExtendedObjectAttributes eoa) {
        super(owner, ns, parent, name);
        this.attributes = eoa;
    }

    public String toSimpleString() { // todo: move to mock. not needed here
        return super.toString();
    }

    private String valueAsString = null;

    @Override
    public String toString() {
        String v = valueAsString;
        if(v == null) {
            v = "{" +
                    "\"name\":\"" + name + '\"' +
                    ",\"etag\":\"" + attributes.etag + '\"' +
                    ",\"size\": " + attributes.size +
                    ",\"md5\":\"" + attributes.md5 + '\"' +
                    ",\"type\":\"" + (attributes.dir ? "directory" : "object") + '\"' +
                    ",\"mtime\":\"" + millisToFileTimeString(attributes.mtime) + '\"' + //
                    ",\"durability\":" + attributes.durability +
                    //",\"parent\":\"" + parent + '\"' +
                    '}';
            valueAsString = v;
        }
        return v;
    }

    private static String millisToFileTimeString(long ms) {
        return FileTime.fromMillis(ms).toString();
    }
}
