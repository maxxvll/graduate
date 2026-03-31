package com.maxxvll.netty;

import com.maxxvll.component.NettyChannelManager;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class PendingMessageManager {

    private static final long DEFAULT_TIMEOUT_MILLIS = 5_000L;
    private static final int DEFAULT_MAX_RETRIES = 3;

    /**
     * 内存缓存上限
     */
    @Value("${ws.pending-message.max-cache-size:10000}")
    private int maxCacheSize;

    /**
     * 消息过期时间（默认 30 分钟）
     */
    @Value("${ws.pending-message.expire-minutes:30}")
    private int expireMinutes;

    /**
     * 统计指标
     */
    private final AtomicLong totalExpired = new AtomicLong(0);
    private final AtomicLong totalRetried = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);

    @Resource
    private NettyChannelManager nettyChannelManager;

    private final Map<String, PendingMessage> pendingMessages = new ConcurrentHashMap<>();
    private final Map<String, Map<String, PendingMessage>> userPendingMessages = new ConcurrentHashMap<>();
    private final DelayQueue<PendingMessageTimeout> timeoutQueue = new DelayQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Thread timeoutWorker;

    @PostConstruct
    public void startTimeoutWorker() {
        running.set(true);
        timeoutWorker = new Thread(null, this::runTimeoutLoop, "pending-message-timeout-worker", 0, false);
        timeoutWorker.setDaemon(true);
        timeoutWorker.start();
    }

    @PreDestroy
    public void stopTimeoutWorker() {
        running.set(false);
        if (timeoutWorker != null) {
            timeoutWorker.interrupt();
        }
    }

    public boolean addPendingMessage(String messageId, String targetUserId, String content, String messageType) {
        if (messageId == null || messageId.isBlank()
                || targetUserId == null || targetUserId.isBlank()
                || content == null || content.isBlank()) {
            log.warn("Skip pending message registration because required fields are missing, targetUserId={}, type={}",
                    targetUserId, messageType);
            return false;
        }

        // 检查缓存上限，如果超过则清理最旧的消息
        if (pendingMessages.size() >= maxCacheSize) {
            cleanupExpiredMessages();
            // 如果清理后仍然超过上限，移除最旧的消息
            if (pendingMessages.size() >= maxCacheSize) {
                String oldestMessageId = pendingMessages.entrySet().stream()
                        .min(Map.Entry.comparingByValue((a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt())))
                        .map(Map.Entry::getKey)
                        .orElse(null);
                if (oldestMessageId != null) {
                    removePendingMessageById(oldestMessageId);
                    log.warn("Pending message cache full, removed oldest message: {}", oldestMessageId);
                }
            }
        }

        long now = System.currentTimeMillis();
        PendingMessage pendingMessage = PendingMessage.builder()
                .messageId(messageId)
                .targetUserId(targetUserId)
                .content(content)
                .messageType(messageType)
                .createdAt(now)
                .lastSentAt(now)
                .retryCount(0)
                .maxRetries(DEFAULT_MAX_RETRIES)
                .timeoutMillis(DEFAULT_TIMEOUT_MILLIS)
                .status(PendingMessage.MessageStatus.PENDING)
                .build();

        pendingMessages.put(messageId, pendingMessage);
        userPendingMessages.computeIfAbsent(targetUserId, ignored -> new ConcurrentHashMap<>())
                .put(messageId, pendingMessage);
        scheduleTimeout(pendingMessage);
        return true;
    }

    /**
     * 根据消息ID移除待确认消息
     */
    private void removePendingMessageById(String messageId) {
        PendingMessage pendingMessage = pendingMessages.remove(messageId);
        if (pendingMessage != null) {
            Map<String, PendingMessage> userMessages = userPendingMessages.get(pendingMessage.getTargetUserId());
            if (userMessages != null) {
                userMessages.remove(messageId);
                if (userMessages.isEmpty()) {
                    userPendingMessages.remove(pendingMessage.getTargetUserId());
                }
            }
        }
    }

    /**
     * 定时清理过期消息
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredMessages() {
        long expireThreshold = System.currentTimeMillis() - (expireMinutes * 60 * 1000L);
        int cleanedCount = 0;

        for (Map.Entry<String, PendingMessage> entry : pendingMessages.entrySet()) {
            PendingMessage message = entry.getValue();
            if (message.getCreatedAt() < expireThreshold && message.getStatus() == PendingMessage.MessageStatus.PENDING) {
                message.setStatus(PendingMessage.MessageStatus.TIMEOUT);
                removePendingMessageById(entry.getKey());
                cleanedCount++;
                totalExpired.incrementAndGet();
            }
        }

        if (cleanedCount > 0) {
            log.info("清理过期待确认消息: count={}, remaining={}", cleanedCount, pendingMessages.size());
        }
    }

    public boolean acknowledgeMessage(String messageId) {
        PendingMessage pendingMessage = pendingMessages.get(messageId);
        if (pendingMessage == null) {
            log.warn("Receive ACK for unknown messageId={}", messageId);
            return false;
        }

        pendingMessage.setStatus(PendingMessage.MessageStatus.ACKED);
        removePendingMessage(pendingMessage);
        log.debug("ACK received, messageId={}, userId={}, elapsedTime={}ms",
                messageId, pendingMessage.getTargetUserId(), pendingMessage.getElapsedTime());
        return true;
    }

    public int getPendingMessageCount() {
        return pendingMessages.size();
    }

    public int getUserPendingMessageCount(String userId) {
        Map<String, PendingMessage> userMessages = userPendingMessages.get(userId);
        return userMessages != null ? userMessages.size() : 0;
    }

    public void clearUserPendingMessages(String userId) {
        Map<String, PendingMessage> userMessages = userPendingMessages.remove(userId);
        if (userMessages == null || userMessages.isEmpty()) {
            return;
        }

        userMessages.keySet().forEach(pendingMessages::remove);
        log.info("Cleared pending messages for userId={}, count={}", userId, userMessages.size());
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPendingMessages", pendingMessages.size());
        stats.put("usersWithPendingMessages", userPendingMessages.size());
        stats.put("totalRetries", pendingMessages.values().stream().mapToInt(PendingMessage::getRetryCount).sum());
        stats.put("maxCacheSize", maxCacheSize);
        stats.put("expireMinutes", expireMinutes);
        stats.put("totalExpired", totalExpired.get());
        stats.put("totalRetried", totalRetried.get());
        stats.put("totalFailed", totalFailed.get());
        return stats;
    }

    private void runTimeoutLoop() {
        while (running.get()) {
            try {
                PendingMessageTimeout timeout = timeoutQueue.take();
                handleTimeout(timeout);
            } catch (InterruptedException e) {
                if (!running.get()) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } catch (Exception e) {
                log.error("Process pending message timeout failed", e);
            }
        }
    }

    private void handleTimeout(PendingMessageTimeout timeout) {
        PendingMessage pendingMessage = pendingMessages.get(timeout.messageId());
        if (pendingMessage == null || pendingMessage.getStatus() != PendingMessage.MessageStatus.PENDING) {
            return;
        }

        if (pendingMessage.getRetryCount() != timeout.retryCountSnapshot()) {
            return;
        }

        if (!pendingMessage.canRetry()) {
            pendingMessage.setStatus(PendingMessage.MessageStatus.FAILED);
            removePendingMessage(pendingMessage);
            totalFailed.incrementAndGet();
            log.error("Pending message exhausted retries, messageId={}, userId={}, retries={}",
                    pendingMessage.getMessageId(),
                    pendingMessage.getTargetUserId(),
                    pendingMessage.getRetryCount());
            return;
        }

        if (retryMessage(pendingMessage)) {
            scheduleTimeout(pendingMessage);
        }
    }

    private boolean retryMessage(PendingMessage pendingMessage) {
        pendingMessage.incrementRetryCount();
        totalRetried.incrementAndGet();

        Channel channel = nettyChannelManager.getChannel(pendingMessage.getTargetUserId());
        if (channel == null || !channel.isActive()) {
            pendingMessage.setStatus(PendingMessage.MessageStatus.FAILED);
            removePendingMessage(pendingMessage);
            totalFailed.incrementAndGet();
            log.warn("Cannot retry pending message because target user is offline, messageId={}, userId={}",
                    pendingMessage.getMessageId(), pendingMessage.getTargetUserId());
            return false;
        }

        channel.writeAndFlush(new TextWebSocketFrame(pendingMessage.getContent()));
        log.debug("Retried pending message, messageId={}, userId={}, retryCount={}",
                pendingMessage.getMessageId(),
                pendingMessage.getTargetUserId(),
                pendingMessage.getRetryCount());
        return true;
    }

    private void scheduleTimeout(PendingMessage pendingMessage) {
        long triggerAt = pendingMessage.getLastSentAt() + pendingMessage.getTimeoutMillis();
        timeoutQueue.offer(new PendingMessageTimeout(
                pendingMessage.getMessageId(),
                pendingMessage.getRetryCount(),
                triggerAt
        ));
    }

    private void removePendingMessage(PendingMessage pendingMessage) {
        pendingMessages.remove(pendingMessage.getMessageId());

        Map<String, PendingMessage> userMessages = userPendingMessages.get(pendingMessage.getTargetUserId());
        if (userMessages == null) {
            return;
        }

        userMessages.remove(pendingMessage.getMessageId());
        if (userMessages.isEmpty()) {
            userPendingMessages.remove(pendingMessage.getTargetUserId());
        }
    }

    private record PendingMessageTimeout(String messageId, int retryCountSnapshot, long triggerAtMillis)
            implements Delayed {

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(triggerAtMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            if (other == this) {
                return 0;
            }
            long diff = other instanceof PendingMessageTimeout timeout
                    ? triggerAtMillis - timeout.triggerAtMillis
                    : getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS);
            if (diff == 0) {
                return 0;
            }
            return diff < 0 ? -1 : 1;
        }
    }
}
