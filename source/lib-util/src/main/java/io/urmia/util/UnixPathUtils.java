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

import static io.urmia.util.StringUtils.isBlank;
import static io.urmia.util.StringUtils.isEmpty;

public class UnixPathUtils {

    private static final CharSequence DOUBLE_SEPARATOR = "//";
    private static final CharSequence SINGLE_SEPARATOR = "/";
    private static final CharSequence PARENT = "/../";

    private static String trimDouble(String s) {
        return isBlank(s) ? "/" : s.trim().replace(DOUBLE_SEPARATOR, SINGLE_SEPARATOR).replace(PARENT, SINGLE_SEPARATOR);
    }

    private static String absolute(String s) {
        return startsWith('/', s) ? s : '/' + s;
    }

    public static String trimSlash(String s) {
        final int len = s.length();
        if (len == 0) return s;
        return s.charAt(len - 1) != '/' ? s : s.substring(0, len - 1);
    }

    public static String appendTrailingSlash(String input) {
        return isEmpty(input) ? "/" : endsWith(input, '/') ? input : input + '/';
    }

    public static boolean startsWith(char c, String s) {
        return s != null && !s.isEmpty() && s.charAt(0) == c;
    }

    public static boolean endsWith(String s, char c) {
        return s != null && !s.isEmpty() && s.charAt(s.length() - 1) == c;
    }

    public static String normalize(String somePath) {
        return absolute(trimSlash(trimDouble(somePath)));
    }

    public static String parent(String normalPath) {
        if (normalPath.length() <= 1) return normalPath;
        final int l = normalPath.lastIndexOf('/');
        return l <= 1 ? "/" : normalPath.substring(0, l);
    }

    public static String name(String normalPath) {
        final int l = normalPath.lastIndexOf('/');
        return l == -1 ? "" : normalPath.substring(l + 1);
    }

    public static String mkPath(String parent, String name) {
        return trimSlash(parent) + absolute(name);
    }
}
