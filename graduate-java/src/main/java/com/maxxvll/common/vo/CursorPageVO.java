package com.maxxvll.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 游标分页响应VO
 * <p>
 * 用于返回基于游标分页的查询结果。
 * 适用于数据量大、实时性强的场景。
 * </p>
 *
 * @param <T> 数据类型
 * @author backend-friend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "游标分页信息")
public class CursorPageVO<T> {

    /**
     * 数据列表
     */
    @Schema(description = "数据列表")
    private List<T> items;

    /**
     * 下一页游标
     */
    @Schema(description = "下一页游标")
    private String nextCursor;

    /**
     * 是否有更多数据
     */
    @Schema(description = "是否有更多数据")
    private Boolean hasMore;
}
