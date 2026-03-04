package com.maxxvll.common.dto;

import com.maxxvll.common.annotation.NotRequired;
import lombok.Data;

/**
 * 语音通话信令 DTO
 */
@Data
public class VoiceCallDTO {

    /**
     * 呼叫类型：1-发起呼叫，2-接听，3-拒绝，4-挂断，5-SDP 交换，6-ICE 交换
     */
    @NotRequired
    private String callType;

    /**
     * 被呼叫人 ID
     */
    private String targetId;

    /**
     * 主叫人 ID（发送方）
     */
    @NotRequired
    private String fromId;

    /**
     * 主叫人昵称（用于来电展示）
     */
    @NotRequired
    private String fromNickname;

    /**
     * 主叫人头像 URL（用于来电展示）
     */
    @NotRequired
    private String fromAvatar;

    /**
     * 会话 ID（用于标识一次通话）
     */
    @NotRequired
    private String sessionId;

    /**
     * SDP offer/answer（WebRTC 信令数据）
     */
    @NotRequired
    private String sdp;

    /**
     * ICE candidate（WebRTC ICE 候选）
     */
    @NotRequired
    private String candidate;

    /**
     * 扩展信息（如呼叫原因等）
     */
    @NotRequired
    private String extraInfo;

    /**
     * 通话模式：audio/video
     */
    @NotRequired
    private String mode;
}
