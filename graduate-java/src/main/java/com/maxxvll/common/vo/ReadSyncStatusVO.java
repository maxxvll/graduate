package com.maxxvll.common.vo;

import lombok.Data;

import java.util.List;

/**
 * 已读同步状态VO
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
public class ReadSyncStatusVO {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 未读消息数量
     */
    private Integer unreadCount;

    /**
     * 最后阅读的消息ID
     */
    private Long lastReadMessageId;

    /**
     * 最后阅读时间
     */
    private Long lastReadTime;

    /**
     * 各设备的阅读进度
     */
    private List<DeviceReadProgress> deviceProgress;

    @Data
    public static class DeviceReadProgress {
        private String deviceId;
        private String deviceType;
        private Long lastReadMessageId;
        private Long lastReadTime;
        private Integer unreadCount;
    }
}
