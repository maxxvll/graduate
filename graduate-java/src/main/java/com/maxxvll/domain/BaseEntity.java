package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

/**
 * 实体基类
 * 所有数据库实体类应继承此类，统一管理公共字段
 *
 * 公共字段：
 * - id: 主键ID（雪花算法生成）
 * - createTime: 创建时间（插入时自动填充）
 * - updateTime: 更新时间（插入和更新时自动填充）
 *
 * @author backend
 */
@Data
public abstract class BaseEntity {

    /**
     * 主键ID（雪花算法生成）
     * 使用 ToStringSerializer 序列化为字符串，防止 JS Number 精度丢失
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

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
}
