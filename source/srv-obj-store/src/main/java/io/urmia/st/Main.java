package io.urmia.st;

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
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.urmia.naming.model.NodeType;
import io.urmia.naming.service.NamingService;
import io.urmia.naming.service.ZkNamingServiceImpl;
import io.urmia.util.ArgumentParseUtil;
import org.apache.curator.x.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final int AZ = 1;

    private static NamingService ns;
    private static ServiceInstance<NodeType> me;

    public static void main(String[] args) throws Exception {

        final int port;
        final String base;

        boolean autoRegister = ArgumentParseUtil.isAutoRegister(args);
        String zkURL = ArgumentParseUtil.getZooKeeperURL(args);

        log.info("starting with zk at: {}, auto register: {}", zkURL, autoRegister);

        ns = new ZkNamingServiceImpl(zkURL, AZ);

        Optional<ServiceInstance<NodeType>> meOpt = ns.whoAmI(NodeType.ODS, autoRegister);

        if(!meOpt.isPresent()) {
            System.err.println("unable to find my instance. use auto register or cli-admin to add my node");
            System.exit(1);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        EventLoopGroup bossGroup = new NioEventLoopGroup(/*1*/);
        //EventLoopGroup bossGroup = new EpollEventLoopGroup(1);
        //EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            me = meOpt.get();

            log.info("my service instance: {}", me);

            ns.register(me);

            base = me.getUriSpec().getParts().get(0).getValue();
            port = me.getPort();

            if(! (new File(base).isDirectory())) {
                System.err.println("base in not directory: " + base);
                return;
            }

            int nHeapArena = 1;
            int nDirectArena= 1;
            int pageSize=/*8192*/4096;
            int maxOrder=1;


            // http://normanmaurer.me/presentations/2014-facebook-eng-netty/slides.html#14.0
            ServerBootstrap b = new ServerBootstrap();

            b
                    .group(bossGroup)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.AUTO_READ, false)
                    .childOption(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(true, nHeapArena, nDirectArena, pageSize, maxOrder))
                    .childHandler(new HttpUploadServerInitializer(base));


            Channel ch = b.bind(port).sync().channel();
            log.info("object storage Server (ODS) at port: {}", port);

            System.err.println("starting ODS " + me.getId() + " on port: " + port + ", base: " + base);

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            //workerGroup.shutdownGracefully();
        }
    }

    private static class HttpUploadServerInitializer extends ChannelInitializer<SocketChannel> {

        int maxInitialLineLength = 4096;
        int maxHeaderSize = 8192;
        int maxChunkSize = 64 * 1024;
        boolean validateHeaders = false;

        private final String base;

        private HttpUploadServerInitializer(String base) {
            this.base = base;
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline()
                    //.addLast("logging", new io.netty.handler.logging.LoggingHandler(io.netty.handler.logging.LogLevel.INFO))
                    .addLast("decoder", new HttpRequestDecoder(/*maxInitialLineLength, maxHeaderSize, maxChunkSize, validateHeaders*/))
                    .addLast("encoder", new HttpResponseEncoder())
                    .addLast("streamer", new ChunkedWriteHandler()) // downloads
                    .addLast("handler", new StorageServerHandler(base))
            ;
        }
    }

    private static class ShutdownHook extends Thread {
        @Override
        public void run() {
            log.info("ODS shutting down...PID: {}", getPID());
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
