package com.maxxvll.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 云盘分享信息响应VO
 * <p>
 * 用于返回云盘文件分享信息。
 * 包含分享基本信息、文件信息、访问统计等。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "云盘分享信息")
public class CloudShareVO {

    /**
     * 分享ID
     */
    @Schema(description = "分享ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 文件ID（对象存储路径）
     */
    @Schema(description = "文件ID")
    private String fileId;

    /**
     * 文件名称
     */
    @Schema(description = "文件名称")
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    /**
     * 文件类型
     */
    @Schema(description = "文件类型")
    private String fileType;

    /**
     * 分享码
     */
    @Schema(description = "分享码")
    private String shareCode;

    /**
     * 分享链接
     */
    @Schema(description = "分享链接")
    private String shareUrl;

    /**
     * 剩余有效期（天）
     */
    @Schema(description = "剩余有效期（天）")
    private Integer expireDays;

    /**
     * 下载次数
     */
    @Schema(description = "下载次数")
    private Integer downloadCount;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 过期时间
     */
    @Schema(description = "过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireTime;

    /**
     * 是否需要密码
     */
    @Schema(description = "是否需要密码")
    private Boolean hasPassword;

    /**
     * 分享者ID
     */
    @Schema(description = "分享者ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 分享者名称
     */
    @Schema(description = "分享者名称")
    private String userName;

    /**
     * 分享者头像URL
     */
    @Schema(description = "分享者头像URL")
    private String userAvatar;

    /**
     * 是否已过期
     */
    @Schema(description = "是否已过期")
    private Boolean expired;

    /**
     * 访问次数
     */
    @Schema(description = "访问次数")
    private Integer viewCount;

    /**
     * 分享标题（可选）
     */
    @Schema(description = "分享标题")
    private String title;

    /**
     * 分享描述（可选）
     */
    @Schema(description = "分享描述")
    private String description;
}
