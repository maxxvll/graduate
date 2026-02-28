package com.maxxvll.common.event;

import com.maxxvll.domain.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 聊天消息事件（用于 Kafka 异步处理）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private ChatMessage message; // 消息实体
    private Boolean isOffline;   // 是否为离线消息
    private String receiverId;   // 接收人ID（单聊：用户ID；群聊：群ID）
}