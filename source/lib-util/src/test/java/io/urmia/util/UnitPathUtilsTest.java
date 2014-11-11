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

import org.junit.Assert;
import org.junit.Test;

public class UnitPathUtilsTest {

    @Test
    public void test_normalize_00() {
        Assert.assertEquals("/", UnixPathUtils.normalize(""));
    }

    @Test
    public void test_normalize_01() {
        Assert.assertEquals("/a/b/c", UnixPathUtils.normalize("a//b/c"));
    }

    @Test
    public void test_normalize_02() {
        Assert.assertEquals("/a/b/c", UnixPathUtils.normalize("/a//b/c/"));
    }

    @Test
    public void test_normalize_03() {
        Assert.assertEquals("/a/b/c", UnixPathUtils.normalize("a/b/c/"));
    }

    @Test
    public void test_normalize_04() {
        Assert.assertEquals("/a/b/c", UnixPathUtils.normalize("a/../b/c/"));
    }

    @Test
    public void test_parent_name_01() {
        final String n = UnixPathUtils.normalize("a/b/c/");
        Assert.assertEquals("/a/b", UnixPathUtils.parent(n));
        Assert.assertEquals("c", UnixPathUtils.name(n));
    }

    @Test
    public void test_parent_name_02() {
        final String n = UnixPathUtils.normalize("");
        Assert.assertEquals("/", UnixPathUtils.parent(n));
        Assert.assertEquals("", UnixPathUtils.name(n));
    }

    @Test
    public void test_root_01() {
        final String n = UnixPathUtils.normalize("somefile");
        Assert.assertEquals("/somefile", n);
        Assert.assertEquals("/", UnixPathUtils.parent(n));
        Assert.assertEquals("somefile", UnixPathUtils.name(n));
    }

    @Test
    public void test_root_02() {
        final String n = UnixPathUtils.normalize("/somefile/");
        Assert.assertEquals("/somefile", n);
        Assert.assertEquals("/", UnixPathUtils.parent(n));
        Assert.assertEquals("somefile", UnixPathUtils.name(n));
    }

    @Test
    public void test_mkPath_01() {
        String p = UnixPathUtils.mkPath("", "a");
        Assert.assertEquals("/a", p);
    }

    @Test
    public void test_mkPath_02() {
        String p = UnixPathUtils.mkPath("a", "b");
        Assert.assertEquals("a/b", p);
    }

    @Test
    public void test_mkPath_03() {
        String p = UnixPathUtils.mkPath("/a", "b");
        Assert.assertEquals("/a/b", p);
    }

    @Test
    public void test_mkPath_04() {
        String p = UnixPathUtils.mkPath("/a/", "b");
        Assert.assertEquals("/a/b", p);
    }

    @Test
    public void test_mkPath_05() {
        String p = UnixPathUtils.mkPath("/a/", "/b");
        Assert.assertEquals("/a/b", p);
    }
}
