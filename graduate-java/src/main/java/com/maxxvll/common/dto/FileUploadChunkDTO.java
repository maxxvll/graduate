package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件分块上传请求DTO
 * <p>
 * 用于大文件分块上传时的请求参数验证。
 * 包含文件MD5、文件名、分块索引、总分块数、文件大小和分块文件。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "文件分块上传请求参数")
public class FileUploadChunkDTO {

    /**
     * 文件MD5值（用于文件去重和完整性校验）
     */
    @Schema(description = "文件MD5", example = "d41d8cd98f00b204e9800998ecf8427e", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "文件MD5不能为空")
    private String md5;

    /**
     * 文件名
     */
    @Schema(description = "文件名", example = "document.pdf", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 当前分块索引（从0开始）
     */
    @Schema(description = "当前分块索引", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "切片索引不能为空")
    @Min(value = 0, message = "切片索引不能为负数")
    private Integer chunkIndex;

    /**
     * 总分块数
     */
    @Schema(description = "总分块数", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "总切片数不能为空")
    @Min(value = 1, message = "总切片数必须大于0")
    private Integer totalChunks;

    /**
     * 总文件大小（字节）
     */
    @Schema(description = "总文件大小（字节）", example = "10485760", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于0")
    @Max(value = 1024L * 1024 * 1024 * 2, message = "文件大小不能超过2GB")
    private Long fileSize;

    /**
     * 分块文件
     */
    @Schema(description = "分块文件", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "切片文件不能为空")
    private MultipartFile file;
}