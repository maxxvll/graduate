package com.maxxvll.common.dto;

import lombok.Data;

import java.util.List;

/**
 * 创建群聊DTO
 */
@Data
public class GroupCreateDTO {
    
    /**
     * 群名称
     */
    private String groupName;
    
    /**
     * 群头像URL
     */
    private String groupAvatar;
    
    /**
     * 群最大成员数（默认200）
     */
    private Integer maxMember;
    
    /**
     * 加群方式：1-需审核，2-免审核，3-仅邀请
     */
    private Integer joinType;
    
    /**
     * 初始成员ID列表（创建时邀请的成员）
     */
    private List<String> memberIds;
}
