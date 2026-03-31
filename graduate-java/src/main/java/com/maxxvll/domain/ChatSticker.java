package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 收藏表情包表
 * @TableName chat_sticker
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "chat_sticker", excludeProperty = {"createTime", "updateTime"})
public class ChatSticker extends BaseEntity {

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 表情名称
     */
    private String name;

    /**
     * 表情图片URL
     */
    private String url;

    /**
     * 表情分类（如：emoji, custom等）
     */
    private String category;
}
