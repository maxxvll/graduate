package com.maxxvll.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文件上传检查响应VO
 * <p>
 * 用于返回文件上传检查结果（秒传检测、断点续传）。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文件上传检查结果")
public class FileUploadCheckVO {

    /**
     * 是否需要上传（false=秒传成功）
     */
    @Schema(description = "是否需要上传")
    private Boolean shouldUpload;

    /**
     * 秒传成功时返回的URL
     */
    @Schema(description = "秒传成功时返回的URL")
    private String fileUrl;

    /**
     * 已上传的切片索引列表（断点续传用）
     */
    @Schema(description = "已上传的切片索引列表")
    private List<Integer> uploadedChunks;
}