package com.maxxvll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxxvll.domain.SearchHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 搜索历史Mapper
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Mapper
public interface SearchHistoryMapper extends BaseMapper<SearchHistory> {

    /**
     * 增加搜索次数
     */
    @Update("UPDATE search_history SET search_count = search_count + 1, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND keyword = #{keyword} AND search_type = #{searchType}")
    int incrementSearchCount(@Param("userId") String userId, @Param("keyword") String keyword, @Param("searchType") String searchType);

    /**
     * 获取用户搜索历史
     */
    List<SearchHistory> selectByUserId(@Param("userId") String userId, @Param("limit") int limit);

    /**
     * 清理超出的历史记录
     */
    @Update("DELETE FROM search_history WHERE user_id = #{userId} AND is_deleted = 0 " +
            "AND id NOT IN (SELECT id FROM (SELECT id FROM search_history WHERE user_id = #{userId} " +
            "AND is_deleted = 0 ORDER BY updated_at DESC LIMIT #{keepCount}) AS t)")
    int deleteOverflowHistory(@Param("userId") String userId, @Param("keepCount") int keepCount);
}
