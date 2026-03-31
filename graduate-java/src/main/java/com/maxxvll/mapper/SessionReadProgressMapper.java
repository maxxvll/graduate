package com.maxxvll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxxvll.domain.SessionReadProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 会话阅读进度Mapper
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Mapper
public interface SessionReadProgressMapper extends BaseMapper<SessionReadProgress> {

    /**
     * 获取用户在指定会话的阅读进度（所有设备）
     */
    List<SessionReadProgress> selectByUserAndSession(@Param("userId") String userId, @Param("sessionId") String sessionId);

    /**
     * 获取用户在指定设备的阅读进度
     */
    SessionReadProgress selectByUserSessionDevice(@Param("userId") String userId,
                                                  @Param("sessionId") String sessionId,
                                                  @Param("deviceId") String deviceId);

    /**
     * 插入或更新阅读进度
     */
    void upsert(SessionReadProgress progress);
}
