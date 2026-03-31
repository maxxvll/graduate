package com.maxxvll.common.vo;

import lombok.Builder;
import lombok.Data;

/**
 * WebSocket 已读同步推送载荷
 */
@Data
@Builder
public class ReadSyncPushVO {

    /**
     * 推送类型
     */
    private String type;

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 最后已读消息 ID
     */
    private Long lastReadMessageId;

    /**
     * 发起同步的设备类型
     */
    private String sourceDeviceType;

    /**
     * 发起同步的设备 ID
     */
    private String sourceDeviceId;

    /**
     * 推送时间戳
     */
    private Long timestamp;
}
