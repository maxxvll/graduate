package com.maxxvll.common.dto;

import lombok.Data;

@Data
public class TokenUserInfoDTO {
    /**
     * 用户唯一ID（主键）
     */
    private String id;

    /**
     * 用户名（登录用，唯一）
     */
    private String username;

    /**
     * 用户昵称（聊天展示用）
     */
    private String nickname;

    /**
     * 用户头像URL（OSS/服务器地址）
     */
    private String avatar;

    /**
     * 手机号（脱敏展示，如：138****1234）
     * 注意：Token中若无需传递手机号，可直接删除该字段
     */
    private String phone;

    /**
     * 邮箱（脱敏展示，如：xx***@xx.com）
     * 注意：Token中若无需传递邮箱，可直接删除该字段
     */
    private String email;

    /**
     * 用户状态：1-正常，2-禁用，3-注销
     * 用于鉴权时判断用户是否可用
     */
    private Integer status;
}
