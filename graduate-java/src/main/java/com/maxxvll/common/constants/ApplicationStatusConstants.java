package com.maxxvll.common.constants;

/**
 * @deprecated 已迁移到 {@link com.maxxvll.common.enums.ApplicationStatus}
 *
 * @author maxxvll
 * @since 2026-03-16
 */
@Deprecated
public final class ApplicationStatusConstants {

    private ApplicationStatusConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 待处理
     * @deprecated 请使用 {@link com.maxxvll.common.enums.ApplicationStatus#PENDING}
     */
    @Deprecated
    public static final int STATUS_PENDING = 0;

    /**
     * 已通过/已接受
     * @deprecated 请使用 {@link com.maxxvll.common.enums.ApplicationStatus#APPROVED}
     */
    @Deprecated
    public static final int STATUS_APPROVED = 1;

    /**
     * 已拒绝
     * @deprecated 请使用 {@link com.maxxvll.common.enums.ApplicationStatus#REJECTED}
     */
    @Deprecated
    public static final int STATUS_REJECTED = 2;

    /**
     * @deprecated 请使用 {@link com.maxxvll.common.enums.ApplicationStatus#getStatusDesc(Integer)}
     */
    @Deprecated
    public static String getStatusDesc(Integer status) {
        return com.maxxvll.common.enums.ApplicationStatus.getStatusDesc(status);
    }
}
