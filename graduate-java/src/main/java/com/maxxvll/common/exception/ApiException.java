package com.maxxvll.common.exception;

import com.maxxvll.common.enums.ErrorCode;
import lombok.Getter;

/**
 * API基础异常
 * 所有API相关异常的基类
 *
 * @author backend
 */
@Getter
public class ApiException extends RuntimeException {
    private final int code;
    private final int httpStatus;

    public ApiException(String message) {
        super(message);
        this.code = ErrorCode.SYSTEM_ERROR.getCode();
        this.httpStatus = 500;
    }

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = 500;
    }

    public ApiException(int code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.httpStatus = mapToHttpStatus(code);
    }

    public ApiException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
        this.httpStatus = mapToHttpStatus(code);
    }

    private static int mapToHttpStatus(int code) {
        if (code >= 400 && code < 500) {
            return code;
        }
        if (code >= 500) {
            return 500;
        }
        return 500;
    }
}
