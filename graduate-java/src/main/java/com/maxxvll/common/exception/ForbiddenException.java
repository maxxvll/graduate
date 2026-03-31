package com.maxxvll.common.exception;

import com.maxxvll.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 权限异常（403 Forbidden）
 * 用于用户无权限访问资源的场景
 *
 * @author backend
 */
@Getter
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN.getCode(), message, 403);
    }

    public ForbiddenException(int code, String message) {
        super(code, message, 403);
    }

    public static ForbiddenException noPermission() {
        return new ForbiddenException(ErrorCode.NO_PERMISSION.getMsg());
    }

    public static ForbiddenException noPermission(String detail) {
        return new ForbiddenException(ErrorCode.NO_PERMISSION.getCode(),
            ErrorCode.NO_PERMISSION.getMsg() + ": " + detail);
    }

    public static ForbiddenException roleNotMatch() {
        return new ForbiddenException(ErrorCode.ROLE_NOT_MATCH.getCode(),
            ErrorCode.ROLE_NOT_MATCH.getMsg());
    }
}
