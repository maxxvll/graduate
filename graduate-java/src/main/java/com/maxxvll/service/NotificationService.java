package com.maxxvll.service;

import com.maxxvll.common.dto.BatchNotificationSendDTO;
import com.maxxvll.common.dto.NotificationSettingUpdateDTO;
import com.maxxvll.common.dto.SystemNotificationSendDTO;
import com.maxxvll.common.vo.NotificationPageVO;
import com.maxxvll.common.vo.NotificationSettingVO;

/**
 * 通知服务接口
 *
 * @author Claude Code
 * @since 2026-03-31
 */
public interface NotificationService {

    // ==================== 通知设置 ====================

    /**
     * 获取用户通知设置
     */
    NotificationSettingVO getNotificationSetting(String userId);

    /**
     * 更新用户通知设置
     */
    void updateNotificationSetting(String userId, NotificationSettingUpdateDTO updateDTO);

    // ==================== 通知查询 ====================

    /**
     * 获取用户通知列表（游标分页）
     */
    NotificationPageVO getNotifications(String userId, String cursor, Integer limit);

    /**
     * 获取用户未读通知数量
     */
    Long getUnreadCount(String userId);

    /**
     * 标记通知为已读
     */
    void markAsRead(String userId, Long notificationId);

    /**
     * 标记所有通知为已读
     */
    void markAllAsRead(String userId);

    /**
     * 删除通知
     */
    void deleteNotification(String userId, Long notificationId);

    // ==================== 通知发送 ====================

    /**
     * 发送系统通知给单个用户
     */
    void sendToUser(SystemNotificationSendDTO dto, String senderId, String senderName, String senderAvatar);

    /**
     * 发送系统通知给群组
     */
    void sendToGroup(SystemNotificationSendDTO dto, String senderId, String senderName, String senderAvatar);

    /**
     * 发送全员通知（管理员）
     */
    void sendBroadcast(SystemNotificationSendDTO dto);

    /**
     * 批量发送通知
     */
    void sendBatch(BatchNotificationSendDTO dto, String senderId, String senderName, String senderAvatar);

    // ==================== 推送服务 ====================

    /**
     * 推送通知到用户（通过WebSocket）
     */
    void pushNotificationToUser(String userId, Object notification);

    /**
     * 检查用户是否可以接收通知
     */
    boolean canReceiveNotification(String userId, String notificationType);
}
