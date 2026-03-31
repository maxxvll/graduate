package com.maxxvll.service.impl;

import com.alibaba.fastjson2.JSON;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.maxxvll.common.dto.MarkMessagesReadDTO;
import com.maxxvll.common.vo.ReadSyncPushVO;
import com.maxxvll.common.vo.ReadSyncStatusVO;
import com.maxxvll.component.NettyChannelManager;
import com.maxxvll.domain.*;
import com.maxxvll.mapper.*;
import com.maxxvll.netty.WebSocketConstants;
import com.maxxvll.service.ChatMessageService;
import com.maxxvll.service.ChatSessionService;
import com.maxxvll.service.ReadSyncService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 已读同步服务实现
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Slf4j
@Service
public class ReadSyncServiceImpl implements ReadSyncService {

    @Resource
    private MessageReadStatusMapper messageReadStatusMapper;

    @Resource
    private SessionReadProgressMapper sessionReadProgressMapper;

    @Resource
    private UnreadSyncQueueMapper unreadSyncQueueMapper;

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private ChatSessionService chatSessionService;

    @Resource
    private ChatSessionMapper chatSessionMapper;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Resource
    private NettyChannelManager nettyChannelManager;

    // ==================== 已读操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(String userId, MarkMessagesReadDTO dto) {
        if (StrUtil.isBlank(userId) || dto == null || StrUtil.isBlank(dto.getSessionId())) {
            return;
        }

        String sessionId = dto.getSessionId();
        String deviceType = dto.getDeviceType();
        String deviceId = dto.getDeviceId();

        // 获取当前会话的阅读进度
        SessionReadProgress progress = getOrCreateProgress(userId, sessionId, deviceType, deviceId);

        // 获取最后阅读的消息ID
        Long lastReadMessageId = dto.getLastReadMessageId();
        if (lastReadMessageId == null) {
            lastReadMessageId = progress.getLastReadMessageId();
        }

        // 更新会话阅读进度
        updateReadProgress(userId, sessionId, lastReadMessageId, deviceType, deviceId);

        // 清零会话未读数
        chatSessionService.clearUnreadCount(sessionId, userId);

        // 记录消息阅读状态
        if (lastReadMessageId != null) {
            recordMessageReadStatus(userId, sessionId, lastReadMessageId, deviceType);
        }

        // 同步到其他设备
        broadcastReadSyncEvent(userId, sessionId, lastReadMessageId, deviceType, deviceId);
        syncToOtherDevices(userId, sessionId, lastReadMessageId);

        log.debug("标记已读成功, userId={}, sessionId={}, lastReadMessageId={}", userId, sessionId, lastReadMessageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSessionAsRead(String userId, String sessionId, String deviceType, String deviceId) {
        // 获取会话的最后一条消息
        ChatMessage lastMessage = chatMessageMapper.selectLastMessageBySessionId(sessionId);
        if (lastMessage != null) {
            MarkMessagesReadDTO dto = new MarkMessagesReadDTO();
            dto.setSessionId(sessionId);
            dto.setLastReadMessageId(lastMessage.getId());
            dto.setDeviceType(deviceType);
            dto.setDeviceId(deviceId);
            markAsRead(userId, dto);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(String userId) {
        // 获取用户所有会话
        List<ChatSession> sessions = chatSessionService.getSessionsByUserId(userId);

        for (ChatSession session : sessions) {
            try {
                markSessionAsRead(userId, String.valueOf(session.getId()), null, null);
            } catch (Exception e) {
                log.warn("标记会话已读失败, userId={}, sessionId={}", userId, session.getId(), e);
            }
        }

        log.info("标记全部已读成功, userId={}, sessionCount={}", userId, sessions.size());
    }

    // ==================== 状态查询 ====================

    @Override
    public ReadSyncStatusVO getSessionReadStatus(String userId, String sessionId) {
        ReadSyncStatusVO vo = new ReadSyncStatusVO();
        vo.setSessionId(sessionId);

        // 获取主设备的阅读进度
        SessionReadProgress mainProgress = sessionReadProgressMapper.selectOne(
                new LambdaQueryWrapper<SessionReadProgress>()
                        .eq(SessionReadProgress::getUserId, userId)
                        .eq(SessionReadProgress::getSessionId, sessionId)
                        .isNull(SessionReadProgress::getDeviceId)
                        .orderByDesc(SessionReadProgress::getUpdateTime)
                        .last("LIMIT 1")
        );

        if (mainProgress != null) {
            vo.setLastReadMessageId(mainProgress.getLastReadMessageId());
            vo.setLastReadTime(mainProgress.getLastReadTime() != null ?
                    mainProgress.getLastReadTime().getTime() : null);
            vo.setUnreadCount(mainProgress.getUnreadCount());
        } else {
            // 从会话表获取未读数
            ChatSession session = chatSessionMapper.selectBySessionIdUserId(sessionId, userId);
            vo.setUnreadCount(session != null ? session.getUnreadCount() : 0);
        }

        // 获取各设备的阅读进度
        List<SessionReadProgress> deviceProgressList = sessionReadProgressMapper.selectByUserAndSession(userId, sessionId);
        List<ReadSyncStatusVO.DeviceReadProgress> deviceProgress = deviceProgressList.stream()
                .filter(p -> StrUtil.isNotBlank(p.getDeviceId()))
                .map(p -> {
                    ReadSyncStatusVO.DeviceReadProgress dp = new ReadSyncStatusVO.DeviceReadProgress();
                    dp.setDeviceId(p.getDeviceId());
                    dp.setDeviceType(p.getDeviceType());
                    dp.setLastReadMessageId(p.getLastReadMessageId());
                    dp.setLastReadTime(p.getLastReadTime() != null ?
                            p.getLastReadTime().getTime() : null);
                    dp.setUnreadCount(p.getUnreadCount());
                    return dp;
                })
                .collect(Collectors.toList());
        vo.setDeviceProgress(deviceProgress);

        return vo;
    }

    @Override
    public List<ReadSyncStatusVO> getAllSessionReadStatus(String userId) {
        List<ChatSession> sessions = chatSessionService.getSessionsByUserId(userId);

        return sessions.stream()
                .map(session -> {
                    ReadSyncStatusVO vo = new ReadSyncStatusVO();
                    vo.setSessionId(String.valueOf(session.getId()));
                    vo.setUnreadCount(session.getUnreadCount());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Integer getTotalUnreadCount(String userId) {
        List<ChatSession> sessions = chatSessionService.getSessionsByUserId(userId);
        return sessions.stream()
                .mapToInt(s -> s.getUnreadCount() != null ? s.getUnreadCount() : 0)
                .sum();
    }

    @Override
    public boolean isMessageRead(String userId, Long messageId) {
        MessageReadStatus status = messageReadStatusMapper.selectByMessageAndUser(messageId, userId);
        return status != null;
    }

    // ==================== 同步操作 ====================

    @Override
    public void syncToOtherDevices(String userId, String sessionId, Long lastReadMessageId) {
        // 获取其他设备的阅读进度
        List<SessionReadProgress> otherDevices = sessionReadProgressMapper.selectList(
                new LambdaQueryWrapper<SessionReadProgress>()
                        .eq(SessionReadProgress::getUserId, userId)
                        .eq(SessionReadProgress::getSessionId, sessionId)
                        .isNotNull(SessionReadProgress::getDeviceId)
        );

        // 为其他设备创建同步任务
        List<UnreadSyncQueue> syncTasks = otherDevices.stream()
                .map(progress -> {
                    UnreadSyncQueue task = new UnreadSyncQueue();
                    task.setUserId(userId);
                    task.setSessionId(sessionId);
                    task.setTargetDeviceId(progress.getDeviceId());
                    task.setSyncType(UnreadSyncQueue.SYNC_TYPE_SESSION_READ);
                    task.setLastSyncedMessageId(lastReadMessageId);
                    task.setLastSyncedTime(new Date());
                    task.setStatus(UnreadSyncQueue.STATUS_PENDING);
                    task.setRetryCount(0);
                    task.setCreateTime(new Date());
                    task.setUpdateTime(new Date());
                    return task;
                })
                .collect(Collectors.toList());

        if (!syncTasks.isEmpty()) {
            unreadSyncQueueMapper.batchInsert(syncTasks);
            log.debug("创建设备同步任务, userId={}, sessionId={}, taskCount={}", userId, sessionId, syncTasks.size());
        }
    }

    @Override
    public List<Long> getUnsyncedMessageIds(String userId, String sessionId, Long fromMessageId) {
        if (fromMessageId == null) {
            return List.of();
        }

        // 获取用户在该会话的阅读进度
        SessionReadProgress progress = sessionReadProgressMapper.selectOne(
                new LambdaQueryWrapper<SessionReadProgress>()
                        .eq(SessionReadProgress::getUserId, userId)
                        .eq(SessionReadProgress::getSessionId, sessionId)
                        .orderByDesc(SessionReadProgress::getUpdateTime)
                        .last("LIMIT 1")
        );

        if (progress == null || progress.getLastReadMessageId() == null) {
            return List.of();
        }

        // 获取未同步的消息ID列表
        List<ChatMessage> messages = chatMessageMapper.selectUnsyncedMessages(
                sessionId, fromMessageId, progress.getLastReadMessageId());

        return messages.stream()
                .map(ChatMessage::getId)
                .collect(Collectors.toList());
    }

    // ==================== 离线同步 ====================

    @Override
    public List<Long> getOfflineUnreadMessageIds(String userId, Long lastSyncTime) {
        List<ChatSession> sessions = chatSessionService.getSessionsByUserId(userId);
        List<Long> unreadMessageIds = new ArrayList<>();

        for (ChatSession session : sessions) {
            if (session.getUnreadCount() != null && session.getUnreadCount() > 0) {
                // 获取该会话的未读消息
                Long afterTime = session.getLastMessageTime() != null ?
                        session.getLastMessageTime().getTime() : 0L;
                List<ChatMessage> unreadMessages = chatMessageMapper.selectUnreadMessagesAfterTime(
                        String.valueOf(session.getId()),
                        afterTime,
                        userId);

                for (ChatMessage msg : unreadMessages) {
                    if (!msg.getSenderId().equals(userId)) { // 不包括自己发送的消息
                        unreadMessageIds.add(msg.getId());
                    }
                }
            }
        }

        return unreadMessageIds;
    }

    // ==================== 私有方法 ====================

    private SessionReadProgress getOrCreateProgress(String userId, String sessionId,
                                                   String deviceType, String deviceId) {
        SessionReadProgress progress;

        if (StrUtil.isNotBlank(deviceId)) {
            progress = sessionReadProgressMapper.selectByUserSessionDevice(userId, sessionId, deviceId);
        } else {
            progress = sessionReadProgressMapper.selectOne(
                    new LambdaQueryWrapper<SessionReadProgress>()
                            .eq(SessionReadProgress::getUserId, userId)
                            .eq(SessionReadProgress::getSessionId, sessionId)
                            .isNull(SessionReadProgress::getDeviceId)
                        .orderByDesc(SessionReadProgress::getUpdateTime)
                            .last("LIMIT 1")
            );
        }

        if (progress == null) {
            progress = new SessionReadProgress();
            progress.setUserId(userId);
            progress.setSessionId(sessionId);
            progress.setDeviceType(deviceType);
            progress.setDeviceId(deviceId);
            progress.setUnreadCount(0);
            progress.setUpdateTime(new Date());
        }

        return progress;
    }

    private void updateReadProgress(String userId, String sessionId, Long lastReadMessageId,
                                   String deviceType, String deviceId) {
        Date now = new Date();

        // 计算未读数
        ChatMessage lastMessage = chatMessageMapper.selectLastMessageBySessionId(sessionId);
        int unreadCount = 0;
        if (lastMessage != null && lastReadMessageId != null) {
            Long lastMessageId = lastMessage.getId();
            if (lastMessageId > lastReadMessageId) {
                unreadCount = (int) (lastMessageId - lastReadMessageId);
            }
        }

        SessionReadProgress progress = new SessionReadProgress();
        progress.setUserId(userId);
        progress.setSessionId(sessionId);
        progress.setLastReadMessageId(lastReadMessageId);
        progress.setLastReadTime(now);
        progress.setUnreadCount(unreadCount);
        progress.setDeviceType(deviceType);
        progress.setDeviceId(deviceId);
        progress.setUpdateTime(now);

        sessionReadProgressMapper.upsert(progress);
    }

    private void broadcastReadSyncEvent(String userId, String sessionId, Long lastReadMessageId,
                                        String deviceType, String deviceId) {
        if (StrUtil.isBlank(userId) || StrUtil.isBlank(sessionId)) {
            return;
        }

        ReadSyncPushVO payload = ReadSyncPushVO.builder()
                .type(WebSocketConstants.MessageType.READ_SYNC)
                .sessionId(sessionId)
                .lastReadMessageId(lastReadMessageId)
                .sourceDeviceType(deviceType)
                .sourceDeviceId(deviceId)
                .timestamp(System.currentTimeMillis())
                .build();

        boolean pushed = nettyChannelManager.sendSerializedMessageToUser(userId, JSON.toJSONString(payload));
        log.debug("Broadcast read sync event, userId={}, sessionId={}, lastReadMessageId={}, pushed={}",
                userId, sessionId, lastReadMessageId, pushed);
    }

    private void recordMessageReadStatus(String userId, String sessionId, Long messageId, String device) {
        // 检查是否已存在
        MessageReadStatus existing = messageReadStatusMapper.selectByMessageAndUser(messageId, userId);
        if (existing != null) {
            return;
        }

        MessageReadStatus status = new MessageReadStatus();
        status.setMessageId(messageId);
        status.setUserId(userId);
        status.setSessionId(sessionId);
        status.setReadTime(new Date());
        status.setReadDevice(device);
        status.setCreateTime(new Date());

        messageReadStatusMapper.insert(status);
    }
}
