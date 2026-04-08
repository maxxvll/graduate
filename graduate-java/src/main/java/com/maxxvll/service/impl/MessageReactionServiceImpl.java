package com.maxxvll.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.common.vo.MessageReactionVO;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.domain.MessageReaction;
import com.maxxvll.mapper.ChatMessageMapper;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.mapper.MessageReactionMapper;
import com.maxxvll.service.MessageReactionService;
import com.maxxvll.utils.MinioUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息反应服务实现
 *
 * @author 20570
 */
@Slf4j
@Service
public class MessageReactionServiceImpl implements MessageReactionService {

    @jakarta.annotation.Resource
    private MessageReactionMapper messageReactionMapper;
    @jakarta.annotation.Resource
    private ChatMessageMapper chatMessageMapper;
    @jakarta.annotation.Resource
    private ChatUserMapper chatUserMapper;
    @jakarta.annotation.Resource
    private MinioUtil minioUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageReaction addReaction(Long messageId, Long userId, String emoji) {
        // 验证消息存在
        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException("消息不存在");
        }

        // 验证表情不为空
        if (StrUtil.isBlank(emoji)) {
            throw new BusinessException("表情不能为空");
        }

        // 检查是否已添加该表情
        if (messageReactionMapper.countByMessageIdAndUserIdAndEmoji(messageId, userId, emoji) > 0) {
            throw new BusinessException("已添加该表情");
        }

        // 创建反应记录
        MessageReaction reaction = new MessageReaction();
        reaction.setMessageId(messageId);
        reaction.setUserId(userId);
        reaction.setEmoji(emoji);
        reaction.setCreateTime(new Date());

        messageReactionMapper.insert(reaction);

        log.info("Message reaction added, messageId={}, userId={}, emoji={}",
                messageId, userId, emoji);

        return reaction;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeReaction(Long messageId, Long userId, String emoji) {
        // 验证消息存在
        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException("消息不存在");
        }

        // 查找反应记录
        List<MessageReaction> reactions = messageReactionMapper.selectByMessageIdAndUserId(messageId, userId);
        Optional<MessageReaction> targetReaction = reactions.stream()
                .filter(r -> emoji.equals(r.getEmoji()))
                .findFirst();

        if (targetReaction.isEmpty()) {
            throw new BusinessException("未添加该表情");
        }

        // 删除反应
        messageReactionMapper.deleteById(targetReaction.get().getId());

        log.info("Message reaction removed, messageId={}, userId={}, emoji={}",
                messageId, userId, emoji);
    }

    @Override
    public List<MessageReactionVO> getReactionsByMessageId(Long messageId) {
        List<MessageReaction> reactions = messageReactionMapper.selectByMessageId(messageId);
        return buildReactionVOList(reactions, null);
    }

    @Override
    public Map<Long, List<MessageReactionVO>> getReactionsByMessageIds(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Map.of();
        }

        // 批量查询所有反应
        String idsStr = messageIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        List<MessageReaction> allReactions = messageReactionMapper.selectByMessageIds(idsStr);

        // 按消息ID分组
        Map<Long, List<MessageReaction>> reactionsByMessage = allReactions.stream()
                .collect(Collectors.groupingBy(MessageReaction::getMessageId));

        // 构建返回结果
        Map<Long, List<MessageReactionVO>> result = new HashMap<>();
        for (Long messageId : messageIds) {
            List<MessageReaction> messageReactions = reactionsByMessage.getOrDefault(messageId, Collections.emptyList());
            result.put(messageId, buildReactionVOList(messageReactions, null));
        }

        return result;
    }

    /**
     * 构建反应VO列表
     *
     * @param reactions       反应列表
     * @param currentUserId 当前用户ID（用于判断是否已添加该表情）
     * @return 反应VO列表
     */
    private List<MessageReactionVO> buildReactionVOList(List<MessageReaction> reactions, Long currentUserId) {
        if (reactions == null || reactions.isEmpty()) {
            return List.of();
        }

        // 按表情分组
        Map<String, List<MessageReaction>> reactionsByEmoji = reactions.stream()
                .collect(Collectors.groupingBy(MessageReaction::getEmoji));

        // 获取所有用户ID
        Set<String> userIds = reactions.stream()
                .map(r -> String.valueOf(r.getUserId()))
                .collect(Collectors.toSet());

        // 批量加载用户信息
        Map<String, ChatUser> userMap = loadUsers(userIds);

        // 构建返回结果
        List<MessageReactionVO> result = new ArrayList<>();
        for (Map.Entry<String, List<MessageReaction>> entry : reactionsByEmoji.entrySet()) {
            String emoji = entry.getKey();
            List<MessageReaction> emojiReactions = entry.getValue();

            MessageReactionVO vo = new MessageReactionVO();
            vo.setEmoji(emoji);
            vo.setCount(emojiReactions.size());

            // 判断当前用户是否已添加该表情
            if (currentUserId != null) {
                boolean isCurrentUserReacted = emojiReactions.stream()
                        .anyMatch(r -> r.getUserId().equals(currentUserId));
                vo.setIsCurrentUserReacted(isCurrentUserReacted);
            }

            // 构建用户列表
            List<MessageReactionVO.ReactionUserVO> users = new ArrayList<>();
            for (MessageReaction reaction : emojiReactions) {
                ChatUser user = userMap.get(String.valueOf(reaction.getUserId()));
                if (user == null) continue;

                MessageReactionVO.ReactionUserVO userVO = MessageReactionVO.ReactionUserVO.builder()
                        .id(String.valueOf(user.getId()))
                        .name(StrUtil.isNotBlank(user.getNickname()) ? user.getNickname() : user.getUsername())
                        .avatar(buildAvatarUrl(user.getAvatar()))
                        .build();
                users.add(userVO);
            }
            vo.setUsers(users);

            result.add(vo);
        }

        return result;
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
}
