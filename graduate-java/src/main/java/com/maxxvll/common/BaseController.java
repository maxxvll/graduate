package com.maxxvll.common;

import cn.dev33.satoken.stp.StpUtil;
import com.maxxvll.common.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制器基类
 * 所有业务控制器需继承此类，统一返回格式和通用方法
 * @RestController 注解：标记为REST控制器，返回JSON数据
 */
@RestController
public class BaseController {

    // ==================== 常量定义 ====================
    /**
     * 默认页码
     */
    protected static final int DEFAULT_PAGE_NUM = 1;

    /**
     * 默认每页大小
     */
    protected static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 最大每页大小（防止恶意请求）
     */
    protected static final int MAX_PAGE_SIZE = 100;

    /**
     * 最小页码
     */
    protected static final int MIN_PAGE_NUM = 1;

    // ==================== 成功返回快捷方法 ====================
    /**
     * 成功返回（无数据）
     */
    protected <T> Result<T> success() {
        return Result.success();
    }

    /**
     * 成功返回（仅消息）
     */
    protected Result<Void> success(String msg) {
        return Result.success(msg);
    }

    /**
     * 成功返回（带数据）
     */
    protected <T> Result<T> success(T data) {
        return Result.success(data);
    }

    /**
     * 成功返回（自定义提示语+数据）
     */
    protected <T> Result<T> success(String msg, T data) {
        return Result.success(msg, data);
    }

    // ==================== 失败返回快捷方法（多重载） ====================
    /**
     * 失败返回（默认错误）
     */
    protected <T> Result<T> fail() {
        return Result.fail();
    }

    /**
     * 失败返回（自定义提示语）
     */
    protected <T> Result<T> fail(String msg) {
        return Result.fail(msg);
    }

    /**
     * 失败返回（自定义错误码+提示语）
     */
    protected <T> Result<T> fail(int code, String msg) {
        return Result.fail(code, msg);
    }

    /**
     * 失败返回（错误码枚举）
     */
    protected <T> Result<T> fail(ErrorCode errorCode) {
        return Result.fail(errorCode);
    }

    /**
     * 失败返回（错误码枚举+自定义提示语）
     */
    protected <T> Result<T> fail(ErrorCode errorCode, String customMsg) {
        return Result.fail(errorCode, customMsg);
    }

    /**
     * 失败返回（错误码枚举+自定义提示语+错误数据）
     */
    protected <T> Result<T> fail(ErrorCode errorCode, String customMsg, T data) {
        return Result.fail(errorCode, customMsg, data);
    }

    // ==================== 用户上下文相关方法 ====================
    /**
     * 获取当前登录用户ID
     * @return 用户ID字符串
     */
    protected String getCurrentUserId() {
        return StpUtil.getLoginId().toString();
    }

    /**
     * 获取当前登录用户ID（Long类型）
     * @return 用户ID
     */
    protected Long getCurrentUserIdAsLong() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 检查用户是否已登录
     * @return 是否已登录
     */
    protected boolean isLogin() {
        return StpUtil.isLogin();
    }

    // ==================== 分页参数校验方法 ====================
    /**
     * 校验并规范化页码
     * @param pageNum 原始页码
     * @return 规范化后的页码（最小为1）
     */
    protected int normalizePageNum(Integer pageNum) {
        if (pageNum == null || pageNum < MIN_PAGE_NUM) {
            return DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    /**
     * 校验并规范化每页大小
     * @param pageSize 原始每页大小
     * @return 规范化后的每页大小（不超过最大值）
     */
    protected int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 校验并规范化游标分页的limit参数
     * @param limit 原始limit
     * @return 规范化后的limit（不超过最大值）
     */
    protected int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(limit, MAX_PAGE_SIZE);
    }

    // ==================== 客户端信息获取方法 ====================
    /**
     * 获取客户端IP地址
     * 优先从X-Forwarded-For头获取（考虑代理情况）
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    protected String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况（X-Forwarded-For可能包含多个IP）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 获取User-Agent
     * @param request HTTP请求
     * @return User-Agent字符串
     */
    protected String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}