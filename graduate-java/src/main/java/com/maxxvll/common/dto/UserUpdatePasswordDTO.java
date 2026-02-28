// 修改密码DTO
package com.maxxvll.common.dto;

import lombok.Data;

@Data
public class UserUpdatePasswordDTO {
    private String oldPassword;
    private String newPassword;
    private String confirmNewPassword;
}