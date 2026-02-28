package com.maxxvll.common.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 消息附件 DTO
 */
@Data
public class ChatMessageFileDTO {
    /** 附件文件 */
    private MultipartFile file;

    /** 消息类型：2-图片，3-视频，4-音频，5-文件 */
    private Integer messageType;

    /** 文件名（可选，不传则用原始文件名） */
    private String fileName;
}