package com.maxxvll.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息响应VO
 * <p>
 * 用于返回聊天消息信息。
 * 包含消息内容、发送者信息、文件信息等。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "聊天消息信息")
public class ChatMessageVO {

    /**
     * 主键ID（序列化为字符串防止JavaScript大整数精度丢失）
     */
    @Schema(description = "消息ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 消息编号（客户端生成，用于去重）
     */
    @Schema(description = "消息编号")
    private String messageNo;

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
     * 发送者ID
     */
    @Schema(description = "发送者ID")
    private String senderId;

    /**
     * 接收者ID
     */
    @Schema(description = "接收者ID")
    private String receiverId;

    /**
     * 消息类型：1-文本，2-图片，3-视频，4-语音，5-文件
     */
    @Schema(description = "消息类型")
    private Integer messageType;

    /**
     * 消息内容
     */
    @Schema(description = "消息内容")
    private String content;

    /**
     * 文件URL
     */
    @Schema(description = "文件URL")
    private String fileUrl;

    /**
     * 文件名
     */
    @Schema(description = "文件名")
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    /**
     * 语音时长（秒）
     */
    @Schema(description = "语音时长（秒）")
    private Integer duration;

    /**
     * 发送时间
     */
    @Schema(description = "发送时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime sendTime;

    /**
     * 消息状态：1-发送中，2-已发送，3-已撤回，4-已删除
     */
    @Schema(description = "消息状态")
    private Integer status;

    /**
     * 撤回/敏感替换内容（如[消息已撤回]）
     */
    @Schema(description = "替换内容")
    private String contentReplaced;

    /**
     * 发送者头像URL（前端展示用）
     */
    @Schema(description = "发送者头像")
    private String senderAvatar;

    /**
     * 发送者名称（前端展示用）
     */
    @Schema(description = "发送者名称")
    private String senderName;

    /**
     * 是否已编辑：0-否，1-是
     */
    @Schema(description = "是否已编辑")
    private Integer isEdited;

    /**
     * 编辑时间
     */
    @Schema(description = "编辑时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime editTime;
}