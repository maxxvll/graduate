package com.maxxvll.common.vo;

import lombok.Data;

/**
 * 搜索历史VO
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
public class SearchHistoryVO {

    /**
     * 历史记录ID
     */
    private Long id;

    /**
     * 搜索类型
     */
    private String searchType;

    /**
     * 搜索类型描述
     */
    private String searchTypeDesc;

    /**
     * 关键词
     */
    private String keyword;

    /**
     * 搜索次数
     */
    private Integer searchCount;

    /**
     * 创建时间
     */
    private Long createdAt;
}
