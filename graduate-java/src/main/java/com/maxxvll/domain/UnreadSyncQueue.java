package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 未读消息同步队列
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
@TableName("unread_sync_queue")
public class UnreadSyncQueue {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 目标设备ID
     */
    private String targetDeviceId;

    /**
     * 同步类型
     */
    private String syncType;

    /**
     * 最后同步的消息ID
     */
    private Long lastSyncedMessageId;

    /**
     * 最后同步时间
     */
    private Date lastSyncedTime;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    // ==================== 自动生成的 getter/setter 通过 Lombok @Data ====================

    // ==================== 常量定义 ====================

    public static final String SYNC_TYPE_SESSION_READ = "SESSION_READ";
    public static final String SYNC_TYPE_ALL_READ = "ALL_READ";

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PROCESSED = 1;
    public static final int STATUS_FAILED = 2;

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
