package com.maxxvll.common.event;

import com.maxxvll.domain.ChatMessage;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 会话更新事件
 * 用于解耦 ChatMessageService 和 ChatSessionService 之间的循环依赖
 *
 * 当消息发送、撤回等操作发生时，发布此事件通知会话更新
 */
@Getter
public class SessionUpdateEvent extends ApplicationEvent {

    /**
     * 事件类型
     */
    private final SessionUpdateType type;

    /**
     * 相关的消息（如果适用）
     */
    private final ChatMessage message;

    /**
     * 会话ID
     */
    private final String sessionId;

    /**
     * 用户ID（用于特定用户操作，如清除未读数）
     */
    private final String userId;

    /**
     * 会话类型
     */
    private final Integer sessionType;

    private SessionUpdateEvent(Builder builder) {
        super(builder.source);
        this.type = builder.type;
        this.message = builder.message;
        this.sessionId = builder.sessionId;
        this.userId = builder.userId;
        this.sessionType = builder.sessionType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Object source;
        private SessionUpdateType type;
        private ChatMessage message;
        private String sessionId;
        private String userId;
        private Integer sessionType;

        public Builder source(Object source) {
            this.source = source;
            return this;
        }

        public Builder type(SessionUpdateType type) {
            this.type = type;
            return this;
        }

        public Builder message(ChatMessage message) {
            this.message = message;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder sessionType(Integer sessionType) {
            this.sessionType = sessionType;
            return this;
        }

        public SessionUpdateEvent build() {
            return new SessionUpdateEvent(this);
        }
    }

    /**
     * 会话更新类型
     */
    public enum SessionUpdateType {
        /** 消息发送后更新会话 */
        MESSAGE_SENT,
        /** 消息撤回后更新会话 */
        MESSAGE_REVOKED,
        /** 消息编辑后更新会话 */
        MESSAGE_EDITED,
        /** 清除未读数 */
        CLEAR_UNREAD,
        /** 刷新最后一条消息 */
        REFRESH_LAST_MESSAGE
    }
}
