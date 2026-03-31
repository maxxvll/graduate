package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 搜索历史记录
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
@TableName("search_history")
public class SearchHistory {

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
     * 搜索类型：MESSAGE/CONTACT/GROUP/ALL
     */
    private String searchType;

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 搜索次数
     */
    private Integer searchCount;

    /**
     * 是否删除
     */
    private Integer isDeleted;

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

    // ==================== 常量定义 ====================

    public static final String TYPE_MESSAGE = "MESSAGE";
    public static final String TYPE_CONTACT = "CONTACT";
    public static final String TYPE_GROUP = "GROUP";
    public static final String TYPE_ALL = "ALL";

    public static final int FLAG_NO = 0;
    public static final int FLAG_YES = 1;

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
