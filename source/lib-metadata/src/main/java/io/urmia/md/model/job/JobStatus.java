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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class JobStatus {

    private static final JsonParser parser = new JsonParser();

    private final String id;
    private final String name = "";

    // todo: long -> date
    private final long timeCreated;
    private final long timeDone;
    private final long timeArchiveStarted;
    private final long timeArchiveDone;

    private final List<JobDefinition.Phase> phases;

    private final Stats stats;

    public JobStatus(String id, JobDefinition def) {
        this.id = id;
        this.phases = def.getPhases();
        this.timeCreated = System.currentTimeMillis();
        this.timeDone = 0;
        this.timeArchiveStarted = 0;
        this.timeArchiveDone = 0;
        this.stats = new Stats(State.created, false, false, new Counters(0,0,0,0,0));
    }

    private static long fromISO8601ToMillis(String s) {
        return javax.xml.bind.DatatypeConverter.parseDateTime(s).getTime().getTime();
    }

    private static String fromMillisToISO8601(long m) {
        Calendar c = GregorianCalendar.getInstance();
        c.setTime(new Date(m));
        return javax.xml.bind.DatatypeConverter.printDateTime(c);
    }

    public JobStatus(String json, Stats stats) {

        JsonObject rootObj = parser.parse(json).getAsJsonObject();

        this.id = rootObj.getAsJsonPrimitive("id").getAsString();
        this.phases = JobDefinition.Phase.fromJsonArray(rootObj.getAsJsonArray("phases"));

        this.timeCreated = fromISO8601ToMillis(rootObj.getAsJsonPrimitive("timeCreated").getAsString());
        this.timeDone = fromISO8601ToMillis(rootObj.getAsJsonPrimitive("timeDone").getAsString());
        this.timeArchiveStarted = fromISO8601ToMillis(rootObj.getAsJsonPrimitive("timeArchiveStarted").getAsString());
        this.timeArchiveDone = fromISO8601ToMillis(rootObj.getAsJsonPrimitive("timeArchiveDone").getAsString());

        this.stats = stats;
    }

    public static enum State {
        created((byte) 0),
        running((byte) 1),
        done((byte) 2),
        unknown((byte) 4);

        private final byte v;

        State(byte v) {
            this.v = v;
        }

        public State valueOf(byte b[]) {
            if(b == null || b.length == 0)
                return created;
            byte v = b[0];
            switch (v) {
                case 0: return created;
                case 1: return running;
                case 2: return done;
                default: return unknown;
            }
        }

    }

    public static enum Counter {
        errors,
        outputs,
        retries,
        tasks,
        tasksDone
    }

    public JsonElement toJson() {
        JsonObject v = new JsonObject();

        v.addProperty("id", id);
        v.addProperty("name", name);

        v.addProperty("state", stats.state.name());

        v.addProperty("cancelled", stats.cancelled);
        v.addProperty("inputDone", stats.inputDone);
        v.addProperty("transient", false);

        v.add("stats", counters());

        v.addProperty("timeCreated", fromMillisToISO8601(timeCreated));
        v.addProperty("timeDone", fromMillisToISO8601(timeDone));
        v.addProperty("timeArchiveStarted", fromMillisToISO8601(timeArchiveStarted));
        v.addProperty("timeArchiveDone", fromMillisToISO8601(timeArchiveDone));

        v.add("phases", JobDefinition.Phase.toJsonArray(phases));

        return v;
    }

    private JsonElement counters() {
        JsonObject j = new JsonObject();

        j.addProperty("errors", stats.counters.errors);
        j.addProperty("outputs", stats.counters.outputs);
        j.addProperty("retries", stats.counters.retries);
        j.addProperty("tasks", stats.counters.tasks);
        j.addProperty("tasksDone", stats.counters.tasksDone);

        return j;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public static class Stats {

        public final State state;
        public final boolean cancelled;
        public final boolean inputDone;

        private final Counters counters;

        public Stats(State state, boolean cancelled, boolean inputDone, Counters counters) {
            this.state = state;
            this.cancelled = cancelled;
            this.inputDone = inputDone;
            this.counters = counters;
        }
    }

    public static class Counters {

        public final int errors;
        public final int outputs;
        public final int retries;
        public final int tasks;
        public final int tasksDone;

        public Counters(int errors, int outputs, int retries, int tasks, int tasksDone) {
            this.errors = errors;
            this.outputs = outputs;
            this.retries = retries;
            this.tasks = tasks;
            this.tasksDone = tasksDone;
        }
    }
}
