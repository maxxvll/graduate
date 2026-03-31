package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 聊天离线游标表（记录用户最后阅读的消息位置）
 * @TableName chat_offline_cursor
 */
@Data
@TableName("chat_offline_cursor")
public class ChatOfflineCursor {

    /**
     * 用户ID（主键）
     */
    @TableId
    private String userId;

    /**
     * 最后读取的消息ID
     */
    private Long lastMessageId;

    /**
     * 最后读取消息时间
     */
    private Date lastMessageTime;

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
