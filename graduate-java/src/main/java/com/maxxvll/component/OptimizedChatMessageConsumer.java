package com.maxxvll.component;

import com.alibaba.fastjson2.JSON;
import com.maxxvll.common.annotation.PerformanceMonitor;
import com.maxxvll.common.constants.LoggingConstants;
import com.maxxvll.common.event.ChatMessageEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.kafka.chat.consumer-mode", havingValue = "batch", matchIfMissing = true)
public class OptimizedChatMessageConsumer {

    @Resource
    private ChatPushSupport chatPushSupport;

    @Resource
    private NettyChannelManager nettyChannelManager;

    @PerformanceMonitor(
            warnThresholdMs = LoggingConstants.KAFKA_CONSUME_THRESHOLD_MS,
            description = "batch chat message consumer"
    )
    @KafkaListener(
            topics = "chat-message-topic",
            groupId = "chat-message-group",
            containerFactory = "batchKafkaListenerFactory"
    )
    public void handleChatMessagesBatch(@Payload List<ConsumerRecord<String, ChatMessageEvent>> records) {
        long startTime = System.currentTimeMillis();
        log.info("{} Batch consume started, count={}",
                LoggingConstants.PREFIX_KAFKA_CONSUME,
                records.size());

        try {
            List<ChatMessageEvent> events = records.stream()
                    .map(ConsumerRecord::value)
                    .toList();

            List<ChatMessageEvent> singleChats = events.stream()
                    .filter(event -> event.getMessage() != null && Integer.valueOf(1).equals(event.getMessage().getSessionType()))
                    .toList();
            if (!singleChats.isEmpty()) {
                handleBatchSingleChat(singleChats);
            }

            List<ChatMessageEvent> groupChats = events.stream()
                    .filter(event -> event.getMessage() != null && Integer.valueOf(2).equals(event.getMessage().getSessionType()))
                    .toList();
            if (!groupChats.isEmpty()) {
                handleBatchGroupChat(groupChats);
            }

            long costTime = System.currentTimeMillis() - startTime;
            log.info("{} Batch consume finished, count={}, costTime={}ms, avgTime={}ms",
                    LoggingConstants.PREFIX_KAFKA_CONSUME,
                    records.size(),
                    costTime,
                    costTime / Math.max(1, records.size()));
        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("{} Batch consume failed, count={}, costTime={}ms",
                    LoggingConstants.PREFIX_KAFKA_CONSUME,
                    records.size(),
                    costTime,
                    e);
        }
    }

    private void handleBatchSingleChat(List<ChatMessageEvent> events) {
        int onlineCount = 0;
        int offlineCount = 0;

        for (ChatMessageEvent event : events) {
            boolean pushed = chatPushSupport.pushSingleMessage(event.getMessage(), event.getReceiverId());
            if (pushed) {
                onlineCount++;
            } else {
                offlineCount++;
            }
        }

        log.info("Single chat batch finished, messages={}, online={}, offline={}",
                events.size(), onlineCount, offlineCount);
    }

    private void handleBatchGroupChat(List<ChatMessageEvent> events) {
        Map<Long, List<ChatMessageEvent>> groupedByGroupId = events.stream()
                .map(this::toResolvedGroupEvent)
                .filter(resolved -> resolved.groupId() != null)
                .collect(Collectors.groupingBy(
                        ResolvedGroupEvent::groupId,
                        LinkedHashMap::new,
                        Collectors.mapping(ResolvedGroupEvent::event, Collectors.toList())
                ));

        int invalidEvents = events.size() - groupedByGroupId.values().stream().mapToInt(List::size).sum();
        if (invalidEvents > 0) {
            log.warn("Skip invalid group chat events, count={}", invalidEvents);
        }

        if (groupedByGroupId.isEmpty()) {
            return;
        }

        Set<String> onlineUsers = nettyChannelManager.getOnlineUsersSnapshot();
        Map<Long, List<String>> membersByGroupId = chatPushSupport.getActiveMemberIdsByGroupIds(groupedByGroupId.keySet());

        int totalMessages = 0;
        int totalRecipients = 0;
        int totalOnlinePushes = 0;
        int totalOfflineRecipients = 0;

        for (Map.Entry<Long, List<ChatMessageEvent>> entry : groupedByGroupId.entrySet()) {
            Long groupId = entry.getKey();
            List<String> memberIds = membersByGroupId.getOrDefault(groupId, List.of());
            if (memberIds.isEmpty()) {
                log.warn("Skip group push because no active members were found, groupId={}", groupId);
                continue;
            }

            for (ChatMessageEvent event : entry.getValue()) {
                ChatPushSupport.GroupPushStats stats = chatPushSupport.pushGroupMessage(
                        event.getMessage(),
                        JSON.toJSONString(event.getMessage()),
                        memberIds,
                        onlineUsers
                );
                totalMessages++;
                totalRecipients += stats.totalRecipients();
                totalOnlinePushes += stats.onlineCount();
                totalOfflineRecipients += stats.offlineCount();
            }
        }

        log.info("Group chat batch finished, messages={}, recipients={}, online={}, offline={}",
                totalMessages, totalRecipients, totalOnlinePushes, totalOfflineRecipients);
    }

    private ResolvedGroupEvent toResolvedGroupEvent(ChatMessageEvent event) {
        Long groupId = chatPushSupport.resolveGroupId(event);
        if (groupId == null) {
            log.warn("Cannot resolve groupId from event, messageId={}, sessionId={}",
                    event.getMessage().getId(),
                    event.getMessage().getSessionId());
        }
        return new ResolvedGroupEvent(groupId, event);
    }

    private record ResolvedGroupEvent(Long groupId, ChatMessageEvent event) {
    }
}
