package com.maxxvll.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.domain.ChatGroupMember;
import com.maxxvll.service.ChatGroupMemberService;
import com.maxxvll.mapper.ChatGroupMemberMapper;
import org.springframework.stereotype.Service;

/**
* @author 20570
* @description 针对表【chat_group_member(群成员关联表（核心多对多）)】的数据库操作Service实现
* @createDate 2026-02-19 12:02:21
*/
@Service
public class ChatGroupMemberServiceImpl extends ServiceImpl<ChatGroupMemberMapper, ChatGroupMember>
    implements ChatGroupMemberService{

}




