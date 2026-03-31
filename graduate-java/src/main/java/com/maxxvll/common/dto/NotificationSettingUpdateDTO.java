package com.maxxvll.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalTime;

/**
 * 通知设置更新DTO
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
public class NotificationSettingUpdateDTO {

    // ==================== 免打扰设置 ====================

    /**
     * 是否启用免打扰
     */
    private Boolean dndEnabled;

    /**
     * 免打扰开始时间（格式：HH:mm:ss）
     */
    private LocalTime dndStartTime;

    /**
     * 免打扰结束时间（格式：HH:mm:ss）
     */
    private LocalTime dndEndTime;

    // ==================== 通知类型开关 ====================

    /**
     * 好友申请通知
     */
    private Boolean notifyFriendApply;

    /**
     * 群申请通知
     */
    private Boolean notifyGroupApply;

    /**
     * 群邀请通知
     */
    private Boolean notifyGroupInvite;

    /**
     * 消息通知
     */
    private Boolean notifyMessage;

    /**
     * @提及通知
     */
    private Boolean notifyAt;

    /**
     * 系统通知
     */
    private Boolean notifySystem;

    // ==================== 推送渠道设置 ====================

    /**
     * WebSocket推送
     */
    private Boolean pushChannelWebsocket;

    /**
     * APP推送
     */
    private Boolean pushChannelApp;

    // ==================== 声音和振动 ====================

    /**
     * 声音提示
     */
    private Boolean soundEnabled;

    /**
     * 振动提示
     */
    private Boolean vibrationEnabled;

    // ==================== 桌面通知 ====================

    /**
     * 桌面通知
     */
    private Boolean desktopNotification;
}
