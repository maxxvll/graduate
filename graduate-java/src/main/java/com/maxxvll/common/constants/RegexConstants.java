package com.maxxvll.common.constants;

/**
 * 正则表达式常量定义
 *
 * <p>统一管理所有常用的正则表达式模式</p>
 *
 * @author maxxvll
 * @since 2026-03-16
 */
public final class RegexConstants {

    private RegexConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 基础校验 ====================

    /**
     * 数字（整数）
     */
    public static final String NUMBER = "^[0-9]+$";

    /**
     * 正整数
     */
    public static final String POSITIVE_INTEGER = "^[1-9][0-9]*$";

    /**
     * 负整数
     */
    public static final String NEGATIVE_INTEGER = "^-[1-9][0-9]*$";

    /**
     * 整数（包括0）
     */
    public static final String INTEGER = "^-?[1-9][0-9]*|0$";

    /**
     * 浮点数
     */
    public static final String DECIMAL = "^-?[1-9][0-9]*\\.[0-9]+$";

    /**
     * 非负浮点数
     */
    public static final String NON_NEGATIVE_DECIMAL = "^[1-9][0-9]*\\.[0-9]+|0\\.[0-9]+$";

    // ==================== 用户相关 ====================

    /**
     * 用户名：字母、数字、下划线，3-20位
     */
    public static final String USERNAME = "^[a-zA-Z0-9_]{3,20}$";

    /**
     * 密码：字母、数字、特殊字符，6-20位
     */
    public static final String PASSWORD = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]{6,20}$";

    /**
     * 昵称：中文、字母、数字、下划线，1-50位
     */
    public static final String NICKNAME = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]{1,50}$";

    // ==================== 联系方式 ====================

    /**
     * 中国手机号：1开头，11位
     */
    public static final String CHINA_PHONE = "^1[3-9]\\d{9}$";

    /**
     * 中国座机号：区号-号码
     */
    public static final String CHINA_TELEPHONE = "^0\\d{2,3}-?\\d{7,8}$";

    /**
     * 邮箱地址
     */
    public static final String EMAIL = "^[a-zA-Z0-9._%\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$";

    /**
     * 邮箱地址（宽松）
     */
    public static final String EMAIL_LOOSE = "^[a-zA-Z0-9._%\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]+$";

    // ==================== 身份标识 ====================

    /**
     * 中国居民身份证号（18位）
     */
    public static final String CHINA_ID_CARD = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$";

    /**
     * QQ号：5-11位数字
     */
    public static final String QQ_NUMBER = "^[1-9][0-9]{4,10}$";

    // ==================== URL/路径 ====================

    /**
     * HTTP/HTTPS URL
     */
    public static final String URL = "^(https?|ftp)://[a-zA-Z0-9\\-.]+(:\\d+)?(/[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?$";

    /**
     * 仅HTTP/HTTPS URL
     */
    public static final String HTTP_URL = "^https?://[a-zA-Z0-9\\-.]+(:\\d+)?(/[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?$";

    /**
     * 文件路径（Unix风格）
     */
    public static final String UNIX_FILE_PATH = "^/[a-zA-Z0-9\\-._~/]*$";

    /**
     * 文件路径（Windows风格）
     */
    public static final String WINDOWS_FILE_PATH = "^[a-zA-Z]:\\\\[a-zA-Z0-9\\-._\\\\]*$";

    // ==================== 文件相关 ====================

    /**
     * 文件名（不包括扩展名）
     */
    public static final String FILE_NAME = "^[a-zA-Z0-9\\-_\\u4e00-\\u9fa5]+\\.?[a-zA-Z0-9]*$";

    /**
     * 常见图片扩展名
     */
    public static final String IMAGE_EXTENSION = "\\.(jpg|jpeg|png|gif|bmp|webp)$";

    /**
     * 常见文档扩展名
     */
    public static final String DOCUMENT_EXTENSION = "\\.(doc|docx|xls|xlsx|ppt|pptx|pdf|txt)$";

    /**
     * 常见视频扩展名
     */
    public static final String VIDEO_EXTENSION = "\\.(mp4|avi|mov|wmv|flv|mkv)$";

    /**
     * 常见音频扩展名
     */
    public static final String AUDIO_EXTENSION = "\\.(mp3|wav|flac|aac|ogg|wma)$";

    /**
     * MD5哈希（32位）
     */
    public static final String MD5 = "^[a-fA-F0-9]{32}$";

    /**
     * SHA256哈希（64位）
     */
    public static final String SHA256 = "^[a-fA-F0-9]{64}$";

    // ==================== IP地址 ====================

    /**
     * IPv4地址
     */
    public static final String IP_V4 = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$";

    /**
     * IPv6地址（简化版）
     */
    public static final String IP_V6 = "^[0-9a-fA-F:]+$";

    // ==================== 时间日期 ====================

    /**
     * 日期格式：YYYY-MM-DD
     */
    public static final String DATE = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$";

    /**
     * 时间格式：HH:mm:ss
     */
    public static final String TIME = "^([01]\\d|2[0-3]):([0-5]\\d):([0-5]\\d)$";

    /**
     * 日期时间格式：YYYY-MM-DD HH:mm:ss
     */
    public static final String DATETIME = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01]) ([01]\\d|2[0-3]):([0-5]\\d):([0-5]\\d)$";

    /**
     * 时间戳（毫秒）
     */
    public static final String TIMESTAMP_MS = "^\\d{13}$";

    // ==================== 特殊格式 ====================

    /**
     * 颜色值（HEX格式）
     */
    public static final String HEX_COLOR = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$";

    /**
     * 邮政编码（中国）
     */
    public static final String CHINA_POSTAL_CODE = "^[1-9]\\d{5}$";

    /**
     * 银行卡号（Luhn算法校验，简化版）
     */
    public static final String BANK_CARD = "^[1-9]\\d{13,19}$";

    /**
     * 中文汉字
     */
    public static final String CHINESE_CHARACTER = "^[\\u4e00-\\u9fa5]+$";

    /**
     * 英文字母
     */
    public static final String ENGLISH_LETTER = "^[a-zA-Z]+$";

    /**
     * 英文字母和数字
     */
    public static final String ALPHANUMERIC = "^[a-zA-Z0-9]+$";
}
