package com.maxxvll.component;

import com.maxxvll.common.event.ChatMessageEvent;
import com.maxxvll.common.vo.GroupMemberVO;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.service.ChatGroupMemberService;
import com.maxxvll.service.ChatMessageService;
import com.maxxvll.utils.UserContextUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka 消息消费者：处理聊天消息的实时推送和离线消息标记
 * 支持单聊和群聊两种场景
 */
@Slf4j
@Component
public class ChatMessageConsumer {

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private NettyChannelManager nettyChannelManager;

    @Resource
    private ChatGroupMemberService chatGroupMemberService;

    /**
     * 处理聊天消息事件（单聊和群聊）
     */
    @KafkaListener(topics = "chat-message-topic", groupId = "chat-message-group")
    public void handleChatMessage(ConsumerRecord<String, ChatMessageEvent> record) {
        ChatMessageEvent event = record.value();
        ChatMessage message = event.getMessage();
        Long messageId = message.getId();
        Integer sessionType = message.getSessionType();
        
        log.info("收到聊天消息事件，messageId: {}, sessionId: {}, sessionType: {}", 
                messageId, message.getSessionId(), sessionType);

        try {
            // 根据会话类型分别处理
            if (sessionType == 1) {
                // 单聊场景
                handleSingleChatPush(event);
            } else if (sessionType == 2) {
                // 群聊场景
                handleGroupChatPush(event);
            } else {
                log.warn("未知的会话类型：{}, messageId: {}", sessionType, messageId);
            }
        } catch (Exception e) {
            log.error("处理聊天消息事件失败，messageId: {}", messageId, e);
            // 可选：重试机制或存入死信队列
        }
    }

    /**
     * 处理单聊消息推送
     * 逻辑：检查接收方是否在线，在线则推送，离线则标记为离线消息
     */
    private void handleSingleChatPush(ChatMessageEvent event) {
        ChatMessage message = event.getMessage();
        String receiverId = event.getReceiverId();
        Long messageId = message.getId();

        // 检查接收方是否在线
        boolean isOnline = checkUserOnline(receiverId);

        if (isOnline) {
            // 在线：通过 Netty WebSocket 实时推送
            log.info("单聊接收方在线，推送消息，messageId: {}, receiverId: {}", messageId, receiverId);
            pushMessageToUser(receiverId, message);
        } else {
            // 离线：标记为离线消息
            log.info("单聊接收方离线，标记为离线消息，messageId: {}", messageId);
            chatMessageService.markMessageAsOffline(messageId);
        }
    }

    /**
     * 处理群聊消息推送
     * 逻辑：获取所有群成员（排除发送者），逐个检查在线状态并推送
     */
    private void handleGroupChatPush(ChatMessageEvent event) {
        ChatMessage message = event.getMessage();
        Long groupId = Long.valueOf(event.getReceiverId());
        Long messageId = message.getId();
        
        // 使用 UserContextUtil 获取当前登录用户 ID（发送者）
        String senderId = UserContextUtil.getCurrentUserId();

        log.info("群聊消息推送开始，messageId: {}, groupId: {}, senderId: {}", 
                messageId, groupId, senderId);

        try {
            // 1. 获取所有群成员列表
            List<GroupMemberVO> members = chatGroupMemberService.getGroupMembers(groupId);
            
            if (members == null || members.isEmpty()) {
                log.warn("群组成员为空，无法推送群消息，groupId: {}", groupId);
                return;
            }

            int onlineCount = 0;
            int offlineCount = 0;

            // 2. 遍历所有成员（排除发送者）
            for (GroupMemberVO member : members) {
                String memberId = member.getUserId();
                
                // 跳过发送者自己
                if (senderId.equals(memberId)) {
                    continue;
                }

                // 3. 检查成员是否在线
                boolean isOnline = checkUserOnline(memberId);

                if (isOnline) {
                    // 在线：推送消息
                    pushMessageToUser(memberId, message);
                    onlineCount++;
                } else {
                    // 离线：不处理（群聊消息默认不标记离线，成员上线后从历史消息加载）
                    offlineCount++;
                }
            }

            log.info("群聊消息推送完成，messageId: {}, 总成员数：{}, 在线：{}, 离线：{}", 
                    messageId, members.size(), onlineCount, offlineCount);

        } catch (Exception e) {
            log.error("群聊消息推送失败，messageId: {}, groupId: {}", messageId, groupId, e);
            throw e; // 重新抛出异常让外层处理
        }
    }

    /**
     * 检查用户是否在线
     * @param userId 用户 ID
     * @return true-在线，false-离线
     */
    private boolean checkUserOnline(String userId) {
        Channel channel = nettyChannelManager.getChannel(userId);
        return channel != null && channel.isActive();
    }

    /**
     * 通过 Netty WebSocket 推送消息给用户
     * @param userId 用户 ID
     * @param message 消息对象
     */
    private void pushMessageToUser(String userId, ChatMessage message) {
        try {
            Channel channel = nettyChannelManager.getChannel(userId);
            
            if (channel == null) {
                log.warn("用户 Channel 不存在，userId: {}", userId);
                return;
            }

            if (!channel.isActive()) {
                log.warn("用户 Channel 已断开，userId: {}", userId);
                nettyChannelManager.removeChannel(userId);
                return;
            }

            // 将消息转换为 JSON 并通过 TextWebSocketFrame 发送
            String messageJson = com.alibaba.fastjson.JSON.toJSONString(message);
            channel.writeAndFlush(new TextWebSocketFrame(messageJson));
            
            log.debug("消息推送成功，userId: {}, messageId: {}", userId, message.getId());
            
        } catch (Exception e) {
            log.error("消息推送失败，userId: {}, messageId: {}", userId, message.getId(), e);
        }
    }
}
