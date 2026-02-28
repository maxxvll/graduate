package com.maxxvll.common.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SessionVO {
    private String sessionId;
    private Integer sessionType; // 1-单聊 2-群聊
    private String targetId;
    private String sessionName;
    private String sessionAvatar;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private String lastMessageSenderId;
    private String lastMessageSenderName; // 用于显示"XX：XXX"
    private Integer unreadCount;
    private Boolean isTop;
    private Boolean isMute;
}