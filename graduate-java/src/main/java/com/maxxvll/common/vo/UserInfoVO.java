package com.maxxvll.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户信息响应VO
 * <p>
 * 用于返回用户基本信息。
 * 包含用户ID、用户名、昵称、头像、邮箱等。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "用户信息")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfoVO {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private String id;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 昵称
     */
    @Schema(description = "昵称")
    private String nickname;

    /**
     * 头像URL
     */
    @Schema(description = "头像URL")
    private String avatar;

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱")
    private String email;

    /**
     * 用户状态：1-正常，2-禁用，3-注销
     */
    @Schema(description = "用户状态")
    private Integer status;

    /**
     * 注册时间
     */
    @Schema(description = "注册时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /**
     * 个性签名
     */
    @Schema(description = "个性签名")
    private String signature;

    /**
     * 地区
     */
    @Schema(description = "地区")
    private String region;

    /**
     * 扩展信息
     */
    @Schema(description = "扩展信息")
    private Map<String, Object> extInfo;
}