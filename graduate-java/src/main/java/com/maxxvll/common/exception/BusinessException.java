package com.maxxvll.common.exception;

import com.maxxvll.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 业务异常
 * 用于业务逻辑中的异常情况
 */
@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.BUSINESS_ERROR.getCode();
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = ErrorCode.BUSINESS_ERROR.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 使用 ErrorCode 枚举创建异常
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    /**
     * 使用 ErrorCode 枚举创建异常（自定义消息）
     */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
    }

    /**
     * 使用 ErrorCode 枚举创建异常（带原因）
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMsg(), cause);
        this.code = errorCode.getCode();
    }

    /**
     * 快速创建用户不存在异常
     */
    public static BusinessException userNotFound() {
        return new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    /**
     * 快速创建群组不存在异常
     */
    public static BusinessException groupNotFound() {
        return new BusinessException(ErrorCode.GROUP_NOT_FOUND);
    }

    /**
     * 快速创建文件不存在异常
     */
    public static BusinessException fileNotFound() {
        return new BusinessException(ErrorCode.FILE_NOT_FOUND);
    }

    /**
     * 快速创建无权限异常
     */
    public static BusinessException noPermission() {
        return new BusinessException(ErrorCode.NO_PERMISSION);
    }

    /**
     * 快速创建参数错误异常
     */
    public static BusinessException paramError(String message) {
        return new BusinessException(ErrorCode.PARAM_ERROR.getCode(), message);
    }
}
