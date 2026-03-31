package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 云盘分享表
 * @TableName cloud_share
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_share")
public class CloudShare extends BaseEntity {

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 分享码（唯一标识）
     */
    private String shareCode;

    /**
     * 访问密码（可选）
     */
    private String password;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 下载次数
     */
    private Integer downloadCount;
}
