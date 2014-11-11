package io.urmia.cli.admin;

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

import org.apache.commons.cli.*;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import io.urmia.naming.service.NamingService;
import io.urmia.naming.service.ZkNamingServiceImpl;


public class AdminMain {

    //private static final Logger log = LoggerFactory.getLogger(AdminMain.class);

    static final Options options = new Options();

    static {
        Option az = OptionBuilder.withArgName("zone").withLongOpt("az").hasArg()
                .withType(Integer.class).withDescription("availability zone (default 1)")
                .create("a");

        Option port = OptionBuilder.withArgName("port").withLongOpt("port").hasArg()
                .withType(Integer.class).withDescription("node port")
                .create("p");

        Option command = new Option("c", "command", true, "command: list, create, delete");
        Option zk = new Option("z", "zk", true, "ZooKeeper URL (defaults to localhost:2181)");
        Option type = new Option("t", "type", true, "node type (MDB,MDS,ODS,JDS,JRS,AAS)");
        Option host = new Option("h", "host", true, "node host");
        Option id = new Option("i", "id", true, "node id (default is auto generated UUID)");
        Option json = new Option("j", "json", true, "JSON file to read input from");
        Option uri = new Option("u", "uri", true, "URI. Required for particular types");
        Option help = new Option("?", "help", false, "show this help");

        options.addOption(command)
                .addOption(zk)
                .addOption(az)
                .addOption(type)
                .addOption(host)
                .addOption(port)
                .addOption(id)
                .addOption(json)
                .addOption(uri)
                .addOption(help);
    }


    private static CuratorFramework getZkClient(String url) {
        CuratorFramework client = CuratorFrameworkFactory.newClient(url, new ExponentialBackoffRetry(1000, 3));
        client.start();
        return client;
    }

    private static HelpFormatter formatter = new HelpFormatter();
    private static CommandLineParser parser = new GnuParser();

    public static void main(String[] args) throws Exception {
        final AdminCommand line = parseArgs(args);
        if (line == null) return;

        final CuratorFramework client = getZkClient(line.getZkUrl());
        final NamingService ns = new ZkNamingServiceImpl(client, line.getAvailabilityZone());
        final NamingAdminFacade facade = new DefaultNamingAdminFacadeImpl(ns);

        try {
            facade.apply(line);
        }catch (Exception e) {
            System.err.println("error: " + e.getMessage());
            System.exit(-1);
        }
    }

    private static void help(boolean ok) {
        formatter.printHelp("AdminMain", options);
        System.exit(ok ? 0 : -1);
    }

    private static AdminCommand parseArgs(String[] args) {

        final CommandLine line;
        final AdminCommand cmd;

        try {
            line = parser.parse(options, args);
            cmd = new AdminCommand(line);
        } catch (ParseException e) {
            help(false);
            return null;
        } catch (IllegalArgumentException e) {
            help(false);
            return null;
        }

        if(line.hasOption("help")) {
            help(true);
            return null;
        }

        return cmd;
    }
}
