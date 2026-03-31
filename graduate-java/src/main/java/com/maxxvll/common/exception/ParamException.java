package com.maxxvll.common.exception;

import com.maxxvll.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 参数异常（400 Bad Request）
 * 用于参数校验失败、参数格式错误等场景
 *
 * @author backend
 */
@Getter
public class ParamException extends ApiException {

    public ParamException(String message) {
        super(ErrorCode.PARAM_ERROR.getCode(), message, 400);
    }

    public ParamException(int code, String message) {
        super(code, message, 400);
    }

    public static ParamException missing(String paramName) {
        return new ParamException(ErrorCode.PARAM_MISSING.getCode(),
            "缺少必要参数: " + paramName);
    }

    public static ParamException invalid(String paramName, String reason) {
        return new ParamException(ErrorCode.PARAM_INVALID.getCode(),
            "参数格式错误: " + paramName + ", " + reason);
    }

    public static ParamException outOfRange(String paramName) {
        return new ParamException(ErrorCode.PARAM_OUT_OF_RANGE.getCode(),
            "参数超出范围: " + paramName);
    }
}
