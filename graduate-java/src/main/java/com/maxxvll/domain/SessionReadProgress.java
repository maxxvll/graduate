package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 会话阅读进度
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
@TableName("session_read_progress")
public class SessionReadProgress {

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
     * 最后阅读的消息ID
     */
    private Long lastReadMessageId;

    /**
     * 最后阅读时间
     */
    private Date lastReadTime;

    /**
     * 当前未读数
     */
    private Integer unreadCount;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 设备唯一标识
     */
    private String deviceId;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    // ==================== 自动生成的 getter/setter 通过 Lombok @Data ====================
    public Date getUpdatedAt() {
        return updateTime;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updateTime = updatedAt;
    }
}
