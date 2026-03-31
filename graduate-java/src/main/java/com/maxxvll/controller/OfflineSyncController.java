package com.maxxvll.controller;

import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.vo.ChatMessageVO;
import com.maxxvll.domain.ChatOfflineCursor;
import com.maxxvll.service.ChatOfflineCursorService;
import com.maxxvll.utils.UserContextUtil;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 离线消息同步控制器
 * 提供基于游标的增量消息同步功能
 *
 * @author maxxvll
 * @since 2026-03-31
 */
@Slf4j
@RestController
@RequestMapping("/offline-sync")
public class OfflineSyncController extends BaseController {

    @Resource
    private ChatOfflineCursorService chatOfflineCursorService;

    /**
     * 获取当前用户的离线游标状态
     */
    @GetMapping("/cursor")
    public Result<ChatOfflineCursor> getCursor() {
        String userId = UserContextUtil.getCurrentUserId();
        ChatOfflineCursor cursor = chatOfflineCursorService.getCursor(userId);
        return Result.success(cursor);
    }

    /**
     * 更新离线游标位置
     */
    @PutMapping("/cursor")
    public Result<Void> updateCursor(
            @RequestParam Long lastMessageId,
            @RequestParam Long lastMessageTime) {
        String userId = UserContextUtil.getCurrentUserId();
        chatOfflineCursorService.updateCursor(userId, lastMessageId, new java.util.Date(lastMessageTime));
        return Result.success("游标更新成功");
    }

    /**
     * 重置离线游标（清除所有离线消息）
     */
    @DeleteMapping("/cursor")
    public Result<Void> resetCursor() {
        String userId = UserContextUtil.getCurrentUserId();
        chatOfflineCursorService.resetCursor(userId);
        return Result.success("游标已重置");
    }

    /**
     * 获取增量离线消息（基于游标的增量同步）
     *
     * @param limit 最大返回消息数量（默认50，最大500）
     * @return 增量消息列表
     */
    @GetMapping("/messages/incremental")
    public Result<List<ChatMessageVO>> getIncrementalMessages(
            @RequestParam(defaultValue = "50") @Min(1) @Max(500) int limit) {
        String userId = UserContextUtil.getCurrentUserId();
        List<ChatMessageVO> messages = chatOfflineCursorService.getIncrementalMessages(userId, limit);

        // 如果有消息，更新游标到最新消息
        if (messages != null && !messages.isEmpty()) {
            ChatMessageVO latestMessage = messages.get(messages.size() - 1);
            java.util.Date lastMessageTime = null;
            if (latestMessage.getSendTime() != null) {
                lastMessageTime = java.util.Date.from(latestMessage.getSendTime().atZone(java.time.ZoneId.systemDefault()).toInstant());
            }
            chatOfflineCursorService.updateCursor(
                    userId,
                    latestMessage.getId(),
                    lastMessageTime != null ? lastMessageTime : new java.util.Date()
            );
        }

        return Result.success(messages);
    }

    /**
     * 获取离线消息（完整同步）
     *
     * @param afterTimestamp 可选，起始时间戳（毫秒），用于增量拉取离线消息
     * @return 离线消息列表
     */
    @GetMapping("/messages/offline")
    public Result<List<ChatMessageVO>> getOfflineMessages(
            @RequestParam(required = false) Long afterTimestamp) {
        String userId = UserContextUtil.getCurrentUserId();
        List<ChatMessageVO> messages = chatOfflineCursorService.getOfflineMessages(userId, afterTimestamp);
        return Result.success(messages);
    }

    /**
     * 标记消息已拉取
     *
     * @param messageIds 已拉取的消息ID列表
     * @return 操作结果
     */
    @PostMapping("/messages/ack")
    public Result<Void> markMessagesAsPulled(@RequestBody List<Long> messageIds) {
        String userId = UserContextUtil.getCurrentUserId();
        chatOfflineCursorService.markMessagesAsPulled(userId, messageIds);
        return Result.success("消息已标记为已拉取");
    }
}
