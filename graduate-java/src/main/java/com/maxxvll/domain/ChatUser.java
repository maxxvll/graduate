package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

/**
 * 用户基础信息表
 * @TableName chat_user
 */
@TableName(value ="chat_user", autoResultMap = true)
@Data
public class ChatUser {
    /**
     * 用户唯一ID（主键，建议用雪花ID/UUID，避免自增ID泄露信息）
     */
    @TableId
    private String id;

    /**
     * 用户名（登录用，唯一）
     */
    private String username;

    /**
     * 用户昵称（聊天展示用）
     */
    private String nickname;

    /**
     * 用户头像URL（OSS/服务器地址）
     */
    private String avatar;

    /**
     * 手机号（可选，用于登录/验证）
     */
    private String phone;

    /**
     * 邮箱（可选）
     */
    private String email;

    /**
     * 密码（加密存储，如BCrypt哈希）
     */
    private String password;

    /**
     * 用户状态：1-正常，2-禁用，3-注销
     */
    private Integer status;

    /**
     * 扩展字段（如性别、签名等小众信息）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
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
        ChatUser other = (ChatUser) that;
        return (this.id == null ? other.id == null : this.id.equals(other.id))
            && (this.username == null ? other.username == null : this.username.equals(other.username))
            && (this.nickname == null ? other.nickname == null : this.nickname.equals(other.nickname))
            && (this.avatar == null ? other.avatar == null : this.avatar.equals(other.avatar))
            && (this.phone == null ? other.phone == null : this.phone.equals(other.phone))
            && (this.email == null ? other.email == null : this.email.equals(other.email))
            && (this.password == null ? other.password == null : this.password.equals(other.password))
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
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        result = prime * result + ((nickname == null) ? 0 : nickname.hashCode());
        result = prime * result + ((avatar == null) ? 0 : avatar.hashCode());
        result = prime * result + ((phone == null) ? 0 : phone.hashCode());
        result = prime * result + ((email == null) ? 0 : email.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
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
        sb.append(", username=").append(username);
        sb.append(", nickname=").append(nickname);
        sb.append(", avatar=").append(avatar);
        sb.append(", phone=").append(phone);
        sb.append(", email=").append(email);
        sb.append(", password=").append(password);
        sb.append(", status=").append(status);
        sb.append(", extInfo=").append(extInfo);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append("]");
        return sb.toString();
    }
}