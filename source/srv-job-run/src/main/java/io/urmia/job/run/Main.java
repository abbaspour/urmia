package io.urmia.job.run;

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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil;
import com.google.common.base.Optional;
import io.urmia.naming.model.NodeType;
import io.urmia.naming.service.NamingService;
import io.urmia.naming.service.ZkNamingServiceImpl;
import io.urmia.util.ArgumentParseUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceInstanceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final Executor executor = Executors.newCachedThreadPool();

    private static NamingService ns;
    private static ServiceInstance<NodeType> me;
    private static ServiceInstance<NodeType> meWithOdsId;

    private static String mountPoint;
    private static String id;
    private static final int AZ = 1;

    private static ServiceInstance<NodeType> meWithId(ServiceInstance<NodeType> me, String id) throws Exception {
        ServiceInstanceBuilder<NodeType> b = ServiceInstance.<NodeType>builder();
        b.id(id)
                .address(me.getAddress())
                .name(me.getName())
                .payload(me.getPayload())
                .port(me.getPort())
                //.sslPort(me.getSslPort())
                .serviceType(me.getServiceType())
                .uriSpec(me.getUriSpec());
        return b.build();
    }

    public static void main(String[] args) throws Exception {

        LoggerContext loggerContext = ((ch.qos.logback.classic.Logger)log).getLoggerContext();
        URL mainURL = ConfigurationWatchListUtil.getMainWatchURL(loggerContext);
        System.err.println(mainURL);
        // or even
        log.info("Logback used '{}' as the configuration file.", mainURL);

        CuratorFramework client = null;
        PathChildrenCache cache = null;


        boolean autoRegister = ArgumentParseUtil.isAutoRegister(args);
        String zkURL = ArgumentParseUtil.getZooKeeperURL(args);

        log.info("starting with zk at: {}, auto register: {}", zkURL, autoRegister);

        ns = new ZkNamingServiceImpl(zkURL, AZ);

        Optional<ServiceInstance<NodeType>> meOpt = ns.whoAmI(NodeType.JRS, autoRegister);

        if(!meOpt.isPresent()) {
            System.err.println("unable to find my instance. use auto register or cli-admin to add my node");
            System.exit(1);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        try {
            me = meOpt.get();

            log.info("my service instance: {}", me);

            ServiceInstance<NodeType> ods = getMyMatchingODSInstance(me);

            id = getId(ods);//me.getId();

            //ns.register(me);
            meWithOdsId = meWithId(me, id);
            ns.register(meWithOdsId);

            mountPoint = getMountPoint(ods);

            String zkPath = getPath(AZ, id);

            System.err.println("instance id: " + me.getId());
            System.err.println("ODS q id   : " + id);
            System.err.println("zk  path   : " + zkPath);
            System.err.println("mount point: " + mountPoint);


            client = CuratorFrameworkFactory.newClient(zkURL, new ExponentialBackoffRetry(1000, 3));
            client.start();

            cache = new PathChildrenCache(client, zkPath, true);
            cache.start();

            cache.getListenable().addListener(new ChildrenListener(), executor);

            log.info("listening path: {}", zkPath);

            loopForever();

        } finally {
            System.err.println("finally block...");
            ns.deregister(meWithOdsId);
            CloseableUtils.closeQuietly(cache);
            CloseableUtils.closeQuietly(client);
        }
    }

    private final static Object lock = new Object();

    private static void loopForever() {
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException ignored) {
                    log.info("interrupted...");
                }
            }
        }
    }

    private static String getPath(int az, String location) throws UnknownHostException {
        return "/urmia/" +  az + "/" + location + "/jobs";
    }

    private static class ChildrenListener implements PathChildrenCacheListener {

        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
            switch (event.getType()) {
                case CHILD_ADDED:
                    log.info("start job process for path: {}", event.getData().getPath());
                    executor.execute(new JobProcess(id, client, event.getData().getPath(), mountPoint, ns));
                    break;
                default:
                    log.info("ignore child event: {}", event.getType());
                    break;
            }

        }
    }

    private static class ShutdownHook extends Thread {
        @Override
        public void run() {
            log.info("job runner shutting down... location: {}, PID: {}", id, getPID());
            try {
                ns.deregister(me);
            } catch (Exception e) {
                log.error("unable to deregister. error: {}", e.getMessage());
            }
        }

        public static long getPID() {
            String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            return Long.parseLong(processName.split("@")[0]);
        }
    }

    private static ServiceInstance<NodeType> getMyMatchingODSInstance(ServiceInstance<NodeType> me) throws Exception {
        int rc = ns.getRunningCount(me);
        System.err.println("my running count: " + rc);
        Optional<ServiceInstance<NodeType>> ods = ns.getOfType(NodeType.ODS, me.getAddress(), rc);
        if(! ods.isPresent()) throw new RuntimeException("unable to find matching ODS at: " + me.getAddress() + ", index: " + rc);
        return ods.get();
    }

    private static String getMountPoint(ServiceInstance<NodeType> ods) throws Exception {
        return ods.getUriSpec().getParts().get(0).getValue();
    }

    private static String getId(ServiceInstance<NodeType> ods) throws Exception {
        return ods.getId();
    }

}
