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
 * 好友申请表
 * @TableName friend_application
 */
@TableName(value ="friend_application")
@Data
public class FriendApplication {
    /**
     * 主键ID（雪花算法）
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 申请人ID（关联用户表user.id）
     */
    private Long applicantId;

    /**
     * 被申请人ID（关联用户表user.id）
     */
    private Long targetUserId;

    /**
     * 申请状态：0-待处理 1-已通过 2-已拒绝
     */
    private Integer status;

    /**
     * 拒绝原因（仅状态为2时填写）
     */
    private String rejectReason;

    /**
     * 申请备注（申请人所写的验证信息）
     */
    private String remark;

    /**
     * 申请创建时间
     */
    private Date createTime;

    /**
     * 申请更新时间
     */
    private Date updateTime;

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
        FriendApplication other = (FriendApplication) that;
        return (this.id == null ? other.id == null : this.id.equals(other.id))
            && (this.applicantId == null ? other.applicantId == null : this.applicantId.equals(other.applicantId))
            && (this.targetUserId == null ? other.targetUserId == null : this.targetUserId.equals(other.targetUserId))
            && (this.status == null ? other.status == null : this.status.equals(other.status))
            && (this.rejectReason == null ? other.rejectReason == null : this.rejectReason.equals(other.rejectReason))
            && (this.createTime == null ? other.createTime == null : this.createTime.equals(other.createTime))
            && (this.updateTime == null ? other.updateTime == null : this.updateTime.equals(other.updateTime));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((applicantId == null) ? 0 : applicantId.hashCode());
        result = prime * result + ((targetUserId == null) ? 0 : targetUserId.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result + ((rejectReason == null) ? 0 : rejectReason.hashCode());
        result = prime * result + ((createTime == null) ? 0 : createTime.hashCode());
        result = prime * result + ((updateTime == null) ? 0 : updateTime.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", applicantId=").append(applicantId);
        sb.append(", targetUserId=").append(targetUserId);
        sb.append(", status=").append(status);
        sb.append(", rejectReason=").append(rejectReason);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}