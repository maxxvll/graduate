package com.maxxvll.common.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUploadChunkDTO {
    private String md5;           // 文件MD5
    private String fileName;      // 文件名
    private Integer chunkIndex;   // 当前切片索引 (从0开始)
    private Integer totalChunks;  // 总切片数
    private Long fileSize;        // 总文件大小
    private MultipartFile file;   // 切片文件
}