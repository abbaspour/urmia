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

import java.nio.charset.Charset;

import static io.urmia.md.model.job.JobInput.END;

public class JobInputRequest extends JobRequest {

    public final String body;
    public final JobInput input;

    public JobInputRequest(FullHttpRequest fullHttpRequest) throws ExceptionInInitializerError {
        super(fullHttpRequest);

        this.body = fullHttpRequest.content().toString(Charset.defaultCharset()).trim();
        this.id = getId(uri);

        if(isEOT())
            input = END;
        else
            input = new LineJobInput(body);
    }

    private boolean isEOT() {
        return uri.endsWith("/live/in/end");
    }

    @Override
    public String toString() {
        return "JobInputRequest{" +
                "id='" + id + '\'' +
                ", body='" + body + '\'' +
                ", input=" + input +
                '}';
    }
}
