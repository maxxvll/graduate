package com.maxxvll.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 好友分组信息响应VO
 *
 * @author backend
 */
@Data
@Schema(description = "好友分组信息")
public class FriendGroupVO {

    /**
     * 分组ID
     */
    @Schema(description = "分组ID")
    private Long id;

    /**
     * 分组名称
     */
    @Schema(description = "分组名称")
    private String name;

    /**
     * 好友数量
     */
    @Schema(description = "好友数量")
    private Integer friendCount;

    /**
     * 是否默认分组
     */
    @Schema(description = "是否默认分组")
    private Boolean isDefault;

    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer order;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 好友列表
     */
    @Schema(description = "好友列表")
    private List<FriendVO> friends;

    /**
     * 好友信息VO
     */
    @Data
    @Schema(description = "好友信息")
    public static class FriendVO {
        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "昵称")
        private String nickname;

        @Schema(description = "用户名")
        private String username;

        @Schema(description = "头像")
        private String avatar;

        @Schema(description = "签名")
        private String signature;

        @Schema(description = "备注")
        private String remark;
    }
}
