package com.maxxvll.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxxvll.domain.SysOperationLog;

/**
 * 操作日志服务接口
 *
 * @author Claude Code
 * @since 2026-03-31
 */
public interface SysOperationLogService extends IService<SysOperationLog> {

    /**
     * 异步保存操作日志
     *
     * @param operationLog 操作日志
     */
    void saveAsync(SysOperationLog operationLog);

    /**
     * 同步保存操作日志
     *
     * @param operationLog 操作日志
     * @return 是否保存成功
     */
    boolean saveSync(SysOperationLog operationLog);
}
