package io.urmia.md.model.job;

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

import java.util.Iterator;

public abstract class JobInput implements Iterable<String> {

    public abstract boolean isEod();
    public abstract byte[] toBytes();
    public abstract int getCount();

    public static JobInput END = new END();

    private static class END extends JobInput {

        private static byte[] EOT = new byte[]{4};

        @Override
        public boolean isEod() {
            return true;
        }

        @Override
        public byte[] toBytes() {
            return EOT;
        }

        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public Iterator<String> iterator() {
            return new Iterator<String>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public String next() {
                    return null;
                }

                @Override
                public void remove() {
                }
            };
        }

        @Override
        public String toString() {
            return "[EOT]";
        }
    }
}
