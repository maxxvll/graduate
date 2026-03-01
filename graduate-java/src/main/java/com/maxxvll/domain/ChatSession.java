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
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getSessionId() == null ? other.getSessionId() == null : this.getSessionId().equals(other.getSessionId()))
            && (this.getSessionType() == null ? other.getSessionType() == null : this.getSessionType().equals(other.getSessionType()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getTargetId() == null ? other.getTargetId() == null : this.getTargetId().equals(other.getTargetId()))
            && (this.getSessionName() == null ? other.getSessionName() == null : this.getSessionName().equals(other.getSessionName()))
            && (this.getSessionAvatar() == null ? other.getSessionAvatar() == null : this.getSessionAvatar().equals(other.getSessionAvatar()))
            && (this.getLastMessageId() == null ? other.getLastMessageId() == null : this.getLastMessageId().equals(other.getLastMessageId()))
            && (this.getLastMessageContent() == null ? other.getLastMessageContent() == null : this.getLastMessageContent().equals(other.getLastMessageContent()))
            && (this.getLastMessageTime() == null ? other.getLastMessageTime() == null : this.getLastMessageTime().equals(other.getLastMessageTime()))
            && (this.getLastMessageSenderId() == null ? other.getLastMessageSenderId() == null : this.getLastMessageSenderId().equals(other.getLastMessageSenderId()))
            && (this.getUnreadCount() == null ? other.getUnreadCount() == null : this.getUnreadCount().equals(other.getUnreadCount()))
            && (this.getIsTop() == null ? other.getIsTop() == null : this.getIsTop().equals(other.getIsTop()))
            && (this.getIsMute() == null ? other.getIsMute() == null : this.getIsMute().equals(other.getIsMute()))
            && (this.getIsHide() == null ? other.getIsHide() == null : this.getIsHide().equals(other.getIsHide()))
            && (this.getIsDeleted() == null ? other.getIsDeleted() == null : this.getIsDeleted().equals(other.getIsDeleted()))
            && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()))
            && (this.getUpdatedAt() == null ? other.getUpdatedAt() == null : this.getUpdatedAt().equals(other.getUpdatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getSessionId() == null) ? 0 : getSessionId().hashCode());
        result = prime * result + ((getSessionType() == null) ? 0 : getSessionType().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getTargetId() == null) ? 0 : getTargetId().hashCode());
        result = prime * result + ((getSessionName() == null) ? 0 : getSessionName().hashCode());
        result = prime * result + ((getSessionAvatar() == null) ? 0 : getSessionAvatar().hashCode());
        result = prime * result + ((getLastMessageId() == null) ? 0 : getLastMessageId().hashCode());
        result = prime * result + ((getLastMessageContent() == null) ? 0 : getLastMessageContent().hashCode());
        result = prime * result + ((getLastMessageTime() == null) ? 0 : getLastMessageTime().hashCode());
        result = prime * result + ((getLastMessageSenderId() == null) ? 0 : getLastMessageSenderId().hashCode());
        result = prime * result + ((getUnreadCount() == null) ? 0 : getUnreadCount().hashCode());
        result = prime * result + ((getIsTop() == null) ? 0 : getIsTop().hashCode());
        result = prime * result + ((getIsMute() == null) ? 0 : getIsMute().hashCode());
        result = prime * result + ((getIsHide() == null) ? 0 : getIsHide().hashCode());
        result = prime * result + ((getIsDeleted() == null) ? 0 : getIsDeleted().hashCode());
        result = prime * result + ((getCreatedAt() == null) ? 0 : getCreatedAt().hashCode());
        result = prime * result + ((getUpdatedAt() == null) ? 0 : getUpdatedAt().hashCode());
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