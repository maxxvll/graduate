package com.maxxvll.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.common.dto.BatchNotificationSendDTO;
import com.maxxvll.common.dto.NotificationSettingUpdateDTO;
import com.maxxvll.common.dto.SystemNotificationSendDTO;
import com.maxxvll.common.vo.NotificationPageVO;
import com.maxxvll.common.vo.NotificationVO;
import com.maxxvll.common.vo.NotificationSettingVO;
import com.maxxvll.component.NettyChannelManager;
import com.maxxvll.domain.*;
import com.maxxvll.mapper.*;
import com.maxxvll.service.ChatGroupMemberService;
import com.maxxvll.service.NotificationService;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通知服务实现
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Slf4j
@Service
public class NotificationServiceImpl extends ServiceImpl<SysNotificationMapper, SysNotification>
        implements NotificationService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Resource
    private UserNotificationSettingMapper notificationSettingMapper;

    @Resource
    private UserNotificationRelationMapper notificationRelationMapper;

    @Resource
    private NettyChannelManager nettyChannelManager;

    @Resource
    private ChatGroupMemberService chatGroupMemberService;

    // ==================== 通知设置 ====================

    @Override
    public NotificationSettingVO getNotificationSetting(String userId) {
        UserNotificationSetting setting = notificationSettingMapper.selectById(userId);
        if (setting == null) {
            setting = UserNotificationSetting.createDefault(userId);
            notificationSettingMapper.insert(setting);
        }
        return convertToVO(setting);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNotificationSetting(String userId, NotificationSettingUpdateDTO updateDTO) {
        UserNotificationSetting setting = notificationSettingMapper.selectById(userId);
        if (setting == null) {
            setting = UserNotificationSetting.createDefault(userId);
        }

        // 更新免打扰设置
        if (updateDTO.getDndEnabled() != null) {
            setting.setDndEnabled(updateDTO.getDndEnabled() ? UserNotificationSetting.FLAG_YES : UserNotificationSetting.FLAG_NO);
        }
        if (updateDTO.getDndStartTime() != null) {
            setting.setDndStartTime(updateDTO.getDndStartTime());
        }
        if (updateDTO.getDndEndTime() != null) {
            setting.setDndEndTime(updateDTO.getDndEndTime());
        }

        // 更新通知类型开关
        updateFlag(setting::getNotifyFriendApply, setting::setNotifyFriendApply, updateDTO.getNotifyFriendApply());
        updateFlag(setting::getNotifyGroupApply, setting::setNotifyGroupApply, updateDTO.getNotifyGroupApply());
        updateFlag(setting::getNotifyGroupInvite, setting::setNotifyGroupInvite, updateDTO.getNotifyGroupInvite());
        updateFlag(setting::getNotifyMessage, setting::setNotifyMessage, updateDTO.getNotifyMessage());
        updateFlag(setting::getNotifyAt, setting::setNotifyAt, updateDTO.getNotifyAt());
        updateFlag(setting::getNotifySystem, setting::setNotifySystem, updateDTO.getNotifySystem());

        // 更新推送渠道设置
        updateFlag(setting::getPushChannelWebsocket, setting::setPushChannelWebsocket, updateDTO.getPushChannelWebsocket());
        updateFlag(setting::getPushChannelApp, setting::setPushChannelApp, updateDTO.getPushChannelApp());

        // 更新声音和振动
        updateFlag(setting::getSoundEnabled, setting::setSoundEnabled, updateDTO.getSoundEnabled());
        updateFlag(setting::getVibrationEnabled, setting::setVibrationEnabled, updateDTO.getVibrationEnabled());

        // 更新桌面通知
        updateFlag(setting::getDesktopNotification, setting::setDesktopNotification, updateDTO.getDesktopNotification());

        if (setting.getCreateTime() == null) {
            setting.setCreateTime(new Date());
        }
        setting.setUpdateTime(new Date());

        notificationSettingMapper.insertOrUpdate(setting);
        log.info("更新用户通知设置, userId={}", userId);
    }

    private void updateFlag(java.util.function.Supplier<Integer> getter, java.util.function.Consumer<Integer> setter, Boolean value) {
        if (value != null) {
            setter.accept(value ? UserNotificationSetting.FLAG_YES : UserNotificationSetting.FLAG_NO);
        }
    }

    private NotificationSettingVO convertToVO(UserNotificationSetting setting) {
        NotificationSettingVO vo = new NotificationSettingVO();
        vo.setDndEnabled(UserNotificationSetting.FLAG_YES == setting.getDndEnabled());
        vo.setDndStartTime(setting.getDndStartTime());
        vo.setDndEndTime(setting.getDndEndTime());
        vo.setInDndPeriod(setting.isInDndPeriod());
        vo.setNotifyFriendApply(UserNotificationSetting.FLAG_YES == setting.getNotifyFriendApply());
        vo.setNotifyGroupApply(UserNotificationSetting.FLAG_YES == setting.getNotifyGroupApply());
        vo.setNotifyGroupInvite(UserNotificationSetting.FLAG_YES == setting.getNotifyGroupInvite());
        vo.setNotifyMessage(UserNotificationSetting.FLAG_YES == setting.getNotifyMessage());
        vo.setNotifyAt(UserNotificationSetting.FLAG_YES == setting.getNotifyAt());
        vo.setNotifySystem(UserNotificationSetting.FLAG_YES == setting.getNotifySystem());
        vo.setPushChannelWebsocket(UserNotificationSetting.FLAG_YES == setting.getPushChannelWebsocket());
        vo.setPushChannelApp(UserNotificationSetting.FLAG_YES == setting.getPushChannelApp());
        vo.setSoundEnabled(UserNotificationSetting.FLAG_YES == setting.getSoundEnabled());
        vo.setVibrationEnabled(UserNotificationSetting.FLAG_YES == setting.getVibrationEnabled());
        vo.setDesktopNotification(UserNotificationSetting.FLAG_YES == setting.getDesktopNotification());
        vo.setUpdatedAt(setting.getUpdateTime());
        return vo;
    }

    // ==================== 通知查询 ====================

    @Override
    public NotificationPageVO getNotifications(String userId, String cursor, Integer limit) {
        if (limit == null || limit <= 0 || limit > 100) {
            limit = 20;
        }

        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotification::getTargetType, SysNotification.TARGET_USER)
                .eq(SysNotification::getTargetId, userId)
                .eq(SysNotification::getStatus, SysNotification.STATUS_UNREAD)
                .orderByDesc(SysNotification::getCreateTime);

        if (cursor != null && !cursor.isBlank()) {
            try {
                long cursorTime = Long.parseLong(cursor);
                wrapper.lt(SysNotification::getCreateTime, new Date(cursorTime));
            } catch (NumberFormatException ignored) {
            }
        }

        List<SysNotification> notifications = this.list(wrapper);
        List<NotificationVO> voList = convertToNotificationVOList(notifications, userId);

        // 获取未读数量
        Long unreadCount = getUnreadCount(userId);

        // 判断是否有更多
        boolean hasMore = notifications.size() >= limit;

        // 计算下一次查询的游标
        String nextCursor = null;
        if (hasMore && !notifications.isEmpty()) {
            Date lastDate = notifications.get(notifications.size() - 1).getCreateTime();
            nextCursor = String.valueOf(lastDate.getTime());
        }

        NotificationPageVO page = new NotificationPageVO();
        page.setItems(voList);
        page.setNextCursor(nextCursor);
        page.setHasMore(hasMore);
        page.setUnreadCount(unreadCount);
        return page;
    }

    @Override
    public Long getUnreadCount(String userId) {
        // 查询用户未读的通知数量
        return baseMapper.selectCount(
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getTargetType, SysNotification.TARGET_USER)
                        .eq(SysNotification::getTargetId, userId)
                        .eq(SysNotification::getStatus, SysNotification.STATUS_UNREAD)
                        .and(w -> w
                                .isNull(SysNotification::getExpireTime)
                                .or()
                                .gt(SysNotification::getExpireTime, new Date())
                        )
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(String userId, Long notificationId) {
        // 创建或更新用户通知关系
        UserNotificationRelation relation = notificationRelationMapper.selectOne(
                new LambdaQueryWrapper<UserNotificationRelation>()
                        .eq(UserNotificationRelation::getUserId, userId)
                        .eq(UserNotificationRelation::getNotificationId, notificationId)
        );

        if (relation == null) {
            relation = new UserNotificationRelation();
            relation.setUserId(userId);
            relation.setNotificationId(notificationId);
            relation.setIsRead(UserNotificationRelation.FLAG_YES);
            relation.setReadTime(new Date());
            relation.setIsDeleted(UserNotificationRelation.FLAG_NO);
            relation.setCreateTime(new Date());
            relation.setUpdateTime(new Date());
            notificationRelationMapper.insert(relation);
        } else if (relation.getIsRead() != UserNotificationRelation.FLAG_YES) {
            relation.setIsRead(UserNotificationRelation.FLAG_YES);
            relation.setReadTime(new Date());
            relation.setUpdateTime(new Date());
            notificationRelationMapper.updateById(relation);
        }

        log.debug("标记通知已读, userId={}, notificationId={}", userId, notificationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(String userId) {
        List<SysNotification> notifications = this.list(
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getTargetType, SysNotification.TARGET_USER)
                        .eq(SysNotification::getTargetId, userId)
                        .eq(SysNotification::getStatus, SysNotification.STATUS_UNREAD)
        );

        List<UserNotificationRelation> relations = new ArrayList<>();
        Date now = new Date();

        for (SysNotification notification : notifications) {
            UserNotificationRelation relation = new UserNotificationRelation();
            relation.setUserId(userId);
            relation.setNotificationId(notification.getId());
            relation.setIsRead(UserNotificationRelation.FLAG_YES);
            relation.setReadTime(now);
            relation.setIsDeleted(UserNotificationRelation.FLAG_NO);
            relation.setCreateTime(now);
            relation.setUpdateTime(now);
            relations.add(relation);
        }

        if (!relations.isEmpty()) {
            // 逐条插入或更新（MyBatis-Plus不支持批量upsert，使用循环处理）
            for (UserNotificationRelation relation : relations) {
                UserNotificationRelation existing = notificationRelationMapper.selectOne(
                        new LambdaQueryWrapper<UserNotificationRelation>()
                                .eq(UserNotificationRelation::getUserId, relation.getUserId())
                                .eq(UserNotificationRelation::getNotificationId, relation.getNotificationId())
                );
                if (existing != null) {
                    notificationRelationMapper.updateById(relation);
                } else {
                    notificationRelationMapper.insert(relation);
                }
            }
        }

        log.info("标记所有通知已读, userId={}, count={}", userId, relations.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(String userId, Long notificationId) {
        // 软删除通知
        SysNotification notification = this.getById(notificationId);
        if (notification != null) {
            notification.setStatus(SysNotification.STATUS_DELETED);
            this.updateById(notification);
        }

        log.debug("删除通知, userId={}, notificationId={}", userId, notificationId);
    }

    // ==================== 通知发送 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendToUser(SystemNotificationSendDTO dto, String senderId, String senderName, String senderAvatar) {
        if (dto.getTargetId() == null || dto.getTargetId().isBlank()) {
            log.warn("发送通知失败：目标用户ID为空");
            return;
        }

        SysNotification notification = createNotification(dto, senderId, senderName, senderAvatar,
                SysNotification.TARGET_USER, dto.getTargetId());
        this.save(notification);

        // 推送实时通知
        if (canReceiveNotification(dto.getTargetId(), dto.getNotificationType())) {
            pushNotificationToUser(dto.getTargetId(), buildPushPayload(notification));
        }

        log.info("发送通知给用户, notificationId={}, targetUserId={}, type={}",
                notification.getId(), dto.getTargetId(), dto.getNotificationType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendToGroup(SystemNotificationSendDTO dto, String senderId, String senderName, String senderAvatar) {
        if (dto.getTargetId() == null || dto.getTargetId().isBlank()) {
            log.warn("发送通知失败：目标群组ID为空");
            return;
        }

        Long groupId;
        try {
            groupId = Long.parseLong(dto.getTargetId());
        } catch (NumberFormatException e) {
            log.warn("发送通知失败：群组ID格式错误, targetId={}", dto.getTargetId());
            return;
        }

        // 获取群组成员
        List<String> memberIds = chatGroupMemberService.getActiveMemberIds(groupId);
        if (memberIds == null || memberIds.isEmpty()) {
            log.warn("发送通知失败：群组成员为空, groupId={}", groupId);
            return;
        }

        Date now = new Date();
        List<SysNotification> notifications = new ArrayList<>();

        for (String memberId : memberIds) {
            if (memberId.equals(senderId)) {
                continue; // 不发送给自己
            }

            SysNotification notification = createNotification(dto, senderId, senderName, senderAvatar,
                    SysNotification.TARGET_USER, memberId);
            notification.setRelatedId(dto.getTargetId());
            notification.setRelatedType("GROUP");
            notifications.add(notification);
        }

        if (!notifications.isEmpty()) {
            this.saveBatch(notifications);

            // 批量推送
            for (SysNotification notification : notifications) {
                if (canReceiveNotification(notification.getTargetId(), dto.getNotificationType())) {
                    pushNotificationToUser(notification.getTargetId(), buildPushPayload(notification));
                }
            }
        }

        log.info("发送通知给群组成员, groupId={}, memberCount={}, type={}",
                dto.getTargetId(), notifications.size(), dto.getNotificationType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendBroadcast(SystemNotificationSendDTO dto) {
        SysNotification notification = createNotification(dto, null, "系统通知", null,
                SysNotification.TARGET_ALL, null);
        this.save(notification);

        // 广播给所有在线用户
        Set<String> onlineUsers = nettyChannelManager.getOnlineUsers();
        for (String userId : onlineUsers) {
            if (canReceiveNotification(userId, dto.getNotificationType())) {
                pushNotificationToUser(userId, buildPushPayload(notification));
            }
        }

        log.info("发送全员通知, notificationId={}, onlineCount={}", notification.getId(), onlineUsers.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendBatch(BatchNotificationSendDTO dto, String senderId, String senderName, String senderAvatar) {
        if (dto.getTargetUserIds() == null || dto.getTargetUserIds().isEmpty()) {
            log.warn("批量发送通知失败：目标用户列表为空");
            return;
        }

        Date now = new Date();
        List<SysNotification> notifications = new ArrayList<>();

        for (String targetUserId : dto.getTargetUserIds()) {
            SysNotification notification = new SysNotification();
            notification.setNotificationType(dto.getNotificationType());
            notification.setTitle(dto.getTitle());
            notification.setContent(dto.getContent());
            notification.setSenderId(senderId);
            notification.setSenderName(senderName);
            notification.setSenderAvatar(senderAvatar);
            notification.setTargetType(SysNotification.TARGET_USER);
            notification.setTargetId(targetUserId);
            notification.setRelatedId(dto.getRelatedId());
            notification.setRelatedType(dto.getRelatedType());
            notification.setPriority(dto.getPriority() != null ? dto.getPriority() : SysNotification.PRIORITY_NORMAL);
            notification.setStatus(SysNotification.STATUS_UNREAD);
            notification.setCreateTime(now);
            notification.setUpdateTime(now);
            notifications.add(notification);
        }

        if (!notifications.isEmpty()) {
            this.saveBatch(notifications);

            // 批量推送
            for (SysNotification notification : notifications) {
                if (canReceiveNotification(notification.getTargetId(), dto.getNotificationType())) {
                    pushNotificationToUser(notification.getTargetId(), buildPushPayload(notification));
                }
            }
        }

        log.info("批量发送通知, count={}, type={}", notifications.size(), dto.getNotificationType());
    }

    private SysNotification createNotification(SystemNotificationSendDTO dto, String senderId,
                                                String senderName, String senderAvatar,
                                                String targetType, String targetId) {
        Date now = new Date();
        SysNotification notification = new SysNotification();
        notification.setNotificationType(dto.getNotificationType());
        notification.setTitle(dto.getTitle());
        notification.setContent(dto.getContent());
        notification.setSenderId(senderId);
        notification.setSenderName(senderName);
        notification.setSenderAvatar(senderAvatar);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setRelatedId(dto.getRelatedId());
        notification.setRelatedType(dto.getRelatedType());
        notification.setPriority(dto.getPriority() != null ? dto.getPriority() : SysNotification.PRIORITY_NORMAL);
        notification.setStatus(SysNotification.STATUS_UNREAD);

        if (dto.getExpireTime() != null) {
            notification.setExpireTime(new Date(dto.getExpireTime()));
        }

        notification.setCreateTime(now);
        notification.setUpdateTime(now);
        return notification;
    }

    // ==================== 推送服务 ====================

    @Override
    public void pushNotificationToUser(String userId, Object notification) {
        try {
            boolean pushed = nettyChannelManager.sendSerializedMessageToUser(userId, JSON.toJSONString(notification));
            if (!pushed) {
                log.debug("用户不在线，未推送通知: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("推送通知失败: userId={}", userId, e);
        }
    }

    @Override
    public boolean canReceiveNotification(String userId, String notificationType) {
        UserNotificationSetting setting = notificationSettingMapper.selectById(userId);
        if (setting == null) {
            return true; // 默认允许
        }

        // 检查免打扰
        if (setting.isInDndPeriod()) {
            // 紧急通知仍然推送，这里简化为允许推送
            return true;
        }

        return setting.canPush(notificationType);
    }

    private Map<String, Object> buildPushPayload(SysNotification notification) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "notification");
        payload.put("notificationType", notification.getNotificationType());
        payload.put("id", notification.getId());
        payload.put("title", notification.getTitle());
        payload.put("content", notification.getContent());
        payload.put("senderId", notification.getSenderId());
        payload.put("senderName", notification.getSenderName());
        payload.put("senderAvatar", notification.getSenderAvatar());
        payload.put("priority", notification.getPriority());
        payload.put("relatedId", notification.getRelatedId());
        payload.put("relatedType", notification.getRelatedType());
        payload.put("createdAt", notification.getCreateTime() != null ? notification.getCreateTime().getTime() : System.currentTimeMillis());
        return payload;
    }

    private List<NotificationVO> convertToNotificationVOList(List<SysNotification> notifications, String userId) {
        if (notifications == null || notifications.isEmpty()) {
            return List.of();
        }

        // 获取已读状态
        List<Long> notificationIds = notifications.stream()
                .map(SysNotification::getId)
                .collect(Collectors.toList());

        Map<Long, Boolean> readStatusMap = new HashMap<>();
        notificationRelationMapper.selectList(
                new LambdaQueryWrapper<UserNotificationRelation>()
                        .eq(UserNotificationRelation::getUserId, userId)
                        .in(UserNotificationRelation::getNotificationId, notificationIds)
        ).forEach(r -> readStatusMap.put(r.getNotificationId(), r.getIsRead() != null && r.getIsRead() == UserNotificationRelation.FLAG_YES));

        return notifications.stream()
                .map(n -> convertToVO(n, readStatusMap.getOrDefault(n.getId(), false)))
                .collect(Collectors.toList());
    }

    private NotificationVO convertToVO(SysNotification notification, boolean isRead) {
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setNotificationType(notification.getNotificationType());
        vo.setNotificationTypeDesc(getTypeDesc(notification.getNotificationType()));
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setSenderId(notification.getSenderId());
        vo.setSenderName(notification.getSenderName());
        vo.setSenderAvatar(notification.getSenderAvatar());
        vo.setRelatedId(notification.getRelatedId());
        vo.setRelatedType(notification.getRelatedType());
        vo.setPriority(notification.getPriority());
        vo.setPriorityDesc(getPriorityDesc(notification.getPriority()));
        vo.setIsRead(isRead);
        vo.setReadTime(notification.getReadTime());
        vo.setCreatedAt(notification.getCreateTime());
        vo.setTimeDesc(getTimeDesc(notification.getCreateTime()));
        return vo;
    }

    private String getTypeDesc(String type) {
        return switch (type) {
            case SysNotification.TYPE_FRIEND_APPLY -> "好友申请";
            case SysNotification.TYPE_GROUP_APPLY -> "群申请";
            case SysNotification.TYPE_GROUP_INVITE -> "群邀请";
            case SysNotification.TYPE_MENTION -> "@提及";
            case SysNotification.TYPE_SYSTEM -> "系统通知";
            case SysNotification.TYPE_BROADCAST -> "全员通知";
            default -> "通知";
        };
    }

    private String getPriorityDesc(Integer priority) {
        if (priority == null) {
            return "普通";
        }
        return switch (priority) {
            case SysNotification.PRIORITY_IMPORTANT -> "重要";
            case SysNotification.PRIORITY_URGENT -> "紧急";
            default -> "普通";
        };
    }

    private String getTimeDesc(Date createdAt) {
        if (createdAt == null) {
            return "";
        }
        LocalDateTime dateTime = createdAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime now = LocalDateTime.now();

        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        if (minutes < 1) {
            return "刚刚";
        }
        if (minutes < 60) {
            return minutes + "分钟前";
        }

        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours < 24) {
            return hours + "小时前";
        }

        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days < 7) {
            return days + "天前";
        }

        return dateTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
    }
}
