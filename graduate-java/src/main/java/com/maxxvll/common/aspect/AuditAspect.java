package com.maxxvll.common.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.maxxvll.common.annotation.Audit;
import com.maxxvll.common.logging.MdcHelper;
import com.maxxvll.domain.SysAuditLog;
import com.maxxvll.service.SysAuditLogService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 审计日志切面
 * <p>
 * 记录被 @Audit 注解标记的敏感操作
 * </p>
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Slf4j
@Aspect
@Component
public class AuditAspect {

    @Resource
    private SysAuditLogService sysAuditLogService;

    @Around("@annotation(audit)")
    public Object doAround(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 构建审计日志基本信息
        SysAuditLog auditLog = new SysAuditLog();
        auditLog.setAuditType(audit.auditType());
        auditLog.setAction(audit.action());
        auditLog.setRiskLevel(audit.riskLevel());
        auditLog.setClientIp(getClientIp());
        auditLog.setRequestUri(getRequestUri());
        auditLog.setMethod(joinPoint.getSignature().toShortString());

        // 获取操作用户信息
        try {
            if (StpUtil.isLogin()) {
                auditLog.setUserId(StpUtil.getLoginIdAsString());
                auditLog.setUsername(getCurrentUsername());
            }
        } catch (Exception e) {
            log.debug("获取用户信息失败", e);
        }

        // 获取 MDC 中的 traceId
        auditLog.setTraceId(MdcHelper.getTraceId());

        // 记录请求参数
        if (audit.saveParams()) {
            auditLog.setParams(getParams(joinPoint, audit));
        }

        Object result = null;
        boolean success = true;
        String errorMessage = null;

        try {
            // 执行目标方法
            result = joinPoint.proceed();

            // 记录响应结果
            if (audit.saveResult() && result != null) {
                try {
                    auditLog.setResult(JSON.toJSONString(result));
                } catch (Exception e) {
                    log.debug("序列化响应结果失败", e);
                }
            }

            return result;

        } catch (Throwable e) {
            success = false;
            errorMessage = e.getMessage();
            throw e;

        } finally {
            // 计算执行时间
            long costTime = System.currentTimeMillis() - startTime;
            auditLog.setCostTime((int) costTime);
            auditLog.setSuccess(success);
            auditLog.setErrorMessage(errorMessage);

            // 记录审计日志
            recordAuditLog(auditLog, audit.riskLevel());

            // 高风险操作打印警告日志
            if (SysAuditLog.RiskLevel.CRITICAL.name().equals(audit.riskLevel()) ||
                SysAuditLog.RiskLevel.HIGH.name().equals(audit.riskLevel())) {
                log.warn("[高风险审计] 类型={}, 动作={}, 用户={}, 目标={}, 风险={}, 耗时={}ms, 成功={}",
                        audit.auditType(), audit.action(), auditLog.getUsername(),
                        auditLog.getTargetName(), audit.riskLevel(), costTime, success);
            }
        }
    }

    /**
     * 记录审计日志
     */
    private void recordAuditLog(SysAuditLog auditLog, String riskLevel) {
        try {
            sysAuditLogService.saveAsync(auditLog);
        } catch (Exception e) {
            log.error("保存审计日志失败", e);
        }
    }

    /**
     * 获取请求参数
     */
    private String getParams(ProceedingJoinPoint joinPoint, Audit audit) {
        try {
            Object[] args = joinPoint.getArgs();
            String[] paramNames = getParamNames(joinPoint);
            Map<String, Object> params = new HashMap<>();

            for (int i = 0; i < args.length && i < paramNames.length; i++) {
                String paramName = paramNames[i];

                // 过滤敏感参数
                if (!isSensitiveParam(paramName, audit.sensitiveParams())) {
                    Object value = args[i];
                    if (value != null && !isExcludedType(value)) {
                        try {
                            params.put(paramName, JSON.toJSONString(value));
                        } catch (Exception e) {
                            params.put(paramName, value.toString());
                        }
                    }
                } else {
                    params.put(paramName, "***");
                }
            }

            return JSON.toJSONString(params);

        } catch (Exception e) {
            log.debug("获取请求参数失败", e);
            return null;
        }
    }

    /**
     * 判断参数是否为敏感参数
     */
    private boolean isSensitiveParam(String paramName, String[] sensitiveParams) {
        if (paramName == null || sensitiveParams == null) {
            return false;
        }
        String lowerParamName = paramName.toLowerCase();
        return Arrays.stream(sensitiveParams)
                .anyMatch(s -> s.toLowerCase().contains(lowerParamName) ||
                               lowerParamName.contains(s.toLowerCase()));
    }

    /**
     * 判断参数类型是否需要排除
     */
    private boolean isExcludedType(Object value) {
        return value instanceof HttpServletRequest ||
               value.getClass().getName().contains("Request") ||
               value.getClass().getName().contains("Response");
    }

    /**
     * 获取请求参数名称
     */
    private String[] getParamNames(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getParameterNames();
    }

    /**
     * 获取当前用户名
     */
    private String getCurrentUsername() {
        try {
            // 尝试从请求属性中获取
            HttpServletRequest request = getRequest();
            if (request != null) {
                Object username = request.getAttribute("username");
                if (username != null) {
                    return username.toString();
                }
            }
        } catch (Exception e) {
            log.debug("获取用户名失败", e);
        }
        return null;
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
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }

    /**
     * 获取请求URI
     */
    private String getRequestUri() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getRequestURI() : "unknown";
    }

    /**
     * 获取当前请求
     */
    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
