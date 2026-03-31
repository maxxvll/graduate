package com.maxxvll.service;

import com.maxxvll.common.dto.MarkMessagesReadDTO;
import com.maxxvll.common.vo.ReadSyncStatusVO;

import java.util.List;

/**
 * 已读同步服务接口
 *
 * @author Claude Code
 * @since 2026-03-31
 */
public interface ReadSyncService {

    // ==================== 已读操作 ====================

    /**
     * 标记消息为已读
     */
    void markAsRead(String userId, MarkMessagesReadDTO dto);

    /**
     * 标记会话所有消息为已读
     */
    void markSessionAsRead(String userId, String sessionId, String deviceType, String deviceId);

    /**
     * 标记所有会话已读
     */
    void markAllAsRead(String userId);

    // ==================== 状态查询 ====================

    /**
     * 获取会话的已读同步状态
     */
    ReadSyncStatusVO getSessionReadStatus(String userId, String sessionId);

    /**
     * 获取用户所有会话的未读数统计
     */
    List<ReadSyncStatusVO> getAllSessionReadStatus(String userId);

    /**
     * 获取用户的总未读数
     */
    Integer getTotalUnreadCount(String userId);

    /**
     * 获取指定消息是否已读
     */
    boolean isMessageRead(String userId, Long messageId);

    // ==================== 同步操作 ====================

    /**
     * 同步未读状态到其他设备
     */
    void syncToOtherDevices(String userId, String sessionId, Long lastReadMessageId);

    /**
     * 获取需要同步的消息
     */
    List<Long> getUnsyncedMessageIds(String userId, String sessionId, Long fromMessageId);

    // ==================== 离线同步 ====================

    /**
     * 获取离线未读消息（用于登录后同步）
     */
    List<Long> getOfflineUnreadMessageIds(String userId, Long lastSyncTime);
}
