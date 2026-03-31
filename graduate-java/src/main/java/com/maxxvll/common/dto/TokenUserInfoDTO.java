package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Token用户信息DTO
 * <p>
 * 用于存储在JWT Token中的用户信息。
 * 包含用户基本信息，不包含敏感数据。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "Token用户信息")
public class TokenUserInfoDTO {

    /**
     * 用户唯一ID（主键）
     */
    @Schema(description = "用户ID", example = "10001")
    private String id;

    /**
     * 用户名（登录用，唯一）
     */
    @Schema(description = "用户名", example = "admin")
    private String username;

    /**
     * 用户昵称（聊天展示用）
     */
    @Schema(description = "昵称", example = "管理员")
    private String nickname;

    /**
     * 用户头像URL（OSS/服务器地址）
     */
    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    /**
     * 手机号（脱敏展示）
     * <p>注意：Token中若无需传递手机号，可直接删除该字段</p>
     */
    @Schema(description = "手机号（脱敏）", example = "138****1234")
    private String phone;

    /**
     * 邮箱（脱敏展示）
     * <p>注意：Token中若无需传递邮箱，可直接删除该字段</p>
     */
    @Schema(description = "邮箱（脱敏）", example = "us***@example.com")
    private String email;

    /**
     * 用户状态：1-正常，2-禁用，3-注销
     * <p>用于鉴权时判断用户是否可用</p>
     */
    @Schema(description = "用户状态", example = "1", allowableValues = {"1", "2", "3"})
    private Integer status;
}
