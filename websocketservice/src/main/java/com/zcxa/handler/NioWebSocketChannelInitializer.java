package com.zcxa.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @author zzk
 * @since 1.0.0
 */
public class NioWebSocketChannelInitializer extends ChannelInitializer<SocketChannel> {


    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        //设置日志监听器
        ch.pipeline().addLast("logging",new LoggingHandler("DEBUG"));

        //设置解码器
        ch.pipeline().addLast("http-code",new HttpServerCodec());

        //聚合器,websocket
        ch.pipeline().addLast("aggregator",new HttpObjectAggregator(65536));

        //大数据的分区传输
        ch.pipeline().addLast("http-chunked",new ChunkedWriteHandler());

        //自定义业务
        ch.pipeline().addLast("handler",new NioWebSocketHandler());
    }
}
