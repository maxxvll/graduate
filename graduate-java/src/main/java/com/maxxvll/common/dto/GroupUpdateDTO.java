package com.maxxvll.common.dto;

import lombok.Data;

/**
 * 更新群聊信息DTO
 */
@Data
public class GroupUpdateDTO {
    
    /**
     * 群ID
     */
    private Long groupId;
    
    /**
     * 群名称
     */
    private String groupName;
    
    /**
     * 群头像URL
     */
    private String groupAvatar;
    
    /**
     * 群最大成员数
     */
    private Integer maxMember;
    
    /**
     * 加群方式：1-需审核，2-免审核，3-仅邀请
     */
    private Integer joinType;
    
    /**
     * 群公告
     */
    private String notice;
    
    /**
     * 是否全员禁言：0-否，1-是
     */
    private Integer isMuteAll;
}
