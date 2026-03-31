package com.maxxvll.common;


import com.maxxvll.common.annotation.NotRequired;
import com.maxxvll.common.exception.DtoValidationException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局 DTO 校验切面
 * <p>
 * 对所有 REST 控制器的 DTO 参数进行校验
 * </p>
 *
 * @author backend
 */
@Slf4j
@Aspect
@Component
public class GlobalValidation {

    private static final String FIELD_CANNOT_BE_EMPTY = "字段不能为空";

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void controllerCutPot() {
    }

    @Around("controllerCutPot()")
    public Object validateDto(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return joinPoint.proceed();
        }

        Map<String, String> allErrors = new HashMap<>();

        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            Class<?> clazz = arg.getClass();
            if (isDTO(clazz)) {
                Map<String, String> errors = validateDtoFields(arg);
                if (!errors.isEmpty()) {
                    allErrors.putAll(errors);
                }
            }
        }

        if (!allErrors.isEmpty()) {
            throw new DtoValidationException(allErrors);
        }

        return joinPoint.proceed();
    }

    /**
     * 判断类是否为 DTO
     */
    private boolean isDTO(Class<?> clazz) {
        return clazz.getName().contains(".dto.") || clazz.getSimpleName().endsWith("DTO");
    }

    /**
     * 校验 DTO 字段
     */
    private Map<String, String> validateDtoFields(Object dto) {
        Map<String, String> errors = new HashMap<>();
        Field[] declaredFields = dto.getClass().getDeclaredFields();

        for (Field field : declaredFields) {
            // 带有 @NotRequired 注解的字段为可选字段，跳过校验
            if (field.isAnnotationPresent(NotRequired.class)) {
                continue;
            }

            String fieldName = field.getName();
            field.setAccessible(true);

            try {
                Object fieldValue = field.get(dto);
                if (fieldValue == null) {
                    errors.put(fieldName, FIELD_CANNOT_BE_EMPTY);
                    continue;
                }

                if (fieldValue instanceof String strValue && !StringUtils.hasText(strValue.trim())) {
                    errors.put(fieldName, FIELD_CANNOT_BE_EMPTY);
                }
            } catch (IllegalAccessException e) {
                log.warn("字段访问失败: field={}, dto={}", fieldName, dto.getClass().getSimpleName());
            }
        }

        return errors;
    }
}
