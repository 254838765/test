package com.zcxa.client;

import com.zcxa.handler.NioWebSocketHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;

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
        WebSocketClientHandler handler = new WebSocketClientHandler();
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast("http-code",new HttpClientCodec());
                ch.pipeline().addLast("aggregator",new HttpObjectAggregator(65536));
                ch.pipeline().addLast("handler",handler);
            }
        });

        try {
            WebSocketClientHandshaker handShaker = WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders());
            channel = bootstrap.connect(this.uri.getHost(), this.uri.getPort()).sync().channel();
            handler.setHandShaker(handShaker);
            handler.handshakeFuture().sync();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }



    class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

        WebSocketClientHandshaker handShaker;

        ChannelPromise handshakeFuture;


        public ChannelFuture handshakeFuture() {
            return handshakeFuture;
        }

        public void setHandShaker(WebSocketClientHandshaker handShaker){
            this.handShaker = handShaker;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            handshakeFuture = ctx.newPromise();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            handShaker.handshake(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            logger.warning("Failed to connect to the service " + uri);
        }


        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

            Channel ch = ctx.channel();

            if (!handShaker.isHandshakeComplete()) {
                handShaker.finishHandshake(ch, (FullHttpResponse) msg);
                logger.info("websocket client connected!");
                handshakeFuture.setSuccess();
                return;
            }

            if (msg instanceof FullHttpResponse) {
                FullHttpResponse response = (FullHttpResponse) msg;
                throw new Exception("Unexpected FullHttpResponse (getStatus=" + response.status() + ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
            }

            if(msg instanceof WebSocketFrame){
                WebSocketFrame frame = (WebSocketFrame)msg;
                if (frame instanceof CloseWebSocketFrame) {
                    //关闭
                    return;
                }
                if (frame instanceof PongWebSocketFrame) {
                    //心跳
                    return;
                }
                if(frame instanceof TextWebSocketFrame){
                    logger.info(((TextWebSocketFrame) frame).text());
                    return;
                }
            }
        }

        /**
         * 异常
         * @param ctx
         * @param cause
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            if (!handshakeFuture.isDone()) {
                handshakeFuture.setFailure(cause);
            }
            ctx.close();
        }


    }
}
