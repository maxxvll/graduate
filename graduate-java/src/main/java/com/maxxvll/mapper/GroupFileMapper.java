package com.maxxvll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxxvll.domain.GroupFile;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface GroupFileMapper extends BaseMapper<GroupFile> {

    @Select("SELECT * FROM group_file WHERE group_id = #{groupId} ORDER BY created_at DESC")
    List<GroupFile> selectByGroupId(@Param("groupId") String groupId);
}
