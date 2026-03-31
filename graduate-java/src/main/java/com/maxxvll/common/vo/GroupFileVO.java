package com.maxxvll.common.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 群文件信息响应VO
 * <p>
 * 用于返回群文件中转站的文件信息。
 * 包含文件基本信息、上传者信息、预览/下载URL等。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "群文件信息")
public class GroupFileVO {

    /**
     * 文件记录ID
     */
    @Schema(description = "文件记录ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 群组ID
     */
    @Schema(description = "群组ID")
    private String groupId;

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
     * 文件扩展名
     */
    @Schema(description = "文件扩展名")
    private String extension;

    /**
     * 上传者ID
     */
    @Schema(description = "上传者ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long uploaderId;

    /**
     * 上传者名称
     */
    @Schema(description = "上传者名称")
    private String uploaderName;

    /**
     * 下载地址
     */
    @Schema(description = "下载地址")
    private String downloadUrl;

    /**
     * 上传时间
     */
    @Schema(description = "上传时间")
    private String uploadTime;

    /**
     * 下载次数
     */
    @Schema(description = "下载次数")
    private Integer downloadCount;

    /**
     * 是否可预览
     */
    @Schema(description = "是否可预览")
    private Boolean previewable;

    /**
     * 预览模式
     */
    @Schema(description = "预览模式")
    private String previewMode;

    /**
     * 预览地址
     */
    @Schema(description = "预览地址")
    private String previewUrl;

    /**
     * 文件分类
     */
    @Schema(description = "文件分类")
    private String category;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private String updateTime;
}
