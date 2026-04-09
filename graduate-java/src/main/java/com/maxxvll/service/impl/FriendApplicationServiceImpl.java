package com.maxxvll.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.common.constants.RedisKeyConstants;
import com.maxxvll.common.config.KafkaFeatureProperties;
import com.maxxvll.common.constants.ApplicationStatusConstants;
import com.maxxvll.common.dto.FriendApplyDTO;
import com.maxxvll.common.dto.FriendApplyHandleDTO;
import com.maxxvll.common.dto.FriendBlacklistUpdateDTO;
import com.maxxvll.common.dto.FriendRelationUpdateDTO;
import com.maxxvll.common.enums.MessageType;
import com.maxxvll.common.enums.SessionType;
import com.maxxvll.common.event.FriendApplicationEvent;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.common.producer.FriendApplicationEventProducer;
import com.maxxvll.common.vo.FriendApplicationVO;
import com.maxxvll.component.ChatPushSupport;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.domain.FriendApplication;
import com.maxxvll.domain.FriendRelationSetting;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.mapper.FriendApplicationMapper;
import com.maxxvll.mapper.FriendRelationSettingMapper;
import com.maxxvll.netty.WebSocketConstants;
import com.maxxvll.service.ChatMessageService;
import com.maxxvll.service.ChatSessionService;
import com.maxxvll.service.FriendApplicationService;
import com.maxxvll.utils.BeanConvertUtil;
import com.maxxvll.utils.MinioUtil;
import com.maxxvll.utils.RedissonCacheUtil;
import com.maxxvll.component.ChatPushSupport;
import com.maxxvll.netty.WebSocketConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class FriendApplicationServiceImpl extends ServiceImpl<FriendApplicationMapper, FriendApplication>
        implements FriendApplicationService {

    private static final int FLAG_NO = 0;
    private static final int FLAG_YES = 1;

    @Resource
    private ChatUserMapper chatUserMapper;
    @Resource
    private MinioUtil minioUtil;
    @Resource
    private ChatSessionService chatSessionService;
    @Resource
    private ChatMessageService chatMessageService;
    @Resource
    private FriendApplicationEventProducer friendApplicationEventProducer;
    @Resource
    private KafkaFeatureProperties kafkaFeatureProperties;
    @Resource
    private FriendRelationSettingMapper friendRelationSettingMapper;
    @Resource
    private RedissonCacheUtil redissonCacheUtil;
    @Resource
    private com.maxxvll.mapper.FriendGroupMapper friendGroupMapper;

    @Resource
    private ChatPushSupport chatPushSupport;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyFriend(FriendApplyDTO applyDTO, String applicantId) {
        String targetId = applyDTO.getTargetId();

        ChatUser targetUser = chatUserMapper.selectById(targetId);
        if (targetUser == null || targetUser.getStatus() != 1) {
            throw new BusinessException("目标用户不存在");
        }
        if (targetId.equals(applicantId)) {
            throw new BusinessException("不能向自己发送好友申请");
        }
        if (isBlacklistedByTarget(applicantId, targetId)) {
            throw new BusinessException("对方暂时无法接收你的好友申请");
        }
        if (isAlreadyFriend(applicantId, targetId)) {
            throw new BusinessException("对方已经是你的好友");
        }

        FriendApplication existing = this.getOne(
                new LambdaQueryWrapper<FriendApplication>()
                        .eq(FriendApplication::getApplicantId, Long.valueOf(applicantId))
                        .eq(FriendApplication::getTargetUserId, Long.valueOf(targetId))
                        .eq(FriendApplication::getStatus, ApplicationStatusConstants.STATUS_PENDING)
        );
        if (existing != null) {
            throw new BusinessException("已存在待处理的好友申请，请勿重复申请");
        }

        FriendApplication application = new FriendApplication();
        application.setApplicantId(Long.valueOf(applicantId));
        application.setTargetUserId(Long.valueOf(targetId));
        application.setRemark(applyDTO.getRemark());
        application.setStatus(ApplicationStatusConstants.STATUS_PENDING);
        application.setCreateTime(new Date());
        application.setUpdateTime(new Date());

        this.save(application);
        log.info("Friend application created, applicantId={}, targetId={}", applicantId, targetId);

        if (kafkaFeatureProperties.isFriendNotificationAsyncEnabled()) {
            try {
                Map<String, ChatUser> userMap = loadUsers(List.of(applicantId, targetId));
                ChatUser applicant = userMap.get(applicantId);
                ChatUser target = userMap.get(targetId);

                FriendApplicationEvent event = FriendApplicationEvent.builder()
                        .applicationId(application.getId())
                        .applicantId(Long.valueOf(applicantId))
                        .applicantUsername(applicant != null ? applicant.getUsername() : "")
                        .applicantAvatar(applicant != null ? applicant.getAvatar() : "")
                        .targetUserId(Long.valueOf(targetId))
                        .targetUsername(target != null ? target.getUsername() : "")
                        .remark(applyDTO.getRemark())
                        .actionType(FriendApplicationEvent.ActionType.APPLY)
                        .createTime(new Date())
                        .build();

                friendApplicationEventProducer.sendFriendApplicationEventWithTargetKey(event);
            } catch (Exception e) {
                log.error("Failed to publish friend application event, applicationId={}", application.getId(), e);
            }
        }
    }

    @Override
    public List<FriendApplicationVO> getReceivedApplications(String userId) {
        List<FriendApplication> applications = this.list(
                new LambdaQueryWrapper<FriendApplication>()
                        .eq(FriendApplication::getTargetUserId, Long.valueOf(userId))
                        .orderByDesc(FriendApplication::getCreateTime)
        );
        return buildVOList(applications);
    }

    @Override
    public List<FriendApplicationVO> getSentApplications(String userId) {
        List<FriendApplication> applications = this.list(
                new LambdaQueryWrapper<FriendApplication>()
                        .eq(FriendApplication::getApplicantId, Long.valueOf(userId))
                        .orderByDesc(FriendApplication::getCreateTime)
        );
        return buildVOList(applications);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleApplication(FriendApplyHandleDTO handleDTO, String handlerId) {
        FriendApplication application = this.getById(handleDTO.getApplyId());
        if (application == null) {
            throw new BusinessException("申请记录不存在");
        }
        if (!application.getTargetUserId().equals(Long.valueOf(handlerId))) {
            throw new BusinessException("无权限处理该申请");
        }
        if (!application.getStatus().equals(ApplicationStatusConstants.STATUS_PENDING)) {
            throw new BusinessException("该申请已处理");
        }

        application.setStatus(handleDTO.getStatus());
        if (handleDTO.getStatus() == ApplicationStatusConstants.STATUS_REJECTED
                && StrUtil.isNotBlank(handleDTO.getRejectReason())) {
            application.setRejectReason(handleDTO.getRejectReason());
        }
        application.setUpdateTime(new Date());
        this.updateById(application);

        String sessionId = null;
        ChatUser applicant = null;
        if (handleDTO.getStatus() == ApplicationStatusConstants.STATUS_APPROVED) {
            long applicantId = application.getApplicantId();
            long targetUserId = application.getTargetUserId();
            sessionId = Math.min(applicantId, targetUserId) + "_" + Math.max(applicantId, targetUserId);

            Map<String, ChatUser> userMap = loadUsers(
                    List.of(String.valueOf(applicantId), String.valueOf(targetUserId))
            );
            applicant = userMap.get(String.valueOf(applicantId));
            ChatUser handler = userMap.get(String.valueOf(targetUserId));

            if (applicant != null && handler != null) {
                chatSessionService.initFriendSession(
                        String.valueOf(applicantId),
                        String.valueOf(targetUserId),
                        sessionId,
                        applicant,
                        handler
                );

                if (StrUtil.isNotBlank(application.getRemark())) {
                    ChatMessage msg = chatMessageService.saveDirectly(
                            sessionId,
                            String.valueOf(applicantId),
                            String.valueOf(targetUserId),
                            SessionType.SINGLE.getCode(),
                            MessageType.TEXT.getCode(),
                            application.getRemark()
                    );
                    chatSessionService.refreshAllLastMessage(msg);
                }
            }

            ensureFriendSettingsBidirectional(applicantId, targetUserId);
        }

        if (kafkaFeatureProperties.isFriendNotificationAsyncEnabled()) {
            try {
                if (applicant == null) {
                    applicant = loadUsers(List.of(String.valueOf(application.getApplicantId())))
                            .get(String.valueOf(application.getApplicantId()));
                }

                FriendApplicationEvent event = FriendApplicationEvent.builder()
                        .applicationId(application.getId())
                        .applicantId(application.getApplicantId())
                        .applicantUsername(applicant != null ? applicant.getUsername() : "")
                        .applicantAvatar(applicant != null ? applicant.getAvatar() : "")
                        .targetUserId(application.getTargetUserId())
                        .remark(application.getRemark())
                        .actionType(handleDTO.getStatus() == ApplicationStatusConstants.STATUS_APPROVED
                                ? FriendApplicationEvent.ActionType.ACCEPT
                                : FriendApplicationEvent.ActionType.REJECT)
                        .status(handleDTO.getStatus())
                        .rejectReason(application.getRejectReason())
                        .sessionId(sessionId)
                        .createTime(application.getCreateTime())
                        .build();

                friendApplicationEventProducer.sendFriendApplicationEventWithApplicantKey(event);
            } catch (Exception e) {
                log.error("Failed to publish friend application handle event, applicationId={}", application.getId(), e);
            }
        }

        // 清除申请人和处理人的好友列表缓存
        String applicantId = String.valueOf(application.getApplicantId());
        String handlerUserId = handlerId;
        invalidateFriendListCache(applicantId);
        invalidateFriendListCache(handlerUserId);
    }

    @Override
    public List<FriendApplicationVO> getFriendList(String userId) {
        // 尝试从缓存获取
        String cacheKey = RedisKeyConstants.buildKey(RedisKeyConstants.FRIEND_PREFIX, RedisKeyConstants.FRIEND_LIST, userId);
        List<FriendApplicationVO> cached = redissonCacheUtil.get(cacheKey);
        if (cached != null) {
            log.debug("从缓存获取好友列表, userId={}", userId);
            return cached;
        }

        // 缓存未命中，查询数据库
        Long userIdLong = Long.valueOf(userId);
        List<FriendApplication> accepted = this.list(
                new LambdaQueryWrapper<FriendApplication>()
                        .eq(FriendApplication::getStatus, ApplicationStatusConstants.STATUS_APPROVED)
                        .and(w -> w.eq(FriendApplication::getApplicantId, userIdLong)
                                .or().eq(FriendApplication::getTargetUserId, userIdLong))
                        .orderByDesc(FriendApplication::getUpdateTime)
        );
        if (accepted.isEmpty()) {
            // 缓存空结果
            redissonCacheUtil.set(cacheKey, List.of(), RedisKeyConstants.CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            return List.of();
        }

        LinkedHashSet<Long> otherUserIds = accepted.stream()
                .map(app -> app.getApplicantId().equals(userIdLong)
                        ? app.getTargetUserId()
                        : app.getApplicantId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, FriendRelationSetting> settingMap = Map.of();
        try {
            ensureFriendSettings(userIdLong, otherUserIds);
            settingMap = loadFriendSettings(userIdLong, otherUserIds);
        } catch (DataAccessException e) {
            if (isFriendRelationSettingTableMissing(e)) {
                log.warn("friend_relation_setting table is missing, fallback to legacy friend list rendering", e);
            } else {
                throw e;
            }
        }
        Map<String, ChatUser> userMap = loadUsers(
                otherUserIds.stream().map(String::valueOf).collect(Collectors.toList())
        );

        // 加载分组信息
        Map<Long, com.maxxvll.domain.FriendGroup> groupMap = Map.of();
        try {
            List<com.maxxvll.domain.FriendGroup> groups = friendGroupMapper.selectByOwnerUserIdOrderByOrder(userIdLong);
            groupMap = groups.stream().collect(Collectors.toMap(com.maxxvll.domain.FriendGroup::getId, g -> g, (a, b) -> a));
        } catch (Exception e) {
            log.warn("Failed to load friend groups for userId={}", userId, e);
        }

        List<FriendApplicationVO> result = new ArrayList<>(accepted.size());
        for (FriendApplication app : accepted) {
            Long otherId = app.getApplicantId().equals(userIdLong) ? app.getTargetUserId() : app.getApplicantId();
            FriendRelationSetting setting = settingMap.get(otherId);
            if (setting != null && (isFlagEnabled(setting.getIsDeleted()) || isFlagEnabled(setting.getIsBlacklisted()))) {
                continue;
            }

            ChatUser other = userMap.get(String.valueOf(otherId));
            if (other == null) {
                continue;
            }

            FriendApplicationVO vo = new FriendApplicationVO();
            vo.setId(app.getId());
            vo.setApplicantId(String.valueOf(otherId));
            vo.setApplicantNickname(other.getNickname());
            vo.setApplicantUsername(other.getUsername());
            vo.setApplicantAvatar(buildAvatarUrl(other.getAvatar()));
            vo.setStatus(ApplicationStatusConstants.STATUS_APPROVED);
            vo.setCreateTime(app.getCreateTime() != null ? app.getCreateTime().toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);
            vo.setUpdateTime(app.getUpdateTime() != null ? app.getUpdateTime().toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);

            if (setting != null) {
                vo.setRemarkName(setting.getRemarkName());
                vo.setTagName(setting.getTagName());
                vo.setGroupId(setting.getGroupId());
                if (setting.getGroupId() != null) {
                    com.maxxvll.domain.FriendGroup group = groupMap.get(setting.getGroupId());
                    if (group != null) {
                        vo.setGroupName(group.getGroupName());
                    }
                }
                vo.setPermissionScope(setting.getPermissionScope());
                vo.setStarred(isFlagEnabled(setting.getIsStarred()));
                vo.setBlacklisted(isFlagEnabled(setting.getIsBlacklisted()));
            }

            result.add(vo);
        }

        result.sort(
                Comparator.comparing((FriendApplicationVO vo) -> Boolean.TRUE.equals(vo.getStarred()))
                        .reversed()
                        .thenComparing(FriendApplicationVO::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder()))
        );

        // 缓存结果
        redissonCacheUtil.set(cacheKey, result, RedisKeyConstants.CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.debug("好友列表已缓存, userId={}, size={}", userId, result.size());

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFriendRelation(FriendRelationUpdateDTO updateDTO, String userId) {
        Long ownerUserId = Long.valueOf(userId);
        Long friendUserId = Long.valueOf(updateDTO.getFriendUserId());
        validateFriendTarget(ownerUserId, friendUserId);

        FriendRelationSetting setting = getOwnedFriendSetting(ownerUserId, friendUserId);
        boolean changed = false;

        if (updateDTO.getRemarkName() != null) {
            setting.setRemarkName(normalizeOptionalText(updateDTO.getRemarkName()));
            setting.setIsDeleted(FLAG_NO);
            changed = true;
        }
        if (updateDTO.getTagName() != null) {
            setting.setTagName(normalizeOptionalText(updateDTO.getTagName()));
            changed = true;
        }
        if (updateDTO.getGroupId() != null) {
            setting.setGroupId(updateDTO.getGroupId());
            changed = true;
        }
        if (updateDTO.getPermissionScope() != null) {
            setting.setPermissionScope(updateDTO.getPermissionScope());
            changed = true;
        }
        if (updateDTO.getStarred() != null) {
            setting.setIsStarred(Boolean.TRUE.equals(updateDTO.getStarred()) ? FLAG_YES : FLAG_NO);
            changed = true;
        }

        if (!changed) {
            return;
        }

        friendRelationSettingMapper.updateById(setting);
        log.info("Friend relation updated, ownerUserId={}, friendUserId={}", ownerUserId, friendUserId);

        // 清除缓存
        invalidateFriendListCache(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFriendBlacklist(FriendBlacklistUpdateDTO updateDTO, String userId) {
        Long ownerUserId = Long.valueOf(userId);
        Long friendUserId = Long.valueOf(updateDTO.getFriendUserId());
        validateFriendTarget(ownerUserId, friendUserId);

        FriendRelationSetting setting = getOwnedFriendSetting(ownerUserId, friendUserId);
        boolean blacklisted = Boolean.TRUE.equals(updateDTO.getBlacklisted());
        setting.setIsBlacklisted(blacklisted ? FLAG_YES : FLAG_NO);
        setting.setIsDeleted(blacklisted ? FLAG_YES : FLAG_NO);
        friendRelationSettingMapper.updateById(setting);

        log.info("Friend blacklist updated, ownerUserId={}, friendUserId={}, blacklisted={}",
                ownerUserId, friendUserId, blacklisted);

        // 清除缓存
        invalidateFriendListCache(userId);
        invalidateBlacklistCache(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFriend(String friendUserId, String userId) {
        Long ownerUserId = Long.valueOf(userId);
        Long targetFriendUserId = Long.valueOf(friendUserId);
        validateFriendTarget(ownerUserId, targetFriendUserId);

        FriendRelationSetting setting = getOwnedFriendSetting(ownerUserId, targetFriendUserId);
        setting.setIsDeleted(FLAG_YES);
        setting.setIsStarred(FLAG_NO);
        friendRelationSettingMapper.updateById(setting);

        log.info("Friend deleted from contact list, ownerUserId={}, friendUserId={}", ownerUserId, targetFriendUserId);

        // 清除缓存
        invalidateFriendListCache(userId);
    }

    @Override
    public List<FriendApplicationVO> getBlacklist(String userId) {
        Long userIdLong = Long.valueOf(userId);

        List<FriendRelationSetting> blacklistedSettings = new ArrayList<>();
        try {
            blacklistedSettings = friendRelationSettingMapper.selectList(
                    new LambdaQueryWrapper<FriendRelationSetting>()
                            .eq(FriendRelationSetting::getOwnerUserId, userIdLong)
                            .eq(FriendRelationSetting::getIsBlacklisted, FLAG_YES)
                            .orderByDesc(FriendRelationSetting::getUpdateTime)
            );
        } catch (DataAccessException e) {
            if (isFriendRelationSettingTableMissing(e)) {
                log.warn("friend_relation_setting table is missing, blacklist is empty", e);
                return List.of();
            }
            throw e;
        }

        if (blacklistedSettings.isEmpty()) {
            return List.of();
        }

        List<Long> blacklistedUserIds = blacklistedSettings.stream()
                .map(FriendRelationSetting::getFriendUserId)
                .collect(Collectors.toList());

        Map<String, ChatUser> userMap = loadUsers(
                blacklistedUserIds.stream().map(String::valueOf).collect(Collectors.toList())
        );

        List<FriendApplicationVO> result = new ArrayList<>(blacklistedSettings.size());
        for (FriendRelationSetting setting : blacklistedSettings) {
            Long blacklistedId = setting.getFriendUserId();
            ChatUser user = userMap.get(String.valueOf(blacklistedId));
            if (user == null) {
                continue;
            }

            FriendApplicationVO vo = new FriendApplicationVO();
            vo.setId(setting.getId());
            vo.setApplicantId(String.valueOf(blacklistedId));
            vo.setApplicantNickname(user.getNickname());
            vo.setApplicantUsername(user.getUsername());
            vo.setApplicantAvatar(buildAvatarUrl(user.getAvatar()));
            vo.setRemarkName(setting.getRemarkName());
            vo.setBlacklisted(true);
            vo.setUpdateTime(setting.getUpdateTime() != null ? setting.getUpdateTime().toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);

            result.add(vo);
        }

        return result;
    }

    private List<FriendApplicationVO> buildVOList(List<FriendApplication> applications) {
        if (applications == null || applications.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> userIds = applications.stream()
                .flatMap(app -> Stream.of(app.getApplicantId(), app.getTargetUserId()))
                .map(String::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, ChatUser> userMap = loadUsers(userIds);
        return applications.stream()
                .map(app -> {
                    FriendApplicationVO vo = BeanConvertUtil.convert(app, FriendApplicationVO.class);
                    vo.setStatusDesc(getStatusDesc(app.getStatus()));

                    ChatUser applicant = userMap.get(String.valueOf(app.getApplicantId()));
                    if (applicant != null) {
                        vo.setApplicantNickname(applicant.getNickname());
                        vo.setApplicantUsername(applicant.getUsername());
                        vo.setApplicantAvatar(buildAvatarUrl(applicant.getAvatar()));
                    }

                    ChatUser target = userMap.get(String.valueOf(app.getTargetUserId()));
                    if (target != null) {
                        vo.setTargetNickname(target.getNickname());
                        vo.setTargetUsername(target.getUsername());
                        vo.setTargetAvatar(buildAvatarUrl(target.getAvatar()));
                    }

                    return vo;
                })
                .collect(Collectors.toList());
    }

    private void validateFriendTarget(Long ownerUserId, Long friendUserId) {
        if (ownerUserId.equals(friendUserId)) {
            throw new BusinessException("不能操作自己");
        }
        if (!isAlreadyFriend(String.valueOf(ownerUserId), String.valueOf(friendUserId))) {
            throw new BusinessException("好友关系不存在");
        }
    }

    private FriendRelationSetting getOwnedFriendSetting(Long ownerUserId, Long friendUserId) {
        ensureFriendSettings(ownerUserId, List.of(friendUserId));
        FriendRelationSetting setting = friendRelationSettingMapper.selectOne(
                new LambdaQueryWrapper<FriendRelationSetting>()
                        .eq(FriendRelationSetting::getOwnerUserId, ownerUserId)
                        .eq(FriendRelationSetting::getFriendUserId, friendUserId)
                        .last("LIMIT 1")
        );
        if (setting == null) {
            throw new BusinessException("好友关系配置不存在");
        }
        return setting;
    }

    private void ensureFriendSettingsBidirectional(Long userIdA, Long userIdB) {
        List<FriendRelationSetting> items = new ArrayList<>(2);
        items.add(buildDefaultSetting(userIdA, userIdB));
        items.add(buildDefaultSetting(userIdB, userIdA));
        friendRelationSettingMapper.batchInsertIgnore(items);
    }

    private void ensureFriendSettings(Long ownerUserId, Collection<Long> friendUserIds) {
        if (friendUserIds == null || friendUserIds.isEmpty()) {
            return;
        }

        List<FriendRelationSetting> items = friendUserIds.stream()
                .filter(Objects::nonNull)
                .filter(friendUserId -> !friendUserId.equals(ownerUserId))
                .distinct()
                .map(friendUserId -> buildDefaultSetting(ownerUserId, friendUserId))
                .toList();

        if (!items.isEmpty()) {
            friendRelationSettingMapper.batchInsertIgnore(items);
        }
    }

    private FriendRelationSetting buildDefaultSetting(Long ownerUserId, Long friendUserId) {
        FriendRelationSetting setting = new FriendRelationSetting();
        setting.setOwnerUserId(ownerUserId);
        setting.setFriendUserId(friendUserId);
        return setting;
    }

    private Map<Long, FriendRelationSetting> loadFriendSettings(Long ownerUserId, Collection<Long> friendUserIds) {
        if (friendUserIds == null || friendUserIds.isEmpty()) {
            return Map.of();
        }

        return friendRelationSettingMapper.selectList(
                        new LambdaQueryWrapper<FriendRelationSetting>()
                                .eq(FriendRelationSetting::getOwnerUserId, ownerUserId)
                                .in(FriendRelationSetting::getFriendUserId, friendUserIds)
                ).stream()
                .collect(Collectors.toMap(
                        FriendRelationSetting::getFriendUserId,
                        setting -> setting,
                        (left, right) -> left
                ));
    }

    private boolean isBlacklistedByTarget(String applicantId, String targetId) {
        try {
            Long count = friendRelationSettingMapper.selectCount(
                    new LambdaQueryWrapper<FriendRelationSetting>()
                            .eq(FriendRelationSetting::getOwnerUserId, Long.valueOf(targetId))
                            .eq(FriendRelationSetting::getFriendUserId, Long.valueOf(applicantId))
                            .eq(FriendRelationSetting::getIsBlacklisted, FLAG_YES)
            );
            return count != null && count > 0;
        } catch (DataAccessException e) {
            if (isFriendRelationSettingTableMissing(e)) {
                log.warn("friend_relation_setting table is missing, blacklist check is skipped temporarily", e);
                return false;
            }
            throw e;
        }
    }

    private boolean isFriendRelationSettingTableMissing(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                    && message.contains("friend_relation_setting")
                    && message.contains("doesn't exist")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Map<String, ChatUser> loadUsers(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        return chatUserMapper.selectList(
                        new LambdaQueryWrapper<ChatUser>()
                                .select(ChatUser::getId, ChatUser::getUsername, ChatUser::getNickname, ChatUser::getAvatar)
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

    private boolean isAlreadyFriend(String userId1, String userId2) {
        Long id1 = Long.valueOf(userId1);
        Long id2 = Long.valueOf(userId2);
        return this.count(
                new LambdaQueryWrapper<FriendApplication>()
                        .eq(FriendApplication::getStatus, ApplicationStatusConstants.STATUS_APPROVED)
                        .and(w -> w.eq(FriendApplication::getApplicantId, id1)
                                .eq(FriendApplication::getTargetUserId, id2)
                                .or()
                                .eq(FriendApplication::getApplicantId, id2)
                                .eq(FriendApplication::getTargetUserId, id1))
        ) > 0;
    }

    private String normalizeOptionalText(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isBlank(trimmed) ? null : trimmed;
    }

    private boolean isFlagEnabled(Integer value) {
        return value != null && value == FLAG_YES;
    }

    private String getStatusDesc(Integer status) {
        return ApplicationStatusConstants.getStatusDesc(status);
    }

    /**
     * 清除用户好友列表缓存
     */
    private void invalidateFriendListCache(String userId) {
        String cacheKey = RedisKeyConstants.buildKey(RedisKeyConstants.FRIEND_PREFIX, RedisKeyConstants.FRIEND_LIST, userId);
        redissonCacheUtil.delete(cacheKey);
        log.debug("好友列表缓存已清除, userId={}", userId);

        // 推送好友列表更新事件给客户端
        pushFriendListUpdateEvent(userId);
    }

    /**
     * 清除用户黑名单缓存
     */
    private void invalidateBlacklistCache(String userId) {
        String cacheKey = RedisKeyConstants.buildKey(RedisKeyConstants.FRIEND_PREFIX, RedisKeyConstants.FRIEND_BLACKLIST, userId);
        redissonCacheUtil.delete(cacheKey);
        log.debug("黑名单缓存已清除, userId={}", userId);
    }


    /**
     * 推送好友列表更新事件到客户端（WebSocket）
     */
    private void pushFriendListUpdateEvent(String userId) {
        try {
            // 获取好友列表
            List<FriendApplicationVO> friendList = getFriendList(userId);

            Map<String, Object> data = new HashMap<>();
            data.put("friends", friendList);

            chatPushSupport.pushCacheSyncEvent(
                userId,
                WebSocketConstants.MessageType.CACHE_SYNC_FRIEND_LIST,
                data
            );
            log.debug("Pushed friend list update event to userId={}", userId);
        } catch (Exception e) {
            log.warn("Failed to push friend list update event, userId={}", userId, e);
        }
    }
}
