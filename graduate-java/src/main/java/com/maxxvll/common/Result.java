package com.maxxvll.common;

import com.maxxvll.common.enums.ErrorCode;

import java.io.Serializable;

/**
 * 全局统一返回结果
 * @param <T> data字段的泛型，支持任意数据类型
 */
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 响应码（200成功，其他为失败）
     */
    private int code;

    /**
     * 响应提示语
     */
    private String msg;

    /**
     * 响应数据
     */
    private T data;

    // 私有构造，禁止外部直接创建
    private Result() {}

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // ==================== 成功返回方法（多重载） ====================
    /**
     * 成功返回（无数据，默认提示语）
     */
    public static <T> Result<T> success() {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
    }

    /**
     * 成功返回（带数据，默认提示语）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), data);
    }
    /**
     * 成功返回（自定义提示语）
     */
    public static <T> Result<T> success(String msg) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), msg, null);
    }

    /**
     * 成功返回（自定义提示语+数据）
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), msg, data);
    }

    // ==================== 失败返回方法（多重载，满足不同场景） ====================
    /**
     * 失败返回（默认错误码+默认提示语）
     */
    public static <T> Result<T> fail() {
        return new Result<>(ErrorCode.FAIL.getCode(), ErrorCode.FAIL.getMsg(), null);
    }

    /**
     * 失败返回（自定义提示语，默认错误码）
     */
    public static <T> Result<T> fail(String msg) {
        return new Result<>(ErrorCode.FAIL.getCode(), msg, null);
    }

    /**
     * 失败返回（自定义错误码+自定义提示语）
     */
    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    /**
     * 失败返回（错误码枚举，默认提示语）
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMsg(), null);
    }

    /**
     * 失败返回（错误码枚举+自定义提示语）
     */
    public static <T> Result<T> fail(ErrorCode errorCode, String customMsg) {
        return new Result<>(errorCode.getCode(), customMsg, null);
    }

    /**
     * 失败返回（错误码枚举+自定义提示语+错误数据）
     */
    public static <T> Result<T> fail(ErrorCode errorCode, String customMsg, T data) {
        return new Result<>(errorCode.getCode(), customMsg, data);
    }

    // 手动添加getter/setter方法
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}