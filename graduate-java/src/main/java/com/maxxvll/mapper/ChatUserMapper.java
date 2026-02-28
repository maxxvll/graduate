package com.maxxvll.mapper;

import com.maxxvll.domain.ChatUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
* @author 20570
* @description 针对表【chat_user(用户基础信息表)】的数据库操作Mapper
* @createDate 2026-02-19 12:02:21
* @Entity com.maxxvll.domain.ChatUser
*/
public interface ChatUserMapper extends BaseMapper<ChatUser> {
    public ChatUser selectByUsername(@Param("username") String username);
}




