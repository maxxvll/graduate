package com.maxxvll.controller;

import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.vo.SessionVO;
import com.maxxvll.service.ChatSessionService;
import com.maxxvll.utils.UserContextUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/session")
public class SessionController extends BaseController {

    @Resource
    private ChatSessionService chatSessionService;

    /**
     * 获取当前用户的会话列表
     * 排序：置顶优先，再按最后消息时间倒序
     */
    @GetMapping("/list")
    public Result<List<SessionVO>> getSessionList() {
        String currentUserId = UserContextUtil.getCurrentUserId();
        List<SessionVO> sessionList = chatSessionService.getSessionList(currentUserId);
        return Result.success(sessionList);
    }

    /**
     * 删除会话（软删除，仅对当前用户隐藏）
     */
    @DeleteMapping("/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        String currentUserId = UserContextUtil.getCurrentUserId();
        chatSessionService.softDeleteSession(sessionId, currentUserId);
        return Result.success("删除成功");
    }

    /**
     * 置顶/取消置顶会话
     */
    @PutMapping("/top/{sessionId}")
    public Result<Void> toggleTop(@PathVariable String sessionId, @RequestParam Integer isTop) {
        String currentUserId = UserContextUtil.getCurrentUserId();
        chatSessionService.updateTopStatus(sessionId, currentUserId, isTop);
        return Result.success(isTop == 1 ? "置顶成功" : "取消置顶成功");
    }

    /**
     * 免打扰/取消免打扰
     */
    @PutMapping("/mute/{sessionId}")
    public Result<Void> toggleMute(@PathVariable String sessionId, @RequestParam Integer isMute) {
        String currentUserId = UserContextUtil.getCurrentUserId();
        chatSessionService.updateMuteStatus(sessionId, currentUserId, isMute);
        return Result.success(isMute == 1 ? "已开启免打扰" : "已关闭免打扰");
    }

    /**
     * 清除会话未读数
     */
    @PutMapping("/read/{sessionId}")
    public Result<Void> clearUnread(@PathVariable String sessionId) {
        String currentUserId = UserContextUtil.getCurrentUserId();
        chatSessionService.clearUnreadCount(sessionId, currentUserId);
        return Result.success("已清除未读");
    }
}
