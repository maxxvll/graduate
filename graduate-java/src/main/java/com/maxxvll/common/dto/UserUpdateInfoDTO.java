package com.maxxvll.common.dto;

import lombok.Data;

@Data
public class UserUpdateInfoDTO {
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    // 【新增】用于接收前端传来的扩展信息（个性签名、地区等）
    private Object extInfo;
}