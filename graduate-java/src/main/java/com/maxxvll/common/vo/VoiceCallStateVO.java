package com.maxxvll.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 语音通话状态响应VO
 * <p>
 * 用于返回语音通话的实时状态信息。
 * 包含通话状态、双方用户信息、通话时间等。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "语音通话状态")
public class VoiceCallStateVO {

    /**
     * 通话状态：1-呼叫中，2-通话中，3-已结束
     */
    @Schema(description = "通话状态", example = "1")
    private Integer state;

    /**
     * 主叫用户ID
     */
    @Schema(description = "主叫用户ID")
    private String callerId;

    /**
     * 主叫用户昵称
     */
    @Schema(description = "主叫用户昵称")
    private String callerNickname;

    /**
     * 主叫用户头像URL
     */
    @Schema(description = "主叫用户头像URL")
    private String callerAvatar;

    /**
     * 被叫用户ID
     */
    @Schema(description = "被叫用户ID")
    private String calleeId;

    /**
     * 被叫用户昵称
     */
    @Schema(description = "被叫用户昵称")
    private String calleeNickname;

    /**
     * 被叫用户头像URL
     */
    @Schema(description = "被叫用户头像URL")
    private String calleeAvatar;

    /**
     * 通话开始时间
     */
    @Schema(description = "通话开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    /**
     * 通话结束时间
     */
    @Schema(description = "通话结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    /**
     * 通话时长（秒）
     */
    @Schema(description = "通话时长（秒）")
    private Long duration;
}
