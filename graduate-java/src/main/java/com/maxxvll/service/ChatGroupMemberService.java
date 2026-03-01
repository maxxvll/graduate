package com.maxxvll.service;

import com.maxxvll.common.dto.GroupMemberAddDTO;
import com.maxxvll.common.dto.GroupMemberRemoveDTO;
import com.maxxvll.common.dto.GroupMemberUpdateDTO;
import com.maxxvll.common.vo.GroupMemberVO;
import com.maxxvll.domain.ChatGroupMember;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 20570
* @description 针对表【chat_group_member(群成员关联表（核心多对多）)】的数据库操作Service
* @createDate 2026-02-19 12:02:21
*/
public interface ChatGroupMemberService extends IService<ChatGroupMember> {

    /**
     * 添加群成员
     * @param addDTO 添加信息
     * @param operatorId 操作人ID
     */
    void addMembers(GroupMemberAddDTO addDTO, String operatorId);

    /**
     * 移除群成员
     * @param removeDTO 移除信息
     * @param operatorId 操作人ID
     */
    void removeMember(GroupMemberRemoveDTO removeDTO, String operatorId);

    /**
     * 更新群成员信息（角色、禁言状态）
     * @param updateDTO 更新信息
     * @param operatorId 操作人ID
     */
    void updateMember(GroupMemberUpdateDTO updateDTO, String operatorId);

    /**
     * 获取群成员列表
     * @param groupId 群ID
     * @return 成员列表
     */
    List<GroupMemberVO> getGroupMembers(Long groupId);

    /**
     * 获取群成员详情
     * @param groupId 群ID
     * @param userId 用户ID
     * @return 成员信息
     */
    GroupMemberVO getMemberInfo(Long groupId, String userId);

    /**
     * 检查用户是否在群中
     * @param groupId 群ID
     * @param userId 用户ID
     * @return 是否在群中
     */
    boolean isGroupMember(Long groupId, String userId);

    /**
     * 获取用户在群中的角色
     * @param groupId 群ID
     * @param userId 用户ID
     * @return 角色：1-群主，2-管理员，3-普通成员，null-不在群中
     */
    Integer getUserRole(Long groupId, String userId);

    /**
     * 获取群成员数量
     * @param groupId 群ID
     * @return 成员数量
     */
    long getMemberCount(Long groupId);
}
