package com.maxxvll.common.annotation;

import java.lang.annotation.*;

/**
 * 标记 DTO 中的可选字段，带有此注解的字段将跳过 GlobalValidation 的非空校验。
 * <p>
 * 适用场景：字段在业务上是可选的（如"拒绝原因"只在拒绝时填写、"申请备注"用户可留空）。
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NotRequired {
}
