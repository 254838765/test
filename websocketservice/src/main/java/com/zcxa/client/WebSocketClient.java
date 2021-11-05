package com.zcxa.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;

import java.net.URI;
import java.util.logging.Logger;

/**
 * @author zzk
 * @since 1.0.0
 */
public class WebSocketClient {

    private static final Logger logger = Logger.getLogger(WebSocketClient.class.getName());

    private static final String PING = "ping";

    private static final String PONG = "pong";

    private final URI uri;

    private Channel channel;

    public WebSocketClient(URI uri) {
        this.uri = uri;
    }

    /**
     * 连接
     */
    public void connect() {
        NioEventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                //解码器
                ch.pipeline().addLast("http-code", new HttpClientCodec());
                //聚合器
                ch.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));
                //自定义处理器
                ch.pipeline().addLast("handler", new WebSocketClientHandler());
            }
        });
        try {
            channel = bootstrap.connect(this.uri.getHost(), this.uri.getPort()).sync().channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    /**
     * 重连
     */
    public void reconnect() {
        connect();
    }

    /**
     * 消息
     *
     * @param text
     */
    public void onMessage(String text) {

    }

    /**
     * 发判断消息
     *
     * @param msg
     */
    public void send(String msg) {
        switch (msg) {
            case PING: {
                WebSocketFrame frame = new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[]{8, 1, 8, 1}));
                channel.writeAndFlush(frame);
            }
            break;
            default: {
                //广播
            }
        }

    }

    /**
     * 关闭
     */
    public void close() throws InterruptedException {
        channel.writeAndFlush(new CloseWebSocketFrame());
        channel.closeFuture().sync();
    }


    class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            logger.warning("Failed to connect to the service "+uri);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
            WebSocketFrame frame = (WebSocketFrame) o;
            Channel ch = channelHandlerContext.channel();
            if (frame instanceof CloseWebSocketFrame) {
                ch.close();
                reconnect();
                return;
            }
            if (frame instanceof PongWebSocketFrame) {
                onMessage(PONG);
                return;
            }
            if (frame instanceof TextWebSocketFrame) {
                onMessage(((TextWebSocketFrame) frame).text());
                return;
            }
        }
    }
}
