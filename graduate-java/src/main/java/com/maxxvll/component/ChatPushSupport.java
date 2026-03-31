package com.maxxvll.component;

import com.alibaba.fastjson2.JSON;
import com.maxxvll.common.event.ChatMessageEvent;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.service.ChatGroupMemberService;
import com.maxxvll.service.ChatMessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
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

    public boolean pushSingleMessage(ChatMessage message, String receiverId) {
        if (receiverId == null || receiverId.isBlank()) {
            log.warn("Skip single push because receiverId is empty, messageId={}", message.getId());
            return false;
        }

        boolean pushed = nettyChannelManager.sendSerializedMessageToUser(receiverId, JSON.toJSONString(message));
        if (!pushed) {
            chatMessageService.markMessageAsOffline(message.getId());
        }

        // 推送给发送者的其他在线设备（发送端已通过 HTTP 拿到消息，upsertMessage 会去重）
        String senderId = message.getSenderId();
        if (senderId != null && !senderId.equals(receiverId)) {
            nettyChannelManager.sendSerializedMessageToUser(senderId, JSON.toJSONString(message));
        }

        return pushed;
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
        return pushGroupMessage(message, JSON.toJSONString(message), memberIds, onlineUsers);
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
            if (onlineUsers.contains(memberId) && nettyChannelManager.sendSerializedMessageToUser(memberId, messageJson)) {
                onlineCount++;
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
}
