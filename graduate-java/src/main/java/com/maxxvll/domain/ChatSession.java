package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.Date;
import lombok.Data;

/**
 * 聊天会话表（支撑聊天列表）
 * @TableName chat_session
 */
@TableName(value ="chat_session")
@Data
public class ChatSession {
    /**
     * 会话记录主键ID（雪花算法）
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

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
     * 最后一条消息内容（缩略版，如“[图片]”“你好”）
     */
    private String lastMessageContent;

    /**
     * 最后一条消息发送时间
     */
    private Date lastMessageTime;

    /**
     * 最后一条消息发送人ID（用于显示“XX：XXX”）
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

    /**
     * 会话创建时间
     */
    private Date createdAt;

    /**
     * 会话更新时间
     */
    private Date updatedAt;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        ChatSession other = (ChatSession) that;
        return (this.id == null ? other.id == null : this.id.equals(other.id))
            && (this.sessionId == null ? other.sessionId == null : this.sessionId.equals(other.sessionId))
            && (this.sessionType == null ? other.sessionType == null : this.sessionType.equals(other.sessionType))
            && (this.userId == null ? other.userId == null : this.userId.equals(other.userId))
            && (this.targetId == null ? other.targetId == null : this.targetId.equals(other.targetId))
            && (this.sessionName == null ? other.sessionName == null : this.sessionName.equals(other.sessionName))
            && (this.sessionAvatar == null ? other.sessionAvatar == null : this.sessionAvatar.equals(other.sessionAvatar))
            && (this.lastMessageId == null ? other.lastMessageId == null : this.lastMessageId.equals(other.lastMessageId))
            && (this.lastMessageContent == null ? other.lastMessageContent == null : this.lastMessageContent.equals(other.lastMessageContent))
            && (this.lastMessageTime == null ? other.lastMessageTime == null : this.lastMessageTime.equals(other.lastMessageTime))
            && (this.lastMessageSenderId == null ? other.lastMessageSenderId == null : this.lastMessageSenderId.equals(other.lastMessageSenderId))
            && (this.unreadCount == null ? other.unreadCount == null : this.unreadCount.equals(other.unreadCount))
            && (this.isTop == null ? other.isTop == null : this.isTop.equals(other.isTop))
            && (this.isMute == null ? other.isMute == null : this.isMute.equals(other.isMute))
            && (this.isHide == null ? other.isHide == null : this.isHide.equals(other.isHide))
            && (this.isDeleted == null ? other.isDeleted == null : this.isDeleted.equals(other.isDeleted))
            && (this.createdAt == null ? other.createdAt == null : this.createdAt.equals(other.createdAt))
            && (this.updatedAt == null ? other.updatedAt == null : this.updatedAt.equals(other.updatedAt));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
        result = prime * result + ((sessionType == null) ? 0 : sessionType.hashCode());
        result = prime * result + ((userId == null) ? 0 : userId.hashCode());
        result = prime * result + ((targetId == null) ? 0 : targetId.hashCode());
        result = prime * result + ((sessionName == null) ? 0 : sessionName.hashCode());
        result = prime * result + ((sessionAvatar == null) ? 0 : sessionAvatar.hashCode());
        result = prime * result + ((lastMessageId == null) ? 0 : lastMessageId.hashCode());
        result = prime * result + ((lastMessageContent == null) ? 0 : lastMessageContent.hashCode());
        result = prime * result + ((lastMessageTime == null) ? 0 : lastMessageTime.hashCode());
        result = prime * result + ((lastMessageSenderId == null) ? 0 : lastMessageSenderId.hashCode());
        result = prime * result + ((unreadCount == null) ? 0 : unreadCount.hashCode());
        result = prime * result + ((isTop == null) ? 0 : isTop.hashCode());
        result = prime * result + ((isMute == null) ? 0 : isMute.hashCode());
        result = prime * result + ((isHide == null) ? 0 : isHide.hashCode());
        result = prime * result + ((isDeleted == null) ? 0 : isDeleted.hashCode());
        result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
        result = prime * result + ((updatedAt == null) ? 0 : updatedAt.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", sessionId=").append(sessionId);
        sb.append(", sessionType=").append(sessionType);
        sb.append(", userId=").append(userId);
        sb.append(", targetId=").append(targetId);
        sb.append(", sessionName=").append(sessionName);
        sb.append(", sessionAvatar=").append(sessionAvatar);
        sb.append(", lastMessageId=").append(lastMessageId);
        sb.append(", lastMessageContent=").append(lastMessageContent);
        sb.append(", lastMessageTime=").append(lastMessageTime);
        sb.append(", lastMessageSenderId=").append(lastMessageSenderId);
        sb.append(", unreadCount=").append(unreadCount);
        sb.append(", isTop=").append(isTop);
        sb.append(", isMute=").append(isMute);
        sb.append(", isHide=").append(isHide);
        sb.append(", isDeleted=").append(isDeleted);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append("]");
        return sb.toString();
    }
}