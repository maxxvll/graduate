package com.maxxvll.component;

import com.maxxvll.common.annotation.PerformanceMonitor;
import com.maxxvll.common.constants.LoggingConstants;
import com.maxxvll.common.event.ChatMessageEvent;
import com.maxxvll.common.logging.LogHelper;
import com.maxxvll.common.logging.MdcHelper;
import com.maxxvll.domain.ChatMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.kafka.chat.consumer-mode", havingValue = "single")
public class ChatMessageConsumer {

    @Resource
    private ChatPushSupport chatPushSupport;

    @Resource
    private NettyChannelManager nettyChannelManager;

    @PerformanceMonitor(
            warnThresholdMs = LoggingConstants.KAFKA_CONSUME_THRESHOLD_MS,
            description = "single chat message consumer"
    )
    @KafkaListener(topics = "chat-message-topic", groupId = "chat-message-group")
    public void handleChatMessage(ConsumerRecord<String, ChatMessageEvent> record) {
        long startTime = System.currentTimeMillis();
        ChatMessageEvent event = record.value();
        ChatMessage message = event.getMessage();

        MdcHelper.setBusinessKey(String.valueOf(message.getId()));
        try {
            if (isSingleChat(message)) {
                handleSingleChatPush(event);
            } else if (isGroupChat(message)) {
                handleGroupChatPush(event);
            } else {
                log.warn("{} Unsupported session type, messageId={}, sessionType={}",
                        LoggingConstants.PREFIX_KAFKA_CONSUME,
                        message.getId(),
                        message.getSessionType());
            }

            long costTime = System.currentTimeMillis() - startTime;
            LogHelper.logKafkaConsume(record.topic(), record.partition(), record.offset(), costTime);
        } catch (Exception e) {
            LogHelper.logKafkaConsumeException(record.topic(), record.partition(), record.offset(), e);
            log.error("{} Consume chat message failed, topic={}, partition={}, offset={}, messageId={}",
                    LoggingConstants.PREFIX_KAFKA_CONSUME,
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    message.getId(),
                    e);
        } finally {
            MdcHelper.clearContext();
        }
    }

    private void handleSingleChatPush(ChatMessageEvent event) {
        ChatMessage message = event.getMessage();
        boolean pushed = chatPushSupport.pushSingleMessage(message, event.getReceiverId());
        log.info("Single chat push finished, messageId={}, receiverId={}, pushed={}",
                message.getId(), event.getReceiverId(), pushed);
    }

    private void handleGroupChatPush(ChatMessageEvent event) {
        Long groupId = chatPushSupport.resolveGroupId(event);
        if (groupId == null) {
            log.warn("Skip group push because groupId is empty, messageId={}, sessionId={}",
                    event.getMessage().getId(), event.getMessage().getSessionId());
            return;
        }

        List<String> memberIds = chatPushSupport.getActiveMemberIds(groupId);
        Set<String> onlineUsers = nettyChannelManager.getOnlineUsersSnapshot();
        ChatPushSupport.GroupPushStats stats = chatPushSupport.pushGroupMessage(event.getMessage(), memberIds, onlineUsers);

        log.info("Group chat push finished, messageId={}, groupId={}, recipients={}, online={}, offline={}",
                event.getMessage().getId(),
                groupId,
                stats.totalRecipients(),
                stats.onlineCount(),
                stats.offlineCount());
    }

    private boolean isSingleChat(ChatMessage message) {
        return message.getSessionType() != null && message.getSessionType() == 1;
    }

    private boolean isGroupChat(ChatMessage message) {
        return message.getSessionType() != null && message.getSessionType() == 2;
    }
}
