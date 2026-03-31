package com.maxxvll.common.enums;

/**
 * 全局错误码枚举
 * 规范：
 * 1. 2xx：成功相关
 * 2. 4xx：客户端错误（参数、权限等）
 * 3. 5xx：服务端错误（系统、业务异常等）
 * 4. 可根据业务扩展子段（如4001xx：商品相关，4002xx：用户相关）
 */
public enum ErrorCode {
    // ==================== 通用成功/失败 ====================
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    // ==================== 客户端错误 4xx ====================

    // 400 参数错误
    PARAM_ERROR(400, "参数校验失败"),
    PARAM_MISSING(4001, "缺少必要参数"),
    PARAM_INVALID(4002, "参数格式错误"),
    PARAM_OUT_OF_RANGE(4003, "参数超出范围"),

    // 401 认证错误
    UNAUTHORIZED(401, "未登录或登录已过期"),
    TOKEN_ERROR(4011, "令牌无效或已过期"),
    TOKEN_EXPIRED(4012, "令牌已过期"),
    TOKEN_MISSING(4013, "缺少令牌"),

    // 403 权限错误
    FORBIDDEN(403, "无操作权限"),
    NO_PERMISSION(4031, "无操作权限"),
    ROLE_NOT_MATCH(4032, "角色权限不足"),

    // 404 资源不存在
    NOT_FOUND(404, "请求资源不存在"),
    RESOURCE_NOT_FOUND(4041, "请求资源不存在"),
    USER_NOT_FOUND(4042, "用户不存在"),
    GROUP_NOT_FOUND(4043, "群组不存在"),
    FILE_NOT_FOUND(4044, "文件不存在"),

    // 409 冲突
    CONFLICT(409, "资源冲突"),
    USERNAME_EXISTS(4091, "用户名已存在"),
    RESOURCE_CONFLICT(4092, "资源冲突"),

    // 429 请求过多
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    // ==================== 服务端错误 5xx ====================

    // 500 系统错误
    SYSTEM_ERROR(500, "系统异常，请稍后重试"),
    INTERNAL_ERROR(5001, "系统内部错误"),

    // 501 业务异常
    BUSINESS_ERROR(501, "业务逻辑异常"),
    BUSINESS_LOGIC_ERROR(5011, "业务逻辑异常"),

    // 502 外部服务错误
    EXTERNAL_SERVICE_ERROR(502, "外部服务异常"),

    // 503 服务不可用
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),

    // ==================== 业务相关错误码 ====================

    // 用户相关 51xx
    USER_DISABLED(5101, "用户已被禁用"),
    USER_LOCKED(5102, "用户已被锁定"),
    PASSWORD_ERROR(5103, "密码错误"),
    OLD_PASSWORD_ERROR(5104, "原密码错误"),

    // 群组相关 52xx
    GROUP_MEMBER_EXIST(5201, "已是群组成员"),
    GROUP_MEMBER_NOT_EXIST(5202, "不是群组成员"),
    GROUP_OWNER_CANNOT_LEAVE(5203, "群主不能退出群组"),
    GROUP_NAME_DUPLICATE(5204, "群组名称重复"),

    // 好友相关 53xx
    FRIEND_ALREADY_EXISTS(5301, "已经是好友关系"),
    FRIEND_NOT_EXISTS(5302, "好友关系不存在"),
    FRIEND_APPLICATION_EXISTS(5303, "好友申请已存在"),
    FRIEND_APPLICATION_NOT_EXISTS(5304, "好友申请不存在"),
    IN_BLACKLIST(5305, "对方已在黑名单中"),

    // 文件相关 54xx
    FILE_UPLOAD_ERROR(5401, "文件上传失败"),
    FILE_DOWNLOAD_ERROR(5402, "文件下载失败"),
    FILE_DELETE_ERROR(5403, "文件删除失败"),
    FILE_SIZE_EXCEEDED(5404, "文件大小超出限制"),
    FILE_TYPE_NOT_ALLOWED(5405, "文件类型不允许"),
    STORAGE_QUOTA_EXCEEDED(5406, "存储空间不足"),

    // 消息相关 55xx
    MESSAGE_SEND_FAILED(5501, "消息发送失败"),
    MESSAGE_NOT_FOUND(5502, "消息不存在"),
    MESSAGE_RECALL_TIMEOUT(5503, "消息撤回超时"),

    // 分享相关 56xx
    SHARE_NOT_EXISTS(5601, "分享不存在"),
    SHARE_EXPIRED(5602, "分享已过期"),
    SHARE_PASSWORD_ERROR(5603, "分享密码错误"),

    // 云盘相关 57xx
    CLOUD_FILE_NOT_EXISTS(5701, "云盘文件不存在"),
    CLOUD_FOLDER_NOT_EXISTS(5702, "文件夹不存在"),
    CLOUD_FILE_EXISTS(5703, "文件已存在"),
    CLOUD_QUOTA_EXCEEDED(5704, "云盘存储空间不足");

    // 错误码
    private final int code;
    // 错误提示语
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    // 获取错误码
    public int getCode() {
        return code;
    }

    // 获取错误提示
    public String getMsg() {
        return msg;
    }

    /**
     * 根据错误码获取枚举
     */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        return SYSTEM_ERROR;
    }

    /**
     * 是否为成功
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * 是否为客户端错误
     */
    public boolean isClientError() {
        return code >= 400 && code < 500;
    }

    /**
     * 是否为服务端错误
     */
    public boolean isServerError() {
        return code >= 500;
    }
}