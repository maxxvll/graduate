package com.maxxvll.common.constants;

/**
 * 业务常量定义
 *
 * <p>统一管理所有业务相关的限制值，包括长度限制、数量限制等</p>
 *
 * @author maxxvll
 * @since 2026-03-16
 */
public final class BusinessConstants {

    private BusinessConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 用户相关限制 ====================

    /**
     * 用户名最小长度
     */
    public static final int USERNAME_MIN_LENGTH = 3;

    /**
     * 用户名最大长度
     */
    public static final int USERNAME_MAX_LENGTH = 20;

    /**
     * 密码最小长度
     */
    public static final int PASSWORD_MIN_LENGTH = 6;

    /**
     * 密码最大长度
     */
    public static final int PASSWORD_MAX_LENGTH = 20;

    /**
     * 昵称最大长度
     */
    public static final int NICKNAME_MAX_LENGTH = 50;

    /**
     * 签名最大长度
     */
    public static final int SIGNATURE_MAX_LENGTH = 100;

    /**
     * 头像URL最大长度
     */
    public static final int AVATAR_URL_MAX_LENGTH = 500;

    // ==================== 消息相关限制 ====================

    /**
     * 消息内容最大长度
     */
    public static final int MESSAGE_CONTENT_MAX_LENGTH = 5000;

    /**
     * 文件名最大长度
     */
    public static final int FILE_NAME_MAX_LENGTH = 255;

    /**
     * 消息撤回时间限制（毫秒）- 2分钟
     */
    public static final long MESSAGE_REVOKE_TIME_LIMIT_MS = 2 * 60 * 1000;

    // ==================== 群组相关限制 ====================

    /**
     * 群名称最大长度
     */
    public static final int GROUP_NAME_MAX_LENGTH = 50;

    /**
     * 群公告最大长度
     */
    public static final int GROUP_ANNOUNCEMENT_MAX_LENGTH = 500;

    /**
     * 默认最大群成员数
     */
    public static final int DEFAULT_MAX_GROUP_MEMBERS = 200;

    /**
     * 最大群成员数上限
     */
    public static final int MAX_GROUP_MEMBERS_LIMIT = 500;

    /**
     * 创建群组最小成员数
     */
    public static final int MIN_GROUP_MEMBERS = 2;

    // ==================== 好友相关限制 ====================

    /**
     * 好友备注最大长度
     */
    public static final int FRIEND_REMARK_MAX_LENGTH = 64;

    /**
     * 好友标签最大长度
     */
    public static final int FRIEND_TAG_MAX_LENGTH = 64;

    /**
     * 申请/拒绝原因最大长度
     */
    public static final int APPLICATION_REASON_MAX_LENGTH = 100;

    // ==================== 云盘相关限制 ====================

    /**
     * 单个文件最大大小（字节）- 100MB
     */
    public static final long CLOUD_FILE_MAX_SIZE = 100 * 1024 * 1024L;

    /**
     * 云盘文件夹名称最大长度
     */
    public static final int CLOUD_FOLDER_NAME_MAX_LENGTH = 100;

    /**
     * 用户云盘默认存储空间（字节）- 5GB
     */
    public static final long DEFAULT_CLOUD_STORAGE_QUOTA = 5L * 1024 * 1024 * 1024;

    // ==================== 分页相关 ====================

    /**
     * 默认分页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 最大分页大小
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * 最小分页大小
     */
    public static final int MIN_PAGE_SIZE = 1;

    /**
     * 聊天消息默认分页大小
     */
    public static final int DEFAULT_MESSAGE_PAGE_SIZE = 50;

    // ==================== 批量操作相关 ====================

    /**
     * 默认批量大小
     */
    public static final int DEFAULT_BATCH_SIZE = 500;

    /**
     * 最大批量大小
     */
    public static final int MAX_BATCH_SIZE = 1000;

    /**
     * 最小批量大小
     */
    public static final int MIN_BATCH_SIZE = 1;

    // ==================== 语音通话相关 ====================

    /**
     * 通话扩展信息最大长度
     */
    public static final int CALL_EXTRA_MAX_LENGTH = 200;

    /**
     * 通话超时时间（秒）- 60秒
     */
    public static final int CALL_TIMEOUT_SECONDS = 60;

    // ==================== 文件上传相关 ====================

    /**
     * 文件分块大小（字节）- 5MB
     */
    public static final int FILE_CHUNK_SIZE = 5 * 1024 * 1024;

    /**
     * 单次上传文件最大大小（字节）- 15MB
     */
    public static final long FILE_UPLOAD_MAX_SIZE = 15 * 1024 * 1024L;

    /**
     * 允许上传的文件类型（MIME类型）
     */
    public static final String[] ALLOWED_FILE_TYPES = {
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/bmp",
            "image/webp",
            "application/pdf",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "video/mp4",
            "video/avi",
            "video/mov",
            "audio/mp3",
            "audio/wav"
    };

    // ==================== 验证码相关 ====================

    /**
     * 验证码长度
     */
    public static final int CAPTCHA_CODE_LENGTH = 4;

    /**
     * 邮箱验证码长度
     */
    public static final int EMAIL_CODE_LENGTH = 6;

    /**
     * 验证码有效期（秒）- 5分钟
     */
    public static final long CAPTCHA_EXPIRE_SECONDS = 5 * 60;

    /**
     * 验证码发送间隔（秒）- 60秒
     */
    public static final long CAPTCHA_SEND_INTERVAL_SECONDS = 60;

    // ==================== 安全相关 ====================

    /**
     * 登录失败最大次数
     */
    public static final int LOGIN_FAIL_MAX_COUNT = 5;

    /**
     * 登录锁定时间（秒）- 15分钟
     */
    public static final long LOGIN_LOCK_TIME_SECONDS = 15 * 60;

    /**
     * 二维码有效期（秒）- 5分钟
     */
    public static final long QR_CODE_EXPIRE_SECONDS = 5 * 60;
}
