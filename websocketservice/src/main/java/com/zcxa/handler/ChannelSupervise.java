package com.zcxa.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChannelSupervise {

    private static ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private static ConcurrentMap<String, ChannelId> channelMap = new ConcurrentHashMap<>();

    public static void addChannel(Channel channel){
        group.add(channel);
        channelMap.put(channel.id().asShortText(),channel.id());
    }

    public static void removeChannel(Channel channel){
        group.remove(channel);
        channelMap.remove(channel.id().asShortText());
    }

    public static Channel findChannel(String id){
        return group.find(channelMap.get(id));
    }

    public static void send2All(TextWebSocketFrame txf){
        group.writeAndFlush(txf);
    }

}
