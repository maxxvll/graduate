package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 添加群成员请求DTO
 * <p>
 * 用于添加群成员时的请求参数验证。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "添加群成员请求参数")
public class GroupMemberAddDTO {

    /**
     * 群ID
     */
    @Schema(description = "群ID", example = "3001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "群ID不能为空")
    private Long groupId;

    /**
     * 要添加的成员ID列表
     */
    @Schema(description = "要添加的成员ID列表", example = "[\"10001\", \"10002\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "成员ID列表不能为空")
    private List<String> userIds;

    /**
     * 添加方式：1-直接添加（管理员/群主），2-邀请加入
     */
    @Schema(description = "添加方式", example = "1", allowableValues = {"1", "2"})
    private Integer addType;
}