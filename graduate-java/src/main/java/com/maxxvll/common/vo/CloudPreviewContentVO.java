package com.maxxvll.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 云盘预览内容响应VO
 * <p>
 * 用于返回云盘文件预览内容。
 * 支持文本预览、HTML预览等模式。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "云盘预览内容")
public class CloudPreviewContentVO {

    /**
     * 预览模式：text/html/image
     */
    @Schema(description = "预览模式")
    private String mode;

    /**
     * 标题
     */
    @Schema(description = "标题")
    private String title;

    /**
     * Content-Type
     */
    @Schema(description = "Content-Type")
    private String contentType;

    /**
     * 文本内容（mode=text时）
     */
    @Schema(description = "文本内容")
    private String textContent;

    /**
     * HTML内容（mode=html时）
     */
    @Schema(description = "HTML内容")
    private String htmlContent;

    /**
     * 内容是否被截断
     */
    @Schema(description = "内容是否被截断")
    private Boolean truncated;

    /**
     * 提示信息
     */
    @Schema(description = "提示信息")
    private String message;
}
