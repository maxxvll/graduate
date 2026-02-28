package com.maxxvll.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxxvll.common.dto.FriendApplyDTO;
import com.maxxvll.common.dto.FriendApplyHandleDTO;
import com.maxxvll.common.vo.FriendApplicationVO;
import com.maxxvll.domain.FriendApplication;

import java.util.List;

/**
 * @author 20570
 * @description 针对表【friend_application(好友申请表)】的数据库操作Service
 * @createDate 2026-02-19 12:02:21
 */
public interface FriendApplicationService extends IService<FriendApplication> {

    /**
     * 发送好友申请
     */
    void applyFriend(FriendApplyDTO applyDTO, String applicantId);

    /**
     * 获取收到的好友申请列表（我是被申请人）
     */
    List<FriendApplicationVO> getReceivedApplications(String userId);

    /**
     * 获取发出的好友申请列表（我是申请人）
     */
    List<FriendApplicationVO> getSentApplications(String userId);

    /**
     * 处理好友申请（接受/拒绝）
     */
    void handleApplication(FriendApplyHandleDTO handleDTO, String handlerId);

    /**
     * 获取好友列表（已互相接受申请的用户）
     */
    List<FriendApplicationVO> getFriendList(String userId);
}
