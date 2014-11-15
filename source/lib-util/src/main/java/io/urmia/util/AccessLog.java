package io.urmia.util;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class AccessLog {

    private static final Logger log = LoggerFactory.getLogger("access");

    public void fail(ChannelHandlerContext ctx, String operation, String object, long startMS) {
        success(ctx.channel().remoteAddress().toString(), operation, object, startMS);
    }

    public void fail(String ip, String operation, String object, long startMS) {
        log(ip, operation, object, System.currentTimeMillis() - startMS, "FAIL");
    }

    public void success(ChannelHandlerContext ctx, String operation, String object, long startMS) {
        success(ctx.channel().remoteAddress().toString(), operation, object, startMS);
    }

    public void success(String ip, String operation, String object, long startMS) {
        log(ip, operation, object, System.currentTimeMillis() - startMS, "OK");
    }

    private void log(String ip, String operation, String object, long ms, String status) {
        log.error("{} {} {} {} {}", ip, operation, object, status, ms);

    }
}
