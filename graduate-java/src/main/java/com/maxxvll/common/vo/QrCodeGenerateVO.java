package com.maxxvll.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCodeGenerateVO {
    /** 二维码唯一ID */
    private String qrCodeId;
    /** 二维码图片的Base64 (或者直接返回图片流，这里为了结合轮询，返回ID+Base64) */
    private String qrCodeBase64;
}
