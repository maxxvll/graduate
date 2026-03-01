package com.maxxvll.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    // 手动添加getter/setter方法
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getEmailCode() { return emailCode; }
    public void setEmailCode(String emailCode) { this.emailCode = emailCode; }
}