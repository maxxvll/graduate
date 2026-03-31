package com.maxxvll.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 搜索请求DTO
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
public class SearchDTO {

    /**
     * 搜索关键词
     */
    @NotBlank(message = "搜索关键词不能为空")
    @Size(max = 255, message = "搜索关键词长度不能超过255")
    private String keyword;

    /**
     * 搜索类型：MESSAGE/CONTACT/GROUP/ALL（默认ALL）
     */
    private String searchType;

    /**
     * 消息类型筛选（仅MESSAGE类型有效）
     */
    private String messageType;

    /**
     * 会话ID筛选（仅MESSAGE类型有效）
     */
    private String sessionId;

    /**
     * 开始时间（时间戳，毫秒）
     */
    private Long startTime;

    /**
     * 结束时间（时间戳，毫秒）
     */
    private Long endTime;

    // ==================== 搜索类型常量 ====================

    public static final String TYPE_MESSAGE = "MESSAGE";
    public static final String TYPE_CONTACT = "CONTACT";
    public static final String TYPE_GROUP = "GROUP";
    public static final String TYPE_ALL = "ALL";
}
