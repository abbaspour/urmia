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

import com.google.common.base.Optional;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ArgumentParseUtilTest {

    @Test
    public void testGetArgument01() {
        Optional<String> v = ArgumentParseUtil.getArgument(new String[]{"-a", "value"}, "-a", "--a");
        assertTrue(v.isPresent());
        assertEquals("value", v.get());
    }

    @Test
    public void testGetArgument02() {
        Optional<String> v = ArgumentParseUtil.getArgument(new String[]{"-a"}, "-a", "--a");
        assertFalse(v.isPresent());
    }
}
