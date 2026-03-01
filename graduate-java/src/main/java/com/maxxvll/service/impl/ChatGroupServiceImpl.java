package com.maxxvll.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.common.dto.GroupCreateDTO;
import com.maxxvll.common.dto.GroupTransferDTO;
import com.maxxvll.common.dto.GroupUpdateDTO;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.utils.BeanConvertUtil;
import com.maxxvll.utils.MinioUtil;
import com.maxxvll.common.vo.GroupInfoVO;
import com.maxxvll.domain.ChatGroup;
import com.maxxvll.domain.ChatGroupMember;
import com.maxxvll.domain.GroupApplication;
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

import java.util.Date;
import java.util.List;
import java.util.UUID;
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
        // 1. 创建群聊（使用雪花算法生成ID）
        ChatGroup group = new ChatGroup();
        // MyBatis Plus 的 @TableId(type = IdType.ASSIGN_ID) 会自动使用雪花算法生成ID
        // 这里不需要手动设置ID，save时会自动生成
        group.setGroupName(createDTO.getGroupName());
        // 未选择头像时使用空串，前端展示时用默认群头像兜底
        String avatar = createDTO.getGroupAvatar();
        group.setGroupAvatar(avatar != null && !avatar.trim().isEmpty() ? avatar.trim() : "");
        group.setCreatorId(Long.valueOf(creatorId));
        group.setMaxMember(createDTO.getMaxMember() != null ? createDTO.getMaxMember() : 200);
        group.setJoinType(createDTO.getJoinType() != null ? createDTO.getJoinType() : 1);
        group.setIsMuteAll(0);
        group.setStatus(1);
        group.setCreatedAt(new Date());
        group.setUpdatedAt(new Date());

        this.save(group);

        // 2. 添加群主为成员
        ChatGroupMember owner = new ChatGroupMember();
        owner.setGroupId(group.getId());
        owner.setUserId(creatorId);
        owner.setRole(1); // 群主
        owner.setJoinTime(new Date());
        owner.setIsMute(0);
        owner.setIsQuit(0);
        owner.setCreatedAt(new Date());
        owner.setUpdatedAt(new Date());
        chatGroupMemberMapper.insert(owner);

        // 3. 添加初始成员（去重且排除群主，避免 uk_group_user 唯一约束冲突）
        if (createDTO.getMemberIds() != null && !createDTO.getMemberIds().isEmpty()) {
            List<String> distinctMemberIds = createDTO.getMemberIds().stream()
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .filter(id -> !id.equals(creatorId))
                    .collect(Collectors.toList());
            for (String memberId : distinctMemberIds) {
                ChatGroupMember member = new ChatGroupMember();
                member.setGroupId(group.getId());
                member.setUserId(memberId);
                member.setRole(3); // 普通成员
                member.setJoinTime(new Date());
                member.setInviterId(creatorId);
                member.setIsMute(0);
                member.setIsQuit(0);
                member.setCreatedAt(new Date());
                member.setUpdatedAt(new Date());
                chatGroupMemberMapper.insert(member);
            }
        }

        log.info("用户[{}]创建了群聊[{}]", creatorId, group.getId());
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
        group.setUpdatedAt(new Date());

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
        group.setUpdatedAt(new Date());
        this.updateById(group);

        // 4. 将所有成员标记为退出
        ChatGroupMember updateMember = new ChatGroupMember();
        updateMember.setIsQuit(1);
        updateMember.setQuitTime(new Date());
        updateMember.setUpdatedAt(new Date());

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

        return groups.stream().map(group -> {
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
            vo.setCurrentMemberCount((int) chatGroupMemberService.getMemberCount(group.getId()));
            // 获取当前用户角色
            Integer myRole = chatGroupMemberService.getUserRole(group.getId(), userId);
            vo.setMyRole(myRole != null ? myRole : 0);
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
        group.setUpdatedAt(new Date());
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
            oldOwner.setUpdatedAt(new Date());
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
            newOwner.setUpdatedAt(new Date());
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
            member.setUpdatedAt(new Date());
            chatGroupMemberMapper.updateById(member);
        }

        log.info("用户[{}]退出了群聊[{}]", userId, groupId);
    }

    @Override
    public List<GroupInfoVO> searchGroup(String keyword, String currentUserId) {
        List<ChatGroup> groups = this.list(
                new LambdaQueryWrapper<ChatGroup>()
                        .like(ChatGroup::getGroupName, keyword)
                        .eq(ChatGroup::getStatus, 1)
        );

        return groups.stream().map(group -> {
            GroupInfoVO vo = BeanConvertUtil.convert(group, GroupInfoVO.class);
            // 转换群头像为公共桶永久直链
            vo.setGroupAvatar(minioUtil.getAvatarUrl(group.getGroupAvatar()));
            vo.setCurrentMemberCount((int) chatGroupMemberService.getMemberCount(group.getId()));

            // 判断当前用户状态
            if (chatGroupMemberService.isGroupMember(group.getId(), currentUserId)) {
                vo.setMyRole(chatGroupMemberService.getUserRole(group.getId(), currentUserId));
                vo.setApplyStatus("member");
            } else {
                vo.setMyRole(0);
                // 检查是否有待处理的申请
                long pendingCount = groupApplicationMapper.selectCount(
                        new LambdaQueryWrapper<GroupApplication>()
                                .eq(GroupApplication::getGroupId, group.getId())
                                .eq(GroupApplication::getApplicantId, Long.valueOf(currentUserId))
                                .eq(GroupApplication::getStatus, 0)
                );
                vo.setApplyStatus(pendingCount > 0 ? "pending" : null);
            }
            return vo;
        }).collect(Collectors.toList());
    }
}




