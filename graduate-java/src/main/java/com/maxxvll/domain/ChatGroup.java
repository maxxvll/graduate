package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 群聊基础信息表
 * @TableName chat_group
 */
@TableName(value ="chat_group")
@Data
public class ChatGroup {
    /**
     * 群ID（主键，雪花ID/UUID，对应消息表receiver_id、会话表target_id）
     */
    @TableId
    private String id;

    /**
     * 群名称（聊天列表展示）
     */
    private String groupName;

    /**
     * 群头像URL
     */
    private String groupAvatar;

    /**
     * 创建人ID（关联chat_user.id）
     */
    private String creatorId;

    /**
     * 群最大成员数（默认200，可调整）
     */
    private Integer maxMember;

    /**
     * 加群方式：1-需审核，2-免审核，3-仅邀请
     */
    private Integer joinType;

    /**
     * 群公告
     */
    private String notice;

    /**
     * 是否全员禁言：0-否，1-是
     */
    private Integer isMuteAll;

    /**
     * 群状态：1-正常，2-解散，3-封禁
     */
    private Integer status;

    /**
     * 扩展字段（如群标签、创建原因等）
     */
    private Object extInfo;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
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
        ChatGroup other = (ChatGroup) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getGroupName() == null ? other.getGroupName() == null : this.getGroupName().equals(other.getGroupName()))
            && (this.getGroupAvatar() == null ? other.getGroupAvatar() == null : this.getGroupAvatar().equals(other.getGroupAvatar()))
            && (this.getCreatorId() == null ? other.getCreatorId() == null : this.getCreatorId().equals(other.getCreatorId()))
            && (this.getMaxMember() == null ? other.getMaxMember() == null : this.getMaxMember().equals(other.getMaxMember()))
            && (this.getJoinType() == null ? other.getJoinType() == null : this.getJoinType().equals(other.getJoinType()))
            && (this.getNotice() == null ? other.getNotice() == null : this.getNotice().equals(other.getNotice()))
            && (this.getIsMuteAll() == null ? other.getIsMuteAll() == null : this.getIsMuteAll().equals(other.getIsMuteAll()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getExtInfo() == null ? other.getExtInfo() == null : this.getExtInfo().equals(other.getExtInfo()))
            && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()))
            && (this.getUpdatedAt() == null ? other.getUpdatedAt() == null : this.getUpdatedAt().equals(other.getUpdatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getGroupName() == null) ? 0 : getGroupName().hashCode());
        result = prime * result + ((getGroupAvatar() == null) ? 0 : getGroupAvatar().hashCode());
        result = prime * result + ((getCreatorId() == null) ? 0 : getCreatorId().hashCode());
        result = prime * result + ((getMaxMember() == null) ? 0 : getMaxMember().hashCode());
        result = prime * result + ((getJoinType() == null) ? 0 : getJoinType().hashCode());
        result = prime * result + ((getNotice() == null) ? 0 : getNotice().hashCode());
        result = prime * result + ((getIsMuteAll() == null) ? 0 : getIsMuteAll().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getExtInfo() == null) ? 0 : getExtInfo().hashCode());
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
        sb.append(", groupName=").append(groupName);
        sb.append(", groupAvatar=").append(groupAvatar);
        sb.append(", creatorId=").append(creatorId);
        sb.append(", maxMember=").append(maxMember);
        sb.append(", joinType=").append(joinType);
        sb.append(", notice=").append(notice);
        sb.append(", isMuteAll=").append(isMuteAll);
        sb.append(", status=").append(status);
        sb.append(", extInfo=").append(extInfo);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append("]");
        return sb.toString();
    }
}