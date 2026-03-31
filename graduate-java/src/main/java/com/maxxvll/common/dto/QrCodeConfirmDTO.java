package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 二维码登录确认请求DTO
 * <p>
 * 用于二维码登录确认时的请求参数验证。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "二维码登录确认请求参数")
public class QrCodeConfirmDTO {

    /**
     * 二维码ID
     */
    @Schema(description = "二维码ID", example = "qrcode_123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "二维码ID不能为空")
    private String qrCodeId;
}
