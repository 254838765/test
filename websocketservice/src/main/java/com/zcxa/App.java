package com.zcxa;

import com.zcxa.handler.NioWebSocketChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.logging.Logger;


/**
 * @author zzk
 * @since 1.0.0
 */
public class App {

    private static final Logger logger = Logger.getLogger(App.class.getName());


    public static void main(String[] args) {
        //路径为jar 路径
        // logger.info(App.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        // start(Integer.valueOf(args[0]));
        start(8080);
    }

    /**
     * 启用服务
     */
    private static void start(int port) {
        logger.info("starting service...");
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup work = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boss, work);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.childHandler(new NioWebSocketChannelInitializer());
        try {
            Channel channel = bootstrap.bind(port).sync().channel();
            logger.info("Service started successfully；" + channel);
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            boss.shutdownGracefully();
            work.shutdownGracefully();
            logger.warning("Service is closed");
        }
    }
}