package com.maxxvll.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 云盘文件分页响应VO
 * <p>
 * 用于返回云盘文件列表及存储统计信息。
 * 包含文件列表、存储空间统计、分页游标等。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "云盘文件分页信息")
public class CloudFilePageVO {

    /**
     * 文件列表
     */
    @Schema(description = "文件列表")
    private List<CloudFileVO> files;

    /**
     * 已使用存储空间（字节）
     */
    @Schema(description = "已使用存储空间（字节）")
    private Long used;

    /**
     * 存储配额（字节），null表示无限制
     */
    @Schema(description = "存储配额（字节）")
    private Long quota;

    /**
     * 使用百分比
     */
    @Schema(description = "使用百分比")
    private Double usagePercent;

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

    /**
     * 当前文件夹ID
     */
    @Schema(description = "当前文件夹ID")
    private String currentFolderId;

    /**
     * 当前文件夹路径
     */
    @Schema(description = "当前文件夹路径")
    private String currentFolderPath;

    /**
     * 文件总数
     */
    @Schema(description = "文件总数")
    private Integer totalCount;

    /**
     * 文件夹数量
     */
    @Schema(description = "文件夹数量")
    private Integer folderCount;

    /**
     * 文件数量
     */
    @Schema(description = "文件数量")
    private Integer fileCount;
}
