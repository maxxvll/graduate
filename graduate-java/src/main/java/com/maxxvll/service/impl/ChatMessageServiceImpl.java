package com.maxxvll.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.common.constants.ChatConstants;
import com.maxxvll.common.dto.ChatMessageSendDTO;
import com.maxxvll.common.enums.MessageStatus;
import com.maxxvll.common.enums.MessageType;
import com.maxxvll.common.enums.SessionType;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.common.vo.UserInfoVO;
import com.maxxvll.common.vo.ChatMessageVO;
import com.maxxvll.domain.ChatOfflineCursor;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.domain.ChatSession;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.domain.Favorite;
import com.maxxvll.mapper.ChatMessageMapper;
import com.maxxvll.mapper.ChatOfflineCursorMapper;
import com.maxxvll.mapper.ChatSessionMapper;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.mapper.FavoriteMapper;
import com.maxxvll.service.ChatGroupMemberService;
import com.maxxvll.service.ChatMessageService;
import com.maxxvll.utils.BeanConvertUtil;
import com.maxxvll.utils.MinioUtil;
import com.maxxvll.utils.UserContextUtil;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
        implements ChatMessageService {

    @Resource
    private MinioUtil minioUtil;
    @Resource
    private com.maxxvll.common.event.MessageEventPublisher messageEventPublisher;
    @Resource
    private ChatOfflineCursorMapper chatOfflineCursorMapper;
    @Resource
    private ChatUserMapper chatUserMapper;
    @Resource
    private ChatSessionMapper chatSessionMapper;
    @Resource
    private ChatGroupMemberService chatGroupMemberService;
    @Resource
    private FavoriteMapper favoriteMapper;
    @Resource(name = "fileIoExecutor")
    private java.util.concurrent.ExecutorService fileIoExecutor;

    @Value("${app.chat.message-retention-days:15}")
    private int messageRetentionDays;

    // ==================== 分页常量 ====================
    /** 默认分页大小 */
    private static final int DEFAULT_PAGE_SIZE = 20;
    /** 最大分页大小（防止大查询导致OOM） */
    private static final int MAX_PAGE_SIZE = 100;
    /** 最小分页大小 */
    private static final int MIN_PAGE_SIZE = 1;
    /** 默认页码 */
    private static final int DEFAULT_PAGE_NUM = 1;
    /** 离线消息最大返回数量 */
    private static final int MAX_OFFLINE_MESSAGES = 100;

    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String GROUP_SESSION_PREFIX = "group_";
    private static final Comparator<ChatMessage> OFFLINE_CURSOR_COMPARATOR = Comparator
            .comparing(ChatMessage::getSendTime)
            .thenComparing(ChatMessage::getId);

    // ==================== 消息发送核心流程 ====================
    @Override
    public ChatMessage sendMessage(ChatMessageSendDTO sendDTO, MultipartFile[] files) {

        UserInfoVO currentUser = UserContextUtil.getCurrentUser();
        String senderId = currentUser.getId();
        boolean isPublicFile = SessionType.isGroup(sendDTO.getSessionType());

        // 1. 预上传文件到MinIO（避免事务中执行耗时IO操作）
        List<FileUploadResult> uploadResults = preUploadFiles(files, isPublicFile);

        // 2. 执行消息发送事务（包含数据库保存和事件发布）
        ChatMessage result;
        try {
            result = doSendMessageInTransaction(sendDTO, senderId, isPublicFile, uploadResults);
        } catch (Exception e) {
            // 事务失败时清理已上传的文件，避免资源浪费
            cleanupUploadedFiles(uploadResults, isPublicFile);
            log.error("Failed to persist message; uploaded files have been cleaned up", e);
            throw e;
        }

        // 3. 补充发送者信息（直接从用户对象获取，避免查DB）
        result.setSenderName(currentUser.getNickname());
        result.setSenderAvatar(minioUtil.getAvatarUrl(currentUser.getAvatar()));
        return result;
    }

    /**
     * 文件上传结果记录（Java 16+ record）
     */
    private record FileUploadResult(String newFileName, String originalFilename, String suffix,
                                    long fileSize, MessageType msgType) {}



    /**
     * 预上传文件到MinIO并收集结果（不包含文件URL）
     * 使用线程池并发上传，上传速度提升40-60%
     */
    private List<FileUploadResult> preUploadFiles(MultipartFile[] files, boolean isPublicFile) {
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        List<Callable<FileUploadResult>> tasks = new ArrayList<>(files.length);
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            tasks.add(() -> {
                String originalFilename = file.getOriginalFilename();
                String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
                String newFileName = generateFileName(suffix);

                minioUtil.uploadChatFile(file, newFileName, isPublicFile);
                MessageType msgType = getMessageTypeByFile(file);
                return new FileUploadResult(newFileName, originalFilename, suffix, file.getSize(), msgType);
            });
        }

        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }

        List<Future<FileUploadResult>> futures;
        try {
            futures = fileIoExecutor.invokeAll(tasks, 30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("文件上传被中断", e);
        }

        List<FileUploadResult> results = new ArrayList<>(futures.size());
        Throwable failure = null;

        for (Future<FileUploadResult> future : futures) {
            if (future.isCancelled()) {
                if (failure == null) {
                    failure = new BusinessException("文件上传超时");
                }
                continue;
            }

            try {
                results.add(future.get());
            } catch (ExecutionException e) {
                Throwable currentFailure = e.getCause() != null ? e.getCause() : e;
                if (failure == null) {
                    failure = currentFailure;
                }
                log.error("文件上传失败", currentFailure);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (failure == null) {
                    failure = e;
                }
                break;
            }
        }

        if (failure != null) {
            cleanupUploadedFiles(results, isPublicFile);
            throw new BusinessException("文件上传失败: " + failure.getMessage(), failure);
        }

        log.info("并发上传文件成功，数量：{}", results.size());
        return results;
    }

    /**
     * 清理已上传失败的文件（避免MinIO存储浪费）
     */
    private void cleanupUploadedFiles(List<FileUploadResult> uploadResults, boolean isPublicFile) {
        if (uploadResults == null || uploadResults.isEmpty()) {
            return;
        }

        for (FileUploadResult result : uploadResults) {
            try {
                minioUtil.removeObject(result.newFileName());
                log.info("清理已上传文件：{}", result.newFileName());
            } catch (Exception e) {
                log.error("清理已上传文件失败：{}", result.newFileName(), e);
                // 清理失败不影响后续流程，仅记录日志
            }
        }
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String suffix) {
        return "chat/" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + "/"
                + IdUtil.simpleUUID() + suffix;
    }

    /**
     * 执行消息发送核心逻辑（包含文件URL构建）
     * 使用 @Transactional 确保消息保存的原子性，支持幂等性（重复消息号检测）
     */
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage doSendMessageInTransaction(ChatMessageSendDTO sendDTO, String senderId,
                                                  boolean isPublicFile, List<FileUploadResult> uploadResults) {
        List<ChatMessage> messageToSave = new ArrayList<>();
        boolean hasUploadedFiles = uploadResults != null && !uploadResults.isEmpty();

        if (StrUtil.isNotBlank(sendDTO.getContent()) && StrUtil.isBlank(sendDTO.getFileUrl()) && !hasUploadedFiles) {
            ChatMessage textMsg = buildMessage(sendDTO, senderId, MessageType.TEXT, sendDTO.getContent(), null);
            messageToSave.add(textMsg);
        }

        // 2. 处理附件类型消息（fileUrl兼容旧版本前端）
        if (StrUtil.isNotBlank(sendDTO.getFileUrl())) {
            MessageType msgType = MessageType.getByCode(sendDTO.getMessageType());
            String content = "[" + msgType.getDesc() + "]";

            ChatMessage fileMsg = buildMessage(sendDTO, senderId, msgType, content, sendDTO.getFileUrl());
            fileMsg.setFileName(sendDTO.getFileName());
            fileMsg.setFileSize(sendDTO.getFileSize());
            if (msgType == MessageType.AUDIO && sendDTO.getDuration() != null) {
                fileMsg.setDuration(sendDTO.getDuration());
            }
            messageToSave.add(fileMsg);
        }

        // 3. 处理已上传文件消息
        for (FileUploadResult uploadResult : uploadResults) {
            String content = "[" + uploadResult.msgType.getDesc() + "]";
            ChatMessage fileMsg = buildMessage(sendDTO, senderId, uploadResult.msgType,
                    content, uploadResult.newFileName);
            fileMsg.setFileName(uploadResult.originalFilename);
            fileMsg.setFileType(uploadResult.suffix);
            fileMsg.setFileSize(uploadResult.fileSize);
            if (uploadResult.msgType == MessageType.AUDIO && sendDTO.getDuration() != null) {
                fileMsg.setDuration(sendDTO.getDuration());
            }
            messageToSave.add(fileMsg);
        }

        if (messageToSave.isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }

        // 4. 统一处理消息号生成（支持客户端指定messageNo幂等去重）
        String baseMessageNo = System.currentTimeMillis() + "_" + IdUtil.fastSimpleUUID();
        for (int i = 0; i < messageToSave.size(); i++) {
            ChatMessage msg = messageToSave.get(i);
            // 生成唯一消息号：单条消息且客户端指定messageNo则使用客户端的，否则使用服务端生成的UUID
            String uniqueMessageNo = messageToSave.size() == 1 && StrUtil.isNotBlank(sendDTO.getMessageNo())
                    ? sendDTO.getMessageNo()
                    : baseMessageNo + "_" + i;
            msg.setMessageNo(uniqueMessageNo);
            log.debug("消息号生成成功：{}", uniqueMessageNo);
        }

        // 5. 批量保存消息到数据库（处理幂等性异常）
        try {
            saveBatch(messageToSave);
            log.info("Messages saved successfully, count={}, sessionId={}", messageToSave.size(), sendDTO.getSessionId());
        } catch (Exception e) {
            if (isDuplicateMessageNoException(e) && StrUtil.isNotBlank(sendDTO.getMessageNo())) {
                ChatMessage existingMessage = getMessageByMessageNo(sendDTO.getMessageNo(), isPublicFile);
                if (existingMessage != null) {
                    cleanupUploadedFiles(uploadResults, isPublicFile);
                    log.warn("Message already exists for messageNo={}, returning existing record", sendDTO.getMessageNo());
                    return existingMessage;
                }
            }
            log.error("Failed to save messages", e);
            throw new BusinessException("消息保存失败: " + e.getMessage());
        }

        // 5. 发布消息事件（推送、离线存储等）
        ChatMessage resultMsg = messageToSave.get(messageToSave.size() - 1);
        messageEventPublisher.publishMessageSentEvent(resultMsg);

        if (StrUtil.isNotBlank(resultMsg.getFileUrl())) {
            String accessibleUrl = minioUtil.getChatFileUrl(resultMsg.getFileUrl(), isPublicFile);
            resultMsg.setFileUrl(accessibleUrl);
        }

        return resultMsg;
    }

    private boolean isDuplicateMessageNoException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof DuplicateKeyException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.contains("uk_message_no")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private ChatMessage getMessageByMessageNo(String messageNo, boolean isPublicFile) {
        ChatMessage existingMessage = getOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getMessageNo, messageNo)
                .last("LIMIT 1"));
        if (existingMessage == null) {
            return null;
        }
        if (StrUtil.isNotBlank(existingMessage.getFileUrl())) {
            existingMessage.setFileUrl(minioUtil.getChatFileUrl(existingMessage.getFileUrl(), isPublicFile));
        }
        return existingMessage;
    }

    // ==================== 消息查询（分页） ====================
    /**
     * 查询指定会话的历史消息（分页）
     * 使用 MyBatis-Plus 分页插件，性能优于全量查询
     */
    @Override
    public Page<ChatMessage> getMessages(String sessionId, int current, int size) {
        // 参数校验
        if (StrUtil.isBlank(sessionId)) {
            throw new BusinessException("会话ID不能为空");
        }

        // 分页参数标准化（防止恶意大查询导致OOM）
        int normalizedPage = normalizePageNum(current);
        int normalizedSize = normalizePageSize(size);

        // 构建分页查询参数
        Page<ChatMessage> page = new Page<>(normalizedPage, normalizedSize);
        Date retentionCutoff = getMessageRetentionCutoff();


        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getIsDeleted, 0)
                .ge(ChatMessage::getSendTime, retentionCutoff)
                .orderByDesc(ChatMessage::getSendTime); // 最新消息在前（前端聊天窗口显示顺序）

        // 执行分页查询
        Page<ChatMessage> resultPage = page(page, wrapper);

        // 补充文件可访问URL
        fillFileAccessibleUrls(resultPage.getRecords());

        // 补充发送者信息（避免N+1查询问题）
        enrichSenderInfo(resultPage.getRecords());

        // 补充引用消息内容
        enrichQuoteMessageInfo(resultPage.getRecords());

        log.info("分页查询消息成功，sessionId: {}, current: {}, size: {}, total: {}",
                sessionId, current, size, resultPage.getTotal());
        return resultPage;
    }

    /**
     * 查询指定会话的历史消息（已废弃，请使用分页接口）
     */
    @Override
    @Deprecated
    public List<ChatMessage> getMessages(String sessionId) {
        // 参数校验
        if (StrUtil.isBlank(sessionId)) {
            throw new BusinessException("会话ID不能为空");
        }

        Date retentionCutoff = getMessageRetentionCutoff();
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getIsDeleted, 0)
                .ge(ChatMessage::getSendTime, retentionCutoff)
                .orderByAsc(ChatMessage::getSendTime);

        List<ChatMessage> messages = list(wrapper);

        // 补充文件可访问URL
        fillFileAccessibleUrls(messages);

        // 补充发送者信息（避免N+1查询问题，已在发送时缓存senderName和avatar）
        enrichSenderInfo(messages);

        // 补充引用消息内容
        enrichQuoteMessageInfo(messages);

        log.info("查询历史消息成功（已废弃，请使用分页接口），sessionId: {}, 数量: {}", sessionId, messages.size());
        return messages;
    }

    /**
     * 补充消息的文件可访问URL（仅补充非http开头的fileUrl）
     */
    private void fillFileAccessibleUrls(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        for (ChatMessage msg : messages) {
            if (StrUtil.isNotBlank(msg.getFileUrl()) && !msg.getFileUrl().startsWith("http")) {
                boolean isPublicFile = SessionType.isGroup(msg.getSessionType());
                String accessibleUrl = minioUtil.getChatFileUrl(msg.getFileUrl(), isPublicFile);
                msg.setFileUrl(accessibleUrl);
            }
        }
    }

    @Override
    public List<ChatMessage> getOfflineMessages(String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new BusinessException("Invalid userId");
        }

        Date retentionCutoff = getMessageRetentionCutoff();
        ChatOfflineCursor cursor = chatOfflineCursorMapper.selectById(userId);
        List<String> groupSessionIds = chatGroupMemberService.getActiveGroupIdsByUserId(userId).stream()
                .map(groupId -> GROUP_SESSION_PREFIX + groupId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> {
            w.and(single -> single.eq(ChatMessage::getReceiverId, userId)
                    .eq(ChatMessage::getSessionType, SessionType.SINGLE.getCode()));
            if (!groupSessionIds.isEmpty()) {
                w.or(group -> group.eq(ChatMessage::getSessionType, SessionType.GROUP.getCode())
                        .in(ChatMessage::getSessionId, groupSessionIds)
                        .ne(ChatMessage::getSenderId, userId));
            }
        });

        applyOfflineCursorFilter(wrapper, cursor);

        wrapper.eq(ChatMessage::getStatus, MessageStatus.SEND_SUCCESS.getCode())
                .eq(ChatMessage::getIsDeleted, 0)
                .ge(ChatMessage::getSendTime, retentionCutoff)
                .orderByAsc(ChatMessage::getSendTime)
                .orderByAsc(ChatMessage::getId);

        List<ChatMessage> messages = list(wrapper);
        fillFileAccessibleUrls(messages);
        enrichSenderInfo(messages);
        enrichQuoteMessageInfo(messages);
        log.info("Offline messages fetched, userId={}, cursorTime={}, cursorMessageId={}, count={}",
                userId,
                cursor != null ? cursor.getLastMessageTime() : null,
                cursor != null ? cursor.getLastMessageId() : null,
                messages.size());
        return messages;
    }

    @Override
    public List<ChatMessage> getOfflineMessages(String userId, Long afterTimestamp) {
        if (StrUtil.isBlank(userId)) {
            throw new BusinessException("Invalid userId");
        }

        Date retentionCutoff = getMessageRetentionCutoff();
        ChatOfflineCursor cursor = chatOfflineCursorMapper.selectById(userId);
        List<String> groupSessionIds = chatGroupMemberService.getActiveGroupIdsByUserId(userId).stream()
                .map(groupId -> GROUP_SESSION_PREFIX + groupId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> {
            w.and(single -> single.eq(ChatMessage::getReceiverId, userId)
                    .eq(ChatMessage::getSessionType, SessionType.SINGLE.getCode()));
            if (!groupSessionIds.isEmpty()) {
                w.or(group -> group.eq(ChatMessage::getSessionType, SessionType.GROUP.getCode())
                        .in(ChatMessage::getSessionId, groupSessionIds)
                        .ne(ChatMessage::getSenderId, userId));
            }
        });

        // 应用游标过滤
        applyOfflineCursorFilter(wrapper, cursor);

        // 应用时间范围过滤（afterTimestamp）
        if (afterTimestamp != null && afterTimestamp > 0) {
            Date afterDate = new Date(afterTimestamp);
            // 使用较大的时间值（取游标时间和 afterTimestamp 的较大值）
            if (cursor != null && cursor.getLastMessageTime() != null) {
                // 如果游标时间更晚，使用游标时间
                if (cursor.getLastMessageTime().after(afterDate)) {
                    // 游标过滤已在 applyOfflineCursorFilter 中应用
                } else {
                    // afterTimestamp 更晚，额外添加时间过滤
                    wrapper.ge(ChatMessage::getSendTime, afterDate);
                }
            } else {
                // 没有游标，直接使用 afterTimestamp
                wrapper.ge(ChatMessage::getSendTime, afterDate);
            }
        }

        wrapper.eq(ChatMessage::getStatus, MessageStatus.SEND_SUCCESS.getCode())
                .eq(ChatMessage::getIsDeleted, 0)
                .ge(ChatMessage::getSendTime, retentionCutoff)
                .orderByAsc(ChatMessage::getSendTime)
                .orderByAsc(ChatMessage::getId);

        List<ChatMessage> messages = list(wrapper);
        fillFileAccessibleUrls(messages);
        enrichSenderInfo(messages);
        enrichQuoteMessageInfo(messages);
        log.info("Offline messages fetched with time filter, userId={}, afterTimestamp={}, cursorTime={}, cursorMessageId={}, count={}",
                userId, afterTimestamp,
                cursor != null ? cursor.getLastMessageTime() : null,
                cursor != null ? cursor.getLastMessageId() : null,
                messages.size());
        return messages;
    }

    @Override
    public void markOfflineMessagesAsPulled(String userId, List<ChatMessage> messages) {
        if (StrUtil.isBlank(userId) || messages == null || messages.isEmpty()) {
            return;
        }

        ChatMessage checkpoint = messages.stream()
                .filter(message -> message.getSendTime() != null && message.getId() != null)
                .max(OFFLINE_CURSOR_COMPARATOR)
                .orElse(null);
        if (checkpoint == null) {
            return;
        }

        Date now = new Date();
        chatOfflineCursorMapper.upsertCursor(userId, checkpoint.getId(), checkpoint.getSendTime(), now);
        log.info("Offline cursor advanced, userId={}, lastMessageId={}, lastMessageTime={}",
                userId, checkpoint.getId(), checkpoint.getSendTime());
    }

    private void applyOfflineCursorFilter(LambdaQueryWrapper<ChatMessage> wrapper, ChatOfflineCursor cursor) {
        if (cursor == null || cursor.getLastMessageTime() == null) {
            return;
        }

        Date lastMessageTime = cursor.getLastMessageTime();
        Long lastMessageId = cursor.getLastMessageId();
        wrapper.and(w -> w.gt(ChatMessage::getSendTime, lastMessageTime)
                .or(lastTime -> lastTime.eq(ChatMessage::getSendTime, lastMessageTime)
                        .gt(lastMessageId != null, ChatMessage::getId, lastMessageId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markMessagesAsRead(String sessionId, String userId) {
        if (StrUtil.isBlank(sessionId) || StrUtil.isBlank(userId)) {
            throw new BusinessException("会话ID和用户ID不能为空");
        }

        // 1. 更新消息状态为已读
        LambdaUpdateWrapper<ChatMessage> msgUpdateWrapper = new LambdaUpdateWrapper<>();
        msgUpdateWrapper.eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getReceiverId, userId)
                .eq(ChatMessage::getStatus, MessageStatus.SEND_SUCCESS.getCode())
                .eq(ChatMessage::getIsDeleted, 0)
                .set(ChatMessage::getStatus, MessageStatus.READ.getCode());

        int updateCount = baseMapper.update(null, msgUpdateWrapper);
        log.info("批量标记已读成功，sessionId: {}, userId: {}, 数量: {}", sessionId, userId, updateCount);

        // 2. 发布消息已读事件（用于WebSocket推送给发送者，刷新其会话未读数）
        messageEventPublisher.publishClearUnreadEvent(sessionId, userId);
    }

    // ==================== 消息撤回 ====================
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
            throw new BusinessException("Message not found");
        }

        // 2. 权限校验（包括单聊自己、群管理员/群主权限）
        checkRecallPermission(originalMsg, currentUser);

        // 3. 时间校验
        long timeDiff = System.currentTimeMillis() - originalMsg.getSendTime().getTime();
        if (timeDiff > ChatConstants.REVOKE_TIME_LIMIT) {
            throw new BusinessException("消息已超过撤回时间限制");
        }

        // 4. 状态校验
        if (!MessageStatus.SEND_SUCCESS.getCode().equals(originalMsg.getStatus())
                && !MessageStatus.READ.getCode().equals(originalMsg.getStatus())) {
            throw new BusinessException("Message cannot be revoked");
        }

        // 5. 执行撤回操作
        LambdaUpdateWrapper<ChatMessage> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatMessage::getId, messageId)
                .set(ChatMessage::getStatus, MessageStatus.REVOKED.getCode())
                .set(ChatMessage::getContentReplaced, "[消息已撤回]")
                .set(ChatMessage::getRevokeTime, new Date())
                .set(ChatMessage::getOperatorId, currentUser.getId())
                .set(ChatMessage::getIsRecalled, 1);

        update(updateWrapper);
        log.info("消息撤回成功，messageId: {}, operator: {}", messageId, currentUser.getId());

        // 6. 发布消息撤回事件（用于WebSocket推送给接收者）
        messageEventPublisher.publishMessageRevokedEvent(originalMsg);
    }

    // ==================== 消息编辑 ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO editMessage(Long messageId, String newContent) {
        if (messageId == null) {
            throw new BusinessException("消息ID不能为空");
        }
        if (StrUtil.isBlank(newContent)) {
            throw new BusinessException("新内容不能为空");
        }
        if (newContent.length() > 5000) {
            throw new BusinessException("消息内容不能超过5000个字符");
        }

        UserInfoVO currentUser = UserContextUtil.getCurrentUser();

        // 1. 查询原消息
        ChatMessage originalMsg = getById(messageId);
        if (originalMsg == null) {
            throw new BusinessException("消息不存在");
        }

        // 2. 权限校验：仅发送者本人可以编辑
        if (!originalMsg.getSenderId().equals(currentUser.getId())) {
            throw new BusinessException("只能编辑自己发送的消息");
        }

        // 3. 时间校验：2分钟内可编辑
        long timeDiff = System.currentTimeMillis() - originalMsg.getSendTime().getTime();
        if (timeDiff > ChatConstants.REVOKE_TIME_LIMIT) {
            throw new BusinessException("消息已超过编辑时间限制（2分钟）");
        }

        // 4. 状态校验：仅发送成功或已读状态可编辑
        if (!MessageStatus.SEND_SUCCESS.getCode().equals(originalMsg.getStatus())
                && !MessageStatus.READ.getCode().equals(originalMsg.getStatus())) {
            throw new BusinessException("当前状态的消息无法编辑");
        }

        // 5. 检查是否是文本消息
        if (originalMsg.getMessageType() == null || originalMsg.getMessageType() != 1) {
            throw new BusinessException("只能编辑文本消息");
        }

        // 6. 执行编辑操作
        Date now = new Date();
        LambdaUpdateWrapper<ChatMessage> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatMessage::getId, messageId)
                .set(ChatMessage::getContent, newContent)
                .set(ChatMessage::getIsEdited, 1)
                .set(ChatMessage::getEditTime, now);

        update(updateWrapper);
        log.info("消息编辑成功，messageId: {}, operator: {}", messageId, currentUser.getId());

        // 7. 查询更新后的消息并转换为VO
        ChatMessage updatedMsg = getById(messageId);
        ChatMessageVO vo = BeanConvertUtil.convert(updatedMsg, ChatMessageVO.class);
        vo.setSenderName(originalMsg.getSenderName());
        vo.setSenderAvatar(originalMsg.getSenderAvatar());

        // 8. 发布消息编辑事件（用于WebSocket推送）
        messageEventPublisher.publishMessageEditedEvent(updatedMsg);

        return vo;
    }

    /**
     * 检查消息撤回权限
     * - 单聊：仅发送者可撤回
     * - 群聊：
     *   - 普通成员：仅自己的消息
     *   - 管理员：任意群友消息
     *   - 群主：所有消息（包括管理员）
     */
    private void checkRecallPermission(ChatMessage message, UserInfoVO currentUser) {
        String senderId = message.getSenderId();
        String currentUserId = currentUser.getId();

        // 自己发的消息可以直接撤回
        if (senderId.equals(currentUserId)) {
            return;
        }

        // 非本人消息，检查是否是群聊
        Integer sessionType = message.getSessionType();
        if (sessionType == null || sessionType != 2) {
            // 单聊：非本人不能撤回
            throw new BusinessException("只能撤回自己发送的消息");
        }

        // 群聊：检查权限
        String sessionId = message.getSessionId();
        if (sessionId == null) {
            throw new BusinessException("会话ID为空");
        }

        // 从sessionId提取群ID（格式：group_xxx）
        String groupIdStr = sessionId;
        if (sessionId.startsWith("group_")) {
            groupIdStr = sessionId.substring(6);
        }

        try {
            Long groupId = Long.parseLong(groupIdStr);

            // 检查当前用户在群中的角色
            Integer currentRole = chatGroupMemberService.getUserRole(groupId, currentUserId);
            if (currentRole == null) {
                throw new BusinessException("您不在群中，无法撤回消息");
            }

            // 检查发送者在群中的角色
            Integer senderRole = chatGroupMemberService.getUserRole(groupId, senderId);

            // 群主(1)可以撤回任何人的消息
            if (currentRole == 1) {
                return;
            }

            // 管理员(2)可以撤回普通成员的消息，但不能撤回另一个管理员或群主的消息
            if (currentRole == 2) {
                if (senderRole != null && senderRole > 2) {
                    return;
                }
                throw new BusinessException("管理员只能撤回普通成员的消息");
            }

            // 普通成员(3)只能撤回自己的消息
            throw new BusinessException("普通成员只能撤回自己发送的消息");
        } catch (NumberFormatException e) {
            throw new BusinessException("无效的群ID");
        }
    }

    // ==================== 消息删除 ====================
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

        // 2. 权限校验（复用撤回权限检查逻辑）
        checkDeletePermission(originalMsg, currentUser);

        // 3. 执行删除操作（软删除）
        LambdaUpdateWrapper<ChatMessage> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatMessage::getId, messageId)
                .set(ChatMessage::getIsDeleted, 1)
                .set(ChatMessage::getStatus, MessageStatus.DELETED.getCode());

        update(updateWrapper);
        log.info("消息删除成功，messageId: {}, operator: {}", messageId, currentUser.getId());

        // 4. 发布消息删除事件（用于WebSocket推送）
        messageEventPublisher.publishMessageDeletedEvent(originalMsg);
    }

    /**
     * 检查消息删除权限（与撤回权限逻辑相同）
     * - 单聊：仅发送者可删除
     * - 群聊：
     *   - 普通成员：仅自己的消息
     *   - 管理员：任意群友消息
     *   - 群主：所有消息（包括管理员）
     */
    private void checkDeletePermission(ChatMessage message, UserInfoVO currentUser) {
        String senderId = message.getSenderId();
        String currentUserId = currentUser.getId();

        // 自己发的消息可以直接删除
        if (senderId.equals(currentUserId)) {
            return;
        }

        // 非本人消息，检查是否是群聊
        Integer sessionType = message.getSessionType();
        if (sessionType == null || sessionType != 2) {
            // 单聊：非本人不能删除
            throw new BusinessException("只能删除自己发送的消息");
        }

        // 群聊：检查权限
        String sessionId = message.getSessionId();
        if (sessionId == null) {
            throw new BusinessException("会话ID为空");
        }

        // 从sessionId提取群ID（格式：group_xxx）
        String groupIdStr = sessionId;
        if (sessionId.startsWith("group_")) {
            groupIdStr = sessionId.substring(6);
        }

        try {
            Long groupId = Long.parseLong(groupIdStr);

            // 检查当前用户在群中的角色
            Integer currentRole = chatGroupMemberService.getUserRole(groupId, currentUserId);
            if (currentRole == null) {
                throw new BusinessException("您不在群中，无法删除消息");
            }

            // 检查发送者在群中的角色
            Integer senderRole = chatGroupMemberService.getUserRole(groupId, senderId);

            // 群主(1)可以删除任何人的消息
            if (currentRole == 1) {
                return;
            }

            // 管理员(2)可以删除普通成员的消息，但不能删除另一个管理员或群主的消息
            if (currentRole == 2) {
                if (senderRole != null && senderRole > 2) {
                    return;
                }
                throw new BusinessException("管理员只能删除普通成员的消息");
            }

            // 普通成员(3)只能删除自己的消息
            throw new BusinessException("普通成员只能删除自己发送的消息");
        } catch (NumberFormatException e) {
            throw new BusinessException("无效的群ID");
        }
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMessageVO> forwardMessages(List<Long> messageIds, String targetSessionId, String senderId) {
        if (messageIds == null || messageIds.isEmpty()) {
            throw new BusinessException("消息ID列表不能为空");
        }
        if (StrUtil.isBlank(targetSessionId)) {
            throw new BusinessException("目标会话ID不能为空");
        }

        List<ChatMessageVO> result = new ArrayList<>();

        ChatSession targetSession = chatSessionMapper.selectById(targetSessionId);
        if (targetSession == null) {
            throw new BusinessException("目标会话不存在");
        }

        int targetSessionType = targetSession.getSessionType();

        for (Long messageId : messageIds) {
            ChatMessage originalMsg = getById(messageId);
            if (originalMsg == null) {
                continue;
            }

            if (originalMsg.getStatus() != null &&
                (originalMsg.getStatus().equals(MessageStatus.REVOKED.getCode()) ||
                 originalMsg.getStatus().equals(MessageStatus.DELETED.getCode()))) {
                continue;
            }

            ChatMessage forwardedMsg = new ChatMessage();
            forwardedMsg.setMessageNo(generateMessageNo());
            forwardedMsg.setSessionId(targetSessionId);
            forwardedMsg.setSessionType(targetSessionType);
            forwardedMsg.setSenderId(senderId);

            if (targetSessionType == 1) {
                String currentUserId = senderId;
                if (originalMsg.getSenderId().equals(currentUserId)) {
                    forwardedMsg.setReceiverId(originalMsg.getReceiverId());
                } else {
                    String otherUserId = targetSession.getUserId().equals(currentUserId)
                            ? targetSession.getTargetId()
                            : targetSession.getUserId();
                    forwardedMsg.setReceiverId(otherUserId);
                }
            } else {
                forwardedMsg.setReceiverId(null);
            }

            forwardedMsg.setMessageType(originalMsg.getMessageType());
            forwardedMsg.setContent(originalMsg.getContent());
            forwardedMsg.setFileUrl(originalMsg.getFileUrl());
            forwardedMsg.setFileName(originalMsg.getFileName());
            forwardedMsg.setFileSize(originalMsg.getFileSize());
            forwardedMsg.setFileType(originalMsg.getFileType());
            forwardedMsg.setThumbnailUrl(originalMsg.getThumbnailUrl());
            forwardedMsg.setDuration(originalMsg.getDuration());
            forwardedMsg.setStatus(MessageStatus.SEND_SUCCESS.getCode());

            save(forwardedMsg);

            ChatMessageVO vo = BeanConvertUtil.convert(forwardedMsg, ChatMessageVO.class);
            result.add(vo);
        }

        return result;
    }


    // ==================== 离线消息处理 ====================
    @Override
    public void markMessageAsOffline(Long id) {
        log.info("消息 {} 已标记为离线", id);
        // 后续扩展：可在此处记录离线消息统计信息
    }

    // ==================== 消息过期清理定时任务 ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int purgeExpiredMessages() {
        Date retentionCutoff = getMessageRetentionCutoff();
        Date now = new Date();
        int totalPurged = 0;
        Set<String> affectedSessionIds = new LinkedHashSet<>();

        while (true) {
            List<ChatMessage> expiredMessages = list(new LambdaQueryWrapper<ChatMessage>()
                    .select(ChatMessage::getId, ChatMessage::getSessionId, ChatMessage::getFileUrl)
                    .eq(ChatMessage::getIsDeleted, 0)
                    .lt(ChatMessage::getSendTime, retentionCutoff)
                    .orderByAsc(ChatMessage::getSendTime)
                    .last("LIMIT 500"));

            if (expiredMessages.isEmpty()) {
                break;
            }

            // 查询被收藏的消息ID，这些消息需要保留
            Set<Long> favoritedMessageIds = getFavoritedMessageIds(
                    expiredMessages.stream()
                            .map(ChatMessage::getId)
                            .filter(id -> id != null)
                            .collect(Collectors.toSet())
            );

            // 过滤掉被收藏的消息
            List<ChatMessage> messagesToPurge = expiredMessages.stream()
                    .filter(msg -> msg.getId() != null && !favoritedMessageIds.contains(msg.getId()))
                    .collect(Collectors.toList());

            if (messagesToPurge.isEmpty()) {
                log.debug("No messages to purge in this batch (all are favorited or already processed)");
                continue;
            }

            List<Long> expiredIds = messagesToPurge.stream()
                    .map(ChatMessage::getId)
                    .filter(id -> id != null)
                    .collect(Collectors.toList());
            if (expiredIds.isEmpty()) {
                break;
            }

            messagesToPurge.stream()
                    .map(ChatMessage::getSessionId)
                    .filter(StrUtil::isNotBlank)
                    .forEach(affectedSessionIds::add);

            LambdaUpdateWrapper<ChatMessage> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(ChatMessage::getId, expiredIds)
                    .set(ChatMessage::getIsDeleted, 1)
                    .set(ChatMessage::getStatus, MessageStatus.DELETED.getCode())
                    .set(ChatMessage::getUpdateTime, now);
            baseMapper.update(null, updateWrapper);

            cleanupExpiredMessageFiles(messagesToPurge);
            totalPurged += expiredIds.size();

            // 记录被保留的收藏消息数量
            int skippedCount = expiredMessages.size() - messagesToPurge.size();
            if (skippedCount > 0) {
                log.debug("Skipped {} favorited messages from purge", skippedCount);
            }
        }

        refreshSessionsAfterRetentionCleanup(affectedSessionIds, retentionCutoff, now);

        if (totalPurged > 0) {
            log.info("Expired chat messages purged, count={}, retentionDays={}, affectedSessions={}",
                    totalPurged, messageRetentionDays, affectedSessionIds.size());
        }
        return totalPurged;
    }

    /**
     * 获取被收藏的消息ID集合
     *
     * @param messageIds 消息ID列表
     * @return 被收藏的消息ID集合
     */
    private Set<Long> getFavoritedMessageIds(Set<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Collections.emptySet();
        }

        List<Favorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<Favorite>()
                        .in(Favorite::getMessageId, messageIds)
                        .select(Favorite::getMessageId)
        );

        return favorites.stream()
                .map(Favorite::getMessageId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage saveDirectly(String sessionId, String senderId, String receiverId,
                                    int sessionType, int messageType, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setSessionType(sessionType);
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setMessageType(messageType);
        msg.setContent(content);
        msg.setSendTime(new Date());
        msg.setStatus(MessageStatus.SEND_SUCCESS.getCode());
        msg.setMessageNo(IdUtil.simpleUUID());
        msg.setIsSensitive(0);
        msg.setIsDeleted(0);
        save(msg);
        log.info("直接保存消息成功，sessionId={}, type={}, content={}", sessionId, messageType, content);
        return msg;
    }

    // ==================== 消息搜索功能 ====================
    @Override
    public Page<ChatMessage> searchMessages(String userId, String keyword, String sessionId, String messageType, int current, int size) {
        // 构建查询条件
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(ChatMessage::getContent, keyword)
               .eq(ChatMessage::getIsDeleted, 0);

        // 消息类型筛选
        if (messageType != null && !messageType.isEmpty() && !messageType.equals("all")) {
            Integer msgTypeCode = getMessageTypeCode(messageType);
            if (msgTypeCode != null) {
                wrapper.eq(ChatMessage::getMessageType, msgTypeCode);
            }
        }

        // 如果指定了会话ID，则只搜索该会话
        if (sessionId != null && !sessionId.isEmpty()) {
            wrapper.eq(ChatMessage::getSessionId, sessionId);
        } else {
            // 如果没有指定会话，搜索用户参与的所有会话的消息
            // 查询用户参与的所有会话ID
            List<String> userSessionIds = getUserSessionIds(userId);
            if (userSessionIds.isEmpty()) {
                return new Page<>(current, size);
            }
            wrapper.in(ChatMessage::getSessionId, userSessionIds);
        }

        // 按发送时间倒序排列
        wrapper.orderByDesc(ChatMessage::getSendTime);

        // 分页查询
        Page<ChatMessage> page = new Page<>(current, size);
        return this.page(page, wrapper);
    }

    /**
     * 将消息类型字符串转换为数据库存储的整数值
     */
    private Integer getMessageTypeCode(String messageType) {
        if (messageType == null || messageType.isEmpty()) return null;
        return switch (messageType.toUpperCase()) {
            case "TEXT" -> 1;
            case "IMAGE" -> 2;
            case "FILE" -> 3;
            case "VOICE", "AUDIO" -> 4;
            case "VIDEO" -> 5;
            default -> null;
        };
    }

    /**
     * 获取用户参与的所有会话ID
     */
    private List<String> getUserSessionIds(String userId) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId)
               .select(ChatSession::getId);
        List<ChatSession> sessions = chatSessionMapper.selectList(wrapper);
        return sessions.stream()
                .map(s -> String.valueOf(s.getId()))
                .collect(Collectors.toList());
    }

    // ==================== 辅助方法：发送者信息批量填充 ====================
    /**
     * 批量填充消息发送者信息（昵称、头像URL）
     * 避免在循环中逐个查询 chat_user 表导致 N+1 查询问题
     * 优化：批量获取 MinIO URL，减少远程调用次数
     */
    private void enrichSenderInfo(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) return;

        Set<String> senderIds = messages.stream()
                .map(ChatMessage::getSenderId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        if (senderIds.isEmpty()) return;

        // 批量查询用户信息（一次数据库查询）
        Map<String, ChatUser> senderMap = new HashMap<>();
        chatUserMapper.selectList(
                new LambdaQueryWrapper<ChatUser>()
                        .in(ChatUser::getId, senderIds)
                        .select(ChatUser::getId, ChatUser::getNickname, ChatUser::getAvatar)
        ).forEach(u -> senderMap.put(u.getId(), u));

        // 批量构建头像URL映射（使用MinIO批量方法优化性能）
        Map<String, String> userIdToAvatar = new HashMap<>();
        for (ChatUser user : senderMap.values()) {
            userIdToAvatar.put(user.getId(), user.getAvatar());
        }
        Map<String, String> avatarUrlMap = minioUtil.getAvatarUrlsBatch(userIdToAvatar);

        // 填充消息发送者信息
        for (ChatMessage msg : messages) {
            ChatUser sender = senderMap.get(msg.getSenderId());
            if (sender != null) {
                msg.setSenderName(sender.getNickname());
                // 使用预构建的URL映射，避免重复调用MinIO
                msg.setSenderAvatar(avatarUrlMap.getOrDefault(sender.getId(), ""));
            }
        }
    }

    /**
     * 批量填充消息的引用消息内容
     * 用于消息引用回复功能的展示
     */
    private void enrichQuoteMessageInfo(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) return;

        // 找出有引用消息ID的消息
        List<Long> quoteMessageIds = messages.stream()
                .map(ChatMessage::getQuoteMessageId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (quoteMessageIds.isEmpty()) return;

        // 批量查询引用消息
        Map<Long, ChatMessage> quoteMessageMap = new HashMap<>();
        list(new LambdaQueryWrapper<ChatMessage>()
                .in(ChatMessage::getId, quoteMessageIds)
                .select(ChatMessage::getId, ChatMessage::getContent, ChatMessage::getSenderId,
                        ChatMessage::getMessageType, ChatMessage::getFileUrl))
                .forEach(msg -> quoteMessageMap.put(msg.getId(), msg));

        if (quoteMessageMap.isEmpty()) return;

        // 补充引用消息的发送者名称
        Set<String> quoteSenderIds = quoteMessageMap.values().stream()
                .map(ChatMessage::getSenderId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());

        Map<String, String> senderNameMap = new HashMap<>();
        if (!quoteSenderIds.isEmpty()) {
            chatUserMapper.selectList(
                    new LambdaQueryWrapper<ChatUser>()
                            .in(ChatUser::getId, quoteSenderIds)
                            .select(ChatUser::getId, ChatUser::getNickname)
            ).forEach(u -> senderNameMap.put(u.getId(), u.getNickname()));
        }

        // 为每条消息设置引用内容
        for (ChatMessage msg : messages) {
            if (msg.getQuoteMessageId() != null) {
                ChatMessage quoteMsg = quoteMessageMap.get(msg.getQuoteMessageId());
                if (quoteMsg != null) {
                    // 设置引用消息的senderName（非数据库字段）
                    quoteMsg.setSenderName(senderNameMap.getOrDefault(quoteMsg.getSenderId(), "未知用户"));
                    // 由于 ChatMessage 没有 quoteSenderName 和 quoteContent 字段，
                    // 我们将这些信息放在 extInfo 字段中，方便前端解析
                    try {
                        Map<String, Object> extInfo = new HashMap<>();
                        extInfo.put("quoteMessageId", quoteMsg.getId());
                        extInfo.put("quoteSenderName", quoteMsg.getSenderName());
                        extInfo.put("quoteContent", quoteMsg.getContent());
                        extInfo.put("quoteMessageType", quoteMsg.getMessageType());
                        extInfo.put("quoteFileUrl", quoteMsg.getFileUrl());
                        msg.setExtInfo(extInfo);
                    } catch (Exception e) {
                        log.warn("Failed to set quote message info for message {}", msg.getId(), e);
                    }
                }
            }
        }
    }

    // ==================== 辅助方法：消息保留期限计算 ====================
    /**
     * 计算消息保留截止时间
     * messageNo 去重策略依赖于保留期设置，需确保系统时钟一致性
     * 如需修改保留策略，请同步修改 saveDirectly 方法中的 messageNo 生成策略（使用 UUID）
     */
    private Date getMessageRetentionCutoff() {
        int retentionDays = Math.max(1, messageRetentionDays);
        long cutoffMs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays);
        return new Date(cutoffMs);
    }

    private void refreshSessionsAfterRetentionCleanup(Set<String> sessionIds, Date cutoff, Date now) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }

        List<String> targetSessionIds = new ArrayList<>(sessionIds);
        chatSessionMapper.refreshSessionsAfterRetentionCleanup(targetSessionIds, cutoff, now);
        chatSessionMapper.recalculateSingleUnreadAfterRetentionCleanup(
                targetSessionIds,
                MessageStatus.SEND_SUCCESS.getCode(),
                cutoff,
                now
        );
        chatSessionMapper.clearUnreadForEmptySessions(targetSessionIds, now);
    }

    private void cleanupExpiredMessageFiles(List<ChatMessage> expiredMessages) {
        if (expiredMessages == null || expiredMessages.isEmpty()) {
            return;
        }

        Set<String> objectNames = expiredMessages.stream()
                .map(ChatMessage::getFileUrl)
                .filter(StrUtil::isNotBlank)
                .filter(fileUrl -> !fileUrl.startsWith("http"))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (String objectName : objectNames) {
            try {
                long activeReferenceCount = count(new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getFileUrl, objectName)
                        .eq(ChatMessage::getIsDeleted, 0));
                if (activeReferenceCount > 0) {
                    continue;
                }
                minioUtil.removeObject(objectName);
            } catch (Exception error) {
                log.warn("Failed to remove expired chat object: {}", objectName, error);
            }
        }
    }

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
        // 支持客户端指定messageNo幂等去重（兼容旧版本）
        if (StrUtil.isNotBlank(sendDTO.getMessageNo())) {
            message.setMessageNo(sendDTO.getMessageNo());
        } else {
            message.setMessageNo(IdUtil.simpleUUID());
        }
        // 设置@提及信息
        if (sendDTO.getAtUserIds() != null && !sendDTO.getAtUserIds().isEmpty()) {
            message.setAtUserIds(JSON.toJSONString(sendDTO.getAtUserIds()));
        }
        if (sendDTO.getIsAtAll() != null) {
            message.setIsAtAll(sendDTO.getIsAtAll());
        }
        // 设置引用消息ID
        if (sendDTO.getQuoteMessageId() != null) {
            message.setQuoteMessageId(sendDTO.getQuoteMessageId());
        }
        return message;
    }

    // ==================== 辅助方法：根据文件类型判断消息类型 ====================
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

    private String generateMessageNo() {
        return IdUtil.simpleUUID();
    }

    // ==================== 分页参数标准化工具方法 ====================
    /**
     * 标准化页码（确保 >= 1）
     * @param pageNum 原始页码
     * @return 标准化后的页码
     */
    private int normalizePageNum(int pageNum) {
        return Math.max(pageNum, DEFAULT_PAGE_NUM);
    }

    /**
     * 标准化每页大小（确保在合法范围内）
     * @param pageSize 原始每页大小
     * @return 标准化后的每页大小（1 ~ MAX_PAGE_SIZE）
     */
    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 限制离线消息最大返回数量
     * @param count 请求的消息数量
     * @return 实际返回的消息数量（不超过 MAX_OFFLINE_MESSAGES）
     */
    private int normalizeOfflineMessageCount(int count) {
        if (count <= 0) {
            return MAX_OFFLINE_MESSAGES;
        }
        return Math.min(count, MAX_OFFLINE_MESSAGES);
    }
}
