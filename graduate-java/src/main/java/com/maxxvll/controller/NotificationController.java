package com.maxxvll.controller;

import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.annotation.RequirePermission;
import com.maxxvll.common.dto.NotificationSettingUpdateDTO;
import com.maxxvll.common.dto.SystemNotificationSendDTO;
import com.maxxvll.common.enums.Permission;
import com.maxxvll.common.enums.Role;
import com.maxxvll.common.vo.NotificationPageVO;
import com.maxxvll.common.vo.NotificationSettingVO;
import com.maxxvll.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器
 * <p>
 * 提供通知相关的REST接口，包括通知设置、通知查询、系统通知发送等功能
 * </p>
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Slf4j
@RestController
@RequestMapping("/notification")
@Tag(name = "通知", description = "通知管理相关接口")
public class NotificationController extends BaseController {

    @Resource
    private NotificationService notificationService;

    // ==================== 通知设置 ====================

    /**
     * 获取当前用户的通知设置
     */
    @GetMapping("/setting")
    @Operation(summary = "获取通知设置", description = "获取当前用户的通知设置")
    public Result<NotificationSettingVO> getNotificationSetting() {
        NotificationSettingVO setting = notificationService.getNotificationSetting(getCurrentUserId());
        return success(setting);
    }

    /**
     * 更新通知设置
     */
    @PutMapping("/setting")
    @Operation(summary = "更新通知设置", description = "更新当前用户的通知设置")
    public Result<Void> updateNotificationSetting(@RequestBody @Valid NotificationSettingUpdateDTO updateDTO) {
        notificationService.updateNotificationSetting(getCurrentUserId(), updateDTO);
        return success("更新成功");
    }

    // ==================== 通知查询 ====================

    /**
     * 获取通知列表（游标分页）
     */
    @GetMapping("/list")
    @Operation(summary = "获取通知列表", description = "获取当前用户的通知列表，支持游标分页")
    public Result<NotificationPageVO> getNotifications(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Positive int limit) {
        limit = normalizeLimit(limit);
        NotificationPageVO page = notificationService.getNotifications(getCurrentUserId(), cursor, limit);
        return success(page);
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    @Operation(summary = "获取未读数量", description = "获取当前用户的未读通知数量")
    public Result<Long> getUnreadCount() {
        Long count = notificationService.getUnreadCount(getCurrentUserId());
        return success(count);
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/{notificationId}/read")
    @Operation(summary = "标记已读", description = "标记指定通知为已读")
    public Result<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(getCurrentUserId(), notificationId);
        return success("标记成功");
    }

    /**
     * 标记所有通知为已读
     */
    @PutMapping("/read-all")
    @Operation(summary = "全部已读", description = "标记所有通知为已读")
    public Result<Void> markAllAsRead() {
        notificationService.markAllAsRead(getCurrentUserId());
        return success("标记成功");
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{notificationId}")
    @Operation(summary = "删除通知", description = "删除指定通知")
    public Result<Void> deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(getCurrentUserId(), notificationId);
        return success("删除成功");
    }

    // ==================== 系统通知（管理员） ====================

    /**
     * 发送全员通知（仅管理员）
     */
    @PostMapping("/broadcast")
    @Operation(summary = "发送全员通知", description = "发送全员通知（仅管理员）")
    @RequirePermission(roles = {Role.ADMIN, Role.SUPER_ADMIN})
    public Result<Void> sendBroadcast(@RequestBody @Valid SystemNotificationSendDTO dto) {
        notificationService.sendBroadcast(dto);
        return success("发送成功");
    }

    /**
     * 发送系统通知给指定用户
     */
    @PostMapping("/send/user")
    @Operation(summary = "发送用户通知", description = "发送系统通知给指定用户")
    @RequirePermission(roles = {Role.ADMIN, Role.SUPER_ADMIN})
    public Result<Void> sendToUser(@RequestBody @Valid SystemNotificationSendDTO dto) {
        notificationService.sendToUser(dto, getCurrentUserId(), "系统通知", null);
        return success("发送成功");
    }

    /**
     * 发送系统通知给指定群组
     */
    @PostMapping("/send/group")
    @Operation(summary = "发送群通知", description = "发送系统通知给指定群组")
    @RequirePermission(roles = {Role.ADMIN, Role.SUPER_ADMIN})
    public Result<Void> sendToGroup(@RequestBody @Valid SystemNotificationSendDTO dto) {
        notificationService.sendToGroup(dto, getCurrentUserId(), "系统通知", null);
        return success("发送成功");
    }
}
