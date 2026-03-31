package com.maxxvll.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应通用类
 * <p>
 * 用于所有分页查询接口的统一响应格式。
 * 包含分页数据、总记录数、分页信息等。
 * </p>
 *
 * @param <T> 数据类型
 * @author backend-friend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页响应结果")
public class PageResponse<T> {

    /**
     * 当前页数据列表
     */
    @Schema(description = "当前页数据列表")
    private List<T> records;

    /**
     * 总记录数
     */
    @Schema(description = "总记录数")
    private Long total;

    /**
     * 当前页码
     */
    @Schema(description = "当前页码")
    private Integer pageNum;

    /**
     * 每页记录数
     */
    @Schema(description = "每页记录数")
    private Integer pageSize;

    /**
     * 总页数
     */
    @Schema(description = "总页数")
    private Integer totalPages;

    /**
     * 创建空分页响应
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param <T>      数据类型
     * @return 空分页响应
     */
    public static <T> PageResponse<T> empty(Integer pageNum, Integer pageSize) {
        return PageResponse.<T>builder()
                .records(List.of())
                .total(0L)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages(0)
                .build();
    }

    /**
     * 创建分页响应
     *
     * @param records  数据列表
     * @param total    总记录数
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param <T>      数据类型
     * @return 分页响应
     */
    public static <T> PageResponse<T> of(List<T> records, Long total, Integer pageNum, Integer pageSize) {
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return PageResponse.<T>builder()
                .records(records)
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .build();
    }
}
