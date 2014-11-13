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

import com.google.common.base.*;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.UnhandledErrorListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.*;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.urmia.naming.model.NodeType;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * this class supposed to cache values and reflect changes in almost real time.
 *
 * todo: need a constructor/builder which accepts args[] and checks for '-z <URL>' and '-a <AZ>'
 */
public class ZkNamingServiceImpl implements NamingService {

    private static final Logger log = LoggerFactory.getLogger(ZkNamingServiceImpl.class);

    private final ServiceDiscovery<NodeType> naming;
    private final ServiceDiscovery<NodeType> discovery;
    private final CuratorFramework client;

    public static final JsonInstanceSerializer<NodeType> serializer = new JsonInstanceSerializer<NodeType>(NodeType.class);

    private static CuratorFramework mkClient(String address) throws InterruptedException {
        log.info("zk mkClient: {}", address);
        final RetryPolicy retryPolicy ;
        retryPolicy = new ExponentialBackoffRetry(1000, 3);
        //retryPolicy = new org.apache.curator.retry.RetryNTimes(3, 1000);
        CuratorFramework client = CuratorFrameworkFactory.newClient(address, retryPolicy);
        client.getUnhandledErrorListenable().addListener(new MyUnhandledErrorListener());
        client.getConnectionStateListenable().addListener(new MyConnectionStateListener());
        log.info("zk mkClient start...");
        client.start();
        Thread.sleep(1000);
        if(! client.getZookeeperClient().isConnected())
            throw new ExceptionInInitializerError("unable to get initial zk connection...");
        return client;
    }

    private static class MyUnhandledErrorListener implements UnhandledErrorListener {

        @Override
        public void unhandledError(String message, Throwable e) {
            log.error("unhandledError: {}", e.getMessage());
            System.exit(-1);
        }
    }
    private static class MyConnectionStateListener implements ConnectionStateListener {
        @Override
        public void stateChanged(CuratorFramework client, ConnectionState newState) {
            log.info("connection stateChanged: {}", newState);
        }
    }

    public ZkNamingServiceImpl(String address, int az) throws Exception {
        this(mkClient(address), az);
    }

    public ZkNamingServiceImpl(CuratorFramework root, int az) throws Exception {

        this.client = root;

        try {
            root.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath("/urmia/" + az + "/naming");
        } catch (KeeperException.NodeExistsException nee) {
            //log.info("skipping to parent create. already exists: {}", "/urmia/" + az + "/naming");
        }

        try {
            root.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath("/urmia/" + az + "/discovery");
        } catch (KeeperException.NodeExistsException nee) {
            //log.info("skipping to parent create. already exists: {}", "/urmia/" + az + "/discovery");
        }

        naming = ServiceDiscoveryBuilder
                .builder(NodeType.class)
                .client(root)
                .basePath("/urmia/" + az + "/naming")
                .serializer(serializer)
                .build();

        naming.start();

        discovery = ServiceDiscoveryBuilder
                .builder(NodeType.class)
                .client(root)
                .basePath("/urmia/" + az + "/discovery")
                .serializer(serializer)
                .build();

        discovery.start();

    }

    static final Comparator<ServiceInstance<NodeType>> nodeTypeComparator = new Comparator<ServiceInstance<NodeType>>() {
        public int compare(ServiceInstance<NodeType> o1, ServiceInstance<NodeType> o2) {
            int c;
            // type
            c = o1.getPayload().ordinal() - o2.getPayload().ordinal();
            if(c != 0) return c;
            // host
            c = o1.getAddress().compareTo(o2.getAddress());
            if(c != 0) return c;
            // port
            c = o1.getPort() - o2.getPort();
            if(c != 0) return c;

            return o1.getId().compareTo(o2.getId());
        }
    };

    @Override
    public final List<ServiceInstance<NodeType>> list(NodeType type, Predicate<ServiceInstance<NodeType>> p) throws Exception {
        return list(type).filter(p).toSortedList(nodeTypeComparator);
    }

    @Override
    public final ServiceInstance<NodeType> get(NodeType type, String id) throws Exception {
        return naming.queryForInstance(type.name(), id);
    }

    private FluentIterable<ServiceInstance<NodeType>> list(NodeType type) throws Exception {
        return FluentIterable
                .from(naming.queryForInstances(type.name()));
                //.filter(fixed());
    }

    private int suggestPort(NodeType type, String host) throws Exception {
        List<ServiceInstance<NodeType>> l = list(type, onHost(host));
        int port = type.defaultPort + l.size();
        log.info("suggestPort of type: {}, on host: {}, size: {} --> {}", type, host, l.size(), port);
        return port;
    }

    @Override
    public ServiceInstanceBuilder<NodeType> builder(NodeType type, ServiceType serviceType) throws Exception {
        return ServiceInstance
                .<NodeType>builder()
                .address(getLocalHostName())
                .payload(type)
                .name(type.name())
                .port(suggestPort(type, getLocalHostName()))
                .serviceType(serviceType);
    }

    @Override
    public List<NodeType> queryTypes() throws Exception {
        return FluentIterable
                .from(naming.queryForNames())
                .transform(nameToType())
                .filter(Predicates.notNull())
                .toList();
    }

    private Function<String, NodeType> nameToType() {
        return new Function<String, NodeType>() {
            public NodeType apply(String input) {
                try {
                    return NodeType.valueOf(input);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        };
    }

    private Predicate<ServiceInstance<NodeType>> onHost(final String host) {
        return new Predicate<ServiceInstance<NodeType>>() {
            public boolean apply(ServiceInstance<NodeType> input) {
                return host.equalsIgnoreCase(input.getAddress());
            }
        };
    }

    @Override
    public void remove(ServiceInstance<NodeType> si) throws Exception {
        log.info("remove: {}", si);
        Preconditions.checkArgument(ServiceType.DYNAMIC != si.getServiceType());
        naming.unregisterService(si);
    }

    @Override
    public void add(ServiceInstance<NodeType> si) throws Exception {
        log.info("add: {}", si);
        Preconditions.checkArgument(ServiceType.DYNAMIC != si.getServiceType());
        naming.registerService(si);
    }

    @Override
    public int getRegisteredOnHostCount(NodeType type, String host) throws Exception {
        return list(type, onHost(host)).size();
    }

    // -- discovery
    @Override
    public ServiceInstance<NodeType> discover(NodeType type, String id) throws Exception {
        return discovery.queryForInstance(type.name(), id);
    }

    static <T> List<T> shuffle(List<T> input, int count) {
        if(input == null || count >= input.size()) return input;
        final List<T> shuffled = new LinkedList<T>(input);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, count);
    }

    @Override
    public List<ServiceInstance<NodeType>> suggestStorage(int durability) throws Exception {
        return shuffle(Lists.newArrayList(discovery.queryForInstances(NodeType.ODS.name())), durability);
    }

    private ServiceInstance<NodeType> dynamic(ServiceInstance<NodeType> si) throws Exception {
        if(si.getServiceType() == ServiceType.DYNAMIC) return si;

        return ServiceInstance.<NodeType>builder()
                .serviceType(ServiceType.DYNAMIC)
                .port(si.getPort())
                .id(si.getId())
                .payload(si.getPayload())
                .name(si.getName())
                .address(si.getAddress())
                .uriSpec(si.getUriSpec())
                .build();
    }

    @Override
    public void register(ServiceInstance<NodeType> si) throws Exception {
        log.info("register: {}", si);
        if(si == null) return;
        deregister(si);
        discovery.registerService(dynamic(si));
    }

    @Override
    public void deregister(ServiceInstance<NodeType> si) throws Exception {
        log.info("deregister: {}", si);
        if(si == null) return;
        if(client == null) return;
        if(client.getZookeeperClient().isConnected())
        //Preconditions.checkArgument(ServiceType.DYNAMIC == si.getServiceType());
            discovery.unregisterService(si);
    }

    private boolean isUp(ServiceInstance<NodeType> si) throws Exception {
        return discover(si.getPayload(), si.getId()) != null;
    }

    @Override
    public Optional<ServiceInstance<NodeType>> getOfType(NodeType t, String host, int order) throws Exception {
        List<ServiceInstance<NodeType>> l = list(t, onHost(host));
        if(l == null || l.isEmpty() || l.size() <= order) return Optional.absent();
        return Optional.fromNullable(l.get(order));

    }

    public int getRunningCount(ServiceInstance si) throws Exception {
        FluentIterable<ServiceInstance<NodeType>> onMyHost = FluentIterable
                .from(discovery.queryForInstances(si.getName()))
                .filter(onHost(si.getAddress()));
        log.info("getRunningCount for {} onMyHost: {}", si, onMyHost);
        return onMyHost.isEmpty() ? 0 : onMyHost.size() /*- 1*/;
    }

    private static String getLocalHostName() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
        /*
        // todo: make a gethostname(3) syscall
        if (System.getProperty("os.name").startsWith("Windows"))
            return System.getenv("COMPUTERNAME");
        return System.getenv("HOSTNAME");
        */
    }

    @Override
    public Optional<ServiceInstance<NodeType>> whoAmI(NodeType t, boolean autoRegister) throws Exception {
        String host = getLocalHostName();
        log.info("my host: {}", host);
        List<ServiceInstance<NodeType>> onMyHost = list(t, onHost(host));

        if(onMyHost.isEmpty()) // todo: register if autoRegister
            return Optional.absent();

        /*
        if(onMyHost.size() == 1)
            return Optional.fromNullable(onMyHost.get(0));
        */

        log.debug("checking whoAmI against: {}", onMyHost);

        for(ServiceInstance<NodeType> si : onMyHost) {
            if (! isUp(si)) {
                log.info("good. this instance is not up: {}", si);
                return Optional.of(si);
            } else {
                log.info("trying next one. this instance is up: {}", si);
            }
        }

        return Optional.absent();
    }


}
