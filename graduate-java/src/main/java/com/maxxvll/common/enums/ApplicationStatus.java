package com.maxxvll.common.enums;

import lombok.Getter;

/**
 * 申请状态枚举
 *
 * <p>用于好友申请、群申请等场景</p>
 *
 * @author maxxvll
 * @since 2026-03-16
 */
@Getter
public enum ApplicationStatus {

    /**
     * 待处理
     */
    PENDING(0, "待处理"),

    /**
     * 已通过/已接受
     */
    APPROVED(1, "已通过"),

    /**
     * 已拒绝
     */
    REJECTED(2, "已拒绝");

    private final Integer code;
    private final String desc;

    ApplicationStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return ApplicationStatus 或 null
     */
    public static ApplicationStatus getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ApplicationStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 获取状态描述
     *
     * @param code 状态码
     * @return 状态描述，未知状态返回"未知"
     */
    public static String getStatusDesc(Integer code) {
        ApplicationStatus status = getByCode(code);
        return status != null ? status.getDesc() : "未知";
    }

    /**
     * 是否为待处理状态
     */
    public boolean isPending() {
        return this == PENDING;
    }

    /**
     * 是否为已通过状态
     */
    public boolean isApproved() {
        return this == APPROVED;
    }

    /**
     * 是否为已拒绝状态
     */
    public boolean isRejected() {
        return this == REJECTED;
    }
}
