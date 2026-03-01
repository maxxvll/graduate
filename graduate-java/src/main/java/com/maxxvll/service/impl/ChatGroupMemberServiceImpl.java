package com.maxxvll.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.common.dto.GroupMemberAddDTO;
import com.maxxvll.common.dto.GroupMemberRemoveDTO;
import com.maxxvll.common.dto.GroupMemberUpdateDTO;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.utils.BeanConvertUtil;
import com.maxxvll.common.vo.GroupMemberVO;
import com.maxxvll.domain.ChatGroup;
import com.maxxvll.domain.ChatGroupMember;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.domain.GroupApplication;
import com.maxxvll.mapper.ChatGroupMapper;
import com.maxxvll.mapper.ChatGroupMemberMapper;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.mapper.GroupApplicationMapper;
import com.maxxvll.service.ChatGroupMemberService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 20570
* @description 针对表【chat_group_member(群成员关联表（核心多对多）)】的数据库操作Service实现
* @createDate 2026-02-19 12:02:21
*/
@Service
@Slf4j
public class ChatGroupMemberServiceImpl extends ServiceImpl<ChatGroupMemberMapper, ChatGroupMember>
    implements ChatGroupMemberService{

    @Resource
    private ChatGroupMapper chatGroupMapper;

    @Resource
    private ChatUserMapper chatUserMapper;

    @Resource
    private GroupApplicationMapper groupApplicationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMembers(GroupMemberAddDTO addDTO, String operatorId) {
        // 1. 检查群是否存在
        ChatGroup group = chatGroupMapper.selectById(addDTO.getGroupId());
        if (group == null || group.getStatus() == 2) {
            throw new BusinessException("群聊不存在或已解散");
        }

        // 2. 检查操作人是否在群中
        Integer operatorRole = getUserRole(addDTO.getGroupId(), operatorId);
        if (operatorRole == null) {
            throw new BusinessException("您不在该群聊中");
        }

        // 3. 普通成员邀请：按群的加群方式来处理
        if (operatorRole == 3) {
            if (group.getJoinType() == 3) {
                throw new BusinessException("该群聊仅支持邀请加入，您无权邀请他人");
            }
            if (group.getJoinType() == 1) {
                // 需审核：创建待审核申请记录
                for (String userId : addDTO.getUserIds()) {
                    if (isGroupMember(addDTO.getGroupId(), userId)) {
                        continue;
                    }
                    long pendingCount = groupApplicationMapper.selectCount(
                            new LambdaQueryWrapper<GroupApplication>()
                                    .eq(GroupApplication::getGroupId, Long.valueOf(addDTO.getGroupId()))
                                    .eq(GroupApplication::getApplicantId, Long.valueOf(userId))
                                    .eq(GroupApplication::getStatus, 0)
                    );
                    if (pendingCount > 0) continue;

                    GroupApplication application = new GroupApplication();
                    application.setApplicantId(Long.valueOf(userId));
                    application.setGroupId(Long.valueOf(addDTO.getGroupId()));
                    application.setStatus(0);
                    application.setCreateTime(new Date());
                    application.setUpdateTime(new Date());
                    groupApplicationMapper.insert(application);
                    log.info("用户[{}]被[{}]邀请加入群聊[{}]（待审核）", userId, operatorId, addDTO.getGroupId());
                }
                return;
            }
            // joinType == 2 (免审核)：普通成员也可直接邀请
        }

        // 4. 管理员/群主直接添加，或免审核群普通成员邀请直接入群（userIds 去重，避免 uk_group_user 唯一约束冲突）
        List<String> userIdsDistinct = addDTO.getUserIds().stream()
                .filter(id -> id != null && !id.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
        long currentCount = getMemberCount(addDTO.getGroupId());
        if (currentCount + userIdsDistinct.size() > group.getMaxMember()) {
            throw new BusinessException("群成员数量将超过上限");
        }

        for (String userId : userIdsDistinct) {
            if (isGroupMember(addDTO.getGroupId(), userId)) {
                continue;
            }
            ChatUser user = chatUserMapper.selectById(userId);
            if (user == null) {
                continue;
            }
            ChatGroupMember member = new ChatGroupMember();
            member.setGroupId(addDTO.getGroupId());
            member.setUserId(userId);
            member.setRole(3);
            member.setJoinTime(new Date());
            member.setInviterId(operatorId);
            member.setIsMute(0);
            member.setIsQuit(0);
            member.setCreatedAt(new Date());
            member.setUpdatedAt(new Date());
            this.save(member);
            log.info("用户[{}]被添加到群聊[{}]", userId, addDTO.getGroupId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(GroupMemberRemoveDTO removeDTO, String operatorId) {
        // 1. 检查群是否存在
        ChatGroup group = chatGroupMapper.selectById(removeDTO.getGroupId());
        if (group == null || group.getStatus() == 2) {
            throw new BusinessException("群聊不存在或已解散");
        }

        // 2. 检查操作人权限
        Integer operatorRole = getUserRole(removeDTO.getGroupId(), operatorId);
        Integer targetRole = getUserRole(removeDTO.getGroupId(), removeDTO.getUserId());

        if (operatorRole == null) {
            throw new BusinessException("您不在该群聊中");
        }

        // 权限检查：
        // - 群主(1)：可以移除任何人（包括管理员和其他群主）
        // - 管理员(2)：只能移除普通成员(3)，不能移除群主和其他管理员
        // - 普通成员(3)：无权移除他人
        if (operatorRole == 2) {
            // 管理员权限检查
            if (targetRole == null) {
                throw new BusinessException("目标用户不在该群聊中");
            }
            if (targetRole <= 2) {  // 不能移除群主(1)或管理员(2)
                throw new BusinessException("管理员无法移除群主或其他管理员");
            }
        } else if (operatorRole == 3) {
            // 普通成员无权限
            throw new BusinessException("普通成员无权限移除他人");
        }

        // 不能移除自己（退出群聊用另一个接口）
        if (removeDTO.getUserId().equals(operatorId)) {
            throw new BusinessException("不能移除自己，请使用退出群聊功能");
        }

        // 3. 标记成员为退出
        ChatGroupMember member = this.getOne(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, removeDTO.getGroupId())
                        .eq(ChatGroupMember::getUserId, removeDTO.getUserId())
                        .eq(ChatGroupMember::getIsQuit, 0)
        );

        if (member != null) {
            member.setIsQuit(1);
            member.setQuitTime(new Date());
            member.setQuitReason(removeDTO.getReason());
            member.setUpdatedAt(new Date());
            this.updateById(member);
            log.info("用户[{}]被从群聊[{}]移除，操作人：[{}]，原因：{}", 
                    removeDTO.getUserId(), removeDTO.getGroupId(), operatorId, 
                    removeDTO.getReason() != null ? removeDTO.getReason() : "无");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMember(GroupMemberUpdateDTO updateDTO, String operatorId) {
        // 1. 检查群是否存在
        ChatGroup group = chatGroupMapper.selectById(updateDTO.getGroupId());
        if (group == null || group.getStatus() == 2) {
            throw new BusinessException("群聊不存在或已解散");
        }

        // 2. 检查操作人权限
        Integer operatorRole = getUserRole(updateDTO.getGroupId(), operatorId);
        Integer targetRole = getUserRole(updateDTO.getGroupId(), updateDTO.getUserId());

        if (operatorRole == null) {
            throw new BusinessException("您不在该群聊中");
        }

        // 3. 检查目标成员是否存在
        ChatGroupMember member = this.getOne(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, updateDTO.getGroupId())
                        .eq(ChatGroupMember::getUserId, updateDTO.getUserId())
                        .eq(ChatGroupMember::getIsQuit, 0)
        );

        if (member == null) {
            throw new BusinessException("成员不存在");
        }

        // 4. 更新角色权限控制
        if (updateDTO.getRole() != null) {
            // 权限检查：
            // - 只有群主可以设置管理员
            // - 不能修改群主的角色
            // - 管理员之间平等，不能相互操作角色
            if (updateDTO.getRole() == 2 && operatorRole != 1) {
                throw new BusinessException("只有群主可以设置管理员");
            }
            if (targetRole != null && targetRole == 1) {
                throw new BusinessException("不能修改群主的角色");
            }
            if (operatorRole == 2 && targetRole != null && targetRole == 2) {
                throw new BusinessException("管理员不能修改其他管理员的角色");
            }
            member.setRole(updateDTO.getRole());
            log.info("用户[{}]在群聊[{}]的角色被更新为[{}]，操作人：[{}]", 
                    updateDTO.getUserId(), updateDTO.getGroupId(), 
                    getRoleName(updateDTO.getRole()), operatorId);
        }

        // 5. 更新禁言状态权限控制
        if (updateDTO.getIsMute() != null) {
            // 权限检查：
            // - 群主可以禁言任何人（包括管理员）
            // - 管理员只能禁言普通成员
            if (operatorRole == 2 && targetRole != null && targetRole <= 2) {
                throw new BusinessException("管理员无法禁言群主或其他管理员");
            }
            member.setIsMute(updateDTO.getIsMute());
            log.info("用户[{}]在群聊[{}]的禁言状态被更新为[{}]，操作人：[{}]", 
                    updateDTO.getUserId(), updateDTO.getGroupId(), 
                    updateDTO.getIsMute() == 1 ? "禁言" : "取消禁言", operatorId);
        }

        member.setUpdatedAt(new Date());
        this.updateById(member);
    }

    @Override
    public List<GroupMemberVO> getGroupMembers(String groupId) {
        // 检查群是否存在
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null || group.getStatus() == 2) {
            throw new BusinessException("群聊不存在或已解散");
        }

        List<ChatGroupMember> members = this.list(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getIsQuit, 0)
                        .orderByAsc(ChatGroupMember::getRole)
                        .orderByDesc(ChatGroupMember::getJoinTime)
        );

        return members.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public GroupMemberVO getMemberInfo(String groupId, String userId) {
        ChatGroupMember member = this.getOne(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, userId)
                        .eq(ChatGroupMember::getIsQuit, 0)
        );

        if (member == null) {
            return null;
        }

        return convertToVO(member);
    }

    @Override
    public boolean isGroupMember(String groupId, String userId) {
        Long count = this.count(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, userId)
                        .eq(ChatGroupMember::getIsQuit, 0)
        );
        return count > 0;
    }

    @Override
    public Integer getUserRole(String groupId, String userId) {
        ChatGroupMember member = this.getOne(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, userId)
                        .eq(ChatGroupMember::getIsQuit, 0)
        );
        return member != null ? member.getRole() : null;
    }

    @Override
    public long getMemberCount(String groupId) {
        return this.count(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getIsQuit, 0)
        );
    }

    /**
     * 转换为VO
     */
    private GroupMemberVO convertToVO(ChatGroupMember member) {
        // 使用BeanConvertUtil转换基础字段
        GroupMemberVO vo = BeanConvertUtil.convert(member, GroupMemberVO.class);

        // 设置扩展字段（关联查询和计算字段）
        vo.setRoleName(getRoleName(member.getRole()));

        // 获取用户信息
        ChatUser user = chatUserMapper.selectById(member.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }

        // 获取邀请人信息
        if (member.getInviterId() != null) {
            ChatUser inviter = chatUserMapper.selectById(member.getInviterId());
            if (inviter != null) {
                vo.setInviterNickname(inviter.getNickname());
            }
        }

        return vo;
    }

    /**
     * 获取角色名称
     */
    private String getRoleName(Integer role) {
        if (role == null) {
            return "未知";
        }
        return switch (role) {
            case 1 -> "群主";
            case 2 -> "管理员";
            case 3 -> "成员";
            default -> "未知";
        };
    }
}




