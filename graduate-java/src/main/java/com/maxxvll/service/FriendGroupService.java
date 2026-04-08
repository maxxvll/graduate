package com.maxxvll.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxxvll.common.vo.FriendGroupVO;
import com.maxxvll.domain.FriendGroup;

import java.util.List;

/**
 * 好友分组服务接口
 * @author 20570
 */
public interface FriendGroupService extends IService<FriendGroup> {

    /**
     * 获取用户的好友分组列表
     */
    List<FriendGroupVO> getGroupList(String userId);

    /**
     * 创建好友分组
     */
    FriendGroup createGroup(String userId, String groupName);

    /**
     * 更新好友分组名称
     */
    void updateGroup(Long groupId, String userId, String newName);

    /**
     * 删除好友分组（好友自动移入默认分组）
     */
    void deleteGroup(Long groupId, String userId);

    /**
     * 移动好友到指定分组
     */
    void moveFriendToGroup(String userId, Long friendUserId, Long groupId);

    /**
     * 确保用户有默认分组
     */
    FriendGroup ensureDefaultGroup(Long userId);

    /**
     * 获取用户的默认分组
     */
    FriendGroup getDefaultGroup(Long userId);
}
