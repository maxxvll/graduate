package com.maxxvll.common.exception;

import com.maxxvll.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 资源不存在异常（404 Not Found）
 * 用于请求的资源不存在的场景
 *
 * @author backend
 */
@Getter
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND.getCode(), message, 404);
    }

    public NotFoundException(int code, String message) {
        super(code, message, 404);
    }

    public static NotFoundException resource(String resourceType) {
        return new NotFoundException(ErrorCode.RESOURCE_NOT_FOUND.getCode(),
            resourceType + "不存在");
    }

    public static NotFoundException user() {
        return new NotFoundException(ErrorCode.USER_NOT_FOUND.getCode(),
            ErrorCode.USER_NOT_FOUND.getMsg());
    }

    public static NotFoundException user(Long userId) {
        return new NotFoundException(ErrorCode.USER_NOT_FOUND.getCode(),
            "用户不存在: " + userId);
    }

    public static NotFoundException group() {
        return new NotFoundException(ErrorCode.GROUP_NOT_FOUND.getCode(),
            ErrorCode.GROUP_NOT_FOUND.getMsg());
    }

    public static NotFoundException group(Long groupId) {
        return new NotFoundException(ErrorCode.GROUP_NOT_FOUND.getCode(),
            "群组不存在: " + groupId);
    }

    public static NotFoundException file() {
        return new NotFoundException(ErrorCode.FILE_NOT_FOUND.getCode(),
            ErrorCode.FILE_NOT_FOUND.getMsg());
    }

    public static NotFoundException file(String fileId) {
        return new NotFoundException(ErrorCode.FILE_NOT_FOUND.getCode(),
            "文件不存在: " + fileId);
    }

    public static NotFoundException message(Long messageId) {
        return new NotFoundException(ErrorCode.MESSAGE_NOT_FOUND.getCode(),
            "消息不存在: " + messageId);
    }
}
