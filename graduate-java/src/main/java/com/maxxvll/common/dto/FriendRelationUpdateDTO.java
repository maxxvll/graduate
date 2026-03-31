package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 好友关系更新请求DTO
 * <p>
 * 用于更新好友关系时的请求参数验证。
 * 包含好友ID、备注名、标签、权限范围和是否星标。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "好友关系更新请求参数")
public class FriendRelationUpdateDTO {

    /**
     * 好友用户ID
     */
    @Schema(description = "好友用户ID", example = "10001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "好友ID不能为空")
    private String friendUserId;

    /**
     * 备注名
     */
    @Schema(description = "备注名", example = "张三")
    @Size(max = 64, message = "备注不能超过64个字符")
    private String remarkName;

    /**
     * 标签
     */
    @Schema(description = "标签", example = "同事")
    @Size(max = 64, message = "标签不能超过64个字符")
    private String tagName;

    /**
     * 权限范围：0-完全可见，1-仅好友可见，2-仅自己可见
     */
    @Schema(description = "权限范围", example = "0", allowableValues = {"0", "1", "2"})
    @Min(value = 0, message = "朋友权限取值无效")
    @Max(value = 2, message = "朋友权限取值无效")
    private Integer permissionScope;

    /**
     * 是否星标
     */
    @Schema(description = "是否星标", example = "true")
    private Boolean starred;
}
