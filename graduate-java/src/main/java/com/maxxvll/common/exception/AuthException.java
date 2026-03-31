package com.maxxvll.common.exception;

import com.maxxvll.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 认证异常（401 Unauthorized）
 * 用于用户未登录、Token无效或已过期等场景
 *
 * @author backend
 */
@Getter
public class AuthException extends ApiException {

    public AuthException(String message) {
        super(ErrorCode.UNAUTHORIZED.getCode(), message, 401);
    }

    public AuthException(int code, String message) {
        super(code, message, 401);
    }

    public static AuthException notLoggedIn() {
        return new AuthException(ErrorCode.UNAUTHORIZED.getMsg());
    }

    public static AuthException tokenError() {
        return new AuthException(ErrorCode.TOKEN_ERROR.getCode(),
            ErrorCode.TOKEN_ERROR.getMsg());
    }

    public static AuthException tokenExpired() {
        return new AuthException(ErrorCode.TOKEN_EXPIRED.getCode(),
            ErrorCode.TOKEN_EXPIRED.getMsg());
    }

    public static AuthException tokenMissing() {
        return new AuthException(ErrorCode.TOKEN_MISSING.getCode(),
            ErrorCode.TOKEN_MISSING.getMsg());
    }
}
