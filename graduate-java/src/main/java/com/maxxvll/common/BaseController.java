package com.maxxvll.common;

import com.maxxvll.common.enums.ErrorCode;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制器基类
 * 所有业务控制器需继承此类，统一返回格式和通用方法
 * @RestController 注解：标记为REST控制器，返回JSON数据
 */
@RestController
public class BaseController {

    // ==================== 成功返回快捷方法 ====================
    /**
     * 成功返回（无数据）
     */
    protected <T> Result<T> success() {
        return Result.success();
    }

    /**
     * 成功返回（带数据）
     */
    protected <T> Result<T> success(T data) {
        return Result.success(data);
    }

    /**
     * 成功返回（自定义提示语+数据）
     */
    protected <T> Result<T> success(String msg, T data) {
        return Result.success(msg, data);
    }

    // ==================== 失败返回快捷方法（多重载） ====================
    /**
     * 失败返回（默认错误）
     */
    protected <T> Result<T> fail() {
        return Result.fail();
    }

    /**
     * 失败返回（自定义提示语）
     */
    protected <T> Result<T> fail(String msg) {
        return Result.fail(msg);
    }

    /**
     * 失败返回（自定义错误码+提示语）
     */
    protected <T> Result<T> fail(int code, String msg) {
        return Result.fail(code, msg);
    }

    /**
     * 失败返回（错误码枚举）
     */
    protected <T> Result<T> fail(ErrorCode errorCode) {
        return Result.fail(errorCode);
    }

    /**
     * 失败返回（错误码枚举+自定义提示语）
     */
    protected <T> Result<T> fail(ErrorCode errorCode, String customMsg) {
        return Result.fail(errorCode, customMsg);
    }

    /**
     * 失败返回（错误码枚举+自定义提示语+错误数据）
     */
    protected <T> Result<T> fail(ErrorCode errorCode, String customMsg, T data) {
        return Result.fail(errorCode, customMsg, data);
    }
}