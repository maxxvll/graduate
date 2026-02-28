package com.maxxvll.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.common.constants.ChatConstants;
import com.maxxvll.common.dto.ChatMessageSendDTO;
import com.maxxvll.common.enums.MessageStatus;
import com.maxxvll.common.enums.MessageType;
import com.maxxvll.common.enums.SessionType;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.common.vo.UserInfoVO;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.mapper.ChatMessageMapper;
import com.maxxvll.service.ChatMessageService;
import com.maxxvll.service.ChatSessionService;
import com.maxxvll.utils.MinioUtil;
import com.maxxvll.utils.UserContextUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
        implements ChatMessageService {

    @Resource
    private MinioUtil minioUtil;
    @Resource
    private ChatSessionService chatSessionService;

    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ==================== 核心方法：发送消息 ====================
    @Override
    public ChatMessage sendMessage(ChatMessageSendDTO sendDTO, MultipartFile[] files) throws Exception {

        UserInfoVO currentUser = UserContextUtil.getCurrentUser();
        String senderId = currentUser.getId();
        boolean isPublicFile = SessionType.isGroup(sendDTO.getSessionType());

        // 2. 预处理文件（事务外执行远程IO）
        List<FileUploadResult> uploadResults = preUploadFiles(files, isPublicFile);

        // 3. 事务内只做数据库操作
        return doSendMessageInTransaction(sendDTO, senderId, isPublicFile, uploadResults);
    }

    /**
     * 文件上传结果封装（Record 类，Java 16+）
     */
    private record FileUploadResult(String newFileName, String originalFilename, String suffix,
                                    long fileSize, MessageType msgType) {}



    /**
     * 预处理文件上传（事务外）
     */
    private List<FileUploadResult> preUploadFiles(MultipartFile[] files, boolean isPublicFile) throws Exception {
        List<FileUploadResult> results = new ArrayList<>();
        if (files == null || files.length == 0) {
            return results;
        }

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFileName = generateFileName(suffix);

            // 事务外上传文件
            minioUtil.uploadChatFile(file, newFileName, isPublicFile);

            MessageType msgType = getMessageTypeByFile(file);
            results.add(new FileUploadResult(newFileName, originalFilename, suffix,
                    file.getSize(), msgType));

            log.info("文件上传成功，originalFilename: {}, newFileName: {}", originalFilename, newFileName);
        }
        return results;
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String suffix) {
        return "chat/" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + "/"
                + IdUtil.simpleUUID() + suffix;
    }

    /**
     * 事务内保存消息
     */
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage doSendMessageInTransaction(ChatMessageSendDTO sendDTO, String senderId,
                                                  boolean isPublicFile, List<FileUploadResult> uploadResults) {
        List<ChatMessage> messageToSave = new ArrayList<>();

        // 1. 处理文本消息
        if (StrUtil.isNotBlank(sendDTO.getContent()) && StrUtil.isBlank(sendDTO.getFileUrl())) {
            ChatMessage textMsg = buildMessage(sendDTO, senderId, MessageType.TEXT, sendDTO.getContent(), null);
            textMsg.setMessageNo(IdUtil.simpleUUID());
            messageToSave.add(textMsg);
        }

        // 2. 处理前端传的 fileUrl（切片上传/秒传）
        if (StrUtil.isNotBlank(sendDTO.getFileUrl())) {
            MessageType msgType = MessageType.getByCode(sendDTO.getMessageType());
            String content = "[" + msgType.getDesc() + "]";

            ChatMessage fileMsg = buildMessage(sendDTO, senderId, msgType, content, sendDTO.getFileUrl());
            fileMsg.setMessageNo(IdUtil.simpleUUID());
            fileMsg.setFileName(sendDTO.getFileName());
            fileMsg.setFileSize(sendDTO.getFileSize());
            if (msgType == MessageType.AUDIO && sendDTO.getDuration() != null) {
                fileMsg.setDuration(sendDTO.getDuration());
            }
            messageToSave.add(fileMsg);
        }

        // 3. 处理刚才上传的文件
        for (FileUploadResult uploadResult : uploadResults) {
            String content = "[" + uploadResult.msgType.getDesc() + "]";
            ChatMessage fileMsg = buildMessage(sendDTO, senderId, uploadResult.msgType,
                    content, uploadResult.newFileName);
            fileMsg.setMessageNo(IdUtil.simpleUUID());
            fileMsg.setFileName(uploadResult.originalFilename);
            fileMsg.setFileType(uploadResult.suffix);
            fileMsg.setFileSize(uploadResult.fileSize);
            messageToSave.add(fileMsg);
        }

        if (messageToSave.isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }

        // 4. 批量保存
        saveBatch(messageToSave);
        log.info("消息保存成功，数量: {}, sessionId: {}", messageToSave.size(), sendDTO.getSessionId());

        // 5. 更新会话
        ChatMessage firstMsg = messageToSave.get(0);
        chatSessionService.updateSessionAfterSend(firstMsg);

        // 6. 生成可访问链接返回
        if (StrUtil.isNotBlank(firstMsg.getFileUrl())) {
            String accessibleUrl = minioUtil.getChatFileUrl(firstMsg.getFileUrl(), isPublicFile);
            firstMsg.setFileUrl(accessibleUrl);
        }

        return firstMsg;
    }

    // ==================== 查询消息列表 ====================
    @Override
    public List<ChatMessage> getMessages(String sessionId) {
        // 参数校验
        if (StrUtil.isBlank(sessionId)) {
            throw new BusinessException("会话ID不能为空");
        }

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getIsDeleted, 0)
                .orderByAsc(ChatMessage::getSendTime);

        List<ChatMessage> messages = list(wrapper);

        // 填充文件可访问链接
        for (ChatMessage msg : messages) {
            if (StrUtil.isNotBlank(msg.getFileUrl()) && !msg.getFileUrl().startsWith("http")) {
                boolean isPublicFile = SessionType.isGroup(msg.getSessionType());
                String accessibleUrl = minioUtil.getChatFileUrl(msg.getFileUrl(), isPublicFile);
                msg.setFileUrl(accessibleUrl);
            }
        }

        log.info("查询消息成功，sessionId: {}, 数量: {}", sessionId, messages.size());
        return messages;
    }

    // ==================== 获取离线消息 ====================
    @Override
    public List<ChatMessage> getOfflineMessages(String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new BusinessException("用户ID不能为空");
        }

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();

        // 条件1：接收人是当前用户（单聊场景）
        wrapper.and(w -> w.eq(ChatMessage::getReceiverId, userId)
                        .eq(ChatMessage::getSessionType, SessionType.SINGLE.getCode()))
                // 条件2：或者是群聊消息（简化处理，实际需结合群成员表）
                .or(w -> w.eq(ChatMessage::getSessionType, SessionType.GROUP.getCode())
                        .eq(ChatMessage::getStatus, MessageStatus.SEND_SUCCESS.getCode()));

        // 条件3：状态为发送成功，且未删除
        wrapper.eq(ChatMessage::getStatus, MessageStatus.SEND_SUCCESS.getCode())
                .eq(ChatMessage::getIsDeleted, 0)
                .orderByAsc(ChatMessage::getSendTime);

        List<ChatMessage> messages = list(wrapper);

        // 填充文件可访问链接
        for (ChatMessage msg : messages) {
            if (StrUtil.isNotBlank(msg.getFileUrl()) && !msg.getFileUrl().startsWith("http")) {
                boolean isPublicFile = SessionType.isGroup(msg.getSessionType());
                String accessibleUrl = minioUtil.getChatFileUrl(msg.getFileUrl(), isPublicFile);
                msg.setFileUrl(accessibleUrl);
            }
        }

        log.info("查询离线消息成功，userId: {}, 数量: {}", userId, messages.size());
        return messages;
    }

    // ==================== 标记离线消息已拉取 ====================
    @Override
    public void markOfflineMessagesAsPulled(String userId) {
        log.info("用户 {} 离线消息已拉取", userId);
        // 实际业务中可通过 Redis 记录用户最后拉取时间
    }

    // ==================== 标记消息已读 ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markMessagesAsRead(String sessionId, String userId) {
        if (StrUtil.isBlank(sessionId) || StrUtil.isBlank(userId)) {
            throw new BusinessException("参数不能为空");
        }

        // 1. 更新消息状态
        LambdaUpdateWrapper<ChatMessage> msgUpdateWrapper = new LambdaUpdateWrapper<>();
        msgUpdateWrapper.eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getReceiverId, userId)
                .eq(ChatMessage::getStatus, MessageStatus.SEND_SUCCESS.getCode())
                .eq(ChatMessage::getIsDeleted, 0)
                .set(ChatMessage::getStatus, MessageStatus.READ.getCode());

        int updateCount = baseMapper.update(null, msgUpdateWrapper);
        log.info("标记消息已读，sessionId: {}, userId: {}, 数量: {}", sessionId, userId, updateCount);

        // 2. 通过 Service 层更新会话未读数
        chatSessionService.clearUnreadCount(sessionId, userId);
    }

    // ==================== 撤回消息 ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeMessage(String messageId) {
        if (StrUtil.isBlank(messageId)) {
            throw new BusinessException("消息ID不能为空");
        }

        UserInfoVO currentUser = UserContextUtil.getCurrentUser();

        // 1. 查询原消息
        ChatMessage originalMsg = getById(messageId);
        if (originalMsg == null) {
            throw new BusinessException("消息不存在");
        }

        // 2. 校验权限
        if (!originalMsg.getSenderId().equals(currentUser.getId())) {
            log.warn("用户 {} 尝试撤回他人的消息 {}", currentUser.getId(), messageId);
            throw new BusinessException("只能撤回自己发送的消息");
        }

        // 3. 校验时间
        long timeDiff = System.currentTimeMillis() - originalMsg.getSendTime().getTime();
        if (timeDiff > ChatConstants.REVOKE_TIME_LIMIT) {
            throw new BusinessException("超过撤回时间限制");
        }

        // 4. 校验状态
        if (!MessageStatus.SEND_SUCCESS.getCode().equals(originalMsg.getStatus())
                && !MessageStatus.READ.getCode().equals(originalMsg.getStatus())) {
            throw new BusinessException("该消息无法撤回");
        }

        // 5. 执行撤回
        LambdaUpdateWrapper<ChatMessage> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatMessage::getId, messageId)
                .set(ChatMessage::getStatus, MessageStatus.REVOKED.getCode())
                .set(ChatMessage::getContentReplaced, "[消息已撤回]")
                .set(ChatMessage::getRevokeTime, new Date())
                .set(ChatMessage::getOperatorId, currentUser.getId());

        update(updateWrapper);
        log.info("消息撤回成功，messageId: {}, operator: {}", messageId, currentUser.getId());

        // 6. 更新会话最后一条消息
        chatSessionService.updateSessionAfterMessageRevoked(originalMsg);
    }

    // ==================== 删除消息（软删除） ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(String messageId) {
        if (StrUtil.isBlank(messageId)) {
            throw new BusinessException("消息ID不能为空");
        }

        UserInfoVO currentUser = UserContextUtil.getCurrentUser();

        // 1. 查询原消息
        ChatMessage originalMsg = getById(messageId);
        if (originalMsg == null) {
            throw new BusinessException("消息不存在");
        }

        // 2. 校验权限
        if (!originalMsg.getSenderId().equals(currentUser.getId())
                && !originalMsg.getReceiverId().equals(currentUser.getId())) {
            throw new BusinessException("无权删除该消息");
        }

        // 3. 执行软删除
        LambdaUpdateWrapper<ChatMessage> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatMessage::getId, messageId)
                .set(ChatMessage::getIsDeleted, 1)
                .set(ChatMessage::getStatus, MessageStatus.DELETED.getCode());

        update(updateWrapper);
        log.info("消息删除成功，messageId: {}, operator: {}", messageId, currentUser.getId());
    }

    // ==================== 标记消息为离线 ====================
    @Override
    public void markMessageAsOffline(Long id) {
        log.info("消息 {} 已标记为离线", id);
        // 实际业务中可扩展 ext_info 字段
    }

    // ==================== 私有方法：构建消息对象 ====================
    private ChatMessage buildMessage(ChatMessageSendDTO sendDTO, String senderId,
                                     MessageType msgType, String content, String fileUrl) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sendDTO.getSessionId());
        message.setSessionType(sendDTO.getSessionType());
        message.setSenderId(senderId);
        message.setReceiverId(sendDTO.getReceiverId());
        message.setMessageType(msgType.getCode());
        message.setContent(content);
        message.setFileUrl(fileUrl);
        message.setSendTime(new Date());
        message.setStatus(MessageStatus.SEND_SUCCESS.getCode());
        message.setIsSensitive(0);
        message.setIsDeleted(0);
        return message;
    }

    // ==================== 私有方法：根据文件类型判断消息类型 ====================
    private MessageType getMessageTypeByFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return MessageType.FILE;
        }
        if (contentType.startsWith("image/")) {
            return MessageType.IMAGE;
        } else if (contentType.startsWith("video/")) {
            return MessageType.VIDEO;
        } else if (contentType.startsWith("audio/")) {
            return MessageType.AUDIO;
        } else {
            return MessageType.FILE;
        }
    }
}