package io.urmia.md.model.job;

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

import io.netty.handler.codec.http.FullHttpRequest;
import io.urmia.util.StringUtils;

public class JobQueryRequest extends JobRequest {

    public static enum Type {
        Status,
        Inputs,
        Outputs,
        Errors,
        Failures,
        List;

        private final String[] paths;

        Type(String... paths) {
            this.paths = paths;
        }

        public static Type of(String path) {
            if(StringUtils.isBlank(path))
                return Status;

            for(Type t : Type.values())
                for(String p : t.paths)
                    if(path.endsWith(p))
                        return t;

            return Status;
        }
    }

    public final Type type;

    public JobQueryRequest(FullHttpRequest fullHttpRequest) throws ExceptionInInitializerError {
        super(fullHttpRequest);
        type = findType(uri);
        id = type == Type.List ? "" : getId(uri);
    }

    @Override
    public String toString() {
        return "JobQueryRequest{" +
                "id='" + id + '\'' +
                ", type=" + type +
                '}';
    }

    private Type findType(String uri) {
        if(uri.endsWith("/live/status")) return Type.Status;
        if(uri.endsWith("/live/in") || uri.endsWith("/in.txt")) return Type.Inputs;
        if(uri.endsWith("/live/out") || uri.endsWith("/out.txt")) return Type.Outputs;
        if(uri.endsWith("/live/err") || uri.endsWith("/err.txt")) return Type.Errors;
        if(uri.endsWith("/live/fail")) return Type.Failures;
        return Type.List;
    }
}
