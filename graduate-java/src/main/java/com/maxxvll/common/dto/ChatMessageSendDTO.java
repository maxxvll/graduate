package com.maxxvll.common.dto;

import lombok.Data;
import java.util.List;

/**
 * 发送消息请求 DTO
 */
@Data
public class ChatMessageSendDTO {
    /** 消息唯一业务编号（客户端生成，用于去重） */
    private String messageNo;

    /** 会话ID */
    private String sessionId;

    /** 会话类型：1-单聊，2-群聊 */
    private Integer sessionType;

    /** 接收者ID（单聊：用户ID；群聊：群ID） */
    private String receiverId;

    /** 消息内容（文本消息必填） */
    private String content;

    /** 语音时长 */
    private Integer duration;

    // ==================== 【新增】秒传/切片上传模式所需字段 ====================
    /**
     * 消息类型：1-文本，2-图片，3-视频，4-语音，5-文件
     * 注意：如果传了 fileUrl，必须传 messageType
     */
    private Integer messageType;


    private String fileUrl;

    /** 原始文件名 */
    private String fileName;

    /** 文件大小 (字节) */
    private Long fileSize;
}