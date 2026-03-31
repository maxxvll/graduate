package com.maxxvll.service.message.template;

import com.maxxvll.common.dto.ChatMessageSendDTO;
import com.maxxvll.common.event.MessageEventPublisher;
import com.maxxvll.common.event.SessionUpdateEvent;
import com.maxxvll.common.enums.MessageType;
import com.maxxvll.common.vo.UserInfoVO;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.service.ChatGroupMemberService;
import com.maxxvll.service.ChatMessageService;
import com.maxxvll.service.message.handler.MessageTypeHandler;
import com.maxxvll.service.message.handler.MessageTypeHandlerRegistry;
import com.maxxvll.utils.UserContextUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 消息发送模板类
 * 定义消息发送的标准化流程：校验 → 构建 → 持久化 → 事件发布
 *
 * @author maxxvll
 * @since 2026-03-31
 */
@Slf4j
@Component
public class MessageSendTemplate {

    @Resource
    private MessageEventPublisher messageEventPublisher;

    @Resource
    private MessageTypeHandlerRegistry handlerRegistry;

    @Resource
    private ChatGroupMemberService chatGroupMemberService;

    /**
     * 执行消息发送模板方法
     *
     * @param context 消息发送上下文
     * @return 发送的消息
     */
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage execute(MessageSendContext context) {
        // 1. 校验阶段
        validate(context);

        // 2. 构建阶段
        ChatMessage message = build(context);

        // 3. 持久化阶段
        persist(context, message);

        // 4. 事件发布阶段
        publishEvents(message);

        return message;
    }

    /**
     * 校验阶段
     */
    private void validate(MessageSendContext context) {
        if (context.getSendDTO() == null) {
            throw new IllegalArgumentException("消息发送DTO不能为空");
        }

        ChatMessageSendDTO sendDTO = context.getSendDTO();

        if (sendDTO.getSessionId() == null || sendDTO.getSessionId().trim().isEmpty()) {
            throw new IllegalArgumentException("会话ID不能为空");
        }

        if (sendDTO.getSessionType() == null) {
            throw new IllegalArgumentException("会话类型不能为空");
        }

        if (sendDTO.getMessageType() == null) {
            throw new IllegalArgumentException("消息类型不能为空");
        }

        // 如果内容为空且没有文件，则不允许发送
        if ((sendDTO.getContent() == null || sendDTO.getContent().trim().isEmpty())
                && (sendDTO.getFileUrl() == null || sendDTO.getFileUrl().trim().isEmpty())) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
    }

    /**
     * 构建阶段
     */
    private ChatMessage build(MessageSendContext context) {
        ChatMessageSendDTO sendDTO = context.getSendDTO();
        String senderId = context.getSenderId();

        ChatMessage message = new ChatMessage();
        message.setSessionId(sendDTO.getSessionId());
        message.setSessionType(sendDTO.getSessionType());
        message.setSenderId(senderId);
        message.setMessageType(sendDTO.getMessageType());
        message.setSendTime(new Date());

        // 使用策略模式处理不同类型的消息
        MessageType messageType = MessageType.getByCode(sendDTO.getMessageType());
        MessageTypeHandler handler = handlerRegistry.getHandler(messageType);

        if (handler != null) {
            handler.handleBeforeSend(message);
        }

        // 设置消息内容
        if (sendDTO.getContent() != null && !sendDTO.getContent().trim().isEmpty()) {
            message.setContent(sendDTO.getContent());
        } else if (handler != null) {
            message.setContent(handler.getDefaultContent());
        }

        // 设置文件相关信息
        if (sendDTO.getFileUrl() != null) {
            message.setFileUrl(sendDTO.getFileUrl());
        }
        if (sendDTO.getFileName() != null) {
            message.setFileName(sendDTO.getFileName());
        }
        if (sendDTO.getFileSize() != null) {
            message.setFileSize(sendDTO.getFileSize());
        }
        if (sendDTO.getDuration() != null) {
            message.setDuration(sendDTO.getDuration());
        }

        // 设置引用消息
        if (sendDTO.getQuoteMessageId() != null) {
            message.setQuoteMessageId(sendDTO.getQuoteMessageId());
        }

        // 生成消息号
        message.setMessageNo(generateMessageNo());

        // 设置状态
        message.setStatus(1); // 发送成功

        return message;
    }

    /**
     * 持久化阶段（由子类实现具体逻辑）
     */
    private void persist(MessageSendContext context, ChatMessage message) {
        // 由调用方的 ChatMessageService 完成
        context.getMessageService().save(message);
        log.debug("Message persisted: {}", message.getId());
    }

    /**
     * 事件发布阶段
     */
    private void publishEvents(ChatMessage message) {
        // 发布消息发送事件，触发会话更新
        messageEventPublisher.publishMessageSentEvent(message);
        log.debug("Message event published: {}", message.getId());
    }

    /**
     * 生成消息号
     */
    private String generateMessageNo() {
        return System.currentTimeMillis() + "_" + cn.hutool.core.util.IdUtil.fastSimpleUUID();
    }
}
