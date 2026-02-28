package com.maxxvll.controller;

import com.alibaba.fastjson2.JSON;
import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.dto.ChatMessageSendDTO;
import com.maxxvll.common.event.ChatMessageEvent;
import com.maxxvll.common.vo.ChatMessageVO;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.service.ChatMessageService;
import com.maxxvll.utils.BeanConvertUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatController extends BaseController {
    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private KafkaTemplate<String, Object> kafkaTemplate;

    // Kafka 主题名称（建议在配置文件中配置）
    private static final String CHAT_MESSAGE_TOPIC = "chat-message-topic";

    /**
     * 发送消息
     * 流程：1. 保存消息到数据库 2. 发送到 Kafka 异步处理（推送/离线存储）
     */
    @PostMapping("/message/send")
    public Result<ChatMessageVO> sendMessage(
            @RequestPart("sendDTO") String sendDTO,
            @RequestPart(value = "files", required = false) MultipartFile[] files) throws Exception {
        ChatMessageSendDTO chatMessageSendDTO = JSON.parseObject(sendDTO, ChatMessageSendDTO.class);

        // 1. 保存消息到数据库（包含文件上传处理）
        ChatMessage message = chatMessageService.sendMessage(chatMessageSendDTO, files);

        // 2. 转换为 VO 返回给前端
        ChatMessageVO messageVO = BeanConvertUtil.convert(message, ChatMessageVO.class);

        // 3. 构建消息事件，发送到 Kafka
        ChatMessageEvent event = ChatMessageEvent.builder()
                .message(message)
                .isOffline(false) // 初始假设在线，由消费者判断是否离线
                .receiverId(message.getReceiverId())
                .build();

        kafkaTemplate.send(CHAT_MESSAGE_TOPIC, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("发送消息到 Kafka 失败，messageId: {}", message.getId(), ex);
                    } else {
                        log.info("发送消息到 Kafka 成功，messageId: {}", message.getId());
                    }
                });

        return Result.success("发送成功", messageVO);
    }

    /**
     * 获取指定会话的历史消息
     */
    @GetMapping("/message/list")
    public Result<List<ChatMessageVO>> getMessageList(@RequestParam String sessionId) {
        List<ChatMessage> messages = chatMessageService.getMessages(sessionId);
        // 转换为 VO 列表
        List<ChatMessageVO> messageVOs = BeanConvertUtil.convertList(messages, ChatMessageVO.class);
        return Result.success("获取成功", messageVOs);
    }

    /**
     * 获取用户的离线消息（登录时调用）
     */
    @GetMapping("/message/offline")
    public Result<List<ChatMessageVO>> getOfflineMessages(@RequestParam String userId) {
        // 从 Service 获取离线消息（需在 ChatMessageService 中实现）
        List<ChatMessage> offlineMessages = chatMessageService.getOfflineMessages(userId);
        List<ChatMessageVO> messageVOs = BeanConvertUtil.convertList(offlineMessages, ChatMessageVO.class);

        // 可选：拉取后标记为已拉取（避免重复拉取）
        chatMessageService.markOfflineMessagesAsPulled(userId);

        return Result.success("获取离线消息成功", messageVOs);
    }

    /**
     * 标记会话消息为已读
     */
    @PutMapping("/message/read")
    public Result<Boolean> markMessagesAsRead(
            @RequestParam String sessionId,
            @RequestParam String userId) {
        // 标记该会话中发给当前用户的未读消息为已读
        chatMessageService.markMessagesAsRead(sessionId, userId);
        return Result.success("标记已读成功", true);
    }

    /**
     * 撤回消息
     */
    @PutMapping("/message/revoke")
    public Result<Boolean> revokeMessage(@RequestParam String messageId) {
        chatMessageService.revokeMessage(messageId);
        return Result.success("撤回成功", true);
    }

    /**
     * 删除消息（软删除）
     */
    @DeleteMapping("/message/{messageId}")
    public Result<Boolean> deleteMessage(@PathVariable String messageId) {
        chatMessageService.deleteMessage(messageId);
        return Result.success("删除成功", true);
    }
}