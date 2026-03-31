package com.maxxvll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxxvll.domain.FriendRelationSetting;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FriendRelationSettingMapper extends BaseMapper<FriendRelationSetting> {

    @Insert({
            "<script>",
            "INSERT INTO friend_relation_setting (owner_user_id, friend_user_id, created_at, updated_at) VALUES ",
            "<foreach collection='items' item='item' separator=','>",
            "(#{item.ownerUserId}, #{item.friendUserId}, NOW(), NOW())",
            "</foreach>",
            " ON DUPLICATE KEY UPDATE updated_at = updated_at",
            "</script>"
    })
    int batchInsertIgnore(@Param("items") List<FriendRelationSetting> items);
}
