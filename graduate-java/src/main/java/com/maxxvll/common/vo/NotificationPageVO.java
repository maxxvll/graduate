package com.maxxvll.common.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 通知分页VO
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationPageVO extends CursorPageVO<NotificationVO> {

    /**
     * 未读通知数量
     */
    private Long unreadCount;

    public NotificationPageVO() {
    }

    public NotificationPageVO(List<NotificationVO> items, String nextCursor, Boolean hasMore, Long unreadCount) {
        super(items, nextCursor, hasMore);
        this.unreadCount = unreadCount;
    }

    public static NotificationPageVO empty() {
        NotificationPageVO page = new NotificationPageVO();
        page.setItems(List.of());
        page.setNextCursor(null);
        page.setHasMore(false);
        page.setUnreadCount(0L);
        return page;
    }
}
