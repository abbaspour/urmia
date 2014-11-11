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

import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;

import static io.urmia.util.DigestUtils.md5sum;

public class DigestUtilTest {

    @Test
    public void testDigest1() throws Exception {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("empty.txt");
        Assert.assertNotNull(in);
        final String sum = md5sum(in);
        System.out.println("sum = " + sum);
        Assert.assertEquals("1B2M2Y8AsgTpgAmY7PhCfg==", sum);
    }

    @Test
    public void testDigest2() throws Exception {
        InputStream in1 = this.getClass().getClassLoader().getResourceAsStream("abc.txt");
        InputStream in2 = this.getClass().getClassLoader().getResourceAsStream("abc.txt");
        byte[] md5 = org.apache.commons.codec.digest.DigestUtils.md5(in1);
        String md5commons = new String(Base64.encodeBase64(md5));
        String md5util = md5sum(in2);
        System.out.println("sum = " + md5util);
        Assert.assertEquals(md5commons, md5util);
        Assert.assertEquals("kAFQmDzST7DWlj99KOF/cg==", md5util);
    }

}
