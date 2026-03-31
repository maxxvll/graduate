package com.maxxvll.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码响应VO
 * <p>
 * 用于返回图形验证码信息。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "验证码信息")
public class CaptchaVO {

    /**
     * 验证码Key（用于提交时校验）
     */
    @Schema(description = "验证码Key")
    private String captchaKey;

    /**
     * 验证码图片Base64
     */
    @Schema(description = "验证码图片Base64", example = "data:image/png;base64,...")
    private String captchaBase64;
}
