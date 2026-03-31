package com.maxxvll.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量通知发送DTO
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
public class BatchNotificationSendDTO {

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
     * 目标用户ID列表
     */
    @NotEmpty(message = "目标用户不能为空")
    private List<String> targetUserIds;

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
}
