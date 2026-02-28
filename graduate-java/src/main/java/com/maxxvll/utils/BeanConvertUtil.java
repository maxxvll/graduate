package com.maxxvll.common.util;

import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bean 转换工具类
 */
public class BeanConvertUtil {

    /**
     * 单个对象转换
     *
     * @param source 源对象
     * @param targetClass 目标对象类型
     * @param <T> 目标类型
     * @return 转换后的对象
     */
    public static <T> T convert(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("对象转换失败", e);
        }
    }

    /**
     * 列表对象转换
     *
     * @param sourceList 源对象列表
     * @param targetClass 目标对象类型
     * @param <T> 目标类型
     * @return 转换后的对象列表
     */
    public static <T> List<T> convertList(List<?> sourceList, Class<T> targetClass) {
        if (CollectionUtils.isEmpty(sourceList)) {
            return new ArrayList<>();
        }
        return sourceList.stream()
                .map(source -> convert(source, targetClass))
                .collect(Collectors.toList());
    }
}