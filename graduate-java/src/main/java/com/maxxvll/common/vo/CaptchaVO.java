package com.maxxvll.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVO {
    /** 验证码 Key (用于提交时校验) */
    private String captchaKey;
    /** 验证码图片 Base64 */
    private String captchaBase64;
}
