package com.maxxvll.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 好友申请事件
 * <p>
 * 用于 Kafka 异步处理好友申请相关的通知，包括：
 * - 申请创建通知（通知目标用户）
 * - 申请通过通知（通知申请人）
 * - 申请拒绝通知（通知申请人）
 * </p>
 *
 * @author Claude Code
 * @since 2026-03-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FriendApplicationEvent extends BaseKafkaEvent {
    private static final long serialVersionUID = 1L;

    /**
     * 申请ID
     */
    private Long applicationId;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 申请人用户名
     */
    private String applicantUsername;

    /**
     * 申请人头像
     */
    private String applicantAvatar;

    /**
     * 目标用户ID
     */
    private Long targetUserId;

    /**
     * 目标用户用户名
     */
    private String targetUsername;

    /**
     * 申请备注
     */
    private String remark;

    /**
     * 事件动作类型
     */
    private ActionType actionType;

    /**
     * 申请状态（1-通过，2-拒绝）
     */
    private Integer status;

    /**
     * 拒绝原因（仅拒绝时有值）
     */
    private String rejectReason;

    /**
     * 会话ID（通过后创建的会话）
     */
    private String sessionId;

    /**
     * 申请创建时间
     */
    private Date createTime;

    @Override
    public String getEventType() {
        return "FRIEND_APPLICATION";
    }

    @Override
    public String getEventDescription() {
        return String.format("[%s] eventId=%s, type=%s, action=%s, applicantId=%d, targetUserId=%d",
            getSource(), getEventId(), getEventType(), actionType,
            applicantId, targetUserId);
    }

    /**
     * 事件动作类型
     */
    public enum ActionType {
        /**
         * 创建申请
         */
        APPLY,

        /**
         * 接受申请
         */
        ACCEPT,

        /**
         * 拒绝申请
         */
        REJECT
    }
}
