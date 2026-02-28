package com.maxxvll.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.domain.ChatGroup;
import com.maxxvll.service.ChatGroupService;
import com.maxxvll.mapper.ChatGroupMapper;
import org.springframework.stereotype.Service;

/**
* @author 20570
* @description 针对表【chat_group(群聊基础信息表)】的数据库操作Service实现
* @createDate 2026-02-19 12:02:21
*/
@Service
public class ChatGroupServiceImpl extends ServiceImpl<ChatGroupMapper, ChatGroup>
    implements ChatGroupService{

}




