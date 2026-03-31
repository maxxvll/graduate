package com.maxxvll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxxvll.domain.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {
}
