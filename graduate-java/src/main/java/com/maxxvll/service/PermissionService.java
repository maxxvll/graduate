package com.maxxvll.service;

import com.maxxvll.common.config.PermissionConfig;
import com.maxxvll.common.enums.Permission;
import com.maxxvll.common.enums.Role;

import java.util.Set;

/**
 * 权限服务接口
 *
 * @author maxxvll
 * @since 2026-03-31
 */
public interface PermissionService {

    /**
     * 检查用户是否拥有指定权限
     *
     * @param userId      用户ID
     * @param permission  权限标识
     * @return 是否拥有权限
     */
    boolean hasPermission(String userId, String permission);

    /**
     * 检查用户是否拥有所有指定权限
     *
     * @param userId       用户ID
     * @param permissions  权限标识数组
     * @return 是否拥有所有权限
     */
    boolean hasAllPermissions(String userId, String... permissions);

    /**
     * 检查用户是否拥有任一指定权限
     *
     * @param userId       用户ID
     * @param permissions  权限标识数组
     * @return 是否拥有任一权限
     */
    boolean hasAnyPermission(String userId, String... permissions);

    /**
     * 检查用户是否拥有指定角色
     *
     * @param userId  用户ID
     * @param role    角色
     * @return 是否拥有角色
     */
    boolean hasRole(String userId, Role role);

    /**
     * 检查用户是否拥有所有指定角色
     *
     * @param userId  用户ID
     * @param roles   角色数组
     * @return 是否拥有所有角色
     */
    boolean hasAllRoles(String userId, Role... roles);

    /**
     * 检查用户是否拥有任一指定角色
     *
     * @param userId  用户ID
     * @param roles   角色数组
     * @return 是否拥有任一角色
     */
    boolean hasAnyRole(String userId, Role... roles);

    /**
     * 获取用户的所有权限
     *
     * @param userId 用户ID
     * @return 权限集合
     */
    Set<String> getUserPermissions(String userId);

    /**
     * 获取用户的所有角色
     *
     * @param userId 用户ID
     * @return 角色集合
     */
    Set<Role> getUserRoles(String userId);

    /**
     * 清除用户权限缓存
     *
     * @param userId 用户ID
     */
    void clearUserPermissionCache(String userId);

    /**
     * 初始化用户默认权限
     *
     * @param userId 用户ID
     */
    void initUserDefaultPermissions(String userId);
}
