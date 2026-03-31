package com.maxxvll.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * ACK 消息处理器
 *
 * @author backend-friend
 */
@Slf4j
@Component
public class AckMessageHandler implements WebSocketMessageHandler {

    @Override
    public String getType() {
        return "ack";
    }

    @Override
    public boolean handle(ChannelHandlerContext ctx, Map<String, Object> data) {
        log.debug("Received ack message: {}", data);
        // ACK 消息处理逻辑可以在这里实现
        return true;
    }
}
