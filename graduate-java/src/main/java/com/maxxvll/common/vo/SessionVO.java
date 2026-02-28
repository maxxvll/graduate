package com.maxxvll.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionVO {
    private String sessionId;
    private Integer sessionType; // 1-单聊 2-群聊
    private String targetId;
    private String sessionName;
    private String sessionAvatar;
    private String lastMessageContent;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastMessageTime;
    private String lastMessageSenderId;
    private String lastMessageSenderName; // 用于显示"XX：XXX"
    private Integer unreadCount;
    private Integer isTop;  // 0-否 1-是
    private Integer isMute; // 0-否 1-是
}