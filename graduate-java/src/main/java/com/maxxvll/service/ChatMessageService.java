package com.maxxvll.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxxvll.common.dto.ChatMessageSendDTO;
import com.maxxvll.common.vo.ChatMessageVO;
import com.maxxvll.domain.ChatMessage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
* @author 20570
* @description 针对表【chat_message(聊天消息记录表（精简版）)】的数据库操作Service
* @createDate 2026-02-19 12:02:21
*/
public interface ChatMessageService extends IService<ChatMessage> {
    ChatMessage sendMessage(ChatMessageSendDTO sendDTO, MultipartFile[] files);

    /**
     * 获取指定会话的消息列表（分页）
     * 优化：使用分页查询，避免返回过多数据
     *
     * @param sessionId 会话ID
     * @param current   当前页码
     * @param size      每页大小
     * @return 分页消息对象
     */
    Page<ChatMessage> getMessages(String sessionId, int current, int size);

    /**
     * 获取指定会话的所有消息（已废弃，请使用分页方法）
     * @deprecated 使用 getMessages(sessionId, current, size) 替代
     */
    @Deprecated
    List<ChatMessage> getMessages(String sessionId);

    // 新增：获取用户的离线消息
    List<ChatMessage> getOfflineMessages(String userId);

    /**
     * 获取用户的离线消息（支持时间范围过滤）
     * @param userId 用户ID
     * @param afterTimestamp 起始时间戳（毫秒），null 表示不限制
     * @return 离线消息列表
     */
    List<ChatMessage> getOfflineMessages(String userId, Long afterTimestamp);

    // 新增：标记离线消息为已拉取
    void markOfflineMessagesAsPulled(String userId, List<ChatMessage> messages);

    // 新增：标记会话消息为已读
    void markMessagesAsRead(String sessionId, String userId);

    // 新增：撤回消息
    void revokeMessage(String messageId);

    // 新增：编辑消息
    ChatMessageVO editMessage(Long messageId, String newContent);

    // 新增：删除消息（软删除）
    void deleteMessage(String messageId);

    // 新增：转发消息
    List<ChatMessageVO> forwardMessages(List<Long> messageIds, String targetSessionId, String senderId);

    void markMessageAsOffline(Long id);

    int purgeExpiredMessages();

    /**
     * 系统级直接保存消息（不依赖 UserContext，不处理文件，适用于后台自动发送系统通知等场景）
     */
    ChatMessage saveDirectly(String sessionId, String senderId, String receiverId,
                             int sessionType, int messageType, String content);

    /**
     * 搜索聊天记录
     * @param userId 用户ID
     * @param keyword 搜索关键词
     * @param sessionId 会话ID（可选）
     * @param messageType 消息类型筛选（可选）
     * @param current 页码
     * @param size 每页数量
     * @return 分页结果
     */
    Page<ChatMessage> searchMessages(String userId, String keyword, String sessionId, String messageType, int current, int size);
}
