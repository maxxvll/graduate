package com.maxxvll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxxvll.domain.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {
}
