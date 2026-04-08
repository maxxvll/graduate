package com.maxxvll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxxvll.domain.FriendGroup;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FriendGroupMapper extends BaseMapper<FriendGroup> {

    /**
     * 获取用户的好友分组列表，按排序字段排序
     */
    @Select("SELECT * FROM friend_group WHERE owner_user_id = #{ownerUserId} ORDER BY group_order ASC, created_at ASC")
    List<FriendGroup> selectByOwnerUserIdOrderByOrder(@Param("ownerUserId") Long ownerUserId);

    /**
     * 获取用户的默认分组
     */
    @Select("SELECT * FROM friend_group WHERE owner_user_id = #{ownerUserId} AND is_default = 1 LIMIT 1")
    FriendGroup selectDefaultGroup(@Param("ownerUserId") Long ownerUserId);

    /**
     * 统计用户的好友分组数量
     */
    @Select("SELECT COUNT(*) FROM friend_group WHERE owner_user_id = #{ownerUserId}")
    int countByOwnerUserId(@Param("ownerUserId") Long ownerUserId);

    /**
     * 检查分组名称是否已存在
     */
    @Select("SELECT COUNT(*) FROM friend_group WHERE owner_user_id = #{ownerUserId} AND group_name = #{groupName}")
    int countByOwnerAndName(@Param("ownerUserId") Long ownerUserId, @Param("groupName") String groupName);
}
