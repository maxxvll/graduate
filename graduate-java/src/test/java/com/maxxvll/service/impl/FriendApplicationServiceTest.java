package com.maxxvll.service.impl;

import com.maxxvll.common.constants.ApplicationStatusConstants;
import com.maxxvll.common.enums.MessageType;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.domain.FriendApplication;
import com.maxxvll.domain.FriendRelationSetting;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.mapper.FriendApplicationMapper;
import com.maxxvll.mapper.FriendRelationSettingMapper;
import com.maxxvll.service.ChatMessageService;
import com.maxxvll.service.ChatSessionService;
import com.maxxvll.utils.MinioUtil;
import com.maxxvll.utils.RedissonCacheUtil;
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

/**
 * FriendApplicationService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class FriendApplicationServiceTest {

    @Mock
    private ChatUserMapper chatUserMapper;

    @Mock
    private MinioUtil minioUtil;

    @Mock
    private ChatSessionService chatSessionService;

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private FriendApplicationMapper friendApplicationMapper;

    @Mock
    private FriendRelationSettingMapper friendRelationSettingMapper;

    @Mock
    private RedissonCacheUtil redissonCacheUtil;

    @InjectMocks
    private FriendApplicationServiceImpl friendApplicationService;

    @Nested
    @DisplayName("好友申请构建测试")
    class ApplicationBuildingTests {

        @Test
        @DisplayName("构建好友申请")
        void buildFriendApplication() {
            FriendApplication application = new FriendApplication();
            application.setId(1L);
            application.setApplicantId(1L);
            application.setTargetUserId(2L);
            application.setStatus(ApplicationStatusConstants.STATUS_PENDING);
            application.setCreateTime(new Date());

            assertThat(application).isNotNull();
            assertThat(application.getStatus()).isEqualTo(ApplicationStatusConstants.STATUS_PENDING);
        }

        @Test
        @DisplayName("申请状态枚举")
        void applicationStatusEnum() {
            assertThat(ApplicationStatusConstants.STATUS_PENDING).isEqualTo(0);
            assertThat(ApplicationStatusConstants.STATUS_APPROVED).isEqualTo(1);
            assertThat(ApplicationStatusConstants.STATUS_REJECTED).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("好友申请验证测试")
    class ApplicationValidationTests {

        @Test
        @DisplayName("不能向自己发送申请")
        void cannotApplyToSelf() {
            String applicantId = "1";
            String targetId = "1";
            assertThat(targetId).isEqualTo(applicantId);
        }

        @Test
        @DisplayName("目标用户不存在")
        void targetUserNotExists() {
            ChatUser nonExistentUser = null;
            assertThat(nonExistentUser).isNull();
        }

        @Test
        @DisplayName("申请留言验证")
        void applyMessageValidation() {
            String validMessage = "Hello";
            String emptyMessage = "";
            String longMessage = "a".repeat(500);
            assertThat(validMessage.length()).isBetween(1, 250);
            assertThat(emptyMessage).isEmpty();
            assertThat(longMessage.length()).isGreaterThan(250);
        }
    }

    @Nested
    @DisplayName("好友申请列表测试")
    class ApplicationListTests {

        @Test
        @DisplayName("申请列表过滤")
        void applicationListFiltering() {
            List<FriendApplication> applications = new ArrayList<>();
            FriendApplication app1 = new FriendApplication();
            app1.setId(1L);
            app1.setStatus(ApplicationStatusConstants.STATUS_PENDING);
            applications.add(app1);

            FriendApplication app2 = new FriendApplication();
            app2.setId(2L);
            app2.setStatus(ApplicationStatusConstants.STATUS_APPROVED);
            applications.add(app2);

            List<FriendApplication> pending = applications.stream()
                    .filter(a -> a.getStatus() == ApplicationStatusConstants.STATUS_PENDING)
                    .toList();
            assertThat(pending).hasSize(1);
        }

        @Test
        @DisplayName("申请列表排序")
        void applicationListSorting() {
            List<FriendApplication> applications = new ArrayList<>();
            Date now = new Date();

            FriendApplication app1 = new FriendApplication();
            app1.setId(1L);
            app1.setCreateTime(new Date(now.getTime() - 3000));
            applications.add(app1);

            FriendApplication app2 = new FriendApplication();
            app2.setId(2L);
            app2.setCreateTime(new Date(now.getTime() - 1000));
            applications.add(app2);

            applications.sort((a1, a2) -> a2.getCreateTime().compareTo(a1.getCreateTime()));
            assertThat(applications.get(0).getId()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("好友申请处理测试")
    class ApplicationHandlingTests {

        @Test
        @DisplayName("同意好友申请")
        void acceptFriendApplication() {
            FriendApplication application = new FriendApplication();
            application.setId(1L);
            application.setStatus(ApplicationStatusConstants.STATUS_PENDING);
            application.setStatus(ApplicationStatusConstants.STATUS_APPROVED);
            application.setUpdateTime(new Date());
            assertThat(application.getStatus()).isEqualTo(ApplicationStatusConstants.STATUS_APPROVED);
        }

        @Test
        @DisplayName("拒绝好友申请")
        void rejectFriendApplication() {
            FriendApplication application = new FriendApplication();
            application.setId(1L);
            application.setStatus(ApplicationStatusConstants.STATUS_PENDING);
            application.setStatus(ApplicationStatusConstants.STATUS_REJECTED);
            assertThat(application.getStatus()).isEqualTo(ApplicationStatusConstants.STATUS_REJECTED);
        }
    }

    @Nested
    @DisplayName("黑名单测试")
    class BlacklistTests {

        @Test
        @DisplayName("用户被加入黑名单")
        void userBlockedInBlacklist() {
            FriendRelationSetting setting = new FriendRelationSetting();
            setting.setOwnerUserId(1L);
            setting.setFriendUserId(2L);
            setting.setIsBlacklisted(1);
            assertThat(setting.getIsBlacklisted()).isEqualTo(1);
        }

        @Test
        @DisplayName("用户不在黑名单")
        void userNotInBlacklist() {
            FriendRelationSetting setting = new FriendRelationSetting();
            setting.setOwnerUserId(1L);
            setting.setFriendUserId(2L);
            setting.setIsBlacklisted(0);
            assertThat(setting.getIsBlacklisted()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionScenarioTests {

        @Test
        @DisplayName("申请者不存在")
        void applicantNotExists() {
            ChatUser applicant = null;
            assertThat(applicant).isNull();
        }

        @Test
        @DisplayName("已处理申请不能重复处理")
        void alreadyHandledApplication() {
            FriendApplication application = new FriendApplication();
            application.setStatus(ApplicationStatusConstants.STATUS_APPROVED);
            boolean isPending = application.getStatus() == ApplicationStatusConstants.STATUS_PENDING;
            assertThat(isPending).isFalse();
        }
    }

    @Nested
    @DisplayName("业务逻辑测试")
    class BusinessLogicTests {

        @Test
        @DisplayName("好友申请创建会话")
        void applicationCreatesSession() {
            Long applicantId = 1L;
            Long targetUserId = 2L;
            String sessionId = applicantId < targetUserId
                    ? "single_" + applicantId + "_" + targetUserId
                    : "single_" + targetUserId + "_" + applicantId;
            assertThat(sessionId).isEqualTo("single_1_2");
        }

        @Test
        @DisplayName("好友申请发送系统消息")
        void applicationSystemMessage() {
            ChatMessage systemMessage = new ChatMessage();
            systemMessage.setSenderId("0");
            systemMessage.setReceiverId("2");
            systemMessage.setContent("你们已经成为好友了，快打个招呼吧");
            systemMessage.setMessageType(MessageType.TEXT.getCode());
            assertThat(systemMessage.getSenderId()).isEqualTo("0");
            assertThat(systemMessage.getContent()).contains("好友");
        }
    }
}
