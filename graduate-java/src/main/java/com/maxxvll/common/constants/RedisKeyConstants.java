package com.maxxvll.common.constants;

/**
 * Redis Key 常量定义
 *
 * <p>统一管理所有 Redis key 的前缀格式，避免 key 冲突和拼写错误。</p>
 *
 * <p>Key 格式规范：
 * <ul>
 *   <li>使用冒号(:)作为分隔符</li>
 *   <li>前缀格式：模块:子模块:具体标识</li>
 *   <li>示例：user:info:123、chat:session:456</li>
 * </ul>
 *
 * @author maxxvll
 * @since 2026-03-16
 */
public interface RedisKeyConstants {

    /**
     * 分隔符
     */
    String SEPARATOR = ":";

    // ==================== 基础前缀 ====================

    /** 用户模块前缀 */
    String USER_PREFIX = "user";

    /** 好友模块前缀 */
    String FRIEND_PREFIX = "friend";

    /** 群组模块前缀 */
    String GROUP_PREFIX = "group";

    /** 聊天模块前缀 */
    String CHAT_PREFIX = "chat";

    /** 会话模块前缀 */
    String SESSION_PREFIX = "session";

    /** 文件模块前缀 */
    String FILE_PREFIX = "file";

    /** 云盘模块前缀 */
    String CLOUD_PREFIX = "cloud";

    // ==================== 用户模块 Keys ====================

    /** 用户信息 Key：user:info:{userId} */
    String USER_INFO = buildKey(USER_PREFIX, "info");

    /** 用户登录失败次数 Key：user:login:fail:{username} */
    String USER_LOGIN_FAIL = buildKey(USER_PREFIX, "login", "fail");

    /** 用户登录锁定 Key：user:login:lock:{username} */
    String USER_LOGIN_LOCK = buildKey(USER_PREFIX, "login", "lock");

    /** 扫码登录 Key：user:qr:{qrCodeId} */
    String USER_QR_LOGIN = buildKey(USER_PREFIX, "qr");

    /** 验证码 Key：user:captcha:{captchaKey} */
    String USER_CAPTCHA = buildKey(USER_PREFIX, "captcha");

    /** 邮箱验证码 Key：user:email:code:{email} */
    String USER_EMAIL_CODE = buildKey(USER_PREFIX, "email", "code");

    /** WebSocket Token Key：user:ws:token:{token} */
    String USER_WS_TOKEN = buildKey(USER_PREFIX, "ws", "token");

    /** 用户权限缓存 Key：user:permission:{userId} */
    String USER_PERMISSION = buildKey(USER_PREFIX, "permission");

    /** 用户角色缓存 Key：user:role:{userId} */
    String USER_ROLE = buildKey(USER_PREFIX, "role");

    // ==================== 好友模块 Keys ====================

    /** 好友列表 Key：friend:list:{userId} */
    String FRIEND_LIST = buildKey(FRIEND_PREFIX, "list");

    /** 好友黑名单 Key：friend:blacklist:{userId} */
    String FRIEND_BLACKLIST = buildKey(FRIEND_PREFIX, "blacklist");

    /** 好友关系设置 Key：friend:setting:{userId}:{friendUserId} */
    String FRIEND_SETTING = buildKey(FRIEND_PREFIX, "setting");

    // ==================== 群组模块 Keys ====================

    /** 群成员列表 Key：group:members:{groupId} */
    String GROUP_MEMBERS = buildKey(GROUP_PREFIX, "members");

    /** 用户管理的群组 Key：group:managed:{userId} */
    String GROUP_MANAGED = buildKey(GROUP_PREFIX, "managed");

    // ==================== 会话模块 Keys ====================

    /** 聊天会话 Key：session:chat:{sessionId} */
    String SESSION_CHAT = buildKey(SESSION_PREFIX, "chat");

    // ==================== 文件模块 Keys ====================

    /** 文件上传进度 Key：file:upload:progress:{fileId} */
    String FILE_UPLOAD_PROGRESS = buildKey(FILE_PREFIX, "upload", "progress");

    // ==================== 云盘模块 Keys ====================

    /** 云盘使用量 Key：cloud:usage:{userId} */
    String CLOUD_USAGE = buildKey(CLOUD_PREFIX, "usage");

    // ==================== 缓存配置 ====================

    /** 默认缓存过期时间（秒）- 5分钟 */
    long CACHE_EXPIRE_SECONDS = 300;

    /** 短缓存过期时间（秒）- 1分钟 */
    long CACHE_EXPIRE_SHORT = 60;

    /** 长缓存过期时间（秒）- 1小时 */
    long CACHE_EXPIRE_LONG = 3600;

    /** 登录失败锁定过期时间（秒）- 15分钟 */
    long LOGIN_LOCK_EXPIRE_SECONDS = 900;

    /** 二维码过期时间（秒）- 5分钟 */
    long QR_CODE_EXPIRE_SECONDS = 300;

    /**
     * 构建 Redis Key
     *
     * @param prefix  前缀
     * @param items   可变参数
     * @return 完整的 Key
     */
    static String buildKey(String prefix, String... items) {
        StringBuilder sb = new StringBuilder(prefix);
        for (String item : items) {
            sb.append(SEPARATOR).append(item);
        }
        return sb.toString();
    }
}
