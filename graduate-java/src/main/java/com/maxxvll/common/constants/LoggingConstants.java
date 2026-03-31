package com.maxxvll.common.constants;

/**
 * 日志常量
 * 用于统一日志前缀和格式
 */
public final class LoggingConstants {
    private LoggingConstants() {}

    /** 日志前缀 - 参数校验 */
    public static final String PREFIX_VALIDATION = "[VALIDATION]";

    /** 日志前缀 - 业务异常 */
    public static final String PREFIX_BUSINESS_EXCEPTION = "[BUSINESS_EXCEPTION]";

    /** 日志前缀 - 系统异常 */
    public static final String PREFIX_SYSTEM_EXCEPTION = "[SYSTEM_EXCEPTION]";

    /** 日志前缀 - Kafka消费 */
    public static final String PREFIX_KAFKA_CONSUMER = "[KAFKA_CONSUMER]";

    /** 日志前缀 - Kafka发送 */
    public static final String PREFIX_KAFKA_SEND = "[KAFKA_SEND]";

    /** 日志前缀 - Kafka消费（别名） */
    public static final String PREFIX_KAFKA_CONSUME = "[KAFKA_CONSUME]";

    /** 日志前缀 - Kafka生产者 */
    public static final String PREFIX_KAFKA_PRODUCER = "[KAFKA_PRODUCER]";

    /** 日志前缀 - WebSocket */
    public static final String PREFIX_WEBSOCKET = "[WEBSOCKET]";

    /** 日志前缀 - 数据库操作 */
    public static final String PREFIX_DATABASE = "[DATABASE]";

    /** 日志前缀 - 文件操作 */
    public static final String PREFIX_FILE = "[FILE]";

    /** 日志前缀 - 文件上传 */
    public static final String PREFIX_FILE_UPLOAD = "[FILE_UPLOAD]";

    /** 日志前缀 - MinIO */
    public static final String PREFIX_MINIO = "[MINIO]";

    /** 日志前缀 - Redis */
    public static final String PREFIX_REDIS = "[REDIS]";

    /** 日志前缀 - 缓存操作 */
    public static final String PREFIX_CACHE = "[CACHE]";

    /** 日志前缀 - 业务操作 */
    public static final String PREFIX_BUSINESS = "[BUSINESS]";

    /** 日志前缀 - 性能监控 */
    public static final String PREFIX_PERFORMANCE_MONITOR = "[PERFORMANCE_MONITOR]";

    /** 日志前缀 - 性能警告 */
    public static final String PREFIX_PERFORMANCE_WARN = "[PERFORMANCE_WARN]";

    /** 日志前缀 - 外部服务 */
    public static final String PREFIX_EXTERNAL_SERVICE = "[EXTERNAL_SERVICE]";

    /** 日志前缀 - 请求开始 */
    public static final String PREFIX_REQUEST_START = "[REQUEST_START]";

    /** 日志前缀 - 请求结束 */
    public static final String PREFIX_REQUEST_END = "[REQUEST_END]";

    /** 日志前缀 - 请求异常 */
    public static final String PREFIX_REQUEST_EXCEPTION = "[REQUEST_EXCEPTION]";

    /** MDC键名 - TraceId */
    public static final String MDC_TRACE_ID = "traceId";

    /** MDC键名 - UserId */
    public static final String MDC_USER_ID = "userId";

    /** MDC键名 - BusinessKey */
    public static final String MDC_BUSINESS_KEY = "businessKey";

    /** MDC键名 - Uri */
    public static final String MDC_URI = "uri";

    /** MDC键名 - Method */
    public static final String MDC_METHOD = "method";

    /** MDC键名 - Ip */
    public static final String MDC_IP = "ip";

    /** Kafka消费阈值时间（毫秒） */
    public static final long KAFKA_CONSUME_THRESHOLD_MS = 200L;

    /** Kafka发送阈值时间（毫秒） */
    public static final long KAFKA_SEND_THRESHOLD_MS = 500L;

    /** 数据库操作阈值时间（毫秒） */
    public static final long DATABASE_PERFORMANCE_THRESHOLD_MS = 1000L;

    /** 默认性能警告阈值（毫秒） */
    public static final long DEFAULT_PERFORMANCE_THRESHOLD_MS = 500L;

    /** Redis操作阈值时间（毫秒） */
    public static final long REDIS_THRESHOLD_MS = 100L;

    /** 缓存操作阈值时间（毫秒） */
    public static final long CACHE_THRESHOLD_MS = 50L;

    /** 外部服务调用阈值时间（毫秒） */
    public static final long EXTERNAL_SERVICE_THRESHOLD_MS = 3000L;

    /** 文件上传阈值时间（毫秒） */
    public static final long FILE_UPLOAD_THRESHOLD_MS = 5000L;

    // ==================== 业务操作名称 ====================

    /** 用户登录 */
    public static final String OP_USER_LOGIN = "用户登录";

    /** 用户登出 */
    public static final String OP_USER_LOGOUT = "用户登出";

    /** 用户注册 */
    public static final String OP_USER_REGISTER = "用户注册";

    /** 更新用户资料 */
    public static final String OP_USER_UPDATE_PROFILE = "更新用户资料";

    /** 发送消息 */
    public static final String OP_MESSAGE_SEND = "发送消息";

    /** 接收消息 */
    public static final String OP_MESSAGE_RECEIVE = "接收消息";

    /** 标记已读 */
    public static final String OP_MESSAGE_READ = "标记已读";

    /** 撤回消息 */
    public static final String OP_MESSAGE_RECALL = "撤回消息";

    // ==================== 审计日志常量 ====================

    /** 审计类型 - 权限变更 */
    public static final String AUDIT_PERMISSION = "PERMISSION";

    /** 审计类型 - 用户管理 */
    public static final String AUDIT_USER_MANAGEMENT = "USER_MANAGEMENT";

    /** 审计类型 - 系统配置 */
    public static final String AUDIT_SYSTEM_CONFIG = "SYSTEM_CONFIG";

    /** 审计类型 - 文件访问 */
    public static final String AUDIT_FILE_ACCESS = "FILE_ACCESS";

    /** 审计类型 - 数据导出 */
    public static final String AUDIT_DATA_EXPORT = "DATA_EXPORT";

    /** 风险等级 - 低 */
    public static final String RISK_LEVEL_LOW = "LOW";

    /** 风险等级 - 中 */
    public static final String RISK_LEVEL_MEDIUM = "MEDIUM";

    /** 风险等级 - 高 */
    public static final String RISK_LEVEL_HIGH = "HIGH";

    /** 风险等级 - 严重 */
    public static final String RISK_LEVEL_CRITICAL = "CRITICAL";

    /** 登录方式 - 密码登录 */
    public static final String LOGIN_TYPE_PASSWORD = "PASSWORD";

    /** 登录方式 - 扫码登录 */
    public static final String LOGIN_TYPE_QR_CODE = "QR_CODE";

    /** 登录方式 - 邮箱登录 */
    public static final String LOGIN_TYPE_EMAIL = "EMAIL";
}
