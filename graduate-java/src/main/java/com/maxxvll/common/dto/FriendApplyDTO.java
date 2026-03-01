package com.maxxvll.common.dto;

import com.maxxvll.common.annotation.NotRequired;
import lombok.Data;

/**
 * 发送好友申请DTO
 */
@Data
public class FriendApplyDTO {

    /**
     * 目标用户ID
     */
    private String targetId;

    /**
     * 申请备注/申请原因（选填，最多100字）
     */
    @NotRequired
    private String remark;
    
    /**
     * 设备类型（前端设备标识，用于区分不同设备的请求）
     */
    @NotRequired
    private String deviceType;
}
