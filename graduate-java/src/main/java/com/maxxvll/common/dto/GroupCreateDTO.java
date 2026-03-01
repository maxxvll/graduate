package com.maxxvll.common.dto;

import com.maxxvll.common.annotation.NotRequired;
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
     * 群头像URL，未传或为空时使用默认头像（前端展示时兜底）
     */
    @NotRequired
    private String groupAvatar;

    /**
     * 群最大成员数，未传时默认 200
     */
    @NotRequired
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
