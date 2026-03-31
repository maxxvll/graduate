package com.maxxvll.service;

import com.maxxvll.common.vo.ChatMessageVO;
import com.maxxvll.domain.ChatOfflineCursor;

import java.util.List;

/**
 * 离线游标服务接口
 * 用于管理用户离线消息同步的游标位置
 *
 * @author maxxvll
 * @since 2026-03-31
 */
public interface ChatOfflineCursorService {

    /**
     * 获取用户当前离线游标
     *
     * @param userId 用户ID
     * @return 离线游标对象，如果不存在则返回null
     */
    ChatOfflineCursor getCursor(String userId);

    /**
     * 更新用户离线游标
     *
     * @param userId 用户ID
     * @param lastMessageId 最后一条同步消息的ID
     * @param lastMessageTime 最后一条同步消息的时间
     */
    void updateCursor(String userId, Long lastMessageId, java.util.Date lastMessageTime);

    /**
     * 重置用户离线游标（清除所有离线消息）
     *
     * @param userId 用户ID
     */
    void resetCursor(String userId);

    /**
     * 获取增量离线消息（基于游标的增量同步）
     *
     * @param userId 用户ID
     * @param limit 最大返回消息数量
     * @return 增量消息列表
     */
    List<ChatMessageVO> getIncrementalMessages(String userId, int limit);

    /**
     * 获取离线消息（完整同步）
     *
     * @param userId 用户ID
     * @param afterTimestamp 可选，起始时间戳（毫秒）
     * @return 离线消息列表
     */
    List<ChatMessageVO> getOfflineMessages(String userId, Long afterTimestamp);

    /**
     * 标记消息已拉取并更新游标
     *
     * @param userId 用户ID
     * @param messageIds 已拉取的消息ID列表
     */
    void markMessagesAsPulled(String userId, List<Long> messageIds);
}
