package com.maxxvll.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserRegisterDTO {
    private String username;
    private String password;
    private String confirmPassword;
    private String nickname;
    private String phone;
    private String email;
    /** 前端字段名为 code，映射到后端 emailCode */
    @JsonProperty("code")
    private String emailCode;
}