package com.maxxvll.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.common.constants.BatchConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * 批量操作工具类
 * 提供批量插入、更新、删除等操作的模板方法
 *
 * @author backend-friend
 */
@Slf4j
public class BatchUtil {

    /**
     * 批量处理（自动分批）
     *
     * @param items 待处理的数据列表
     * @param batchSize 批次大小
     * @param consumer 处理逻辑
     * @param <T> 数据类型
     */
    public static <T> void processBatch(List<T> items, int batchSize, Consumer<List<T>> consumer) {
        if (items == null || items.isEmpty()) {
            return;
        }

        int size = items.size();
        for (int i = 0; i < size; i += batchSize) {
            int end = Math.min(i + batchSize, size);
            List<T> batch = items.subList(i, end);
            consumer.accept(batch);
        }

        log.debug("批量处理完成, 总数={}, 批次大小={}, 处理批次数={}", size, batchSize, (size + batchSize - 1) / batchSize);
    }

    /**
     * 批量处理（使用默认批次大小）
     *
     * @param items 待处理的数据列表
     * @param consumer 处理逻辑
     * @param <T> 数据类型
     */
    public static <T> void processBatch(List<T> items, Consumer<List<T>> consumer) {
        processBatch(items, BatchConstants.DEFAULT_BATCH_SIZE, consumer);
    }

    /**
     * 批量保存（自动分批）
     *
     * @param service MyBatis-Plus Service 实例
     * @param items 待保存的数据列表
     * @param <T> 实体类型
     * @param <S> Service 类型
     * @return 成功保存的记录数
     */
    public static <T, S extends ServiceImpl<?, T>> int saveBatch(S service, List<T> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }

        int batchSize = BatchConstants.DEFAULT_BATCH_SIZE;
        List<List<T>> batches = splitList(items, batchSize);
        int totalSaved = 0;

        for (List<T> batch : batches) {
            boolean success = service.saveBatch(batch);
            if (success) {
                totalSaved += batch.size();
            }
        }

        log.debug("批量保存完成, 总数={}, 成功={}", items.size(), totalSaved);
        return totalSaved;
    }

    /**
     * 批量更新（自动分批）
     *
     * @param service MyBatis-Plus Service 实例
     * @param items 待更新的数据列表
     * @param <T> 实体类型
     * @param <S> Service 类型
     * @return 成功更新的记录数
     */
    public static <T, S extends ServiceImpl<?, T>> int updateBatchById(S service, List<T> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }

        int batchSize = BatchConstants.DEFAULT_BATCH_SIZE;
        List<List<T>> batches = splitList(items, batchSize);
        int totalUpdated = 0;

        for (List<T> batch : batches) {
            boolean success = service.updateBatchById(batch);
            if (success) {
                totalUpdated += batch.size();
            }
        }

        log.debug("批量更新完成, 总数={}, 成功={}", items.size(), totalUpdated);
        return totalUpdated;
    }

    /**
     * 批量删除（自动分批）
     *
     * @param service MyBatis-Plus Service 实例
     * @param ids 待删除的ID列表
     * @param <T> 实体类型
     * @param <S> Service 类型
     * @return 成功删除的记录数
     */
    public static <T, S extends ServiceImpl<?, T>> int removeBatchByIds(S service, List<?> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        boolean success = service.removeByIds(ids);
        return success ? ids.size() : 0;
    }

    /**
     * 批量查询（自动分批，避免 IN 子句过长）
     *
     * @param mapper MyBatis-Plus Mapper 实例
     * @param ids ID列表
     * @param batchSize 批次大小
     * @param <T> 实体类型
     * @param <ID> ID类型
     * @return 查询结果列表
     */
    public static <T, ID extends Serializable> List<T> selectBatchIds(BaseMapper<T> mapper, List<ID> ids, int batchSize) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        List<T> result = new ArrayList<>();
        List<List<ID>> batches = splitList(new ArrayList<>(ids), batchSize);

        for (List<ID> batch : batches) {
            Collection<Serializable> serializableBatch = new ArrayList<>(batch);
            List<T> batchResult = mapper.selectBatchIds(serializableBatch);
            if (batchResult != null) {
                result.addAll(batchResult);
            }
        }

        log.debug("批量查询完成, 总数={}, 批次大小={}, 查询批次数={}", ids.size(), batchSize, batches.size());
        return result;
    }

    /**
     * 批量条件查询（自动分批）
     *
     * @param mapper MyBatis-Plus Mapper 实例
     * @param ids ID列表
     * @param field 字段名
     * @param <T> 实体类型
     * @param <ID> ID类型
     * @return 查询结果列表
     */
    public static <T, ID> List<T> selectInBatch(BaseMapper<T> mapper, List<ID> ids,
                                                java.util.function.Function<List<ID>, LambdaQueryWrapper<T>> wrapperFunction) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        int batchSize = BatchConstants.DEFAULT_BATCH_SIZE;
        List<List<ID>> batches = splitList(new ArrayList<>(ids), batchSize);
        List<T> result = new ArrayList<>();

        for (List<ID> batch : batches) {
            LambdaQueryWrapper<T> wrapper = wrapperFunction.apply(batch);
            List<T> batchResult = mapper.selectList(wrapper);
            if (batchResult != null) {
                result.addAll(batchResult);
            }
        }

        log.debug("批量条件查询完成, 总数={}, 批次大小={}, 查询批次数={}", ids.size(), batchSize, batches.size());
        return result;
    }

    /**
     * 分割列表
     */
    private static <T> List<List<T>> splitList(List<T> list, int batchSize) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        List<List<T>> result = new ArrayList<>();
        int size = list.size();
        for (int i = 0; i < size; i += batchSize) {
            int end = Math.min(i + batchSize, size);
            result.add(new ArrayList<>(list.subList(i, end)));
        }
        return result;
    }

    /**
     * 计算批次数
     */
    public static int calculateBatchCount(int totalSize, int batchSize) {
        if (totalSize <= 0) {
            return 0;
        }
        return (totalSize + batchSize - 1) / batchSize;
    }

    private BatchUtil() {
    }
}
