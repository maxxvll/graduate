package com.maxxvll.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.common.dto.GroupApplyDTO;
import com.maxxvll.common.dto.GroupApplyHandleDTO;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.utils.BeanConvertUtil;
import com.maxxvll.common.vo.GroupApplicationVO;
import com.maxxvll.domain.ChatGroup;
import com.maxxvll.domain.ChatGroupMember;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.domain.GroupApplication;
import com.maxxvll.mapper.ChatGroupMapper;
import com.maxxvll.mapper.ChatGroupMemberMapper;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.mapper.GroupApplicationMapper;
import com.maxxvll.service.ChatGroupMemberService;
import com.maxxvll.service.GroupApplicationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 20570
* @description 针对表【group_application(群申请表)】的数据库操作Service实现
* @createDate 2026-02-19 12:02:21
*/
@Service
@Slf4j
public class GroupApplicationServiceImpl extends ServiceImpl<GroupApplicationMapper, GroupApplication>
    implements GroupApplicationService{

    @Resource
    private ChatGroupMapper chatGroupMapper;

    @Resource
    private ChatGroupMemberMapper chatGroupMemberMapper;

    @Resource
    private ChatUserMapper chatUserMapper;

    @Resource
    private ChatGroupMemberService chatGroupMemberService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyJoinGroup(GroupApplyDTO applyDTO, String applicantId) {
        // 1. 检查群是否存在
        ChatGroup group = chatGroupMapper.selectById(applyDTO.getGroupId());
        if (group == null || group.getStatus() == 2) {
            throw new BusinessException("群聊不存在或已解散");
        }

        // 2. 检查是否已在群中
        if (chatGroupMemberService.isGroupMember(applyDTO.getGroupId(), applicantId)) {
            throw new BusinessException("您已在该群聊中");
        }

        // 3. 检查是否有待处理的申请
        Long pendingCount = this.count(
                new LambdaQueryWrapper<GroupApplication>()
                        .eq(GroupApplication::getGroupId, applyDTO.getGroupId())
                        .eq(GroupApplication::getApplicantId, Long.valueOf(applicantId))
                        .eq(GroupApplication::getStatus, 0)
        );
        if (pendingCount > 0) {
            throw new BusinessException("您已提交过申请，请等待审核");
        }

        // 4. 根据加群方式处理
        if (group.getJoinType() == 2) {
            // 免审核，直接加入
            addMemberToGroup(applyDTO.getGroupId(), applicantId, null);
            log.info("用户[{}]直接加入群聊[{}]（免审核）", applicantId, applyDTO.getGroupId());
            return;
        }

        if (group.getJoinType() == 3) {
            throw new BusinessException("该群聊仅支持邀请加入");
        }

        // 5. 创建申请记录（需审核）
        GroupApplication application = new GroupApplication();
        application.setApplicantId(Long.valueOf(applicantId));
        application.setGroupId(Long.valueOf(applyDTO.getGroupId()));
        application.setStatus(0);
        application.setCreateTime(new Date());
        application.setUpdateTime(new Date());

        this.save(application);
        log.info("用户[{}]申请加入群聊[{}]", applicantId, applyDTO.getGroupId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleApplication(GroupApplyHandleDTO handleDTO, String operatorId) {
        // 1. 获取申请记录
        GroupApplication application = this.getById(handleDTO.getApplyId());
        if (application == null) {
            throw new BusinessException("申请记录不存在");
        }

        // 2. 检查是否已处理
        if (application.getStatus() != 0) {
            throw new BusinessException("该申请已处理");
        }

        // 3. 检查操作人权限
        Integer role = chatGroupMemberService.getUserRole(String.valueOf(application.getGroupId()), operatorId);
        if (role == null || role > 2) {
            throw new BusinessException("无权限处理该申请");
        }

        // 4. 更新申请状态
        application.setStatus(handleDTO.getStatus());
        application.setOperatorId(Long.valueOf(operatorId));
        if (handleDTO.getStatus() == 2) {
            application.setRejectReason(handleDTO.getRejectReason());
        }
        application.setUpdateTime(new Date());
        this.updateById(application);

        // 5. 如果通过，添加成员
        if (handleDTO.getStatus() == 1) {
            addMemberToGroup(String.valueOf(application.getGroupId()), String.valueOf(application.getApplicantId()), operatorId);
        }

        log.info("用户[{}]处理申请[{}]，结果：[{}]", operatorId, handleDTO.getApplyId(), handleDTO.getStatus() == 1 ? "通过" : "拒绝");
    }

    @Override
    public List<GroupApplicationVO> getGroupApplications(String groupId, Integer status) {
        LambdaQueryWrapper<GroupApplication> wrapper = new LambdaQueryWrapper<GroupApplication>()
                .eq(GroupApplication::getGroupId, Long.valueOf(groupId))
                .orderByDesc(GroupApplication::getCreateTime);

        if (status != null) {
            wrapper.eq(GroupApplication::getStatus, status);
        }

        List<GroupApplication> applications = this.list(wrapper);
        return applications.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<GroupApplicationVO> getUserApplications(String userId) {
        List<GroupApplication> applications = this.list(
                new LambdaQueryWrapper<GroupApplication>()
                        .eq(GroupApplication::getApplicantId, Long.valueOf(userId))
                        .orderByDesc(GroupApplication::getCreateTime)
        );
        return applications.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<GroupApplicationVO> getReceivedApplications(String userId) {
        // 获取用户管理的所有群聊
        List<ChatGroupMember> managedGroups = chatGroupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getUserId, userId)
                        .in(ChatGroupMember::getRole, List.of(1, 2))
                        .eq(ChatGroupMember::getIsQuit, 0)
        );

        if (managedGroups.isEmpty()) {
            return List.of();
        }

        List<Long> groupIds = managedGroups.stream()
                .map(m -> Long.valueOf(m.getGroupId()))
                .collect(Collectors.toList());

        List<GroupApplication> applications = this.list(
                new LambdaQueryWrapper<GroupApplication>()
                        .in(GroupApplication::getGroupId, groupIds)
                        .eq(GroupApplication::getStatus, 0)
                        .orderByDesc(GroupApplication::getCreateTime)
        );

        return applications.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public void cancelApplication(Long applyId, String applicantId) {
        GroupApplication application = this.getById(applyId);
        if (application == null) {
            throw new BusinessException("申请记录不存在");
        }

        // 只能撤回自己的申请
        if (!application.getApplicantId().equals(Long.valueOf(applicantId))) {
            throw new BusinessException("只能撤回自己的申请");
        }

        // 只能撤回待处理的申请
        if (application.getStatus() != 0) {
            throw new BusinessException("该申请已处理，无法撤回");
        }

        this.removeById(applyId);
        log.info("用户[{}]撤回了申请[{}]", applicantId, applyId);
    }

    /**
     * 添加成员到群聊
     */
    private void addMemberToGroup(String groupId, String userId, String inviterId) {
        // 检查群成员上限
        ChatGroup group = chatGroupMapper.selectById(groupId);
        long currentCount = chatGroupMemberService.getMemberCount(groupId);
        if (currentCount >= group.getMaxMember()) {
            throw new BusinessException("群成员数量已达上限");
        }

        ChatGroupMember member = new ChatGroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(3); // 普通成员
        member.setJoinTime(new Date());
        member.setInviterId(inviterId);
        member.setIsMute(0);
        member.setIsQuit(0);
        member.setCreatedAt(new Date());
        member.setUpdatedAt(new Date());

        chatGroupMemberMapper.insert(member);
    }

    /**
     * 转换为VO
     */
    private GroupApplicationVO convertToVO(GroupApplication application) {
        // 使用BeanConvertUtil转换基础字段
        GroupApplicationVO vo = BeanConvertUtil.convert(application, GroupApplicationVO.class);

        // 设置扩展字段（关联查询）
        vo.setStatusDesc(getStatusDesc(application.getStatus()));

        // 获取群信息
        ChatGroup group = chatGroupMapper.selectById(application.getGroupId());
        if (group != null) {
            vo.setGroupName(group.getGroupName());
        }

        // 获取申请人信息
        ChatUser applicant = chatUserMapper.selectById(application.getApplicantId());
        if (applicant != null) {
            vo.setApplicantNickname(applicant.getNickname());
            vo.setApplicantAvatar(applicant.getAvatar());
        }

        // 获取操作人信息
        if (application.getOperatorId() != null) {
            ChatUser operator = chatUserMapper.selectById(application.getOperatorId());
            if (operator != null) {
                vo.setOperatorNickname(operator.getNickname());
            }
        }

        return vo;
    }

    /**
     * 获取状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待处理";
            case 1 -> "已通过";
            case 2 -> "已拒绝";
            default -> "未知";
        };
    }
}




