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

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestUtils {

    private static final MessageDigest md;

    static {
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static String md5sum(File file) throws IOException {
        //return md5sum(Files.newInputStream(file.toPath(), StandardOpenOption.READ));
        InputStream inp = null;
        try {
            inp = new FileInputStream(file);
            return md5sum(inp);
        } finally {
            if(inp != null) inp.close();
        }
    }

    public static String md5sum(InputStream in) throws IOException {
        DigestInputStream md5stream = null;

        byte[] buffer = new byte[1024];

        try {
            //in = new FileInputStream(file);
            md5stream = new DigestInputStream(in, (MessageDigest) md.clone());
            //noinspection StatementWithEmptyBody
            while (md5stream.read(buffer) > -1);
            byte[] md5Digest = md5stream.getMessageDigest().digest();

            return new String(Base64.encode(Unpooled.wrappedBuffer(md5Digest)).array()).trim();

        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        } finally {
            if(in != null)
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            if(md5stream != null)
                try {
                    md5stream.close();
                } catch (IOException ignored) {
                }
        }
    }

}
