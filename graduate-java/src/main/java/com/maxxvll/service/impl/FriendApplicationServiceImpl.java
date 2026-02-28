package com.maxxvll.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.common.dto.FriendApplyDTO;
import com.maxxvll.common.dto.FriendApplyHandleDTO;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.common.vo.FriendApplicationVO;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.domain.FriendApplication;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.mapper.FriendApplicationMapper;
import com.maxxvll.service.FriendApplicationService;
import com.maxxvll.utils.BeanConvertUtil;
import com.maxxvll.utils.MinioUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 20570
 * @description 针对表【friend_application(好友申请表)】的数据库操作Service实现
 * @createDate 2026-02-19 12:02:21
 */
@Slf4j
@Service
public class FriendApplicationServiceImpl extends ServiceImpl<FriendApplicationMapper, FriendApplication>
        implements FriendApplicationService {

    @Resource
    private ChatUserMapper chatUserMapper;
    @Resource
    private MinioUtil minioUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyFriend(FriendApplyDTO applyDTO, String applicantId) {
        String targetId = applyDTO.getTargetId();

        // 1. 目标用户是否存在
        ChatUser targetUser = chatUserMapper.selectById(targetId);
        if (targetUser == null || targetUser.getStatus() != 1) {
            throw new BusinessException("目标用户不存在");
        }

        // 2. 不能向自己发申请
        if (targetId.equals(applicantId)) {
            throw new BusinessException("不能向自己发送好友申请");
        }

        // 3. 检查是否已是好友（双方已有通过的申请）
        boolean alreadyFriend = isAlreadyFriend(applicantId, targetId);
        if (alreadyFriend) {
            throw new BusinessException("对方已是您的好友");
        }

        // 4. 检查是否已有待处理的申请
        FriendApplication existing = this.getOne(
                new LambdaQueryWrapper<FriendApplication>()
                        .eq(FriendApplication::getApplicantId, Long.valueOf(applicantId))
                        .eq(FriendApplication::getTargetUserId, Long.valueOf(targetId))
                        .eq(FriendApplication::getStatus, 0)
        );
        if (existing != null) {
            throw new BusinessException("已存在待处理的好友申请，请勿重复申请");
        }

        // 5. 创建申请记录
        FriendApplication application = new FriendApplication();
        application.setApplicantId(Long.valueOf(applicantId));
        application.setTargetUserId(Long.valueOf(targetId));
        application.setStatus(0);
        application.setCreateTime(new Date());
        application.setUpdateTime(new Date());

        this.save(application);
        log.info("好友申请创建成功，applicantId={}, targetId={}", applicantId, targetId);
    }

    @Override
    public List<FriendApplicationVO> getReceivedApplications(String userId) {
        List<FriendApplication> applications = this.list(
                new LambdaQueryWrapper<FriendApplication>()
                        .eq(FriendApplication::getTargetUserId, Long.valueOf(userId))
                        .orderByDesc(FriendApplication::getCreateTime)
        );
        return applications.stream().map(app -> buildVO(app, false)).collect(Collectors.toList());
    }

    @Override
    public List<FriendApplicationVO> getSentApplications(String userId) {
        List<FriendApplication> applications = this.list(
                new LambdaQueryWrapper<FriendApplication>()
                        .eq(FriendApplication::getApplicantId, Long.valueOf(userId))
                        .orderByDesc(FriendApplication::getCreateTime)
        );
        return applications.stream().map(app -> buildVO(app, true)).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleApplication(FriendApplyHandleDTO handleDTO, String handlerId) {
        // 1. 获取申请记录
        FriendApplication application = this.getById(handleDTO.getApplyId());
        if (application == null) {
            throw new BusinessException("申请记录不存在");
        }

        // 2. 验证是否是被申请人操作
        if (!application.getTargetUserId().equals(Long.valueOf(handlerId))) {
            throw new BusinessException("无权限处理该申请");
        }

        // 3. 检查是否已处理
        if (application.getStatus() != 0) {
            throw new BusinessException("该申请已处理");
        }

        // 4. 更新申请状态
        application.setStatus(handleDTO.getStatus());
        if (handleDTO.getStatus() == 2 && StrUtil.isNotBlank(handleDTO.getRejectReason())) {
            application.setRejectReason(handleDTO.getRejectReason());
        }
        application.setUpdateTime(new Date());
        this.updateById(application);

        log.info("用户[{}]处理好友申请[{}]，结果：{}",
                handlerId, handleDTO.getApplyId(), handleDTO.getStatus() == 1 ? "接受" : "拒绝");
    }

    @Override
    public List<FriendApplicationVO> getFriendList(String userId) {
        Long userIdLong = Long.valueOf(userId);

        // 查询已通过的好友申请（双方向）
        List<FriendApplication> accepted = this.list(
                new LambdaQueryWrapper<FriendApplication>()
                        .eq(FriendApplication::getStatus, 1)
                        .and(w -> w.eq(FriendApplication::getApplicantId, userIdLong)
                                   .or().eq(FriendApplication::getTargetUserId, userIdLong))
        );

        List<FriendApplicationVO> result = new ArrayList<>();
        for (FriendApplication app : accepted) {
            // 取对方信息
            Long otherId = app.getApplicantId().equals(userIdLong)
                    ? app.getTargetUserId()
                    : app.getApplicantId();

            ChatUser other = chatUserMapper.selectById(otherId);
            if (other == null) continue;

            FriendApplicationVO vo = new FriendApplicationVO();
            vo.setId(app.getId());
            vo.setApplicantId(String.valueOf(otherId));
            vo.setApplicantNickname(other.getNickname());
            vo.setApplicantAvatar(buildAvatarUrl(other.getAvatar()));
            vo.setStatus(1);
            vo.setCreateTime(app.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    // ==================== 私有方法 ====================

    private FriendApplicationVO buildVO(FriendApplication app, boolean isSentByMe) {
        FriendApplicationVO vo = BeanConvertUtil.convert(app, FriendApplicationVO.class);
        vo.setStatusDesc(getStatusDesc(app.getStatus()));

        // 设置申请人信息
        ChatUser applicant = chatUserMapper.selectById(app.getApplicantId());
        if (applicant != null) {
            vo.setApplicantNickname(applicant.getNickname());
            vo.setApplicantAvatar(buildAvatarUrl(applicant.getAvatar()));
        }

        // 设置被申请人信息
        ChatUser target = chatUserMapper.selectById(app.getTargetUserId());
        if (target != null) {
            vo.setTargetNickname(target.getNickname());
            vo.setTargetAvatar(buildAvatarUrl(target.getAvatar()));
        }

        return vo;
    }

    private String buildAvatarUrl(String avatar) {
        if (StrUtil.isBlank(avatar) || avatar.startsWith("http")) {
            return avatar;
        }
        return minioUtil.getAvatarUrl(avatar);
    }

    private boolean isAlreadyFriend(String userId1, String userId2) {
        Long id1 = Long.valueOf(userId1);
        Long id2 = Long.valueOf(userId2);
        return this.count(
                new LambdaQueryWrapper<FriendApplication>()
                        .eq(FriendApplication::getStatus, 1)
                        .and(w -> w.eq(FriendApplication::getApplicantId, id1)
                                   .eq(FriendApplication::getTargetUserId, id2)
                                   .or()
                                   .eq(FriendApplication::getApplicantId, id2)
                                   .eq(FriendApplication::getTargetUserId, id1))
        ) > 0;
    }

    private String getStatusDesc(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待处理";
            case 1 -> "已通过";
            case 2 -> "已拒绝";
            default -> "未知";
        };
    }
}





