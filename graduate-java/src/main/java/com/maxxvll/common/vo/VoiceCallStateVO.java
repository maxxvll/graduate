package com.maxxvll.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 语音通话状态 VO
 */
@Data
public class VoiceCallStateVO {

    /**
     * 通话状态：1-呼叫中，2-通话中，3-已结束
     */
    private Integer state;

    /**
     * 主叫用户 ID
     */
    private String callerId;

    /**
     * 主叫用户昵称
     */
    private String callerNickname;

    /**
     * 主叫用户头像
     */
    private String callerAvatar;

    /**
     * 被叫用户 ID
     */
    private String calleeId;

    /**
     * 被叫用户昵称
     */
    private String calleeNickname;

    /**
     * 被叫用户头像
     */
    private String calleeAvatar;

    /**
     * 通话开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    /**
     * 通话结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    /**
     * 通话时长（秒）
     */
    private Long duration;
}
