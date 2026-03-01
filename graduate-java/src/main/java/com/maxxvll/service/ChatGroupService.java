package com.maxxvll.service;

import com.maxxvll.common.dto.GroupCreateDTO;
import com.maxxvll.common.dto.GroupTransferDTO;
import com.maxxvll.common.dto.GroupUpdateDTO;
import com.maxxvll.common.vo.GroupInfoVO;
import com.maxxvll.domain.ChatGroup;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 20570
* @description 针对表【chat_group(群聊基础信息表)】的数据库操作Service
* @createDate 2026-02-19 12:02:21
*/
public interface ChatGroupService extends IService<ChatGroup> {

    /**
     * 创建群聊
     * @param createDTO 创建信息
     * @param creatorId 创建人ID
     * @return 群信息
     */
    GroupInfoVO createGroup(GroupCreateDTO createDTO, String creatorId);

    /**
     * 更新群聊信息
     * @param updateDTO 更新信息
     * @param operatorId 操作人ID
     * @return 更新后的群信息
     */
    GroupInfoVO updateGroup(GroupUpdateDTO updateDTO, String operatorId);

    /**
     * 解散群聊
     * @param groupId 群ID
     * @param operatorId 操作人ID
     */
    void dissolveGroup(Long groupId, String operatorId);

    /**
     * 获取群聊信息
     * @param groupId 群ID
     * @param userId 当前用户ID
     * @return 群信息
     */
    GroupInfoVO getGroupInfo(Long groupId, String userId);

    /**
     * 获取用户加入的群聊列表
     * @param userId 用户ID
     * @return 群列表
     */
    List<GroupInfoVO> getUserGroups(String userId);

    /**
     * 转让群主
     * @param transferDTO 转让信息
     * @param currentOwnerId 当前群主ID
     */
    void transferGroupOwner(GroupTransferDTO transferDTO, String currentOwnerId);

    /**
     * 退出群聊
     * @param groupId 群ID
     * @param userId 用户ID
     */
    void quitGroup(Long groupId, String userId);

    /**
     * 搜索群聊（按群名模糊匹配）
     * @param keyword 关键词
     * @param currentUserId 当前用户ID（用于判断申请状态）
     * @return 群列表
     */
    List<GroupInfoVO> searchGroup(String keyword, String currentUserId);
}
