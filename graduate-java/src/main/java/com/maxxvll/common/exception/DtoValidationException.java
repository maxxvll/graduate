package com.maxxvll.common.exception;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
@Data
public class DtoValidationException extends RuntimeException{
    private final Map<String, String> errorMap;


    public DtoValidationException(String message) {
        super(message);
        this.errorMap = new HashMap<>();
    }
    public DtoValidationException(Map<String, String> errorMap) {
        super("DTO字段校验失败");
        this.errorMap = errorMap;
    }
}
