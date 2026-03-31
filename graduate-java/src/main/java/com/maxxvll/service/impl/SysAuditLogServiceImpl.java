package com.maxxvll.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.domain.SysAuditLog;
import com.maxxvll.mapper.SysAuditLogMapper;
import com.maxxvll.service.SysAuditLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务实现
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Slf4j
@Service
public class SysAuditLogServiceImpl extends ServiceImpl<SysAuditLogMapper, SysAuditLog>
        implements SysAuditLogService {

    @Resource
    private SysAuditLogMapper sysAuditLogMapper;

    @Override
    @Async
    public void saveAsync(SysAuditLog auditLog) {
        try {
            long startTime = System.currentTimeMillis();
            sysAuditLogMapper.insert(auditLog);
            long costTime = System.currentTimeMillis() - startTime;

            log.info("[审计] 类型={}, 动作={}, 用户={}, 目标={}, 风险={}, 耗时={}ms",
                    auditLog.getAuditType(), auditLog.getAction(),
                    auditLog.getUsername(), auditLog.getTargetName(),
                    auditLog.getRiskLevel(), costTime);
        } catch (Exception e) {
            log.error("[审计] 保存审计日志失败: 类型={}, 动作={}, error={}",
                    auditLog.getAuditType(), auditLog.getAction(), e.getMessage(), e);
        }
    }

    @Override
    public boolean saveSync(SysAuditLog auditLog) {
        try {
            return sysAuditLogMapper.insert(auditLog) > 0;
        } catch (Exception e) {
            log.error("[审计] 同步保存审计日志失败: 类型={}, 动作={}, error={}",
                    auditLog.getAuditType(), auditLog.getAction(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void recordAudit(String auditType, String action, String targetType,
                           String targetId, String targetName, String beforeValue,
                           String afterValue, String riskLevel) {
        SysAuditLog auditLog = new SysAuditLog();
        auditLog.setAuditType(auditType);
        auditLog.setAction(action);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        auditLog.setTargetName(targetName);
        auditLog.setBeforeValue(beforeValue);
        auditLog.setAfterValue(afterValue);
        auditLog.setRiskLevel(riskLevel);

        // 高风险操作单独告警
        if (SysAuditLog.RiskLevel.CRITICAL.name().equals(riskLevel) ||
            SysAuditLog.RiskLevel.HIGH.name().equals(riskLevel)) {
            log.warn("[高风险审计] 类型={}, 动作={}, 目标={}, 风险等级={}",
                    auditType, action, targetName, riskLevel);
        }

        saveAsync(auditLog);
    }
}
