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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import io.urmia.md.model.storage.ObjectName;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class LineJobInput extends JobInput {

    private static final Pattern EOL = Pattern.compile("\\r?\\n");
    private static final Splitter SPLITTER = Splitter.on(EOL).omitEmptyStrings().trimResults();

    private final List<String> lines;
    private final String input;
    private int lineCount = 0;

    public LineJobInput(Collection<ObjectName> input) {
        //this.objectNames = input.iterator();

        StringBuilder sb = new StringBuilder();
        lines = new LinkedList<String>();

        for(ObjectName on : input) {
            String l = on.toString();
            lines.add(l);
            sb.append(l).append('\n');
            lineCount++;
        }
        this.input = sb.toString();
    }

    public LineJobInput(final List<String> lines) {
        this.lines = lines;
        this.input = Joiner.on('\n').join(lines); // lines.toString();
    }

    public LineJobInput(String input) {
        this.input = input;
        final Iterator<String> lines = SPLITTER.split(input).iterator();
        this.lines = Lists.newArrayList(lines);
    }

    @Override
    public Iterator<String> iterator() {
        return lines.iterator();
    }

    @Override
    public boolean isEod() {
        return false;
    }

    @Override
    public String toString() {
        return input;
    }

    @Override
    public byte[] toBytes() {
        return input.getBytes();
    }

    @Override
    public int getCount() {
        return lineCount;
    }
}
