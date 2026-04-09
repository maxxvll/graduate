package com.maxxvll.component;

import com.alibaba.fastjson2.JSON;
import com.maxxvll.common.event.ChatMessageEvent;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.netty.WebSocketConstants;
import com.maxxvll.service.ChatGroupMemberService;
import com.maxxvll.service.ChatMessageService;
import com.maxxvll.service.ChatUserService;
import com.maxxvll.utils.MinioUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class ChatPushSupport {

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private ChatGroupMemberService chatGroupMemberService;

    @Resource
    private NettyChannelManager nettyChannelManager;

    @Resource
    private ChatUserService chatUserService;

    @Resource
    private MinioUtil minioUtil;

    public boolean pushSingleMessage(ChatMessage message, String receiverId) {
        if (receiverId == null || receiverId.isBlank()) {
            log.warn("Skip single push because receiverId is empty, messageId={}", message.getId());
            return false;
        }

        String wrappedMessageJson = wrapChatMessage(message);
        boolean pushed = nettyChannelManager.sendSerializedMessageToUser(receiverId, wrappedMessageJson);
        if (!pushed) {
            chatMessageService.markMessageAsOffline(message.getId());
        }

        // 推送给发送者的其他在线设备（发送端已通过 HTTP 拿到消息，upsertMessage 会去重）
        String senderId = message.getSenderId();
        if (senderId != null && !senderId.equals(receiverId)) {
            nettyChannelManager.sendSerializedMessageToUser(senderId, wrappedMessageJson);
        }

        return pushed;
    }

    /**
     * 将 ChatMessage 包装为 WebSocket 消息格式
     * 格式: {type: 'message', data: ChatMessage}
     */
    private String wrapChatMessage(ChatMessage message) {
        // 推送前填充发送者信息
        enrichSenderInfo(message);

        Map<String, Object> wrappedMessage = new LinkedHashMap<>();
        wrappedMessage.put("type", "message");
        wrappedMessage.put("data", message);
        return JSON.toJSONString(wrappedMessage);
    }

    /**
     * 填充消息的发送者信息（用于 WebSocket 推送）
     */
    private void enrichSenderInfo(ChatMessage message) {
        if (message == null || message.getSenderId() == null) {
            return;
        }

        // 如果已经有发送者信息，则不再查询
        if (message.getSenderName() != null && message.getSenderAvatar() != null) {
            return;
        }

        try {
            ChatUser sender = chatUserService.getById(message.getSenderId());
            if (sender != null) {
                message.setSenderName(sender.getNickname());
                message.setSenderAvatar(minioUtil.getAvatarUrl(sender.getAvatar()));
            }
        } catch (Exception e) {
            log.warn("Failed to enrich sender info for messageId={}, senderId={}",
                    message.getId(), message.getSenderId(), e);
        }
    }

    public List<String> getActiveMemberIds(Long groupId) {
        return chatGroupMemberService.getActiveMemberIds(groupId);
    }

    public Map<Long, List<String>> getActiveMemberIdsByGroupIds(Collection<Long> groupIds) {
        return chatGroupMemberService.getActiveMemberIdsByGroupIds(groupIds);
    }

    public Long resolveGroupId(ChatMessageEvent event) {
        if (event.getReceiverId() != null && !event.getReceiverId().isBlank()) {
            return parseGroupId(event.getReceiverId());
        }

        ChatMessage message = event.getMessage();
        String sessionId = message != null ? message.getSessionId() : null;
        if (sessionId != null && sessionId.startsWith("group_")) {
            return parseGroupId(sessionId.substring(6));
        }
        return null;
    }

    public GroupPushStats pushGroupMessage(ChatMessage message, Collection<String> memberIds, Set<String> onlineUsers) {
        return pushGroupMessage(message, wrapChatMessage(message), memberIds, onlineUsers);
    }

    public GroupPushStats pushGroupMessage(ChatMessage message,
                                           String messageJson,
                                           Collection<String> memberIds,
                                           Set<String> onlineUsers) {
        if (memberIds == null || memberIds.isEmpty()) {
            return GroupPushStats.empty();
        }

        String senderId = message.getSenderId();
        int totalRecipients = 0;
        int onlineCount = 0;
        int offlineCount = 0;

        for (String memberId : memberIds) {
            if (memberId == null || memberId.equals(senderId)) {
                continue;
            }

            totalRecipients++;
            // 添加调试日志，便于排查在线用户匹配问题
            boolean isOnline = onlineUsers.contains(memberId);
            boolean sent = false;
            if (isOnline) {
                sent = nettyChannelManager.sendSerializedMessageToUser(memberId, messageJson);
                if (sent) {
                    onlineCount++;
                } else {
                    log.warn("Member is in online set but send failed, memberId={}", memberId);
                }
            } else {
                offlineCount++;
            }
        }

        // 推送给发送者的其他设备
        if (senderId != null) {
            nettyChannelManager.sendSerializedMessageToUser(senderId, messageJson);
        }

        return new GroupPushStats(totalRecipients, onlineCount, offlineCount);
    }

    private Long parseGroupId(String rawGroupId) {
        if (rawGroupId == null || rawGroupId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(rawGroupId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public record GroupPushStats(int totalRecipients, int onlineCount, int offlineCount) {
        public static GroupPushStats empty() {
            return new GroupPushStats(0, 0, 0);
        }
    }

    /**
     * 推送缓存同步事件到指定用户
     *
     * @param userId     用户ID
     * @param eventType  事件类型（使用 WebSocketConstants.MessageType 中的常量）
     * @param data       事件数据
     */
    public void pushCacheSyncEvent(String userId, String eventType, Object data) {
        if (userId == null || userId.isBlank()) {
            log.warn("Skip cache sync push because userId is empty, eventType={}", eventType);
            return;
        }

        try {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("type", eventType);
            message.put("data", data);
            message.put("timestamp", System.currentTimeMillis());

            String json = JSON.toJSONString(message);
            boolean sent = nettyChannelManager.sendSerializedMessageToUser(userId, json);

            if (sent) {
                log.debug("Cache sync event pushed: userId={}, type={}", userId, eventType);
            } else {
                log.debug("Cache sync event not sent (user offline): userId={}, type={}", userId, eventType);
            }
        } catch (Exception e) {
            log.error("Failed to push cache sync event: userId={}, type={}", userId, eventType, e);
        }
    }

    /**
     * 推送缓存同步事件到多个用户
     *
     * @param userIds    用户ID集合
     * @param eventType  事件类型
     * @param data       事件数据
     */
    public void pushCacheSyncEventToUsers(Collection<String> userIds, String eventType, Object data) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        for (String userId : userIds) {
            pushCacheSyncEvent(userId, eventType, data);
        }
    }
}
