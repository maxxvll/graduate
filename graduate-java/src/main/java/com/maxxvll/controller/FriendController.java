package com.maxxvll.controller;

import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.dto.FriendApplyDTO;
import com.maxxvll.common.dto.FriendApplyHandleDTO;
import com.maxxvll.common.vo.FriendApplicationVO;
import com.maxxvll.service.FriendApplicationService;
import com.maxxvll.utils.UserContextUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 好友管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/friend")
public class FriendController extends BaseController {

    @Resource
    private FriendApplicationService friendApplicationService;

    /**
     * 发送好友申请
     */
    @PostMapping("/apply")
    public Result<Void> applyFriend(@RequestBody FriendApplyDTO applyDTO) {
        String currentUserId = UserContextUtil.getCurrentUserId();
        friendApplicationService.applyFriend(applyDTO, currentUserId);
        return Result.success("好友申请已发送");
    }

    /**
     * 获取收到的好友申请列表
     */
    @GetMapping("/apply/received")
    public Result<List<FriendApplicationVO>> getReceivedApplications() {
        String currentUserId = UserContextUtil.getCurrentUserId();
        List<FriendApplicationVO> list = friendApplicationService.getReceivedApplications(currentUserId);
        return Result.success(list);
    }

    /**
     * 获取发出的好友申请列表
     */
    @GetMapping("/apply/sent")
    public Result<List<FriendApplicationVO>> getSentApplications() {
        String currentUserId = UserContextUtil.getCurrentUserId();
        List<FriendApplicationVO> list = friendApplicationService.getSentApplications(currentUserId);
        return Result.success(list);
    }

    /**
     * 处理好友申请（接受/拒绝）
     */
    @PostMapping("/apply/handle")
    public Result<Void> handleApplication(@RequestBody FriendApplyHandleDTO handleDTO) {
        String currentUserId = UserContextUtil.getCurrentUserId();
        friendApplicationService.handleApplication(handleDTO, currentUserId);
        return Result.success(handleDTO.getStatus() == 1 ? "已接受好友申请" : "已拒绝");
    }

    /**
     * 获取好友列表
     */
    @GetMapping("/list")
    public Result<List<FriendApplicationVO>> getFriendList() {
        String currentUserId = UserContextUtil.getCurrentUserId();
        List<FriendApplicationVO> list = friendApplicationService.getFriendList(currentUserId);
        return Result.success(list);
    }
}
