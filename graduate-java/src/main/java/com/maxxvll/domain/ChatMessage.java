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
}