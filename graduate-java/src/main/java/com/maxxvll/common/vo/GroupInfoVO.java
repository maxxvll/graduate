package com.maxxvll.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 群聊信息VO
 */
@Data
public class GroupInfoVO {
    
    /**
     * 群ID
     */
    private String id;
    
    /**
     * 群名称
     */
    private String groupName;
    
    /**
     * 群头像URL
     */
    private String groupAvatar;
    
    /**
     * 创建人ID
     */
    private String creatorId;
    
    /**
     * 创建人昵称
     */
    private String creatorNickname;
    
    /**
     * 群最大成员数
     */
    private Integer maxMember;
    
    /**
     * 当前成员数
     */
    private Integer currentMemberCount;
    
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
    
    /**
     * 群状态：1-正常，2-解散，3-封禁
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
    
    /**
     * 当前用户在群中的角色：1-群主，2-管理员，3-普通成员，0-非成员
     */
    private Integer myRole;

    /**
     * 当前用户申请状态：null-未申请，member-已是成员，pending-申请待审核
     */
    private String applyStatus;
}
