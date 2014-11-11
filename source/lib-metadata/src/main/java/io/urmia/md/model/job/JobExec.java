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

import com.google.common.base.Splitter;
import com.google.gson.JsonArray;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public interface JobExec {

    public static enum Type {
        SHELL,
        JVM
    }

    public String getCommand();
    public List<String> getCommands();

    int execute();

    Type type();

    public static class Shell implements JobExec {

        private final String command;
        private final List<String> commands;

        private static final Pattern SPACE_DELIMITER_QUOTED = Pattern.compile(" (?=([^\"]*\"[^\"]*\")*[^\"]*$)");

        public Shell(String command) {
            this.command = command;

            List<String> interimCommands = Splitter.on(SPACE_DELIMITER_QUOTED).trimResults().omitEmptyStrings().splitToList(command);

            commands = new ArrayList<String>(interimCommands.size());

            for(String ic : interimCommands)
                commands.add(ic.replace('"',' ').trim());
        }

        @Override
        public String getCommand() {
            return command;
        }

        public List<String> getCommands() {
            return commands;
        }


        @Override
        public int execute() {
            System.err.println("this is supposed to run: " + command);
            return 0;
        }

        @Override
        public Type type() {
            return Type.SHELL;
        }

    }

    public static class Init implements JobExec {

        private final List<String> assets;
        private final JobExec job;

        public Init(String command, List<String> assets) {
            this.assets = assets;
            this.job = new Shell(command);
        }

        @Override
        public String getCommand() {
            return job.getCommand();
        }

        @Override
        public List<String> getCommands() {
            return job.getCommands();
        }

        public Init(String command, JsonArray assets) {
            this.assets = null;
            this.job = new Shell(command);
        }

        @Override
        public int execute() {
            return 0;
        }

        @Override
        public Type type() {
            return job.type();
        }

    }

}
