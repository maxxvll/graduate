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
 * 群成员关联表（核心多对多）
 * @TableName chat_group_member
 */
@TableName(value ="chat_group_member")
@Data
public class ChatGroupMember {
    /**
     * 主键ID（雪花算法）
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 群ID（关联chat_group.id）
     */
    private Long groupId;

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
     * 移除原因（管理员/群主移除成员时填写）
     */
    private String quitReason;

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
        return (this.id == null ? other.id == null : this.id.equals(other.id))
            && (this.groupId == null ? other.groupId == null : this.groupId.equals(other.groupId))
            && (this.userId == null ? other.userId == null : this.userId.equals(other.userId))
            && (this.role == null ? other.role == null : this.role.equals(other.role))
            && (this.joinTime == null ? other.joinTime == null : this.joinTime.equals(other.joinTime))
            && (this.inviterId == null ? other.inviterId == null : this.inviterId.equals(other.inviterId))
            && (this.isMute == null ? other.isMute == null : this.isMute.equals(other.isMute))
            && (this.isQuit == null ? other.isQuit == null : this.isQuit.equals(other.isQuit))
            && (this.quitTime == null ? other.quitTime == null : this.quitTime.equals(other.quitTime))
            && (this.createdAt == null ? other.createdAt == null : this.createdAt.equals(other.createdAt))
            && (this.updatedAt == null ? other.updatedAt == null : this.updatedAt.equals(other.updatedAt));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
        result = prime * result + ((userId == null) ? 0 : userId.hashCode());
        result = prime * result + ((role == null) ? 0 : role.hashCode());
        result = prime * result + ((joinTime == null) ? 0 : joinTime.hashCode());
        result = prime * result + ((inviterId == null) ? 0 : inviterId.hashCode());
        result = prime * result + ((isMute == null) ? 0 : isMute.hashCode());
        result = prime * result + ((isQuit == null) ? 0 : isQuit.hashCode());
        result = prime * result + ((quitTime == null) ? 0 : quitTime.hashCode());
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