package com.maxxvll.common.dto;

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
     * 申请备注
     */
    private String remark;
}
