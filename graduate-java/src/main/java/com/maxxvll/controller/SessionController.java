package com.maxxvll.controller;

import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.vo.SessionVO;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/session")
public class SessionController extends BaseController {

    // 内存存储假数据（实际项目中请查询数据库）
    private static final List<SessionVO> mockSessions = new ArrayList<>();

    // 初始化假数据
    static {
        // 假数据：用户ID（假设当前用户是 "user_1"）
        String currentUserId = "user_1";

        // 1. 单聊会话 - 苏珊
        mockSessions.add(SessionVO.builder()
                .sessionId("user_1_user_2")
                .sessionType(1)
                .targetId("user_2")
                .sessionName("tom")
                .sessionAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=Susan")
                .lastMessageContent("所以你是谁")
                .lastMessageTime(LocalDateTime.now().minusMinutes(30))
                .lastMessageSenderId("user_2")
                .lastMessageSenderName("tom")
                .unreadCount(0)
                .isTop(false)
                .isMute(false)
                .build());

        // 2. 单聊会话 - 老罗编程学习
        mockSessions.add(SessionVO.builder()
                .sessionId("user_1_user_3")
                .sessionType(1)
                .targetId("user_3")
                .sessionName("老罗编程学习")
                .sessionAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=Luo")
                .lastMessageContent("[图片]")
                .lastMessageTime(LocalDateTime.now().minusHours(1))
                .lastMessageSenderId("user_1")
                .lastMessageSenderName("我")
                .unreadCount(2)
                .isTop(false)
                .isMute(false)
                .build());

        // 3. 群聊会话 - 微信游戏
        mockSessions.add(SessionVO.builder()
                .sessionId("group_group_1001")
                .sessionType(2)
                .targetId("group_1001")
                .sessionName("微信游戏")
                .sessionAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=GameGroup")
                .lastMessageContent("[12条] 《穿越火线》新版本上线")
                .lastMessageTime(LocalDateTime.now().minusHours(2))
                .lastMessageSenderId("system")
                .lastMessageSenderName("系统通知")
                .unreadCount(12)
                .isTop(true)
                .isMute(false)
                .build());

        // 4. 单聊会话 - 公众号
        mockSessions.add(SessionVO.builder()
                .sessionId("user_1_official_1")
                .sessionType(1)
                .targetId("official_1")
                .sessionName("公众号")
                .sessionAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=Official")
                .lastMessageContent("[20条] 中国反邪教：警惕新型诈骗")
                .lastMessageTime(LocalDateTime.now().minusDays(1))
                .lastMessageSenderId("official_1")
                .lastMessageSenderName("公众号")
                .unreadCount(20)
                .isTop(false)
                .isMute(true)
                .build());

        // 5. 群聊会话 - 技术交流群
        mockSessions.add(SessionVO.builder()
                .sessionId("group_group_1002")
                .sessionType(2)
                .targetId("group_1002")
                .sessionName("只聊技术，吹水踢群")
                .sessionAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=TechGroup")
                .lastMessageContent("fan: 其实我是变形金刚...")
                .lastMessageTime(LocalDateTime.now().minusDays(1))
                .lastMessageSenderId("user_5")
                .lastMessageSenderName("fan")
                .unreadCount(99)
                .isTop(false)
                .isMute(false)
                .build());

        // 6. 单聊会话 - 肖阳
        mockSessions.add(SessionVO.builder()
                .sessionId("user_1_user_6")
                .sessionType(1)
                .targetId("user_6")
                .sessionName("Tom")
                .sessionAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=man")
                .lastMessageContent("确实")
                .lastMessageTime(LocalDateTime.now().minusDays(1))
                .lastMessageSenderId("user_6")
                .lastMessageSenderName("肖阳")
                .unreadCount(0)
                .isTop(false)
                .isMute(false)
                .build());
    }

    /**
     * 获取当前用户的会话列表
     */
    @GetMapping("/list")
    public Result<List<SessionVO>> getSessionList() {
        // 模拟：从数据库查询当前用户的会话列表
        // 实际项目中：String currentUserId = getCurrentUserId();

        // 排序逻辑：置顶的排在前面，然后按最后消息时间倒序
        List<SessionVO> sortedList = mockSessions.stream()
                .sorted(Comparator.comparing(SessionVO::getIsTop).reversed()
                        .thenComparing(SessionVO::getLastMessageTime).reversed())
                .collect(Collectors.toList());

        return Result.success(sortedList);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{sessionId}")
    public Result<Boolean> deleteSession(@PathVariable String sessionId) {
        mockSessions.removeIf(session -> session.getSessionId().equals(sessionId));
        return Result.success(true);
    }

    /**
     * 置顶/取消置顶会话
     */
    @PutMapping("/top/{sessionId}")
    public Result<Boolean> toggleTop(@PathVariable String sessionId, @RequestParam Boolean isTop) {
        mockSessions.stream()
                .filter(session -> session.getSessionId().equals(sessionId))
                .findFirst()
                .ifPresent(session -> session.setIsTop(isTop));
        return Result.success(true);
    }

    /**
     * 免打扰/取消免打扰
     */
    @PutMapping("/mute/{sessionId}")
    public Result<Boolean> toggleMute(@PathVariable String sessionId, @RequestParam Boolean isMute) {
        mockSessions.stream()
                .filter(session -> session.getSessionId().equals(sessionId))
                .findFirst()
                .ifPresent(session -> session.setIsMute(isMute));
        return Result.success(true);
    }

    /**
     * 清除未读消息数
     */
    @PutMapping("/read/{sessionId}")
    public Result<Boolean> clearUnread(@PathVariable String sessionId) {
        mockSessions.stream()
                .filter(session -> session.getSessionId().equals(sessionId))
                .findFirst()
                .ifPresent(session -> session.setUnreadCount(0));
        return Result.success(true);
    }
    /**
     * 更新会话最后一条消息（消息发送成功后调用）
     */
    @PutMapping("/update-last-msg")
    public Result<Boolean> updateSessionLastMsg(
            @RequestParam String sessionId,
            @RequestParam String content,
            @RequestParam String senderId,
            @RequestParam String senderName
    ) {
        mockSessions.stream()
                .filter(session -> session.getSessionId().equals(sessionId))
                .findFirst()
                .ifPresent(session -> {
                    session.setLastMessageContent(content);
                    session.setLastMessageTime(LocalDateTime.now());
                    session.setLastMessageSenderId(senderId);
                    session.setLastMessageSenderName(senderName);
                    // 如果不是自己发的，增加未读
                    if (!senderId.equals("user_1")) {
                        session.setUnreadCount(session.getUnreadCount() + 1);
                    }
                });
        return Result.success(true);
    }
}