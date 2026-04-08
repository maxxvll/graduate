package com.maxxvll.controller;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.dto.ChatMessageSendDTO;
import com.maxxvll.common.dto.MessageForwardDTO;
import com.maxxvll.common.event.ChatMessageEvent;
import com.maxxvll.common.vo.ChatMessageVO;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.service.ChatMessageService;
import com.maxxvll.utils.BeanConvertUtil;
import com.maxxvll.utils.UserContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 聊天消息控制器
 * 提供聊天消息发送、查询、撤回等功能的REST接口
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@Validated
@Tag(name = "聊天", description = "聊天消息相关接口")
public class ChatController extends BaseController {

    private static final String CHAT_MESSAGE_TOPIC = "chat-message-topic";

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 发送消息（multipart/form-data格式）
     */
    @PostMapping("/message/send")
    @Operation(summary = "发送消息", description = "发送文本或多媒体消息，支持multipart/form-data格式")
    public Result<ChatMessageVO> sendMessage(
            @RequestPart("sendDTO") String sendDTO,
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        ChatMessageSendDTO chatMessageSendDTO = JSON.parseObject(sendDTO, ChatMessageSendDTO.class);
        return sendMessageInternal(chatMessageSendDTO, files);
    }

    /**
     * 发送消息（JSON格式）
     */
    @PostMapping(value = "/message/send", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "发送消息(JSON)", description = "发送文本消息，请求体为JSON格式")
    public Result<ChatMessageVO> sendMessageJson(@Valid @RequestBody ChatMessageSendDTO chatMessageSendDTO) {
        return sendMessageInternal(chatMessageSendDTO, null);
    }

    /**
     * 内部发送消息方法
     */
    private Result<ChatMessageVO> sendMessageInternal(ChatMessageSendDTO chatMessageSendDTO, MultipartFile[] files) {
        long startTime = System.currentTimeMillis();

        // 手动验证 DTO（@Valid 对 @RequestPart String 参数不生效）
        validateChatMessageSendDTO(chatMessageSendDTO);

        // 保存消息到数据库
        ChatMessage message = chatMessageService.sendMessage(chatMessageSendDTO, files);

        // 转换为 VO 返回给前端
        ChatMessageVO messageVO = BeanConvertUtil.convert(message, ChatMessageVO.class);

        // 构建消息事件，发送到 Kafka
        ChatMessageEvent event = ChatMessageEvent.builder()
                .message(message)
                .isOffline(false)
                .receiverId(message.getReceiverId())
                .build();

        kafkaTemplate.send(CHAT_MESSAGE_TOPIC, event)
                .whenComplete((result, ex) -> {
                    long costTime = System.currentTimeMillis() - startTime;
                    if (ex != null) {
                        log.error("[Kafka发送] 消息推送失败, messageId={}, sessionId={}, error={}",
                            message.getId(), message.getSessionId(), ex.getMessage(), ex);
                    } else {
                        log.info("[Kafka发送] 消息推送成功, messageId={}, sessionId={}, receiverId={}",
                            message.getId(), message.getSessionId(), message.getReceiverId());
                    }
                });

        return success("发送成功", messageVO);
    }

    /**
     * 获取指定会话的历史消息（分页或增量）
     * 当传入 afterTime（毫秒时间戳）时，返回该时间之后的所有新消息（增量同步）；
     * 否则按 current/size 分页返回。
     */
    @GetMapping("/message/list")
    @Operation(summary = "获取消息列表", description = "获取指定会话的历史消息，支持分页或增量同步（传 afterTime）")
    public Result<Page<ChatMessageVO>> getMessageList(
            @Parameter(description = "会话ID") @RequestParam @NotBlank String sessionId,
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") @PositiveOrZero int current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "50") @Positive @Max(100) int size,
            @Parameter(description = "增量同步起始时间戳（毫秒），有值时忽略分页参数") @RequestParam(required = false) @PositiveOrZero Long afterTime) {
        Page<ChatMessage> page = afterTime != null
                ? chatMessageService.getMessages(sessionId, afterTime)
                : chatMessageService.getMessages(sessionId, current, size);
        Page<ChatMessageVO> voPage = BeanConvertUtil.convertPage(page, ChatMessageVO.class);
        return success("获取成功", voPage);
    }

    /**
     * 获取指定会话的历史消息（已废弃，请使用分页接口）
     */
    @Deprecated
    @GetMapping("/message/list/all")
    @Operation(summary = "获取全部消息(已废弃)", description = "请使用分页接口 /v1/chat/message/list")
    public Result<List<ChatMessageVO>> getMessageListAll(@Parameter(description = "会话ID") @RequestParam @NotBlank String sessionId) {
        List<ChatMessage> messages = chatMessageService.getMessages(sessionId);
        List<ChatMessageVO> messageVOs = BeanConvertUtil.convertList(messages, ChatMessageVO.class);
        return success("获取成功", messageVOs);
    }

    /**
     * 获取用户的离线消息
     */
    @GetMapping("/message/offline/{userId}")
    @Operation(summary = "获取离线消息", description = "获取指定用户的离线消息列表")
    public Result<List<ChatMessageVO>> getOfflineMessages(
            @Parameter(description = "用户ID") @PathVariable @NotBlank String userId,
            @Parameter(description = "获取此时间戳之后的离线消息") @RequestParam(required = false) Long afterTimestamp) {
        List<ChatMessage> offlineMessages = (afterTimestamp != null && afterTimestamp > 0)
                ? chatMessageService.getOfflineMessages(userId, afterTimestamp)
                : chatMessageService.getOfflineMessages(userId);

        List<ChatMessageVO> messageVOs = BeanConvertUtil.convertList(offlineMessages, ChatMessageVO.class);
        chatMessageService.markOfflineMessagesAsPulled(userId, offlineMessages);

        return success("获取离线消息成功", messageVOs);
    }

    /**
     * 标记会话消息为已读
     */
    @PutMapping("/message/read")
    @Operation(summary = "标记消息已读", description = "将指定会话的消息标记为已读状态")
    public Result<Boolean> markMessagesAsRead(
            @Parameter(description = "会话ID") @RequestParam @NotBlank String sessionId,
            @Parameter(description = "用户ID") @RequestParam @NotBlank String userId) {
        chatMessageService.markMessagesAsRead(sessionId, userId);
        return success("标记已读成功", true);
    }

    /**
     * 撤回消息
     */
    @PutMapping("/message/revoke")
    @Operation(summary = "撤回消息", description = "撤回指定的消息，仅能撤回发送者自己的消息")
    public Result<Boolean> revokeMessage(@Parameter(description = "消息ID") @RequestParam @NotBlank String messageId) {
        chatMessageService.revokeMessage(messageId);
        return success("撤回成功", true);
    }

    /**
     * 删除消息（软删除）
     */
    @DeleteMapping("/message/{messageId}")
    @Operation(summary = "删除消息", description = "软删除指定的消息")
    public Result<Boolean> deleteMessage(@Parameter(description = "消息ID") @PathVariable String messageId) {
        chatMessageService.deleteMessage(messageId);
        return success("删除成功", true);
    }

    /**
     * 编辑消息
     */
    @PutMapping("/message/edit")
    @Operation(summary = "编辑消息", description = "编辑已发送的消息内容")
    public Result<ChatMessageVO> editMessage(
            @Parameter(description = "消息ID") @RequestParam @NotBlank Long messageId,
            @Parameter(description = "新内容") @RequestParam @NotBlank String newContent) {
        ChatMessageVO result = chatMessageService.editMessage(messageId, newContent);
        return success("编辑成功", result);
    }

    /**
     * 转发消息
     */
    @PostMapping("/message/forward")
    @Operation(summary = "转发消息", description = "将消息转发到指定会话")
    public Result<List<ChatMessageVO>> forwardMessage(@Valid @RequestBody MessageForwardDTO dto) {
        List<ChatMessageVO> messages = chatMessageService.forwardMessages(
                dto.getMessageIds(), dto.getTargetSessionId(), getCurrentUserId());
        return success("转发成功", messages);
    }

    /**
     * 搜索聊天记录
     */
    @GetMapping("/search")
    @Operation(summary = "搜索消息", description = "根据关键词搜索聊天记录")
    public Result<Page<ChatMessageVO>> searchMessages(
            @Parameter(description = "搜索关键词") @RequestParam @NotBlank String keyword,
            @Parameter(description = "会话ID（可选）") @RequestParam(required = false) String sessionId,
            @Parameter(description = "消息类型（可选）") @RequestParam(required = false) String messageType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        page = normalizePageNum(page);
        size = normalizePageSize(size);

        Page<ChatMessage> messagePage = chatMessageService.searchMessages(
                getCurrentUserId(), keyword, sessionId, messageType, page, size);
        Page<ChatMessageVO> voPage = BeanConvertUtil.convertPage(messagePage, ChatMessageVO.class);
        return success("搜索成功", voPage);
    }

    /**
     * 手动验证 ChatMessageSendDTO
     * 由于 @Valid 注解对 @RequestPart String 参数不生效
     */
    private void validateChatMessageSendDTO(ChatMessageSendDTO dto) {
        if (dto.getMessageNo() == null || dto.getMessageNo().trim().isEmpty()) {
            throw new IllegalArgumentException("消息编号不能为空");
        }
        if (dto.getSessionId() == null || dto.getSessionId().trim().isEmpty()) {
            throw new IllegalArgumentException("会话ID不能为空");
        }
        if (dto.getSessionType() == null || dto.getSessionType() < 1 || dto.getSessionType() > 2) {
            throw new IllegalArgumentException("会话类型必须为1或2");
        }
        if (dto.getContent() != null && dto.getContent().length() > 5000) {
            throw new IllegalArgumentException("消息内容不能超过5000个字符");
        }
        if (dto.getDuration() != null && (dto.getDuration() < 0 || dto.getDuration() > 300)) {
            throw new IllegalArgumentException("语音时长必须在0-300秒之间");
        }
        if (dto.getMessageType() != null && (dto.getMessageType() < 1 || dto.getMessageType() > 5)) {
            throw new IllegalArgumentException("消息类型必须为1-5之间的整数");
        }
        if (dto.getFileName() != null && dto.getFileName().length() > 255) {
            throw new IllegalArgumentException("文件名不能超过255个字符");
        }
        if (dto.getFileSize() != null && (dto.getFileSize() < 0 || dto.getFileSize() > 1024L * 1024 * 1024 * 2)) {
            throw new IllegalArgumentException("文件大小必须在0-2GB之间");
        }
    }
}
