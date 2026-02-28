package com.maxxvll.common.enums;

import lombok.Getter;

@Getter
public enum MessageStatus {
    SEND_SUCCESS(1, "发送成功"),
    READ(2, "已读"),
    REVOKED(3, "已撤回"),
    DELETED(4, "已删除"),
    SEND_FAILED(5, "发送失败");

    private final Integer code;
    private final String desc;

    MessageStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}