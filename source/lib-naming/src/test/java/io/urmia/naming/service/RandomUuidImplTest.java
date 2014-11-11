package io.urmia.naming.service;

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

public class RandomUuidImplTest {

    private static final Uuid uuid = new RandomUuidImpl();

    @Test
    public void test01() {

        String u1 = uuid.next();
        Assert.assertNotNull(u1);

        String u2 = uuid.next();
        Assert.assertNotNull(u2);

        Assert.assertNotEquals(u1, u2);
    }

    /*
    @Test
    public void test02() {
        String s = uuid.nextSmall();
        System.out.println("s: " + s);

        Assert.assertNotNull(s);
        Assert.assertFalse("nextSmall is not blank", s.isEmpty());
    }
    */

}
