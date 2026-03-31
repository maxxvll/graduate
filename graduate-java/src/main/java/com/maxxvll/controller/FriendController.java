package com.maxxvll.controller;

import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.dto.FriendApplyDTO;
import com.maxxvll.common.dto.FriendApplyHandleDTO;
import com.maxxvll.common.dto.FriendBlacklistUpdateDTO;
import com.maxxvll.common.dto.FriendRelationUpdateDTO;
import com.maxxvll.common.vo.FriendApplicationVO;
import com.maxxvll.service.FriendApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 好友管理控制器
 * 提供好友申请、好友列表、黑名单等功能的REST接口
 */
@Slf4j
@RestController
@RequestMapping("/friend")
@Tag(name = "好友", description = "好友管理相关接口")
public class FriendController extends BaseController {

    @Resource
    private FriendApplicationService friendApplicationService;

    /**
     * 发送好友申请
     */
    @PostMapping("/apply")
    public Result<Void> applyFriend(@Valid @RequestBody FriendApplyDTO applyDTO) {
        friendApplicationService.applyFriend(applyDTO, getCurrentUserId());
        return success("好友申请已发送");
    }

    /**
     * 获取收到的好友申请列表
     */
    @GetMapping("/apply/received")
    public Result<List<FriendApplicationVO>> getReceivedApplications() {
        List<FriendApplicationVO> list = friendApplicationService.getReceivedApplications(getCurrentUserId());
        return success(list);
    }

    /**
     * 获取发出的好友申请列表
     */
    @GetMapping("/apply/sent")
    public Result<List<FriendApplicationVO>> getSentApplications() {
        List<FriendApplicationVO> list = friendApplicationService.getSentApplications(getCurrentUserId());
        return success(list);
    }

    /**
     * 处理好友申请（接受/拒绝）
     */
    @PostMapping("/apply/handle")
    public Result<Void> handleApplication(@Valid @RequestBody FriendApplyHandleDTO handleDTO) {
        friendApplicationService.handleApplication(handleDTO, getCurrentUserId());
        return success(handleDTO.getStatus() != null && handleDTO.getStatus() == 1 ? "已接受好友申请" : "已拒绝");
    }

    /**
     * 获取好友列表
     */
    @GetMapping("/list")
    public Result<List<FriendApplicationVO>> getFriendList() {
        List<FriendApplicationVO> list = friendApplicationService.getFriendList(getCurrentUserId());
        return success(list);
    }

    /**
     * 更新好友关系资料
     */
    @PutMapping("/relation")
    public Result<Void> updateFriendRelation(@Valid @RequestBody FriendRelationUpdateDTO updateDTO) {
        friendApplicationService.updateFriendRelation(updateDTO, getCurrentUserId());
        return success("好友资料已更新");
    }

    /**
     * 更新好友黑名单状态
     */
    @PutMapping("/blacklist")
    public Result<Void> updateFriendBlacklist(@Valid @RequestBody FriendBlacklistUpdateDTO updateDTO) {
        friendApplicationService.updateFriendBlacklist(updateDTO, getCurrentUserId());
        return success(Boolean.TRUE.equals(updateDTO.getBlacklisted()) ? "已加入黑名单" : "已移出黑名单");
    }

    /**
     * 删除好友
     */
    @DeleteMapping("/{friendUserId}")
    public Result<Void> deleteFriend(@PathVariable String friendUserId) {
        friendApplicationService.deleteFriend(friendUserId, getCurrentUserId());
        return success("联系人已从通讯录移除");
    }

    /**
     * 获取黑名单列表
     */
    @GetMapping("/blacklist")
    public Result<List<FriendApplicationVO>> getBlacklist() {
        List<FriendApplicationVO> list = friendApplicationService.getBlacklist(getCurrentUserId());
        return success(list);
    }
}
