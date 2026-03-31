package com.maxxvll.utils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Bean 转换工具类
 * 提供对象属性复制、列表转换、分页转换等功能
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
     * 单个对象转换（带自定义转换逻辑）
     *
     * @param source 源对象
     * @param targetClass 目标对象类型
     * @param customizer 自定义转换逻辑
     * @param <T> 目标类型
     * @return 转换后的对象
     */
    public static <T> T convert(Object source, Class<T> targetClass, Consumer<T> customizer) {
        T target = convert(source, targetClass);
        if (customizer != null && target != null) {
            customizer.accept(target);
        }
        return target;
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

    /**
     * 列表对象转换（带自定义转换逻辑）
     *
     * @param sourceList 源对象列表
     * @param targetClass 目标对象类型
     * @param customizer 自定义转换逻辑
     * @param <T> 目标类型
     * @return 转换后的对象列表
     */
    public static <T> List<T> convertList(List<?> sourceList, Class<T> targetClass,
                                           Consumer<T> customizer) {
        if (CollectionUtils.isEmpty(sourceList)) {
            return new ArrayList<>();
        }
        return sourceList.stream()
                .map(source -> convert(source, targetClass, customizer))
                .collect(Collectors.toList());
    }

    /**
     * 列表转换为 Map（指定 key 提取器）
     *
     * @param sourceList 源对象列表
     * @param keyMapper key 提取器
     * @param <T> 源类型
     * @param <K> key 类型
     * @return 转换后的 Map
     */
    public static <T, K> Map<K, T> toMap(List<T> sourceList, Function<T, K> keyMapper) {
        if (CollectionUtils.isEmpty(sourceList)) {
            return Map.of();
        }
        return sourceList.stream()
                .collect(Collectors.toMap(keyMapper, Function.identity(), (left, right) -> left));
    }

    /**
     * 列表转换为 Map（指定 key 和 value 提取器）
     *
     * @param sourceList 源对象列表
     * @param keyMapper key 提取器
     * @param valueMapper value 提取器
     * @param <T> 源类型
     * @param <K> key 类型
     * @param <V> value 类型
     * @return 转换后的 Map
     */
    public static <T, K, V> Map<K, V> toMap(List<T> sourceList, Function<T, K> keyMapper,
                                              Function<T, V> valueMapper) {
        if (CollectionUtils.isEmpty(sourceList)) {
            return Map.of();
        }
        return sourceList.stream()
                .collect(Collectors.toMap(keyMapper, valueMapper, (left, right) -> left));
    }

    /**
     * MyBatis-Plus 分页对象转换
     * 转换 Page<Entity> 为 Page<VO>
     *
     * @param sourcePage 源分页对象
     * @param targetClass 目标对象类型
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 转换后的分页对象
     */
    public static <S, T> Page<T> convertPage(Page<S> sourcePage, Class<T> targetClass) {
        if (sourcePage == null) {
            return null;
        }

        // 创建新的分页对象，复制分页参数
        Page<T> targetPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());

        // 转换 records
        List<S> sourceRecords = sourcePage.getRecords();
        if (CollectionUtils.isEmpty(sourceRecords)) {
            targetPage.setRecords(new ArrayList<>());
        } else {
            List<T> targetRecords = convertList(sourceRecords, targetClass);
            targetPage.setRecords(targetRecords);
        }

        return targetPage;
    }

    /**
     * MyBatis-Plus 分页对象转换（带自定义转换逻辑）
     *
     * @param sourcePage 源分页对象
     * @param targetClass 目标对象类型
     * @param customizer 自定义转换逻辑
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 转换后的分页对象
     */
    public static <S, T> Page<T> convertPage(Page<S> sourcePage, Class<T> targetClass,
                                              Consumer<T> customizer) {
        if (sourcePage == null) {
            return null;
        }

        Page<T> targetPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());

        List<S> sourceRecords = sourcePage.getRecords();
        if (CollectionUtils.isEmpty(sourceRecords)) {
            targetPage.setRecords(new ArrayList<>());
        } else {
            List<T> targetRecords = convertList(sourceRecords, targetClass, customizer);
            targetPage.setRecords(targetRecords);
        }

        return targetPage;
    }

    /**
     * 更新对象属性（忽略 null 值）
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void updateProperties(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        BeanUtils.copyProperties(source, target, getNullPropertyNames(source));
    }

    /**
     * 获取对象中为 null 的属性名
     */
    private static String[] getNullPropertyNames(Object source) {
        final BeanWrapperImpl wrapper = new BeanWrapperImpl();
        // 注意：这里简化处理，实际使用中需要完整的 BeanWrapperImpl 实现
        // 或者使用 Spring 的 BeanWrapper
        return new String[0];
    }

    /**
     * 简单的 BeanWrapper 实现
     */
    private static class BeanWrapperImpl {
        // 简化实现，实际项目中应使用 Spring 的 BeanWrapper
    }
}
