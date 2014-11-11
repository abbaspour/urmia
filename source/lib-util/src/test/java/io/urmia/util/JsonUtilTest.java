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

import java.util.HashMap;
import java.util.Map;

public class JsonUtilTest {
    @Test
    public void testJsonUtil_01() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("k1", "v1");
        m.put("k2", 1234L);

        String json = StringUtils.mapToJson(m);
        Assert.assertEquals(json, "{\"k1\":\"v1\", \"k2\":\"1234\"}");
    }
}
