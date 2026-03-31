package com.maxxvll.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.maxxvll.common.constants.LoggingConstants;
import com.maxxvll.common.enums.ErrorCode;
import com.maxxvll.common.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一处理所有API异常，提供：
 * - 异常信息脱敏（不暴露内部堆栈）
 * - 统一的错误码规范
 * - 详细的日志记录
 * </p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ==================== API异常处理 ====================

    /**
     * 处理参数异常
     */
    @ExceptionHandler(ParamException.class)
    public Result<Void> handleParamException(ParamException e, HttpServletRequest request) {
        log.warn("{} 参数异常, uri={}, traceId={}, code={}, message={}",
            LoggingConstants.PREFIX_VALIDATION,
            request.getRequestURI(),
            org.slf4j.MDC.get("traceId"),
            e.getCode(),
            e.getMessage());

        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthException.class)
    public Result<Void> handleAuthException(AuthException e, HttpServletRequest request) {
        log.warn("{} 认证异常, uri={}, traceId={}, code={}, message={}",
            LoggingConstants.PREFIX_SYSTEM_EXCEPTION,
            request.getRequestURI(),
            org.slf4j.MDC.get("traceId"),
            e.getCode(),
            e.getMessage());

        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理权限异常
     */
    @ExceptionHandler(ForbiddenException.class)
    public Result<Void> handleForbiddenException(ForbiddenException e, HttpServletRequest request) {
        log.warn("{} 权限异常, uri={}, traceId={}, code={}, message={}",
            LoggingConstants.PREFIX_SYSTEM_EXCEPTION,
            request.getRequestURI(),
            org.slf4j.MDC.get("traceId"),
            e.getCode(),
            e.getMessage());

        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理资源不存在异常
     */
    @ExceptionHandler(NotFoundException.class)
    public Result<Void> handleNotFoundException(NotFoundException e, HttpServletRequest request) {
        log.warn("{} 资源不存在, uri={}, traceId={}, code={}, message={}",
            LoggingConstants.PREFIX_SYSTEM_EXCEPTION,
            request.getRequestURI(),
            org.slf4j.MDC.get("traceId"),
            e.getCode(),
            e.getMessage());

        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理API基础异常
     */
    @ExceptionHandler(ApiException.class)
    public Result<Void> handleApiException(ApiException e, HttpServletRequest request) {
        log.error("{} API异常, uri={}, traceId={}, code={}, httpStatus={}, message={}",
            LoggingConstants.PREFIX_SYSTEM_EXCEPTION,
            request.getRequestURI(),
            org.slf4j.MDC.get("traceId"),
            e.getCode(),
            e.getHttpStatus(),
            e.getMessage());

        return Result.fail(e.getCode(), e.getMessage());
    }

    // ==================== 业务异常处理 ====================

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("{} 业务异常, uri={}, traceId={}, code={}, message={}",
            LoggingConstants.PREFIX_BUSINESS_EXCEPTION,
            request.getRequestURI(),
            org.slf4j.MDC.get("traceId"),
            e.getCode(),
            e.getMessage());

        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理DTO校验异常（多字段错误）
     */
    @ExceptionHandler(DtoValidationException.class)
    public Result<Map<String, String>> handleDtoValidationException(
            DtoValidationException e, HttpServletRequest request) {
        Map<String, String> errors = e.getErrorMap();
        String errorDetails = errors.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .collect(Collectors.joining(", "));

        log.error("{} 参数校验失败, uri={}, traceId={}, errors={}",
            LoggingConstants.PREFIX_VALIDATION,
            request.getRequestURI(),
            org.slf4j.MDC.get("traceId"),
            errors);

        return Result.fail(ErrorCode.PARAM_ERROR, "参数校验失败", errors);
    }

    /**
     * 处理方法参数校验异常（单字段错误）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String fieldName = fieldError != null ? fieldError.getField() : "unknown";
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";

        log.warn("{} 参数校验失败, uri={}, traceId={}, field={}, message={}",
            LoggingConstants.PREFIX_VALIDATION,
            request.getRequestURI(),
            org.slf4j.MDC.get("traceId"),
            fieldName,
            message);

        return Result.fail(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String fieldName = fieldError != null ? fieldError.getField() : "unknown";
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数绑定失败";

        log.warn("{} 参数绑定失败, uri={}, traceId={}, field={}, message={}",
            LoggingConstants.PREFIX_VALIDATION,
            request.getRequestURI(),
            org.slf4j.MDC.get("traceId"),
            fieldName,
            message);

        return Result.fail(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("{} 缺少请求参数, uri={}, traceId={}, parameter={}, type={}",
            LoggingConstants.PREFIX_VALIDATION,
            request.getRequestURI(),
            org.slf4j.MDC.get("traceId"),
            e.getParameterName(),
            e.getParameterType());

        return Result.fail(ErrorCode.PARAM_MISSING.getCode(),
            "缺少必要参数: " + e.getParameterName());
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("{} 参数类型不匹配, uri={}, traceId={}, parameter={}, value={}, requiredType={}",
            LoggingConstants.PREFIX_VALIDATION,
            request.getRequestURI(),
            org.slf4j.MDC.get("traceId"),
            e.getName(),
            e.getValue(),
            e.getRequiredType());

        String message = String.format("参数 %s 类型错误，期望值类型: %s",
            e.getName(),
            e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");
        return Result.fail(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    // ==================== Sa-Token 认证异常处理 ====================

    /**
     * 处理未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e, HttpServletRequest request) {
        log.warn("{} 用户未登录, uri={}, traceId={}, message={}",
            LoggingConstants.PREFIX_SYSTEM_EXCEPTION,
            request.getRequestURI(),
            org.slf4j.MDC.get("traceId"),
            e.getMessage());

        return Result.fail(HttpStatus.UNAUTHORIZED.value(), ErrorCode.UNAUTHORIZED.getMsg());
    }

    /**
     * 处理权限不足异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException e, HttpServletRequest request) {
        log.warn("{} 权限不足, uri={}, traceId={}, permission={}",
            LoggingConstants.PREFIX_SYSTEM_EXCEPTION,
            request.getRequestURI(),
            org.slf4j.MDC.get("traceId"),
            e.getPermission());

        return Result.fail(HttpStatus.FORBIDDEN.value(), "无权限: " + e.getPermission());
    }

    /**
     * 处理角色权限不足异常
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException e, HttpServletRequest request) {
        log.warn("{} 角色权限不足, uri={}, traceId={}, role={}",
            LoggingConstants.PREFIX_SYSTEM_EXCEPTION,
            request.getRequestURI(),
            org.slf4j.MDC.get("traceId"),
            e.getRole());

        return Result.fail(HttpStatus.FORBIDDEN.value(), "无角色权限: " + e.getRole());
    }

    // ==================== 其他异常处理 ====================

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("{} 资源不存在, uri={}, traceId={}, message={}",
            LoggingConstants.PREFIX_SYSTEM_EXCEPTION,
            request.getRequestURI(),
            org.slf4j.MDC.get("traceId"),
            e.getMessage());

        return Result.fail(HttpStatus.NOT_FOUND.value(), "请求的资源不存在");
    }

    /**
     * 处理内容协商异常
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Result<Void>> handleHttpMediaTypeNotAcceptableException(
            HttpMediaTypeNotAcceptableException e, HttpServletRequest request) {
        log.error("{} 内容协商失败, uri={}, traceId={}, message={}",
            LoggingConstants.PREFIX_SYSTEM_EXCEPTION,
            request.getRequestURI(),
            org.slf4j.MDC.get("traceId"),
            e.getMessage());

        Result<Void> result = Result.fail(HttpStatus.NOT_ACCEPTABLE.value(), "无法处理请求的内容类型");
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    /**
     * 处理所有未分类异常（兜底）
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("{} 系统异常, uri={}, traceId={}, exceptionType={}, message={}",
            LoggingConstants.PREFIX_SYSTEM_EXCEPTION,
            request.getRequestURI(),
            org.slf4j.MDC.get("traceId"),
            e.getClass().getSimpleName(),
            e.getMessage(),
            e);

        return Result.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误");
    }
}
