package com.maxxvll.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.domain.SysOperationLog;
import com.maxxvll.mapper.SysOperationLogMapper;
import com.maxxvll.service.SysOperationLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务实现
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Slf4j
@Service
public class SysOperationLogServiceImpl extends ServiceImpl<SysOperationLogMapper, SysOperationLog>
        implements SysOperationLogService {

    @Resource
    private SysOperationLogMapper sysOperationLogMapper;

    @Override
    @Async
    public void saveAsync(SysOperationLog operationLog) {
        try {
            long startTime = System.currentTimeMillis();
            sysOperationLogMapper.insert(operationLog);
            long costTime = System.currentTimeMillis() - startTime;

            log.debug("异步保存操作日志成功: id={}, module={}, action={}, costTime={}ms",
                    operationLog.getId(), operationLog.getModule(),
                    operationLog.getAction(), costTime);
        } catch (Exception e) {
            log.error("异步保存操作日志失败: module={}, action={}, error={}",
                    operationLog.getModule(), operationLog.getAction(), e.getMessage(), e);
        }
    }

    @Override
    public boolean saveSync(SysOperationLog operationLog) {
        try {
            return sysOperationLogMapper.insert(operationLog) > 0;
        } catch (Exception e) {
            log.error("同步保存操作日志失败: module={}, action={}, error={}",
                    operationLog.getModule(), operationLog.getAction(), e.getMessage(), e);
            return false;
        }
    }
}
