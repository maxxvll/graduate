package com.maxxvll.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.common.vo.FriendApplicationVO;
import com.maxxvll.common.vo.FriendGroupVO;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.domain.FriendApplication;
import com.maxxvll.domain.FriendGroup;
import com.maxxvll.domain.FriendRelationSetting;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.mapper.FriendApplicationMapper;
import com.maxxvll.mapper.FriendGroupMapper;
import com.maxxvll.mapper.FriendRelationSettingMapper;
import com.maxxvll.service.FriendApplicationService;
import com.maxxvll.service.FriendGroupService;
import com.maxxvll.utils.MinioUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 好友分组服务实现
 *
 * @author 20570
 */
@Slf4j
@Service
public class FriendGroupServiceImpl extends ServiceImpl<FriendGroupMapper, FriendGroup>
        implements FriendGroupService {

    private static final int FLAG_NO = 0;
    private static final int FLAG_YES = 1;

    private static final String DEFAULT_GROUP_NAME = "我的好友";

    @jakarta.annotation.Resource
    private FriendGroupMapper friendGroupMapper;
    @jakarta.annotation.Resource
    private FriendRelationSettingMapper friendRelationSettingMapper;
    @jakarta.annotation.Resource
    private ChatUserMapper chatUserMapper;
    @jakarta.annotation.Resource
    private MinioUtil minioUtil;

    @Override
    public List<FriendGroupVO> getGroupList(String userId) {
        Long ownerUserId = Long.valueOf(userId);

        // 确保默认分组存在
        ensureDefaultGroup(ownerUserId);

        // 获取用户的所有分组
        List<FriendGroup> groups = friendGroupMapper.selectByOwnerUserIdOrderByOrder(ownerUserId);

        // 获取好友关系设置
        List<FriendRelationSetting> friendSettings = friendRelationSettingMapper.selectList(
                new LambdaQueryWrapper<FriendRelationSetting>()
                        .eq(FriendRelationSetting::getOwnerUserId, ownerUserId)
                        .eq(FriendRelationSetting::getIsDeleted, FLAG_NO)
                        .eq(FriendRelationSetting::getIsBlacklisted, FLAG_NO)
                        .isNotNull(FriendRelationSetting::getGroupId)
        );

        // 按分组ID分组好友
        Map<Long, List<FriendRelationSetting>> settingsByGroup = friendSettings.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getGroupId() != null ? s.getGroupId() : 0L,
                        Collectors.toList()
                ));

        // 获取所有好友用户信息
        Set<String> friendUserIds = friendSettings.stream()
                .map(s -> String.valueOf(s.getFriendUserId()))
                .collect(Collectors.toSet());

        Map<String, ChatUser> userMap = loadUsers(friendUserIds);

        // 构建返回结果
        List<FriendGroupVO> result = new ArrayList<>();
        for (FriendGroup group : groups) {
            FriendGroupVO vo = new FriendGroupVO();
            vo.setId(group.getId());
            vo.setName(group.getGroupName());
            vo.setOrder(group.getGroupOrder());
            vo.setIsDefault(group.getIsDefault() == FLAG_YES);
            vo.setCreateTime(group.getCreateTime() != null ?
                    group.getCreateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);

            List<FriendRelationSetting> groupSettings = settingsByGroup.getOrDefault(group.getId(), Collections.emptyList());
            vo.setFriendCount(groupSettings.size());

            // 构建好友列表
            List<FriendGroupVO.FriendVO> friends = new ArrayList<>();
            for (FriendRelationSetting setting : groupSettings) {
                ChatUser user = userMap.get(String.valueOf(setting.getFriendUserId()));
                if (user == null) continue;

                FriendGroupVO.FriendVO friendVO = new FriendGroupVO.FriendVO();
                friendVO.setUserId(Long.valueOf(user.getId()));
                friendVO.setNickname(user.getNickname());
                friendVO.setUsername(user.getUsername());
                friendVO.setAvatar(buildAvatarUrl(user.getAvatar()));
                friendVO.setSignature(null);
                friendVO.setRemark(setting.getRemarkName());
                friends.add(friendVO);
            }
            vo.setFriends(friends);

            result.add(vo);
        }

        // 处理未分组的好友（分配到默认分组）
        FriendGroup defaultGroup = groups.stream()
                .filter(g -> g.getIsDefault() == FLAG_YES)
                .findFirst()
                .orElse(null);

        if (defaultGroup != null) {
            FriendGroupVO defaultVO = result.stream()
                    .filter(v -> v.getId().equals(defaultGroup.getId()))
                    .findFirst()
                    .orElse(null);

            if (defaultVO != null) {
                // 添加未分组好友到默认分组
                List<FriendRelationSetting> ungroupedSettings = friendSettings.stream()
                        .filter(s -> s.getGroupId() == null)
                        .toList();

                for (FriendRelationSetting setting : ungroupedSettings) {
                    ChatUser user = userMap.get(String.valueOf(setting.getFriendUserId()));
                    if (user == null) continue;

                    FriendGroupVO.FriendVO friendVO = new FriendGroupVO.FriendVO();
                    friendVO.setUserId(Long.valueOf(user.getId()));
                    friendVO.setNickname(user.getNickname());
                    friendVO.setUsername(user.getUsername());
                    friendVO.setAvatar(buildAvatarUrl(user.getAvatar()));
                    friendVO.setSignature(null);
                    friendVO.setRemark(setting.getRemarkName());
                    defaultVO.getFriends().add(friendVO);
                }
                defaultVO.setFriendCount(defaultVO.getFriends().size());
            }
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FriendGroup createGroup(String userId, String groupName) {
        Long ownerUserId = Long.valueOf(userId);
        String trimmedName = StrUtil.trim(groupName);

        if (StrUtil.isBlank(trimmedName)) {
            throw new BusinessException("分组名称不能为空");
        }
        if (trimmedName.length() > 32) {
            throw new BusinessException("分组名称不能超过32个字符");
        }
        if (DEFAULT_GROUP_NAME.equals(trimmedName)) {
            throw new BusinessException("该名称为保留名称");
        }

        // 检查是否已存在同名分组
        if (friendGroupMapper.countByOwnerAndName(ownerUserId, trimmedName) > 0) {
            throw new BusinessException("分组名称已存在");
        }

        // 获取当前分组数量，用于设置排序
        int currentCount = friendGroupMapper.countByOwnerUserId(ownerUserId);

        FriendGroup group = new FriendGroup();
        group.setOwnerUserId(ownerUserId);
        group.setGroupName(trimmedName);
        group.setGroupOrder(currentCount);
        group.setIsDefault(FLAG_NO);
        group.setCreateTime(new Date());
        group.setUpdateTime(new Date());

        friendGroupMapper.insert(group);
        log.info("Friend group created, userId={}, groupId={}, groupName={}", userId, group.getId(), trimmedName);

        return group;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGroup(Long groupId, String userId, String newName) {
        Long ownerUserId = Long.valueOf(userId);
        String trimmedName = StrUtil.trim(newName);

        FriendGroup group = friendGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("分组不存在");
        }
        if (!group.getOwnerUserId().equals(ownerUserId)) {
            throw new BusinessException("无权限操作该分组");
        }
        if (group.getIsDefault() == FLAG_YES) {
            throw new BusinessException("默认分组不可重命名");
        }

        if (StrUtil.isBlank(trimmedName)) {
            throw new BusinessException("分组名称不能为空");
        }
        if (trimmedName.length() > 32) {
            throw new BusinessException("分组名称不能超过32个字符");
        }
        if (DEFAULT_GROUP_NAME.equals(trimmedName)) {
            throw new BusinessException("该名称为保留名称");
        }
        if (trimmedName.equals(group.getGroupName())) {
            return; // 名称未变，无需更新
        }

        // 检查是否已存在同名分组
        if (friendGroupMapper.countByOwnerAndName(ownerUserId, trimmedName) > 0) {
            throw new BusinessException("分组名称已存在");
        }

        group.setGroupName(trimmedName);
        group.setUpdateTime(new Date());
        friendGroupMapper.updateById(group);

        log.info("Friend group updated, groupId={}, newName={}", groupId, trimmedName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long groupId, String userId) {
        Long ownerUserId = Long.valueOf(userId);

        FriendGroup group = friendGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("分组不存在");
        }
        if (!group.getOwnerUserId().equals(ownerUserId)) {
            throw new BusinessException("无权限操作该分组");
        }
        if (group.getIsDefault() == FLAG_YES) {
            throw new BusinessException("默认分组不可删除");
        }

        // 获取默认分组
        FriendGroup defaultGroup = getDefaultGroup(ownerUserId);
        if (defaultGroup == null) {
            defaultGroup = ensureDefaultGroup(ownerUserId);
        }

        // 将该分组的好友移动到默认分组
        friendRelationSettingMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<FriendRelationSetting>()
                        .eq(FriendRelationSetting::getOwnerUserId, ownerUserId)
                        .eq(FriendRelationSetting::getGroupId, groupId)
                        .set(FriendRelationSetting::getGroupId, defaultGroup.getId())
                        .set(FriendRelationSetting::getUpdateTime, new Date())
        );

        // 删除分组
        friendGroupMapper.deleteById(groupId);

        log.info("Friend group deleted, groupId={}, moved {} friends to default group",
                groupId, defaultGroup.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveFriendToGroup(String userId, Long friendUserId, Long groupId) {
        Long ownerUserId = Long.valueOf(userId);

        // 验证好友关系存在
        FriendRelationSetting setting = friendRelationSettingMapper.selectOne(
                new LambdaQueryWrapper<FriendRelationSetting>()
                        .eq(FriendRelationSetting::getOwnerUserId, ownerUserId)
                        .eq(FriendRelationSetting::getFriendUserId, friendUserId)
                        .last("LIMIT 1")
        );
        if (setting == null) {
            throw new BusinessException("好友关系不存在");
        }

        // 验证分组
        if (groupId != null) {
            FriendGroup group = friendGroupMapper.selectById(groupId);
            if (group == null) {
                throw new BusinessException("分组不存在");
            }
            if (!group.getOwnerUserId().equals(ownerUserId)) {
                throw new BusinessException("无权限操作该分组");
            }
        }

        setting.setGroupId(groupId);
        setting.setUpdateTime(new Date());
        friendRelationSettingMapper.updateById(setting);

        log.info("Friend moved to group, userId={}, friendUserId={}, groupId={}",
                userId, friendUserId, groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FriendGroup ensureDefaultGroup(Long userId) {
        FriendGroup defaultGroup = friendGroupMapper.selectDefaultGroup(userId);
        if (defaultGroup != null) {
            return defaultGroup;
        }

        // 创建默认分组
        defaultGroup = new FriendGroup();
        defaultGroup.setOwnerUserId(userId);
        defaultGroup.setGroupName(DEFAULT_GROUP_NAME);
        defaultGroup.setGroupOrder(0);
        defaultGroup.setIsDefault(FLAG_YES);
        defaultGroup.setCreateTime(new Date());
        defaultGroup.setUpdateTime(new Date());

        friendGroupMapper.insert(defaultGroup);
        log.info("Default friend group created for userId={}", userId);

        return defaultGroup;
    }

    @Override
    public FriendGroup getDefaultGroup(Long userId) {
        return friendGroupMapper.selectDefaultGroup(userId);
    }

    private Map<String, ChatUser> loadUsers(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        return chatUserMapper.selectList(
                        new LambdaQueryWrapper<ChatUser>()
                                .select(ChatUser::getId, ChatUser::getUsername, ChatUser::getNickname,
                                        ChatUser::getAvatar)
                                .in(ChatUser::getId, userIds)
                ).stream()
                .collect(Collectors.toMap(ChatUser::getId, user -> user));
    }

    private String buildAvatarUrl(String avatar) {
        if (StrUtil.isBlank(avatar) || avatar.startsWith("http")) {
            return avatar;
        }
        return minioUtil.getAvatarUrl(avatar);
    }
}
