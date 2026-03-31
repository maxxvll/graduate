package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 聊天消息收藏表
 * @TableName chat_favorite
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_favorite")
public class Favorite extends BaseEntity {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 收藏内容
     */
    private String content;

    /**
     * 消息类型：TEXT/IMAGE/FILE/VOICE
     */
    private String messageType;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 发送者ID
     */
    private String senderId;

    /**
     * 发送者昵称
     */
    private String senderName;

    /**
     * 发送者头像
     */
    private String senderAvatar;

    /**
     * 会话ID
     */
    private String sessionId;
}
