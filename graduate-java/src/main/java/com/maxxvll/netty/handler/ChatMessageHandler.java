package com.maxxvll.netty.handler;

import com.maxxvll.netty.WebSocketConstants;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 聊天消息处理器
 *
 * @author backend-friend
 */
@Slf4j
public abstract class ChatMessageHandler implements WebSocketMessageHandler {

    @Override
    public String getType() {
        return "chat";
    }

    @Override
    public int getOrder() {
        return 50;
    }

    @Override
    public boolean handle(ChannelHandlerContext ctx, Map<String, Object> data) {
        String userId = ctx.channel().attr(WebSocketConstants.USER_ID_KEY).get();
        if (userId == null) {
            log.warn("Unauthorized chat message, userId=null");
            return false;
        }

        return handleChatMessage(ctx, userId, data);
    }

    /**
     * 处理聊天消息
     *
     * @param ctx ChannelHandlerContext
     * @param userId 用户ID
     * @param data 消息数据
     * @return 是否处理成功
     */
    protected abstract boolean handleChatMessage(ChannelHandlerContext ctx, String userId, Map<String, Object> data);
}
