package com.maxxvll.common.enums;

import lombok.Getter;

/**
 * 权限枚举
 *
 * <p>定义系统中所有的权限标识符</p>
 *
 * @author maxxvll
 * @since 2026-03-31
 */
@Getter
public enum Permission {

    // ==================== 用户管理权限 ====================
    USER_READ("user:read", "查看用户信息"),
    USER_WRITE("user:write", "编辑用户信息"),
    USER_DELETE("user:delete", "删除用户"),
    USER_BAN("user:ban", "禁用/启用用户"),

    // ==================== 好友权限 ====================
    FRIEND_READ("friend:read", "查看好友列表"),
    FRIEND_ADD("friend:add", "添加好友"),
    FRIEND_DELETE("friend:delete", "删除好友"),
    FRIEND_BLACKLIST("friend:blacklist", "拉黑/取消拉黑"),

    // ==================== 群组权限 ====================
    GROUP_READ("group:read", "查看群组"),
    GROUP_CREATE("group:create", "创建群组"),
    GROUP_UPDATE("group:update", "编辑群组信息"),
    GROUP_DELETE("group:delete", "解散群组"),
    GROUP_MEMBER_ADD("group:member:add", "添加群成员"),
    GROUP_MEMBER_REMOVE("group:member:remove", "移除群成员"),
    GROUP_MEMBER_UPDATE("group:member:update", "修改群成员角色"),
    GROUP_TRANSFER("group:transfer", "转让群主"),

    // ==================== 消息权限 ====================
    MESSAGE_SEND("message:send", "发送消息"),
    MESSAGE_READ("message:read", "读取消息"),
    MESSAGE_RECALL("message:recall", "撤回消息"),
    MESSAGE_DELETE("message:delete", "删除消息"),

    // ==================== 文件权限 ====================
    FILE_UPLOAD("file:upload", "上传文件"),
    FILE_DOWNLOAD("file:download", "下载文件"),
    FILE_DELETE("file:delete", "删除文件"),

    // ==================== 云盘权限 ====================
    CLOUD_READ("cloud:read", "查看云盘"),
    CLOUD_UPLOAD("cloud:upload", "上传到云盘"),
    CLOUD_DOWNLOAD("cloud:download", "从云盘下载"),
    CLOUD_DELETE("cloud:delete", "删除云盘文件"),
    CLOUD_SHARE("cloud:share", "分享云盘文件"),

    // ==================== 系统管理权限 ====================
    SYSTEM_CONFIG("system:config", "系统配置"),
    SYSTEM_LOG("system:log", "查看系统日志"),
    SYSTEM_USER_MANAGE("system:user:manage", "管理系统用户"),

    // ==================== 语音通话权限 ====================
    VOICE_CALL("voice:call", "发起语音通话"),
    VIDEO_CALL("video:call", "发起视频通话");

    private final String code;
    private final String desc;

    Permission(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据权限码获取枚举
     */
    public static Permission getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (Permission permission : values()) {
            if (permission.getCode().equals(code)) {
                return permission;
            }
        }
        return null;
    }

    /**
     * 是否为系统管理权限
     */
    public boolean isSystemPermission() {
        return this.code.startsWith("system:");
    }

    /**
     * 是否为管理权限（管理员及以上）
     */
    public boolean isAdminPermission() {
        return this == SYSTEM_CONFIG || this == SYSTEM_LOG ||
               this == SYSTEM_USER_MANAGE || this == USER_BAN ||
               this == USER_DELETE;
    }
}
