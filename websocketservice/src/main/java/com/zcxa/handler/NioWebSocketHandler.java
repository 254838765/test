package com.zcxa.handler;

import com.alibaba.fastjson.JSONObject;
import com.zcxa.model.Message;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


/**
 * @author zzk
 * @since 1.0.0
 */
public class NioWebSocketHandler extends SimpleChannelInboundHandler<Object> {


    private static final Logger logger = Logger.getLogger(NioWebSocketHandler.class.getName());

    private static final String PING = "ping";

    private static final String PONG = "pong";

    private static final String UPGRADE = "Upgrade";

    private static final String WEB_SOCKET = "websocket";

    private static final String ALL = "ALL";

    private static final int REQUEST_OK = 200;

    private static final String FORMAT_EXPRESSION = "message format error";


    private WebSocketServerHandshaker handShaker;

    private static Map<String, String> user = new ConcurrentHashMap<>();


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("A client joins the connection " + ctx.channel());
        ChannelSupervise.addChannel(ctx.channel());
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Client disconnected " + ctx.channel());
        ChannelSupervise.removeChannel(ctx.channel());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        //是否关闭指令
        if (frame instanceof CloseWebSocketFrame) {
            handShaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }

        //是否心跳指令
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        if (frame instanceof TextWebSocketFrame) {
            String text = ((TextWebSocketFrame) frame).text();
            if (PING.equals(text)) {
                TextWebSocketFrame tws = new TextWebSocketFrame(PONG);
                // 回发
                ctx.channel().writeAndFlush(tws);
                return;
            }
            Message msg = JSONObject.parseObject(text, Message.class);
            if (null != msg) {
                TextWebSocketFrame tws = new TextWebSocketFrame(msg.getMsg());
                switch (msg.getTo()) {
                    case ALL: {
                        //群发
                        ChannelSupervise.send2All(tws);
                    }
                    break;
                    default: {
                        //发给指定端
                        ChannelSupervise.findChannel(user.get(msg.getTo())).writeAndFlush(tws);
                    }
                    break;
                }
            } else {
                TextWebSocketFrame tws = new TextWebSocketFrame(FORMAT_EXPRESSION);
                //回发
                ctx.channel().writeAndFlush(tws);
                return;
            }
        } else {
            TextWebSocketFrame tws = new TextWebSocketFrame(FORMAT_EXPRESSION);
            // 回发
            ctx.channel().writeAndFlush(tws);
            return;
        }
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (!request.decoderResult().isSuccess() || (!WEB_SOCKET.equals(request.headers().get(UPGRADE)))) {
            sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory("ws://host:port/{clientId}", null, false);
        handShaker = factory.newHandshaker(request);
        if (null == handShaker) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            user.put(request.uri().substring(1), ctx.channel().id().asShortText());
            handShaker.handshake(ctx.channel(), request);
        }
    }


    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest request, DefaultFullHttpResponse response) {
        if (response.status().code() != REQUEST_OK) {
            ByteBuf buf = Unpooled.copiedBuffer(response.status().toString(), CharsetUtil.UTF_8);
            response.content().writeBytes(buf);
            buf.release();
        }
        ChannelFuture f = ctx.channel().writeAndFlush(response);
        if (!HttpUtil.isKeepAlive(request) || response.status().code() != REQUEST_OK) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
