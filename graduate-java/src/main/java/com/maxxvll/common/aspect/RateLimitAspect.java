package com.maxxvll.common.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.maxxvll.common.annotation.RateLimit;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.utils.RedissonCacheUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * 接口限流切面
 * <p>
 * 使用 Redis 滑动窗口算法实现接口限流
 * </p>
 *
 * <p><b>限流策略:</b></p>
 * <ul>
 *     <li>DEFAULT: 按 IP + 方法名限流</li>
 *     <li>IP: 按 IP 限流</li>
 *     <li>USER: 按用户ID限流（需要登录）</li>
 *     <li>CUSTOM: 按自定义 key 限流</li>
 * </ul>
 *
 * <p><b>限流算法:</b></p>
 * 使用 Redis 滑动时间窗口，确保在指定时间窗口内请求次数不超过限制
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    @Resource
    private RedissonCacheUtil redissonCacheUtil;

    /**
     * 限流拦截
     */
    @Before("@annotation(rateLimit)")
    public void doBefore(JoinPoint joinPoint, RateLimit rateLimit) {
        String limitKey = buildLimitKey(joinPoint, rateLimit);
        int limit = rateLimit.limit();
        int period = rateLimit.period();

        // 获取当前计数
        Integer currentCount = redissonCacheUtil.get(limitKey);
        if (currentCount == null) {
            currentCount = 0;
        }

        // 检查是否超过限制
        if (currentCount >= limit) {
            log.warn("接口限流触发: key={}, limit={}, period={}, current={}",
                    limitKey, limit, period, currentCount);
            throw new BusinessException(rateLimit.message());
        }

        // 递增计数
        long ttl = getTtl(limitKey);
        if (ttl <= 0) {
            // 第一次请求或已过期，设置新值
            redissonCacheUtil.set(limitKey, 1, period, TimeUnit.SECONDS);
        } else {
            // 递增现有值
            redissonCacheUtil.set(limitKey, currentCount + 1, (long) period, TimeUnit.SECONDS);
        }

        log.debug("接口限流检查通过: key={}, count={}/{}", limitKey, currentCount + 1, limit);
    }

    /**
     * 构建限流 key
     */
    private String buildLimitKey(JoinPoint joinPoint, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder(RATE_LIMIT_PREFIX);

        switch (rateLimit.limitType()) {
            case IP:
                keyBuilder.append("ip:").append(getClientIp());
                break;

            case USER:
                String userId = getCurrentUserId();
                if (userId == null) {
                    // 未登录用户使用 IP
                    keyBuilder.append("ip:").append(getClientIp());
                } else {
                    keyBuilder.append("user:").append(userId);
                }
                break;

            case CUSTOM:
                keyBuilder.append("custom:").append(rateLimit.key());
                break;

            case DEFAULT:
            default:
                keyBuilder.append("default:")
                        .append(getClientIp())
                        .append(":")
                        .append(getMethodName(joinPoint));
                break;
        }

        return keyBuilder.toString();
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况（X-Forwarded-For 可能包含多个IP）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }

    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        try {
            if (StpUtil.isLogin()) {
                return StpUtil.getLoginIdAsString();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取方法名
     */
    private String getMethodName(JoinPoint joinPoint) {
        return joinPoint.getSignature().getName();
    }

    /**
     * 获取当前请求
     */
    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 key 的剩余 TTL
     */
    private long getTtl(String key) {
        try {
            Long remainingTime = redissonCacheUtil.getRemainingTime(key);
            return remainingTime != null ? remainingTime : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
