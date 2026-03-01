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
 * 聊天消息记录表（精简版）
 * @TableName chat_message
 */
@TableName(value ="chat_message")
@Data
public class ChatMessage {
    /**
     * 消息唯一主键ID（雪花算法，插入前生成，保证前端可直接使用）
     * 使用 ToStringSerializer 序列化为字符串，防止 JS Number 精度丢失
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 消息唯一业务编号（客户端生成，用于去重，避免重复发送）
     */
    private String messageNo;

    /**
     * 会话ID（单聊：小ID_大ID；群聊：group_群ID）
     */
    private String sessionId;

    /**
     * 会话类型：1-单聊，2-群聊（核心区分场景）
     */
    private Integer sessionType;

    /**
     * 发信人ID（用户ID/系统标识）
     */
    private String senderId;

    /**
     * 收信人ID（单聊：用户ID；群聊：群ID）
     */
    private String receiverId;

    /**
     * 消息类型：1-文本，2-图片，3-视频，4-音频，5-文件，6-表情，7-系统通知
     */
    private Integer messageType;

    /**
     * 原始消息内容（文本消息存内容，多媒体存占位符如【图片】）
     */
    private String content;

    /**
     * 撤回/敏感消息的替换内容（如[消息已撤回]）
     */
    private String contentReplaced;

    /**
     * 被@的用户ID列表（JSON格式，如["1001","1002"]）
     */
    private Object atUserIds;

    /**
     * 是否@所有人：0-否，1-是（群聊核心需求）
     */
    private Integer isAtAll;

    /**
     * 引用/回复的原消息ID（高频需求）
     */
    private Long quoteMessageId;

    /**
     * 多媒体文件URL（OSS/服务器地址）
     */
    private String fileUrl;

    /**
     * 文件原始名称（如截图2026.png）
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件MIME类型（如image/jpeg）
     */
    private String fileType;

    /**
     * 图片/视频缩略图URL（优化加载）
     */
    private String thumbnailUrl;

    /**
     * 文件是否过期：0-有效，1-过期（避免加载失效文件）
     */
    private Integer fileExpired;

    /**
     * 消息发送时间
     */
    private Date sendTime;

    /**
     * 撤回时间（非撤回消息为空）
     */
    private Date revokeTime;

    /**
     * 核心状态：1-发送成功，2-已读，3-已撤回，4-已删除，5-发送失败
     */
    private Integer status;

    /**
     * 消息操作人ID（如管理员撤回消息时填管理员ID）
     */
    private String operatorId;

    /**
     * 轻量扩展字段（存储小众需求，避免加字段）
     */
    private Object extInfo;

    /**
     * 是否敏感消息：0-否，1-是
     */
    private Integer isSensitive;

    /**
     * 软删除标识：0-未删除，1-已删除
     */
    private Integer isDeleted;

    /**
     * 记录创建时间
     */
    private Date createdAt;

    /**
     * 记录更新时间
     */
    private Date updatedAt;
    private Integer duration;

    /**
     * 发送人昵称（非DB字段，由 getMessages/sendMessage 动态填充，不入库）
     */
    @TableField(exist = false)
    private String senderName;

    /**
     * 发送人头像完整URL（非DB字段，由 getMessages/sendMessage 动态填充，不入库）
     */
    @TableField(exist = false)
    private String senderAvatar;

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
        ChatMessage other = (ChatMessage) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getMessageNo() == null ? other.getMessageNo() == null : this.getMessageNo().equals(other.getMessageNo()))
            && (this.getSessionId() == null ? other.getSessionId() == null : this.getSessionId().equals(other.getSessionId()))
            && (this.getSessionType() == null ? other.getSessionType() == null : this.getSessionType().equals(other.getSessionType()))
            && (this.getSenderId() == null ? other.getSenderId() == null : this.getSenderId().equals(other.getSenderId()))
            && (this.getReceiverId() == null ? other.getReceiverId() == null : this.getReceiverId().equals(other.getReceiverId()))
            && (this.getMessageType() == null ? other.getMessageType() == null : this.getMessageType().equals(other.getMessageType()))
            && (this.getContent() == null ? other.getContent() == null : this.getContent().equals(other.getContent()))
            && (this.getContentReplaced() == null ? other.getContentReplaced() == null : this.getContentReplaced().equals(other.getContentReplaced()))
            && (this.getAtUserIds() == null ? other.getAtUserIds() == null : this.getAtUserIds().equals(other.getAtUserIds()))
            && (this.getIsAtAll() == null ? other.getIsAtAll() == null : this.getIsAtAll().equals(other.getIsAtAll()))
            && (this.getQuoteMessageId() == null ? other.getQuoteMessageId() == null : this.getQuoteMessageId().equals(other.getQuoteMessageId()))
            && (this.getFileUrl() == null ? other.getFileUrl() == null : this.getFileUrl().equals(other.getFileUrl()))
            && (this.getFileName() == null ? other.getFileName() == null : this.getFileName().equals(other.getFileName()))
            && (this.getFileSize() == null ? other.getFileSize() == null : this.getFileSize().equals(other.getFileSize()))
            && (this.getFileType() == null ? other.getFileType() == null : this.getFileType().equals(other.getFileType()))
            && (this.getThumbnailUrl() == null ? other.getThumbnailUrl() == null : this.getThumbnailUrl().equals(other.getThumbnailUrl()))
            && (this.getFileExpired() == null ? other.getFileExpired() == null : this.getFileExpired().equals(other.getFileExpired()))
            && (this.getSendTime() == null ? other.getSendTime() == null : this.getSendTime().equals(other.getSendTime()))
            && (this.getRevokeTime() == null ? other.getRevokeTime() == null : this.getRevokeTime().equals(other.getRevokeTime()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getOperatorId() == null ? other.getOperatorId() == null : this.getOperatorId().equals(other.getOperatorId()))
            && (this.getExtInfo() == null ? other.getExtInfo() == null : this.getExtInfo().equals(other.getExtInfo()))
            && (this.getIsSensitive() == null ? other.getIsSensitive() == null : this.getIsSensitive().equals(other.getIsSensitive()))
            && (this.getIsDeleted() == null ? other.getIsDeleted() == null : this.getIsDeleted().equals(other.getIsDeleted()))
            && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()))
            && (this.getUpdatedAt() == null ? other.getUpdatedAt() == null : this.getUpdatedAt().equals(other.getUpdatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getMessageNo() == null) ? 0 : getMessageNo().hashCode());
        result = prime * result + ((getSessionId() == null) ? 0 : getSessionId().hashCode());
        result = prime * result + ((getSessionType() == null) ? 0 : getSessionType().hashCode());
        result = prime * result + ((getSenderId() == null) ? 0 : getSenderId().hashCode());
        result = prime * result + ((getReceiverId() == null) ? 0 : getReceiverId().hashCode());
        result = prime * result + ((getMessageType() == null) ? 0 : getMessageType().hashCode());
        result = prime * result + ((getContent() == null) ? 0 : getContent().hashCode());
        result = prime * result + ((getContentReplaced() == null) ? 0 : getContentReplaced().hashCode());
        result = prime * result + ((getAtUserIds() == null) ? 0 : getAtUserIds().hashCode());
        result = prime * result + ((getIsAtAll() == null) ? 0 : getIsAtAll().hashCode());
        result = prime * result + ((getQuoteMessageId() == null) ? 0 : getQuoteMessageId().hashCode());
        result = prime * result + ((getFileUrl() == null) ? 0 : getFileUrl().hashCode());
        result = prime * result + ((getFileName() == null) ? 0 : getFileName().hashCode());
        result = prime * result + ((getFileSize() == null) ? 0 : getFileSize().hashCode());
        result = prime * result + ((getFileType() == null) ? 0 : getFileType().hashCode());
        result = prime * result + ((getThumbnailUrl() == null) ? 0 : getThumbnailUrl().hashCode());
        result = prime * result + ((getFileExpired() == null) ? 0 : getFileExpired().hashCode());
        result = prime * result + ((getSendTime() == null) ? 0 : getSendTime().hashCode());
        result = prime * result + ((getRevokeTime() == null) ? 0 : getRevokeTime().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getOperatorId() == null) ? 0 : getOperatorId().hashCode());
        result = prime * result + ((getExtInfo() == null) ? 0 : getExtInfo().hashCode());
        result = prime * result + ((getIsSensitive() == null) ? 0 : getIsSensitive().hashCode());
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
        sb.append(", messageNo=").append(messageNo);
        sb.append(", sessionId=").append(sessionId);
        sb.append(", sessionType=").append(sessionType);
        sb.append(", senderId=").append(senderId);
        sb.append(", receiverId=").append(receiverId);
        sb.append(", messageType=").append(messageType);
        sb.append(", content=").append(content);
        sb.append(", contentReplaced=").append(contentReplaced);
        sb.append(", atUserIds=").append(atUserIds);
        sb.append(", isAtAll=").append(isAtAll);
        sb.append(", quoteMessageId=").append(quoteMessageId);
        sb.append(", fileUrl=").append(fileUrl);
        sb.append(", fileName=").append(fileName);
        sb.append(", fileSize=").append(fileSize);
        sb.append(", fileType=").append(fileType);
        sb.append(", thumbnailUrl=").append(thumbnailUrl);
        sb.append(", fileExpired=").append(fileExpired);
        sb.append(", sendTime=").append(sendTime);
        sb.append(", revokeTime=").append(revokeTime);
        sb.append(", status=").append(status);
        sb.append(", operatorId=").append(operatorId);
        sb.append(", extInfo=").append(extInfo);
        sb.append(", isSensitive=").append(isSensitive);
        sb.append(", isDeleted=").append(isDeleted);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append("]");
        return sb.toString();
    }

    public void setF() {

    }

    public void setFile() {

    }
}