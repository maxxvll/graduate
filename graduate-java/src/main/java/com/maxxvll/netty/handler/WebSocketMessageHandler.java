package com.maxxvll.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import java.util.Map;

/**
 * WebSocket 消息处理器接口
 *
 * @author backend-friend
 */
public interface WebSocketMessageHandler {

    /**
     * 获取支持的消息类型
     *
     * @return 消息类型
     */
    String getType();

    /**
     * 处理消息
     *
     * @param ctx ChannelHandlerContext
     * @param data 消息数据
     * @return 是否处理成功
     */
    boolean handle(ChannelHandlerContext ctx, Map<String, Object> data);

    /**
     * 获取处理器的优先级（数字越小优先级越高）
     *
     * @return 优先级
     */
    default int getOrder() {
        return 100;
    }
}
