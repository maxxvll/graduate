package com.maxxvll.controller;

import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.vo.FriendGroupVO;
import com.maxxvll.domain.FriendGroup;
import com.maxxvll.service.FriendGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 好友分组控制器
 *
 * @author 20570
 */
@Slf4j
@RestController
@RequestMapping("/friend/group")
@Tag(name = "好友分组", description = "好友分组管理相关接口")
public class FriendGroupController extends BaseController {

    @jakarta.annotation.Resource
    private FriendGroupService friendGroupService;

    /**
     * 获取好友分组列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取好友分组列表", description = "获取当前用户的所有好友分组及分组内的好友")
    public Result<List<FriendGroupVO>> getGroupList() {
        List<FriendGroupVO> list = friendGroupService.getGroupList(getCurrentUserId());
        return success(list);
    }

    /**
     * 创建好友分组
     */
    @PostMapping("/create")
    @Operation(summary = "创建好友分组", description = "创建新的好友分组")
    public Result<FriendGroup> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        FriendGroup group = friendGroupService.createGroup(getCurrentUserId(), request.getName());
        return success(group);
    }

    /**
     * 更新好友分组名称
     */
    @PutMapping("/update")
    @Operation(summary = "更新好友分组", description = "更新好友分组的名称")
    public Result<Void> updateGroup(@Valid @RequestBody UpdateGroupRequest request) {
        friendGroupService.updateGroup(request.getId(), getCurrentUserId(), request.getName());
        return success("分组已更新");
    }

    /**
     * 删除好友分组
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除好友分组", description = "删除好友分组，组内好友将移动到默认分组")
    public Result<Void> deleteGroup(@PathVariable Long id) {
        friendGroupService.deleteGroup(id, getCurrentUserId());
        return success("分组已删除");
    }

    /**
     * 移动好友到分组
     */
    @PutMapping("/move")
    @Operation(summary = "移动好友到分组", description = "将好友移动到指定分组")
    public Result<Void> moveFriendToGroup(@Valid @RequestBody MoveFriendRequest request) {
        friendGroupService.moveFriendToGroup(getCurrentUserId(), request.getFriendId(), request.getGroupId());
        return success("好友已移动到新分组");
    }

    /**
     * 创建分组请求
     */
    public static class CreateGroupRequest {
        @NotBlank(message = "分组名称不能为空")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * 更新分组请求
     */
    public static class UpdateGroupRequest {
        private Long id;
        @NotBlank(message = "分组名称不能为空")
        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * 移动好友请求
     */
    public static class MoveFriendRequest {
        private Long friendId;
        private Long groupId;

        public Long getFriendId() {
            return friendId;
        }

        public void setFriendId(Long friendId) {
            this.friendId = friendId;
        }

        public Long getGroupId() {
            return groupId;
        }

        public void setGroupId(Long groupId) {
            this.groupId = groupId;
        }
    }
}
