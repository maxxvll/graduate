package com.maxxvll.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.annotation.RequirePermission;
import com.maxxvll.common.dto.*;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.common.vo.GroupApplicationVO;
import com.maxxvll.common.vo.GroupInfoVO;
import com.maxxvll.common.vo.GroupMemberVO;
import com.maxxvll.service.ChatGroupMemberService;
import com.maxxvll.service.ChatGroupService;
import com.maxxvll.service.GroupApplicationService;
import com.maxxvll.utils.MinioUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 群聊管理控制器
 * 提供群聊创建、管理、成员管理等功能的REST接口
 */
@Slf4j
@RestController
@RequestMapping("/group")
@Tag(name = "群聊", description = "群聊管理相关接口")
public class GroupController extends BaseController {

    @Resource
    private ChatGroupService chatGroupService;

    @Resource
    private ChatGroupMemberService chatGroupMemberService;

    @Resource
    private GroupApplicationService groupApplicationService;

    @Resource
    private MinioUtil minioUtil;

    // ==================== 群头像上传 ====================

    /**
     * 上传群头像（创建群聊时使用）
     */
    @SneakyThrows
    @PostMapping("/avatar/upload")
    public Result<Map<String, String>> uploadGroupAvatar(@RequestParam("file") @NotNull MultipartFile file) {
        validateImageFile(file);
        String filePath = minioUtil.uploadGroupAvatar(file);
        String previewUrl = minioUtil.getAvatarUrl(filePath);

        Map<String, String> result = new HashMap<>();
        result.put("filePath", filePath);
        result.put("previewUrl", previewUrl);
        return success("群头像上传成功", result);
    }

    // ==================== 群聊基础操作 ====================

    /**
     * 创建群聊
     */
    @PostMapping("/create")
    public Result<GroupInfoVO> createGroup(@Valid @RequestBody GroupCreateDTO createDTO) {
        GroupInfoVO groupInfo = chatGroupService.createGroup(createDTO, getCurrentUserId());
        return success("创建群聊成功", groupInfo);
    }

    /**
     * 更新群聊信息
     */
    @PostMapping("/update")
    @RequirePermission("group:update")
    public Result<GroupInfoVO> updateGroup(@Valid @RequestBody GroupUpdateDTO updateDTO) {
        GroupInfoVO groupInfo = chatGroupService.updateGroup(updateDTO, getCurrentUserId());
        return success("更新群聊信息成功", groupInfo);
    }

    /**
     * 获取群聊信息
     */
    @GetMapping("/info/{groupId}")
    public Result<GroupInfoVO> getGroupInfo(@PathVariable @NotNull Long groupId) {
        GroupInfoVO groupInfo = chatGroupService.getGroupInfo(groupId, getCurrentUserId());
        return success("获取群聊信息成功", groupInfo);
    }

    /**
     * 搜索群聊（按群名模糊匹配）
     */
    @GetMapping("/search")
    public Result<List<GroupInfoVO>> searchGroup(@RequestParam @NotBlank String keyword) {
        List<GroupInfoVO> groups = chatGroupService.searchGroup(keyword, getCurrentUserId());
        return success("搜索成功", groups);
    }

    /**
     * 分页搜索群聊（按群名模糊匹配）
     */
    @GetMapping("/search/page")
    public Result<Page<GroupInfoVO>> searchGroupPage(
            @RequestParam @NotBlank String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        page = normalizePageNum(page);
        size = normalizePageSize(size);
        Page<GroupInfoVO> groups = chatGroupService.searchGroupPage(keyword, getCurrentUserId(), page, size);
        return success("搜索成功", groups);
    }

    /**
     * 获取用户加入的群聊列表
     */
    @GetMapping("/list")
    public Result<List<GroupInfoVO>> getUserGroups() {
        List<GroupInfoVO> groups = chatGroupService.getUserGroups(getCurrentUserId());
        return success("获取群聊列表成功", groups);
    }

    /**
     * 解散群聊
     */
    @PostMapping("/dissolve/{groupId}")
    @RequirePermission(value = {"group:dissolve"}, resourceParam = "groupId")
    public Result<Void> dissolveGroup(@PathVariable @NotNull Long groupId) {
        chatGroupService.dissolveGroup(groupId, getCurrentUserId());
        return success("解散群聊成功");
    }

    /**
     * 退出群聊
     */
    @PostMapping("/quit/{groupId}")
    @RequirePermission(value = {"group:quit"}, resourceParam = "groupId")
    public Result<Void> quitGroup(@PathVariable @NotNull Long groupId) {
        chatGroupService.quitGroup(groupId, getCurrentUserId());
        return success("退出群聊成功");
    }

    /**
     * 转让群主
     */
    @PostMapping("/transfer")
    @RequirePermission("group:transfer")
    public Result<Void> transferGroupOwner(@Valid @RequestBody GroupTransferDTO transferDTO) {
        chatGroupService.transferGroupOwner(transferDTO, getCurrentUserId());
        return success("转让群主成功");
    }

    // ==================== 群成员管理 ====================

    /**
     * 添加群成员
     */
    @PostMapping("/member/add")
    public Result<Void> addMembers(@Valid @RequestBody GroupMemberAddDTO addDTO) {
        chatGroupMemberService.addMembers(addDTO, getCurrentUserId());
        return success("添加群成员成功");
    }

    /**
     * 移除群成员
     */
    @PostMapping("/member/remove")
    public Result<Void> removeMember(@Valid @RequestBody GroupMemberRemoveDTO removeDTO) {
        chatGroupMemberService.removeMember(removeDTO, getCurrentUserId());
        return success("移除群成员成功");
    }

    /**
     * 更新群成员信息（角色、禁言状态）
     */
    @PostMapping("/member/update")
    public Result<Void> updateMember(@Valid @RequestBody GroupMemberUpdateDTO updateDTO) {
        chatGroupMemberService.updateMember(updateDTO, getCurrentUserId());
        return success("更新群成员信息成功");
    }

    /**
     * 获取群成员列表
     */
    @GetMapping("/member/list/{groupId}")
    public Result<List<GroupMemberVO>> getGroupMembers(@PathVariable @NotNull Long groupId) {
        List<GroupMemberVO> members = chatGroupMemberService.getGroupMembers(groupId);
        return success("获取群成员列表成功", members);
    }

    /**
     * 获取当前用户在群中的角色
     */
    @GetMapping("/member/role/{groupId}")
    public Result<Integer> getUserRole(@PathVariable @NotNull Long groupId) {
        Integer role = chatGroupMemberService.getUserRole(groupId, getCurrentUserId());
        return success("获取角色成功", role);
    }

    // ==================== 群申请管理 ====================

    /**
     * 申请加入群聊
     */
    @PostMapping("/apply")
    public Result<Void> applyJoinGroup(@Valid @RequestBody GroupApplyDTO applyDTO) {
        groupApplicationService.applyJoinGroup(applyDTO, getCurrentUserId());
        return success("申请已提交");
    }

    /**
     * 处理群申请
     */
    @PostMapping("/apply/handle")
    public Result<Void> handleApplication(@Valid @RequestBody GroupApplyHandleDTO handleDTO) {
        groupApplicationService.handleApplication(handleDTO, getCurrentUserId());
        return success("处理成功");
    }

    /**
     * 获取群申请列表（管理员/群主查看）
     */
    @GetMapping("/apply/list/{groupId}")
    public Result<List<GroupApplicationVO>> getGroupApplications(
            @PathVariable @NotNull Long groupId,
            @RequestParam(required = false) Integer status) {
        List<GroupApplicationVO> applications = groupApplicationService.getGroupApplications(groupId, status);
        return success("获取申请列表成功", applications);
    }

    /**
     * 获取用户发送的申请列表
     */
    @GetMapping("/apply/my")
    public Result<List<GroupApplicationVO>> getUserApplications() {
        List<GroupApplicationVO> applications = groupApplicationService.getUserApplications(getCurrentUserId());
        return success("获取申请列表成功", applications);
    }

    /**
     * 获取用户收到的申请列表（作为群主/管理员）
     */
    @GetMapping("/apply/received")
    public Result<List<GroupApplicationVO>> getReceivedApplications() {
        List<GroupApplicationVO> applications = groupApplicationService.getReceivedApplications(getCurrentUserId());
        return success("获取申请列表成功", applications);
    }

    /**
     * 撤回申请
     */
    @PostMapping("/apply/cancel/{applyId}")
    public Result<Void> cancelApplication(@PathVariable @NotNull Long applyId) {
        groupApplicationService.cancelApplication(applyId, getCurrentUserId());
        return success("撤回申请成功");
    }

    // ==================== 群公告管理 ====================

    /**
     * 发布/更新群公告
     */
    @PostMapping("/notice/publish")
    public Result<Void> publishNotice(
            @RequestParam @NotNull Long groupId,
            @RequestParam(required = false) String notice) {
        chatGroupService.publishNotice(groupId, notice, getCurrentUserId());
        return success("公告发布成功");
    }

    /**
     * 获取群公告
     */
    @GetMapping("/notice/{groupId}")
    public Result<String> getNotice(@PathVariable @NotNull Long groupId) {
        String notice = chatGroupService.getNotice(groupId);
        return success("获取公告成功", notice);
    }

    /**
     * 删除群公告（仅群主/管理员）
     */
    @DeleteMapping("/notice/{groupId}")
    public Result<Void> deleteNotice(@PathVariable @NotNull Long groupId) {
        chatGroupService.deleteNotice(groupId, getCurrentUserId());
        return success("删除公告成功");
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 验证图片文件
     */
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("上传失败：文件为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException("上传失败：文件名为空");
        }
        int dotIdx = originalFilename.lastIndexOf(".");
        if (dotIdx < 0) {
            throw new BusinessException("上传失败：文件格式不支持");
        }
        String suffix = originalFilename.substring(dotIdx).toLowerCase();
        if (!suffix.matches("\\.(jpg|jpeg|png|gif|webp)$")) {
            throw new BusinessException("上传失败：仅支持 jpg/jpeg/png/gif/webp 格式");
        }
    }
}
