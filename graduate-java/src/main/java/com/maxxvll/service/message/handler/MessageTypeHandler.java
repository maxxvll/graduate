package com.maxxvll.service.message.handler;

import com.maxxvll.common.enums.MessageType;
import com.maxxvll.domain.ChatMessage;

/**
 * 消息类型处理器接口
 * 使用策略模式处理不同类型的消息
 *
 * @author maxxvll
 * @since 2026-03-31
 */
public interface MessageTypeHandler {

    /**
     * 判断是否支持该消息类型
     *
     * @param type 消息类型
     * @return 是否支持
     */
    boolean supports(MessageType type);

    /**
     * 处理消息（发送前处理）
     *
     * @param message 待处理的消息
     */
    void handleBeforeSend(ChatMessage message);

    /**
     * 获取默认消息内容
     *
     * @return 消息内容
     */
    default String getDefaultContent() {
        return "";
    }

    /**
     * 是否需要文件URL
     *
     * @return 是否需要文件URL
     */
    default boolean requiresFileUrl() {
        return false;
    }
}
