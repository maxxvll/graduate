package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 消息附件上传请求DTO
 * <p>
 * 用于上传消息附件时的请求参数验证。
 * 支持图片、视频、音频、文件等多种类型。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "消息附件上传请求参数")
public class ChatMessageFileDTO {

    /**
     * 附件文件
     */
    @Schema(description = "附件文件", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "附件文件不能为空")
    private MultipartFile file;

    /**
     * 消息类型：2-图片，3-视频，4-音频，5-文件
     */
    @Schema(description = "消息类型", example = "2", allowableValues = {"2", "3", "4", "5"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "消息类型不能为空")
    private Integer messageType;

    /**
     * 文件名（可选，不传则用原始文件名）
     */
    @Schema(description = "文件名", example = "photo.jpg")
    @Size(max = 255, message = "文件名不能超过255个字符")
    private String fileName;
}