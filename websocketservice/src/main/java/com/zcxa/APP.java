package com.zcxa;

import com.zcxa.handler.NioWebSocketChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.logging.Logger;


public class APP {

    private static final Logger logger = Logger.getLogger(APP.class.getName());


    public static void main(String[] args){
        start(Integer.valueOf(args[0]));
    }

    /**
     * 启用服务
     */
    private static void start(int port){
        logger.info("正启WebSocket服务...");
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup work = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boss,work);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.childHandler(new NioWebSocketChannelInitializer());
        try {
            Channel channel = bootstrap.bind(port).sync().channel();
            logger.info("WebSocket服务启动成功；" + channel);
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            boss.shutdownGracefully();
            work.shutdownGracefully();
            logger.warning("服务已关闭");
        }
    }
}
