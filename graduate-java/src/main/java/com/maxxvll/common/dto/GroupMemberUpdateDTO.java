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
    private Long groupId;
    
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
    
    // 手动添加 getter/setter 方法以解决 Lombok 编译问题
    public Long getGroupId() {
        return groupId;
    }
    
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Integer getRole() {
        return role;
    }
    
    public void setRole(Integer role) {
        this.role = role;
    }
    
    public Integer getIsMute() {
        return isMute;
    }
    
    public void setIsMute(Integer isMute) {
        this.isMute = isMute;
    }
}