package com.maxxvll.common.dto;

import lombok.Data;

@Data
public class UserRegisterDTO {
    private String username;
    private String password;
    private String confirmPassword;
    private String nickname;
    private String phone;
    private String email;
    private String captchaKey;
    private String captchaCode;
}