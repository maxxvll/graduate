package com.maxxvll.controller;

import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.constants.MemberConstants;
import com.maxxvll.common.vo.CursorPageVO;
import com.maxxvll.common.vo.SessionVO;
import com.maxxvll.service.ChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会话控制器
 * 提供会话管理相关的REST接口，包括会话列表查询、删除、置顶、免打扰等功能
 */
@Slf4j
@RestController
@RequestMapping("/session")
@Tag(name = "会话", description = "会话管理相关接口")
public class SessionController extends BaseController {

    @Resource
    private ChatSessionService chatSessionService;

    /**
     * 获取当前用户的会话列表
     * 排序：置顶优先，再按最后消息时间倒序
     */
    @GetMapping("/list")
    public Result<List<SessionVO>> getSessionList(
            @RequestParam(defaultValue = "100") @Positive int limit) {
        limit = normalizeLimit(limit);
        CursorPageVO<SessionVO> page = chatSessionService.getSessionPage(getCurrentUserId(), limit, null);
        return success(page.getItems());
    }

    /**
     * 游标分页获取会话列表
     */
    @GetMapping("/list/page")
    public Result<CursorPageVO<SessionVO>> getSessionPage(
            @RequestParam(defaultValue = "100") @Positive int limit,
            @RequestParam(required = false) String cursor) {
        limit = normalizeLimit(limit);
        return success(chatSessionService.getSessionPage(getCurrentUserId(), limit, cursor));
    }

    /**
     * 基于更新时间游标增量同步会话变更
     */
    @GetMapping("/list/sync")
    public Result<CursorPageVO<SessionVO>> syncSessionList(
            @RequestParam(defaultValue = "100") @Positive int limit,
            @RequestParam(required = false) String cursor) {
        limit = normalizeLimit(limit);
        return success(chatSessionService.syncSessionList(getCurrentUserId(), limit, cursor));
    }

    /**
     * 删除会话（软删除，仅对当前用户隐藏）
     */
    @DeleteMapping("/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        chatSessionService.softDeleteSession(sessionId, getCurrentUserId());
        return success("删除成功");
    }

    /**
     * 置顶/取消置顶会话
     */
    @PutMapping("/top/{sessionId}")
    public Result<Void> toggleTop(
            @PathVariable String sessionId,
            @RequestParam @Min(0) @Max(1) Integer isTop) {
        chatSessionService.updateTopStatus(sessionId, getCurrentUserId(), isTop);
        return success(isTop.equals(MemberConstants.TopStatus.TOP) ? "置顶成功" : "取消置顶成功");
    }

    /**
     * 免打扰/取消免打扰
     */
    @PutMapping("/mute/{sessionId}")
    public Result<Void> toggleMute(
            @PathVariable String sessionId,
            @RequestParam @Min(0) @Max(1) Integer isMute) {
        chatSessionService.updateMuteStatus(sessionId, getCurrentUserId(), isMute);
        return success(isMute.equals(MemberConstants.MuteStatus.MUTED) ? "已开启免打扰" : "已关闭免打扰");
    }

    /**
     * 清除会话未读数
     */
    @PutMapping("/read/{sessionId}")
    public Result<Void> clearUnread(@PathVariable String sessionId) {
        chatSessionService.clearUnreadCount(sessionId, getCurrentUserId());
        return success("已清除未读");
    }
}
