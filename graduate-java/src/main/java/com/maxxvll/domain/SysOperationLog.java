package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统操作日志
 * <p>
 * 记录用户的操作行为，用于审计和追踪
 * </p>
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
@TableName(value = "sys_operation_log", autoResultMap = true)
public class SysOperationLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * 操作用户ID
     */
    private String userId;

    /**
     * 操作用户名
     */
    private String username;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 操作动作
     */
    private String action;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 请求URI
     */
    private String requestUri;

    /**
     * HTTP方法
     */
    private String httpMethod;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 请求参数(JSON格式)
     */
    private String params;

    /**
     * 响应结果(JSON格式)
     */
    private String result;

    /**
     * 是否成功: 0-失败, 1-成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行耗时(毫秒)
     */
    private Integer costTime;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
