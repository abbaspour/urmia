package io.urmia.naming.service;

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

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

public class ZkNamingServiceTestMain {

    private static final Logger log = LoggerFactory.getLogger(ZkNamingServiceTestMain.class);


    private static final int ZK_PORT = 21818;
    private static final String ZK_SERVER = "localhost:" + ZK_PORT;

    private static ServerCnxnFactory standaloneServerFactory;

    static CuratorFramework client;

    @BeforeClass
    public static void startZooKeeperServer() throws IOException, InterruptedException {
        log.info("startZooKeeperServer on: {}", ZK_SERVER);

        int clientPort = ZK_PORT; // none-standard
        int numConnections = 5;
        int tickTime = 2000;
        String dataDirectory = System.getProperty("java.io.tmpdir");

        log.info("dataDirectory: {}", dataDirectory);

        File dir = new File(dataDirectory, "zookeeper").getAbsoluteFile();

        ZooKeeperServer server = new ZooKeeperServer(dir, dir, tickTime);
        standaloneServerFactory = NIOServerCnxnFactory.createFactory(new InetSocketAddress(clientPort), numConnections);

        standaloneServerFactory.startup(server); // start the server.

        log.info("temp zk server is up...");

        client = CuratorFrameworkFactory.newClient(ZK_SERVER, new ExponentialBackoffRetry(1000, 3));
        client.start();
    }

    @AfterClass
    public static void stopZooKeeperServer() {
        client.close();

        log.info("stopZooKeeperServer...");
        standaloneServerFactory.shutdown();
        log.info("stopZooKeeperServer stopped");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        startZooKeeperServer();
    }
}
