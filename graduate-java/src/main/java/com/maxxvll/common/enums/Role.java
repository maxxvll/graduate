package com.maxxvll.common.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 *
 * @author maxxvll
 * @since 2026-03-31
 */
@Getter
public enum Role {

    /**
     * 普通用户
     */
    USER(1, "普通用户"),

    /**
     * 管理员
     */
    ADMIN(2, "管理员"),

    /**
     * 超级管理员
     */
    SUPER_ADMIN(3, "超级管理员");

    private final Integer code;
    private final String desc;

    Role(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据角色码获取枚举
     */
    public static Role getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (Role role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return null;
    }

    /**
     * 是否为管理员或更高
     */
    public boolean isAdminOrAbove() {
        return this == ADMIN || this == SUPER_ADMIN;
    }

    /**
     * 是否为超级管理员
     */
    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN;
    }

    /**
     * 是否为普通用户
     */
    public boolean isUser() {
        return this == USER;
    }
}
