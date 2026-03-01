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
    private Long groupId;
    
    /**
     * 要添加的成员ID列表
     */
    private List<String> userIds;
    
    /**
     * 添加方式：1-直接添加（管理员/群主），2-邀请加入
     */
    private Integer addType;
    
    // 手动添加 getter/setter 方法以解决 Lombok 编译问题
    public Long getGroupId() {
        return groupId;
    }
    
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
    
    public List<String> getUserIds() {
        return userIds;
    }
    
    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }
    
    public Integer getAddType() {
        return addType;
    }
    
    public void setAddType(Integer addType) {
        this.addType = addType;
    }
}