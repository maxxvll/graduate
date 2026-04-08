package com.maxxvll.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息反应响应VO
 *
 * @author backend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "消息反应信息")
public class MessageReactionVO {

    /**
     * 表情标识
     */
    @Schema(description = "表情标识")
    private String emoji;

    /**
     * 反应用户列表
     */
    @Schema(description = "反应用户列表")
    private List<ReactionUserVO> users;

    /**
     * 反应数量
     */
    @Schema(description = "反应数量")
    private Integer count;

    /**
     * 当前用户是否添加了该表情
     */
    @Schema(description = "当前用户是否添加了该表情")
    private Boolean isCurrentUserReacted;

    /**
     * 反应用户信息VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "反应用户信息")
    public static class ReactionUserVO {
        /**
         * 用户ID
         */
        @Schema(description = "用户ID")
        private String id;

        /**
         * 用户名称
         */
        @Schema(description = "用户名称")
        private String name;

        /**
         * 用户头像
         */
        @Schema(description = "用户头像")
        private String avatar;
    }
}
