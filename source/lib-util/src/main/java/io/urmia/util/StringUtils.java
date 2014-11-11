package io.urmia.util;

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

import com.google.common.base.Splitter;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

public class StringUtils {

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isNotEmpty(String s) {
        return !isEmpty(s);
    }

    public static boolean isBlank(String s) {
        final int strLen;
        if (s == null || (strLen = s.length()) == 0)
            return true;

        for (int i = 0; i < strLen; i++)
            if (!Character.isWhitespace(s.charAt(i)))
                return false;

        return true;
    }

    public static boolean isNotBlank(String s) {
        return !isBlank(s);
    }

    public static String mapToJson(Map<String, Object> map) {
        final StringBuilder sb = new StringBuilder("{");

        Iterator<Map.Entry<String, Object>> itr = map.entrySet().iterator();

        while (itr.hasNext()) {
            Map.Entry<String, Object> e = itr.next();
            sb.append('"').append(e.getKey()).append("\":\"").append(e.getValue().toString()).append('"');
            if (itr.hasNext()) sb.append(", ");
        }

        return sb.append('}').toString();
    }

    public static Iterable<String> splitRespectEscape(String s, char c) {
        String cs = shouldEscape(c) ? "\\" + c : Character.toString(c);
        Pattern p = Pattern.compile("[^\\\\]" + cs);
        //return Splitter.on(c).omitEmptyStrings().trimResults().split(s);
        return Splitter.on(p).omitEmptyStrings().trimResults().split(s);
    }

    private static boolean shouldEscape(char c) {
        switch (c) {
            case '|': return true;
            default: return false;
        }
    }

    public static byte[] append(byte[] orig, String toAppend) {
        return append(orig, toAppend.getBytes(), (byte) '\n');
    }

    public static byte[] append(byte[] existing, byte[] addition) {
        return append(existing, addition, (byte) '\n');
    }

    public static byte[] append(byte[] existing, byte[] addition, byte delimiter) {

        byte[] content = new byte[existing.length + 1 + addition.length];

        System.arraycopy(existing, 0, content, 0, existing.length);
        content[existing.length] = delimiter;
        System.arraycopy(addition, 0, content, existing.length + 1, addition.length);

        return content;
    }
}
