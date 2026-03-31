package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 云盘分享创建请求DTO
 * <p>
 * 用于创建云盘文件分享时的请求参数验证。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "云盘分享创建请求参数")
public class CloudShareCreateDTO {

    /**
     * 文件ID
     */
    @Schema(description = "文件ID", example = "file_001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "文件ID不能为空")
    private String fileId;

    /**
     * 访问密码
     */
    @Schema(description = "访问密码", example = "123456")
    private String password;

    /**
     * 有效期（天）
     */
    @Schema(description = "有效期（天）", example = "7")
    @Min(value = 1, message = "有效期必须大于0天")
    private Integer expireDays;
}
