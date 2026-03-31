package com.maxxvll.netty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 待确认的消息
 * 用于实现消息 ACK 机制，确保消息可靠送达
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingMessage {

    /**
     * 消息唯一ID（UUID）
     */
    private String messageId;

    /**
     * 目标用户ID
     */
    private String targetUserId;

    /**
     * 消息内容（JSON 字符串）
     */
    private String content;

    /**
     * 消息类型（chat, signal, typing, etc.）
     */
    private String messageType;

    /**
     * 消息创建时间戳
     */
    private long createdAt;

    /**
     * 最后一次发送时间戳
     */
    private long lastSentAt;

    /**
     * 重试次数
     */
    private int retryCount;

    /**
     * 最大重试次数
     */
    private int maxRetries;

    /**
     * 超时时间（毫秒）
     */
    private long timeoutMillis;

    /**
     * 消息状态
     */
    private MessageStatus status;

    /**
     * 消息状态枚举
     */
    public enum MessageStatus {
        PENDING,    // 等待确认
        ACKED,      // 已确认
        TIMEOUT,    // 超时
        FAILED      // 失败（重试次数耗尽）
    }

    /**
     * 检查消息是否超时
     */
    public boolean isTimeout() {
        return (System.currentTimeMillis() - lastSentAt) > timeoutMillis;
    }

    /**
     * 检查是否可以重试
     */
    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
        this.lastSentAt = System.currentTimeMillis();
    }

    /**
     * 获取已用时间（毫秒）
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - createdAt;
    }
}
