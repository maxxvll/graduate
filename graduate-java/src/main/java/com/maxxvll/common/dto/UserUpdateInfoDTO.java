package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户信息更新请求DTO
 * <p>
 * 用于用户更新个人信息时的请求参数。
 * 包含昵称、头像、手机号、邮箱和扩展信息。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "用户信息更新请求参数")
public class UserUpdateInfoDTO {

    /**
     * 昵称
     */
    @Schema(description = "昵称", example = "新昵称")
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    /**
     * 头像URL
     */
    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    @Size(max = 500, message = "头像URL长度不能超过500个字符")
    private String avatar;

    /**
     * 手机号
     */
    @Schema(description = "手机号", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱", example = "user@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 扩展信息（个性签名、地区等）
     */
    @Schema(description = "扩展信息", example = "{\"signature\":\"这是我的个性签名\",\"region\":\"北京\"}")
    private Object extInfo;
}