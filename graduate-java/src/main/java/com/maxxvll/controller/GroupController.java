package com.maxxvll.controller;

import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.dto.*;
import com.maxxvll.common.vo.GroupApplicationVO;
import com.maxxvll.common.vo.GroupInfoVO;
import com.maxxvll.common.vo.GroupMemberVO;
import com.maxxvll.service.ChatGroupMemberService;
import com.maxxvll.service.ChatGroupService;
import com.maxxvll.service.GroupApplicationService;
import com.maxxvll.utils.UserContextUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 群聊管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/group")
public class GroupController extends BaseController {

    @Resource
    private ChatGroupService chatGroupService;

    @Resource
    private ChatGroupMemberService chatGroupMemberService;

    @Resource
    private GroupApplicationService groupApplicationService;

    // ==================== 群聊基础操作 ====================

    /**
     * 创建群聊
     */
    @PostMapping("/create")
    public Result<GroupInfoVO> createGroup(@RequestBody GroupCreateDTO createDTO) {
        String userId = UserContextUtil.getCurrentUserId();
        GroupInfoVO groupInfo = chatGroupService.createGroup(createDTO, userId);
        return Result.success("创建群聊成功", groupInfo);
    }

    /**
     * 更新群聊信息
     */
    @PostMapping("/update")
    public Result<GroupInfoVO> updateGroup(@RequestBody GroupUpdateDTO updateDTO) {
        String userId = UserContextUtil.getCurrentUserId();
        GroupInfoVO groupInfo = chatGroupService.updateGroup(updateDTO, userId);
        return Result.success("更新群聊信息成功", groupInfo);
    }

    /**
     * 获取群聊信息
     */
    @GetMapping("/info/{groupId}")
    public Result<GroupInfoVO> getGroupInfo(@PathVariable String groupId) {
        String userId = UserContextUtil.getCurrentUserId();
        GroupInfoVO groupInfo = chatGroupService.getGroupInfo(groupId, userId);
        return Result.success("获取群聊信息成功", groupInfo);
    }

    /**
     * 搜索群聊（按群名模糊匹配）
     */
    @GetMapping("/search")
    public Result<List<GroupInfoVO>> searchGroup(@RequestParam String keyword) {
        String userId = UserContextUtil.getCurrentUserId();
        List<GroupInfoVO> groups = chatGroupService.searchGroup(keyword, userId);
        return Result.success("搜索成功", groups);
    }

    /**
     * 获取用户加入的群聊列表
     */
    @GetMapping("/list")
    public Result<List<GroupInfoVO>> getUserGroups() {
        String userId = UserContextUtil.getCurrentUserId();
        List<GroupInfoVO> groups = chatGroupService.getUserGroups(userId);
        return Result.success("获取群聊列表成功", groups);
    }

    /**
     * 解散群聊
     */
    @PostMapping("/dissolve/{groupId}")
    public Result<Void> dissolveGroup(@PathVariable String groupId) {
        String userId = UserContextUtil.getCurrentUserId();
        chatGroupService.dissolveGroup(groupId, userId);
        return Result.success("解散群聊成功");
    }

    /**
     * 退出群聊
     */
    @PostMapping("/quit/{groupId}")
    public Result<Void> quitGroup(@PathVariable String groupId) {
        String userId = UserContextUtil.getCurrentUserId();
        chatGroupService.quitGroup(groupId, userId);
        return Result.success("退出群聊成功");
    }

    /**
     * 转让群主
     */
    @PostMapping("/transfer")
    public Result<Void> transferGroupOwner(@RequestBody GroupTransferDTO transferDTO) {
        String userId = UserContextUtil.getCurrentUserId();
        chatGroupService.transferGroupOwner(transferDTO, userId);
        return Result.success("转让群主成功");
    }

    // ==================== 群成员管理 ====================

    /**
     * 添加群成员
     */
    @PostMapping("/member/add")
    public Result<Void> addMembers(@RequestBody GroupMemberAddDTO addDTO) {
        String userId = UserContextUtil.getCurrentUserId();
        chatGroupMemberService.addMembers(addDTO, userId);
        return Result.success("添加群成员成功");
    }

    /**
     * 移除群成员
     */
    @PostMapping("/member/remove")
    public Result<Void> removeMember(@RequestBody GroupMemberRemoveDTO removeDTO) {
        String userId = UserContextUtil.getCurrentUserId();
        chatGroupMemberService.removeMember(removeDTO, userId);
        return Result.success("移除群成员成功");
    }

    /**
     * 更新群成员信息（角色、禁言状态）
     */
    @PostMapping("/member/update")
    public Result<Void> updateMember(@RequestBody GroupMemberUpdateDTO updateDTO) {
        String userId = UserContextUtil.getCurrentUserId();
        chatGroupMemberService.updateMember(updateDTO, userId);
        return Result.success("更新群成员信息成功");
    }

    /**
     * 获取群成员列表
     */
    @GetMapping("/member/list/{groupId}")
    public Result<List<GroupMemberVO>> getGroupMembers(@PathVariable String groupId) {
        List<GroupMemberVO> members = chatGroupMemberService.getGroupMembers(groupId);
        return Result.success("获取群成员列表成功", members);
    }

    /**
     * 获取当前用户在群中的角色
     */
    @GetMapping("/member/role/{groupId}")
    public Result<Integer> getUserRole(@PathVariable String groupId) {
        String userId = UserContextUtil.getCurrentUserId();
        Integer role = chatGroupMemberService.getUserRole(groupId, userId);
        return Result.success("获取角色成功", role);
    }

    // ==================== 群申请管理 ====================

    /**
     * 申请加入群聊
     */
    @PostMapping("/apply")
    public Result<Void> applyJoinGroup(@RequestBody GroupApplyDTO applyDTO) {
        String userId = UserContextUtil.getCurrentUserId();
        groupApplicationService.applyJoinGroup(applyDTO, userId);
        return Result.success("申请已提交");
    }

    /**
     * 处理群申请
     */
    @PostMapping("/apply/handle")
    public Result<Void> handleApplication(@RequestBody GroupApplyHandleDTO handleDTO) {
        String userId = UserContextUtil.getCurrentUserId();
        groupApplicationService.handleApplication(handleDTO, userId);
        return Result.success("处理成功");
    }

    /**
     * 获取群申请列表（管理员/群主查看）
     */
    @GetMapping("/apply/list/{groupId}")
    public Result<List<GroupApplicationVO>> getGroupApplications(
            @PathVariable String groupId,
            @RequestParam(required = false) Integer status) {
        List<GroupApplicationVO> applications = groupApplicationService.getGroupApplications(groupId, status);
        return Result.success("获取申请列表成功", applications);
    }

    /**
     * 获取用户发送的申请列表
     */
    @GetMapping("/apply/my")
    public Result<List<GroupApplicationVO>> getUserApplications() {
        String userId = UserContextUtil.getCurrentUserId();
        List<GroupApplicationVO> applications = groupApplicationService.getUserApplications(userId);
        return Result.success("获取申请列表成功", applications);
    }

    /**
     * 获取用户收到的申请列表（作为群主/管理员）
     */
    @GetMapping("/apply/received")
    public Result<List<GroupApplicationVO>> getReceivedApplications() {
        String userId = UserContextUtil.getCurrentUserId();
        List<GroupApplicationVO> applications = groupApplicationService.getReceivedApplications(userId);
        return Result.success("获取申请列表成功", applications);
    }

    /**
     * 撤回申请
     */
    @PostMapping("/apply/cancel/{applyId}")
    public Result<Void> cancelApplication(@PathVariable Long applyId) {
        String userId = UserContextUtil.getCurrentUserId();
        groupApplicationService.cancelApplication(applyId, userId);
        return Result.success("撤回申请成功");
    }
}
