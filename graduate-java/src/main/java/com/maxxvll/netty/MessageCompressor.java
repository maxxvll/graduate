package com.maxxvll.netty;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;

/**
 * 消息压缩工具
 * <p>
 * 使用GZIP压缩大消息，减少网络传输量
 * 适用于消息体较大的场景（如文件分享、链接预览等）
 * </p>
 *
 * <p><b>使用示例:</b></p>
 * <pre>{@code
 * // 压缩消息
 * CompressedMessage compressed = MessageCompressor.compress(message);
 * if (compressed.compressed()) {
 *     // 发送压缩后的消息
 *     channel.writeAndFlush(new TextWebSocketFrame(compressed.data()));
 * }
 *
 * // 解压消息
 * String original = MessageCompressor.decompress(compressed.data());
 * }</pre>
 *
 * @author backend-msg
 * @since 2026-03-31
 */
@Slf4j
@Component
public class MessageCompressor {

    /**
     * 启用压缩的最小消息大小（字节）
     */
    @Value("${ws.compression.min-size-bytes:1024}")
    private int minSizeBytes;

    /**
     * 压缩级别（0-9）
     */
    @Value("${ws.compression.level:6}")
    private int compressionLevel;

    /**
     * 最大消息大小（超过此大小不压缩）
     */
    @Value("${ws.compression.max-size-bytes:10485760}")
    private int maxSizeBytes;

    /**
     * 是否启用压缩
     */
    @Value("${ws.compression.enabled:true}")
    private boolean compressionEnabled;

    /**
     * 压缩头部标记
     */
    private static final byte[] GZIP_HEADER = new byte[]{(byte) 0x1F, (byte) 0x8B};

    /**
     * Base64编码表
     */
    private static final char[] BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
            .toCharArray();

    /**
     * 统计
     */
    private final AtomicLong totalOriginalBytes = new AtomicLong(0);
    private final AtomicLong totalCompressedBytes = new AtomicLong(0);
    private final AtomicLong totalMessagesCompressed = new AtomicLong(0);

    @PostConstruct
    public void init() {
        log.info("消息压缩器初始化完成, enabled={}, minSize={}B, maxSize={}B, level={}",
                compressionEnabled, minSizeBytes, maxSizeBytes, compressionLevel);
    }

    /**
     * 压缩消息
     *
     * @param message 原始消息
     * @return 压缩结果
     */
    public CompressedMessage compress(String message) {
        if (message == null || message.isEmpty()) {
            return new CompressedMessage(new byte[0], false, 0);
        }

        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        int originalSize = data.length;

        // 大小检查
        if (!compressionEnabled || originalSize < minSizeBytes || originalSize > maxSizeBytes) {
            return new CompressedMessage(data, false, originalSize);
        }

        try {
            byte[] compressed = gzipCompress(data);
            int compressedSize = compressed.length;

            // 如果压缩后没有明显减小，则返回原始数据
            if (compressedSize >= originalSize * 0.95) {
                return new CompressedMessage(data, false, originalSize);
            }

            // 更新统计
            totalOriginalBytes.addAndGet(originalSize);
            totalCompressedBytes.addAndGet(compressedSize);
            totalMessagesCompressed.incrementAndGet();

            log.debug("Compressed message: {}B -> {}B ({:.1f}% reduction)",
                    originalSize, compressedSize, 100.0 * (1 - (double) compressedSize / originalSize));

            return new CompressedMessage(compressed, true, originalSize);
        } catch (IOException e) {
            log.warn("Message compression failed, using original: {}", e.getMessage());
            return new CompressedMessage(data, false, originalSize);
        }
    }

    /**
     * 解压消息
     *
     * @param data 压缩数据（Base64编码）
     * @return 原始消息
     */
    public String decompress(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }

        // 检查是否是GZIP格式
        if (!isGzipped(data)) {
            return new String(data, StandardCharsets.UTF_8);
        }

        try {
            byte[] decompressed = gzipDecompress(data);
            return new String(decompressed, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Message decompression failed: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 解压Base64编码的消息
     *
     * @param base64Data Base64编码的压缩数据
     * @return 原始消息
     */
    public String decompressFromBase64(String base64Data) {
        if (base64Data == null || base64Data.isEmpty()) {
            return "";
        }

        try {
            byte[] data = base64Decode(base64Data);
            return decompress(data);
        } catch (IllegalArgumentException e) {
            log.error("Base64 decode failed: {}", e.getMessage());
            return "";
        }
    }

    /**
     * GZIP压缩
     */
    private byte[] gzipCompress(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        GZIPOutputStream gzip = new GZIPOutputStream(bos) {
            {
                def.setLevel(compressionLevel);
            }
        };
        gzip.write(data);
        gzip.finish();
        gzip.close();
        return bos.toByteArray();
    }

    /**
     * GZIP解压缩
     */
    private byte[] gzipDecompress(byte[] data) throws IOException {
        java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(
                new java.io.ByteArrayInputStream(data));
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length * 2);
        byte[] buffer = new byte[1024];
        int len;
        while ((len = gzip.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        gzip.close();
        return bos.toByteArray();
    }

    /**
     * 检查数据是否是GZIP格式
     */
    private boolean isGzipped(byte[] data) {
        if (data == null || data.length < 2) {
            return false;
        }
        return (data[0] == GZIP_HEADER[0]) && (data[1] == GZIP_HEADER[1]);
    }

    /**
     * Base64编码
     */
    private String base64Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int len = data.length;
        int i = 0;

        while (i < len) {
            int b1 = data[i++] & 0xff;
            int b2 = i < len ? data[i++] & 0xff : 0;
            int b3 = i < len ? data[i++] & 0xff : 0;

            sb.append(BASE64_CHARS[b1 >> 2]);
            sb.append(BASE64_CHARS[((b1 & 0x03) << 4) | (b2 >> 4)]);
            sb.append(i > len + 1 ? '=' : BASE64_CHARS[((b2 & 0x0f) << 2) | (b3 >> 6)]);
            sb.append(i > len ? '=' : BASE64_CHARS[b3 & 0x3f]);
        }

        return sb.toString();
    }

    /**
     * Base64解码
     */
    private byte[] base64Decode(String data) {
        // 使用Java内置的Base64解码器
        return java.util.Base64.getDecoder().decode(data);
    }

    /**
     * 构建压缩消息的包装
     *
     * @param originalMessage 原始消息
     * @return 压缩后的消息字符串
     */
    public String compressAndWrap(String originalMessage) {
        CompressedMessage compressed = compress(originalMessage);
        if (compressed.compressed()) {
            String encoded = base64Encode(compressed.data());
            return "{\"type\":\"compressed\",\"data\":\"" + encoded + "\",\"originalSize\":" + compressed.originalSize + "}";
        }
        return originalMessage;
    }

    /**
     * 解压包装后的消息
     *
     * @param wrappedMessage 压缩包装后的消息
     * @return 原始消息
     */
    public String unwrapAndDecompress(String wrappedMessage) {
        try {
            com.alibaba.fastjson2.JSONObject obj = com.alibaba.fastjson2.JSON.parseObject(wrappedMessage);
            if ("compressed".equals(obj.getString("type"))) {
                String encoded = obj.getString("data");
                return decompressFromBase64(encoded);
            }
        } catch (Exception e) {
            log.debug("Failed to unwrap compressed message: {}", e.getMessage());
        }
        return wrappedMessage;
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalMessagesCompressed", totalMessagesCompressed.get());
        stats.put("totalOriginalBytes", totalOriginalBytes.get());
        stats.put("totalCompressedBytes", totalCompressedBytes.get());
        if (totalOriginalBytes.get() > 0) {
            long saved = totalOriginalBytes.get() - totalCompressedBytes.get();
            stats.put("totalSavedBytes", saved);
            stats.put("compressionRatio", String.format("%.1f%%",
                    100.0 * saved / totalOriginalBytes.get()));
        }
        stats.put("compressionEnabled", compressionEnabled);
        return stats;
    }

    /**
     * 压缩结果
     *
     * @param data         压缩后的数据
     * @param compressed   是否进行了压缩
     * @param originalSize 原始大小
     */
    public record CompressedMessage(byte[] data, boolean compressed, int originalSize) {

        public boolean compressed() {
            return compressed;
        }

        public byte[] data() {
            return data;
        }

        public int originalSize() {
            return originalSize;
        }

        public String toBase64() {
            return java.util.Base64.getEncoder().encodeToString(data);
        }
    }

}
