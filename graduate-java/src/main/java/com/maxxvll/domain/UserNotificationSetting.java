package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalTime;
import java.util.Date;

/**
 * 用户通知设置
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
@TableName("user_notification_setting")
public class UserNotificationSetting {

    /**
     * 用户ID
     */
    @TableId
    private String userId;

    // ==================== 免打扰设置 ====================

    /**
     * 是否启用免打扰
     */
    private Integer dndEnabled;

    /**
     * 免打扰开始时间
     */
    private LocalTime dndStartTime;

    /**
     * 免打扰结束时间
     */
    private LocalTime dndEndTime;

    // ==================== 通知类型开关 ====================

    /**
     * 好友申请通知
     */
    private Integer notifyFriendApply;

    /**
     * 群申请通知
     */
    private Integer notifyGroupApply;

    /**
     * 群邀请通知
     */
    private Integer notifyGroupInvite;

    /**
     * 消息通知
     */
    private Integer notifyMessage;

    /**
     * @提及通知
     */
    private Integer notifyAt;

    /**
     * 系统通知
     */
    private Integer notifySystem;

    // ==================== 推送渠道设置 ====================

    /**
     * WebSocket推送
     */
    private Integer pushChannelWebsocket;

    /**
     * APP推送
     */
    private Integer pushChannelApp;

    // ==================== 声音和振动 ====================

    /**
     * 声音提示
     */
    private Integer soundEnabled;

    /**
     * 振动提示
     */
    private Integer vibrationEnabled;

    // ==================== 桌面通知 ====================

    /**
     * 桌面通知
     */
    private Integer desktopNotification;

    // ==================== 时间戳 ====================

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    // ==================== 常量定义 ====================

    public static final int FLAG_NO = 0;
    public static final int FLAG_YES = 1;

    /**
     * 创建默认设置
     */
    public static UserNotificationSetting createDefault(String userId) {
        UserNotificationSetting setting = new UserNotificationSetting();
        setting.setUserId(userId);
        setting.setDndEnabled(FLAG_NO);
        setting.setNotifyFriendApply(FLAG_YES);
        setting.setNotifyGroupApply(FLAG_YES);
        setting.setNotifyGroupInvite(FLAG_YES);
        setting.setNotifyMessage(FLAG_YES);
        setting.setNotifyAt(FLAG_YES);
        setting.setNotifySystem(FLAG_YES);
        setting.setPushChannelWebsocket(FLAG_YES);
        setting.setPushChannelApp(FLAG_YES);
        setting.setSoundEnabled(FLAG_YES);
        setting.setVibrationEnabled(FLAG_YES);
        setting.setDesktopNotification(FLAG_YES);
        return setting;
    }

    /**
     * 是否启用免打扰
     */
    public boolean isDndEnabled() {
        return dndEnabled != null && dndEnabled == FLAG_YES;
    }

    /**
     * 是否在免打扰时段内
     */
    public boolean isInDndPeriod() {
        if (!isDndEnabled() || dndStartTime == null || dndEndTime == null) {
            return false;
        }
        LocalTime now = LocalTime.now();
        if (dndStartTime.isBefore(dndEndTime)) {
            return !now.isBefore(dndStartTime) && now.isBefore(dndEndTime);
        } else {
            // 跨午夜的情况（如 22:00 - 08:00）
            return !now.isBefore(dndStartTime) || now.isBefore(dndEndTime);
        }
    }

    /**
     * 是否可以推送
     */
    public boolean canPush(String notificationType) {
        // 如果在免打扰时段内，默认为不允许推送
        if (isInDndPeriod()) {
            return false;
        }

        return switch (notificationType) {
            case "FRIEND_APPLY" -> notifyFriendApply == FLAG_YES;
            case "GROUP_APPLY" -> notifyGroupApply == FLAG_YES;
            case "GROUP_INVITE" -> notifyGroupInvite == FLAG_YES;
            case "MESSAGE", "CHAT" -> notifyMessage == FLAG_YES;
            case "MENTION", "@" -> notifyAt == FLAG_YES;
            case "SYSTEM", "BROADCAST" -> notifySystem == FLAG_YES;
            default -> true;
        };
    }
    public Date getCreatedAt() {
        return createTime;
    }

    public void setCreatedAt(Date createdAt) {
        this.createTime = createdAt;
    }

    public Date getUpdatedAt() {
        return updateTime;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updateTime = updatedAt;
    }
}
