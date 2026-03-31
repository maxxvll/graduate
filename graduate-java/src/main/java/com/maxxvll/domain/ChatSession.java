package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 聊天会话表（支撑聊天列表）
 * @TableName chat_session
 */
@TableName(value = "chat_session")
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatSession extends BaseEntity {

    /**
     * 会话ID（和消息表一致：单聊=小ID_大ID；群聊=group_群ID）
     */
    private String sessionId;

    /**
     * 会话类型（和消息表一致：1-单聊，2-群聊）
     */
    private Integer sessionType;

    /**
     * 【核心】所属用户ID（每个用户的每个会话都有一条记录）
     */
    private String userId;

    /**
     * 会话对方ID：单聊=对方用户ID；群聊=群ID
     */
    private String targetId;

    /**
     * 会话名称：单聊=对方昵称；群聊=群名称（避免频繁查用户/群表）
     */
    private String sessionName;

    /**
     * 会话头像：单聊=对方头像；群聊=群头像
     */
    private String sessionAvatar;

    /**
     * 最后一条消息ID（关联消息表id）
     */
    private Long lastMessageId;

    /**
     * 最后一条消息内容（缩略版，如"【图片】""你好"）
     */
    private String lastMessageContent;

    /**
     * 最后一条消息发送时间
     */
    private Date lastMessageTime;

    /**
     * 最后一条消息发送人ID（用于显示"XX：XXX"）
     */
    private String lastMessageSenderId;

    /**
     * 未读消息数（用户未读的消息数量）
     */
    private Integer unreadCount;

    /**
     * 是否置顶：0-否，1-是
     */
    private Integer isTop;

    /**
     * 是否免打扰：0-否，1-是
     */
    private Integer isMute;

    /**
     * 是否隐藏会话：0-否，1-是
     */
    private Integer isHide;

    /**
     * 软删除标识：0-未删除，1-已删除
     */
    private Integer isDeleted;
}
