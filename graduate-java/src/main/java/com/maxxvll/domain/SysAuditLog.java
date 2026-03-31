package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 敏感操作审计日志
 * <p>
 * 记录高危操作，用于安全审计
 * </p>
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
@TableName(value = "sys_audit_log", autoResultMap = true)
public class SysAuditLog implements Serializable {
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
     * 审计类型
     */
    private String auditType;

    /**
     * 操作动作
     */
    private String action;

    /**
     * 操作用户ID
     */
    private String userId;

    /**
     * 操作用户名
     */
    private String username;

    /**
     * 目标类型: USER-用户, ROLE-角色, PERMISSION-权限, GROUP-群组
     */
    private String targetType;

    /**
     * 目标ID
     */
    private String targetId;

    /**
     * 目标名称
     */
    private String targetName;

    /**
     * 变更前值(JSON)
     */
    private String beforeValue;

    /**
     * 变更后值(JSON)
     */
    private String afterValue;

    /**
     * 响应结果(JSON)
     */
    private String result;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 请求URI
     */
    private String requestUri;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 请求参数
     */
    private String params;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 风险等级: LOW-低, MEDIUM-中, HIGH-高, CRITICAL-严重
     */
    private String riskLevel;

    /**
     * 执行耗时(毫秒)
     */
    private Integer costTime;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 审计类型枚举
     */
    public enum AuditType {
        PERMISSION("权限变更"),
        USER_MANAGEMENT("用户管理"),
        SYSTEM_CONFIG("系统配置"),
        FILE_ACCESS("文件访问"),
        DATA_EXPORT("数据导出");

        private final String description;

        AuditType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 风险等级枚举
     */
    public enum RiskLevel {
        LOW("低风险"),
        MEDIUM("中风险"),
        HIGH("高风险"),
        CRITICAL("严重风险");

        private final String description;

        RiskLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
