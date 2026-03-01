create table chat_group
(
    id           bigint unsigned                        not null comment '群ID（主键，雪花算法生成，对应消息表receiver_id、会话表target_id）'
        primary key,
    group_name   varchar(128)                           not null comment '群名称（聊天列表展示）',
    group_avatar varchar(256)                           null comment '群头像URL',
    creator_id   bigint unsigned                    not null comment '创建人ID（关联chat_user.id）',
    max_member   int unsigned default '200'             not null comment '群最大成员数（默认200，可调整）',
    join_type    tinyint      default 1                 not null comment '加群方式：1-需审核，2-免审核，3-仅邀请',
    notice       text                                   null comment '群公告',
    is_mute_all  tinyint      default 0                 not null comment '是否全员禁言：0-否，1-是',
    status       tinyint      default 1                 not null comment '群状态：1-正常，2-解散，3-封禁',
    ext_info     json                                   null comment '扩展字段（如群标签、创建原因等）',
    created_at   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '群聊基础信息表' collate = utf8mb4_unicode_ci;

create index idx_creator_id
    on chat_group (creator_id)
    comment '查询用户创建的所有群';

create index idx_status
    on chat_group (status)
    comment '筛选正常/解散的群';

create table chat_group_member
(
    id         bigint unsigned auto_increment comment '主键ID'
        primary key,
    group_id   bigint unsigned                    not null comment '群ID（关联chat_group.id）',
    user_id    varchar(64)                        not null comment '成员ID（关联chat_user.id）',
    role       tinyint  default 3                 not null comment '成员角色：1-群主，2-管理员，3-普通成员',
    join_time  datetime default CURRENT_TIMESTAMP not null comment '加入时间',
    inviter_id varchar(64)                        null comment '邀请人ID（非邀请加入则为空）',
    is_mute    tinyint  default 0                 not null comment '是否被禁言：0-否，1-是（仅群主/管理员可设置）',
    is_quit    tinyint  default 0                 not null comment '是否退出：0-未退出，1-已退出',
    quit_time  datetime                           null comment '退出时间（未退出则为空）',
    quit_reason varchar(255)                      null comment '移除原因（管理员/群主移除成员时填写）',
    created_at datetime default CURRENT_TIMESTAMP not null comment '记录创建时间',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '记录更新时间',
    constraint uk_group_user
        unique (group_id, user_id) comment '一个用户在一个群仅一条记录'
)
    comment '群成员关联表（核心多对多）' collate = utf8mb4_unicode_ci;

create index idx_group_role
    on chat_group_member (group_id, role)
    comment '查询群内群主/管理员';

create index idx_user_id
    on chat_group_member (user_id)
    comment '查询用户加入的所有群';

create table chat_message
(
    id               bigint unsigned auto_increment comment '消息唯一主键ID（自增）'
        primary key,
    message_no       varchar(64)                        not null comment '消息唯一业务编号（客户端生成，用于去重，避免重复发送）',
    session_id       varchar(64)                        not null comment '会话ID（单聊：小ID_大ID；群聊：group_群ID）',
    session_type     tinyint                            not null comment '会话类型：1-单聊，2-群聊（核心区分场景）',
    sender_id        varchar(64)                        not null comment '发信人ID（用户ID/系统标识）',
    receiver_id      varchar(64)                        null comment '接收人ID（单聊必填，群聊为空）',
    message_type     tinyint                            not null comment '消息类型：1-文本，2-图片，3-视频，4-音频，5-文件，6-表情，7-系统通知',
    content          text                               null comment '原始消息内容（文本消息存内容，多媒体存占位符如【图片】）',
    content_replaced text                               null comment '撤回/敏感消息的替换内容（如[消息已撤回]）',
    at_user_ids      json                               null comment '被@的用户ID列表（JSON格式，如["1001","1002"]）',
    is_at_all        tinyint  default 0                 not null comment '是否@所有人：0-否，1-是（群聊核心需求）',
    quote_message_id bigint unsigned                    null comment '引用/回复的原消息ID（高频需求）',
    file_url         text                               null comment '多媒体文件URL（OSS/服务器地址）',
    file_name        varchar(255)                       null comment '文件原始名称（如截图2026.png）',
    file_size        bigint unsigned                    null comment '文件大小（字节）',
    file_type        varchar(32)                        null comment '文件MIME类型（如image/jpeg）',
    duration         int unsigned                       null comment '语音/视频时长（秒），仅音频/视频消息有效',
    thumbnail_url    varchar(256)                       null comment '图片/视频缩略图URL（优化加载）',
    file_expired     tinyint  default 0                 not null comment '文件是否过期：0-有效，1-过期（避免加载失效文件）',
    send_time        datetime default CURRENT_TIMESTAMP not null comment '消息发送时间',
    revoke_time      datetime                           null comment '撤回时间（非撤回消息为空）',
    status           tinyint  default 1                 not null comment '核心状态：1-发送成功，2-已读，3-已撤回，4-已删除，5-发送失败',
    operator_id      varchar(64)                        null comment '消息操作人ID（如管理员撤回消息时填管理员ID）',
    ext_info         json                               null comment '轻量扩展字段（存储小众需求，避免加字段）',
    is_sensitive     tinyint  default 0                 not null comment '是否敏感消息：0-否，1-是',
    is_deleted       tinyint  default 0                 not null comment '软删除标识：0-未删除，1-已删除',
    created_at       datetime default CURRENT_TIMESTAMP not null comment '记录创建时间',
    updated_at       datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '记录更新时间',
    constraint uk_message_no
        unique (message_no) comment '唯一业务编号，防止重复消息'
)
    comment '聊天消息记录表（精简版）' collate = utf8mb4_unicode_ci;

create index idx_sender_receiver
    on chat_message (sender_id, receiver_id)
    comment '按收发方筛选消息';

create index idx_session_time
    on chat_message (session_id, send_time)
    comment '按会话查历史消息（最核心）';

create table chat_session
(
    id                     bigint unsigned auto_increment comment '会话记录主键ID'
        primary key,
    session_id             varchar(64)                            not null comment '会话ID（和消息表一致：单聊=小ID_大ID；群聊=group_群ID）',
    session_type           tinyint                                not null comment '会话类型（和消息表一致：1-单聊，2-群聊）',
    user_id                varchar(64)                            not null comment '【核心】所属用户ID（每个用户的每个会话都有一条记录）',
    target_id              varchar(64)                            not null comment '会话对方ID：单聊=对方用户ID；群聊=群ID',
    session_name           varchar(128)                           not null comment '会话名称：单聊=对方昵称；群聊=群名称（避免频繁查用户/群表）',
    session_avatar         varchar(256)                           null comment '会话头像：单聊=对方头像；群聊=群头像',
    last_message_id        bigint unsigned                        null comment '最后一条消息ID（关联消息表id）',
    last_message_content   varchar(256)                           null comment '最后一条消息内容（缩略版，如“[图片]”“你好”）',
    last_message_time      datetime                               null comment '最后一条消息发送时间',
    last_message_sender_id varchar(64)                            null comment '最后一条消息发送人ID（用于显示“XX：XXX”）',
    unread_count           int unsigned default '0'               not null comment '未读消息数（用户未读的消息数量）',
    is_top                 tinyint      default 0                 not null comment '是否置顶：0-否，1-是',
    is_mute                tinyint      default 0                 not null comment '是否免打扰：0-否，1-是',
    is_hide                tinyint      default 0                 not null comment '是否隐藏会话：0-否，1-是',
    is_deleted             tinyint      default 0                 not null comment '软删除标识：0-未删除，1-已删除',
    created_at             datetime     default CURRENT_TIMESTAMP not null comment '会话创建时间',
    updated_at             datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '会话更新时间',
    constraint uk_user_session
        unique (user_id, session_id) comment '一个用户的一个会话仅一条记录'
)
    comment '聊天会话表（支撑聊天列表）' collate = utf8mb4_unicode_ci;

create index idx_last_message_time
    on chat_session (user_id, last_message_time)
    comment '按用户+最后消息时间排序（聊天列表默认排序）';

create index idx_session_id
    on chat_session (session_id)
    comment '按会话ID关联消息表';

create table chat_user
(
    id         varchar(64)                        not null comment '用户唯一ID（主键，建议用雪花ID/UUID，避免自增ID泄露信息）'
        primary key,
    username   varchar(64)                        not null comment '用户名（登录用，唯一）',
    nickname   varchar(64)                        not null comment '用户昵称（聊天展示用）',
    avatar     varchar(256)                       null comment '用户头像URL（OSS/服务器地址）',
    phone      varchar(20)                        null comment '手机号（可选，用于登录/验证）',
    email      varchar(128)                       null comment '邮箱（可选）',
    password   varchar(128)                       not null comment '密码（加密存储，如BCrypt哈希）',
    status     tinyint  default 1                 not null comment '用户状态：1-正常，2-禁用，3-注销',
    ext_info   json                               null comment '扩展字段（如性别、签名等小众信息）',
    created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_phone
        unique (phone) comment '手机号唯一（如有）',
    constraint uk_username
        unique (username) comment '用户名唯一，避免重复注册'
)
    comment '用户基础信息表' collate = utf8mb4_unicode_ci;

create index idx_status
    on chat_user (status)
    comment '筛选正常/禁用用户';

create table friend_application
(
    id             bigint auto_increment comment '主键ID'
        primary key,
    applicant_id   bigint                             not null comment '申请人ID（关联用户表user.id）',
    target_user_id bigint                             not null comment '被申请人ID（关联用户表user.id）',
    status         tinyint  default 0                 not null comment '申请状态：0-待处理 1-已通过 2-已拒绝',
    reject_reason  varchar(255)                       null comment '拒绝原因（仅状态为2时填写）',
    create_time    datetime default CURRENT_TIMESTAMP not null comment '申请创建时间',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '申请更新时间',
    constraint uk_applicant_target
        unique (applicant_id, target_user_id)
)
    comment '好友申请表';

create table group_application
(
    id            bigint auto_increment comment '主键ID'
        primary key,
    applicant_id  bigint                             not null comment '申请人ID（关联用户表user.id）',
    group_id      bigint                             not null comment '目标群聊ID（关联群表group.id）',
    status        tinyint  default 0                 not null comment '申请状态：0-待处理 1-已通过 2-已拒绝',
    reject_reason varchar(255)                       null comment '拒绝原因（仅状态为2时填写）',
    operator_id   bigint                             null comment '操作人ID（处理申请的群主/管理员，关联用户表user.id，状态为0时为NULL）',
    create_time   datetime default CURRENT_TIMESTAMP not null comment '申请创建时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '申请更新时间',
    constraint uk_applicant_group
        unique (applicant_id, group_id)
)
    comment '群申请表';

