package com.maxxvll.common.aspect;

import com.maxxvll.common.annotation.RequirePermission;
import com.maxxvll.common.config.PermissionConfig;
import com.maxxvll.common.enums.ErrorCode;
import com.maxxvll.common.enums.Role;
import com.maxxvll.service.PermissionService;
import com.maxxvll.utils.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * 权限验证切面
 *
 * <p>拦截标记了 @RequirePermission 注解的方法，进行权限检查</p>
 *
 * <p>支持以下验证模式：</p>
 * <ul>
 *   <li>AND_ROLE_PERMISSION: 角色和权限同时满足</li>
 *   <li>OR_ROLE_PERMISSION: 角色或权限满足其一</li>
 *   <li>ROLE_ONLY: 仅验证角色</li>
 *   <li>PERMISSION_ONLY: 仅验证权限</li>
 * </ul>
 *
 * @author maxxvll
 * @since 2026-03-16
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;
    private final PermissionConfig permissionConfig;
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 前置通知：在方法执行前进行权限验证
     */
    @Before("@annotation(com.maxxvll.common.annotation.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {
        // 检查权限验证是否启用
        if (!permissionConfig.isEnabled()) {
            if (log.isDebugEnabled()) {
                log.debug("权限验证已禁用，跳过检查");
            }
            return;
        }

        // 1. 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission requirePermission = method.getAnnotation(RequirePermission.class);

        if (requirePermission == null) {
            return;
        }

        // 2. 获取当前用户ID
        String currentUserId;
        try {
            currentUserId = UserContextUtil.getCurrentUserId();
            if (currentUserId == null) {
                throw new RuntimeException("用户未登录");
            }
        } catch (Exception e) {
            log.warn("权限验证失败 - 用户未登录: {}", e.getMessage());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }

        // 3. 获取资源ID（如果配置了 resourceParam）
        String resourceId = extractResourceId(joinPoint, method, requirePermission.resourceParam());

        // 4. 检查权限
        checkUserPermission(currentUserId, resourceId, requirePermission);
    }

    /**
     * 从方法参数中提取资源ID
     */
    private String extractResourceId(JoinPoint joinPoint, Method method, String resourceParam) {
        if (resourceParam == null || resourceParam.isEmpty()) {
            return null;
        }

        try {
            // 获取方法参数名
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
            if (parameterNames == null) {
                return null;
            }

            // 获取方法参数值
            Object[] args = joinPoint.getArgs();

            // 查找匹配的参数
            for (int i = 0; i < parameterNames.length; i++) {
                if (parameterNames[i].equals(resourceParam)) {
                    Object arg = args[i];
                    if (arg != null) {
                        return arg.toString();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("提取资源ID失败: {}", e.getMessage());
        }

        log.debug("未找到资源参数: {}", resourceParam);
        return null;
    }

    /**
     * 检查用户是否具有所需权限
     */
    private void checkUserPermission(String userId, String resourceId, RequirePermission requirePermission) {
        String[] requiredPermissions = requirePermission.value();
        Role[] requiredRoles = requirePermission.roles();
        RequirePermission.PermissionMode mode = requirePermission.mode();
        boolean requireAll = requirePermission.requireAll();
        boolean loggable = requirePermission.loggable();

        // 如果没有指定权限和角色要求，直接通过
        if ((requiredPermissions == null || requiredPermissions.length == 0)
                && (requiredRoles == null || requiredRoles.length == 0)) {
            return;
        }

        boolean hasPermission = true;
        boolean hasRole = true;

        // 检查权限要求
        if (requiredPermissions != null && requiredPermissions.length > 0) {
            if (requireAll) {
                hasPermission = permissionService.hasAllPermissions(userId, requiredPermissions);
            } else {
                hasPermission = permissionService.hasAnyPermission(userId, requiredPermissions);
            }
        }

        // 检查角色要求
        if (requiredRoles != null && requiredRoles.length > 0) {
            hasRole = permissionService.hasAnyRole(userId, requiredRoles);
        }

        // 根据模式判断是否通过
        boolean passed = switch (mode) {
            case AND_ROLE_PERMISSION -> hasPermission && hasRole;
            case OR_ROLE_PERMISSION -> hasPermission || hasRole;
            case ROLE_ONLY -> hasRole;
            case PERMISSION_ONLY -> hasPermission;
        };

        // 记录日志
        if (loggable) {
            if (passed) {
                log.debug("权限验证通过 - userId: {}, permissions: {}, roles: {}, mode: {}, resourceId: {}",
                        userId,
                        Arrays.toString(requiredPermissions),
                        Arrays.toString(requiredRoles),
                        mode,
                        resourceId);
            } else {
                log.warn("权限验证失败 - userId: {}, permissions: {}, roles: {}, mode: {}, hasPermission: {}, hasRole: {}, resourceId: {}",
                        userId,
                        Arrays.toString(requiredPermissions),
                        Arrays.toString(requiredRoles),
                        mode,
                        hasPermission,
                        hasRole,
                        resourceId);
            }
        }

        if (!passed) {
            throw new BusinessException(ErrorCode.FORBIDDEN, requirePermission.message());
        }
    }

    /**
     * 业务异常（内部类）
     */
    private static class BusinessException extends RuntimeException {
        private final int code;
        private final String message;

        public BusinessException(String message) {
            super(message);
            this.code = ErrorCode.FORBIDDEN.getCode();
            this.message = message;
        }

        public BusinessException(ErrorCode errorCode, String message) {
            super(message);
            this.code = errorCode.getCode();
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}
