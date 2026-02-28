package com.maxxvll.component;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NettyChannelManager {
    private final Map<String, Channel> channelMap=new ConcurrentHashMap<>();
    private final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    public void bindChannel(String key, Channel channel) {
        if(key!=null && channel!=null){
            channelMap.put(key, channel);
            channelGroup.add(channel);
        }

    }
    public Channel getChannel(String key) {
        return channelMap.get(key);
    }
    public void removeChannel(String key) {
        if(key!=null){
            channelMap.remove(key);
            channelGroup.remove(channelMap.remove(key));
        }
    }

}
