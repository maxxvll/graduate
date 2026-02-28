package com.maxxvll.component;

import com.maxxvll.common.event.ChatMessageEvent;
import com.maxxvll.service.ChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

@Slf4j
@Component
public class ChatMessageConsumer {

    @Resource
    private ChatMessageService chatMessageService;

    // 假设你有一个 WebSocket 服务用于在线推送
    // @Resource
    // private WebSocketService webSocketService;

    @KafkaListener(topics = "chat-message-topic", groupId = "chat-message-group")
    public void handleChatMessage(ConsumerRecord<String, ChatMessageEvent> record) {
        ChatMessageEvent event = record.value();
        log.info("收到聊天消息事件，messageId: {}", event.getMessage().getId());

        try {
            // 1. 判断接收方是否在线（需结合 WebSocket 连接状态判断）
            boolean isReceiverOnline = checkReceiverOnline(event.getReceiverId(), event.getMessage().getSessionType());

            if (isReceiverOnline) {
                // 2. 在线：通过 WebSocket 实时推送
                // webSocketService.pushMessage(event.getReceiverId(), event.getMessage());
                log.info("接收方在线，推送消息，messageId: {}", event.getMessage().getId());
            } else {
                // 3. 离线：标记为离线消息（需在数据库中更新状态）
                chatMessageService.markMessageAsOffline(event.getMessage().getId());
                log.info("接收方离线，标记为离线消息，messageId: {}", event.getMessage().getId());
            }
        } catch (Exception e) {
            log.error("处理聊天消息事件失败，messageId: {}", event.getMessage().getId(), e);
            // 可选：重试机制或存入死信队列
        }
    }

    /**
     * 判断接收方是否在线（示例方法，需结合实际 WebSocket 实现）
     */
    private boolean checkReceiverOnline(String receiverId, Integer sessionType) {
        // 单聊：判断用户是否在线
        // 群聊：遍历群成员，分别处理在线推送和离线存储
        // 这里简化处理，返回 false 模拟离线
        return false;
    }
}