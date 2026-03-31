package com.maxxvll.common.constants;

/**
 * 群成员状态常量
 *
 * @author maxxvll
 * @since 2026-03-16
 */
public final class MemberConstants {

    private MemberConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 退出状态
     */
    public static final class QuitStatus {
        /** 未退出 */
        public static final Integer ACTIVE = 0;
        /** 已退出 */
        public static final Integer QUIT = 1;

        private QuitStatus() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }

        public static boolean isActive(Integer status) {
            return ACTIVE.equals(status);
        }

        public static boolean isQuit(Integer status) {
            return QUIT.equals(status);
        }
    }

    /**
     * 免打扰状态
     */
    public static final class MuteStatus {
        /** 未免打扰 */
        public static final Integer UNMUTED = 0;
        /** 已免打扰 */
        public static final Integer MUTED = 1;

        private MuteStatus() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }

        public static boolean isMuted(Integer status) {
            return MUTED.equals(status);
        }
    }

    /**
     * 置顶状态
     */
    public static final class TopStatus {
        /** 未置顶 */
        public static final Integer NOT_TOP = 0;
        /** 已置顶 */
        public static final Integer TOP = 1;

        private TopStatus() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }

        public static boolean isTop(Integer status) {
            return TOP.equals(status);
        }
    }
}
