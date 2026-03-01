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
    private Long groupId;
    
    /**
     * 要移除的成员ID
     */
    private String userId;
    
    /**
     * 移除原因
     */
    private String reason;
    
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
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}