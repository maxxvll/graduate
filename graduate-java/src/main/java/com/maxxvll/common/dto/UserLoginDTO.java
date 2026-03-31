package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户登录请求DTO
 * <p>
 * 用于用户登录时的请求参数验证。
 * 包含用户名、密码和设备类型信息。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "用户登录请求参数")
public class UserLoginDTO {

    /**
     * 用户名
     */
    @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
    private String username;

    /**
     * 密码
     */
    @Schema(description = "密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String password;

    /**
     * 设备类型（用于区分不同设备的登录状态）
     */
    @Schema(description = "设备类型", example = "web", allowableValues = {"web", "android", "ios", "h5", "mp-weixin"})
    private String deviceType;
}
