package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 群成员关联表（核心多对多）
 * @TableName chat_group_member
 */
@TableName(value ="chat_group_member")
@Data
public class ChatGroupMember {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 群ID（关联chat_group.id）
     */
    private String groupId;

    /**
     * 成员ID（关联chat_user.id）
     */
    private String userId;

    /**
     * 成员角色：1-群主，2-管理员，3-普通成员
     */
    private Integer role;

    /**
     * 加入时间
     */
    private Date joinTime;

    /**
     * 邀请人ID（非邀请加入则为空）
     */
    private String inviterId;

    /**
     * 是否被禁言：0-否，1-是（仅群主/管理员可设置）
     */
    private Integer isMute;

    /**
     * 是否退出：0-未退出，1-已退出
     */
    private Integer isQuit;

    /**
     * 退出时间（未退出则为空）
     */
    private Date quitTime;

    /**
     * 记录创建时间
     */
    private Date createdAt;

    /**
     * 记录更新时间
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
        ChatGroupMember other = (ChatGroupMember) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getGroupId() == null ? other.getGroupId() == null : this.getGroupId().equals(other.getGroupId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getRole() == null ? other.getRole() == null : this.getRole().equals(other.getRole()))
            && (this.getJoinTime() == null ? other.getJoinTime() == null : this.getJoinTime().equals(other.getJoinTime()))
            && (this.getInviterId() == null ? other.getInviterId() == null : this.getInviterId().equals(other.getInviterId()))
            && (this.getIsMute() == null ? other.getIsMute() == null : this.getIsMute().equals(other.getIsMute()))
            && (this.getIsQuit() == null ? other.getIsQuit() == null : this.getIsQuit().equals(other.getIsQuit()))
            && (this.getQuitTime() == null ? other.getQuitTime() == null : this.getQuitTime().equals(other.getQuitTime()))
            && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()))
            && (this.getUpdatedAt() == null ? other.getUpdatedAt() == null : this.getUpdatedAt().equals(other.getUpdatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getGroupId() == null) ? 0 : getGroupId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getRole() == null) ? 0 : getRole().hashCode());
        result = prime * result + ((getJoinTime() == null) ? 0 : getJoinTime().hashCode());
        result = prime * result + ((getInviterId() == null) ? 0 : getInviterId().hashCode());
        result = prime * result + ((getIsMute() == null) ? 0 : getIsMute().hashCode());
        result = prime * result + ((getIsQuit() == null) ? 0 : getIsQuit().hashCode());
        result = prime * result + ((getQuitTime() == null) ? 0 : getQuitTime().hashCode());
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
        sb.append(", groupId=").append(groupId);
        sb.append(", userId=").append(userId);
        sb.append(", role=").append(role);
        sb.append(", joinTime=").append(joinTime);
        sb.append(", inviterId=").append(inviterId);
        sb.append(", isMute=").append(isMute);
        sb.append(", isQuit=").append(isQuit);
        sb.append(", quitTime=").append(quitTime);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append("]");
        return sb.toString();
    }
}