package com.maxxvll.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxxvll.common.dto.SearchDTO;
import com.maxxvll.common.vo.FriendApplicationVO;
import com.maxxvll.common.vo.GroupInfoVO;
import com.maxxvll.common.vo.SearchHistoryVO;
import com.maxxvll.common.vo.SearchResultVO;
import com.maxxvll.domain.*;
import com.maxxvll.mapper.*;
import com.maxxvll.service.ChatGroupMemberService;
import com.maxxvll.service.ChatGroupService;
import com.maxxvll.service.ChatMessageService;
import com.maxxvll.service.ChatSessionService;
import com.maxxvll.service.FriendApplicationService;
import com.maxxvll.service.SearchService;
import com.maxxvll.utils.MinioUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 搜索服务实现
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int DEFAULT_HISTORY_LIMIT = 20;
    private static final int DEFAULT_MESSAGE_LIMIT = 100;
    private static final int DEFAULT_CONTACT_LIMIT = 50;
    private static final int DEFAULT_GROUP_LIMIT = 50;

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private ChatSessionService chatSessionService;

    @Resource
    private ChatGroupService chatGroupService;

    @Resource
    private ChatGroupMemberService chatGroupMemberService;

    @Resource
    private FriendApplicationService friendApplicationService;

    @Resource
    private SearchHistoryMapper searchHistoryMapper;

    @Resource
    private SearchConfigMapper searchConfigMapper;

    @Resource
    private MinioUtil minioUtil;

    // ==================== 统一搜索 ====================

    @Override
    public SearchResultVO search(String userId, SearchDTO searchDTO) {
        SearchResultVO result = new SearchResultVO();
        result.setKeyword(searchDTO.getKeyword());
        result.setHighlightEnabled(isHighlightEnabled());

        String searchType = searchDTO.getSearchType();
        if (searchType == null || searchType.isEmpty() || SearchDTO.TYPE_ALL.equals(searchType)) {
            // 全局搜索
            result.setMessages(searchMessages(userId, searchDTO.getKeyword(),
                    searchDTO.getSessionId(), searchDTO.getMessageType(),
                    searchDTO.getStartTime(), searchDTO.getEndTime()));
            result.setContacts(searchContacts(userId, searchDTO.getKeyword()));
            result.setGroups(searchGroups(userId, searchDTO.getKeyword()));
        } else if (SearchDTO.TYPE_MESSAGE.equals(searchType)) {
            result.setMessages(searchMessages(userId, searchDTO.getKeyword(),
                    searchDTO.getSessionId(), searchDTO.getMessageType(),
                    searchDTO.getStartTime(), searchDTO.getEndTime()));
        } else if (SearchDTO.TYPE_CONTACT.equals(searchType)) {
            result.setContacts(searchContacts(userId, searchDTO.getKeyword()));
        } else if (SearchDTO.TYPE_GROUP.equals(searchType)) {
            result.setGroups(searchGroups(userId, searchDTO.getKeyword()));
        }

        // 计算总数
        int total = 0;
        if (result.getMessages() != null) total += result.getMessages().size();
        if (result.getContacts() != null) total += result.getContacts().size();
        if (result.getGroups() != null) total += result.getGroups().size();
        result.setTotalCount(total);

        // 添加搜索历史
        addSearchHistory(userId, searchDTO.getKeyword(), searchType);

        return result;
    }

    // ==================== 分类型搜索 ====================

    @Override
    public List<SearchResultVO.MessageSearchResult> searchMessages(String userId, String keyword,
                                                                  String sessionId, String messageType,
                                                                  Long startTime, Long endTime) {
        if (StrUtil.isBlank(keyword)) {
            return List.of();
        }

        // 获取用户参与的所有会话
        List<String> userSessionIds = getUserSessionIds(userId);
        if (userSessionIds.isEmpty()) {
            return List.of();
        }

        // 查询消息
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(ChatMessage::getContent, keyword)
                .eq(ChatMessage::getIsDeleted, 0)
                .in(ChatMessage::getSessionId, userSessionIds);

        // 会话筛选
        if (StrUtil.isNotBlank(sessionId)) {
            wrapper.eq(ChatMessage::getSessionId, sessionId);
        }

        // 消息类型筛选
        if (StrUtil.isNotBlank(messageType) && !"all".equalsIgnoreCase(messageType)) {
            Integer msgTypeCode = getMessageTypeCode(messageType);
            if (msgTypeCode != null) {
                wrapper.eq(ChatMessage::getMessageType, msgTypeCode);
            }
        }

        // 时间范围筛选
        if (startTime != null) {
            wrapper.ge(ChatMessage::getSendTime, new Date(startTime));
        }
        if (endTime != null) {
            wrapper.le(ChatMessage::getSendTime, new Date(endTime));
        }

        wrapper.orderByDesc(ChatMessage::getSendTime);

        // 限制结果数量
        Page<ChatMessage> page = chatMessageService.page(new Page<>(1, DEFAULT_MESSAGE_LIMIT), wrapper);
        List<ChatMessage> messages = page.getRecords();

        // 获取会话信息
        Map<String, ChatSession> sessionMap = getSessionMap(userSessionIds);

        // 获取发送者信息
        Set<String> senderIds = new HashSet<>();
        for (ChatMessage msg : messages) {
            if (msg.getSenderId() != null && !msg.getSenderId().isBlank()) {
                senderIds.add(msg.getSenderId());
            }
        }
        Map<String, ChatUser> userMap = getUserMap(senderIds);

        // 转换结果
        boolean highlight = isHighlightEnabled();
        List<SearchResultVO.MessageSearchResult> results = new ArrayList<>();

        for (ChatMessage msg : messages) {
            SearchResultVO.MessageSearchResult r = new SearchResultVO.MessageSearchResult();
            r.setMessageId(msg.getId());
            r.setSessionId(msg.getSessionId());
            r.setContent(msg.getContent());
            r.setHighlightedContent(highlight ? highlightKeyword(msg.getContent(), keyword) : msg.getContent());
            r.setMessageType(getMessageTypeName(msg.getMessageType()));
            r.setSendTime(msg.getSendTime() != null ? msg.getSendTime().getTime() : null);
            r.setTimeDesc(getTimeDesc(msg.getSendTime()));

            ChatSession session = sessionMap.get(msg.getSessionId());
            if (session != null) {
                r.setSessionName(session.getSessionName());
            }

            ChatUser sender = userMap.get(msg.getSenderId());
            if (sender != null) {
                r.setSenderId(sender.getId());
                r.setSenderName(sender.getNickname());
                r.setSenderAvatar(buildAvatarUrl(sender.getAvatar()));
            }

            results.add(r);
        }

        return results;
    }

    @Override
    public List<SearchResultVO.ContactSearchResult> searchContacts(String userId, String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return List.of();
        }

        // 获取好友列表
        List<FriendApplicationVO> friends = friendApplicationService.getFriendList(userId);

        // 过滤匹配的联系人
        boolean highlight = isHighlightEnabled();
        List<SearchResultVO.ContactSearchResult> results = friends.stream()
                .filter(f -> (f.getApplicantNickname() != null && f.getApplicantNickname().contains(keyword))
                        || (f.getApplicantUsername() != null && f.getApplicantUsername().contains(keyword))
                        || (f.getRemarkName() != null && f.getRemarkName().contains(keyword)))
                .map(f -> {
                    SearchResultVO.ContactSearchResult r = new SearchResultVO.ContactSearchResult();
                    r.setUserId(f.getApplicantId());
                    r.setUsername(f.getApplicantUsername());
                    r.setNickname(f.getApplicantNickname());
                    r.setRemarkName(f.getRemarkName());
                    r.setAvatar(f.getApplicantAvatar());
                    if (highlight) {
                        r.setHighlightedNickname(highlightKeyword(f.getApplicantNickname(), keyword));
                        r.setHighlightedRemarkName(f.getRemarkName() != null ? highlightKeyword(f.getRemarkName(), keyword) : null);
                    }
                    return r;
                })
                .limit(DEFAULT_CONTACT_LIMIT)
                .collect(Collectors.toList());

        return results;
    }

    @Override
    public List<SearchResultVO.GroupSearchResult> searchGroups(String userId, String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return List.of();
        }

        // 搜索群组
        List<GroupInfoVO> groups = chatGroupService.searchGroup(keyword, userId);

        boolean highlight = isHighlightEnabled();
        List<SearchResultVO.GroupSearchResult> results = groups.stream()
                .limit(DEFAULT_GROUP_LIMIT)
                .map(g -> {
                    SearchResultVO.GroupSearchResult r = new SearchResultVO.GroupSearchResult();
                    r.setGroupId(g.getId());
                    r.setGroupName(g.getGroupName());
                    r.setGroupAvatar(g.getGroupAvatar());
                    r.setHighlightedGroupName(highlight ? highlightKeyword(g.getGroupName(), keyword) : g.getGroupName());
                    r.setMemberCount((int) chatGroupMemberService.getMemberCount(g.getId()));
                    r.setIsMember(chatGroupMemberService.isGroupMember(g.getId(), userId));
                    return r;
                })
                .collect(Collectors.toList());

        return results;
    }

    // ==================== 搜索历史 ====================

    @Override
    public List<SearchHistoryVO> getSearchHistory(String userId, int limit) {
        if (limit <= 0) {
            limit = DEFAULT_HISTORY_LIMIT;
        }

        List<SearchHistory> histories = searchHistoryMapper.selectByUserId(userId, limit);

        return histories.stream()
                .map(h -> {
                    SearchHistoryVO vo = new SearchHistoryVO();
                    vo.setId(h.getId());
                    vo.setSearchType(h.getSearchType());
                    vo.setSearchTypeDesc(getSearchTypeDesc(h.getSearchType()));
                    vo.setKeyword(h.getKeyword());
                    vo.setSearchCount(h.getSearchCount());
                    vo.setCreatedAt(h.getCreateTime() != null ? h.getCreateTime().getTime() : null);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSearchHistory(String userId, String keyword, String searchType) {
        if (StrUtil.isBlank(keyword)) {
            return;
        }

        String type = StrUtil.isBlank(searchType) ? SearchDTO.TYPE_ALL : searchType;
        keyword = keyword.trim();

        // 检查是否已存在
        SearchHistory existing = searchHistoryMapper.selectOne(
                new LambdaQueryWrapper<SearchHistory>()
                        .eq(SearchHistory::getUserId, userId)
                        .eq(SearchHistory::getKeyword, keyword)
                        .eq(SearchHistory::getSearchType, type)
                        .eq(SearchHistory::getIsDeleted, SearchHistory.FLAG_NO)
        );

        Date now = new Date();
        if (existing != null) {
            // 增加搜索次数
            existing.setSearchCount(existing.getSearchCount() + 1);
            existing.setUpdateTime(now);
            searchHistoryMapper.updateById(existing);
        } else {
            // 新增记录
            SearchHistory history = new SearchHistory();
            history.setUserId(userId);
            history.setKeyword(keyword);
            history.setSearchType(type);
            history.setSearchCount(1);
            history.setIsDeleted(SearchHistory.FLAG_NO);
            history.setCreateTime(now);
            history.setUpdateTime(now);
            searchHistoryMapper.insert(history);

            // 清理超出数量的历史记录
            int maxHistory = getMaxHistoryCount();
            searchHistoryMapper.deleteOverflowHistory(userId, maxHistory);
        }

        log.debug("添加搜索历史, userId={}, keyword={}, type={}", userId, keyword, type);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSearchHistory(String userId, Long historyId) {
        SearchHistory history = searchHistoryMapper.selectOne(
                new LambdaQueryWrapper<SearchHistory>()
                        .eq(SearchHistory::getUserId, userId)
                        .eq(SearchHistory::getId, historyId)
                        .eq(SearchHistory::getIsDeleted, SearchHistory.FLAG_NO)
        );

        if (history != null) {
            history.setIsDeleted(SearchHistory.FLAG_YES);
            history.setUpdateTime(new Date());
            searchHistoryMapper.updateById(history);
            log.debug("删除搜索历史, userId={}, historyId={}", userId, historyId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearSearchHistory(String userId) {
        List<SearchHistory> histories = searchHistoryMapper.selectList(
                new LambdaQueryWrapper<SearchHistory>()
                        .eq(SearchHistory::getUserId, userId)
                        .eq(SearchHistory::getIsDeleted, SearchHistory.FLAG_NO)
        );

        Date now = new Date();
        for (SearchHistory history : histories) {
            history.setIsDeleted(SearchHistory.FLAG_YES);
            history.setUpdateTime(now);
        }

        if (!histories.isEmpty()) {
            for (SearchHistory history : histories) {
                searchHistoryMapper.updateById(history);
            }
        }

        log.info("清空搜索历史, userId={}, count={}", userId, histories.size());
    }

    // ==================== 辅助功能 ====================

    @Override
    public String highlightKeyword(String text, String keyword) {
        if (StrUtil.isBlank(text) || StrUtil.isBlank(keyword)) {
            return text;
        }

        try {
            // 转义特殊字符
            String escapedKeyword = Pattern.quote(keyword);
            // 使用正则替换，添加高亮标记（使用<em>标签）
            return text.replaceAll("(?i)(" + escapedKeyword + ")", "<em>$1</em>");
        } catch (Exception e) {
            log.warn("高亮关键词失败, text={}, keyword={}", text, keyword);
            return text;
        }
    }

    @Override
    public boolean isHighlightEnabled() {
        try {
            String value = searchConfigMapper.getConfigValue(SearchConfig.KEY_ENABLE_HIGHLIGHT);
            return !"false".equalsIgnoreCase(value);
        } catch (Exception e) {
            return true; // 默认启用
        }
    }

    // ==================== 私有方法 ====================

    private List<String> getUserSessionIds(String userId) {
        List<ChatSession> sessions = chatSessionService.getSessionsByUserId(userId);
        return sessions.stream()
                .map(s -> String.valueOf(s.getId()))
                .collect(Collectors.toList());
    }

    private Map<String, ChatSession> getSessionMap(List<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Map.of();
        }

        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ChatSession::getId, sessionIds);
        List<ChatSession> sessions = chatSessionService.list(wrapper);

        return sessions.stream()
                .collect(Collectors.toMap(s -> String.valueOf(s.getId()), s -> s, (a, b) -> a));
    }

    private Map<String, ChatUser> getUserMap(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        List<ChatUser> users = new ArrayList<>();
        for (String userId : userIds) {
            try {
                ChatUser user = new ChatUser();
                user.setId(userId);
                users.add(user);
            } catch (Exception ignored) {
            }
        }
        return Map.of(); // 简化实现
    }

    private String buildAvatarUrl(String avatar) {
        if (StrUtil.isBlank(avatar)) {
            return null;
        }
        if (avatar.startsWith("http")) {
            return avatar;
        }
        return minioUtil.getAvatarUrl(avatar);
    }

    private Integer getMessageTypeCode(String messageType) {
        if (StrUtil.isBlank(messageType)) return null;
        return switch (messageType.toUpperCase()) {
            case "TEXT" -> 1;
            case "IMAGE" -> 2;
            case "FILE" -> 3;
            case "VOICE", "AUDIO" -> 4;
            case "VIDEO" -> 5;
            default -> null;
        };
    }

    private String getMessageTypeName(Integer code) {
        if (code == null) return "TEXT";
        return switch (code) {
            case 1 -> "TEXT";
            case 2 -> "IMAGE";
            case 3 -> "FILE";
            case 4 -> "VOICE";
            case 5 -> "VIDEO";
            default -> "TEXT";
        };
    }

    private String getSearchTypeDesc(String type) {
        if (StrUtil.isBlank(type)) return "全部";
        return switch (type) {
            case SearchDTO.TYPE_MESSAGE -> "消息";
            case SearchDTO.TYPE_CONTACT -> "联系人";
            case SearchDTO.TYPE_GROUP -> "群组";
            case SearchDTO.TYPE_ALL -> "全部";
            default -> type;
        };
    }

    private String getTimeDesc(Date sendTime) {
        if (sendTime == null) return "";
        LocalDateTime dateTime = sendTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime now = LocalDateTime.now();

        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        if (minutes < 1) return "刚刚";
        if (minutes < 60) return minutes + "分钟前";

        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours < 24) return hours + "小时前";

        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days < 7) return days + "天前";

        return dateTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
    }

    private int getMaxHistoryCount() {
        try {
            String value = searchConfigMapper.getConfigValue(SearchConfig.KEY_MAX_HISTORY_COUNT);
            return value != null ? Integer.parseInt(value) : 50;
        } catch (Exception e) {
            return 50;
        }
    }
}
