package com.maxxvll.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.maxxvll.common.enums.ErrorCode;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.common.exception.DtoValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DtoValidationException.class)
    public Result<Map<String, String>> handleDtoValidationException(DtoValidationException e) {
        Map<String, String> errors = e.getErrorMap();
        log.error("参数校验失败：{}", errors);
        return Result.fail(ErrorCode.FAIL, "参数校验失败", errors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null ? 
            e.getBindingResult().getFieldError().getDefaultMessage() : "参数校验失败";
        log.error("参数校验失败：{}", message);
        return Result.fail(HttpStatus.BAD_REQUEST.value(), message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldError() != null ? 
            e.getBindingResult().getFieldError().getDefaultMessage() : "参数校验失败";
        log.error("参数校验失败：{}", message);
        return Result.fail(HttpStatus.BAD_REQUEST.value(), message);
    }

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        log.error("未登录或登录已过期");
        return Result.fail(HttpStatus.UNAUTHORIZED.value(), "未登录或登录已过期");
    }

    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException e) {
        log.error("无权限：{}", e.getPermission());
        return Result.fail(HttpStatus.FORBIDDEN.value(), "无权限：" + e.getPermission());
    }

    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException e) {
        log.error("无角色权限：{}", e.getRole());
        return Result.fail(HttpStatus.FORBIDDEN.value(), "无角色权限：" + e.getRole());
    }

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常：{}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Result<Void>> handleHttpMediaTypeNotAcceptableException(HttpMediaTypeNotAcceptableException e) {
        log.error("内容协商错误：{}", e.getMessage());
        Result<Void> result = Result.fail(HttpStatus.NOT_ACCEPTABLE.value(), "无法处理请求的内容类型");
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("服务器内部错误：{}", e.getMessage(), e);
        return Result.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误");
    }
}