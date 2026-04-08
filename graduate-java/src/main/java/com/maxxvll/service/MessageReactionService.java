package com.maxxvll.service;

import com.maxxvll.common.vo.MessageReactionVO;
import com.maxxvll.domain.MessageReaction;

import java.util.List;
import java.util.Map;

/**
 * 消息反应服务接口
 * @author 20570
 */
public interface MessageReactionService {

    /**
     * 添加表情反应
     */
    MessageReaction addReaction(Long messageId, Long userId, String emoji);

    /**
     * 移除表情反应
     */
    void removeReaction(Long messageId, Long userId, String emoji);

    /**
     * 获取消息的反应列表
     */
    List<MessageReactionVO> getReactionsByMessageId(Long messageId);

    /**
     * 批量获取消息的反应（用于消息列表加载）
     */
    Map<Long, List<MessageReactionVO>> getReactionsByMessageIds(List<Long> messageIds);
}
