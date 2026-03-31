package com.maxxvll.common.constants;

/**
 * 聊天相关常量
 *
 * @deprecated 请使用 {@link BusinessConstants} 中的相关常量
 * @author maxxvll
 */
@Deprecated
public interface ChatConstants {

    /**
     * 消息撤回时间限制（毫秒）
     * @deprecated 请使用 {@link BusinessConstants#MESSAGE_REVOKE_TIME_LIMIT_MS}
     */
    @Deprecated
    long REVOKE_TIME_LIMIT = BusinessConstants.MESSAGE_REVOKE_TIME_LIMIT_MS;

    /**
     * 默认分页大小
     * @deprecated 请使用 {@link BusinessConstants#DEFAULT_MESSAGE_PAGE_SIZE}
     */
    @Deprecated
    int DEFAULT_PAGE_SIZE = BusinessConstants.DEFAULT_MESSAGE_PAGE_SIZE;
}
