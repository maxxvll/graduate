package com.maxxvll.service;

import com.maxxvll.common.dto.GroupApplyDTO;
import com.maxxvll.common.dto.GroupApplyHandleDTO;
import com.maxxvll.common.vo.GroupApplicationVO;
import com.maxxvll.domain.GroupApplication;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 20570
* @description 针对表【group_application(群申请表)】的数据库操作Service
* @createDate 2026-02-19 12:02:21
*/
public interface GroupApplicationService extends IService<GroupApplication> {

    /**
     * 申请加入群聊
     * @param applyDTO 申请信息
     * @param applicantId 申请人ID
     */
    void applyJoinGroup(GroupApplyDTO applyDTO, String applicantId);

    /**
     * 处理群申请
     * @param handleDTO 处理信息
     * @param operatorId 操作人ID
     */
    void handleApplication(GroupApplyHandleDTO handleDTO, String operatorId);

    /**
     * 获取群申请列表（管理员/群主查看）
     * @param groupId 群ID
     * @param status 状态：0-待处理，1-已通过，2-已拒绝，null-全部
     * @return 申请列表
     */
    List<GroupApplicationVO> getGroupApplications(Long groupId, Integer status);

    /**
     * 获取用户发送的申请列表
     * @param userId 用户ID
     * @return 申请列表
     */
    List<GroupApplicationVO> getUserApplications(String userId);

    /**
     * 获取用户收到的申请列表（作为群主/管理员）
     * @param userId 用户ID
     * @return 申请列表
     */
    List<GroupApplicationVO> getReceivedApplications(String userId);

    /**
     * 撤回申请
     * @param applyId 申请ID
     * @param applicantId 申请人ID
     */
    void cancelApplication(Long applyId, String applicantId);
}
