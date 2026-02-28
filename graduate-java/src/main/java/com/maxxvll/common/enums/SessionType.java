package com.maxxvll.common.enums;

import lombok.Getter;

@Getter
public enum SessionType {
    SINGLE(1, "单聊"),
    GROUP(2, "群聊");

    private final Integer code;
    private final String desc;

    SessionType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static boolean isGroup(Integer code) {
        return GROUP.getCode().equals(code);
    }

    public static boolean isSingle(Integer code) {
        return SINGLE.getCode().equals(code);
    }
}