package com.maxxvll.common.dto;

import lombok.Data;

/**
 * 申请加入群聊DTO
 */
@Data
public class GroupApplyDTO {
    
    /**
     * 群ID
     */
    private Long groupId;
    
    /**
     * 申请备注信息
     */
    private String remark;
}
