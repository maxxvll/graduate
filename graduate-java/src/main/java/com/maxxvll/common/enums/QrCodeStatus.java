package com.maxxvll.common.enums;

import lombok.Getter;

@Getter
public enum QrCodeStatus {
    WAITING("waiting", "待扫描"),
    SCANNED("scanned", "已扫描，待确认"),
    CONFIRMED("confirmed", "已确认"),
    EXPIRED("expired", "已过期");

    private final String code;
    private final String desc;

    QrCodeStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
