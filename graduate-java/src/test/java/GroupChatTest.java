package com.maxxvll.test;

import com.maxxvll.common.dto.*;
import com.maxxvll.common.vo.GroupInfoVO;
import com.maxxvll.common.vo.GroupMemberVO;
import com.maxxvll.service.ChatGroupMemberService;
import com.maxxvll.service.ChatGroupService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class GroupChatTest {

    @Resource
    private ChatGroupService chatGroupService;
    
    @Resource
    private ChatGroupMemberService chatGroupMemberService;

    @Test
    public void testGroupPermissions() {
        // 1. 创建群聊
        GroupCreateDTO createDTO = new GroupCreateDTO();
        createDTO.setGroupName("测试群");
        createDTO.setMaxMember(50);
        createDTO.setJoinType(2); // 免审核
        // createDTO.setMemberIds(Arrays.asList("user2", "user3")); // 添加初始成员

        // GroupInfoVO groupInfo = chatGroupService.createGroup(createDTO, "user1");
        // System.out.println("创建群聊成功: " + groupInfo);

        // 2. 邀请成员测试（假设user1是群主）
        // GroupMemberAddDTO addDTO = new GroupMemberAddDTO();
        // addDTO.setGroupId("group1");
        // addDTO.setUserIds(Arrays.asList("user4", "user5"));
        // chatGroupMemberService.addMembers(addDTO, "user1");

        // 3. 移除成员测试
        // GroupMemberRemoveDTO removeDTO = new GroupMemberRemoveDTO();
        // removeDTO.setGroupId("group1");
        // removeDTO.setUserId("user4");
        // removeDTO.setReason("测试移除原因");
        // chatGroupMemberService.removeMember(removeDTO, "user1");

        // 4. 更新成员权限测试
        // GroupMemberUpdateDTO updateDTO = new GroupMemberUpdateDTO();
        // updateDTO.setGroupId("group1");
        // updateDTO.setUserId("user5");
        // updateDTO.setRole(2); // 设为管理员
        // updateDTO.setIsMute(0);
        // chatGroupMemberService.updateMember(updateDTO, "user1");

        // 5. 查看成员列表
        // List<GroupMemberVO> members = chatGroupMemberService.getGroupMembers("group1");
        // System.out.println("群成员列表:");
        // members.forEach(member -> {
        //     System.out.println("用户ID: " + member.getUserId() + 
        //                        ", 角色: " + member.getRoleName() + 
        //                        ", 是否禁言: " + member.getIsMute());
        // });
    }
}