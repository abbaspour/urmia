package io.urmia.api;

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
import com.jolbox.bonecp.BoneCPConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.urmia.api.handler.RestApiHandler;
import io.urmia.md.repo.util.JdbcPool;
import io.urmia.md.repo.MetadataRepository;
import io.urmia.md.repo.psql.PsqlMetadataRepositoryImpl;
import io.urmia.md.service.DefaultMetadataServiceImpl;
import io.urmia.md.service.MetadataService;
import io.urmia.naming.model.NodeType;
import io.urmia.naming.service.NamingService;
import io.urmia.naming.service.ZkNamingServiceImpl;
import io.urmia.util.ArgumentParseUtil;
import io.urmia.naming.service.RandomUuidImpl;
import io.urmia.naming.service.Uuid;
import org.apache.curator.x.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static MetadataService mds;
    private static Uuid uuid;

    private static final int AZ = 1;

    private static NamingService ns;
    private static ServiceInstance<NodeType> me;

    public static void main(String[] args) throws Exception {

        boolean autoRegister = ArgumentParseUtil.isAutoRegister(args);
        String zkURL = ArgumentParseUtil.getZooKeeperURL(args);

        log.info("starting with zk at: {}, auto register: {}", zkURL, autoRegister);

        ns = new ZkNamingServiceImpl(zkURL, AZ);

        Optional<ServiceInstance<NodeType>> meOpt = ns.whoAmI(NodeType.MDS, autoRegister);

        if(!meOpt.isPresent()) {
            System.err.println("unable to find my instance. use auto register or cli-admin to add my node");
            System.exit(1);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        uuid = new RandomUuidImpl();

        //Properties properties = parseArguments(args);
        EventLoopGroup bossGroup = new NioEventLoopGroup(/*1*/);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            me = meOpt.get();

            log.info("my service instance: {}", me);

            BoneCPConfig boneCPConfig = getBoneCPConfig(ns);

            ns.register(me);

            int port = me.getPort();

            JdbcPool pool = new JdbcPool.BoneCPJdbcPool(boneCPConfig);

            MetadataRepository repository = new PsqlMetadataRepositoryImpl(pool);

            mds = new DefaultMetadataServiceImpl(repository);

            // http://normanmaurer.me/presentations/2014-facebook-eng-netty/slides.html#14.0
            ServerBootstrap b = new ServerBootstrap();

            b
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.AUTO_READ, false)
                    .childOption(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(true))
                    .childHandler(new HttpUploadServerInitializer());


            Channel ch = b.bind(port).sync().channel();
            log.info("object metadata API server (MDS) at port: {}", port);

            ch.closeFuture().sync();
        } finally {
            ns.deregister(me);
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static BoneCPConfig getBoneCPConfig(NamingService ns) throws Exception {
        BoneCPConfig config = new BoneCPConfig();

        List<ServiceInstance<NodeType>> dbs = ns.list(NodeType.MDB, Predicates.<ServiceInstance<NodeType>>alwaysTrue());

        if(dbs.isEmpty())
            throw new Exception("unable to find any MDB node");

        ServiceInstance<NodeType> mdb = dbs.get(0);

        log.info("MDB service instance: {}", mdb);

        String host = mdb.getAddress();
        String db = mdb.getUriSpec().getParts().get(0).getValue();
        int port = mdb.getPort();
        String user = "uadmin"; //properties.getProperty("mdb_user", "uadmin");
        String pass = "uadmin"; //properties.getProperty("mdb_pass", "uadmin");
        Integer min = 1; //Integer.parseInt(properties.getProperty("mdb_min", "1"));
        Integer max = 5; //Integer.parseInt(properties.getProperty("mdb_max", "5"));

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        log.info("jdbcUrl: {}", jdbcUrl);

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(pass);
        config.setMinConnectionsPerPartition(min);
        config.setMaxConnectionsPerPartition(max);
        config.setPartitionCount(1);
        config.setLogStatementsEnabled(true);

        return config;
    }

    private static class HttpUploadServerInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline()
                    //.addLast("logging", new io.netty.handler.logging.LoggingHandler(io.netty.handler.logging.LogLevel.INFO))
                    .addLast("decoder", new HttpRequestDecoder())
                    .addLast("encoder", new HttpResponseEncoder())
                    .addLast("api", new RestApiHandler(mds, uuid, ns))
            ;
        }
    }

    private static class ShutdownHook extends Thread {
        @Override
        public void run() {
            log.info("MDS shutting down...PID: {}", getPID());
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

}
