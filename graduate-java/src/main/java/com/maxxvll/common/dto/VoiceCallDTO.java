package com.maxxvll.common.dto;

import com.maxxvll.common.annotation.NotRequired;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 语音通话信令请求DTO
 * <p>
 * 用于WebRTC语音通话的信令交互。
 * 包含呼叫类型、目标用户ID、SDP、ICE候选等WebRTC相关参数。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "语音通话信令请求参数")
public class VoiceCallDTO {

    /**
     * 呼叫类型：1-发起呼叫，2-接听，3-拒绝，4-挂断，5-SDP交换，6-ICE交换
     */
    @Schema(description = "呼叫类型", example = "1", allowableValues = {"1", "2", "3", "4", "5", "6"})
    @Pattern(regexp = "^[1-6]$", message = "呼叫类型必须为1-6之间的整数")
    @NotRequired
    private String callType;

    /**
     * 被呼叫人ID
     */
    @Schema(description = "被呼叫人ID", example = "10001")
    private String targetId;

    /**
     * 主叫人ID（发送方）
     */
    @Schema(description = "主叫人ID", example = "10002")
    @NotRequired
    private String fromId;

    /**
     * 主叫人昵称（用于来电展示）
     */
    @Schema(description = "主叫人昵称", example = "张三")
    @NotRequired
    private String fromNickname;

    /**
     * 主叫人头像URL（用于来电展示）
     */
    @Schema(description = "主叫人头像URL", example = "https://example.com/avatar.jpg")
    @NotRequired
    private String fromAvatar;

    /**
     * 会话ID（用于标识一次通话）
     */
    @Schema(description = "会话ID", example = "call_session_001")
    @NotRequired
    private String sessionId;

    /**
     * SDP offer/answer（WebRTC信令数据）
     */
    @Schema(description = "SDP offer/answer")
    @NotRequired
    private String sdp;

    /**
     * ICE candidate（WebRTC ICE候选）
     */
    @Schema(description = "ICE candidate")
    @NotRequired
    private String candidate;

    /**
     * 扩展信息（如呼叫原因等）
     */
    @Schema(description = "扩展信息", example = "有急事找你")
    @Size(max = 200, message = "扩展信息不能超过200个字符")
    @NotRequired
    private String extraInfo;

    /**
     * 通话模式：audio/video
     */
    @Schema(description = "通话模式", example = "audio", allowableValues = {"audio", "video"})
    @Pattern(regexp = "^(audio|video)$", message = "通话模式必须为audio或video")
    @NotRequired
    private String mode;

    /**
     * 媒体传输方式：webrtc/stream
     */
    @Schema(description = "媒体传输方式", example = "webrtc", allowableValues = {"webrtc", "stream"})
    @Pattern(regexp = "^(webrtc|stream)$", message = "媒体传输方式必须为webrtc或stream")
    @NotRequired
    private String transport;
}
