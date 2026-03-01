package com.maxxvll.common.dto;

import lombok.Data;

/**
 * 处理好友申请DTO
 */
@Data
public class FriendApplyHandleDTO {

    /**
     * 申请ID
     */
    private Long applyId;

    /**
     * 处理结果：1-接受 2-拒绝
     */
    private Integer status;

    /**
     * 拒绝原因（拒绝时填写）
     */
    private String rejectReason;
}
