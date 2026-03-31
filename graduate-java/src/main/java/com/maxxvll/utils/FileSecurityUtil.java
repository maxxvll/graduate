package com.maxxvll.utils;

import cn.hutool.core.io.FileUtil;
import com.maxxvll.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件安全验证工具类
 * <p>
 * 提供文件上传安全验证功能：
 * - 文件扩展名白名单校验
 * - 文件内容头（MIME Type）校验
 * - 文件大小限制
 * - 可执行文件检测
 * </p>
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Slf4j
@Component
public class FileSecurityUtil {

    // ==================== 允许的文件扩展名白名单 ====================

    /** 图片文件扩展名白名单 */
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp"
    ));

    /** 文档文件扩展名白名单 */
    private static final Set<String> ALLOWED_DOCUMENT_EXTENSIONS = new HashSet<>(Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf"
    ));

    /** 视频文件扩展名白名单 */
    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp4", "avi", "mov", "wmv", "mkv", "flv", "webm"
    ));

    /** 音频文件扩展名白名单 */
    private static final Set<String> ALLOWED_AUDIO_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp3", "wav", "ogg", "flac", "aac", "wma", "m4a"
    ));

    /** 压缩文件扩展名白名单 */
    private static final Set<String> ALLOWED_ARCHIVE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "zip", "rar", "7z", "tar", "gz"
    ));

    /** 头像文件扩展名白名单（仅允许图片） */
    private static final Set<String> ALLOWED_AVATAR_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp"
    ));

    /** 所有允许的扩展名 */
    private static final Set<String> ALL_ALLOWED_EXTENSIONS;

    static {
        ALL_ALLOWED_EXTENSIONS = new HashSet<>();
        ALL_ALLOWED_EXTENSIONS.addAll(ALLOWED_IMAGE_EXTENSIONS);
        ALL_ALLOWED_EXTENSIONS.addAll(ALLOWED_DOCUMENT_EXTENSIONS);
        ALL_ALLOWED_EXTENSIONS.addAll(ALLOWED_VIDEO_EXTENSIONS);
        ALL_ALLOWED_EXTENSIONS.addAll(ALLOWED_AUDIO_EXTENSIONS);
        ALL_ALLOWED_EXTENSIONS.addAll(ALLOWED_ARCHIVE_EXTENSIONS);
    }

    // ==================== 文件大小限制 ====================

    /** 单个文件最大大小（100MB） */
    public static final long MAX_FILE_SIZE = 100 * 1024 * 1024;

    /** 图片文件最大大小（10MB） */
    public static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;

    /** 视频文件最大大小（500MB） */
    public static final long MAX_VIDEO_SIZE = 500 * 1024 * 1024;

    /** 文档文件最大大小（50MB） */
    public static final long MAX_DOCUMENT_SIZE = 50 * 1024 * 1024;

    /** 头像文件最大大小（5MB） */
    public static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;

    /** 切片文件最大大小（10MB） */
    public static final long MAX_CHUNK_SIZE = 10 * 1024 * 1024;

    // ==================== 危险文件扩展名黑名单 ====================

    /** 可执行文件扩展名黑名单 */
    private static final Set<String> DANGEROUS_EXTENSIONS = new HashSet<>(Arrays.asList(
            "exe", "dll", "so", "sh", "bat", "cmd", "msi", "app", "dmg",
            "jar", "war", "jsp", "php", "asp", "aspx", "cgi", "pl", "py",
            "rb", "js", "vbs", "scr", "pif", "msc", "hta", "cpl", "inf",
            "reg", "com", "vxd", "sys", "ocx", "ini", "cfg"
    ));

    // ==================== 文件内容头（MIME Type）映射 ====================

    /** 图片文件内容头 */
    private static final Set<String> IMAGE_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp"
    ));

    /** 文档文件内容头 */
    private static final Set<String> DOCUMENT_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain", "text/rtf"
    ));

    /** 视频文件内容头 */
    private static final Set<String> VIDEO_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "video/mp4", "video/x-msvideo", "video/quicktime", "video/x-ms-wmv",
            "video/x-matroska", "video/x-flv", "video/webm"
    ));

    /** 音频文件内容头 */
    private static final Set<String> AUDIO_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "audio/mpeg", "audio/wav", "audio/ogg", "audio/flac", "audio/aac",
            "audio/x-ms-wma", "audio/mp4", "audio/x-m4a"
    ));

    /**
     * 验证文件扩展名
     *
     * @param fileName 文件名
     * @throws BusinessException 如果文件扩展名不安全
     */
    public void validateExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException("文件名不能为空");
        }

        String extension = getFileExtension(fileName).toLowerCase();

        // 检查危险扩展名
        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            log.warn("危险文件扩展名被拒绝: {}", extension);
            throw new BusinessException("不支持的文件类型: " + extension);
        }

        // 检查是否在白名单中
        if (!ALL_ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("未授权的文件扩展名: {}", extension);
            throw new BusinessException("不支持的文件扩展名: " + extension);
        }
    }

    /**
     * 验证头像文件扩展名
     *
     * @param fileName 文件名
     * @throws BusinessException 如果文件扩展名不安全
     */
    public void validateAvatarExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException("文件名不能为空");
        }

        String extension = getFileExtension(fileName).toLowerCase();

        if (!ALLOWED_AVATAR_EXTENSIONS.contains(extension)) {
            log.warn("非法的头像文件扩展名: {}", extension);
            throw new BusinessException("头像仅支持 JPG、PNG、GIF、WebP 格式");
        }
    }

    /**
     * 验证文件大小
     *
     * @param size 文件大小（字节）
     * @param maxSize 最大允许大小
     * @throws BusinessException 如果文件大小超限
     */
    public void validateFileSize(long size, long maxSize) {
        if (size <= 0) {
            throw new BusinessException("文件大小无效");
        }

        if (size > maxSize) {
            log.warn("文件大小超限: {} bytes, 最大: {} bytes", size, maxSize);
            throw new BusinessException("文件大小不能超过 " + formatFileSize(maxSize));
        }
    }

    /**
     * 验证文件内容头（MIME Type）
     *
     * @param contentType 文件的 MIME 类型
     * @param allowedTypes 允许的 MIME 类型集合
     * @throws BusinessException 如果 MIME 类型不安全
     */
    public void validateContentType(String contentType, Set<String> allowedTypes) {
        if (contentType == null || contentType.isBlank()) {
            throw new BusinessException("文件类型未知");
        }

        if (!allowedTypes.contains(contentType.toLowerCase())) {
            log.warn("非法的文件内容类型: {}", contentType);
            throw new BusinessException("不支持的文件类型");
        }
    }

    /**
     * 验证文件内容头（使用扩展名对应的标准 MIME 类型）
     *
     * @param fileName 文件名
     * @param contentType 文件的 MIME 类型
     * @throws BusinessException 如果 MIME 类型与扩展名不匹配
     */
    public void validateContentTypeWithExtension(String fileName, String contentType) {
        if (fileName == null || contentType == null) {
            throw new BusinessException("文件名或类型不能为空");
        }

        String extension = getFileExtension(fileName).toLowerCase();
        String expectedContentType = getContentTypeByExtension(extension);

        if (expectedContentType != null && !contentType.equalsIgnoreCase(expectedContentType)) {
            // 允许一些特殊情况（如 application/octet-stream）
            if (!"application/octet-stream".equalsIgnoreCase(contentType)) {
                log.warn("文件扩展名与内容类型不匹配: extension={}, contentType={}", extension, contentType);
                throw new BusinessException("文件内容与扩展名不匹配");
            }
        }
    }

    /**
     * 通过文件扩展名获取预期的 MIME 类型
     */
    private String getContentTypeByExtension(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "mp4" -> "video/mp4";
            case "mp3" -> "audio/mpeg";
            default -> null;
        };
    }

    /**
     * 检测文件内容是否包含可执行代码或危险内容
     * <p>
     * 通过读取文件头部字节来判断文件真实类型
     * </p>
     *
     * @param inputStream 文件输入流
     * @param fileName 文件名
     * @return true 如果文件安全
     * @throws IOException 如果读取失败
     */
    public boolean isFileContentSafe(InputStream inputStream, String fileName) throws IOException {
        if (inputStream == null) {
            return false;
        }

        String extension = getFileExtension(fileName).toLowerCase();

        // 读取文件头部
        byte[] header = new byte[16];
        int bytesRead = inputStream.read(header);

        if (bytesRead < 4) {
            return false;
        }

        // 检查图片文件头部
        if (ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            return isValidImageHeader(header);
        }

        // 检查 PDF 文件
        if ("pdf".equals(extension)) {
            return isPdfHeader(header);
        }

        // 通用检查：确保不是脚本或可执行文件
        return !isScriptOrExecutableHeader(header);
    }

    /**
     * 检查图片文件头部是否有效
     */
    private boolean isValidImageHeader(byte[] header) {
        // JPEG: FF D8 FF
        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
            return true;
        }
        // PNG: 89 50 4E 47
        if (header[0] == (byte) 0x89 && header[1] == (byte) 0x50 &&
                header[2] == (byte) 0x4E && header[3] == (byte) 0x47) {
            return true;
        }
        // GIF: 47 49 46 38
        if (header[0] == (byte) 0x47 && header[1] == (byte) 0x49 &&
                header[2] == (byte) 0x46 && header[3] == (byte) 0x38) {
            return true;
        }
        // BMP: 42 4D
        if (header[0] == (byte) 0x42 && header[1] == (byte) 0x4D) {
            return true;
        }
        // WebP: 52 49 46 46 ... 57 45 42 50 (RIFF....WEBP)
        if (header[0] == (byte) 0x52 && header[1] == (byte) 0x49 &&
                header[2] == (byte) 0x46 && header[3] == (byte) 0x46) {
            // 需要检查后面的字节
            return bytesToHex(header, 8, 4).equals("57454250"); // WEBP
        }

        return false;
    }

    /**
     * 检查 PDF 文件头部
     */
    private boolean isPdfHeader(byte[] header) {
        String headerStr = new String(header, 0, Math.min(5, header.length));
        return headerStr.startsWith("%PDF");
    }

    /**
     * 检查是否是脚本或可执行文件头部
     */
    private boolean isScriptOrExecutableHeader(byte[] header) {
        // 检查是否以 #! 开头（脚本）
        if (header[0] == '#' && header[1] == '!') {
            return true;
        }
        // 检查是否是 PE 可执行文件 (MZ 头)
        if (header[0] == 'M' && header[1] == 'Z') {
            return true;
        }
        // 检查是否是 ELF 可执行文件
        if (header[0] == 0x7F && header[1] == 'E' && header[2] == 'L' && header[3] == 'F') {
            return true;
        }

        return false;
    }

    /**
     * 获取文件扩展名
     */
    public String getFileExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1);
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    private String bytesToHex(byte[] bytes, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < offset + length && i < bytes.length; i++) {
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }

    /**
     * 综合验证文件安全性
     *
     * @param fileName 文件名
     * @param contentType 文件 MIME 类型
     * @param fileSize 文件大小
     * @param maxSize 最大允许大小
     * @throws BusinessException 如果验证失败
     */
    public void validateFile(String fileName, String contentType, long fileSize, long maxSize) {
        // 1. 验证扩展名
        validateExtension(fileName);

        // 2. 验证文件大小
        validateFileSize(fileSize, maxSize);

        // 3. 验证内容类型
        String extension = getFileExtension(fileName).toLowerCase();
        Set<String> allowedTypes = getAllowedContentTypesByExtension(extension);
        if (allowedTypes != null && !allowedTypes.isEmpty()) {
            validateContentType(contentType, allowedTypes);
        }

        log.debug("文件安全验证通过: {}, size={}", fileName, fileSize);
    }

    /**
     * 根据扩展名获取允许的 MIME 类型集合
     */
    private Set<String> getAllowedContentTypesByExtension(String extension) {
        if (ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            return IMAGE_CONTENT_TYPES;
        }
        if (ALLOWED_DOCUMENT_EXTENSIONS.contains(extension)) {
            return DOCUMENT_CONTENT_TYPES;
        }
        if (ALLOWED_VIDEO_EXTENSIONS.contains(extension)) {
            return VIDEO_CONTENT_TYPES;
        }
        if (ALLOWED_AUDIO_EXTENSIONS.contains(extension)) {
            return AUDIO_CONTENT_TYPES;
        }
        return null;
    }
}
