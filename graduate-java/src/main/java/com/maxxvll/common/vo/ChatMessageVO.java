package com.maxxvll.common.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天消息 VO
 */
@Data
public class ChatMessageVO {
    /** 主键 ID，序列化为字符串防止 JavaScript 大整数精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String messageNo;
    private String sessionId;
    private Integer sessionType;
    private String senderId;
    private String receiverId;
    private Integer messageType;
    private String content;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private Integer duration;
    private LocalDateTime sendTime;
    private Integer status;
    /** 撤回/敏感替换内容（如[消息已撤回]），status=3时展示此字段 */
    private String contentReplaced;
    private String senderAvatar; // 发送人头像（前端展示用）
    private String senderName;   // 发送人名称（前端展示用）
}