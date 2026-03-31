package com.maxxvll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxxvll.domain.SearchConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 搜索配置Mapper
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Mapper
public interface SearchConfigMapper extends BaseMapper<SearchConfig> {

    /**
     * 获取配置值
     */
    @Select("SELECT config_value FROM search_config WHERE config_key = #{configKey}")
    String getConfigValue(@Param("configKey") String configKey);

    /**
     * 获取配置值（带默认值）
     */
    default String getConfigValueWithDefault(String configKey, String defaultValue) {
        String value = getConfigValue(configKey);
        return value != null ? value : defaultValue;
    }
}
