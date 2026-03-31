package com.maxxvll.controller;

import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.dto.MarkMessagesReadDTO;
import com.maxxvll.common.vo.ReadSyncStatusVO;
import com.maxxvll.service.ReadSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 已读同步控制器
 * <p>
 * 提供消息已读同步相关的REST接口
 * </p>
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Slf4j
@RestController
@RequestMapping("/read-sync")
@Tag(name = "已读同步", description = "消息已读同步相关接口")
public class ReadSyncController extends BaseController {

    @Resource
    private ReadSyncService readSyncService;

    // ==================== 已读操作 ====================

    /**
     * 标记消息已读
     */
    @PostMapping("/mark-read")
    @Operation(summary = "标记消息已读", description = "标记指定会话的消息为已读状态")
    public Result<Void> markAsRead(@RequestBody @Valid MarkMessagesReadDTO dto) {
        readSyncService.markAsRead(getCurrentUserId(), dto);
        return success("标记成功");
    }

    /**
     * 标记会话全部已读
     */
    @PostMapping("/mark-session-read/{sessionId}")
    @Operation(summary = "标记会话已读", description = "标记指定会话的所有消息为已读")
    public Result<Void> markSessionAsRead(
            @PathVariable String sessionId,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String deviceId) {
        readSyncService.markSessionAsRead(getCurrentUserId(), sessionId, deviceType, deviceId);
        return success("标记成功");
    }

    /**
     * 标记所有会话已读
     */
    @PostMapping("/mark-all-read")
    @Operation(summary = "全部已读", description = "标记所有会话的消息为已读")
    public Result<Void> markAllAsRead() {
        readSyncService.markAllAsRead(getCurrentUserId());
        return success("标记成功");
    }

    // ==================== 状态查询 ====================

    /**
     * 获取会话已读状态
     */
    @GetMapping("/session-status/{sessionId}")
    @Operation(summary = "会话已读状态", description = "获取指定会话的已读同步状态")
    public Result<ReadSyncStatusVO> getSessionReadStatus(@PathVariable String sessionId) {
        ReadSyncStatusVO status = readSyncService.getSessionReadStatus(getCurrentUserId(), sessionId);
        return success(status);
    }

    /**
     * 获取所有会话未读数
     */
    @GetMapping("/all-status")
    @Operation(summary = "所有会话状态", description = "获取用户所有会话的未读数统计")
    public Result<List<ReadSyncStatusVO>> getAllSessionReadStatus() {
        List<ReadSyncStatusVO> statusList = readSyncService.getAllSessionReadStatus(getCurrentUserId());
        return success(statusList);
    }

    /**
     * 获取总未读数
     */
    @GetMapping("/total-unread")
    @Operation(summary = "总未读数", description = "获取用户所有会话的未读消息总数")
    public Result<Integer> getTotalUnreadCount() {
        Integer total = readSyncService.getTotalUnreadCount(getCurrentUserId());
        return success(total);
    }

    // ==================== 离线同步 ====================

    /**
     * 获取离线未读消息
     */
    @GetMapping("/offline-unread")
    @Operation(summary = "离线未读消息", description = "获取离线期间的未读消息ID列表")
    public Result<List<Long>> getOfflineUnreadMessageIds(@RequestParam(required = false) Long lastSyncTime) {
        List<Long> messageIds = readSyncService.getOfflineUnreadMessageIds(getCurrentUserId(), lastSyncTime);
        return success(messageIds);
    }

    /**
     * 获取需要同步的消息
     */
    @GetMapping("/unsynced-messages")
    @Operation(summary = "未同步消息", description = "获取指定会话中需要同步的消息ID列表")
    public Result<List<Long>> getUnsyncedMessages(
            @RequestParam String sessionId,
            @RequestParam Long fromMessageId) {
        List<Long> messageIds = readSyncService.getUnsyncedMessageIds(getCurrentUserId(), sessionId, fromMessageId);
        return success(messageIds);
    }
}
