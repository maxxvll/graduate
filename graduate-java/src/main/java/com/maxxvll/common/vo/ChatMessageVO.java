package com.maxxvll.common.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天消息 VO
 */
@Data
public class ChatMessageVO {
    private String id;
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
    private String senderAvatar; // 发送人头像（前端展示用）
    private String senderName;   // 发送人名称（前端展示用）
}