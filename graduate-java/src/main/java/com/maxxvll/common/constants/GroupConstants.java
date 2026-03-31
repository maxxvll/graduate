package com.maxxvll.common.constants;

/**
 * 群聊相关常量
 *
 * @author maxxvll
 * @since 2026-03-16
 */
public final class GroupConstants {

    private GroupConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 群成员角色 ====================

    /**
     * 群成员角色
     */
    public static final class Role {
        /** 群主 */
        public static final int OWNER = 1;
        /** 管理员 */
        public static final int ADMIN = 2;
        /** 普通成员 */
        public static final int MEMBER = 3;

        private Role() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }

        public static String getRoleName(Integer role) {
            if (role == null) {
                return "未知";
            }
            return switch (role) {
                case OWNER -> "群主";
                case ADMIN -> "管理员";
                case MEMBER -> "成员";
                default -> "未知";
            };
        }

        /**
         * 是否为群主
         */
        public static boolean isOwner(Integer role) {
            return OWNER == role;
        }

        /**
         * 是否为管理员或群主
         */
        public static boolean isAdminOrOwner(Integer role) {
            return OWNER == role || ADMIN == role;
        }
    }

    // ==================== 加群方式 ====================

    /**
     * 加群方式
     */
    public static final class JoinType {
        /** 需审核 */
        public static final int NEED_APPROVAL = 1;
        /** 免审核 */
        public static final int AUTO_JOIN = 2;
        /** 仅邀请 */
        public static final int INVITE_ONLY = 3;

        private JoinType() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }

        public static String getJoinTypeName(Integer joinType) {
            if (joinType == null) {
                return "未知";
            }
            return switch (joinType) {
                case NEED_APPROVAL -> "需审核";
                case AUTO_JOIN -> "免审核";
                case INVITE_ONLY -> "仅邀请";
                default -> "未知";
            };
        }
    }

    // ==================== 群状态 ====================

    /**
     * 群状态
     */
    public static final class Status {
        /** 正常 */
        public static final int NORMAL = 1;
        /** 已解散 */
        public static final int DISSOLVED = 2;

        private Status() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    // ==================== 群成员数限制 ====================

    /**
     * 默认最大成员数
     * @deprecated 请使用 {@link BusinessConstants#DEFAULT_MAX_GROUP_MEMBERS}
     */
    @Deprecated
    public static final int DEFAULT_MAX_MEMBERS = BusinessConstants.DEFAULT_MAX_GROUP_MEMBERS;

    /**
     * 最大成员数上限
     * @deprecated 请使用 {@link BusinessConstants#MAX_GROUP_MEMBERS_LIMIT}
     */
    @Deprecated
    public static final int MAX_MEMBERS_LIMIT = BusinessConstants.MAX_GROUP_MEMBERS_LIMIT;

    /**
     * 创建群组最小成员数
     * @deprecated 请使用 {@link BusinessConstants#MIN_GROUP_MEMBERS}
     */
    @Deprecated
    public static final int MIN_MEMBERS = BusinessConstants.MIN_GROUP_MEMBERS;
}
