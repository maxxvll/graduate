package com.maxxvll.service.impl;

import com.maxxvll.common.config.PermissionConfig;
import com.maxxvll.common.constants.RedisKeyConstants;
import com.maxxvll.common.enums.Permission;
import com.maxxvll.common.enums.Role;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.service.PermissionService;
import com.maxxvll.utils.RedissonCacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 权限服务实现
 *
 * @author maxxvll
 * @since 2026-03-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final ChatUserMapper chatUserMapper;
    private final RedissonCacheUtil cacheUtil;
    private final PermissionConfig permissionConfig;

    /**
     * 权限缓存 Key 前缀
     */
    private static final String PERMISSION_CACHE_PREFIX = RedisKeyConstants.USER_PERMISSION;

    /**
     * 角色缓存 Key 前缀
     */
    private static final String ROLE_CACHE_PREFIX = RedisKeyConstants.USER_ROLE;

    @Override
    public boolean hasPermission(String userId, String permission) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }

        // 管理员拥有所有权限
        if (isAdmin(userId)) {
            return true;
        }

        Set<String> userPermissions = getUserPermissions(userId);
        return userPermissions.contains(permission);
    }

    @Override
    public boolean hasAllPermissions(String userId, String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return true;
        }

        // 管理员拥有所有权限
        if (isAdmin(userId)) {
            return true;
        }

        Set<String> userPermissions = getUserPermissions(userId);
        for (String permission : permissions) {
            if (!userPermissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasAnyPermission(String userId, String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return true;
        }

        // 管理员拥有所有权限
        if (isAdmin(userId)) {
            return true;
        }

        Set<String> userPermissions = getUserPermissions(userId);
        for (String permission : permissions) {
            if (userPermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasRole(String userId, Role role) {
        if (role == null) {
            return true;
        }

        Set<Role> userRoles = getUserRoles(userId);
        return userRoles.contains(role);
    }

    @Override
    public boolean hasAllRoles(String userId, Role... roles) {
        if (roles == null || roles.length == 0) {
            return true;
        }

        Set<Role> userRoles = getUserRoles(userId);
        for (Role role : roles) {
            if (!userRoles.contains(role)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasAnyRole(String userId, Role... roles) {
        if (roles == null || roles.length == 0) {
            return true;
        }

        Set<Role> userRoles = getUserRoles(userId);
        for (Role role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> getUserPermissions(String userId) {
        String cacheKey = PERMISSION_CACHE_PREFIX + ":" + userId;

        // 先从缓存获取
        if (permissionConfig.isCacheEnabled()) {
            Object cached = cacheUtil.get(cacheKey);
            if (cached != null) {
                if (cached instanceof Set) {
                    @SuppressWarnings("unchecked")
                    Set<String> permissions = (Set<String>) cached;
                    log.debug("从缓存获取用户[{}]权限: {}", userId, permissions);
                    return permissions;
                }
            }
        }

        // 从数据库获取
        Set<String> permissions = loadUserPermissions(userId);

        // 写入缓存
        if (permissionConfig.isCacheEnabled()) {
            cacheUtil.set(cacheKey, permissions, permissionConfig.getCacheTtlSeconds(), TimeUnit.SECONDS);
            log.debug("缓存用户[{}]权限: {}", userId, permissions);
        }

        return permissions;
    }

    @Override
    public Set<Role> getUserRoles(String userId) {
        String cacheKey = ROLE_CACHE_PREFIX + ":" + userId;

        // 先从缓存获取
        if (permissionConfig.isCacheEnabled()) {
            Object cached = cacheUtil.get(cacheKey);
            if (cached != null) {
                if (cached instanceof Set) {
                    @SuppressWarnings("unchecked")
                    Set<Role> roles = (Set<Role>) cached;
                    log.debug("从缓存获取用户[{}]角色: {}", userId, roles);
                    return roles;
                }
            }
        }

        // 从数据库获取
        Set<Role> roles = loadUserRoles(userId);

        // 写入缓存
        if (permissionConfig.isCacheEnabled()) {
            cacheUtil.set(cacheKey, roles, permissionConfig.getCacheTtlSeconds(), TimeUnit.SECONDS);
            log.debug("缓存用户[{}]角色: {}", userId, roles);
        }

        return roles;
    }

    @Override
    public void clearUserPermissionCache(String userId) {
        String permissionCacheKey = PERMISSION_CACHE_PREFIX + ":" + userId;
        String roleCacheKey = ROLE_CACHE_PREFIX + ":" + userId;

        cacheUtil.delete(permissionCacheKey);
        cacheUtil.delete(roleCacheKey);

        log.info("清除用户[{}]权限缓存", userId);
    }

    @Override
    public void initUserDefaultPermissions(String userId) {
        // 新用户默认分配普通用户角色
        Set<Role> defaultRoles = new HashSet<>();
        defaultRoles.add(Role.USER);

        Set<String> defaultPermissions = new HashSet<>();
        // 分配默认权限
        for (Permission permission : Permission.values()) {
            if (!permission.isAdminPermission()) {
                defaultPermissions.add(permission.getCode());
            }
        }

        // 缓存默认权限和角色
        if (permissionConfig.isCacheEnabled()) {
            String permissionCacheKey = PERMISSION_CACHE_PREFIX + ":" + userId;
            String roleCacheKey = ROLE_CACHE_PREFIX + ":" + userId;

            cacheUtil.set(permissionCacheKey, defaultPermissions, permissionConfig.getCacheTtlSeconds(), TimeUnit.SECONDS);
            cacheUtil.set(roleCacheKey, defaultRoles, permissionConfig.getCacheTtlSeconds(), TimeUnit.SECONDS);

            log.info("初始化用户[{}]默认权限和角色", userId);
        }
    }

    /**
     * 从数据库加载用户权限
     */
    private Set<String> loadUserPermissions(String userId) {
        // 获取用户角色
        Set<Role> roles = loadUserRoles(userId);

        // 根据角色分配权限
        Set<String> permissions = new HashSet<>();

        for (Role role : roles) {
            permissions.addAll(getPermissionsByRole(role));
        }

        // 如果有管理员角色，添加所有非管理权限
        if (roles.contains(Role.ADMIN) || roles.contains(Role.SUPER_ADMIN)) {
            for (Permission permission : Permission.values()) {
                if (!permission.isSystemPermission()) {
                    permissions.add(permission.getCode());
                }
            }
        }

        // 超级管理员拥有所有权限
        if (roles.contains(Role.SUPER_ADMIN)) {
            for (Permission permission : Permission.values()) {
                permissions.add(permission.getCode());
            }
        }

        return permissions;
    }

    /**
     * 从数据库加载用户角色
     */
    private Set<Role> loadUserRoles(String userId) {
        Set<Role> roles = new HashSet<>();

        // 从用户表获取用户信息
        ChatUser user = chatUserMapper.selectById(userId);

        if (user != null && user.getRole() != null) {
            // 根据 role 字段设置角色
            // role 可以是字符串：USER, ADMIN, SUPER_ADMIN
            // 也可以是数字：1, 2, 3
            try {
                // 先尝试作为 Role 枚举名解析
                Role role = Role.valueOf(user.getRole().toUpperCase());
                roles.add(role);
            } catch (IllegalArgumentException e) {
                // 如果不是枚举名，尝试作为数字解析
                try {
                    Role role = Role.getByCode(Integer.parseInt(user.getRole()));
                    if (role != null) {
                        roles.add(role);
                    }
                } catch (NumberFormatException ex) {
                    // 解析失败，添加默认角色
                    roles.add(Role.USER);
                }
            }
        }

        // 默认添加普通用户角色
        if (roles.isEmpty()) {
            roles.add(Role.USER);
        }

        return roles;
    }

    /**
     * 根据角色获取权限列表
     */
    private Set<String> getPermissionsByRole(Role role) {
        Set<String> permissions = new HashSet<>();

        switch (role) {
            case SUPER_ADMIN:
                // 超级管理员拥有所有权限
                for (Permission permission : Permission.values()) {
                    permissions.add(permission.getCode());
                }
                break;
            case ADMIN:
                // 管理员拥有大部分权限，但不包含系统配置
                for (Permission permission : Permission.values()) {
                    if (!permission.isSystemPermission() ||
                        permission == Permission.SYSTEM_LOG) {
                        permissions.add(permission.getCode());
                    }
                }
                break;
            case USER:
            default:
                // 普通用户只有基本权限
                permissions.add(Permission.USER_READ.getCode());
                permissions.add(Permission.USER_WRITE.getCode());
                permissions.add(Permission.FRIEND_READ.getCode());
                permissions.add(Permission.FRIEND_ADD.getCode());
                permissions.add(Permission.FRIEND_DELETE.getCode());
                permissions.add(Permission.GROUP_READ.getCode());
                permissions.add(Permission.GROUP_CREATE.getCode());
                permissions.add(Permission.GROUP_UPDATE.getCode());
                permissions.add(Permission.GROUP_MEMBER_ADD.getCode());
                permissions.add(Permission.GROUP_MEMBER_REMOVE.getCode());
                permissions.add(Permission.MESSAGE_SEND.getCode());
                permissions.add(Permission.MESSAGE_READ.getCode());
                permissions.add(Permission.MESSAGE_RECALL.getCode());
                permissions.add(Permission.FILE_UPLOAD.getCode());
                permissions.add(Permission.FILE_DOWNLOAD.getCode());
                permissions.add(Permission.FILE_DELETE.getCode());
                permissions.add(Permission.CLOUD_READ.getCode());
                permissions.add(Permission.CLOUD_UPLOAD.getCode());
                permissions.add(Permission.CLOUD_DOWNLOAD.getCode());
                permissions.add(Permission.CLOUD_DELETE.getCode());
                permissions.add(Permission.CLOUD_SHARE.getCode());
                permissions.add(Permission.VOICE_CALL.getCode());
                break;
        }

        return permissions;
    }

    /**
     * 检查用户是否为管理员
     */
    private boolean isAdmin(String userId) {
        Set<Role> roles = getUserRoles(userId);
        return roles.contains(Role.ADMIN) || roles.contains(Role.SUPER_ADMIN);
    }
}
