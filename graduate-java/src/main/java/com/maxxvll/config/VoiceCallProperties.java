package com.maxxvll.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 语音通话配置属性
 *
 * @author maxxvll
 * @since 2026-03-16
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.voice-call")
public class VoiceCallProperties {

    private List<String> supportedTransports = new ArrayList<>(List.of("webrtc", "stream"));

    private List<IceServerProperties> iceServers = new ArrayList<>();

    /**
     * 流媒体服务器推流地址（用于 live-pusher）
     * 格式：rtmp://服务器地址/应用名/流名前缀
     * 示例：rtmp://media-server.com/live/
     */
    private String pushBaseUrl;

    /**
     * 流媒体服务器拉流地址（用于 live-player）
     * 格式：rtmp://服务器地址/应用名/流名前缀 或 http://服务器地址/应用名/流名前缀
     * 示例：rtmp://media-server.com/live/ 或 http://media-server.com/live/
     */
    private String playBaseUrl;

    @Data
    public static class IceServerProperties {
        private List<String> urls = new ArrayList<>();
        private String username;
        private String credential;
    }
}
