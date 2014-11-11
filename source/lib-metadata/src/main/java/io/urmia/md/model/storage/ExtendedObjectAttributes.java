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

public class ExtendedObjectAttributes {

    public final boolean dir;
    public final long size;
    public final String md5;
    public final String etag; // todo: Etag object
    public final int durability;
    public final long mtime;

    public ExtendedObjectAttributes(boolean dir, long size, String md5, String etag, int durability, long mtime) {
        this.dir = dir;
        this.size = size;
        this.md5 = md5;
        this.etag = etag;
        this.durability = durability;
        this.mtime = mtime;
    }

}
