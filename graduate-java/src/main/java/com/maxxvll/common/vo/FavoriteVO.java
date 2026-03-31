package com.maxxvll.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏消息响应VO
 * <p>
 * 用于返回消息收藏信息。
 * 包含收藏内容、消息类型、发送者信息等。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "收藏消息信息")
public class FavoriteVO {

    /**
     * 收藏ID
     */
    @Schema(description = "收藏ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 消息ID
     */
    @Schema(description = "消息ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long messageId;

    /**
     * 收藏内容
     */
    @Schema(description = "收藏内容")
    private String content;

    /**
     * 消息类型：TEXT/IMAGE/FILE/VOICE
     */
    @Schema(description = "消息类型")
    private String messageType;

    /**
     * 文件URL
     */
    @Schema(description = "文件URL")
    private String fileUrl;

    /**
     * 发送者ID
     */
    @Schema(description = "发送者ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long senderId;

    /**
     * 会话ID
     */
    @Schema(description = "会话ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 发送者用户名
     */
    @Schema(description = "发送者用户名")
    private String senderName;

    /**
     * 发送者头像URL
     */
    @Schema(description = "发送者头像URL")
    private String senderAvatar;
}
