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

import com.google.common.base.Optional;

public class ArgumentParseUtil {

    private static final String DEFAULT_ZK_SERVER = "localhost:2181";

    public static String getZooKeeperURL(String[] args) {
        Optional<String> zkConfig = getArgument(args, "-z", "--zk");
        return zkConfig.or(DEFAULT_ZK_SERVER);
    }

    public static boolean isAutoRegister(String[] args) {
        return false;
    }

    /*
    public static void configLogger(String[] args) {
        Optional<String> logbackConfig = getArgument(args, "-l", "--log");
        if(logbackConfig.isPresent())
            LogbackConfiguration.configure(logbackConfig.get());
    }
    */

    static Optional<String> getArgument(String[] args, String shortName, String longName) {
        int i = 0;

        while (i < args.length && args[i].startsWith("-")) {
            String arg = args[i++];

            if (arg.equals(shortName) || arg.equals(longName))
                if (i < args.length)
                    return Optional.fromNullable(args[i]);
        }

        return Optional.absent();
    }

    public static void printUsageHelp(int exitCode) {

    }
}
