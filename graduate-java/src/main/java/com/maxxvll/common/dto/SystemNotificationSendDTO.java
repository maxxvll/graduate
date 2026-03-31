package com.maxxvll.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 系统通知发送DTO
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
public class SystemNotificationSendDTO {

    /**
     * 通知类型
     */
    @NotBlank(message = "通知类型不能为空")
    private String notificationType;

    /**
     * 通知标题
     */
    @NotBlank(message = "通知标题不能为空")
    private String title;

    /**
     * 通知内容
     */
    @NotBlank(message = "通知内容不能为空")
    private String content;

    /**
     * 目标类型：USER/GROUP/ALL
     */
    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    /**
     * 目标ID（单用户ID、群组ID或NULL表示全员）
     */
    private String targetId;

    /**
     * 关联业务ID
     */
    private String relatedId;

    /**
     * 关联业务类型
     */
    private String relatedType;

    /**
     * 优先级：0-普通，1-重要，2-紧急
     */
    private Integer priority;

    /**
     * 过期时间（时间戳，毫秒）
     */
    private Long expireTime;

    // ==================== 目标类型常量 ====================

    public static final String TARGET_USER = "USER";
    public static final String TARGET_GROUP = "GROUP";
    public static final String TARGET_ALL = "ALL";

    // ==================== 通知类型常量 ====================

    public static final String TYPE_FRIEND_APPLY = "FRIEND_APPLY";
    public static final String TYPE_GROUP_APPLY = "GROUP_APPLY";
    public static final String TYPE_GROUP_INVITE = "GROUP_INVITE";
    public static final String TYPE_MENTION = "MENTION";
    public static final String TYPE_SYSTEM = "SYSTEM";
    public static final String TYPE_BROADCAST = "BROADCAST";
}
