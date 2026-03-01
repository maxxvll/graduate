package com.maxxvll.common.dto;

import lombok.Data;

/**
 * 转让群主DTO
 */
@Data
public class GroupTransferDTO {
    
    /**
     * 群ID
     */
    private String groupId;
    
    /**
     * 新群主ID
     */
    private String newOwnerId;
}
