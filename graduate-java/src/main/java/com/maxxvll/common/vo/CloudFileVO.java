package com.maxxvll.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 云盘文件信息响应VO
 * <p>
 * 用于返回云盘文件详细信息。
 * 包含文件基本信息、预览/下载URL、分类等。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "云盘文件信息")
public class CloudFileVO {

    /**
     * 文件名称
     */
    @Schema(description = "文件名称")
    private String name;

    /**
     * 对象存储路径
     */
    @Schema(description = "对象存储路径")
    private String object;

    /**
     * 文件大小（字节）
     */
    @Schema(description = "文件大小（字节）")
    private Long size;

    /**
     * 修改时间
     */
    @Schema(description = "修改时间")
    private String modifyTime;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private String createTime;

    /**
     * Content-Type
     */
    @Schema(description = "Content-Type")
    private String contentType;

    /**
     * 文件扩展名
     */
    @Schema(description = "文件扩展名")
    private String extension;

    /**
     * 文件分类：image/video/audio/document/archive/file
     */
    @Schema(description = "文件分类")
    private String category;

    /**
     * 预览模式：image/video/audio/text/document/pdf/html
     */
    @Schema(description = "预览模式")
    private String previewMode;

    /**
     * 是否可预览
     */
    @Schema(description = "是否可预览")
    private Boolean previewable;

    /**
     * 是否可流式播放
     */
    @Schema(description = "是否可流式播放")
    private Boolean streamable;

    /**
     * 下载地址
     */
    @Schema(description = "下载地址")
    private String downloadUrl;

    /**
     * 预览地址
     */
    @Schema(description = "预览地址")
    private String previewUrl;

    /**
     * 流式播放地址
     */
    @Schema(description = "流式播放地址")
    private String streamUrl;

    /**
     * 是否为文件夹
     */
    @Schema(description = "是否为文件夹")
    private Boolean isFolder;

    /**
     * 文件夹ID（用于文件夹导航）
     */
    @Schema(description = "文件夹ID")
    private String folderId;

    /**
     * 文件版本号（预留）
     */
    @Schema(description = "文件版本号")
    private Long version;

    /**
     * 下载次数
     */
    @Schema(description = "下载次数")
    private Long downloadCount;

    /**
     * 是否已收藏
     */
    @Schema(description = "是否已收藏")
    private Boolean starred;

    /**
     * 文件标签（逗号分隔）
     */
    @Schema(description = "文件标签")
    private String tags;
}
