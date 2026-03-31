package com.maxxvll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxxvll.domain.CloudShare;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CloudShareMapper extends BaseMapper<CloudShare> {

    @Select("SELECT * FROM cloud_share WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<CloudShare> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM cloud_share WHERE share_code = #{shareCode} AND (expire_time IS NULL OR expire_time > NOW())")
    CloudShare selectByShareCode(@Param("shareCode") String shareCode);
}
