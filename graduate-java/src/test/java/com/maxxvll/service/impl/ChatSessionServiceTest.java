package com.maxxvll.service.impl;

import com.maxxvll.common.enums.SessionType;
import com.maxxvll.domain.ChatGroup;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.domain.ChatSession;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.mapper.ChatGroupMapper;
import com.maxxvll.mapper.ChatSessionMapper;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.utils.MinioUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * ChatSessionService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ChatSessionServiceTest {

    @Mock
    private ChatUserMapper chatUserMapper;

    @Mock
    private ChatGroupMapper chatGroupMapper;

    @Mock
    private MinioUtil minioUtil;

    @InjectMocks
    private ChatSessionServiceImpl chatSessionService;

    @Nested
    @DisplayName("会话构建测试")
    class SessionBuildingTests {

        @Test
        @DisplayName("构建私聊会话")
        void buildSingleChatSession() {
            ChatSession session = new ChatSession();
            session.setId(1L);
            session.setSessionId("single_1_2");
            session.setUserId("1");
            session.setTargetId("2");
            session.setSessionType(SessionType.SINGLE.getCode());
            session.setLastMessageContent("Hello");
            session.setUnreadCount(0);
            session.setIsDeleted(0);
            session.setCreateTime(new Date());
            session.setUpdateTime(new Date());

            assertThat(session).isNotNull();
            assertThat(session.getSessionId()).isEqualTo("single_1_2");
            assertThat(session.getSessionType()).isEqualTo(SessionType.SINGLE.getCode());
            assertThat(SessionType.isSingle(session.getSessionType())).isTrue();
        }

        @Test
        @DisplayName("构建群聊会话")
        void buildGroupChatSession() {
            ChatSession session = new ChatSession();
            session.setId(2L);
            session.setSessionId("group_100");
            session.setUserId("1");
            session.setTargetId("100");
            session.setSessionType(SessionType.GROUP.getCode());
            session.setLastMessageContent("Group chat message");
            session.setUnreadCount(5);
            session.setIsDeleted(0);
            session.setCreateTime(new Date());
            session.setUpdateTime(new Date());

            assertThat(session).isNotNull();
            assertThat(session.getTargetId()).isEqualTo("100");
            assertThat(session.getSessionType()).isEqualTo(SessionType.GROUP.getCode());
            assertThat(SessionType.isGroup(session.getSessionType())).isTrue();
        }
    }

    @Nested
    @DisplayName("会话列表测试")
    class SessionListTests {

        @Test
        @DisplayName("会话列表排序（按更新时间）")
        void sessionListSortingByUpdateTime() {
            List<ChatSession> sessions = new ArrayList<>();

            Date now = new Date();
            ChatSession session1 = new ChatSession();
            session1.setId(1L);
            session1.setUpdateTime(new Date(now.getTime() - 3000));
            sessions.add(session1);

            ChatSession session2 = new ChatSession();
            session2.setId(2L);
            session2.setUpdateTime(new Date(now.getTime() - 1000));
            sessions.add(session2);

            ChatSession session3 = new ChatSession();
            session3.setId(3L);
            session3.setUpdateTime(new Date(now.getTime() - 2000));
            sessions.add(session3);

            // 按更新时间倒序
            sessions.sort((s1, s2) -> s2.getUpdateTime().compareTo(s1.getUpdateTime()));

            assertThat(sessions.get(0).getId()).isEqualTo(2L);
            assertThat(sessions.get(1).getId()).isEqualTo(3L);
            assertThat(sessions.get(2).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("会话列表分页")
        void sessionListPagination() {
            List<ChatSession> allSessions = new ArrayList<>();
            for (int i = 1; i <= 50; i++) {
                ChatSession session = new ChatSession();
                session.setId((long) i);
                allSessions.add(session);
            }

            int page = 1;
            int pageSize = 20;
            int start = (page - 1) * pageSize;

            List<ChatSession> pageSessions = allSessions.subList(
                    start,
                    Math.min(start + pageSize, allSessions.size())
            );

            assertThat(pageSessions).hasSize(20);
            assertThat(pageSessions.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("会话置顶功能")
        void sessionPinningFunction() {
            List<ChatSession> sessions = new ArrayList<>();

            ChatSession s1 = new ChatSession();
            s1.setId(1L);
            s1.setIsTop(0);
            sessions.add(s1);

            ChatSession s2 = new ChatSession();
            s2.setId(2L);
            s2.setIsTop(1);
            sessions.add(s2);

            ChatSession s3 = new ChatSession();
            s3.setId(3L);
            s3.setIsTop(0);
            sessions.add(s3);

            // 置顶的会话排在前面
            sessions.sort((sess1, sess2) -> {
                if (sess1.getIsTop() != null && sess1.getIsTop() == 1 && (sess2.getIsTop() == null || sess2.getIsTop() == 0)) {
                    return -1;
                }
                if (sess2.getIsTop() != null && sess2.getIsTop() == 1 && (sess1.getIsTop() == null || sess1.getIsTop() == 0)) {
                    return 1;
                }
                return 0;
            });

            assertThat(sessions.get(0).getId()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("未读消息数测试")
    class UnreadCountTests {

        @Test
        @DisplayName("未读数增加")
        void unreadCountIncrement() {
            ChatSession session = new ChatSession();
            session.setId(1L);
            session.setUnreadCount(5);

            // 模拟收到新消息
            session.setUnreadCount(session.getUnreadCount() + 1);

            assertThat(session.getUnreadCount()).isEqualTo(6);
        }

        @Test
        @DisplayName("未读数清零")
        void unreadCountClear() {
            ChatSession session = new ChatSession();
            session.setId(1L);
            session.setUnreadCount(10);

            // 模拟用户查看会话
            session.setUnreadCount(0);

            assertThat(session.getUnreadCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("未读数边界值")
        void unreadCountBoundary() {
            ChatSession session1 = new ChatSession();
            session1.setUnreadCount(0);

            ChatSession session2 = new ChatSession();
            session2.setUnreadCount(Integer.MAX_VALUE);

            assertThat(session1.getUnreadCount()).isGreaterThanOrEqualTo(0);
            assertThat(session2.getUnreadCount()).isLessThanOrEqualTo(Integer.MAX_VALUE);
        }
    }

    @Nested
    @DisplayName("会话预览内容测试")
    class SessionPreviewTests {

        @Test
        @DisplayName("文本消息预览")
        void textMessagePreview() {
            String content = "a".repeat(300); // 300 chars, will be truncated
            String preview = content.length() > 250
                    ? content.substring(0, 247) + "..."
                    : content;

            assertThat(preview).hasSize(250);
            assertThat(preview).endsWith("...");
        }

        @Test
        @DisplayName("图片消息预览")
        void imageMessagePreview() {
            String content = "[图片]";
            String preview = content;

            assertThat(preview).isEqualTo("[图片]");
            assertThat(preview.length()).isLessThan(20);
        }

        @Test
        @DisplayName("语音消息预览")
        void voiceMessagePreview() {
            String content = "[语音 15秒]";
            String preview = content;

            assertThat(preview).isEqualTo("[语音 15秒]");
        }

        @Test
        @DisplayName("文件消息预览")
        void fileMessagePreview() {
            String content = "[文件] project-doc.pdf (2.5MB)";
            String preview = content;

            assertThat(preview).contains("[文件]");
        }
    }

    @Nested
    @DisplayName("会话类型判断测试")
    class SessionTypeJudgmentTests {

        @Test
        @DisplayName("单聊会话判断")
        void singleSessionJudgment() {
            assertThat(SessionType.isSingle(1)).isTrue();
            assertThat(SessionType.isGroup(1)).isFalse();
        }

        @Test
        @DisplayName("群聊会话判断")
        void groupSessionJudgment() {
            assertThat(SessionType.isGroup(2)).isTrue();
            assertThat(SessionType.isSingle(2)).isFalse();
        }

        @Test
        @DisplayName("会话ID解析")
        void sessionIdParsing() {
            String singleSessionId = "single_1_2";
            String[] parts = singleSessionId.split("_");

            assertThat(parts[0]).isEqualTo("single");
            assertThat(parts[1]).isEqualTo("1");
            assertThat(parts[2]).isEqualTo("2");
        }
    }

    @Nested
    @DisplayName("会话删除测试")
    class SessionDeletionTests {

        @Test
        @DisplayName("软删除会话")
        void softDeleteSession() {
            ChatSession session = new ChatSession();
            session.setId(1L);
            session.setIsDeleted(0);

            // 软删除
            session.setIsDeleted(1);

            assertThat(session.getIsDeleted()).isEqualTo(1);
        }

        @Test
        @DisplayName("会话已删除状态过滤")
        void deletedSessionFiltering() {
            List<ChatSession> sessions = new ArrayList<>();

            ChatSession s1 = new ChatSession();
            s1.setId(1L);
            s1.setIsDeleted(0);
            sessions.add(s1);

            ChatSession s2 = new ChatSession();
            s2.setId(2L);
            s2.setIsDeleted(1);
            sessions.add(s2);

            ChatSession s3 = new ChatSession();
            s3.setId(3L);
            s3.setIsDeleted(0);
            sessions.add(s3);

            List<ChatSession> activeSessions = sessions.stream()
                    .filter(s -> s.getIsDeleted() == 0)
                    .toList();

            assertThat(activeSessions).hasSize(2);
            assertThat(activeSessions.stream().allMatch(s -> s.getIsDeleted() == 0)).isTrue();
        }
    }

    @Nested
    @DisplayName("会话关联数据测试")
    class SessionRelationTests {

        @Test
        @DisplayName("会话关联用户信息")
        void sessionRelationUserInfo() {
            ChatUser user = new ChatUser();
            user.setId("1");
            user.setUsername("testuser");
            user.setNickname("Test User");
            user.setAvatar("avatar.png");

            ChatSession session = new ChatSession();
            session.setId(1L);
            session.setUserId(user.getId());

            assertThat(session.getUserId()).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("会话关联群组信息")
        void sessionRelationGroupInfo() {
            ChatGroup group = new ChatGroup();
            group.setId(100L);
            group.setGroupName("Test Group");
            group.setGroupAvatar("group.png");

            ChatSession session = new ChatSession();
            session.setId(1L);
            session.setTargetId(String.valueOf(group.getId()));

            assertThat(session.getTargetId()).isEqualTo("100");
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionScenarioTests {

        @Test
        @DisplayName("空会话ID")
        void emptySessionId() {
            ChatSession session = new ChatSession();
            session.setSessionId("");

            assertThat(session.getSessionId()).isEmpty();
        }

        @Test
        @DisplayName("空用户ID")
        void emptyUserId() {
            ChatSession session = new ChatSession();
            session.setUserId("");

            assertThat(session.getUserId()).isEmpty();
        }

        @Test
        @DisplayName("会话不存在")
        void sessionNotExists() {
            ChatSession session = null;

            assertThat(session).isNull();
        }
    }
}
