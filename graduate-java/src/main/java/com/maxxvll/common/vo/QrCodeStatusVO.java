package com.maxxvll.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 二维码状态响应VO
 * <p>
 * 用于返回二维码登录状态查询结果。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "二维码状态信息")
public class QrCodeStatusVO {

    /**
     * 二维码ID
     */
    @Schema(description = "二维码ID")
    private String qrCodeId;

    /**
     * 状态：pending-待扫描，scanned-已扫描，confirmed-已确认，expired-已过期
     */
    @Schema(description = "状态", example = "pending")
    private String status;

    /**
     * 登录成功后返回的Token
     */
    @Schema(description = "登录Token")
    private String token;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 用户昵称
     */
    @Schema(description = "用户昵称")
    private String nickname;
}
