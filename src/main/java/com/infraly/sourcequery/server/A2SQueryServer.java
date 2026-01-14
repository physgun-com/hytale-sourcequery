package com.infraly.sourcequery.server;

import com.hypixel.hytale.logger.HytaleLogger;
import com.infraly.sourcequery.protocol.A2SPacketHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.IoEventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioDatagramChannel;

public final class A2SQueryServer {

    private final int port;
    private final HytaleLogger logger;
    private IoEventLoopGroup group;
    private Channel channel;

    public A2SQueryServer(int port, HytaleLogger logger) {
        this.port = port;
        this.logger = logger;
    }

    public void start() throws InterruptedException {
        group = new MultiThreadIoEventLoopGroup(1, r -> {
            Thread t = new Thread(r, "A2S-Query");
            t.setDaemon(true);
            return t;
        }, NioIoHandler.newFactory());

        channel = new Bootstrap()
                .group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_REUSEADDR, true)
                .handler(new A2SPacketHandler(logger))
                .bind(port)
                .sync()
                .channel();
    }

    public void stop() {
        if (channel != null) {
            channel.close().syncUninterruptibly();
        }
        if (group != null) {
            group.shutdownGracefully().syncUninterruptibly();
        }
    }
}
