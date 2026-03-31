package com.maxxvll.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

/**
 * 云盘文件下载信息
 * 包含文件流、文件名、Content-Type 等信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudDownloadVO {

    /**
     * 文件输入流
     */
    private InputStream inputStream;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * Content-Type
     */
    private String contentType;

    /**
     * 文件大小
     */
    private long size;
}