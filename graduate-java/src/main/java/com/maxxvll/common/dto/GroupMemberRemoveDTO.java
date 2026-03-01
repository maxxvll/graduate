package com.maxxvll.common.dto;

import lombok.Data;

/**
 * 移除群成员DTO
 */
@Data
public class GroupMemberRemoveDTO {
    
    /**
     * 群ID
     */
    private String groupId;
    
    /**
     * 要移除的成员ID
     */
    private String userId;
    
    /**
     * 移除原因
     */
    private String reason;
}
