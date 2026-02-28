package com.maxxvll.common.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.Date;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfoVO {
    private String id;
    private String username;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private Integer status;
    private Date createdAt;

    // 【新增】直接展现在VO里，方便前端使用
    private String signature; // 个性签名
    private String region;    // 地区

    // 【新增】原始扩展字段，用于兼容
    private Map<String, Object> extInfo;
}