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
    // 通用成功/失败
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    // 客户端错误
    PARAM_ERROR(400, "参数校验失败"),
    TOKEN_ERROR(401, "令牌无效或已过期"),
    NO_PERMISSION(403, "无操作权限"),
    RESOURCE_NOT_FOUND(404, "请求资源不存在"),

    // 服务端错误
    SYSTEM_ERROR(500, "系统异常，请稍后重试"),
    BUSINESS_ERROR(501, "业务逻辑异常");

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


}