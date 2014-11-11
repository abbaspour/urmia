package io.urmia.proxy;

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

public class ProxyUserEvent {

    public static enum Type {
        OUTBOUND_CONTINUE,
        OUTBOUND_COMPLETED,
        OUTBOUND_INACTIVE,
        OUTBOUND_ERROR
    }

    public final Type type;
    public final int index;

    public ProxyUserEvent(Type type, int index) {
        this.type = type;
        this.index = index;
    }

    @Override
    public String toString() {
        return "ProxyUserEvent{" +
                "type=" + type +
                ", index=" + index +
                '}';
    }
}
