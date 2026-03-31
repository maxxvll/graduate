package com.maxxvll.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话信息响应VO
 * <p>
 * 用于返回会话列表和会话详情。
 * 包含会话基本信息、最后一条消息、未读数等。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话信息")
public class SessionVO {

    /**
     * 会话ID
     */
    @Schema(description = "会话ID")
    private String sessionId;

    /**
     * 会话类型：1-单聊，2-群聊
     */
    @Schema(description = "会话类型", example = "1")
    private Integer sessionType;

    /**
     * 目标ID（单聊为对方用户ID，群聊为群ID）
     */
    @Schema(description = "目标ID")
    private String targetId;

    /**
     * 会话名称
     */
    @Schema(description = "会话名称")
    private String sessionName;

    /**
     * 会话头像URL
     */
    @Schema(description = "会话头像URL")
    private String sessionAvatar;

    /**
     * 最后一条消息内容
     */
    @Schema(description = "最后一条消息内容")
    private String lastMessageContent;

    /**
     * 最后一条消息时间
     */
    @Schema(description = "最后一条消息时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastMessageTime;

    /**
     * 最后一条消息发送者ID
     */
    @Schema(description = "最后一条消息发送者ID")
    private String lastMessageSenderId;

    /**
     * 最后一条消息发送者名称
     */
    @Schema(description = "最后一条消息发送者名称")
    private String lastMessageSenderName;

    /**
     * 未读消息数
     */
    @Schema(description = "未读消息数")
    private Integer unreadCount;

    /**
     * 是否置顶：0-否，1-是
     */
    @Schema(description = "是否置顶")
    private Integer isTop;

    /**
     * 是否免打扰：0-否，1-是
     */
    @Schema(description = "是否免打扰")
    private Integer isMute;
}