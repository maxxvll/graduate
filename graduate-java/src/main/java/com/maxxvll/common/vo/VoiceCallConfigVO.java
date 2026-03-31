package com.maxxvll.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 语音通话配置响应VO
 * <p>
 * 用于返回语音通话所需的配置信息。
 * 包含支持的传输方式、ICE服务器配置、流媒体服务器地址等。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "语音通话配置")
public class VoiceCallConfigVO {

    /**
     * 支持的传输方式
     */
    @Schema(description = "支持的传输方式", example = "[\"webrtc\", \"stream\"]")
    private List<String> supportedTransports = new ArrayList<>();

    /**
     * ICE服务器配置列表
     */
    @Schema(description = "ICE服务器配置")
    private List<IceServerVO> iceServers = new ArrayList<>();

    /**
     * 流媒体服务器推流地址（用于live-pusher）
     */
    @Schema(description = "推流地址")
    private String pushBaseUrl;

    /**
     * 流媒体服务器拉流地址（用于live-player）
     */
    @Schema(description = "拉流地址")
    private String playBaseUrl;

    /**
     * ICE服务器配置
     */
    @Data
    @Schema(description = "ICE服务器配置")
    public static class IceServerVO {
        /**
         * ICE服务器URL列表
         */
        @Schema(description = "ICE服务器URL列表")
        private List<String> urls = new ArrayList<>();

        /**
         * 用户名（用于TURN认证）
         */
        @Schema(description = "ICE服务器用户名")
        private String username;

        /**
         * 凭证（用于TURN认证）
         */
        @Schema(description = "ICE服务器凭证")
        private String credential;
    }
}
