package com.maxxvll.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.common.constants.ApplicationStatusConstants;
import com.maxxvll.common.dto.GroupCreateDTO;
import com.maxxvll.common.dto.GroupTransferDTO;
import com.maxxvll.common.dto.GroupUpdateDTO;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.utils.BeanConvertUtil;
import com.maxxvll.utils.MinioUtil;
import com.maxxvll.common.vo.GroupInfoVO;
import com.maxxvll.domain.ChatGroup;
import com.maxxvll.domain.ChatGroupMember;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.mapper.ChatGroupMapper;
import com.maxxvll.mapper.ChatGroupMemberMapper;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.mapper.GroupApplicationMapper;
import com.maxxvll.service.ChatGroupMemberService;
import com.maxxvll.service.ChatGroupService;
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

/**
* @author 20570
* @description 针对表【chat_group(群聊基础信息表)】的数据库操作Service实现
* @createDate 2026-02-19 12:02:21
*/
@Service
@Slf4j
public class ChatGroupServiceImpl extends ServiceImpl<ChatGroupMapper, ChatGroup>
    implements ChatGroupService{

    private static final int DEFAULT_GROUP_SEARCH_SIZE = 20;
    private static final int MAX_GROUP_SEARCH_SIZE = 50;

    @Resource
    private ChatGroupMemberMapper chatGroupMemberMapper;

    @Resource
    private ChatGroupMemberService chatGroupMemberService;

    @Resource
    private ChatUserMapper chatUserMapper;

    @Resource
    private GroupApplicationMapper groupApplicationMapper;

    @Resource
    private MinioUtil minioUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupInfoVO createGroup(GroupCreateDTO createDTO, String creatorId) {
        ChatGroup group = new ChatGroup();
        group.setGroupName(createDTO.getGroupName());
        String avatar = createDTO.getGroupAvatar();
        group.setGroupAvatar(avatar != null && !avatar.trim().isEmpty() ? avatar.trim() : "");
        group.setCreatorId(Long.valueOf(creatorId));
        group.setMaxMember(createDTO.getMaxMember() != null ? createDTO.getMaxMember() : 200);
        group.setJoinType(createDTO.getJoinType() != null ? createDTO.getJoinType() : 1);
        group.setIsMuteAll(0);
        group.setStatus(1);
        group.setCreateTime(new Date());
        group.setUpdateTime(new Date());

        this.save(group);

        LinkedHashSet<String> requestedMemberIds = createDTO.getMemberIds() == null
                ? new LinkedHashSet<>()
                : createDTO.getMemberIds().stream()
                        .filter(id -> id != null && !id.trim().isEmpty())
                        .filter(id -> !id.equals(creatorId))
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> validMemberIds = requestedMemberIds.isEmpty()
                ? List.of()
                : chatUserMapper.selectList(
                                new LambdaQueryWrapper<ChatUser>()
                                        .select(ChatUser::getId)
                                        .in(ChatUser::getId, requestedMemberIds)
                        ).stream()
                        .map(ChatUser::getId)
                        .collect(Collectors.toList());

        if (validMemberIds.size() + 1 > group.getMaxMember()) {
            throw new BusinessException("Member count would exceed the group limit");
        }

        Date now = new Date();
        List<ChatGroupMember> membersToSave = new java.util.ArrayList<>(validMemberIds.size() + 1);

        ChatGroupMember owner = new ChatGroupMember();
        owner.setGroupId(group.getId());
        owner.setUserId(creatorId);
        owner.setRole(1);
        owner.setJoinTime(now);
        owner.setInviterId(null);
        owner.setIsMute(0);
        owner.setIsQuit(0);
        owner.setQuitTime(null);
        owner.setQuitReason(null);
        owner.setCreateTime(now);
        owner.setUpdateTime(now);
        membersToSave.add(owner);

        for (String memberId : validMemberIds) {
            ChatGroupMember member = new ChatGroupMember();
            member.setGroupId(group.getId());
            member.setUserId(memberId);
            member.setRole(3);
            member.setJoinTime(now);
            member.setInviterId(creatorId);
            member.setIsMute(0);
            member.setIsQuit(0);
            member.setQuitTime(null);
            member.setQuitReason(null);
            member.setCreateTime(now);
            member.setUpdateTime(now);
            membersToSave.add(member);
        }

        chatGroupMemberMapper.batchUpsertMembers(membersToSave);
        log.info(String.format("Group created, creatorId=%s, groupId=%s, initialMemberCount=%d",
                creatorId, group.getId(), membersToSave.size()));
        return getGroupInfo(group.getId(), creatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupInfoVO updateGroup(GroupUpdateDTO updateDTO, String operatorId) {
        // 1. 检查群是否存在
        ChatGroup group = this.getById(updateDTO.getGroupId());
        if (group == null || group.getStatus() == 2) {
            throw new BusinessException("群聊不存在或已解散");
        }

        // 2. 检查权限（只有群主和管理员可以修改）
        Integer role = chatGroupMemberService.getUserRole(updateDTO.getGroupId(), operatorId);
        if (role == null || role > 2) {
            throw new BusinessException("无权限修改群信息");
        }

        // 3. 更新群信息
        if (updateDTO.getGroupName() != null) {
            group.setGroupName(updateDTO.getGroupName());
        }
        if (updateDTO.getGroupAvatar() != null) {
            group.setGroupAvatar(updateDTO.getGroupAvatar());
        }
        if (updateDTO.getMaxMember() != null) {
            // 检查新上限是否小于当前成员数
            long currentCount = chatGroupMemberService.getMemberCount(updateDTO.getGroupId());
            if (updateDTO.getMaxMember() < currentCount) {
                throw new BusinessException("群最大成员数不能小于当前成员数");
            }
            group.setMaxMember(updateDTO.getMaxMember());
        }
        if (updateDTO.getJoinType() != null) {
            group.setJoinType(updateDTO.getJoinType());
        }
        if (updateDTO.getNotice() != null) {
            group.setNotice(updateDTO.getNotice());
        }
        if (updateDTO.getIsMuteAll() != null) {
            group.setIsMuteAll(updateDTO.getIsMuteAll());
        }
        group.setUpdateTime(new Date());

        this.updateById(group);
        log.info("用户[{}]更新了群聊[{}]信息", operatorId, updateDTO.getGroupId());

        return getGroupInfo(updateDTO.getGroupId(), operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolveGroup(Long groupId, String operatorId) {
        // 1. 检查群是否存在
        ChatGroup group = this.getById(groupId);
        if (group == null || group.getStatus() == 2) {
            throw new BusinessException("群聊不存在或已解散");
        }

        // 2. 检查权限（只有群主可以解散）
        Integer role = chatGroupMemberService.getUserRole(groupId, operatorId);
        if (role == null || role != 1) {
            throw new BusinessException("只有群主可以解散群聊");
        }

        // 3. 解散群聊（软删除）
        group.setStatus(2);
        group.setUpdateTime(new Date());
        this.updateById(group);

        // 4. 将所有成员标记为退出
        ChatGroupMember updateMember = new ChatGroupMember();
        updateMember.setIsQuit(1);
        updateMember.setQuitTime(new Date());
        updateMember.setUpdateTime(new Date());

        chatGroupMemberMapper.update(updateMember, new LambdaQueryWrapper<ChatGroupMember>()
                .eq(ChatGroupMember::getGroupId, groupId)
                .eq(ChatGroupMember::getIsQuit, 0));

        log.info("群主[{}]解散了群聊[{}]", operatorId, groupId);
    }

    @Override
    public GroupInfoVO getGroupInfo(Long groupId, String userId) {
        ChatGroup group = this.getById(groupId);
        if (group == null || group.getStatus() == 2) {
            throw new BusinessException("群聊不存在或已解散");
        }

        GroupInfoVO vo = BeanConvertUtil.convert(group, GroupInfoVO.class);
        // 转换群头像为公共桶永久直链
        vo.setGroupAvatar(minioUtil.getAvatarUrl(group.getGroupAvatar()));

        // 获取创建人昵称
        if (group.getCreatorId() != null) {
            com.maxxvll.domain.ChatUser creator = chatUserMapper.selectById(group.getCreatorId());
            if (creator != null) {
                vo.setCreatorNickname(creator.getNickname());
            }
        }

        // 获取当前成员数
        vo.setCurrentMemberCount((int) chatGroupMemberService.getMemberCount(groupId));

        // 获取当前用户角色
        Integer myRole = chatGroupMemberService.getUserRole(groupId, userId);
        vo.setMyRole(myRole != null ? myRole : 0);

        return vo;
    }

    @Override
    public List<GroupInfoVO> getUserGroups(String userId) {
        // 查询用户加入的所有群成员记录
        List<ChatGroupMember> members = chatGroupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getUserId, userId)
                        .eq(ChatGroupMember::getIsQuit, 0)
        );

        if (members.isEmpty()) {
            return List.of();
        }

        // 获取群ID列表
        List<Long> groupIds = members.stream()
                .map(ChatGroupMember::getGroupId)
                .collect(Collectors.toList());

        // 查询群信息
        List<ChatGroup> groups = this.list(
                new LambdaQueryWrapper<ChatGroup>()
                        .in(ChatGroup::getId, groupIds)
                        .eq(ChatGroup::getStatus, 1)
        );

        // ===== N+1查询优化：批量查询替代循环查询 =====

        // 1. 批量获取所有创建人信息
        List<String> creatorIds = groups.stream()
                .map(group -> String.valueOf(group.getCreatorId()))
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询创建人信息
        List<com.maxxvll.domain.ChatUser> creators = creatorIds.isEmpty() ?
                List.of() :
                chatUserMapper.selectList(
                        new LambdaQueryWrapper<com.maxxvll.domain.ChatUser>()
                                .in(com.maxxvll.domain.ChatUser::getId, creatorIds)
                );

        // 构建创建人ID -> 创建人信息的映射
        var creatorMap = creators.stream()
                .collect(Collectors.toMap(com.maxxvll.domain.ChatUser::getId, c -> c));

        // 2. 获取所有群的成员数（使用直接查询，避免 MyBatis foreach 批量查询的问题）
        List<Long> groupIdsList = groups.stream()
                .map(ChatGroup::getId)
                .collect(Collectors.toList());

        // 使用直接查询获取每个群的成员数（更可靠）
        Map<Long, Long> memberCountMap = new java.util.HashMap<>();
        for (Long groupId : groupIdsList) {
            memberCountMap.put(groupId, chatGroupMemberService.getMemberCount(groupId));
        }

        var userRoleList = chatGroupMemberMapper.getUserRolesInGroups(userId, groupIdsList);
        Map<Long, Integer> userRoleMap = userRoleList.stream()
                .collect(Collectors.toMap(ChatGroupMemberMapper.UserGroupRole::getGroupId,
                        ChatGroupMemberMapper.UserGroupRole::getRole));

        // 4. 在内存中组装数据
        return groups.stream().map(group -> {
            GroupInfoVO vo = BeanConvertUtil.convert(group, GroupInfoVO.class);
            // 转换群头像为公共桶永久直链
            vo.setGroupAvatar(minioUtil.getAvatarUrl(group.getGroupAvatar()));

            // 从批量查询的Map中获取创建人昵称
            if (group.getCreatorId() != null) {
                com.maxxvll.domain.ChatUser creator = creatorMap.get(group.getCreatorId());
                if (creator != null) {
                    vo.setCreatorNickname(creator.getNickname());
                }
            }

            // 从Map中获取成员数
            vo.setCurrentMemberCount(memberCountMap.getOrDefault(group.getId(), 0L).intValue());

            // 从Map中获取用户角色
            vo.setMyRole(userRoleMap.getOrDefault(group.getId(), 0));

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferGroupOwner(GroupTransferDTO transferDTO, String currentOwnerId) {
        // 1. 检查群是否存在
        ChatGroup group = this.getById(transferDTO.getGroupId());
        if (group == null || group.getStatus() == 2) {
            throw new BusinessException("群聊不存在或已解散");
        }

        // 2. 检查当前用户是否是群主
        Integer role = chatGroupMemberService.getUserRole(transferDTO.getGroupId(), currentOwnerId);
        if (role == null || role != 1) {
            throw new BusinessException("只有群主可以转让群聊");
        }

        // 3. 检查新群主是否在群中
        if (!chatGroupMemberService.isGroupMember(transferDTO.getGroupId(), transferDTO.getNewOwnerId())) {
            throw new BusinessException("新群主不在该群聊中");
        }

        // 4. 更新群主
        group.setCreatorId(Long.valueOf(transferDTO.getNewOwnerId()));
        group.setUpdateTime(new Date());
        this.updateById(group);

        // 5. 更新成员角色
        // 原群主变为普通成员
        ChatGroupMember oldOwner = chatGroupMemberMapper.selectOne(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, transferDTO.getGroupId())
                        .eq(ChatGroupMember::getUserId, currentOwnerId)
        );
        if (oldOwner != null) {
            oldOwner.setRole(3);
            oldOwner.setUpdateTime(new Date());
            chatGroupMemberMapper.updateById(oldOwner);
        }

        // 新群主变为群主
        ChatGroupMember newOwner = chatGroupMemberMapper.selectOne(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, transferDTO.getGroupId())
                        .eq(ChatGroupMember::getUserId, transferDTO.getNewOwnerId())
        );
        if (newOwner != null) {
            newOwner.setRole(1);
            newOwner.setUpdateTime(new Date());
            chatGroupMemberMapper.updateById(newOwner);
        }

        log.info("群主[{}]将群聊[{}]转让给[{}]", currentOwnerId, transferDTO.getGroupId(), transferDTO.getNewOwnerId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void quitGroup(Long groupId, String userId) {
        // 1. 检查群是否存在
        ChatGroup group = this.getById(groupId);
        if (group == null || group.getStatus() == 2) {
            throw new BusinessException("群聊不存在或已解散");
        }

        // 2. 检查用户是否在群中
        if (!chatGroupMemberService.isGroupMember(groupId, userId)) {
            throw new BusinessException("您不在该群聊中");
        }

        // 3. 检查是否是群主（群主不能直接退出，需要先转让）
        Integer role = chatGroupMemberService.getUserRole(groupId, userId);
        if (role != null && role == 1) {
            throw new BusinessException("群主无法直接退出群聊，请先转让群主身份");
        }

        // 4. 标记为退出
        ChatGroupMember member = chatGroupMemberMapper.selectOne(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, userId)
        );
        if (member != null) {
            member.setIsQuit(1);
            member.setQuitTime(new Date());
            member.setUpdateTime(new Date());
            chatGroupMemberMapper.updateById(member);
        }

        log.info("用户[{}]退出了群聊[{}]", userId, groupId);
    }

    @Override
    public List<GroupInfoVO> searchGroup(String keyword, String currentUserId) {
        return searchGroupPage(keyword, currentUserId, 1, DEFAULT_GROUP_SEARCH_SIZE).getRecords();
    }

    @Override
    public Page<GroupInfoVO> searchGroupPage(String keyword, String currentUserId, int current, int size) {
        int normalizedCurrent = Math.max(1, current);
        int normalizedSize = normalizeGroupSearchSize(size);
        if (keyword == null || keyword.trim().isEmpty()) {
            Page<GroupInfoVO> emptyPage = new Page<>(normalizedCurrent, normalizedSize, 0);
            emptyPage.setRecords(List.of());
            return emptyPage;
        }

        Page<ChatGroup> groupPage = this.page(
                new Page<>(normalizedCurrent, normalizedSize),
                new LambdaQueryWrapper<ChatGroup>()
                        .like(ChatGroup::getGroupName, keyword)
                        .eq(ChatGroup::getStatus, 1)
                        .orderByDesc(ChatGroup::getUpdateTime)
                        .orderByDesc(ChatGroup::getId)
        );

        Page<GroupInfoVO> result = new Page<>(groupPage.getCurrent(), groupPage.getSize(), groupPage.getTotal());
        result.setRecords(buildSearchGroupVOs(groupPage.getRecords(), currentUserId));
        return result;
    }

    private List<GroupInfoVO> buildSearchGroupVOs(List<ChatGroup> groups, String currentUserId) {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }

        List<Long> groupIds = groups.stream()
                .map(ChatGroup::getId)
                .toList();

        // 使用直接查询获取每个群的成员数（避免 MyBatis foreach 批量查询的问题）
        Map<Long, Long> memberCountMap = new java.util.HashMap<>();
        for (Long groupId : groupIds) {
            memberCountMap.put(groupId, chatGroupMemberService.getMemberCount(groupId));
        }

        var userRoleList = chatGroupMemberMapper.getUserRolesInGroups(currentUserId, groupIds);
        Map<Long, Integer> roleMap = userRoleList.stream()
                .collect(Collectors.toMap(ChatGroupMemberMapper.UserGroupRole::getGroupId,
                        ChatGroupMemberMapper.UserGroupRole::getRole));
        Set<Long> pendingGroupIds = groupApplicationMapper
                .selectGroupIdsByApplicantAndStatusAndGroupIds(
                        Long.valueOf(currentUserId),
                        ApplicationStatusConstants.STATUS_PENDING,
                        groupIds
                ).stream()
                .collect(Collectors.toSet());
        Map<String, ChatUser> creatorMap = loadUsers(groups.stream()
                .map(ChatGroup::getCreatorId)
                .filter(java.util.Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        return groups.stream()
                .map(group -> {
                    GroupInfoVO vo = BeanConvertUtil.convert(group, GroupInfoVO.class);
                    vo.setGroupAvatar(minioUtil.getAvatarUrl(group.getGroupAvatar()));
                    vo.setCurrentMemberCount(memberCountMap.getOrDefault(group.getId(), 0L).intValue());

                    ChatUser creator = creatorMap.get(String.valueOf(group.getCreatorId()));
                    if (creator != null) {
                        vo.setCreatorNickname(creator.getNickname());
                    }

                    Integer role = roleMap.get(group.getId());
                    vo.setMyRole(role != null ? role : 0);
                    if (role != null) {
                        vo.setApplyStatus("member");
                    } else if (pendingGroupIds.contains(group.getId())) {
                        vo.setApplyStatus("pending");
                    } else {
                        vo.setApplyStatus(null);
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private Map<String, ChatUser> loadUsers(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        return chatUserMapper.selectList(
                        new LambdaQueryWrapper<ChatUser>()
                                .select(ChatUser::getId, ChatUser::getNickname)
                                .in(ChatUser::getId, userIds)
                ).stream()
                .collect(Collectors.toMap(ChatUser::getId, user -> user, (left, right) -> left));
    }

    private int normalizeGroupSearchSize(int size) {
        if (size <= 0) {
            return DEFAULT_GROUP_SEARCH_SIZE;
        }
        return Math.min(size, MAX_GROUP_SEARCH_SIZE);
    }

    // ==================== 群公告功能实现 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishNotice(Long groupId, String notice, String operatorId) {
        // 1. 检查群是否存在
        ChatGroup group = this.getById(groupId);
        if (group == null || group.getStatus() == 2) {
            throw new BusinessException("群聊不存在或已解散");
        }

        // 2. 检查权限（只有群主和管理员可以发布公告）
        Integer role = chatGroupMemberService.getUserRole(groupId, operatorId);
        if (role == null || role > 2) {
            throw new BusinessException("无权限发布群公告");
        }

        // 3. 更新公告
        group.setNotice(notice);
        group.setUpdateTime(new Date());
        this.updateById(group);

        log.info("用户[{}]发布了群聊[{}]公告，长度: {}", operatorId, groupId, notice != null ? notice.length() : 0);
    }

    @Override
    public String getNotice(Long groupId) {
        ChatGroup group = this.getById(groupId);
        if (group == null || group.getStatus() == 2) {
            throw new BusinessException("群聊不存在或已解散");
        }
        return group.getNotice();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(Long groupId, String operatorId) {
        // 1. 检查群是否存在
        ChatGroup group = this.getById(groupId);
        if (group == null || group.getStatus() == 2) {
            throw new BusinessException("群聊不存在或已解散");
        }

        // 2. 检查权限（只有群主和管理员可以删除公告）
        Integer role = chatGroupMemberService.getUserRole(groupId, operatorId);
        if (role == null || role > 2) {
            throw new BusinessException("无权限删除群公告");
        }

        // 3. 清空公告
        group.setNotice(null);
        group.setUpdateTime(new Date());
        this.updateById(group);

        log.info("用户[{}]删除了群聊[{}]公告", operatorId, groupId);
    }
}




