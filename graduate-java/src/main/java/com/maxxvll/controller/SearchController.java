package com.maxxvll.controller;

import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.dto.SearchDTO;
import com.maxxvll.common.vo.SearchHistoryVO;
import com.maxxvll.common.vo.SearchResultVO;
import com.maxxvll.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 搜索控制器
 * <p>
 * 提供统一的搜索接口，支持消息、联系人、群组搜索
 * </p>
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Slf4j
@RestController
@RequestMapping("/search")
@Tag(name = "搜索", description = "统一搜索相关接口")
public class SearchController extends BaseController {

    @Resource
    private SearchService searchService;

    // ==================== 搜索 ====================

    /**
     * 统一搜索（消息、联系人、群组）
     */
    @PostMapping
    @Operation(summary = "统一搜索", description = "搜索消息、联系人和群组")
    public Result<SearchResultVO> search(@RequestBody @Valid SearchDTO searchDTO) {
        SearchResultVO result = searchService.search(getCurrentUserId(), searchDTO);
        return success(result);
    }

    /**
     * 搜索消息
     */
    @GetMapping("/messages")
    @Operation(summary = "搜索消息", description = "搜索聊天记录，支持关键词、时间范围和类型筛选")
    public Result<List<SearchResultVO.MessageSearchResult>> searchMessages(
            @RequestParam String keyword,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {
        List<SearchResultVO.MessageSearchResult> results =
                searchService.searchMessages(getCurrentUserId(), keyword, sessionId, messageType, startTime, endTime);
        return success(results);
    }

    /**
     * 搜索联系人
     */
    @GetMapping("/contacts")
    @Operation(summary = "搜索联系人", description = "搜索好友列表，支持用户名和备注")
    public Result<List<SearchResultVO.ContactSearchResult>> searchContacts(@RequestParam String keyword) {
        List<SearchResultVO.ContactSearchResult> results =
                searchService.searchContacts(getCurrentUserId(), keyword);
        return success(results);
    }

    /**
     * 搜索群组
     */
    @GetMapping("/groups")
    @Operation(summary = "搜索群组", description = "搜索群组列表")
    public Result<List<SearchResultVO.GroupSearchResult>> searchGroups(@RequestParam String keyword) {
        List<SearchResultVO.GroupSearchResult> results =
                searchService.searchGroups(getCurrentUserId(), keyword);
        return success(results);
    }

    // ==================== 搜索历史 ====================

    /**
     * 获取搜索历史
     */
    @GetMapping("/history")
    @Operation(summary = "获取搜索历史", description = "获取用户的搜索历史记录")
    public Result<List<SearchHistoryVO>> getSearchHistory(
            @RequestParam(defaultValue = "20") int limit) {
        limit = normalizeLimit(limit);
        List<SearchHistoryVO> history = searchService.getSearchHistory(getCurrentUserId(), limit);
        return success(history);
    }

    /**
     * 删除单条搜索历史
     */
    @DeleteMapping("/history/{historyId}")
    @Operation(summary = "删除搜索历史", description = "删除指定的搜索历史记录")
    public Result<Void> deleteSearchHistory(@PathVariable Long historyId) {
        searchService.deleteSearchHistory(getCurrentUserId(), historyId);
        return success("删除成功");
    }

    /**
     * 清空搜索历史
     */
    @DeleteMapping("/history")
    @Operation(summary = "清空搜索历史", description = "清空用户的所有搜索历史")
    public Result<Void> clearSearchHistory() {
        searchService.clearSearchHistory(getCurrentUserId());
        return success("清空成功");
    }
}
