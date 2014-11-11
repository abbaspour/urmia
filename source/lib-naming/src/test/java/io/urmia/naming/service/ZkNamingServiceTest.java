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

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceType;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.urmia.naming.model.NodeType;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static io.urmia.naming.model.NodeType.*;

public class ZkNamingServiceTest {

    private static final Logger log = LoggerFactory.getLogger(ZkNamingServiceTest.class);


    private static final int ZK_PORT = 21818;
    private static final String ZK_SERVER = "localhost:" + ZK_PORT;

    private static ServerCnxnFactory standaloneServerFactory;

    static CuratorFramework client;
    static ZooKeeperServer server;

    static NamingService ns;

    @BeforeClass
    public static void startZooKeeperServer() throws Exception {
        log.info("startZooKeeperServer on: {}", ZK_SERVER);

        int clientPort = ZK_PORT; // none-standard
        int numConnections = 5;
        int tickTime = 2000;
        //String dataDirectory = System.getProperty("java.io.tmpdir");
        String dataDirectory = "/tmp/ZkNamingServiceTest";

        log.info("dataDirectory: {}", dataDirectory);
        File dir = new File(dataDirectory, "zookeeper").getAbsoluteFile();

        server = new ZooKeeperServer(dir, dir, tickTime);
        standaloneServerFactory = NIOServerCnxnFactory.createFactory(new InetSocketAddress(clientPort), numConnections);

        standaloneServerFactory.startup(server); // start the server.

        log.info("temp zk server is up...");

        client = CuratorFrameworkFactory.newClient(ZK_SERVER, new ExponentialBackoffRetry(1000, 3));
        client.start();


        ns = new ZkNamingServiceImpl(client, 1);

        Thread.sleep(1000);

        List<NodeType> types = ns.queryTypes();

        for(NodeType t : types)
            deleteAll(t);

    }

    @AfterClass
    public static void stopZooKeeperServer() {
        CloseableUtils.closeQuietly(client);

        server.shutdown();

        log.info("stopZooKeeperServer...");
        try {
            standaloneServerFactory.shutdown();
        }catch (Exception e) {
            log.info("e: ", e.getMessage());
        }
        log.info("stopZooKeeperServer stopped");
    }

    @Test
    public void testSimpleAddFindSingleNode2() throws Exception {

        String id = UUID.randomUUID().toString();

        ServiceInstance<NodeType> si = ServiceInstance.<NodeType>builder()
                .address("localhost")
                .name(NodeType.JRS.name())
                .id(id)
                .port(NodeType.JRS.defaultPort)
                .payload(NodeType.JRS)
                .serviceType(ServiceType.PERMANENT)
                .build();

        ns.add(si);
        log.debug("added node: {}", si);

        Thread.sleep(100);

        ServiceInstance<NodeType> hostNode = ns.get(NodeType.JRS, id);
        log.info("nodes: {}", hostNode);

        assertNotNull(hostNode);
        assertEquals(NodeType.JRS, hostNode.getPayload());
        assertEquals(NodeType.JRS.name(), hostNode.getName());
        assertEquals("localhost", hostNode.getAddress());
        assertEquals(id, hostNode.getId());
    }

    private static void deleteAll(NodeType t) throws Exception {
        for(ServiceInstance<NodeType> si : ns.list(t, Predicates.<ServiceInstance<NodeType>>alwaysTrue())) {
            log.info("removing: {}", si);
            ns.remove(si);
        }
    }

    @Test
    public void testWhoAmI_1instance() throws Exception {

        deleteAll(ODS);

        ServiceInstance<NodeType> b = ns.builder(ODS, ServiceType.STATIC).build();

        ns.add(b);

        Optional<ServiceInstance<NodeType>> opt = ns.whoAmI(ODS, false);

        assertTrue(opt.isPresent());
        ServiceInstance<NodeType> si = opt.get();

        assertEquals(ODS, si.getPayload());
        assertEquals(b.getName(), si.getName());
        assertEquals(b.getId(), si.getId());
    }

    @Test
    public void testWhoAmI_2instance_sort_Port() throws Exception {
        deleteAll(ODS);

        ServiceInstance<NodeType> b1 = ns.builder(ODS, ServiceType.STATIC).id("testWhoAmI_2instance_sort_Port-id1").build();
        ns.add(b1);

        ServiceInstance<NodeType> b2 = ns.builder(ODS, ServiceType.STATIC).id("testWhoAmI_2instance_sort_Port-id2").build();
        ns.add(b2);

        Optional<ServiceInstance<NodeType>> opt = ns.whoAmI(ODS, false);

        assertTrue(opt.isPresent());
        ServiceInstance<NodeType> si = opt.get();

        assertEquals(ODS, si.getPayload());
        assertEquals(b1.getName(), si.getName());
        assertEquals(b1.getId(), si.getId());
        assertEquals(ODS.defaultPort, si.getPort().intValue());

    }

    @Test
    public void testWhoAmI_2instance_1running() throws Exception {

        deleteAll(ODS);

        ServiceInstance<NodeType> b1 = ns.builder(ODS, ServiceType.STATIC).id("testWhoAmI_2instance_1running-id1").build();
        ns.add(b1);

        ServiceInstance<NodeType> b2 = ns.builder(ODS, ServiceType.STATIC).id("testWhoAmI_2instance_1running-id2").build();
        ns.add(b2);

        ns.register(b1);

        Optional<ServiceInstance<NodeType>> opt = ns.whoAmI(ODS, false);

        assertTrue(opt.isPresent());
        ServiceInstance<NodeType> si = opt.get();

        assertEquals(ODS, si.getPayload());
        assertEquals(b2.getName(), si.getName());
        assertEquals(b2.getId(), si.getId());
        assertEquals(b2.getPort().intValue(), ODS.defaultPort + 1);

        ns.deregister(b1);

    }

    private ServiceInstance<NodeType> buildSI(NodeType type, String host, int port) throws Exception {
        return buildSI(type, UUID.randomUUID().toString(), host, port);
    }

    private ServiceInstance<NodeType> buildSI(NodeType type, String id, String host, int port) throws Exception {
        return ServiceInstance.<NodeType>builder()
                .id(id)
                .name(type.name())
                .address(host)
                .payload(type)
                .port(port)
                .build();

    }

    @Test
    public void testNodeTypeComparatorTest_port() throws Exception {
        ServiceInstance<NodeType> i1_8080 = buildSI(ODS, "localhost", 8080);
        ServiceInstance<NodeType> i2_8081 = buildSI(ODS, "localhost", 8081);

        int c = ZkNamingServiceImpl.nodeTypeComparator.compare(i1_8080, i2_8081);
        assertTrue(c < 0);
    }

    @Test
    public void testNodeTypeComparatorTest_list() throws Exception {
        ServiceInstance<NodeType> i1_8080 = buildSI(ODS, "localhost", 8080);
        ServiceInstance<NodeType> i2_8081 = buildSI(ODS, "localhost", 8081);

        List<ServiceInstance<NodeType>> l1 = Lists.newArrayList(i1_8080, i2_8081);
        List<ServiceInstance<NodeType>> l2 = Lists.newArrayList(i2_8081, i1_8080);

        List<ServiceInstance<NodeType>> s1 = Ordering.from(ZkNamingServiceImpl.nodeTypeComparator).immutableSortedCopy(l1);
        List<ServiceInstance<NodeType>> s2 = Ordering.from(ZkNamingServiceImpl.nodeTypeComparator).immutableSortedCopy(l2);

        assertFalse(s1.isEmpty());
        assertFalse(s2.isEmpty());

        assertEquals(2, s1.size());
        assertEquals(2, s2.size());

        assertEquals(i1_8080, s1.get(0));
        assertEquals(i1_8080, s2.get(0));

        assertEquals(i2_8081, s1.get(1));
        assertEquals(i2_8081, s2.get(1));
    }
}
