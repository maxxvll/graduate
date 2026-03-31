package com.maxxvll.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.common.constants.ApplicationStatusConstants;
import com.maxxvll.common.constants.GroupConstants;
import com.maxxvll.common.constants.MemberConstants;
import com.maxxvll.common.dto.GroupApplyDTO;
import com.maxxvll.common.dto.GroupApplyHandleDTO;
import com.maxxvll.common.enums.MessageType;
import com.maxxvll.common.enums.SessionType;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.common.vo.GroupApplicationVO;
import com.maxxvll.domain.ChatGroup;
import com.maxxvll.domain.ChatGroupMember;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.domain.GroupApplication;
import com.maxxvll.mapper.ChatGroupMapper;
import com.maxxvll.mapper.ChatGroupMemberMapper;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.mapper.GroupApplicationMapper;
import com.maxxvll.service.ChatGroupMemberService;
import com.maxxvll.service.ChatMessageService;
import com.maxxvll.service.ChatSessionService;
import com.maxxvll.service.GroupApplicationService;
import com.maxxvll.utils.BeanConvertUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GroupApplicationServiceImpl extends ServiceImpl<GroupApplicationMapper, GroupApplication>
        implements GroupApplicationService {

    @Resource
    private ChatGroupMapper chatGroupMapper;
    @Resource
    private ChatGroupMemberMapper chatGroupMemberMapper;
    @Resource
    private ChatUserMapper chatUserMapper;
    @Resource
    private ChatGroupMemberService chatGroupMemberService;
    @Resource
    private ChatSessionService chatSessionService;
    @Resource
    private ChatMessageService chatMessageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyJoinGroup(GroupApplyDTO applyDTO, String applicantId) {
        if (applyDTO.getGroupId() == null) {
            throw new BusinessException("群ID不能为空");
        }

        ChatGroup group = chatGroupMapper.selectById(applyDTO.getGroupId());
        if (group == null || group.getStatus() == GroupConstants.Status.DISSOLVED) {
            throw new BusinessException("群聊不存在或已解散");
        }
        if (chatGroupMemberService.isGroupMember(applyDTO.getGroupId(), applicantId)) {
            throw new BusinessException("您已在该群聊中");
        }

        Long pendingCount = this.count(
                new LambdaQueryWrapper<GroupApplication>()
                        .eq(GroupApplication::getGroupId, applyDTO.getGroupId())
                        .eq(GroupApplication::getApplicantId, Long.valueOf(applicantId))
                        .eq(GroupApplication::getStatus, ApplicationStatusConstants.STATUS_PENDING)
        );
        if (pendingCount > 0) {
            throw new BusinessException("您已提交过申请，请等待审核");
        }

        if (group.getJoinType() == GroupConstants.JoinType.AUTO_JOIN) {
            addMemberToGroup(applyDTO.getGroupId(), applicantId, null);
            log.info("用户[{}]直接加入群聊[{}]（免审核）", applicantId, applyDTO.getGroupId());
            return;
        }
        if (group.getJoinType() == GroupConstants.JoinType.INVITE_ONLY) {
            throw new BusinessException("该群聊仅支持邀请加入");
        }

        GroupApplication application = new GroupApplication();
        application.setApplicantId(Long.valueOf(applicantId));
        application.setGroupId(applyDTO.getGroupId());
        application.setStatus(ApplicationStatusConstants.STATUS_PENDING);
        application.setCreateTime(new Date());
        application.setUpdateTime(new Date());
        this.save(application);
        log.info("用户[{}]申请加入群聊[{}]", applicantId, applyDTO.getGroupId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleApplication(GroupApplyHandleDTO handleDTO, String operatorId) {
        GroupApplication application = this.getById(handleDTO.getApplyId());
        if (application == null) {
            throw new BusinessException("申请记录不存在");
        }
        if (!application.getStatus().equals(ApplicationStatusConstants.STATUS_PENDING)) {
            throw new BusinessException("该申请已处理");
        }

        Integer role = chatGroupMemberService.getUserRole(application.getGroupId(), operatorId);
        if (role == null || role > GroupConstants.Role.ADMIN) {
            throw new BusinessException("无权限处理该申请");
        }

        application.setStatus(handleDTO.getStatus());
        application.setOperatorId(Long.valueOf(operatorId));
        if (handleDTO.getStatus() == ApplicationStatusConstants.STATUS_REJECTED) {
            application.setRejectReason(handleDTO.getRejectReason());
        }
        application.setUpdateTime(new Date());
        this.updateById(application);

        if (handleDTO.getStatus() == ApplicationStatusConstants.STATUS_APPROVED) {
            addMemberToGroup(application.getGroupId(), application.getApplicantId().toString(), operatorId);
            sendGroupJoinNotification(application.getGroupId(), application.getApplicantId());
        }

        log.info("用户[{}]处理申请[{}]，结果：[{}]", operatorId, handleDTO.getApplyId(),
                handleDTO.getStatus() != null && handleDTO.getStatus() == ApplicationStatusConstants.STATUS_APPROVED ? "通过" : "拒绝");
    }

    @Override
    public List<GroupApplicationVO> getGroupApplications(Long groupId, Integer status) {
        LambdaQueryWrapper<GroupApplication> wrapper = new LambdaQueryWrapper<GroupApplication>()
                .eq(GroupApplication::getGroupId, groupId)
                .orderByDesc(GroupApplication::getCreateTime);
        if (status != null) {
            wrapper.eq(GroupApplication::getStatus, status);
        }
        return convertToVOBatch(this.list(wrapper));
    }

    @Override
    public List<GroupApplicationVO> getUserApplications(String userId) {
        List<GroupApplication> applications = this.list(
                new LambdaQueryWrapper<GroupApplication>()
                        .eq(GroupApplication::getApplicantId, Long.valueOf(userId))
                        .orderByDesc(GroupApplication::getCreateTime)
        );
        return convertToVOBatch(applications);
    }

    @Override
    public List<GroupApplicationVO> getReceivedApplications(String userId) {
        List<ChatGroupMember> managedGroups = chatGroupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getUserId, userId)
                        .in(ChatGroupMember::getRole, List.of(GroupConstants.Role.OWNER, GroupConstants.Role.ADMIN))
                        .eq(ChatGroupMember::getIsQuit, 0)
        );
        if (managedGroups.isEmpty()) {
            return List.of();
        }

        List<Long> groupIds = managedGroups.stream()
                .map(ChatGroupMember::getGroupId)
                .distinct()
                .collect(Collectors.toList());

        List<GroupApplication> applications = this.list(
                new LambdaQueryWrapper<GroupApplication>()
                        .in(GroupApplication::getGroupId, groupIds)
                        .eq(GroupApplication::getStatus, ApplicationStatusConstants.STATUS_PENDING)
                        .orderByDesc(GroupApplication::getCreateTime)
        );
        return convertToVOBatch(applications);
    }

    @Override
    public void cancelApplication(Long applyId, String applicantId) {
        GroupApplication application = this.getById(applyId);
        if (application == null) {
            throw new BusinessException("申请记录不存在");
        }
        if (!application.getApplicantId().equals(Long.valueOf(applicantId))) {
            throw new BusinessException("只能撤回自己的申请");
        }
        if (!application.getStatus().equals(ApplicationStatusConstants.STATUS_PENDING)) {
            throw new BusinessException("该申请已处理，无法撤回");
        }

        this.removeById(applyId);
        log.info("用户[{}]撤回了申请[{}]", applicantId, applyId);
    }

    private void addMemberToGroup(Long groupId, String userId, String inviterId) {
        if (!chatGroupMemberMapper.selectActiveUserIdsByGroupIdAndUserIds(groupId, List.of(userId)).isEmpty()) {
            return;
        }

        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null || group.getStatus() == GroupConstants.Status.DISSOLVED) {
            throw new BusinessException("群聊不存在或已解散");
        }
        long currentCount = chatGroupMemberService.getMemberCount(groupId);
        if (currentCount >= group.getMaxMember()) {
            throw new BusinessException("群成员数量已达到上限");
        }

        chatGroupMemberMapper.batchUpsertMembers(List.of(buildGroupMember(groupId, userId, inviterId, GroupConstants.Role.MEMBER)));
    }

    private ChatGroupMember buildGroupMember(Long groupId, String userId, String inviterId, Integer role) {
        Date now = new Date();
        ChatGroupMember member = new ChatGroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(role);
        member.setJoinTime(now);
        member.setInviterId(inviterId);
        member.setIsMute(MemberConstants.MuteStatus.UNMUTED);
        member.setIsQuit(MemberConstants.QuitStatus.ACTIVE);
        member.setQuitTime(null);
        member.setQuitReason(null);
        member.setCreateTime(now);
        member.setUpdateTime(now);
        return member;
    }

    private void sendGroupJoinNotification(Long groupId, Long applicantId) {
        String groupIdStr = String.valueOf(groupId);
        String applicantIdStr = String.valueOf(applicantId);
        String sessionId = "group_" + groupIdStr;

        ChatGroup group = chatGroupMapper.selectById(groupId);
        ChatUser applicant = chatUserMapper.selectById(applicantId);
        if (group == null || applicant == null) {
            return;
        }

        String content = applicant.getNickname() + " 加入了群聊";
        chatSessionService.initGroupMemberSession(
                applicantIdStr,
                groupIdStr,
                sessionId,
                group.getGroupName(),
                group.getGroupAvatar()
        );

        ChatMessage sysMsg = chatMessageService.saveDirectly(
                sessionId,
                groupIdStr,
                groupIdStr,
                SessionType.GROUP.getCode(),
                MessageType.SYSTEM.getCode(),
                content
        );
        chatSessionService.refreshAllLastMessage(sysMsg);
        log.info("群入群系统通知已发送，sessionId={}, 内容={}", sessionId, content);
    }

    private List<GroupApplicationVO> convertToVOBatch(List<GroupApplication> applications) {
        if (applications == null || applications.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Long> groupIds = applications.stream()
                .map(GroupApplication::getGroupId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<String> userIds = applications.stream()
                .flatMap(application -> application.getOperatorId() == null
                        ? java.util.stream.Stream.of(application.getApplicantId())
                        : java.util.stream.Stream.of(application.getApplicantId(), application.getOperatorId()))
                .map(String::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, ChatGroup> groupMap = loadGroups(groupIds);
        Map<String, ChatUser> userMap = loadUsers(userIds);

        return applications.stream()
                .map(application -> {
                    GroupApplicationVO vo = BeanConvertUtil.convert(application, GroupApplicationVO.class);
                    vo.setStatusDesc(getStatusDesc(application.getStatus()));

                    ChatGroup group = groupMap.get(application.getGroupId());
                    if (group != null) {
                        vo.setGroupName(group.getGroupName());
                    }

                    ChatUser applicant = userMap.get(String.valueOf(application.getApplicantId()));
                    if (applicant != null) {
                        vo.setApplicantNickname(applicant.getNickname());
                        vo.setApplicantAvatar(applicant.getAvatar());
                    }

                    if (application.getOperatorId() != null) {
                        ChatUser operator = userMap.get(String.valueOf(application.getOperatorId()));
                        if (operator != null) {
                            vo.setOperatorNickname(operator.getNickname());
                        }
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private Map<Long, ChatGroup> loadGroups(Collection<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Map.of();
        }

        return chatGroupMapper.selectList(
                        new LambdaQueryWrapper<ChatGroup>()
                                .select(ChatGroup::getId, ChatGroup::getGroupName)
                                .in(ChatGroup::getId, groupIds)
                ).stream()
                .collect(Collectors.toMap(ChatGroup::getId, group -> group, (left, right) -> left));
    }

    private Map<String, ChatUser> loadUsers(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        return chatUserMapper.selectList(
                        new LambdaQueryWrapper<ChatUser>()
                                .select(ChatUser::getId, ChatUser::getNickname, ChatUser::getAvatar)
                                .in(ChatUser::getId, userIds)
                ).stream()
                .collect(Collectors.toMap(ChatUser::getId, user -> user, (left, right) -> left));
    }

    private String getStatusDesc(Integer status) {
        return ApplicationStatusConstants.getStatusDesc(status);
    }
}
