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
     * 群ID（主键，雪花算法生成，对应消息表receiver_id、会话表target_id）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

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
    private Long creatorId;

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
        return (this.id == null ? other.id == null : this.id.equals(other.id))
            && (this.groupName == null ? other.groupName == null : this.groupName.equals(other.groupName))
            && (this.groupAvatar == null ? other.groupAvatar == null : this.groupAvatar.equals(other.groupAvatar))
            && (this.creatorId == null ? other.creatorId == null : this.creatorId.equals(other.creatorId))
            && (this.maxMember == null ? other.maxMember == null : this.maxMember.equals(other.maxMember))
            && (this.joinType == null ? other.joinType == null : this.joinType.equals(other.joinType))
            && (this.notice == null ? other.notice == null : this.notice.equals(other.notice))
            && (this.isMuteAll == null ? other.isMuteAll == null : this.isMuteAll.equals(other.isMuteAll))
            && (this.status == null ? other.status == null : this.status.equals(other.status))
            && (this.extInfo == null ? other.extInfo == null : this.extInfo.equals(other.extInfo))
            && (this.createdAt == null ? other.createdAt == null : this.createdAt.equals(other.createdAt))
            && (this.updatedAt == null ? other.updatedAt == null : this.updatedAt.equals(other.updatedAt));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
        result = prime * result + ((groupAvatar == null) ? 0 : groupAvatar.hashCode());
        result = prime * result + ((creatorId == null) ? 0 : creatorId.hashCode());
        result = prime * result + ((maxMember == null) ? 0 : maxMember.hashCode());
        result = prime * result + ((joinType == null) ? 0 : joinType.hashCode());
        result = prime * result + ((notice == null) ? 0 : notice.hashCode());
        result = prime * result + ((isMuteAll == null) ? 0 : isMuteAll.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result + ((extInfo == null) ? 0 : extInfo.hashCode());
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