package com.maxxvll.common.dto;

import lombok.Data;

import java.util.List;

/**
 * 添加群成员DTO
 */
@Data
public class GroupMemberAddDTO {
    
    /**
     * 群ID
     */
    private String groupId;
    
    /**
     * 要添加的成员ID列表
     */
    private List<String> userIds;
    
    /**
     * 添加方式：1-直接添加（管理员/群主），2-邀请加入
     */
    private Integer addType;
}
