package com.maxxvll.common.constants;

/**
 * 语音通话信令类型常量
 *
 * @author maxxvll
 * @since 2026-03-16
 */
public final class VoiceCallConstants {

    private VoiceCallConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 信令类型 ====================

    /**
     * 信令类型
     */
    public static final class CallType {
        /** 发起呼叫 */
        public static final String CALL = "1";
        /** 接听 */
        public static final String ANSWER = "2";
        /** 拒绝 */
        public static final String REJECT = "3";
        /** 挂断 */
        public static final String HANGUP = "4";
        /** SDP交换 */
        public static final String SDP = "5";
        /** ICE交换 */
        public static final String ICE = "6";

        private CallType() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }

        public static String getCallTypeName(String callType) {
            if (callType == null) {
                return "未知";
            }
            return switch (callType) {
                case CALL -> "发起呼叫";
                case ANSWER -> "接听";
                case REJECT -> "拒绝";
                case HANGUP -> "挂断";
                case SDP -> "SDP交换";
                case ICE -> "ICE交换";
                default -> "未知";
            };
        }

        public static boolean isCall(String callType) {
            return CALL.equals(callType);
        }

        public static boolean isAnswer(String callType) {
            return ANSWER.equals(callType);
        }

        public static boolean isHangup(String callType) {
            return HANGUP.equals(callType) || REJECT.equals(callType);
        }
    }

    // ==================== 通话模式 ====================

    /**
     * 通话模式
     */
    public static final class Mode {
        /** 语音通话 */
        public static final String AUDIO = "audio";
        /** 视频通话 */
        public static final String VIDEO = "video";

        private Mode() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }

        public static boolean isAudio(String mode) {
            return AUDIO.equals(mode);
        }

        public static boolean isVideo(String mode) {
            return VIDEO.equals(mode);
        }
    }

    // ==================== 媒体传输方式 ====================

    /**
     * 媒体传输方式
     */
    public static final class Transport {
        /** WebRTC */
        public static final String WEBRTC = "webrtc";
        /** 流媒体 */
        public static final String STREAM = "stream";

        private Transport() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }

        public static boolean isWebRtc(String transport) {
            return WEBRTC.equals(transport);
        }
    }

    // ==================== 业务限制 ====================

    /**
     * 通话扩展信息最大长度
     * @deprecated 请使用 {@link BusinessConstants#CALL_EXTRA_MAX_LENGTH}
     */
    @Deprecated
    public static final int CALL_EXTRA_MAX_LENGTH = BusinessConstants.CALL_EXTRA_MAX_LENGTH;

    /**
     * 通话超时时间（秒）
     * @deprecated 请使用 {@link BusinessConstants#CALL_TIMEOUT_SECONDS}
     */
    @Deprecated
    public static final int CALL_TIMEOUT_SECONDS = BusinessConstants.CALL_TIMEOUT_SECONDS;
}
