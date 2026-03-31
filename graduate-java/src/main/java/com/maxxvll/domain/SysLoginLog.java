package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统登录日志
 * <p>
 * 记录用户的登录行为，用于安全审计
 * </p>
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
@TableName(value = "sys_login_log", autoResultMap = true)
public class SysLoginLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID(登录成功后)
     */
    private String userId;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * 登录方式: PASSWORD-密码登录, QR_CODE-扫码登录, EMAIL-邮箱登录
     */
    private String loginType;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * IP归属地
     */
    private String ipLocation;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 完整UserAgent
     */
    private String userAgent;

    /**
     * 登录状态: 0-失败, 1-成功
     */
    private Boolean loginStatus;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 登录时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime loginTime;

    /**
     * 登出时间
     */
    private LocalDateTime logoutTime;

    /**
     * 在线时长(秒)
     */
    private Integer onlineDuration;

    /**
     * 是否可疑: 0-正常, 1-可疑
     */
    private Boolean isSuspicious;

    /**
     * 可疑原因
     */
    private String suspiciousReason;

    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * 登录方式枚举
     */
    public enum LoginType {
        PASSWORD("密码登录"),
        QR_CODE("扫码登录"),
        EMAIL("邮箱登录");

        private final String description;

        LoginType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
