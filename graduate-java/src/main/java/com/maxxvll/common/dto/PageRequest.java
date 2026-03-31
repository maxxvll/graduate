package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页请求基础类
 * <p>
 * 用于所有需要分页的查询接口，提供统一的分页参数。
 * 默认每页20条记录，最大每页100条。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "分页请求参数")
public class PageRequest {

    /**
     * 页码，从1开始
     */
    @Schema(description = "页码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNum = 1;

    /**
     * 每页记录数
     */
    @Schema(description = "每页记录数", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 1, message = "每页记录数必须大于0")
    @Max(value = 100, message = "每页记录数不能超过100")
    private Integer pageSize = 20;

    /**
     * 计算偏移量（用于MyBatis-Plus等ORM）
     *
     * @return 偏移量
     */
    public long getOffset() {
        return (long) (pageNum - 1) * pageSize;
    }
}
