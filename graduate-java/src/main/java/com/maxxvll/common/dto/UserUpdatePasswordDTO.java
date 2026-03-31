package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求DTO
 * <p>
 * 用于用户修改密码时的请求参数验证。
 * 包含旧密码、新密码和确认新密码。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "修改密码请求参数")
public class UserUpdatePasswordDTO {

    /**
     * 旧密码
     */
    @Schema(description = "旧密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    /**
     * 新密码
     */
    @Schema(description = "新密码", example = "654321", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度必须在6-20个字符之间")
    private String newPassword;

    /**
     * 确认新密码
     */
    @Schema(description = "确认新密码", example = "654321", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "确认新密码不能为空")
    private String confirmNewPassword;
}