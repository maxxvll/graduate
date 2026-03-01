package com.maxxvll.common;


import com.maxxvll.common.annotation.NotRequired;
import com.maxxvll.common.exception.DtoValidationException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.springframework.util.StringUtils;
@Aspect
@Component
public class GlobalValidation {
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void controllerCutPot() {
    }
    @Around("controllerCutPot()")
    public Object validateDto(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if(args==null || args.length== 0 ){
            return joinPoint.proceed();
        }
        Map<String,String> allErrors=new HashMap<>();
        for(Object arg:args){
            if(arg==null){
                continue;
            }
            Class<?> aClass = arg.getClass();
            if(isDTO(aClass)){
                Map<String,String> errors= validateDtoFields(arg);
                if(!errors.isEmpty()) {
                    allErrors.putAll(errors);
                }
            }
        }
        if(!allErrors.isEmpty()){
            throw new DtoValidationException(allErrors);
        }
        return joinPoint.proceed();
    }
    private boolean isDTO(Class<?> clazz){
        return clazz.getName().contains(".dto.") || clazz.getSimpleName().endsWith("DTO");
    }
    private Map<String,String> validateDtoFields(Object dto) throws IllegalAccessException{
        Map<String,String> errors=new HashMap<>();
        Field[] declaredFields = dto.getClass().getDeclaredFields();
        for(Field field :declaredFields){
            // 带有 @NotRequired 注解的字段为可选字段，跳过校验
            if (field.isAnnotationPresent(NotRequired.class)) {
                continue;
            }
            String filedName=field.getName();
            field.setAccessible(true);
            Object fieldValue = field.get(dto);
            if(fieldValue==null){
                errors.put(filedName,"字段不能为空");
                continue;
            }
            if(fieldValue instanceof String){
                String strValue = ((String) fieldValue).trim();
                if(!StringUtils.hasText(strValue)){
                    errors.put(filedName,"字段不能为空");
                }
            }
        }
        return errors;
    }
}
