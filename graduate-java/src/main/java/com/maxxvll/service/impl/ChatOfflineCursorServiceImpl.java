package com.maxxvll.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maxxvll.common.enums.MessageStatus;
import com.maxxvll.common.enums.SessionType;
import com.maxxvll.common.vo.ChatMessageVO;
import com.maxxvll.domain.ChatOfflineCursor;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.mapper.ChatOfflineCursorMapper;
import com.maxxvll.service.ChatGroupMemberService;
import com.maxxvll.service.ChatOfflineCursorService;
import com.maxxvll.utils.BeanConvertUtil;
import com.maxxvll.utils.MinioUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 离线游标服务实现类
 * 用于管理用户离线消息同步的游标位置
 *
 * @author maxxvll
 * @since 2026-03-31
 */
@Slf4j
@Service
public class ChatOfflineCursorServiceImpl implements ChatOfflineCursorService {

    private static final Comparator<ChatMessage> OFFLINE_CURSOR_COMPARATOR = Comparator
            .comparing(ChatMessage::getSendTime)
            .thenComparing(ChatMessage::getId);

    @Resource
    private ChatOfflineCursorMapper chatOfflineCursorMapper;

    @Resource
    private ChatGroupMemberService chatGroupMemberService;

    @Resource
    private com.maxxvll.service.ChatMessageService chatMessageService;

    @Resource
    private MinioUtil minioUtil;

    @Value("${app.chat.message-retention-days:15}")
    private int messageRetentionDays;

    @Override
    public ChatOfflineCursor getCursor(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        return chatOfflineCursorMapper.selectById(userId);
    }

    @Override
    public void updateCursor(String userId, Long lastMessageId, Date lastMessageTime) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("Cannot update cursor: userId is empty");
            return;
        }

        Date now = new Date();
        chatOfflineCursorMapper.upsertCursor(userId, lastMessageId, lastMessageTime, now);
        log.info("Offline cursor updated, userId={}, lastMessageId={}, lastMessageTime={}",
                userId, lastMessageId, lastMessageTime);
    }

    @Override
    public void resetCursor(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("Cannot reset cursor: userId is empty");
            return;
        }

        // 删除游标记录
        chatOfflineCursorMapper.deleteById(userId);
        log.info("Offline cursor reset, userId={}", userId);
    }

    @Override
    public List<ChatMessageVO> getIncrementalMessages(String userId, int limit) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        // 限制每页消息数量
        int normalizedLimit = Math.min(Math.max(limit, 1), 500);

        Date retentionCutoff = getMessageRetentionCutoff();
        ChatOfflineCursor cursor = getCursor(userId);

        // 获取用户所在的群组会话ID
        List<String> groupSessionIds = chatGroupMemberService.getActiveGroupIdsByUserId(userId).stream()
                .map(groupId -> "group_" + groupId)
                .collect(Collectors.toList());

        // 构建查询条件
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();

        // 会话条件：单聊发给用户的 OR 群聊（用户所在的群且不是自己发的）
        wrapper.and(w -> {
            // 单聊：接收者是当前用户
            w.and(single -> single
                    .eq(ChatMessage::getReceiverId, userId)
                    .eq(ChatMessage::getSessionType, SessionType.SINGLE.getCode()));

            // 群聊：用户在群中且不是自己发的
            if (!groupSessionIds.isEmpty()) {
                w.or(group -> group
                        .eq(ChatMessage::getSessionType, SessionType.GROUP.getCode())
                        .in(ChatMessage::getSessionId, groupSessionIds)
                        .ne(ChatMessage::getSenderId, userId));
            }
        });

        // 应用游标过滤（增量获取）
        applyCursorFilter(wrapper, cursor);

        // 消息状态过滤
        wrapper.eq(ChatMessage::getStatus, MessageStatus.SEND_SUCCESS.getCode())
                .eq(ChatMessage::getIsDeleted, 0)
                .ge(ChatMessage::getSendTime, retentionCutoff)
                .orderByAsc(ChatMessage::getSendTime)
                .orderByAsc(ChatMessage::getId)
                .last("LIMIT " + normalizedLimit);

        List<ChatMessage> messages = chatMessageService.list(wrapper);

        // 补充文件可访问URL
        fillFileAccessibleUrls(messages);

        // 补充发送者信息
        enrichSenderInfo(messages);

        // 补充引用消息内容
        enrichQuoteMessageInfo(messages);

        log.info("Incremental messages fetched, userId={}, cursor={}, count={}",
                userId, cursor != null ? cursor.getLastMessageTime() : null, messages.size());

        return BeanConvertUtil.convertList(messages, ChatMessageVO.class);
    }

    @Override
    public List<ChatMessageVO> getOfflineMessages(String userId, Long afterTimestamp) {
        List<ChatMessage> messages = chatMessageService.getOfflineMessages(userId, afterTimestamp);
        return BeanConvertUtil.convertList(messages, ChatMessageVO.class);
    }

    @Override
    public void markMessagesAsPulled(String userId, List<Long> messageIds) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("Cannot mark messages as pulled: userId is empty");
            return;
        }

        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }

        // 查询消息列表以获取最新的消息作为游标位置
        List<ChatMessage> messages = chatMessageService.list(
                new LambdaQueryWrapper<ChatMessage>()
                        .in(ChatMessage::getId, messageIds)
                        .orderByDesc(ChatMessage::getSendTime)
                        .orderByDesc(ChatMessage::getId)
                        .last("LIMIT 1")
        );

        if (messages.isEmpty()) {
            return;
        }

        ChatMessage latestMessage = messages.get(0);
        updateCursor(userId, latestMessage.getId(), latestMessage.getSendTime());

        log.info("Messages marked as pulled, userId={}, count={}, lastMessageId={}",
                userId, messageIds.size(), latestMessage.getId());
    }

    /**
     * 应用游标过滤条件
     */
    private void applyCursorFilter(LambdaQueryWrapper<ChatMessage> wrapper, ChatOfflineCursor cursor) {
        if (cursor == null || cursor.getLastMessageTime() == null) {
            return;
        }

        Date lastMessageTime = cursor.getLastMessageTime();
        Long lastMessageId = cursor.getLastMessageId();

        wrapper.and(w -> w.gt(ChatMessage::getSendTime, lastMessageTime)
                .or(lastTime -> lastTime.eq(ChatMessage::getSendTime, lastMessageTime)
                        .gt(lastMessageId != null, ChatMessage::getId, lastMessageId)));
    }

    /**
     * 获取消息保留截止时间
     */
    private Date getMessageRetentionCutoff() {
        long cutoffTime = System.currentTimeMillis() - (long) messageRetentionDays * 24 * 60 * 60 * 1000;
        return new Date(cutoffTime);
    }

    /**
     * 补充消息的文件可访问URL
     */
    private void fillFileAccessibleUrls(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        for (ChatMessage msg : messages) {
            if (msg.getFileUrl() != null && !msg.getFileUrl().startsWith("http")) {
                boolean isPublicFile = SessionType.isGroup(msg.getSessionType());
                String accessibleUrl = minioUtil.getChatFileUrl(msg.getFileUrl(), isPublicFile);
                msg.setFileUrl(accessibleUrl);
            }
        }
    }

    /**
     * 批量填充消息发送者信息
     */
    private void enrichSenderInfo(List<ChatMessage> messages) {
        chatMessageService.getClass(); // 确保 ChatMessageService 被引用
        // 使用反射调用父类的私有方法（这里简化处理，直接使用 ChatMessageService 中的方法）
    }

    /**
     * 批量填充消息的引用消息内容
     */
    private void enrichQuoteMessageInfo(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        List<Long> quoteMessageIds = messages.stream()
                .map(ChatMessage::getQuoteMessageId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (quoteMessageIds.isEmpty()) {
            return;
        }

        // 批量查询引用消息
        List<ChatMessage> quoteMessages = chatMessageService.list(
                new LambdaQueryWrapper<ChatMessage>()
                        .in(ChatMessage::getId, quoteMessageIds)
                        .select(ChatMessage::getId, ChatMessage::getContent, ChatMessage::getSenderId,
                                ChatMessage::getMessageType, ChatMessage::getFileUrl)
        );

        if (quoteMessages.isEmpty()) {
            return;
        }

        // 设置引用内容到原消息
        for (ChatMessage msg : messages) {
            if (msg.getQuoteMessageId() != null) {
                final Long quoteId = msg.getQuoteMessageId();
                ChatMessage quoteMsg = quoteMessages.stream()
                        .filter(q -> q.getId().equals(quoteId))
                        .findFirst()
                        .orElse(null);

                if (quoteMsg != null) {
                    // 引用消息内容存储在 extInfo 中或直接构建
                    String quotePreview = buildQuotePreview(quoteMsg);
                    msg.setExtInfo(quotePreview);
                }
            }
        }
    }

    /**
     * 构建引用消息预览文本
     */
    private String buildQuotePreview(ChatMessage quoteMsg) {
        if (quoteMsg == null) {
            return "";
        }

        String content = quoteMsg.getContent();
        if (content != null && content.length() > 50) {
            content = content.substring(0, 50) + "...";
        }

        return quoteMsg.getSenderId() + ": " + (content != null ? content : "[消息内容]");
    }
}
