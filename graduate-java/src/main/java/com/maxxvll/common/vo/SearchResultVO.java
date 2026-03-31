package com.maxxvll.common.vo;

import lombok.Data;

import java.util.List;

/**
 * 搜索结果VO
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
public class SearchResultVO {

    /**
     * 消息搜索结果
     */
    private List<MessageSearchResult> messages;

    /**
     * 联系人搜索结果
     */
    private List<ContactSearchResult> contacts;

    /**
     * 群组搜索结果
     */
    private List<GroupSearchResult> groups;

    /**
     * 是否启用高亮
     */
    private Boolean highlightEnabled;

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 总结果数
     */
    private Integer totalCount;

    // ==================== 内部类：消息搜索结果 ====================

    @Data
    public static class MessageSearchResult {
        private Long messageId;
        private String sessionId;
        private String sessionName;
        private String senderId;
        private String senderName;
        private String senderAvatar;
        private String content;
        private String highlightedContent;
        private String messageType;
        private Long sendTime;
        private String timeDesc;
    }

    // ==================== 内部类：联系人搜索结果 ====================

    @Data
    public static class ContactSearchResult {
        private String userId;
        private String username;
        private String nickname;
        private String remarkName;
        private String avatar;
        private String highlightedNickname;
        private String highlightedRemarkName;
    }

    // ==================== 内部类：群组搜索结果 ====================

    @Data
    public static class GroupSearchResult {
        private Long groupId;
        private String groupName;
        private String groupAvatar;
        private String highlightedGroupName;
        private Integer memberCount;
        private Boolean isMember;
    }
}
