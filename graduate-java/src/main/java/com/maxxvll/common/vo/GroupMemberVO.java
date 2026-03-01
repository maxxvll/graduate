package com.maxxvll.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 群成员信息VO
 */
@Data
public class GroupMemberVO {
    
    /**
     * 记录ID
     */
    private Long id;
    
    /**
     * 群ID
     */
    private Long groupId;
    
    /**
     * 成员ID
     */
    private String userId;
    
    /**
     * 成员昵称
     */
    private String nickname;
    
    /**
     * 成员头像
     */
    private String avatar;
    
    /**
     * 成员角色：1-群主，2-管理员，3-普通成员
     */
    private Integer role;
    
    /**
     * 角色名称
     */
    private String roleName;
    
    /**
     * 加入时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date joinTime;
    
    /**
     * 邀请人ID
     */
    private String inviterId;
    
    /**
     * 邀请人昵称
     */
    private String inviterNickname;
    
    /**
     * 是否被禁言：0-否，1-是
     */
    private Integer isMute;
}
