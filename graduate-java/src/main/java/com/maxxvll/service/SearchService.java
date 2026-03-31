package com.maxxvll.service;

import com.maxxvll.common.dto.SearchDTO;
import com.maxxvll.common.vo.SearchHistoryVO;
import com.maxxvll.common.vo.SearchResultVO;

import java.util.List;

/**
 * 搜索服务接口
 *
 * @author Claude Code
 * @since 2026-03-31
 */
public interface SearchService {

    // ==================== 统一搜索 ====================

    /**
     * 统一搜索（消息、联系人、群组）
     */
    SearchResultVO search(String userId, SearchDTO searchDTO);

    // ==================== 分类型搜索 ====================

    /**
     * 搜索消息
     */
    List<SearchResultVO.MessageSearchResult> searchMessages(String userId, String keyword, String sessionId,
                                                           String messageType, Long startTime, Long endTime);

    /**
     * 搜索联系人
     */
    List<SearchResultVO.ContactSearchResult> searchContacts(String userId, String keyword);

    /**
     * 搜索群组
     */
    List<SearchResultVO.GroupSearchResult> searchGroups(String userId, String keyword);

    // ==================== 搜索历史 ====================

    /**
     * 获取用户搜索历史
     */
    List<SearchHistoryVO> getSearchHistory(String userId, int limit);

    /**
     * 添加搜索历史
     */
    void addSearchHistory(String userId, String keyword, String searchType);

    /**
     * 删除单条搜索历史
     */
    void deleteSearchHistory(String userId, Long historyId);

    /**
     * 清空用户搜索历史
     */
    void clearSearchHistory(String userId);

    // ==================== 辅助功能 ====================

    /**
     * 高亮关键词
     */
    String highlightKeyword(String text, String keyword);

    /**
     * 检查是否启用高亮
     */
    boolean isHighlightEnabled();
}
