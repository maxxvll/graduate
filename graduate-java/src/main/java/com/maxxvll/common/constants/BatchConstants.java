package com.maxxvll.common.constants;

/**
 * 批量操作常量
 *
 * @deprecated 请使用 {@link BusinessConstants} 中的相关常量
 *
 * @author backend-friend
 */
@Deprecated
public final class BatchConstants {

    private BatchConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 默认批量大小
     * @deprecated 请使用 {@link BusinessConstants#DEFAULT_BATCH_SIZE}
     */
    @Deprecated
    public static final int DEFAULT_BATCH_SIZE = BusinessConstants.DEFAULT_BATCH_SIZE;

    /**
     * 最大批量大小
     * @deprecated 请使用 {@link BusinessConstants#MAX_BATCH_SIZE}
     */
    @Deprecated
    public static final int MAX_BATCH_SIZE = BusinessConstants.MAX_BATCH_SIZE;

    /**
     * 最小批量大小
     * @deprecated 请使用 {@link BusinessConstants#MIN_BATCH_SIZE}
     */
    @Deprecated
    public static final int MIN_BATCH_SIZE = BusinessConstants.MIN_BATCH_SIZE;
}
