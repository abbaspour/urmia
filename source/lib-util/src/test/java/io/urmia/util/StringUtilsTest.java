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

import org.junit.Ignore;
import org.junit.Test;

import java.util.Iterator;
import java.util.StringTokenizer;

import static io.urmia.util.StringUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StringUtilsTest {

    @Test
    public void test_isEmpty_01() {
        assertTrue(isEmpty(null));
        assertFalse(isNotEmpty(null));
    }

    @Test
    public void test_isEmpty_02() {
        assertTrue(isEmpty(""));
        assertFalse(isNotEmpty(""));
    }

    @Test
    public void test_isEmpty_03() {
        assertFalse(isEmpty(" "));
        assertTrue(isNotEmpty(" "));
    }

    @Test
    public void test_isBlank_01() {
        assertTrue(isBlank(null));
        assertFalse(isNotBlank(null));
    }

    @Test
    public void test_isBlank_02() {
        assertTrue(isBlank(""));
        assertFalse(isNotBlank(""));
    }

    @Test
    public void test_isBlank_03() {
        assertTrue(isBlank(" "));
        assertFalse(isNotBlank(" "));
    }

    @Test
    public void test_stringTokenizer() {
        String hostnamePort = "localhost:5352";
        StringTokenizer st = new StringTokenizer(hostnamePort, ":");
        String hostname = st.nextToken();

        assertEquals("localhost", hostname);

        int port = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 53;
        assertEquals(5352, port);
    }

    @Test
    public void test_stringTokenizerNoPort() {
        String hostnamePort = "localhost";
        StringTokenizer st = new StringTokenizer(hostnamePort, ":");
        String hostname = st.nextToken();

        assertEquals("localhost", hostname);

        int port = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 53;
        assertEquals(53, port);
    }

    @Ignore
    @Test
    public void test_split_no_escape_simple_1() {
        String s = "a b";
        Iterator<String> r = splitRespectEscape(s, ' ').iterator();
        assertTrue(r.hasNext());
        assertEquals("a", r.next());
        assertTrue(r.hasNext());
        assertEquals("b", r.next());
    }

    @Test
    public void test_split_no_escape_simple_2() {
        String s = " a | b ";
        Iterator<String> r = splitRespectEscape(s, '|').iterator();
        assertTrue(r.hasNext());
        assertEquals("a", r.next());
        assertEquals("b", r.next());
    }

    @Test
    public void test_split_escape_simple_1() {
        String s = "a\\ b";
        Iterator<String> r = splitRespectEscape(s, ' ').iterator();
        assertTrue(r.hasNext());
        assertEquals("a\\ b", r.next());
    }

    @Test
    public void test_split_escape_simple_2() {
        String s = "a \\| b";
        Iterator<String> r = splitRespectEscape(s, '|').iterator();
        assertTrue(r.hasNext());
        assertEquals("a \\| b", r.next());
    }

    @Test
    public void test_split_escape_command_pipe() {
        String s = "ls -1 | wc -l";
        Iterator<String> r = splitRespectEscape(s, '|').iterator();
        assertTrue(r.hasNext());
        assertEquals("ls -1", r.next());
        assertEquals("wc -l", r.next());
    }

    @Test
    public void test_split_escape_02() {
        String s = "/bin/ls -1 | grep usr";
        Iterator<String> r = splitRespectEscape(s, '|').iterator();
        assertTrue(r.hasNext());
        assertEquals("/bin/ls -1", r.next());
        assertEquals("grep usr", r.next());
    }

    @Test
    public void testAppend() {
        assertEquals("1\n2", new String(append("1".getBytes(), "2")));
    }

}
