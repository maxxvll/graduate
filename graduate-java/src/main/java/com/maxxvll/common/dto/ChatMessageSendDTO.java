package com.maxxvll.common.dto;

import com.maxxvll.common.annotation.NotRequired;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

/**
 * 发送消息请求DTO
 * <p>
 * 用于发送聊天消息时的请求参数验证。
 * 支持文本、图片、视频、语音、文件等多种消息类型。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "发送消息请求参数")
public class ChatMessageSendDTO {

    /**
     * 消息唯一业务编号（客户端生成，用于去重）
     */
    @Schema(description = "消息编号", example = "msg_123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "消息编号不能为空")
    private String messageNo;

    /**
     * 会话ID
     */
    @Schema(description = "会话ID", example = "session_001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /**
     * 会话类型：1-单聊，2-群聊
     */
    @Schema(description = "会话类型", example = "1", allowableValues = {"1", "2"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会话类型不能为空")
    @Min(value = 1, message = "会话类型必须为1或2")
    @Max(value = 2, message = "会话类型必须为1或2")
    private Integer sessionType;

    /**
     * 接收者ID（单聊：用户ID；群聊：群ID）
     * 群聊时可为空，由后端根据 sessionId 解析
     */
    @Schema(description = "接收者ID", example = "10001")
    @NotRequired
    private String receiverId;

    /**
     * 消息内容（文本消息必填）
     */
    @Schema(description = "消息内容", example = "你好")
    @Size(max = 5000, message = "消息内容不能超过5000个字符")
    @NotRequired
    private String content;

    /**
     * 语音时长（秒）
     */
    @Schema(description = "语音时长（秒）", example = "30")
    @Min(value = 0, message = "语音时长不能为负数")
    @Max(value = 300, message = "语音时长不能超过300秒")
    @NotRequired
    private Integer duration;

    /**
     * 消息类型：1-文本，2-图片，3-视频，4-语音，5-文件
     * <p>注意：如果传了 fileUrl，必须传 messageType</p>
     */
    @Schema(description = "消息类型", example = "1", allowableValues = {"1", "2", "3", "4", "5"})
    @Min(value = 1, message = "消息类型必须为1-5之间的整数")
    @Max(value = 5, message = "消息类型必须为1-5之间的整数")
    private Integer messageType;

    /**
     * 文件URL
     */
    @Schema(description = "文件URL", example = "https://example.com/file.jpg")
    @NotRequired
    private String fileUrl;

    /**
     * 原始文件名
     */
    @Schema(description = "文件名", example = "document.pdf")
    @Size(max = 255, message = "文件名不能超过255个字符")
    @NotRequired
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @Schema(description = "文件大小（字节）", example = "1024000")
    @Min(value = 0, message = "文件大小不能为负数")
    @Max(value = 1024L * 1024 * 1024 * 2, message = "文件大小不能超过2GB")
    @NotRequired
    private Long fileSize;

    /**
     * 被@的用户ID列表
     */
    @Schema(description = "被@的用户ID列表", example = "[\"10001\",\"10002\"]")
    @NotRequired
    private List<String> atUserIds;

    /**
     * 是否@所有人：0-否，1-是
     */
    @Schema(description = "是否@所有人", example = "0", allowableValues = {"0", "1"})
    @Min(value = 0, message = "isAtAll必须为0或1")
    @Max(value = 1, message = "isAtAll必须为0或1")
    @NotRequired
    private Integer isAtAll;

    /**
     * 引用的消息ID
     */
    @Schema(description = "引用的消息ID", example = "12345")
    @NotRequired
    private Long quoteMessageId;

    /**
     * 编辑的消息ID（用于消息编辑功能）
     */
    @Schema(description = "编辑的消息ID", example = "12346")
    @NotRequired
    private Long editMessageId;
}
