package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 消息阅读状态
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
@TableName("message_read_status")
public class MessageReadStatus {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 阅读时间
     */
    private Date readTime;

    /**
     * 阅读设备
     */
    private String readDevice;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createTime;

    public Date getCreatedAt() {
        return createTime;
    }

    public void setCreatedAt(Date createdAt) {
        this.createTime = createdAt;
    }
}
