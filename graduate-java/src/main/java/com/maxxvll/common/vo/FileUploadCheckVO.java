package com.maxxvll.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadCheckVO {
    private Boolean shouldUpload;    // 是否需要上传 (false=秒传成功)
    private String fileUrl;           // 秒传成功时返回的URL
    private List<Integer> uploadedChunks; // 已上传的切片索引列表 (断点续传用)
}