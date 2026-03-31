package com.maxxvll.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.domain.Favorite;
import com.maxxvll.mapper.ChatMessageMapper;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.mapper.FavoriteMapper;
import com.maxxvll.service.FavoriteService;
import com.maxxvll.utils.MinioUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 收藏服务实现类
 */
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    private final ChatMessageMapper chatMessageMapper;
    private final ChatUserMapper chatUserMapper;
    private final MinioUtil minioUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Favorite addFavorite(String userId, Long messageId, String content, String messageType,
                                  String fileUrl, String senderId, String sessionId) {
        // 检查是否已收藏
        if (isFavorited(userId, messageId)) {
            throw new IllegalArgumentException("该消息已收藏");
        }

        // 如果没有提供内容，尝试从消息中获取
        if ((content == null || content.isEmpty()) && messageId != null) {
            ChatMessage message = chatMessageMapper.selectById(messageId);
            if (message != null) {
                content = message.getContent();
                if (messageType == null) {
                    messageType = getMessageTypeString(message.getMessageType());
                }
                if (fileUrl == null) {
                    fileUrl = message.getFileUrl();
                }
                if (senderId == null) {
                    senderId = message.getSenderId();
                }
                if (sessionId == null) {
                    sessionId = message.getSessionId();
                }
            }
        }

        // 获取发送者信息
        String senderName = null;
        String senderAvatar = null;
        if (senderId != null) {
            ChatUser sender = chatUserMapper.selectById(senderId);
            if (sender != null) {
                senderName = sender.getNickname();
                senderAvatar = minioUtil.getAvatarUrl(sender.getAvatar());
            }
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setMessageId(messageId);
        favorite.setContent(content);
        favorite.setMessageType(messageType != null ? messageType : "TEXT");
        favorite.setFileUrl(fileUrl);
        favorite.setSenderId(senderId);
        favorite.setSenderName(senderName);
        favorite.setSenderAvatar(senderAvatar);
        favorite.setSessionId(sessionId);

        this.save(favorite);
        return favorite;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeFavorite(Long favoriteId, String userId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getId, favoriteId)
               .eq(Favorite::getUserId, userId);
        return this.remove(wrapper);
    }

    @Override
    public Page<Favorite> getFavoriteList(String userId, int current, int size) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
               .orderByDesc(Favorite::getCreateTime);

        Page<Favorite> page = new Page<>(current, size);
        Page<Favorite> result = this.page(page, wrapper);

        // 补充发送者信息
        enrichSenderInfo(result.getRecords());

        return result;
    }

    @Override
    public Page<Favorite> searchFavorites(String userId, String keyword, String messageType, int current, int size) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
               .like(keyword != null && !keyword.isEmpty(), Favorite::getContent, keyword)
               .eq(messageType != null && !messageType.isEmpty() && !"all".equalsIgnoreCase(messageType), Favorite::getMessageType, messageType)
               .orderByDesc(Favorite::getCreateTime);

        Page<Favorite> page = new Page<>(current, size);
        Page<Favorite> result = this.page(page, wrapper);

        // 补充发送者信息
        enrichSenderInfo(result.getRecords());

        return result;
    }

    @Override
    public boolean isFavorited(String userId, Long messageId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
               .eq(Favorite::getMessageId, messageId);
        return this.count(wrapper) > 0;
    }

    /**
     * 补充发送者信息
     */
    private void enrichSenderInfo(java.util.List<Favorite> favorites) {
        if (favorites == null || favorites.isEmpty()) return;

        // 收集所有发送者ID
        Set<String> senderIds = favorites.stream()
                .map(Favorite::getSenderId)
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());

        if (senderIds.isEmpty()) return;

        // 批量查询发送者信息
        java.util.Map<String, ChatUser> senderMap = chatUserMapper.selectList(
                new LambdaQueryWrapper<ChatUser>()
                        .in(ChatUser::getId, senderIds)
                        .select(ChatUser::getId, ChatUser::getNickname, ChatUser::getAvatar)
        ).stream()
                .collect(Collectors.toMap(ChatUser::getId, u -> u));

        // 填充发送者信息
        for (Favorite favorite : favorites) {
            if (favorite.getSenderId() != null) {
                ChatUser sender = senderMap.get(favorite.getSenderId());
                if (sender != null) {
                    if (favorite.getSenderName() == null) {
                        favorite.setSenderName(sender.getNickname());
                    }
                    if (favorite.getSenderAvatar() == null) {
                        favorite.setSenderAvatar(minioUtil.getAvatarUrl(sender.getAvatar()));
                    }
                }
            }
        }
    }

    /**
     * 将消息类型整数转换为字符串
     */
    private String getMessageTypeString(Integer messageType) {
        if (messageType == null) return "TEXT";
        return switch (messageType) {
            case 1 -> "TEXT";
            case 2 -> "IMAGE";
            case 3 -> "FILE";
            case 4 -> "VOICE";
            case 5 -> "VIDEO";
            default -> "TEXT";
        };
    }
}
