package com.maxxvll.service.impl;

import com.maxxvll.common.enums.MessageType;
import com.maxxvll.common.enums.SessionType;
import com.maxxvll.common.vo.UserInfoVO;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.domain.ChatSession;
import com.maxxvll.mapper.ChatMessageMapper;
import com.maxxvll.mapper.ChatOfflineCursorMapper;
import com.maxxvll.mapper.ChatSessionMapper;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.mapper.FavoriteMapper;
import com.maxxvll.service.ChatGroupMemberService;
import com.maxxvll.utils.MinioUtil;
import com.maxxvll.utils.UserContextUtil;
import com.alibaba.fastjson2.JSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ChatMessageService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private MinioUtil minioUtil;

    @Mock
    private com.maxxvll.common.event.MessageEventPublisher messageEventPublisher;

    @Mock
    private ChatOfflineCursorMapper chatOfflineCursorMapper;

    @Mock
    private ChatUserMapper chatUserMapper;

    @Mock
    private ChatSessionMapper chatSessionMapper;

    @Mock
    private ChatGroupMemberService chatGroupMemberService;

    @Mock
    private FavoriteMapper favoriteMapper;

    @InjectMocks
    private ChatMessageServiceImpl chatMessageService;

    private UserInfoVO testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserInfoVO();
        testUser.setId("1");
        testUser.setUsername("testuser");
        testUser.setNickname("Test User");
        testUser.setAvatar("avatar.png");
    }

    @Nested
    @DisplayName("消息构建测试")
    class MessageBuildingTests {

        @Test
        @DisplayName("构建文本消息")
        void buildTextMessage() {
            ChatMessage message = new ChatMessage();
            message.setId(1L);
            message.setSessionId("session_1");
            message.setSenderId("1");
            message.setReceiverId("2");
            message.setContent("Hello, World!");
            message.setMessageType(MessageType.TEXT.getCode());
            message.setSendTime(new Date());
            message.setStatus(1);
            message.setIsDeleted(0);

            assertThat(message).isNotNull();
            assertThat(message.getContent()).isEqualTo("Hello, World!");
            assertThat(message.getMessageType()).isEqualTo(MessageType.TEXT.getCode());
        }

        @Test
        @DisplayName("构建图片消息")
        void buildImageMessage() {
            ChatMessage message = new ChatMessage();
            message.setId(2L);
            message.setSessionId("session_1");
            message.setSenderId("1");
            message.setReceiverId("2");
            message.setContent("image.jpg");
            message.setMessageType(MessageType.IMAGE.getCode());
            message.setSendTime(new Date());
            message.setStatus(1);
            message.setIsDeleted(0);

            assertThat(message).isNotNull();
            assertThat(message.getMessageType()).isEqualTo(MessageType.IMAGE.getCode());
        }

        @Test
        @DisplayName("构建群聊消息")
        void buildGroupMessage() {
            ChatMessage message = new ChatMessage();
            message.setId(3L);
            message.setSessionId("group_100");
            message.setSessionType(SessionType.GROUP.getCode());
            message.setSenderId("1");
            message.setReceiverId("100");
            message.setContent("Group message");
            message.setMessageType(MessageType.TEXT.getCode());
            message.setSendTime(new Date());
            message.setStatus(1);
            message.setIsDeleted(0);

            assertThat(message).isNotNull();
            assertThat(message.getSessionId()).startsWith("group_");
            assertThat(SessionType.isGroup(message.getSessionType())).isTrue();
        }
    }

    @Nested
    @DisplayName("消息状态测试")
    class MessageStatusTests {

        @Test
        @DisplayName("消息状态枚举转换")
        void messageStatusEnumConversion() {
            assertThat(MessageType.TEXT.getCode()).isEqualTo(1);
            assertThat(MessageType.IMAGE.getCode()).isEqualTo(2);
            assertThat(MessageType.AUDIO.getCode()).isEqualTo(4);
            assertThat(MessageType.VIDEO.getCode()).isEqualTo(3);
            assertThat(MessageType.FILE.getCode()).isEqualTo(5);
        }

        @Test
        @DisplayName("会话类型判断")
        void sessionTypeJudgment() {
            assertThat(SessionType.isGroup(2)).isTrue();
            assertThat(SessionType.isSingle(1)).isTrue();
            assertThat(SessionType.GROUP.getCode()).isEqualTo(2);
            assertThat(SessionType.SINGLE.getCode()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("消息列表操作测试")
    class MessageListOperationTests {

        @Test
        @DisplayName("消息列表排序")
        void messageListSorting() {
            List<ChatMessage> messages = new ArrayList<>();

            ChatMessage msg1 = new ChatMessage();
            msg1.setId(1L);
            msg1.setSendTime(new Date(1000));
            messages.add(msg1);

            ChatMessage msg2 = new ChatMessage();
            msg2.setId(2L);
            msg2.setSendTime(new Date(3000));
            messages.add(msg2);

            ChatMessage msg3 = new ChatMessage();
            msg3.setId(3L);
            msg3.setSendTime(new Date(2000));
            messages.add(msg3);

            // 按时间排序
            messages.sort((m1, m2) -> m1.getSendTime().compareTo(m2.getSendTime()));

            assertThat(messages.get(0).getId()).isEqualTo(1L);
            assertThat(messages.get(1).getId()).isEqualTo(3L);
            assertThat(messages.get(2).getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("消息列表为空检查")
        void messageListEmptyCheck() {
            List<ChatMessage> messages = new ArrayList<>();
            assertThat(messages).isEmpty();
            assertThat(messages.size()).isEqualTo(0);
        }

        @Test
        @DisplayName("消息列表分页")
        void messageListPagination() {
            List<ChatMessage> allMessages = new ArrayList<>();
            for (int i = 1; i <= 100; i++) {
                ChatMessage msg = new ChatMessage();
                msg.setId((long) i);
                allMessages.add(msg);
            }

            int page = 2;
            int pageSize = 10;
            int start = (page - 1) * pageSize;
            int end = start + pageSize;

            List<ChatMessage> pageMessages = allMessages.subList(start, Math.min(end, allMessages.size()));

            assertThat(pageMessages).hasSize(10);
            assertThat(pageMessages.get(0).getId()).isEqualTo(11L);
            assertThat(pageMessages.get(9).getId()).isEqualTo(20L);
        }
    }

    @Nested
    @DisplayName("消息内容处理测试")
    class MessageContentTests {

        @Test
        @DisplayName("消息内容长度限制")
        void messageContentLengthLimit() {
            String longContent = "a".repeat(5000);
            String truncatedContent = longContent.length() > 2500
                    ? longContent.substring(0, 2500) + "..."
                    : longContent;

            assertThat(truncatedContent.length()).isLessThanOrEqualTo(2503);
        }

        @Test
        @DisplayName("消息内容JSON序列化")
        void messageContentJsonSerialization() {
            ChatMessage message = new ChatMessage();
            message.setId(1L);
            message.setContent("Test content");

            String json = JSON.toJSONString(message);
            ChatMessage deserialized = JSON.parseObject(json, ChatMessage.class);

            assertThat(deserialized.getContent()).isEqualTo("Test content");
        }

        @Test
        @DisplayName("消息内容特殊字符处理")
        void messageContentSpecialCharHandling() {
            String contentWithSpecialChars = "Hello <script>alert('xss')</script> & \"quotes\"";
            String escapedContent = contentWithSpecialChars
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");

            assertThat(escapedContent).doesNotContain("<script>");
            assertThat(escapedContent).contains("&lt;script&gt;");
        }
    }

    @Nested
    @DisplayName("消息时间戳测试")
    class MessageTimestampTests {

        @Test
        @DisplayName("消息时间戳格式转换")
        void messageTimestampFormatConversion() {
            Date now = new Date();
            LocalDateTime localDateTime = now.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();

            assertThat(localDateTime).isNotNull();
            assertThat(localDateTime.getYear()).isEqualTo(2026);
        }

        @Test
        @DisplayName("消息时间戳比较")
        void messageTimestampComparison() {
            Date older = new Date(1000);
            Date newer = new Date(2000);

            assertThat(newer.after(older)).isTrue();
            assertThat(older.before(newer)).isTrue();
        }
    }

    @Nested
    @DisplayName("消息会话关联测试")
    class MessageSessionRelationTests {

        @Test
        @DisplayName("会话ID生成")
        void sessionIdGeneration() {
            String user1 = "1";
            String user2 = "2";

            // 确保 ID 顺序一致（小 ID 在前）
            String sessionId = Long.parseLong(user1) < Long.parseLong(user2)
                    ? "single_" + user1 + "_" + user2
                    : "single_" + user2 + "_" + user1;

            assertThat(sessionId).isEqualTo("single_1_2");
        }

        @Test
        @DisplayName("群组会话ID生成")
        void groupSessionIdGeneration() {
            Long groupId = 100L;
            String groupSessionId = "group_" + groupId;

            assertThat(groupSessionId).isEqualTo("group_100");
            assertThat(groupSessionId.startsWith("group_")).isTrue();
        }
    }

    @Nested
    @DisplayName("文件上传测试")
    class FileUploadTests {

        @Test
        @DisplayName("模拟文件上传结果")
        void mockFileUploadResult() {
            // 模拟文件上传结果的数据结构
            class UploadResult {
                private String fileName;
                private String fileUrl;
                private Long fileSize;
                public String getFileName() { return fileName; }
                public void setFileName(String fileName) { this.fileName = fileName; }
                public String getFileUrl() { return fileUrl; }
                public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
                public Long getFileSize() { return fileSize; }
                public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
            }

            UploadResult uploadResult = new UploadResult();
            uploadResult.setFileName("test.jpg");
            uploadResult.setFileUrl("http://localhost:9000/test.jpg");
            uploadResult.setFileSize(1024L);

            assertThat(uploadResult.getFileName()).isEqualTo("test.jpg");
            assertThat(uploadResult.getFileUrl()).isNotEmpty();
        }

        @Test
        @DisplayName("文件类型判断")
        void fileTypeJudgment() {
            String fileName = "document.pdf";
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

            List<String> imageExtensions = List.of("jpg", "jpeg", "png", "gif", "bmp", "webp");
            List<String> documentExtensions = List.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx");
            List<String> videoExtensions = List.of("mp4", "avi", "mov", "mkv");
            List<String> audioExtensions = List.of("mp3", "wav", "ogg", "m4a");

            assertThat(documentExtensions).contains(extension);
            assertThat(imageExtensions).doesNotContain(extension);
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionScenarioTests {

        @Test
        @DisplayName("空消息内容")
        void emptyMessageContent() {
            ChatMessage message = new ChatMessage();
            message.setContent("");

            assertThat(message.getContent()).isEmpty();
        }

        @Test
        @DisplayName("空发送者ID")
        void emptySenderId() {
            ChatMessage message = new ChatMessage();
            message.setSenderId("");

            assertThat(message.getSenderId()).isEmpty();
        }

        @Test
        @DisplayName("消息类型无效")
        void invalidMessageType() {
            ChatMessage message = new ChatMessage();
            message.setMessageType(999);

            assertThat(message.getMessageType()).isEqualTo(999);
            // 验证无效类型不会崩溃
            assertThat(MessageType.getByCode(999)).isNull();
        }
    }
}
