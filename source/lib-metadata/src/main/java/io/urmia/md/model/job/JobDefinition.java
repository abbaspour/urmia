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
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.*;
import io.urmia.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JobDefinition {

    private static final JsonParser parser = new JsonParser();

    private final String owner;
    private final List<Phase> phases;

    public JobDefinition(String owner, List<Phase> phases) {
        this.owner = owner;
        this.phases = phases;
    }

    public JobDefinition(String jsonStr) {
        this(parser.parse(jsonStr));
    }

    public JobDefinition(String owner, String phasesJsonStr) {
        JsonObject rootObj = parser.parse(phasesJsonStr).getAsJsonObject();
        this.phases = Phase.fromJsonArray(rootObj.get("phases").getAsJsonArray());
        this.owner = owner;
    }

    public String getOwner() {
        return owner;
    }

    public List<Phase> getPhases() {
        return phases;
    }

    public List<Phase> getPhases(Phase.Type type) {
        return FluentIterable.from(phases).filter(phaseType(type)).toList();
    }

    private Predicate<Phase> phaseType(final Phase.Type type) {
        return new Predicate<Phase>() {
            @Override
            public boolean apply(Phase input) {
                return type.equals(input.type);
            }
        };
    }

    public JobDefinition(JsonElement root) {
        JsonObject rootObj = root.getAsJsonObject();
        JsonArray phasesArray = rootObj.get("phases").getAsJsonArray();

        Iterator<JsonElement> phasesIterator = phasesArray.iterator();

        ImmutableList.Builder<Phase> b = ImmutableList.builder();

        while (phasesIterator.hasNext())
            b.add(new Phase(phasesIterator.next()));

        this.phases = b.build();
        this.owner = rootObj.get("owner").getAsString();
        this.json = root;
    }

    private JsonElement json = null;

    public JsonElement toJson() {
        if(json != null)
            return json;

        JsonObject v = new JsonObject();
        v.add("phases", Phase.toJsonArray(phases));
        v.add("owner", new JsonPrimitive(owner));

        this.json = v;
        return json;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public boolean hasReduce() {
        for(Phase p : phases)
            if(Phase.Type.REDUCE == p.type)
                return true;
        return false;
    }

    public static final int DEFAULT_MEMORY = 2048;
    public static final int DEFAULT_DISK = 16;

    public static class Phase {

        public static enum Type {
            MAP,
            REDUCE,
            INIT
        }

        public final Type type;
        public final List<JobExec> exec;
        public final int count;
        public final Optional<ZoneImage> image;
        public final Optional<JobExec> init;
        public final int memory; // in megabyte
        public final int disk; // in gigabyte

        public static List<Phase> fromJsonArray(JsonArray phasesArray) {
            Iterator<JsonElement> phasesIterator = phasesArray.iterator();
            ImmutableList.Builder<Phase> b = ImmutableList.builder();
            while (phasesIterator.hasNext()) b.add(new Phase(phasesIterator.next()));
            return b.build();
        }

        public static JsonArray toJsonArray(List<Phase> phases) {
            JsonArray jsonPhases = new JsonArray();
            for(Phase p : phases) jsonPhases.add(p.toJson());
            return jsonPhases;
        }

        public Phase(String execStr) {
            this(parseJobString(execStr), Type.MAP, 1, null, null);
        }

        public Phase(JobExec exec) {
            this(Lists.newArrayList(exec), Type.MAP, 1, null, null);
        }

        public Phase(List<JobExec> exec, Type type, int count, ZoneImage image, JobExec init) {
            this.exec = exec;
            this.type = type;
            this.count = count;
            this.image = Optional.fromNullable(image);
            this.init = Optional.fromNullable(init);
            this.memory = 2048;
            this.disk = 16;
        }

        private static List<JobExec> parseJobString(String execStr) {
            ImmutableList.Builder<JobExec> b = ImmutableList.builder();
            for(String ec : StringUtils.splitRespectEscape(execStr, '|'))
                b.add(new JobExec.Shell(ec));
            return b.build();
        }

        public Phase(JsonElement phaseElement) {

            if(phaseElement == null) throw new IllegalArgumentException("json element is null");

            JsonObject phase = phaseElement.getAsJsonObject();
            if(phase == null) throw new IllegalArgumentException("phase element is null");

            // -- exec --
            if(! phase.has("exec")) throw new IllegalArgumentException("exec is empty");
            String execStr = phase.getAsJsonPrimitive("exec").getAsString();
            if(StringUtils.isBlank(execStr)) throw new IllegalArgumentException("exec is empty");

            this.exec = parseJobString(execStr);

            // -- type --
            this.type = phase.has("type") ? Type.valueOf(phase.getAsJsonPrimitive("type").getAsString().toUpperCase()) : Type.MAP;

            // -- count --
            this.count = phase.has("count") ? phase.getAsJsonPrimitive("count").getAsInt() : 1;

            // -- image --
            this.image = phase.has("image") ? Optional.of(new ZoneImage(phase.getAsJsonPrimitive("image").getAsString()))
                    : Optional.<ZoneImage>absent();

            // -- assets && init --
            if(phase.has("init")) {
                String initStr = phase.getAsJsonPrimitive("init").getAsString();
                this.init = Optional.<JobExec>of(new JobExec.Init(initStr, phase.getAsJsonArray("assets")));
            } else {
                this.init = Optional.absent();
            }

            this.memory = phase.has("memory") ? phase.getAsJsonPrimitive("memory").getAsInt() : DEFAULT_MEMORY;
            this.disk = phase.has("disk") ? phase.getAsJsonPrimitive("disk").getAsInt() : DEFAULT_DISK;
        }

        public JsonElement toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("exec", Joiner.on(" | ").skipNulls().join(getCommands()));
            json.addProperty("type", type.name().toLowerCase());
            // todo: add rest
            return json;
        }

        private List<String> getCommands() {
            List<String> commands = new ArrayList<String>(exec.size());
            for(JobExec jobExec : exec)
               commands.add(jobExec.getCommand());
            return commands;
        }

        @Override
        public String toString() {
            return toJson().toString();
        }
    }


}
