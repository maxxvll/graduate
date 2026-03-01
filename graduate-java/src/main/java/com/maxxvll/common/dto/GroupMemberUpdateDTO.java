package com.maxxvll.common.dto;

import lombok.Data;

/**
 * 更新群成员信息DTO
 */
@Data
public class GroupMemberUpdateDTO {
    
    /**
     * 群ID
     */
    private String groupId;
    
    /**
     * 成员ID
     */
    private String userId;
    
    /**
     * 成员角色：1-群主，2-管理员，3-普通成员
     */
    private Integer role;
    
    /**
     * 是否禁言：0-否，1-是
     */
    private Integer isMute;
}
