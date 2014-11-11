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

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;

public class FileTimeTest {

    String ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss.sZ";

    @Test
    public void test3() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_8601);
        //Date d = sdf.parse("2014-08-06T07:04:29.700Z");
        String s = "2014-08-06T07:04:29.700Z";
        //Date d = sdf.parse();
        Date d = javax.xml.bind.DatatypeConverter.parseDateTime(s).getTime();
        System.out.println("d = " + d);
    }

    @Test
    public void test4() throws ParseException {
        Calendar c = GregorianCalendar.getInstance();
        c.setTime(new Date());
        String s = javax.xml.bind.DatatypeConverter.printDateTime(c);
        System.out.println("s = " + s);

        Date d = javax.xml.bind.DatatypeConverter.parseDateTime(s).getTime();
        System.out.println("d = " + d);
    }

}
