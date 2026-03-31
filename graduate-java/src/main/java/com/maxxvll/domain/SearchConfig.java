package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 搜索配置
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
@TableName("search_config")
public class SearchConfig {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置描述
     */
    private String description;

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

    // ==================== 配置键常量 ====================

    public static final String KEY_MAX_HISTORY_COUNT = "search.max_history_count";
    public static final String KEY_ENABLE_HIGHLIGHT = "search.enable_highlight";
    public static final String KEY_MESSAGE_MAX_RESULTS = "search.message.max_results";
    public static final String KEY_CONTACT_MAX_RESULTS = "search.contact.max_results";
    public static final String KEY_GROUP_MAX_RESULTS = "search.group.max_results";

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
