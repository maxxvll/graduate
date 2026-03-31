package com.maxxvll.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxxvll.domain.SysAuditLog;

/**
 * 审计日志服务接口
 *
 * @author Claude Code
 * @since 2026-03-31
 */
public interface SysAuditLogService extends IService<SysAuditLog> {

    /**
     * 异步保存审计日志
     *
     * @param auditLog 审计日志
     */
    void saveAsync(SysAuditLog auditLog);

    /**
     * 同步保存审计日志
     *
     * @param auditLog 审计日志
     * @return 是否保存成功
     */
    boolean saveSync(SysAuditLog auditLog);

    /**
     * 记录敏感操作
     *
     * @param auditType 审计类型
     * @param action 操作动作
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @param targetName 目标名称
     * @param beforeValue 变更前值
     * @param afterValue 变更后值
     * @param riskLevel 风险等级
     */
    void recordAudit(String auditType, String action, String targetType,
                     String targetId, String targetName, String beforeValue,
                     String afterValue, String riskLevel);
}
