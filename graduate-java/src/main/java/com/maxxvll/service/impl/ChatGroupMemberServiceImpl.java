package com.maxxvll.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.common.constants.ApplicationStatusConstants;
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
import com.maxxvll.utils.MinioUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Resource
    private MinioUtil minioUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMembers(GroupMemberAddDTO addDTO, String operatorId) {
        ChatGroup group = chatGroupMapper.selectById(addDTO.getGroupId());
        if (group == null || group.getStatus() == 2) {
            throw new BusinessException("Group not found or already dissolved");
        }

        Integer operatorRole = getUserRole(addDTO.getGroupId(), operatorId);
        if (operatorRole == null) {
            throw new BusinessException("Operator is not in this group");
        }

        LinkedHashSet<String> requestedUserIds = addDTO.getUserIds() == null
                ? new LinkedHashSet<>()
                : addDTO.getUserIds().stream()
                        .filter(id -> id != null && !id.trim().isEmpty())
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requestedUserIds.isEmpty()) {
            return;
        }

        if (operatorRole == 3) {
            if (group.getJoinType() == 3) {
                throw new BusinessException("This group only supports invite-only joins");
            }
            if (group.getJoinType() == 1) {
                Set<String> activeMemberIds = new java.util.HashSet<>(
                        baseMapper.selectActiveUserIdsByGroupIdAndUserIds(addDTO.getGroupId(), requestedUserIds)
                );
                List<String> validInviteeIds = chatUserMapper.selectList(
                                new LambdaQueryWrapper<ChatUser>()
                                        .select(ChatUser::getId)
                                        .in(ChatUser::getId, requestedUserIds)
                        ).stream()
                        .map(ChatUser::getId)
                        .filter(userId -> !activeMemberIds.contains(userId))
                        .collect(Collectors.toList());

                if (validInviteeIds.isEmpty()) {
                    return;
                }

                List<Long> inviteeIdLongs = validInviteeIds.stream()
                        .map(Long::valueOf)
                        .collect(Collectors.toList());
                Set<Long> pendingApplicantIds = new java.util.HashSet<>(
                        groupApplicationMapper.selectApplicantIdsByGroupIdAndStatusAndApplicantIds(
                                addDTO.getGroupId(),
                                ApplicationStatusConstants.STATUS_PENDING,
                                inviteeIdLongs
                        )
                );

                Date now = new Date();
                List<GroupApplication> applicationsToSave = validInviteeIds.stream()
                        .map(Long::valueOf)
                        .filter(applicantId -> !pendingApplicantIds.contains(applicantId))
                        .map(applicantId -> {
                            GroupApplication application = new GroupApplication();
                            application.setApplicantId(applicantId);
                            application.setGroupId(addDTO.getGroupId());
                            application.setStatus(ApplicationStatusConstants.STATUS_PENDING);
                            application.setRejectReason(null);
                            application.setOperatorId(null);
                            application.setCreateTime(now);
                            application.setUpdateTime(now);
                            return application;
                        })
                        .collect(Collectors.toList());

                if (applicationsToSave.isEmpty()) {
                    return;
                }

                groupApplicationMapper.batchUpsertApplications(applicationsToSave);
                log.info("Batch pending group invitations created, groupId={}, operatorId={}, count={}",
                        addDTO.getGroupId(), operatorId, applicationsToSave.size());
                return;
            }
        }

        Set<String> activeMemberIds = new java.util.HashSet<>(
                baseMapper.selectActiveUserIdsByGroupIdAndUserIds(addDTO.getGroupId(), requestedUserIds)
        );
        List<String> validNewUserIds = chatUserMapper.selectList(
                        new LambdaQueryWrapper<ChatUser>()
                                .select(ChatUser::getId)
                                .in(ChatUser::getId, requestedUserIds)
                ).stream()
                .map(ChatUser::getId)
                .filter(userId -> !activeMemberIds.contains(userId))
                .collect(Collectors.toList());

        if (validNewUserIds.isEmpty()) {
            return;
        }

        long currentCount = getMemberCount(addDTO.getGroupId());
        if (currentCount + validNewUserIds.size() > group.getMaxMember()) {
            throw new BusinessException("Member count would exceed the group limit");
        }

        Date now = new Date();
        List<ChatGroupMember> membersToSave = new java.util.ArrayList<>(validNewUserIds.size());
        for (String userId : validNewUserIds) {
            ChatGroupMember member = new ChatGroupMember();
            member.setGroupId(addDTO.getGroupId());
            member.setUserId(userId);
            member.setRole(3);
            member.setJoinTime(now);
            member.setInviterId(operatorId);
            member.setIsMute(0);
            member.setIsQuit(0);
            member.setQuitTime(null);
            member.setQuitReason(null);
            member.setCreateTime(now);
            member.setUpdateTime(now);
            membersToSave.add(member);
        }

        baseMapper.batchUpsertMembers(membersToSave);
        log.info("Batch group member add completed, groupId={}, operatorId={}, count={}", addDTO.getGroupId(), operatorId, membersToSave.size());
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
            member.setUpdateTime(new Date());
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

        member.setUpdateTime(new Date());
        this.updateById(member);
    }

    @Override
    public List<GroupMemberVO> getGroupMembers(Long groupId) {
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

        // ===== N+1查询优化：批量查询用户信息 =====
        return convertToVOBatch(members);
    }

    @Override
    public List<String> getActiveMemberIds(Long groupId) {
        return baseMapper.selectActiveUserIdsByGroupId(groupId);
    }

    @Override
    public Map<Long, List<String>> getActiveMemberIdsByGroupIds(Collection<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Map.of();
        }

        return baseMapper.selectActiveMembersByGroupIds(groupIds).stream()
                .collect(Collectors.groupingBy(
                        ChatGroupMember::getGroupId,
                        Collectors.mapping(ChatGroupMember::getUserId, Collectors.toList())
                ));
    }

    @Override
    public List<Long> getActiveGroupIdsByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return List.of();
        }
        return baseMapper.selectActiveGroupIdsByUserId(userId);
    }

    /**
     * 批量转换为VO（优化N+1查询）
     * @param members 群成员列表
     * @return VO列表
     */
    private List<GroupMemberVO> convertToVOBatch(List<ChatGroupMember> members) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }

        // 1. 收集所有用户ID和邀请人ID
        List<String> userIds = members.stream()
                .map(ChatGroupMember::getUserId)
                .distinct()
                .collect(Collectors.toList());

        List<String> inviterIds = members.stream()
                .map(ChatGroupMember::getInviterId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        // 合并所有需要查询的用户ID
        List<String> allUserIds = Stream.concat(userIds.stream(), inviterIds.stream())
                .distinct()
                .collect(Collectors.toList());

        // 2. 批量查询所有用户信息
        List<ChatUser> users = allUserIds.isEmpty() ?
                List.of() :
                chatUserMapper.selectList(
                        new LambdaQueryWrapper<ChatUser>()
                                .in(ChatUser::getId, allUserIds)
                );

        // 3. 构建用户ID -> 用户信息的映射
        Map<String, ChatUser> userMap = users.stream()
                .collect(Collectors.toMap(ChatUser::getId, u -> u));

        // 4. 在内存中转换每个成员
        return members.stream().map(member -> {
            GroupMemberVO vo = BeanConvertUtil.convert(member, GroupMemberVO.class);
            vo.setRoleName(getRoleName(member.getRole()));

            // 从Map中获取用户信息
            ChatUser user = userMap.get(member.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(minioUtil.getAvatarUrl(user.getAvatar()));
            }

            // 从Map中获取邀请人信息
            if (member.getInviterId() != null) {
                ChatUser inviter = userMap.get(member.getInviterId());
                if (inviter != null) {
                    vo.setInviterNickname(inviter.getNickname());
                }
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public GroupMemberVO getMemberInfo(Long groupId, String userId) {
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
    public boolean isGroupMember(Long groupId, String userId) {
        Long count = this.count(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, userId)
                        .eq(ChatGroupMember::getIsQuit, 0)
        );
        return count > 0;
    }

    @Override
    public Integer getUserRole(Long groupId, String userId) {
        ChatGroupMember member = this.getOne(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, userId)
                        .eq(ChatGroupMember::getIsQuit, 0)
        );
        return member != null ? member.getRole() : null;
    }

    @Override
    public long getMemberCount(Long groupId) {
        return this.count(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getIsQuit, 0)
        );
    }

    @Override
    public boolean hasPermission(Long groupId, String userId, int requiredRole) {
        Integer userRole = getUserRole(groupId, userId);
        if (userRole == null) {
            return false;
        }
        // 角色值越小权限越高：1(群主) < 2(管理员) < 3(成员)
        return userRole <= requiredRole;
    }

    @Override
    public void checkPermission(Long groupId, String userId, int requiredRole, String errorMessage) {
        if (!hasPermission(groupId, userId, requiredRole)) {
            throw new BusinessException(errorMessage != null ? errorMessage : "权限不足");
        }
    }

    /**
     * 转换为VO
     */
    private GroupMemberVO convertToVO(ChatGroupMember member) {
        List<GroupMemberVO> vos = convertToVOBatch(List.of(member));
        return vos.isEmpty() ? null : vos.get(0);
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




