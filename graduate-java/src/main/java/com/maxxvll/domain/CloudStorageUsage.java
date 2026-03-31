package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 云存储空间使用记录表
 * @TableName cloud_storage_usage
 */
@Data
@TableName("cloud_storage_usage")
public class CloudStorageUsage {

    /**
     * 用户ID（主键）
     */
    @TableId
    private String userId;

    /**
     * 已使用空间（字节）
     */
    private Long usedBytes;

    /**
     * 存储配额（字节），默认10GB
     */
    private Long quotaBytes;

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

    /**
     * 获取使用百分比
     */
    public Double getUsagePercent() {
        if (quotaBytes == null || quotaBytes <= 0) {
            return null;
        }
        return (double) usedBytes / quotaBytes * 100;
    }

    /**
     * 获取剩余空间（字节）
     */
    public Long getRemainingBytes() {
        if (quotaBytes == null || quotaBytes <= 0) {
            return null;
        }
        return Math.max(0, quotaBytes - usedBytes);
    }

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
