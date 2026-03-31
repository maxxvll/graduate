package com.maxxvll.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 二维码生成响应VO
 * <p>
 * 用于返回生成的二维码信息。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "二维码生成信息")
public class QrCodeGenerateVO {

    /**
     * 二维码唯一ID
     */
    @Schema(description = "二维码唯一ID")
    private String qrCodeId;

    /**
     * 二维码图片的Base64
     */
    @Schema(description = "二维码图片Base64", example = "data:image/png;base64,...")
    private String qrCodeBase64;
}
