package io.urmia.dd;

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

import io.netty.util.internal.NativeLibraryLoader;
import io.netty.util.internal.PlatformDependent;

import java.nio.ByteBuffer;

public class DirectDigest {

    static {
            NativeLibraryLoader.load("direct-digest", PlatformDependent.getClassLoader(DirectDigest.class));
    }

    public native static long md5_init();
    public native static void md5_update(long ctx, ByteBuffer buffer);
    public native static void md5_update(long ctx, byte[] array);
    public native static byte[] md5_final(long ctx);
}
