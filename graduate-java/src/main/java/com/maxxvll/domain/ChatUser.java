package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户基础信息表
 * @TableName chat_user
 */
@TableName(value = "chat_user", autoResultMap = true)
@Data
public class ChatUser {

    /**
     * 用户唯一ID（主键，String类型，非雪花ID）
     */
    @TableId(type = com.baomidou.mybatisplus.annotation.IdType.ASSIGN_ID)
    private String id;

    /**
     * 创建时间（插入时自动填充）
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间（插入和更新时自动填充）
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

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
     * 用户角色：USER-普通用户，ADMIN-管理员，SUPER_ADMIN-超级管理员
     */
    private String role;

    /**
     * 扩展字段（如性别、签名等小众信息）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object extInfo;

    public Date getCreatedAt() {
        return createTime;
    }

    public void setCreatedAt(Date createdAt) {
        this.createTime = createdAt;
    }

    public Date getUpdatedAt() {
        return updateTime;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updateTime = updatedAt;
    }

}
