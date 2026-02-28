<template>
  <!-- #ifdef H5 -->
  <!-- PC 和移动端都在这里，通过 isMobileView 运行时判断 -->

  <!-- PC 端三列布局 -->
  <view v-if="!isMobileView" class="wechat-layout" @click="closeMoreMenu">
    <!-- 侧边栏 -->
    <view class="sidebar">
      <view class="sidebar-top">
        <view class="sidebar-item avatar" @click.stop="switchToProfile">
          <image
            :src="userInfo.avatar || defaultAvatar"
            class="avatar-img"
            mode="aspectFill"
          ></image>
        </view>
        <view
          class="sidebar-item"
          :class="{ active: currentSidebarTab === 'chat' }"
          @click.stop="switchSidebarTab('chat')"
        >
          <text class="icon">💬</text>
          <view class="red-dot" v-if="totalUnread > 0">{{ totalUnread }}</view>
        </view>
        <view
          class="sidebar-item"
          :class="{ active: currentSidebarTab === 'contact' }"
          @click.stop="switchSidebarTab('contact')"
        >
          <text class="icon">👥</text>
          <view class="red-dot" v-if="pendingNotifyCount > 0">{{ pendingNotifyCount }}</view>
        </view>
        <view class="sidebar-item">
          <text class="icon">📁</text>
        </view>
      </view>

      <view class="sidebar-bottom">
        <view
          class="sidebar-item"
          :class="{ active: showMoreMenu }"
          @click.stop="toggleMoreMenu"
          ref="moreBtnRef"
        >
          <text class="icon">☰</text>
        </view>
      </view>

      <view class="more-menu" v-if="showMoreMenu" @click.stop>
        <view class="more-menu-item" @click="openChatFiles">
          <text class="menu-text">聊天文件</text>
        </view>
        <view class="more-menu-item" @click="manageChatHistory">
          <text class="menu-text">聊天记录管理</text>
        </view>
        <view class="more-menu-item" @click="loadHistoryChat">
          <text class="menu-text">加载历史聊天记录</text>
        </view>
        <view class="more-menu-item" @click="lockApp">
          <text class="menu-text">锁定</text>
        </view>
        <view class="more-menu-item" @click="openFeedback">
          <text class="menu-text">意见反馈</text>
        </view>
        <view class="more-menu-item menu-divider" @click="openSettings">
          <text class="menu-text">设置</text>
          <text class="menu-dot">●</text>
        </view>
      </view>
    </view>

    <!-- 会话列表 -->
    <view class="session-list" v-if="currentSidebarTab === 'chat'">
      <view class="search-bar">
        <text class="search-icon">🔍</text>
        <input
          type="text"
          placeholder="搜索"
          class="search-input"
          v-model="sidebarSearchText"
          @keyup.enter="performSessionSearch"
        />
        <!-- ➕ 按钮 + 下拉菜单 -->
        <view class="add-btn-wrapper" @click.stop="togglePlusMenu">
          <text class="add-icon" :class="{ 'add-icon-active': showPlusMenu }">➕</text>
          <view class="plus-dropdown" v-if="showPlusMenu" @click.stop>
            <view class="plus-dropdown-item" @click="openAddFriend">
              <text class="plus-dropdown-icon">🧑‍🤝‍🧑</text>
              <text class="plus-dropdown-text">添加朋友</text>
            </view>
            <view class="plus-dropdown-item" @click="openJoinGroup">
              <text class="plus-dropdown-icon">🔎</text>
              <text class="plus-dropdown-text">加入群聊</text>
            </view>
            <view class="plus-dropdown-item" @click="openCreateGroup">
              <text class="plus-dropdown-icon">👥</text>
              <text class="plus-dropdown-text">发起群聊</text>
            </view>
          </view>
        </view>
      </view>
      <view class="session-scroll">
        <view
          class="session-item"
          v-for="item in filteredSessions"
          :key="item.sessionId"
          @click="switchSession(item)"
          :class="{ active: currentSession?.sessionId === item.sessionId }"
        >
          <image
            :src="item.sessionAvatar"
            class="session-avatar"
            mode="aspectFill"
          ></image>
          <view class="session-info">
            <view class="session-name">{{ item.sessionName }}</view>
            <view class="session-last-msg">{{ item.lastMessageContent }}</view>
          </view>
          <view class="session-time">{{
            formatMessageTime(item.lastMessageTime)
          }}</view>
          <view class="unread-badge" v-if="item.unreadCount > 0">{{
            item.unreadCount
          }}</view>
        </view>
      </view>
    </view>

    <!-- 通讯录列表 -->
    <view class="contact-list" v-else-if="currentSidebarTab === 'contact'">
      <view class="search-bar">
        <text class="search-icon">🔍</text>
        <input
          type="text"
          placeholder="搜索"
          class="search-input"
          v-model="contactSearchText"
          @keyup.enter="performContactSearch"
        />
      </view>
      <view class="contact-tab-bar">
        <view
          class="tab-item"
          :class="{ active: selectedFriendTab === 'friends' }"
          @click="switchContactTab('friends')"
        >
          <text>好友</text>
        </view>
        <view
          class="tab-item"
          :class="{ active: selectedFriendTab === 'groups' }"
          @click="switchContactTab('groups')"
        >
          <text>群聊</text>
        </view>
        <view
          class="tab-item notify-tab-item"
          :class="{ active: selectedFriendTab === 'notify' }"
          @click="switchContactTab('notify')"
        >
          <text>通知</text>
          <view class="tab-notify-badge" v-if="pendingNotifyCount > 0">{{ pendingNotifyCount }}</view>
        </view>
      </view>
      <view class="contact-scroll">
        <!-- 好友列表 -->
        <view v-if="selectedFriendTab === 'friends'">
          <view
            class="contact-item friend-item"
            v-for="friend in filteredFriends"
            :key="friend.userId"
            @click="selectFriend(friend)"
          >
            <image
              :src="friend.avatar"
              class="person-avatar"
              mode="aspectFill"
            ></image>
            <view class="contact-info">
              <view class="contact-name">{{ friend.nickname }}</view>
              <view class="contact-desc">{{ friend.signature }}</view>
            </view>
            <view class="contact-action" @click.stop="chatWithFriend(friend)">
              <text class="chat-icon">💬</text>
            </view>
          </view>
        </view>

        <!-- 群聊列表 -->
        <view v-else-if="selectedFriendTab === 'groups'">
          <view
            class="contact-item group-item"
            v-for="group in filteredGroups"
            :key="group.groupId"
            @click="selectGroup(group)"
          >
            <image
              :src="group.groupAvatar || defaultAvatar"
              class="person-avatar"
              mode="aspectFill"
            ></image>
            <view class="contact-info">
              <view class="contact-name">{{ group.groupName }}</view>
              <view class="contact-desc">{{ group.memberCount }} 个成员</view>
            </view>
            <view class="contact-action" @click.stop="chatWithGroup(group)">
              <text class="chat-icon">💬</text>
            </view>
          </view>
          <view class="contact-empty" v-if="filteredGroups.length === 0">
            <text class="contact-empty-text">暂无群聊</text>
          </view>
        </view>

        <!-- 通知列表 -->
        <view v-else-if="selectedFriendTab === 'notify'">
          <view class="notify-loading" v-if="notifyLoading">
            <text>加载中...</text>
          </view>
          <template v-else>
            <!-- 好友申请 -->
            <view class="notify-section" v-if="notifications.friendApplies.length > 0">
              <view class="notify-section-title">好友申请</view>
              <view
                class="notify-item"
                v-for="apply in notifications.friendApplies"
                :key="'friend_' + apply.id"
              >
                <image
                  :src="apply.applicantAvatar || defaultAvatar"
                  class="notify-avatar"
                  mode="aspectFill"
                />
                <view class="notify-info">
                  <text class="notify-name">{{ apply.applicantNickname }}</text>
                  <text class="notify-time">{{ formatMessageTime(apply.createTime) }}</text>
                </view>
                <view class="notify-actions" v-if="apply.status === 0">
                  <button class="notify-btn accept-btn" @click.stop="handleFriendApply(apply.id, 1)">接受</button>
                  <button class="notify-btn reject-btn" @click.stop="handleFriendApply(apply.id, 2)">拒绝</button>
                </view>
                <text class="notify-status-tag" :class="apply.status === 1 ? 'tag-accepted' : 'tag-rejected'" v-else>
                  {{ apply.status === 1 ? '已接受' : '已拒绝' }}
                </text>
              </view>
            </view>

            <!-- 群聊申请（作为群主/管理员收到的） -->
            <view class="notify-section" v-if="notifications.groupApplies.length > 0">
              <view class="notify-section-title">群聊申请</view>
              <view
                class="notify-item"
                v-for="apply in notifications.groupApplies"
                :key="'group_' + apply.id"
              >
                <image
                  :src="apply.applicantAvatar || defaultAvatar"
                  class="notify-avatar"
                  mode="aspectFill"
                />
                <view class="notify-info">
                  <text class="notify-name">{{ apply.applicantNickname }}</text>
                  <text class="notify-group-name">申请加入 「{{ apply.groupName }}」</text>
                  <text class="notify-time">{{ formatMessageTime(apply.createTime) }}</text>
                </view>
                <view class="notify-actions">
                  <button class="notify-btn accept-btn" @click.stop="handleGroupApply(apply.id, 1)">同意</button>
                  <button class="notify-btn reject-btn" @click.stop="handleGroupApply(apply.id, 2)">拒绝</button>
                </view>
              </view>
            </view>

            <!-- 空状态 -->
            <view
              class="notify-empty"
              v-if="!notifications.friendApplies.length && !notifications.groupApplies.length"
            >
              <text class="notify-empty-icon">🔔</text>
              <text class="notify-empty-text">暂无新通知</text>
            </view>
          </template>
        </view>
      </view>
    </view>

    <!-- 个人主页（PC端：左导航 + 右内容双栏设置页） -->
    <view class="profile-area" v-else-if="currentSidebarTab === 'profile'">
      <!-- 左侧分类导航 -->
      <view class="settings-nav">
        <view class="settings-user-brief" @click="profileSettingsTab = 'info'">
          <image :src="userInfo.avatar || defaultAvatar" class="settings-brief-avatar" mode="aspectFill" />
          <view class="settings-brief-info">
            <text class="settings-brief-name">{{ userInfo.nickname }}</text>
            <text class="settings-brief-id">ID: {{ userInfo.wechatId }}</text>
          </view>
        </view>
        <view class="settings-nav-group">
          <view class="settings-nav-item" :class="{ active: profileSettingsTab === 'info' }" @click="profileSettingsTab = 'info'">
            <text class="settings-nav-icon">👤</text>
            <text class="settings-nav-label">个人信息</text>
          </view>
          <view class="settings-nav-item" :class="{ active: profileSettingsTab === 'security' }" @click="profileSettingsTab = 'security'">
            <text class="settings-nav-icon">🔒</text>
            <text class="settings-nav-label">账号与安全</text>
          </view>
          <view class="settings-nav-item" :class="{ active: profileSettingsTab === 'notify' }" @click="profileSettingsTab = 'notify'">
            <text class="settings-nav-icon">🔔</text>
            <text class="settings-nav-label">消息通知</text>
          </view>
          <view class="settings-nav-item" :class="{ active: profileSettingsTab === 'privacy' }" @click="profileSettingsTab = 'privacy'">
            <text class="settings-nav-icon">🛡️</text>
            <text class="settings-nav-label">隐私设置</text>
          </view>
          <view class="settings-nav-item" :class="{ active: profileSettingsTab === 'general' }" @click="profileSettingsTab = 'general'">
            <text class="settings-nav-icon">⚙️</text>
            <text class="settings-nav-label">通用设置</text>
          </view>
        </view>
      </view>

      <!-- 右侧内容区 -->
      <view class="settings-content">
        <!-- 个人信息 -->
        <template v-if="profileSettingsTab === 'info'">
          <view class="settings-panel-header">
            <text class="settings-panel-title">个人信息</text>
            <text class="settings-panel-subtitle">管理你的个人资料和展示信息</text>
          </view>
          <view class="settings-body">
            <!-- 头像 -->
            <view class="settings-section">
              <text class="section-label">头像</text>
              <view class="avatar-edit-block">
                <image :src="userInfo.avatar || defaultAvatar" class="settings-avatar-preview" mode="aspectFill" />
                <view class="avatar-edit-meta">
                  <text class="avatar-tip">建议使用清晰的正面照，支持 JPG / PNG 格式</text>
                  <button class="settings-action-btn" @click="openEditProfile">更换头像</button>
                </view>
              </view>
            </view>
            <!-- 基本资料 -->
            <view class="settings-section">
              <text class="section-label">基本资料</text>
              <view class="settings-form">
                <view class="form-row">
                  <text class="form-field-label">昵称</text>
                  <text class="form-field-value">{{ userInfo.nickname }}</text>
                  <text class="form-field-action" @click="openEditProfile">编辑</text>
                </view>
                <view class="form-row">
                  <text class="form-field-label">账号 ID</text>
                  <text class="form-field-value mono">{{ userInfo.wechatId }}</text>
                </view>
                <view class="form-row">
                  <text class="form-field-label">个性签名</text>
                  <text class="form-field-value muted">{{ userInfo.signature || '未填写' }}</text>
                  <text class="form-field-action" @click="openEditProfile">编辑</text>
                </view>
                <view class="form-row no-border">
                  <text class="form-field-label">所在地区</text>
                  <text class="form-field-value muted">{{ userInfo.region || '未设置' }}</text>
                  <text class="form-field-action" @click="openEditProfile">编辑</text>
                </view>
              </view>
            </view>
            <!-- 二维码 -->
            <view class="settings-section">
              <text class="section-label">我的二维码</text>
              <view class="qrcode-card">
                <view class="qrcode-placeholder-box">
                  <text class="qrcode-placeholder-icon">📱</text>
                </view>
                <view class="qrcode-card-info">
                  <text class="qrcode-card-title">扫码添加好友</text>
                  <text class="qrcode-card-desc">将此二维码分享给朋友，让他们扫码添加你</text>
                  <button class="settings-action-btn">查看二维码</button>
                </view>
              </view>
            </view>
          </view>
        </template>

        <!-- 账号与安全 -->
        <template v-else-if="profileSettingsTab === 'security'">
          <view class="settings-panel-header">
            <text class="settings-panel-title">账号与安全</text>
            <text class="settings-panel-subtitle">管理你的登录凭证和绑定信息</text>
          </view>
          <view class="settings-body">
            <view class="settings-section">
              <text class="section-label">登录凭证</text>
              <view class="settings-option-list">
                <view class="settings-option-row">
                  <view class="option-row-left">
                    <text class="option-row-title">登录密码</text>
                    <text class="option-row-desc">定期更换密码有助于保护账号安全</text>
                  </view>
                  <button class="settings-action-btn">修改密码</button>
                </view>
                <view class="settings-option-row no-border">
                  <view class="option-row-left">
                    <text class="option-row-title">手机号码</text>
                    <text class="option-row-desc">{{ userInfo.phone || '暂未绑定' }}</text>
                  </view>
                  <button class="settings-action-btn">{{ userInfo.phone ? '更换' : '绑定' }}</button>
                </view>
              </view>
            </view>
          </view>
        </template>

        <!-- 消息通知 -->
        <template v-else-if="profileSettingsTab === 'notify'">
          <view class="settings-panel-header">
            <text class="settings-panel-title">消息通知</text>
            <text class="settings-panel-subtitle">设置你希望接收通知的方式</text>
          </view>
          <view class="settings-body">
            <view class="settings-section">
              <text class="section-label">通知偏好</text>
              <view class="settings-option-list">
                <view class="settings-option-row">
                  <view class="option-row-left">
                    <text class="option-row-title">新消息提醒</text>
                    <text class="option-row-desc">收到新消息时在桌面显示通知</text>
                  </view>
                  <view class="toggle-switch" :class="{ on: notifySettings.newMessage }" @click="notifySettings.newMessage = !notifySettings.newMessage">
                    <view class="toggle-thumb"></view>
                  </view>
                </view>
                <view class="settings-option-row no-border">
                  <view class="option-row-left">
                    <text class="option-row-title">消息提示音</text>
                    <text class="option-row-desc">收到消息时播放提示音效</text>
                  </view>
                  <view class="toggle-switch" :class="{ on: notifySettings.sound }" @click="notifySettings.sound = !notifySettings.sound">
                    <view class="toggle-thumb"></view>
                  </view>
                </view>
              </view>
            </view>
          </view>
        </template>

        <!-- 隐私设置 -->
        <template v-else-if="profileSettingsTab === 'privacy'">
          <view class="settings-panel-header">
            <text class="settings-panel-title">隐私设置</text>
            <text class="settings-panel-subtitle">控制谁可以联系你和查看你的信息</text>
          </view>
          <view class="settings-body">
            <view class="settings-section">
              <text class="section-label">好友管理</text>
              <view class="settings-option-list">
                <view class="settings-option-row no-border">
                  <view class="option-row-left">
                    <text class="option-row-title">添加好友需要验证</text>
                    <text class="option-row-desc">开启后，他人添加你时需通过验证</text>
                  </view>
                  <view class="toggle-switch" :class="{ on: privacySettings.friendVerify }" @click="privacySettings.friendVerify = !privacySettings.friendVerify">
                    <view class="toggle-thumb"></view>
                  </view>
                </view>
              </view>
            </view>
          </view>
        </template>

        <!-- 通用设置 -->
        <template v-else-if="profileSettingsTab === 'general'">
          <view class="settings-panel-header">
            <text class="settings-panel-title">通用设置</text>
            <text class="settings-panel-subtitle">应用全局功能配置</text>
          </view>
          <view class="settings-body">
            <view class="settings-section">
              <text class="section-label">账号操作</text>
              <view class="settings-option-list">
                <view class="settings-option-row danger-row no-border" @click="handleLogout">
                  <view class="option-row-left">
                    <text class="option-row-title danger">退出登录</text>
                    <text class="option-row-desc">退出后需重新登录才能继续使用</text>
                  </view>
                  <text class="option-row-arrow">›</text>
                </view>
              </view>
            </view>
          </view>
        </template>
      </view>
    </view>

    <!-- 聊天区域 -->
    <view
      class="chat-area"
      v-if="currentSidebarTab === 'chat' && currentSession"
    >
      <view class="chat-header">
        <text class="chat-name">{{ currentSession.sessionName }}</text>
        <view class="header-tools">
          <text class="tool-icon">📞</text>
          <text class="tool-icon">📹</text>
          <text class="tool-icon" @click.stop="toggleChatInfoPanel">···</text>
        </view>
        <!-- 菜单仅在头像弹出层内显示，头部不再渲染重复菜单 -->
      </view>
      <!-- 聊天信息面板（点击头部小点显示） -->
      <view class="chat-info-panel" v-if="showChatInfoPanel" @click.stop>
        <view class="chat-info-avatars">
          <view
            class="avatar-item"
            v-if="getFriendForCurrentSession()"
            @click.stop="
              openProfileFromPanel(
                $event,
                getFriendForCurrentSession(),
                getFriendForCurrentSession().userId,
              )
            "
          >
            <image
              :id="'profile-avatar-' + getFriendForCurrentSession().userId"
              :src="getFriendForCurrentSession().avatar || defaultAvatar"
              class="info-avatar"
              mode="aspectFill"
              @click.stop="
                openProfileFromPanel(
                  $event,
                  getFriendForCurrentSession(),
                  getFriendForCurrentSession().userId,
                )
              "
            />
            <text
              class="avatar-name"
              @click.stop="
                openProfileFromPanel(
                  $event,
                  getFriendForCurrentSession(),
                  getFriendForCurrentSession().userId,
                )
              "
              >{{ getFriendForCurrentSession().nickname }}</text
            >
            <!-- 头像弹出层已移出到顶级 Teleport 渲染，保持此处占位 -->
          </view>
          <view class="avatar-item add-avatar">
            <text class="add-plus">+</text>
            <text class="avatar-name">添加</text>
          </view>
        </view>
        <view class="chat-info-options">
          <view class="option-item" @click="findChatContent">查找聊天内容</view>
          <view class="option-item toggle-row">
            <text>消息免打扰</text>
            <input
              type="checkbox"
              :checked="doNotDisturb"
              @change="toggleDoNotDisturb"
            />
          </view>
          <view class="option-item toggle-row">
            <text>置顶聊天</text>
            <input type="checkbox" :checked="pinned" @change="togglePinChat" />
          </view>
          <view class="option-item option-danger" @click="clearChatRecords"
            >清空聊天记录</view
          >
        </view>
      </view>

      <scroll-view
        class="chat-messages"
        scroll-y="true"
        :scroll-into-view="scrollToView"
        :scroll-with-animation="true"
        @scroll="onChatScroll"
        ref="chatMessagesRef"
      >
        <view class="messages-inner-wrapper">
          <view v-if="messagesWithTime.length === 0" class="empty-tip">
            暂无消息，发送一条消息开始聊天吧
          </view>

          <template v-for="(item, index) in messagesWithTime" :key="index">
            <view v-if="item.type === 'time'" class="time-separator">
              {{ item.timeText }}
            </view>

            <view
              v-else-if="item.type === 'msg'"
              class="message-item"
              :class="isMyMessage(item.msg) ? 'self' : 'other'"
              :id="'msg-' + item.msg.id"
            >
              <image
                :src="item.msg.avatar || defaultAvatar"
                class="message-avatar"
                mode="aspectFill"
              ></image>

              <view class="message-content-wrapper">
                <view
                  v-if="
                    currentSession.sessionType === SESSION_TYPE.GROUP &&
                    !isMyMessage(item.msg) &&
                    item.msg.sender_name
                  "
                  class="message-sender"
                >
                  {{ item.msg.sender_name }}
                </view>
                <view
                  v-if="
                    item.msg.message_type === MESSAGE_TYPE.TEXT &&
                    item.msg.content &&
                    item.msg.content.trim()
                  "
                  class="message-content"
                >
                  {{ item.msg.content }}
                </view>

                <image
                  v-else-if="
                    item.msg.message_type === MESSAGE_TYPE.IMAGE &&
                    item.msg.file_url
                  "
                  :src="item.msg.local_file_url || item.msg.file_url"
                  class="message-image"
                  @click="handleImageClick(item.msg)"
                  mode="aspectFill"
                  :lazy-load="true"
                ></image>

                <video
                  v-else-if="
                    item.msg.message_type === MESSAGE_TYPE.VIDEO &&
                    item.msg.file_url
                  "
                  :src="item.msg.local_file_url || item.msg.file_url"
                  class="message-video"
                  controls
                  show-center-play-btn
                  @click="handleVideoClick(item.msg)"
                ></video>

                <view
                  v-else-if="item.msg.message_type === MESSAGE_TYPE.AUDIO"
                  class="message-voice"
                  @click="handleVoiceClick(item.msg)"
                >
                  <text class="voice-icon">🔊</text>
                  <text class="voice-duration"
                    >{{ item.msg.duration || '1' }}''</text
                  >
                  <text v-if="item.msg.is_downloading" class="downloading-text"
                    >下载中...</text
                  >
                </view>

                <view
                  v-else-if="
                    item.msg.message_type === MESSAGE_TYPE.FILE &&
                    item.msg.file_url
                  "
                  class="message-file"
                  @click="handleFileClick(item.msg)"
                >
                  <text class="file-icon">📄</text>
                  <view class="file-info">
                    <text class="file-name">{{
                      item.msg.file_name || '未知文件'
                    }}</text>
                    <text class="file-size">{{
                      formatFileSize(item.msg.file_size)
                    }}</text>
                    <text
                      v-if="item.msg.is_downloading"
                      class="downloading-text"
                      >下载中 {{ item.msg.download_progress || 0 }}%</text
                    >
                  </view>
                </view>

                <view v-else class="message-content">
                  {{ item.msg.content_replaced || item.msg.content || '' }}
                </view>
              </view>
            </view>
          </template>
        </view>
      </scroll-view>

      <view
        v-if="showScrollToBottom"
        class="scroll-to-bottom-btn"
        @click="scrollToBottom"
      >
        <text class="scroll-icon">↓</text>
      </view>

      <view v-if="isRecording" class="recording-mask" @click.stop>
        <view class="recording-modal">
          <text class="recording-wave">🎙️</text>
          <text class="recording-modal-text"
            >正在录音... {{ recordingDuration }}s</text
          >
          <view class="recording-btns">
            <button class="cancel-voice-btn" @click="cancelVoice">取消</button>
            <button class="send-voice-btn" @click="stopAndSendVoice">
              发送
            </button>
          </view>
        </view>
      </view>

      <view class="chat-input-area">
        <view v-if="pendingFiles.length > 0" class="pending-files">
          <view
            v-for="(file, index) in pendingFiles"
            :key="index"
            class="pending-file-item"
          >
            <image
              v-if="file.message_type === MESSAGE_TYPE.IMAGE"
              :src="file.file_url"
              class="pending-file-image"
              mode="aspectFill"
            ></image>
            <view
              v-else-if="file.message_type === MESSAGE_TYPE.VIDEO"
              class="pending-file-video"
            >
              <text class="video-icon">🎬</text>
              <text class="file-name">{{ file.file_name }}</text>
            </view>
            <view v-else class="pending-file-doc">
              <text class="doc-icon">📄</text>
              <text class="file-name">{{ file.file_name }}</text>
            </view>
            <text class="remove-btn" @click="removePendingFile(index)">×</text>
          </view>
        </view>

        <view class="input-tools">
          <view class="tool-icon-wrapper" @click="toggleVoiceRecording">
            <text v-if="!isRecording" class="tool-icon">🎤</text>
            <view v-else class="recording-btn-active">
              <text class="recording-pulse">🔴</text>
            </view>
          </view>

          <view class="emoji-btn-wrapper" ref="emojiBtnRef">
            <text class="tool-icon" @click="toggleEmojiPanel">😊</text>
            <view class="emoji-panel" v-if="showEmojiPanel" @click.stop>
              <view class="emoji-section" v-if="recentEmojis.length > 0">
                <view class="emoji-title">最近使用</view>
                <view class="emoji-grid">
                  <text
                    class="emoji-item"
                    v-for="(emoji, index) in recentEmojis"
                    :key="`recent-${index}`"
                    @click="insertEmoji(emoji)"
                    >{{ emoji }}</text
                  >
                </view>
              </view>

              <view class="emoji-section">
                <view class="emoji-title">所有表情</view>
                <scroll-view class="emoji-scroll" scroll-y="true">
                  <view class="emoji-grid">
                    <text
                      class="emoji-item"
                      v-for="(emoji, index) in allEmojis"
                      :key="`all-${index}`"
                      @click="insertEmoji(emoji)"
                      >{{ emoji }}</text
                    >
                  </view>
                </scroll-view>
              </view>
            </view>
          </view>

          <text class="tool-icon">📦</text>
          <text class="tool-icon" @click="chooseFile">📁</text>
          <text class="tool-icon" @click="chooseImage">🖼️</text>
        </view>

        <div
          class="input-container"
          @dragenter="onInputDragEnter"
          @dragover="onInputDragOver"
          @dragleave="onInputDragLeave"
          @drop="onInputDrop"
          :class="{ 'drag-over': isInputDragOver }"
        >
          <textarea
            v-model="inputMsg"
            placeholder="输入消息..."
            class="chat-input"
            @keydown.enter="handleEnterKey"
            @keydown.ctrl.enter="handleCtrlEnter"
            :auto-height="true"
            :maxlength="-1"
            ref="inputRef"
          />

          <view class="send-btn-wrapper">
            <button
              class="send-btn"
              :disabled="
                (!inputMsg.trim() && pendingFiles.length === 0) || isSending
              "
              @click="sendMessageWithFiles"
            >
              {{ isSending ? '发送中...' : '发送(S)' }}
            </button>
          </view>
        </div>
      </view>
    </view>

    <!-- 通讯录详情 -->
    <view
      class="contact-content-area"
      v-else-if="currentSidebarTab === 'contact'"
    >
      <view class="contact-empty" v-if="!currentContact">
        <image
          src="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZyBmaWxsPSIjY2NjY2NjIj48Y2lyY2xlIGN4PSI1MCIgY3k9IjM1IiByPSIxNSIvPjxwYXRoIGQ9Ik0yNSA4MGMwLTEzLjggMTEuMi0yNSAyNS0yNXMyNSAxMS4yIDI1IDI1SDI1eiIvPjwvZz48L3N2Zz4="
          class="empty-logo"
          mode="aspectFit"
        ></image>
      </view>

      <view class="contact-detail" v-else>
        <view class="detail-header">
          <image
            :src="currentContact.avatar || defaultAvatar"
            class="detail-avatar"
            mode="aspectFill"
          ></image>
          <view class="detail-info">
            <text class="detail-name">{{ currentContact.sessionName }}</text>
            <text class="detail-desc" v-if="currentContact.type === 'person'"
              >微信号：wx_{{ currentContact.targetId }}</text
            >
            <text class="detail-desc" v-else>{{
              currentContact.desc || '暂无描述'
            }}</text>
          </view>
        </view>

        <view class="detail-btns">
          <button class="primary-btn" @click="chatWithContact">发消息</button>
          <button class="default-btn">音视频通话</button>
        </view>

        <view class="detail-list">
          <view class="detail-item">
            <text class="item-label">备注</text>
            <text class="item-value">{{ currentContact.sessionName }}</text>
          </view>
          <view class="detail-item" v-if="currentContact.type === 'person'">
            <text class="item-label">地区</text>
            <text class="item-value">中国 福建</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 编辑弹窗（需自行创建该组件） -->
    <EditProfile
      :visible="showEditProfile"
      :userInfo="userInfo"
      @close="showEditProfile = false"
      @update:userInfo="handleUserInfoUpdate"
    />

    <!-- 添加朋友弹窗 -->
    <view v-if="showAddFriendModal" class="add-friend-mask" @click.self="closeAddFriend">
      <view class="add-friend-modal" @click.stop>
        <view class="add-friend-header">
          <text class="add-friend-title">添加朋友</text>
          <text class="add-friend-close" @click="closeAddFriend">✕</text>
        </view>
        <view class="add-friend-search-bar">
          <view class="add-friend-input-wrap">
            <text class="add-friend-search-icon">🔍</text>
            <input
              v-model="addFriendKeyword"
              class="add-friend-input"
              placeholder="搜索用户名或手机号"
              @keyup.enter="searchUserForAdd"
              :disabled="isSearchingUser"
            />
            <text v-if="addFriendKeyword" class="add-friend-clear" @click="clearAddFriendSearch">✕</text>
          </view>
          <button
            class="add-friend-search-btn"
            :disabled="!addFriendKeyword.trim() || isSearchingUser"
            @click="searchUserForAdd"
          >{{ isSearchingUser ? '搜索中...' : '搜索' }}</button>
        </view>
        <view class="add-friend-body">
          <view v-if="!addFriendSearched" class="add-friend-hint">
            <text class="hint-icon">👤</text>
            <text class="hint-text">输入用户名或手机号，搜索添加新朋友</text>
          </view>
          <view v-else-if="isSearchingUser" class="add-friend-loading">
            <text class="loading-text">搜索中...</text>
          </view>
          <view v-else-if="addFriendResult === null" class="add-friend-empty">
            <text class="empty-icon">🔍</text>
            <text class="empty-text">未找到该用户，请确认用户名是否正确</text>
          </view>
          <view v-else class="add-friend-result">
            <image :src="addFriendResult.avatar || defaultAvatar" class="result-avatar" mode="aspectFill" />
            <view class="result-info">
              <text class="result-nickname">{{ addFriendResult.nickname }}</text>
              <text class="result-username">用户名：{{ addFriendResult.username }}</text>
              <text class="result-signature" v-if="addFriendResult.signature">{{ addFriendResult.signature }}</text>
            </view>
            <view class="result-action">
              <button v-if="addFriendResult.isFriend" class="result-btn result-btn-already" disabled>已是好友</button>
              <button v-else-if="addFriendResult.isApplied" class="result-btn result-btn-applied" disabled>已申请</button>
              <button v-else class="result-btn result-btn-add" :disabled="isSendingApply" @click="sendFriendApply">
                {{ isSendingApply ? '发送中...' : '+加好友' }}
              </button>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 加入群聊弹窗 -->
    <view v-if="showJoinGroupModal" class="add-friend-mask" @click.self="closeJoinGroup">
      <view class="add-friend-modal" @click.stop>
        <view class="add-friend-header">
          <text class="add-friend-title">加入群聊</text>
          <text class="add-friend-close" @click="closeJoinGroup">✕</text>
        </view>
        <view class="add-friend-search-bar">
          <view class="add-friend-input-wrap">
            <text class="add-friend-search-icon">🔍</text>
            <input
              v-model="joinGroupKeyword"
              class="add-friend-input"
              placeholder="输入群名称搜索"
              @keyup.enter="searchGroupForJoin"
              :disabled="isSearchingGroup"
            />
            <text v-if="joinGroupKeyword" class="add-friend-clear" @click="clearJoinGroupSearch">✕</text>
          </view>
          <button
            class="add-friend-search-btn"
            :disabled="!joinGroupKeyword.trim() || isSearchingGroup"
            @click="searchGroupForJoin"
          >{{ isSearchingGroup ? '搜索中...' : '搜索' }}</button>
        </view>
        <view class="add-friend-body">
          <view v-if="!joinGroupSearched" class="add-friend-hint">
            <text class="hint-icon">👥</text>
            <text class="hint-text">输入群名称，搜索并申请加入群聊</text>
          </view>
          <view v-else-if="isSearchingGroup" class="add-friend-loading">
            <text class="loading-text">搜索中...</text>
          </view>
          <view v-else-if="!joinGroupResults || joinGroupResults.length === 0" class="add-friend-empty">
            <text class="empty-icon">🔍</text>
            <text class="empty-text">未找到相关群聊</text>
          </view>
          <view v-else class="join-group-result-list">
            <view
              class="join-group-item"
              v-for="group in joinGroupResults"
              :key="group.id"
            >
              <image :src="group.groupAvatar || defaultAvatar" class="result-avatar" mode="aspectFill" />
              <view class="result-info">
                <text class="result-nickname">{{ group.groupName }}</text>
                <text class="result-username">{{ group.currentMemberCount }}/{{ group.maxMember }} 人</text>
                <text class="result-signature">
                  {{ group.joinType === 2 ? '免审核加入' : group.joinType === 3 ? '仅邀请加入' : '需审核加入' }}
                </text>
              </view>
              <view class="result-action">
                <button
                  v-if="group.applyStatus === 'member'"
                  class="result-btn result-btn-already" disabled
                >已加入</button>
                <button
                  v-else-if="group.applyStatus === 'pending'"
                  class="result-btn result-btn-applied" disabled
                >已申请</button>
                <button
                  v-else-if="group.joinType === 3"
                  class="result-btn result-btn-already" disabled
                >仅邀请</button>
                <button
                  v-else
                  class="result-btn result-btn-add"
                  :disabled="isApplyingGroup"
                  @click="applyJoinGroupFn(group)"
                >
                  {{ isApplyingGroup ? '申请中...' : '申请加入' }}
                </button>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>
    <!-- 个人资料模态（从聊天信息面板或头像点击打开） -->
    <view
      v-if="showProfileModal"
      class="profile-modal-mask"
      @click.self="closeProfileModal"
    >
      <view class="profile-modal">
        <view class="profile-modal-header">
          <view style="display: flex; align-items: center; gap: 12px">
            <image
              :src="currentContact?.avatar || defaultAvatar"
              class="profile-modal-avatar"
              mode="aspectFill"
            />
            <view>
              <text class="profile-modal-name">{{
                currentContact?.sessionName
              }}</text>
              <text class="profile-modal-sub"
                >昵称：{{ currentContact?.sessionName }}</text
              >
              <text class="profile-modal-sub"
                >微信号：wx_{{ currentContact?.targetId }}</text
              >
              <text class="profile-modal-sub"
                >地区：{{ userInfo.region || '未设置' }}</text
              >
            </view>
          </view>
          <text class="profile-modal-dot" @click.stop="toggleChatMenu"
            >···</text
          >
        </view>
        <view class="profile-modal-body">
          <view class="profile-row"
            ><text class="row-label">备注</text
            ><text class="row-value">{{
              currentContact?.sessionName
            }}</text></view
          >
          <view class="profile-row"
            ><text class="row-label">共同群聊</text
            ><text class="row-value">6个</text></view
          >
          <view class="profile-row"
            ><text class="row-label">个性签名</text
            ><text class="row-value">{{
              currentContact?.desc || '这人很懒，没写签名'
            }}</text></view
          >
          <view class="profile-row"
            ><text class="row-label">来源</text
            ><text class="row-value">通过群聊添加</text></view
          >
          <view class="profile-actions">
            <button class="primary-btn" @click="chatWithContact">发消息</button>
            <button class="default-btn">语音聊天</button>
            <button class="default-btn">视频聊天</button>
          </view>
        </view>
      </view>
    </view>
  </view>

  <!-- 移动端布局：H5 响应式用 v-else -->
  <view v-if="isMobileView" class="mobile-container">
    <!-- 聊天列表页面 -->
    <view
      v-if="mobileCurrentTab === 'chat' && !currentMobileChat"
      class="mobile-session-list"
    >
      <!-- 顶部导航栏 -->
      <view class="mobile-navbar">
        <text class="mobile-title">微信</text>
        <view class="mobile-nav-icons">
          <text class="mobile-icon" @click="toggleSearch">🔍</text>
          <text class="mobile-icon">➕</text>
        </view>
      </view>

      <!-- 搜索框（可选显示） -->
      <view v-if="showMobileSearch" class="mobile-search-box">
        <input
          v-model="mobileSearchText"
          type="text"
          placeholder="搜索"
          class="mobile-search-input"
          @blur="showMobileSearch = false"
        />
      </view>

      <!-- 会话列表 -->
      <scroll-view scroll-y class="mobile-sessions-scroll">
        <view
          v-for="session in filteredMobileSessions"
          :key="session.sessionId"
          class="mobile-session-item"
          @click="enterMobileChat(session)"
        >
          <image
            :src="session.sessionAvatar || defaultAvatar"
            class="mobile-avatar"
          />

          <view class="mobile-session-content">
            <view class="mobile-session-header">
              <text class="mobile-session-name">{{ session.sessionName }}</text>
              <text class="mobile-time">{{
                formatMessageTime(session.lastMessageTime)
              }}</text>
            </view>
            <view class="mobile-last-msg">{{
              session.lastMessageContent
            }}</view>
          </view>

          <view v-if="session.unreadCount > 0" class="mobile-unread-badge">
            {{ session.unreadCount }}
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 聊天详情页面 (H5 响应式) -->
    <view
      v-else-if="mobileCurrentTab === 'chat' && currentMobileChat"
      class="mobile-chat-detail"
    >
      <!-- 聊天顶部栏 -->
      <view class="mobile-chat-header">
        <text class="mobile-back-btn" @click="backToMobileList"
          >← {{ currentMobileChat.sessionName }}</text
        >
        <text class="mobile-more-icon">···</text>
      </view>

      <!-- 消息列表 -->
      <scroll-view
        scroll-y
        class="mobile-messages-container"
        :scroll-into-view="scrollToView"
        :scroll-with-animation="true"
      >
        <view class="mobile-messages-wrapper">
          <view v-if="messages.length === 0" class="mobile-empty-tip">
            暂无消息，发送一条开始聊天吧
          </view>

          <template v-for="(item, idx) in messagesWithTime" :key="idx">
            <!-- 时间分隔线 -->
            <view v-if="item.type === 'time'" class="mobile-time-separator">
              {{ item.timeText }}
            </view>

            <!-- 消息气泡 -->
            <view
              v-if="item.type === 'msg'"
              class="mobile-message-bubble"
              :class="isMyMessage(item.msg) ? 'mobile-own' : 'mobile-other'"
              :id="'msg-' + item.msg.id"
            >
              <image
                :src="item.msg.avatar || defaultAvatar"
                class="mobile-bubble-avatar"
              />

              <view class="mobile-bubble-content">
                <!-- 文本消息 -->
                <view
                  v-if="
                    item.msg.message_type === MESSAGE_TYPE.TEXT &&
                    item.msg.content &&
                    item.msg.content.trim()
                  "
                  class="mobile-text-msg"
                >
                  {{ item.msg.content }}
                </view>

                <!-- 图片消息 -->
                <image
                  v-else-if="
                    item.msg.message_type === MESSAGE_TYPE.IMAGE &&
                    item.msg.file_url
                  "
                  :src="item.msg.local_file_url || item.msg.file_url"
                  class="mobile-image-msg"
                  @click="handleImageClick(item.msg)"
                  mode="aspectFill"
                />

                <!-- 表情消息 -->
                <text
                  v-else-if="item.msg.message_type === MESSAGE_TYPE.AUDIO"
                  class="mobile-emoji-msg"
                >
                  🎤 语音
                </text>

                <!-- 文件消息 -->
                <view
                  v-else-if="
                    item.msg.message_type === MESSAGE_TYPE.FILE &&
                    item.msg.file_url
                  "
                  class="mobile-message-file"
                  @click="handleFileClick(item.msg)"
                >
                  <text class="mobile-file-icon">📄</text>
                  <view class="mobile-file-info">
                    <text class="mobile-file-name">{{
                      item.msg.file_name || '未知文件'
                    }}</text>
                    <text class="mobile-file-size">{{
                      formatFileSize(item.msg.file_size)
                    }}</text>
                  </view>
                </view>
              </view>
            </view>
          </template>
        </view>
      </scroll-view>

      <!-- 输入栏 -->
      <view class="mobile-input-bar">
        <view class="mobile-input-tools">
          <text class="mobile-tool-icon" @click="startMobileRecord">🎤</text>
          <text class="mobile-tool-icon" @click="toggleMobileEmoji">😊</text>
          <text class="mobile-tool-icon" @click="chooseMobileImage">🖼️</text>
          <text class="mobile-tool-icon">📁</text>
        </view>

        <view class="mobile-input-field">
          <input
            v-model="inputMsg"
            type="text"
            placeholder="输入消息..."
            class="mobile-text-input"
            @keyup.enter="sendMessageWithFiles"
          />
          <text class="mobile-send-btn" @click="sendMessageWithFiles"
            >发送</text
          >
        </view>
      </view>

      <!-- 表情选择器 -->
      <view v-if="showEmojiMobile" class="mobile-emoji-picker">
        <scroll-view scroll-x class="mobile-emoji-scroll">
          <view class="mobile-emoji-grid">
            <text
              v-for="emoji in allEmojis"
              :key="emoji"
              class="mobile-emoji-item"
              @click="insertMobileEmoji(emoji)"
            >
              {{ emoji }}
            </text>
          </view>
        </scroll-view>
      </view>
    </view>

    <!-- 移动端通讯录页面 -->
    <view
      v-else-if="mobileCurrentTab === 'contact'"
      class="mobile-contact-page"
    >
      <!-- 顶部栏 -->
      <view class="mobile-chat-header">
        <text class="mobile-back-btn" @click="mobileCurrentTab = 'chat'"
          >← 通讯录</text
        >
        <text class="mobile-more-icon">···</text>
      </view>
      <!-- 搜索框 -->
      <view class="mobile-search-box">
        <input
          v-model="contactSearchText"
          class="mobile-search-input"
          placeholder="搜索"
          @keyup.enter="performContactSearch"
        />
      </view>
      <!-- 好友/群切换 -->
      <view class="mobile-contact-tab-bar">
        <view
          class="mobile-tab-item"
          :class="{ 'mobile-active': selectedFriendTab === 'friends' }"
          @click="selectedFriendTab = 'friends'"
        >
          <text>好友</text>
        </view>
        <view
          class="mobile-tab-item"
          :class="{ 'mobile-active': selectedFriendTab === 'groups' }"
          @click="selectedFriendTab = 'groups'"
        >
          <text>群聊</text>
        </view>
      </view>
      <scroll-view scroll-y class="mobile-contacts-scroll">
        <view v-if="selectedFriendTab === 'friends'">
          <view
            class="mobile-contact-item"
            v-for="friend in filteredFriends"
            :key="friend.userId"
            @click="chatWithFriend(friend)"
          >
            <image
              :src="friend.avatar"
              class="mobile-avatar"
              mode="aspectFill"
            />
            <view class="mobile-session-content">
              <text class="mobile-session-name">{{ friend.nickname }}</text>
              <text class="mobile-last-msg">{{ friend.signature }}</text>
            </view>
          </view>
        </view>
        <view v-else>
          <view
            class="mobile-contact-item"
            v-for="group in filteredGroups"
            :key="group.groupId"
            @click="chatWithGroup(group)"
          >
            <image
              :src="group.groupAvatar"
              class="mobile-avatar"
              mode="aspectFill"
            />
            <view class="mobile-session-content">
              <text class="mobile-session-name">{{ group.groupName }}</text>
              <text class="mobile-last-msg"
                >{{ group.memberCount }} 个成员</text
              >
            </view>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 个人资料页面 (H5 响应式) -->
    <view
      v-else-if="mobileCurrentTab === 'profile'"
      class="mobile-profile-page"
    >
      <!-- 顶部栏 -->
      <view class="mobile-chat-header">
        <text class="mobile-back-btn" @click="backToMobileList">← 我</text>
        <text class="mobile-more-icon">···</text>
      </view>

      <!-- 个人资料内容 -->
      <scroll-view scroll-y class="mobile-profile-content">
        <!-- 个人信息卡片 -->
        <view class="mobile-profile-card">
          <image
            :src="userInfo.avatar || defaultAvatar"
            class="mobile-profile-avatar"
            mode="aspectFill"
          ></image>
          <view class="mobile-profile-base-info">
            <text class="mobile-profile-nickname">{{ userInfo.nickname }}</text>
            <text class="mobile-profile-wechat-id"
              >微信号：{{ userInfo.wechatId }}</text
            >
            <view class="mobile-profile-edit-btn" @click="openEditProfile"
              >编辑资料</view
            >
          </view>
        </view>

        <!-- 基本信息 -->
        <view class="mobile-profile-section">
          <view class="mobile-profile-item" @click="openEditProfile">
            <text class="mobile-profile-label">昵称</text>
            <text class="mobile-profile-value">{{ userInfo.nickname }}</text>
          </view>
          <view class="mobile-profile-item" @click="openEditProfile">
            <text class="mobile-profile-label">个性签名</text>
            <text class="mobile-profile-value">{{
              userInfo.signature || '这个人很懒，什么都没写'
            }}</text>
          </view>
          <view class="mobile-profile-item">
            <text class="mobile-profile-label">地区</text>
            <text class="mobile-profile-value">{{
              userInfo.region || '未设置'
            }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 底部导航栏 (H5 响应式) -->
    <view class="mobile-bottom-tab-bar">
      <view
        class="mobile-tab-item"
        :class="{ 'mobile-active': mobileCurrentTab === 'chat' }"
        @click="setMobileTab('chat')"
      >
        <text class="mobile-tab-icon">💬</text>
        <text class="mobile-tab-label">聊天</text>
      </view>
      <view
        class="mobile-tab-item"
        :class="{ 'mobile-active': mobileCurrentTab === 'contact' }"
        @click="setMobileTab('contact')"
      >
        <text class="mobile-tab-icon">👥</text>
        <text class="mobile-tab-label">通讯录</text>
      </view>
      <view
        class="mobile-tab-item"
        :class="{ 'mobile-active': mobileCurrentTab === 'profile' }"
        @click="setMobileTab('profile')"
      >
        <text class="mobile-tab-icon">👤</text>
        <text class="mobile-tab-label">我</text>
      </view>
    </view>
  </view>
  <!-- #endif -->

  <!-- #ifndef H5 -->
  <!-- App 原生移动端（安卓和苹果） -->
  <view class="mobile-container">
    <!-- 使用相同的移动端布局，因为原生 APP 本身就是移动端 -->
    <!-- 会话列表页面 -->
    <view v-if="!currentMobileChat" class="mobile-session-list">
      <!-- 顶部导航栏 -->
      <view class="mobile-navbar">
        <text class="mobile-title">微信</text>
        <view class="mobile-nav-icons">
          <text class="mobile-icon" @click="toggleSearch">🔍</text>
          <text class="mobile-icon">➕</text>
        </view>
      </view>

      <!-- 会话列表 -->
      <scroll-view scroll-y class="mobile-sessions-scroll">
        <view
          v-for="session in filteredMobileSessions"
          :key="session.sessionId"
          class="mobile-session-item"
          @click="enterMobileChat(session)"
        >
          <image
            :src="session.sessionAvatar || defaultAvatar"
            class="mobile-avatar"
          />
          <view class="mobile-session-content">
            <view class="mobile-session-header">
              <text class="mobile-session-name">{{ session.sessionName }}</text>
              <text class="mobile-time">{{
                formatMessageTime(session.lastMessageTime)
              }}</text>
            </view>
            <view class="mobile-last-msg">{{
              session.lastMessageContent
            }}</view>
          </view>
          <view v-if="session.unreadCount > 0" class="mobile-unread-badge">
            {{ session.unreadCount }}
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 聊天详情页面 -->
    <view v-else class="mobile-chat-detail">
      <!-- 聊天顶部栏 -->
      <view class="mobile-chat-header">
        <text class="mobile-back-btn" @click="backToMobileList"
          >← {{ currentMobileChat.sessionName }}</text
        >
        <text class="mobile-more-icon">···</text>
      </view>

      <!-- 消息列表 -->
      <scroll-view
        scroll-y
        class="mobile-messages-container"
        :scroll-into-view="scrollToView"
        :scroll-with-animation="true"
      >
        <view class="mobile-messages-wrapper">
          <template v-for="(item, idx) in messagesWithTime" :key="idx">
            <view v-if="item.type === 'time'" class="mobile-time-separator">
              {{ item.timeText }}
            </view>
            <view
              v-if="item.type === 'msg'"
              class="mobile-message-bubble"
              :class="isMyMessage(item.msg) ? 'mobile-own' : 'mobile-other'"
            >
              <image
                :src="item.msg.avatar || defaultAvatar"
                class="mobile-bubble-avatar"
              />
              <view class="mobile-bubble-content">
                <view
                  v-if="
                    item.msg.message_type === MESSAGE_TYPE.TEXT &&
                    item.msg.content &&
                    item.msg.content.trim()
                  "
                  class="mobile-text-msg"
                >
                  {{ item.msg.content }}
                </view>
                <image
                  v-else-if="
                    item.msg.message_type === MESSAGE_TYPE.IMAGE &&
                    item.msg.file_url
                  "
                  :src="item.msg.local_file_url || item.msg.file_url"
                  class="mobile-image-msg"
                  @click="handleImageClick(item.msg)"
                  mode="aspectFill"
                />
              </view>
            </view>
          </template>
        </view>
      </scroll-view>

      <!-- 输入栏 -->
      <view class="mobile-input-bar">
        <view class="mobile-input-tools">
          <text class="mobile-tool-icon" @click="startMobileRecord">🎤</text>
          <text class="mobile-tool-icon" @click="toggleMobileEmoji">😊</text>
          <text class="mobile-tool-icon" @click="chooseMobileImage">🖼️</text>
          <text class="mobile-tool-icon">📁</text>
        </view>
        <view class="mobile-input-field">
          <input
            v-model="inputMsg"
            type="text"
            placeholder="输入消息..."
            class="mobile-text-input"
            @keyup.enter="sendMessageWithFiles"
          />
          <text class="mobile-send-btn" @click="sendMessageWithFiles"
            >发送</text
          >
        </view>
      </view>
    </view>

    <!-- 底部导航栏 -->
    <view class="mobile-bottom-tab-bar">
      <view
        class="mobile-tab-item"
        :class="{ 'mobile-active': !currentMobileChat }"
        @click="currentMobileChat = null"
      >
        <text class="mobile-tab-icon">💬</text>
        <text class="mobile-tab-label">聊天</text>
      </view>
      <view class="mobile-tab-item">
        <text class="mobile-tab-icon">👥</text>
        <text class="mobile-tab-label">通讯录</text>
      </view>
      <view class="mobile-tab-item">
        <text class="mobile-tab-icon">👤</text>
        <text class="mobile-tab-label">我</text>
      </view>
    </view>
  </view>
  <!-- #endif -->
  <teleport to="body">
    <view
      class="profile-popover"
      v-if="showProfilePopover"
      :style="popoverStyle"
      @click.stop
    >
      <view :class="['popover-arrow', popoverSide]" />
      <view class="popover-header">
        <image
          :src="currentContact?.avatar || defaultAvatar"
          class="popover-avatar"
          mode="aspectFill"
        />
        <view class="popover-meta">
          <text class="popover-name">{{ currentContact?.sessionName }}</text>
          <text class="popover-sub"
            >昵称：{{ currentContact?.sessionName }} 微信号：wx_{{
              currentContact?.targetId
            }}</text
          >
        </view>
        <text
          class="tool-icon popover-dots"
          @click.stop="showChatMenu = !showChatMenu"
          >···</text
        >
        <view class="popover-menu" v-if="showChatMenu" @click.stop>
          <view class="chat-menu-item" @click="openSetRemark"
            >设置备注和标签</view
          >
          <view class="chat-menu-item" @click="setFriendPermissions"
            >设置朋友权限</view
          >
          <view class="chat-menu-item" @click="recommendFriend"
            >把他推荐给朋友</view
          >
          <view class="chat-menu-item" @click="toggleStarFriend">{{
            friendIsStarText
          }}</view>
          <view class="chat-menu-item" @click="addToBlacklist">加入黑名单</view>
          <view
            class="chat-menu-item chat-menu-item-danger"
            @click="deleteContact"
            >删除联系人</view
          >
        </view>
      </view>
      <view class="popover-body">
        <view class="popover-row"
          ><text>备注</text><text>{{ currentContact?.sessionName }}</text></view
        >
        <view class="popover-row"><text>共同群聊</text><text>6个</text></view>
        <view class="popover-row"
          ><text>个性签名</text
          ><text>{{ currentContact?.desc || '暂无' }}</text></view
        >
        <view class="popover-actions">
          <button class="primary-btn" @click="chatWithContact">发消息</button>
          <button class="default-btn">语音聊天</button>
          <button class="default-btn">视频聊天</button>
        </view>
      </view>
    </view>
  </teleport>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onUnmounted, watch } from 'vue'
import ChatStorage from '@/utils/chat-storage'
import service from '@/utils/request'
import EditProfile from '@/components/EditProfile.vue'
// 引入下载工具
import {
  handleMsgDownload,
  FILE_SIZE_THRESHOLD,
  getUserInfoFromStorage,
  saveUserInfoToStorage,
} from '@/utils/download-util'

// #ifdef H5
import SparkMD5 from 'spark-md5'
// #endif

// 编辑弹窗
const showEditProfile = ref(false)
const openEditProfile = () => {
  showEditProfile.value = true
}
const handleUserInfoUpdate = (newInfo) => {
  userInfo.value = { ...userInfo.value, ...newInfo }
  saveUserInfoToStorage(userInfo.value)
}

// 核心配置
const CURRENT_USER_ID = ref(null) // 动态从 /user/info 获取，初始为 null
const selfAvatar =
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iMjAiIGN5PSIyMCIgcj0iMjAiIGZpbGw9IiNjY2NjY2MiLz48L3N2Zz4='
const defaultAvatar = selfAvatar
const targetAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=Susan'

// 枚举
const MESSAGE_TYPE = { TEXT: 1, IMAGE: 2, VIDEO: 3, AUDIO: 4, FILE: 5 }
const SESSION_TYPE = { SINGLE: 1, GROUP: 2 }
const SEND_STATUS = { PENDING: 'pending', SUCCESS: 'success', FAILED: 'failed' }
const CHUNK_SIZE = 10 * 1024 * 1024

// 【H5 设备检测】
// #ifdef H5
const isMobileView = ref(false)

const detectDevice = () => {
  if (typeof window !== 'undefined') {
    const ua = navigator.userAgent.toLowerCase()
    // 检测 User-Agent 或窗口宽度
    const isMobileUA =
      /mobile|tablet|ios|android|iphone|ipad|windows phone/.test(ua)
    const isMobileWidth = window.innerWidth < 768
    isMobileView.value = isMobileUA || isMobileWidth
  }
}

// 页面加载时检测
const initDeviceDetection = () => {
  detectDevice()
  if (typeof window !== 'undefined') {
    window.addEventListener('resize', detectDevice)
  }
}

const cleanupDeviceDetection = () => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', detectDevice)
  }
}
// #endif

// 【修奢1：用户信息持久化初始化】
const userInfo = ref({
  id: '',
  nickname: '我的昵称',
  avatar: selfAvatar,
  wechatId: '',
  region: '',
  signature: '',
})

// 个人主页设置页 tab
const profileSettingsTab = ref('info')
// 通知设置（本地状态，可后续对接接口）
const notifySettings = ref({ newMessage: true, sound: true })
// 隐私设置（本地状态，可后续对接接口）
const privacySettings = ref({ friendVerify: true })

// 状态
const currentSidebarTab = ref('chat')
const currentContact = ref(null)
const sessions = ref([])
const currentSession = ref(null)
// 搜索状态（会话与通讯录分别）
const sidebarSearchText = ref('')
const sidebarSearchQuery = ref('')
const contactSearchText = ref('')
const contactSearchQuery = ref('')

const inputMsg = ref('')
const messages = ref([])
const pendingFiles = ref([])
const showScrollToBottom = ref(false)
const scrollToView = ref('')
const isInputDragOver = ref(false)
const showEmojiPanel = ref(false)
const recentEmojis = ref([])
const allEmojis = ref([
  '😀',
  '😅',
  '😍',
  '😭',
  '😊',
  '😎',
  '😢',
  '😡',
  '🥳',
  '😱',
  '🤔',
  '👍',
])
// 聊天右上角菜单状态
const showChatMenu = ref(false)
// 黑名单（本地模拟）
const blacklist = ref([])
// 聊天信息面板与配置
const showChatInfoPanel = ref(false)
const doNotDisturb = ref(false)
const pinned = ref(false)
const showProfileModal = ref(false)
// 头像右侧小弹出层
const showProfilePopover = ref(false)
const showMoreMenu = ref(false)
const showPlusMenu = ref(false) // 搜索栏旁的 + 下拉菜单
// 添加朋友弹窗相关状态
const showAddFriendModal = ref(false)
const addFriendKeyword = ref('')
const addFriendResult = ref(undefined) // undefined=未搜索, null=无结果, object=结果
const addFriendSearched = ref(false)
const isSearchingUser = ref(false)
const isSendingApply = ref(false)
// 加入群聊弹窗相关状态
const showJoinGroupModal = ref(false)
const joinGroupKeyword = ref('')
const joinGroupSearched = ref(false)
const isSearchingGroup = ref(false)
const isApplyingGroup = ref(false)
const joinGroupResults = ref([])
// 通知相关状态
const notifications = ref({ friendApplies: [], groupApplies: [] })
const notifyLoading = ref(false)
const isRecording = ref(false)
const recordingDuration = ref(0)
// 弹出层定位样式（用于把 profile popover 渲染到 body 并根据头像位置定位）
const popoverStyle = ref({})
// 弹出层当前所在侧：'right' 或 'left'
const popoverSide = ref('right')

// 文档级点击与键盘处理，用于关闭浮层
const handleDocClick = (e) => {
  const pop = document.querySelector('.profile-popover')
  if (pop && !pop.contains(e.target)) {
    showProfilePopover.value = false
  }
}
const handleDocKeydown = (e) => {
  if (e.key === 'Escape') showProfilePopover.value = false
}

watch(showProfilePopover, (visible) => {
  if (visible) {
    document.addEventListener('click', handleDocClick)
    document.addEventListener('keydown', handleDocKeydown)
  } else {
    document.removeEventListener('click', handleDocClick)
    document.removeEventListener('keydown', handleDocKeydown)
  }
})

// 好友和群聊（从后端 API 加载，初始为空）
const friends = ref([])

const groups = ref([])

const selectedFriendTab = ref('friends') // 'friends' | 'groups'

// 移动端状态
const currentMobileChat = ref(null)
const mobileCurrentTab = ref('chat') // 'chat' | 'contact' | 'profile'
const showMobileSearch = ref(false)
const mobileSearchText = ref('')
const showEmojiMobile = ref(false)

let mediaRecorder = null
let audioStream = null
let recordingTimer = null
const isSending = ref(false)
let msgIdCounter = 0

// 计算属性
const totalUnread = computed(() => {
  return sessions.value.reduce((sum, item) => sum + (item.unreadCount || 0), 0)
})

// 待处理通知数
const pendingNotifyCount = computed(() => {
  const fCount = notifications.value.friendApplies.filter(a => a.status === 0).length
  const gCount = notifications.value.groupApplies.length
  return fCount + gCount
})

const messagesWithTime = computed(() => {
  const list = []
  const messagesList = messages.value
  const TIME_GAP = 3 * 60 * 1000

  for (let i = 0; i < messagesList.length; i++) {
    const currentMsg = messagesList[i]
    const prevMsg = messagesList[i - 1]

    if (i === 0) {
      const timeText = formatMessageTime(currentMsg.send_time)
      list.push({ type: 'time', timeText })
    } else {
      const currentTime = new Date(currentMsg.send_time).getTime()
      const prevTime = new Date(prevMsg.send_time).getTime()
      if (
        !isNaN(currentTime) &&
        !isNaN(prevTime) &&
        currentTime - prevTime > TIME_GAP
      ) {
        const timeText = formatMessageTime(currentMsg.send_time)
        list.push({ type: 'time', timeText })
      }
    }
    list.push({ type: 'msg', msg: currentMsg })
  }
  return list
})

// 移动端会话过滤
const filteredMobileSessions = computed(() => {
  if (!mobileSearchText.value) return sessions.value
  return sessions.value.filter((s) =>
    s.sessionName.toLowerCase().includes(mobileSearchText.value.toLowerCase()),
  )
})
// PC 会话搜索结果（实时根据输入过滤）
const filteredSessions = computed(() => {
  // 如果搜索框为空则返回全部
  if (!sidebarSearchText.value) return sessions.value
  const q = sidebarSearchText.value.toLowerCase()
  return sessions.value.filter((s) => s.sessionName.toLowerCase().includes(q))
})

// 通讯录搜索结果（实时响应输入 `contactSearchText`）
const filteredFriends = computed(() => {
  if (!contactSearchText.value) return friends.value
  const q = contactSearchText.value.toLowerCase()
  return friends.value.filter((f) => f.nickname.toLowerCase().includes(q))
})
const filteredGroups = computed(() => {
  if (!contactSearchText.value) return groups.value
  const q = contactSearchText.value.toLowerCase()
  return groups.value.filter((g) => g.groupName.toLowerCase().includes(q))
})

// 工具方法
const formatMessageTime = (timeStr) => {
  if (!timeStr) return ''
  const msgTime = new Date(timeStr)
  if (isNaN(msgTime.getTime())) return ''

  const now = new Date()
  const oneDay = 24 * 60 * 60 * 1000

  const isToday = msgTime.toDateString() === now.toDateString()
  if (isToday) return msgTime.toTimeString().slice(0, 5)

  const yesterday = new Date(now.getTime() - oneDay)
  const isYesterday = msgTime.toDateString() === yesterday.toDateString()
  if (isYesterday) return `昨天 ${msgTime.toTimeString().slice(0, 5)}`

  const isThisYear = msgTime.getFullYear() === now.getFullYear()
  if (isThisYear)
    return `${msgTime.getMonth() + 1}-${msgTime.getDate()} ${msgTime.toTimeString().slice(0, 5)}`

  return `${msgTime.getFullYear()}-${msgTime.getMonth() + 1}-${msgTime.getDate()}`
}

const isMyMessage = (msg) => {
  if (!msg) return false
  const msgSenderId = String(
    msg.sender_id || msg.senderId || msg.sender || '',
  ).trim()
  const currentUserId = String(CURRENT_USER_ID.value || '').trim()
  return msgSenderId === currentUserId && msgSenderId !== ''
}

const generateUniqueId = () => {
  return 'msg_' + Date.now().toString(36) + ++msgIdCounter
}

const cleanMessage = (rawMsg, defaultContent = '', forceMessageType = null) => {
  if (!rawMsg) rawMsg = {}
  const rawSenderId = rawMsg.senderId || rawMsg.sender_id || ''
  const senderId = rawSenderId.trim()
    ? String(rawSenderId).trim()
    : String(CURRENT_USER_ID.value || '').trim()
  const senderName = rawMsg.senderName || rawMsg.sender_name || ''

  const sendTime =
    rawMsg.sendTime || rawMsg.send_time || new Date().toISOString()
  const msgType = Number(
    forceMessageType ||
      rawMsg.messageType ||
      rawMsg.message_type ||
      MESSAGE_TYPE.TEXT,
  )
  const fileUrl = rawMsg.fileUrl || rawMsg.file_url || null
  const fileName = rawMsg.fileName || rawMsg.file_name || null
  const fileSize = Number(rawMsg.fileSize || rawMsg.file_size || 0)

  return {
    id: rawMsg.id || rawMsg.messageId || Date.now() + ++msgIdCounter,
    messageNo: rawMsg.messageNo || generateUniqueId(),
    session_id: String(
      rawMsg.sessionId ||
        rawMsg.session_id ||
        currentSession.value?.sessionId ||
        '',
    ),
    session_type: Number(rawMsg.sessionType || rawMsg.session_type || 1),
    sender_id: senderId,
    sender_name: senderName,
    receiver_id: rawMsg.receiverId || rawMsg.receiver_id || '',
    message_type: msgType,
    content: rawMsg.content || defaultContent,
    content_replaced: rawMsg.content_replaced || '',
    send_time: sendTime,
    file_url: fileUrl,
    local_file_url: rawMsg.local_file_url || null,
    file_name: fileName,
    file_size: fileSize,
    duration: rawMsg.duration ? Number(rawMsg.duration) : null,
    send_status: rawMsg.send_status || SEND_STATUS.SUCCESS,
    is_downloading: rawMsg.is_downloading || false,
    download_progress: rawMsg.download_progress || 0,
    avatar: isMyMessage({ sender_id: senderId })
      ? userInfo.value.avatar || selfAvatar
      : targetAvatar,
  }
}

const updateMsgInList = (updatedMsg) => {
  const idx = messages.value.findIndex(
    (m) => m.id === updatedMsg.id || m.messageNo === updatedMsg.messageNo,
  )
  if (idx !== -1) {
    messages.value[idx] = { ...messages.value[idx], ...updatedMsg }
  }
}
// 获取当前聊天对应的好友对象（若是单聊）
const getFriendForCurrentSession = () => {
  if (!currentSession.value) return null
  if (currentSession.value.sessionType !== SESSION_TYPE.SINGLE) return null
  const targetId = String(
    currentSession.value.targetId || currentSession.value.targetId,
  )
  return (
    friends.value.find((f) => String(f.userId) === String(targetId)) || null
  )
}

const friendIsStarText = computed(() => {
  const f = getFriendForCurrentSession()
  if (!f) return '设为星标朋友'
  return f.isStar ? '取消星标朋友' : '设为星标朋友'
})

const toggleChatMenu = () => {
  showChatMenu.value = !showChatMenu.value
}

const openSetRemark = () => {
  const f = getFriendForCurrentSession()
  if (!f) return uni.showToast({ title: '非好友会话', icon: 'none' })
  // 复用编辑弹窗：先把 currentContact 填上，再打开编辑
  selectFriend(f)
  openEditProfile()
  showChatMenu.value = false
}

const setFriendPermissions = () => {
  const f = getFriendForCurrentSession()
  if (!f) return uni.showToast({ title: '非好友会话', icon: 'none' })
  f.permissions = f.permissions === 'restricted' ? 'normal' : 'restricted'
  uni.showToast({ title: `已设置权限：${f.permissions}`, icon: 'none' })
  showChatMenu.value = false
}

const recommendFriend = () => {
  const f = getFriendForCurrentSession()
  if (!f) return uni.showToast({ title: '非好友会话', icon: 'none' })
  uni.showToast({
    title: `已把 ${f.nickname} 推荐给朋友（本地模拟）`,
    icon: 'success',
  })
  showChatMenu.value = false
}

const toggleStarFriend = () => {
  const f = getFriendForCurrentSession()
  if (!f) return uni.showToast({ title: '非好友会话', icon: 'none' })
  f.isStar = !f.isStar
  uni.showToast({
    title: f.isStar ? '已设为星标' : '已取消星标',
    icon: 'success',
  })
  showChatMenu.value = false
}

const addToBlacklist = () => {
  const f = getFriendForCurrentSession()
  if (!f) return uni.showToast({ title: '非好友会话', icon: 'none' })
  // 标记并从好友列表移除（本地模拟）
  f.isBlacklisted = true
  blacklist.value.push(f)
  friends.value = friends.value.filter((ff) => ff.userId !== f.userId)
  // 移除与该联系人的会话
  sessions.value = sessions.value.filter((s) => s.targetId !== f.userId)
  if (currentSession.value && currentSession.value.targetId === f.userId) {
    currentSession.value = null
    messages.value = []
  }
  uni.showToast({ title: `${f.nickname} 已加入黑名单（本地）`, icon: 'none' })
  showChatMenu.value = false
}

const deleteContact = () => {
  const f = getFriendForCurrentSession()
  if (!f) return uni.showToast({ title: '非好友会话', icon: 'none' })
  uni.showModal({
    title: '确认删除',
    content: `确定要删除联系人 ${f.nickname} 吗？此操作只影响本地模拟数据。`,
    success: (res) => {
      if (res.confirm) {
        friends.value = friends.value.filter((ff) => ff.userId !== f.userId)
        sessions.value = sessions.value.filter((s) => s.targetId !== f.userId)
        if (
          currentSession.value &&
          currentSession.value.targetId === f.userId
        ) {
          currentSession.value = null
          messages.value = []
        }
        currentContact.value = null
        uni.showToast({ title: '联系人已删除（本地）', icon: 'success' })
      }
    },
  })
  showChatMenu.value = false
}

// 新增：聊天信息面板控制与操作
const toggleChatInfoPanel = () => {
  showChatInfoPanel.value = !showChatInfoPanel.value
  // 关闭个人菜单，避免冲突
  showChatMenu.value = false
}

const findChatContent = () => {
  showChatInfoPanel.value = false
  uni.showToast({ title: '查找聊天内容（本地模拟）', icon: 'none' })
}

const toggleDoNotDisturb = () => {
  doNotDisturb.value = !doNotDisturb.value
  uni.showToast({
    title: doNotDisturb.value ? '已开启免打扰' : '已关闭免打扰',
    icon: 'none',
  })
}

const togglePinChat = () => {
  pinned.value = !pinned.value
  if (currentSession.value) {
    const idx = sessions.value.findIndex(
      (s) => s.sessionId === currentSession.value.sessionId,
    )
    if (idx !== -1) {
      sessions.value[idx].isTop = pinned.value ? 1 : 0
      // 重新排序：置顶优先
      sessions.value = [...sessions.value].sort((a, b) => {
        if ((a.isTop || 0) !== (b.isTop || 0))
          return (b.isTop || 0) - (a.isTop || 0)
        return new Date(b.lastMessageTime) - new Date(a.lastMessageTime)
      })
    }
  }
  uni.showToast({ title: pinned.value ? '已置顶' : '取消置顶', icon: 'none' })
}

const clearChatRecords = async () => {
  if (!currentSession.value)
    return uni.showToast({ title: '无会话', icon: 'none' })
  const confirmed = await new Promise((resolve) => {
    uni.showModal({
      title: '清空聊天记录',
      content: '确认清空当前会话的聊天记录？此操作仅影响本地模拟数据。',
      success: (res) => resolve(res.confirm),
    })
  })
  if (!confirmed) return
  // 本地清空
  messages.value = []
  // 若有 ChatStorage，尝试删除记录
  if (ChatStorage && ChatStorage.deleteMessages) {
    try {
      await ChatStorage.deleteMessages(currentSession.value.sessionId)
    } catch (e) {
      console.warn('ChatStorage.deleteMessages 异常', e)
    }
  }
  uni.showToast({ title: '聊天记录已清空（本地）', icon: 'success' })
  showChatInfoPanel.value = false
}

const openProfileFromPanel = (eventOrFriend, maybeFriend, maybeFriendId) => {
  // 支持两种调用：openProfileFromPanel($event, friend) 或 openProfileFromPanel(friend)
  let ev = null
  let friend = null
  if (maybeFriend) {
    ev = eventOrFriend
    friend = maybeFriend
  } else {
    friend = eventOrFriend
  }
  if (!friend) return uni.showToast({ title: '无用户信息', icon: 'none' })
  currentContact.value = {
    sessionName: friend.nickname,
    avatar: friend.avatar,
    type: 'person',
    targetId: friend.userId,
    desc: friend.signature,
  }
  // 显示独立于面板的靠近头像的小弹出层，隐藏其它浮层但不依赖 chat-info-panel
  showProfilePopover.value = true
  showProfileModal.value = false
  showChatMenu.value = false
  // 计算位置：优先使用 friendId 或 event 目标的 DOM 获取位置，根据空间决定左侧或右侧显示
  nextTick(() => {
    const POP_width = 240 // 缩小一点宽度
    const margin = 12
    let leftPos = null
    let topPos = null
    try {
      let rect = null
      if (maybeFriendId) {
        const el = document.getElementById('profile-avatar-' + maybeFriendId)
        if (el && el.getBoundingClientRect) rect = el.getBoundingClientRect()
      }
      if (
        !rect &&
        ev &&
        ev.currentTarget &&
        ev.currentTarget.getBoundingClientRect
      ) {
        rect = ev.currentTarget.getBoundingClientRect()
      }
      if (!rect && ev && ev.target && ev.target.getBoundingClientRect) {
        rect = ev.target.getBoundingClientRect()
      }
      if (rect) {
        topPos = rect.top + window.scrollY - 6
        // 如果右侧空间不足则放左边
        if (rect.right + POP_width + margin > window.innerWidth) {
          popoverSide.value = 'left'
          leftPos = rect.left + window.scrollX - POP_width - margin
        } else {
          popoverSide.value = 'right'
          leftPos = rect.right + window.scrollX + margin
        }
      }
    } catch (e) {
      // ignore
    }
    if (leftPos !== null && topPos !== null) {
      // 保证不出界
      leftPos = Math.max(
        margin,
        Math.min(leftPos, window.innerWidth - POP_width - margin),
      )
      popoverStyle.value = {
        position: 'absolute',
        left: `${leftPos}px`,
        top: `${topPos}px`,
      }
    } else {
      // 兜底位置
      popoverSide.value = 'right'
      popoverStyle.value = { position: 'absolute', right: '20px', top: '60px' }
    }
  })
}

const closeProfileModal = () => {
  showProfileModal.value = false
}

// 【修复2：彻底移除自动下载逻辑，只保留空壳防止报错】
const autoDownloadFiles = (msgList) => {
  console.log('[Auto Download] 已关闭自动下载功能')
}

const formatFileSize = (bytes) => {
  if (!bytes) return '0B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

// 导航
const switchToProfile = () => {
  currentSidebarTab.value = 'profile'
  showMoreMenu.value = false
  showEmojiPanel.value = false
}
const toggleMoreMenu = () => {
  showMoreMenu.value = !showMoreMenu.value
  showEmojiPanel.value = false
}
const closeMoreMenu = () => {
  showMoreMenu.value = false
  showPlusMenu.value = false
  // 优先关闭头像弹出层
  if (showProfilePopover.value) {
    showProfilePopover.value = false
    return
  }
  // 关闭聊天相关面板
  showChatMenu.value = false
  showChatInfoPanel.value = false
}

// ➕ 下拉菜单控制
const togglePlusMenu = () => {
  showPlusMenu.value = !showPlusMenu.value
  showMoreMenu.value = false
}

// 打开添加朋友弹窗
const openAddFriend = () => {
  showPlusMenu.value = false
  addFriendKeyword.value = ''
  addFriendResult.value = undefined
  addFriendSearched.value = false
  isSearchingUser.value = false
  isSendingApply.value = false
  showAddFriendModal.value = true
}

// 关闭添加朋友弹窗
const closeAddFriend = () => {
  showAddFriendModal.value = false
  addFriendKeyword.value = ''
  addFriendResult.value = undefined
  addFriendSearched.value = false
}

// 清除搜索输入
const clearAddFriendSearch = () => {
  addFriendKeyword.value = ''
  addFriendResult.value = undefined
  addFriendSearched.value = false
}

// 搜索用户
const searchUserForAdd = async () => {
  const keyword = addFriendKeyword.value.trim()
  if (!keyword) return
  isSearchingUser.value = true
  addFriendSearched.value = true
  addFriendResult.value = undefined
  try {
    const res = await service.get('/user/search', { params: { keyword } })
    const user = res.data
    if (!user) {
      addFriendResult.value = null
      return
    }
    // 检查是否已是好友
    const alreadyFriend = friends.value.some((f) => f.userId === String(user.id))
    addFriendResult.value = {
      userId: String(user.id),
      username: user.username,
      nickname: user.nickname || user.username,
      avatar: user.avatar || '',
      signature: user.signature || '',
      isFriend: alreadyFriend,
      isApplied: false,
    }
  } catch (e) {
    // 搜索失败列为未找到
    addFriendResult.value = null
    console.warn('搜索用户失败', e)
  } finally {
    isSearchingUser.value = false
  }
}

// 发送好友申请
const sendFriendApply = async () => {
  if (!addFriendResult.value || isSendingApply.value) return
  isSendingApply.value = true
  try {
    await service.post('/friend/apply', {
      targetId: addFriendResult.value.userId,
      remark: '',
    })
    addFriendResult.value = { ...addFriendResult.value, isApplied: true }
    uni.showToast({ title: '好友申请已发送', icon: 'success' })
  } catch (e) {
    console.warn('发送好友申请失败', e)
  } finally {
    isSendingApply.value = false
  }
}

const openCreateGroup = () => {
  showPlusMenu.value = false
  uni.showToast({ title: '发起群聊功能开发中', icon: 'none' })
}

// 切换通讯录 tab（切到通知时自动刷新）
const switchContactTab = (tab) => {
  selectedFriendTab.value = tab
  if (tab === 'notify') {
    loadNotifications()
  } else if (tab === 'friends') {
    loadFriends()
  } else if (tab === 'groups') {
    loadGroups()
  }
}

// 加载好友列表
const loadFriends = async () => {
  try {
    const res = await service.get('/friend/list')
    if (res.code === 200 && Array.isArray(res.data)) {
      friends.value = res.data.map(f => ({
        userId: String(f.applicantId),
        nickname: f.applicantNickname || f.applicantId,
        avatar: f.applicantAvatar || '',
        signature: f.remark || '',
        status: 1,
        isFriend: true,
      }))
    }
  } catch (e) {
    console.error('加载好友列表失败', e)
  }
}

// 加载群聊列表
const loadGroups = async () => {
  try {
    const res = await service.get('/group/list')
    if (res.code === 200 && Array.isArray(res.data)) {
      groups.value = res.data.map(g => ({
        groupId: g.id,
        groupName: g.groupName,
        groupAvatar: g.groupAvatar || '',
        memberCount: g.currentMemberCount || 0,
        creatorId: g.creatorId,
        myRole: g.myRole,
        status: 1,
      }))
    }
  } catch (e) {
    console.error('加载群聊列表失败', e)
  }
}

// 加载通知（好友申请 + 群聊申请）
const loadNotifications = async () => {
  notifyLoading.value = true
  try {
    const [friendRes, groupRes] = await Promise.all([
      service.get('/friend/apply/received'),
      service.get('/group/apply/received'),
    ])
    notifications.value.friendApplies = (friendRes.code === 200 && Array.isArray(friendRes.data))
      ? friendRes.data
      : []
    notifications.value.groupApplies = (groupRes.code === 200 && Array.isArray(groupRes.data))
      ? groupRes.data
      : []
  } catch (e) {
    console.error('加载通知失败', e)
  } finally {
    notifyLoading.value = false
  }
}

// 处理好友申请
const handleFriendApply = async (applyId, status) => {
  try {
    await service.post('/friend/apply/handle', { applyId, status, rejectReason: '' })
    // 就地更新状态
    const idx = notifications.value.friendApplies.findIndex(a => a.id === applyId)
    if (idx !== -1) {
      notifications.value.friendApplies[idx] = {
        ...notifications.value.friendApplies[idx],
        status,
      }
    }
    uni.showToast({ title: status === 1 ? '已接受好友申请' : '已拒绝', icon: 'success' })
    // 接受后刷新好友列表
    if (status === 1) loadFriends()
  } catch (e) {
    uni.showToast({ title: '操作失败', icon: 'none' })
    console.error('处理好友申请失败', e)
  }
}

// 处理群聊申请
const handleGroupApply = async (applyId, status) => {
  try {
    await service.post('/group/apply/handle', { applyId, status, rejectReason: '' })
    // 就地移除已处理的申请
    notifications.value.groupApplies = notifications.value.groupApplies.filter(a => a.id !== applyId)
    uni.showToast({ title: status === 1 ? '已同意入群' : '已拒绝', icon: 'success' })
    if (status === 1) loadGroups()
  } catch (e) {
    uni.showToast({ title: '操作失败', icon: 'none' })
    console.error('处理群聊申请失败', e)
  }
}

// 加入群聊弹窗相关
const openJoinGroup = () => {
  showPlusMenu.value = false
  joinGroupKeyword.value = ''
  joinGroupResults.value = []
  joinGroupSearched.value = false
  isSearchingGroup.value = false
  isApplyingGroup.value = false
  showJoinGroupModal.value = true
}

const closeJoinGroup = () => {
  showJoinGroupModal.value = false
  joinGroupKeyword.value = ''
  joinGroupResults.value = []
  joinGroupSearched.value = false
}

const clearJoinGroupSearch = () => {
  joinGroupKeyword.value = ''
  joinGroupResults.value = []
  joinGroupSearched.value = false
}

const searchGroupForJoin = async () => {
  const keyword = joinGroupKeyword.value.trim()
  if (!keyword) return
  isSearchingGroup.value = true
  joinGroupSearched.value = true
  joinGroupResults.value = []
  try {
    const res = await service.get('/group/search', { params: { keyword } })
    if (res.code === 200 && Array.isArray(res.data)) {
      joinGroupResults.value = res.data
    }
  } catch (e) {
    console.warn('搜索群聊失败', e)
  } finally {
    isSearchingGroup.value = false
  }
}

const applyJoinGroupFn = async (group) => {
  if (isApplyingGroup.value) return
  isApplyingGroup.value = true
  try {
    await service.post('/group/apply', { groupId: group.id, remark: '' })
    // 更新该群在结果列表中的状态
    const idx = joinGroupResults.value.findIndex(g => g.id === group.id)
    if (idx !== -1) {
      joinGroupResults.value[idx] = { ...joinGroupResults.value[idx], applyStatus: 'pending' }
    }
    uni.showToast({
      title: group.joinType === 2 ? '已入群！' : '申请已提交，请等待审核',
      icon: group.joinType === 2 ? 'success' : 'none',
    })
    if (group.joinType === 2) {
      // 免审核直接加入，刷新群聊列表
      loadGroups()
    }
  } catch (e) {
    uni.showToast({ title: '申请失败', icon: 'none' })
    console.warn('申请加入群聊失败', e)
  } finally {
    isApplyingGroup.value = false
  }
}

const openChatFiles = () => {
  uni.showToast({ title: '功能开发中', icon: 'none' })
}
const manageChatHistory = () => {
  uni.showToast({ title: '功能开发中', icon: 'none' })
}
const loadHistoryChat = () => {
  uni.showToast({ title: '功能开发中', icon: 'none' })
}
const lockApp = () => {
  uni.showToast({ title: '功能开发中', icon: 'none' })
}
const openFeedback = () => {
  uni.showToast({ title: '功能开发中', icon: 'none' })
}
const openSettings = () => {
  uni.showToast({ title: '功能开发中', icon: 'none' })
}

// 退出登录
const handleLogout = async () => {
  try {
    await service.post('/user/logout')
  } catch (e) {
    console.warn('退出登录接口异常', e)
  } finally {
    uni.removeStorageSync('token')
    uni.redirectTo({ url: '/pages/index/index' })
  }
}
const switchSidebarTab = (tab) => {
  currentSidebarTab.value = tab
  showMoreMenu.value = false
  showEmojiPanel.value = false
}
const selectContact = (item) => {
  currentContact.value = item
}
const toggleGroupExpand = (item) => {
  item.expanded = !item.expanded
}

// 选择好友用于显示详情
const selectFriend = (friend) => {
  currentContact.value = {
    sessionName: friend.nickname,
    avatar: friend.avatar,
    type: 'person',
    targetId: friend.userId,
    desc: friend.signature,
  }
}

// 选择群用于显示详情
const selectGroup = (group) => {
  currentContact.value = {
    sessionName: group.groupName,
    avatar: group.groupAvatar,
    type: 'group',
    targetId: group.groupId,
    desc: `${group.memberCount} 个成员`,
  }
}

const chatWithFriend = (friend) => {
  const myId = CURRENT_USER_ID.value || ''
  const sessionId = `${Math.min(Number(myId), Number(friend.userId))}_${Math.max(Number(myId), Number(friend.userId))}`
  let session = sessions.value.find((s) => s.sessionId === sessionId)
  if (!session) {
    session = {
      sessionId,
      sessionName: friend.nickname,
      sessionAvatar: friend.avatar,
      targetType: 'person',
      targetId: friend.userId,
      lastMessageContent: '',
      lastMessageTime: new Date().toISOString(),
      unreadCount: 0,
      sessionType: SESSION_TYPE.SINGLE,
    }
    sessions.value.unshift(session)
  }
  currentSidebarTab.value = 'chat'
  switchSession(session)
}

const chatWithGroup = (group) => {
  const sessionId = `group_${group.groupId}`
  let session = sessions.value.find((s) => s.sessionId === sessionId)
  if (!session) {
    session = {
      sessionId,
      sessionName: group.groupName,
      sessionAvatar: group.groupAvatar,
      targetType: 'group',
      targetId: group.groupId,
      lastMessageContent: '',
      lastMessageTime: new Date().toISOString(),
      unreadCount: 0,
      sessionType: SESSION_TYPE.GROUP,
    }
    sessions.value.unshift(session)
  }
  currentSidebarTab.value = 'chat'
  switchSession(session)
}

// 通用详情页聊天按钮
const chatWithContact = () => {
  if (!currentContact.value) return
  if (currentContact.value.type === 'person') {
    chatWithFriend({
      userId: currentContact.value.targetId,
      nickname: currentContact.value.sessionName,
      avatar: currentContact.value.avatar,
    })
  } else if (currentContact.value.type === 'group') {
    chatWithGroup({
      groupId: currentContact.value.targetId,
      groupName: currentContact.value.sessionName,
      groupAvatar: currentContact.value.avatar,
    })
  }
}
const switchSession = async (item) => {
  currentSession.value = item
  pendingFiles.value = []
  inputMsg.value = ''
  try {
    await service.put(`/session/read/${item.sessionId}`)
    const idx = sessions.value.findIndex((s) => s.sessionId === item.sessionId)
    if (idx !== -1) sessions.value[idx].unreadCount = 0
  } catch (e) {
    console.error('清除未读失败', e)
  }
  loadMessages(item.sessionId)
}

// 加载当前登录用户信息
const loadCurrentUser = async () => {
  try {
    const res = await service.get('/user/info')
    if (res.code === 200 && res.data) {
      const info = res.data
      CURRENT_USER_ID.value = info.id
      userInfo.value = {
        id: info.id,
        nickname: info.nickname || '',
        avatar: info.avatar || selfAvatar,
        wechatId: info.username || '',
        region: info.region || '',
        signature: info.signature || '',
      }
      saveUserInfoToStorage(userInfo.value)
    }
  } catch (e) {
    console.error('加载用户信息失败', e)
  }
}

// 数据加载
const loadSessionList = async () => {
  try {
    const res = await service.get('/session/list')
    if (res.code === 200 && Array.isArray(res.data)) {
      sessions.value = res.data
    } else {
      sessions.value = []
    }
    if (!currentSession.value && sessions.value.length > 0) {
      currentSession.value = sessions.value[0]
      loadMessages(currentSession.value.sessionId)
    }
  } catch (e) {
    console.error('拉取会话列表失败', e)
    uni.showToast({ title: '会话列表加载失败', icon: 'none' })
  }
}

// 【修复3：确保会话列表响应式更新，直接修改原数组项】
const updateSessionLastMsg = (sessionId, content, sendTime) => {
  const idx = sessions.value.findIndex((s) => s.sessionId === sessionId)
  if (idx !== -1) {
    // 使用 Vue.set 或直接替换数组确保响应式
    const updatedSession = {
      ...sessions.value[idx],
      lastMessageContent: content,
      lastMessageTime: sendTime,
      lastMessageSenderId: CURRENT_USER_ID.value,
    }
    sessions.value.splice(idx, 1, updatedSession)
    // 重新排序
    sessions.value = [...sessions.value].sort((a, b) => {
      if (a.isTop !== b.isTop) return b.isTop - a.isTop
      return new Date(b.lastMessageTime) - new Date(a.lastMessageTime)
    })
  }
}

const loadMessages = async (sessionId) => {
  if (!sessionId) return
  try {
    // 《第一步》优先读取本地缓存，实现即时渲染，无散头的加载空白
    const localMsgs = await ChatStorage.queryMessages(sessionId)
    if (localMsgs.length > 0) {
      messages.value = localMsgs.map((item) => cleanMessage(item))
      await nextTick()
      scrollToBottom()
    }

    // 《第二步》后台拉取服务端数据（以服务端为权威来源）
    const res = await service.get('/chat/message/list', {
      params: { sessionId },
    })
    if (res.code === 200 && Array.isArray(res.data) && res.data.length > 0) {
      // 《第三步》将服务端数据写入本地缓存
      await ChatStorage.insertMessages(sessionId, res.data)
      // 《第四步》用服务端最新数据更新视图
      const cleanedList = res.data.map((item) => cleanMessage(item))
      messages.value = cleanedList
      autoDownloadFiles(cleanedList)
    }
  } catch (e) {
    console.error('加载消息失败', e)
    // 网络异常时，本地数据已在第一步展示，无需重复读取
  } finally {
    await nextTick()
    scrollToBottom()
  }
}

// 滚动
const onChatScroll = (e) => {
  const scrollTop = e.detail.scrollTop
  const scrollHeight = e.detail.scrollHeight
  const clientHeight = e.detail.clientHeight
  showScrollToBottom.value = scrollHeight - scrollTop - clientHeight > 100
}
const scrollToBottom = async () => {
  await nextTick()
  if (messages.value.length > 0) {
    const lastMsg = messages.value[messages.value.length - 1]
    scrollToView.value = 'msg-' + lastMsg.id
  }
  setTimeout(() => {
    scrollToView.value = ''
  }, 200)
}

// 媒体处理
const handleImageClick = (msg) => {
  const url = msg.local_file_url || msg.file_url
  if (url) {
    // #ifdef H5
    uni.previewImage({ urls: [url] })
    // #endif
  }
}
const handleVideoClick = (msg) => {
  console.log('点击视频', msg)
}
const handleVoiceClick = (msg) => {
  playVoice(msg)
}
const handleFileClick = (msg) => {
  console.log('[Manual Click] 触发下载:', msg)
  // 【修复4：手动点击下载，传入 false 表示非自动触发】
  handleMsgDownload(msg, updateMsgInList, false)
}
const playVoice = (msg) => {
  const url = msg.local_file_url || msg.file_url
  if (!url) return uni.showToast({ title: '语音不存在', icon: 'none' })
  // #ifdef H5
  new Audio(url)
    .play()
    .catch(() => uni.showToast({ title: '播放失败', icon: 'none' }))
  // #endif
}

// 输入与表情
const handleEnterKey = (e) => {
  if (!e.ctrlKey) {
    e.preventDefault()
    sendMessageWithFiles()
  }
}
const handleCtrlEnter = (e) => {
  inputMsg.value += '\n'
}
const toggleEmojiPanel = () => {
  showEmojiPanel.value = !showEmojiPanel.value
  showMoreMenu.value = false
}
const insertEmoji = (emoji) => {
  inputMsg.value += emoji
  let list = recentEmojis.value.filter((e) => e !== emoji)
  list.unshift(emoji)
  recentEmojis.value = list.slice(0, 8)
}

// 文件选择
const chooseImage = () => {
  // #ifdef H5
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = (e) => {
    const file = e.target.files[0]
    if (file) {
      const fileUrl = URL.createObjectURL(file)
      pendingFiles.value.push({
        message_type: MESSAGE_TYPE.IMAGE,
        content: '[图片]',
        file_url: fileUrl,
        file_name: file.name,
        file_size: file.size,
        originalFile: file,
      })
    }
  }
  input.click()
  // #endif
}
const chooseFile = () => {
  // #ifdef H5
  const input = document.createElement('input')
  input.type = 'file'
  input.onchange = (e) => {
    const file = e.target.files[0]
    if (file) {
      const fileUrl = URL.createObjectURL(file)
      pendingFiles.value.push({
        message_type: MESSAGE_TYPE.FILE,
        content: '[文件]',
        file_url: fileUrl,
        file_name: file.name,
        file_size: file.size,
        originalFile: file,
      })
    }
  }
  input.click()
  // #endif
}
const removePendingFile = (index) => {
  pendingFiles.value.splice(index, 1)
}
const onInputDragEnter = (e) => {
  e.preventDefault()
  isInputDragOver.value = true
}
const onInputDragOver = (e) => {
  e.preventDefault()
  isInputDragOver.value = true
}
const onInputDragLeave = (e) => {
  e.preventDefault()
  isInputDragOver.value = false
}
const onInputDrop = (e) => {
  e.preventDefault()
  isInputDragOver.value = false
  // #ifdef H5
  const files = e.dataTransfer?.files
  if (!files) return
  Array.from(files).forEach((file) => {
    const fileUrl = URL.createObjectURL(file)
    let msgType = MESSAGE_TYPE.FILE
    if (file.type.startsWith('image/')) msgType = MESSAGE_TYPE.IMAGE
    else if (file.type.startsWith('video/')) msgType = MESSAGE_TYPE.VIDEO
    pendingFiles.value.push({
      message_type: msgType,
      content: `[${msgType === MESSAGE_TYPE.IMAGE ? '图片' : msgType === MESSAGE_TYPE.VIDEO ? '视频' : '文件'}]`,
      file_url: fileUrl,
      file_name: file.name,
      file_size: file.size,
      originalFile: file,
    })
  })
  // #endif
}

// 语音
const toggleVoiceRecording = () => {
  isRecording.value ? cancelVoice() : startVoiceRecording()
}
const startVoiceRecording = async () => {
  // #ifdef H5
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    audioStream = stream
    const chunks = []
    mediaRecorder = new MediaRecorder(stream)
    mediaRecorder.ondataavailable = (e) =>
      e.data.size > 0 && chunks.push(e.data)
    mediaRecorder.onstop = () => {
      const audioBlob = new Blob(chunks, { type: 'audio/mpeg' })
      const audioUrl = URL.createObjectURL(audioBlob)
      processVoiceData(
        audioUrl,
        recordingDuration.value * 1000,
        audioBlob.size,
        audioBlob,
      )
    }
    mediaRecorder.start()
    startRecordingTimer()
    isRecording.value = true
  } catch (e) {
    uni.showToast({ title: '请允许麦克风权限', icon: 'none' })
  }
  // #endif
}
const stopAndSendVoice = () => {
  if (!isRecording.value) return
  stopRecordingTimer()
  isRecording.value = false
  // #ifdef H5
  if (mediaRecorder && mediaRecorder.state !== 'inactive') mediaRecorder.stop()
  if (audioStream) audioStream.getTracks().forEach((t) => t.stop())
  // #endif
}
const cancelVoice = () => {
  isRecording.value = false
  stopRecordingTimer()
  if (audioStream) audioStream.getTracks().forEach((t) => t.stop())
  if (mediaRecorder && mediaRecorder.state !== 'inactive') mediaRecorder.stop()
}
const startRecordingTimer = () => {
  recordingDuration.value = 0
  if (recordingTimer) clearInterval(recordingTimer)
  recordingTimer = setInterval(() => recordingDuration.value++, 1000)
}
const stopRecordingTimer = () => {
  if (recordingTimer) clearInterval(recordingTimer)
}
const processVoiceData = async (tempUrl, duration, fileSize, blob) => {
  if (recordingDuration.value < 1) {
    uni.showToast({ title: '录音时间太短', icon: 'none' })
    return
  }
  const voiceFile = new File([blob], `voice_${Date.now()}.mp3`, {
    type: 'audio/mpeg',
  })
  const voiceData = {
    message_type: MESSAGE_TYPE.AUDIO,
    content: '[语音]',
    file_url: tempUrl,
    file_name: voiceFile.name,
    file_size: fileSize,
    duration: Math.ceil(duration / 1000),
    originalFile: voiceFile,
  }
  isSending.value = true
  try {
    await sendSingleMessageInternal(voiceData)
  } finally {
    isSending.value = false
  }
}

// 发送消息
const sendMessageWithFiles = async () => {
  if (isSending.value || !currentSession.value) return
  const hasText = inputMsg.value.trim()
  const hasFiles = pendingFiles.value.length > 0
  if (!hasText && !hasFiles) return
  isSending.value = true
  try {
    if (hasText) {
      await sendSingleMessageInternal({
        message_type: MESSAGE_TYPE.TEXT,
        content: inputMsg.value,
        originalFile: null,
      })
      inputMsg.value = ''
    }
    for (const file of pendingFiles.value) {
      await sendSingleMessageInternal(file)
    }
    pendingFiles.value = []
  } catch (err) {
    console.error('发送失败', err)
    uni.showToast({ title: '发送失败', icon: 'none' })
  } finally {
    isSending.value = false
  }
}
const unwrapData = (res) => {
  let data = res
  if (data && data.data) data = data.data
  if (data && data.data) data = data.data
  return data
}
const calculateFileMD5 = (file) => {
  return new Promise((resolve, reject) => {
    // #ifdef H5
    if (!file) return reject('No file')
    const reader = new FileReader()
    const spark = new SparkMD5.ArrayBuffer()
    const chunkSize = 2 * 1024 * 1024
    const chunks = Math.ceil(file.size / chunkSize)
    let currentChunk = 0
    reader.onload = (e) => {
      spark.append(e.target.result)
      currentChunk++
      if (currentChunk < chunks) loadNext()
      else resolve(spark.end())
    }
    reader.onerror = () => reject(new Error('MD5 failed'))
    const loadNext = () => {
      const start = currentChunk * chunkSize
      const end = Math.min(start + chunkSize, file.size)
      reader.readAsArrayBuffer(file.slice(start, end))
    }
    loadNext()
    // #endif
    // #ifndef H5
    resolve(`md5_${Date.now()}`)
    // #endif
  })
}

const sendSingleMessageInternal = async (msgData) => {
  const sessionId = currentSession.value.sessionId
  const newId = Date.now() + ++msgIdCounter
  const nowTime = new Date().toISOString()

  const localMsg = cleanMessage({
    id: newId,
    session_id: sessionId,
    message_type: msgData.message_type,
    content: msgData.content,
    local_file_url: msgData.file_url,
    file_url: msgData.file_url,
    file_name: msgData.file_name,
    file_size: msgData.file_size,
    duration: msgData.duration,
    send_time: nowTime,
    send_status: SEND_STATUS.PENDING,
    sender_id: CURRENT_USER_ID.value,
  })

  messages.value.push(localMsg)
  // 【修复5：发送消息立即更新会话列表】
  updateSessionLastMsg(sessionId, msgData.content, nowTime)
  scrollToBottom()

  try {
    let finalFileUrl = null
    let finalFileName = msgData.file_name
    let finalFileSize = msgData.file_size

    if (msgData.originalFile) {
      const file = msgData.originalFile
      if (file.size < 10 * 1024 * 1024) {
        console.log('>> 直传模式')
        const sendDTO = {
          messageNo: localMsg.messageNo,
          sessionId,
          sessionType: currentSession.value.sessionType,
          senderId: CURRENT_USER_ID.value,
          receiverId:
            currentSession.value.sessionType === SESSION_TYPE.SINGLE
              ? currentSession.value.targetId
              : null,
          messageType: msgData.message_type,
          content: msgData.content,
          duration: msgData.duration,
          fileUrl: null,
          fileName: finalFileName,
          fileSize: finalFileSize,
        }
        const formData = new FormData()
        formData.append('sendDTO', JSON.stringify(sendDTO))
        formData.append('files', file)
        const res = await service({
          url: '/chat/message/send',
          method: 'post',
          data: formData,
        })
        const data = unwrapData(res)
        finalFileUrl = data?.fileUrl
        finalFileName = file.name
        finalFileSize = file.size
      } else {
        console.log('>> 切片模式')
        const fileMD5 = await calculateFileMD5(file)
        const totalChunks = Math.ceil(file.size / CHUNK_SIZE)
        console.log('>> [1/4] 检查文件...')
        const checkRes = await service({
          url: '/chat/file/check',
          method: 'post',
          params: { md5: fileMD5, fileName: file.name },
        })
        const checkData = unwrapData(checkRes)
        if (!checkData) throw new Error('检查接口返回数据异常')
        if (!checkData.shouldUpload) {
          finalFileUrl = checkData.fileUrl
        } else {
          console.log('>> [2/4] 上传切片...')
          const uploadedChunks = checkData.uploadedChunks || []
          for (let i = 0; i < totalChunks; i++) {
            if (uploadedChunks.includes(i)) continue
            const start = i * CHUNK_SIZE
            const end = Math.min(start + CHUNK_SIZE, file.size)
            const chunkBlob = file.slice(start, end)
            const formData = new FormData()
            formData.append('md5', fileMD5)
            formData.append('chunkIndex', i)
            formData.append('fileName', file.name)
            formData.append('totalChunks', totalChunks)
            formData.append('fileSize', file.size)
            formData.append('file', chunkBlob)
            await service({
              url: '/chat/file/upload-chunk',
              method: 'post',
              data: formData,
            })
            console.log(`>> 切片 ${i + 1}/${totalChunks} 完成`)
          }
          console.log('>> [3/4] 合并文件...')
          const mergeRes = await service({
            url: '/chat/file/merge',
            method: 'post',
            params: {
              md5: fileMD5,
              fileName: file.name,
              totalChunks,
              isPublic: currentSession.value.sessionType === SESSION_TYPE.GROUP,
            },
          })
          finalFileUrl = unwrapData(mergeRes)
          if (finalFileUrl && typeof finalFileUrl === 'object') {
            finalFileUrl =
              finalFileUrl.url || finalFileUrl.fileUrl || finalFileUrl
          }
        }
        finalFileName = file.name
        finalFileSize = file.size
      }
    }

    if (
      !msgData.originalFile ||
      (msgData.originalFile && msgData.originalFile.size >= 10 * 1024 * 1024)
    ) {
      console.log('>> [4/4] 发送消息通知...')
      const sendDTO = {
        messageNo: localMsg.messageNo,
        sessionId,
        sessionType: currentSession.value.sessionType,
        senderId: CURRENT_USER_ID.value,
        receiverId:
          currentSession.value.sessionType === SESSION_TYPE.SINGLE
            ? currentSession.value.targetId
            : null,
        messageType: msgData.message_type,
        content: msgData.content,
        duration: msgData.duration,
        fileUrl: finalFileUrl,
        fileName: finalFileName,
        fileSize: finalFileSize,
      }
      const formData = new FormData()
      formData.append('sendDTO', JSON.stringify(sendDTO))
      const res = await service({
        url: '/chat/message/send',
        method: 'post',
        data: formData,
      })
      const serverData = unwrapData(res)
      if (serverData) Object.assign(localMsg, serverData)
    }

    const serverMsg = {
      ...localMsg,
      file_url: finalFileUrl,
      send_status: SEND_STATUS.SUCCESS,
    }
    serverMsg.local_file_url = localMsg.local_file_url
    const idx = messages.value.findIndex(
      (m) => m.messageNo === localMsg.messageNo,
    )
    if (idx !== -1) {
      messages.value[idx] = cleanMessage(serverMsg)
      if (ChatStorage.insertMessage)
        await ChatStorage.insertMessage(messages.value[idx])
    }
    console.log('>> 发送流程完成')
  } catch (err) {
    console.error('>> 发送异常:', err)
    const idx = messages.value.findIndex((m) => m.id === newId)
    if (idx !== -1) {
      messages.value[idx].send_status = SEND_STATUS.FAILED
      messages.value = [...messages.value]
    }
    uni.showToast({ title: err.message || '发送失败', icon: 'none' })
  }
}

const performContactSearch = () => {
  contactSearchQuery.value = contactSearchText.value.trim()
  console.log('联系人搜索：', contactSearchQuery.value)
}
const performSessionSearch = () => {
  sidebarSearchQuery.value = sidebarSearchText.value.trim()
  console.log('会话搜索：', sidebarSearchQuery.value)
}

// 切换好友/群时重置搜索内容
watch(selectedFriendTab, () => {
  contactSearchText.value = ''
  contactSearchQuery.value = ''
})
// 切换侧边栏标签时清空所有搜索状态
watch(currentSidebarTab, () => {
  sidebarSearchText.value = ''
  sidebarSearchQuery.value = ''
  contactSearchText.value = ''
  contactSearchQuery.value = ''
})

// 切换聊天列表搜索框（仅针对移动端会话）
const toggleSearch = () => {
  showMobileSearch.value = !showMobileSearch.value
  mobileSearchText.value = ''
}

// H5/Mobile 底部 tab 切换统一方法
const setMobileTab = (tab) => {
  mobileCurrentTab.value = tab
  if (tab === 'chat') {
    currentMobileChat.value = null
  }
}

// ========== 移动端方法 ==========
// #ifndef H5

// 进入聊天详情
const enterMobileChat = (session) => {
  currentMobileChat.value = session
  currentSession.value = session
  messages.value = []
  loadMobileMessages(session.sessionId)
  scrollToView.value = ''
}

// 返回会话列表或退出详情页
const backToMobileList = () => {
  if (currentMobileChat.value) {
    currentMobileChat.value = null
    messages.value = []
    inputMsg.value = ''
    showEmojiMobile.value = false
  } else {
    mobileCurrentTab.value = 'chat'
  }
}

// 加载消息
const loadMobileMessages = (sessionId) => {
  switchSession(currentSession.value)
}

// 切换表情选择器
const toggleMobileEmoji = () => {
  showEmojiMobile.value = !showEmojiMobile.value
}

// 插入表情
const insertMobileEmoji = (emoji) => {
  inputMsg.value += emoji
}

// 选择图片
const chooseMobileImage = () => {
  // #ifndef H5
  uni.chooseImage({
    count: 1,
    sizeType: ['original', 'compressed'],
    success(res) {
      const imagePath = res.tempFilePaths[0]
      pendingFiles.value.push({
        localPath: imagePath,
        name: 'image_' + Date.now() + '.jpg',
        size: 0,
        type: MESSAGE_TYPE.IMAGE,
      })
    },
  })
  // #endif

  // #ifdef H5
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = (e) => {
    const file = e.target.files[0]
    if (file) {
      pendingFiles.value.push({
        localPath: URL.createObjectURL(file),
        name: file.name,
        size: file.size,
        type: MESSAGE_TYPE.IMAGE,
      })
    }
  }
  input.click()
  // #endif
}

// 开始录音
const startMobileRecord = () => {
  console.log('语音录制功能')
}

// #endif

// ========== 生命周期 ==========
onMounted(async () => {
  // #ifdef H5
  initDeviceDetection()
  // #endif

  // 先从后端加载当前登录用户信息，再加载会话列表
  await loadCurrentUser()
  loadSessionList()
  // 并行加载好友、群聊、通知
  loadFriends()
  loadGroups()
  loadNotifications()
})
onUnmounted(() => {
  // #ifdef H5
  cleanupDeviceDetection()
  // #endif

  if (audioStream) audioStream.getTracks().forEach((t) => t.stop())
  if (recordingTimer) clearInterval(recordingTimer)
})
</script>
<style scoped>
/* 全局布局 */
.wechat-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  background: #fff;
  overflow: hidden;
  font-family: 'Microsoft YaHei', Arial, sans-serif;
  position: relative;
}

/* 侧边栏 */
.sidebar {
  width: 60px;
  background: #2c2c2c;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 0;
  flex-shrink: 0;
  position: relative;
  justify-content: space-between;
  height: 100vh;
  box-sizing: border-box;
}
.sidebar-top,
.sidebar-bottom {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  gap: 10px;
}
.sidebar-bottom {
  padding-bottom: 10px;
}
.sidebar-item {
  width: 40px;
  height: 40px;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 22px;
  cursor: pointer;
  border-radius: 8px;
  position: relative;
  transition: all 0.2s;
}
.sidebar-item:hover,
.sidebar-item.active {
  background: #07c160;
}
.sidebar-item.avatar {
  margin-bottom: 10px;
}
.sidebar-item.avatar .avatar-img {
  width: 40px;
  height: 40px;
  border-radius: 8px;
}
.red-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  background: #ff3b30;
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  box-sizing: border-box;
}
.more-menu {
  position: absolute;
  left: 70px;
  bottom: 20px;
  width: 200px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  z-index: 999;
  overflow: hidden;
}
.more-menu-item {
  height: 44px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: background 0.2s;
}
.more-menu-item:hover {
  background: #f5f5f5;
}
.more-menu-item.menu-divider {
  border-top: 1px solid #e6e6e6;
}
.menu-text {
  font-size: 14px;
  color: #333;
}
.menu-dot {
  font-size: 12px;
  color: #ff3b30;
}
/* 聊天右上角菜单 */
.chat-menu {
  position: absolute;
  right: 20px;
  top: 56px;
  width: 220px;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
  z-index: 2000;
  overflow: hidden;
}
.chat-menu-item {
  height: 44px;
  padding: 0 14px;
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #333;
  border-bottom: 1px solid #f5f5f5;
}
.chat-menu-item:hover {
  background: #f5f5f5;
}
.chat-menu-item-danger {
  color: #ff3b30;
}
/* 聊天信息面板 */
.chat-info-panel {
  position: absolute;
  right: 20px;
  top: 56px;
  width: 320px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.12);
  z-index: 2000;
  overflow: hidden;
  padding: 12px;
}
.chat-info-avatars {
  display: flex;
  gap: 12px;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid #f5f5f5;
}
.avatar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  position: relative;
}
.info-avatar {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  background: #eee;
}
.avatar-name {
  font-size: 12px;
  color: #333;
  margin-top: 6px;
}
.profile-popover {
  position: absolute; /* left/top 会由 popoverStyle 覆盖 */
  width: 260px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.16);
  z-index: 2600;
  padding: 12px;
}
.profile-popover .popover-arrow {
  position: absolute;
  top: 18px;
  width: 0;
  height: 0;
}
.popover-arrow.right {
  left: -8px;
  border-top: 8px solid transparent;
  border-bottom: 8px solid transparent;
  border-right: 8px solid #fff;
  box-shadow: -2px 2px 6px rgba(0, 0, 0, 0.06);
}
.popover-arrow.left {
  right: -8px;
  border-top: 8px solid transparent;
  border-bottom: 8px solid transparent;
  border-left: 8px solid #fff;
  box-shadow: 2px 2px 6px rgba(0, 0, 0, 0.06);
}
.popover-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.popover-avatar {
  width: 56px;
  height: 56px;
  border-radius: 6px;
}
.popover-meta {
  flex: 1;
}
.popover-name {
  font-weight: 600;
  display: block;
}
.popover-sub {
  color: #888;
  font-size: 12px;
  display: block;
  margin-top: 4px;
}
.popover-dots,
.popover-dots.tool-icon {
  cursor: pointer;
  /* padding: 4px;
  font-size: 18px;
  line-height: 1;
  font-weight: 400; */
}
.popover-body {
  margin-top: 8px;
}
.popover-menu {
  position: absolute;
  right: 12px;
  top: 40px;
  width: 220px;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
  z-index: 2600;
  overflow: hidden;
}
.popover-menu .chat-menu-item {
  padding: 10px 12px;
  height: auto;
  border-bottom: 1px solid #f5f5f5;
}
.popover-menu .chat-menu-item:hover {
  background: #fafafa;
}
.popover-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px solid #f5f5f5;
}
.popover-actions {
  display: flex;
  gap: 6px;
  margin-top: 10px;
}
.popover-actions button {
  flex: 1;
  height: 36px;
  font-size: 14px;
  line-height: 36px;
  border-radius: 4px;
  white-space: nowrap;
}
.popover-actions .primary-btn {
  padding: 0;
  background: #07c160;
  color: #fff;
  border: none;
}
.popover-actions .default-btn {
  padding: 0;
  background: #fff;
  color: #333;
  border: 1px solid #d9d9d9;
}
.add-avatar .add-plus {
  width: 56px;
  height: 56px;
  border: 1px dashed #ccc;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #666;
}
.chat-info-options {
  padding-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.option-item {
  padding: 10px;
  cursor: pointer;
  border-radius: 6px;
}
.option-item:hover {
  background: #f5f5f5;
}
.toggle-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.option-danger {
  color: #ff3b30;
}

/* 个人资料模态 */
.profile-modal-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
}
.profile-modal {
  width: 520px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}
.profile-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;
}
.profile-modal-title {
  font-size: 16px;
  font-weight: 600;
}
.profile-modal-dot {
  cursor: pointer;
}
.profile-modal-body {
  padding: 20px;
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.profile-modal-avatar {
  width: 96px;
  height: 96px;
  border-radius: 8px;
}
.profile-modal-info {
  flex: 1;
}
.profile-modal-name {
  font-size: 18px;
  font-weight: 600;
}
.profile-modal-desc {
  color: #666;
  margin-top: 8px;
}
.profile-modal-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

/* 会话/通讯录列表 */
.session-list,
.contact-list {
  width: 260px;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  height: 100%;
  border-right: 1px solid #e6e6e6;
}
.search-bar {
  height: 52px;
  padding: 0 12px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 8px;
}
.search-input {
  flex: 1;
  background: #ebebeb;
  border: none;
  border-radius: 4px;
  padding: 6px 10px;
  font-size: 14px;
  height: 32px;
  outline: none;
}
.session-scroll,
.contact-scroll {
  flex: 1;
  overflow-y: auto;
}
.session-item,
.contact-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  cursor: pointer;
  border-radius: 4px;
  margin: 0 4px;
  position: relative;
}
.session-item:hover,
.session-item.active,
.contact-item:hover,
.contact-item.active {
  background: #ebebeb;
}
.session-avatar,
.person-avatar {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  margin-right: 10px;
  flex-shrink: 0;
  background: #cccccc;
}
.session-info {
  flex: 1;
  min-width: 0;
}
.session-name,
.contact-name {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
  color: #333;
}
.session-last-msg {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.session-time,
.arrow-icon,
.contact-count {
  font-size: 12px;
  color: #999;
}
.unread-badge {
  position: absolute;
  right: 12px;
  bottom: 10px;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  background: #ff3b30;
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  box-sizing: border-box;
}
.contact-manage-btn {
  height: 44px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  border-bottom: 1px solid #e6e6e6;
}
.contact-manage-btn:hover {
  background: #ebebeb;
}
.contact-tab-bar {
  display: flex;
  border-bottom: 1px solid #e6e6e6;
  background: #fff;
}
.contact-tab-bar .tab-item {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  cursor: pointer;
  color: #666;
}
.contact-tab-bar .tab-item.active {
  color: #07c160;
  border-bottom: 2px solid #07c160;
}

/* 重用现有固定/分组列表样式保留，但隐藏下面内容 */
.contact-group {
  border-bottom: 1px solid #e6e6e6;
}
.group-header {
  height: 44px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.group-header:hover {
  background: #ebebeb;
}
.group-children {
  padding-left: 24px;
}
.arrow-icon.expanded {
  transform: rotate(90deg);
}

/* 新的好友/群样式 */
.contact-item.friend-item,
.contact-item.group-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  cursor: pointer;
  border-bottom: 1px solid #e6e6e6;
}
.contact-item.friend-item:hover,
.contact-item.group-item:hover {
  background: #ebebeb;
}
.contact-item.friend-item .contact-info,
.contact-item.group-item .contact-info {
  flex: 1;
  min-width: 0;
  margin-left: 10px;
}
.contact-item.friend-item .contact-name,
.contact-item.group-item .contact-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}
.contact-item.friend-item .contact-desc,
.contact-item.group-item .contact-desc {
  font-size: 12px;
  color: #999;
}
.contact-action {
  padding: 4px;
}
.contact-action .chat-icon {
  font-size: 18px;
  color: #666;
}

/* ─── 个人主页（PC端设置页重设计）─── */
.profile-area {
  flex: 1;
  display: flex;
  flex-direction: row;
  background: #f7f8fa;
  height: 100%;
  min-width: 0;
  overflow: hidden;
}

/* 左侧导航栏 */
.settings-nav {
  width: 220px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid #ebebeb;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
}
.settings-user-brief {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.15s;
}
.settings-user-brief:hover { background: #fafafa; }
.settings-brief-avatar {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  flex-shrink: 0;
  background: #e6e6e6;
}
.settings-brief-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.settings-brief-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.settings-brief-id {
  font-size: 12px;
  color: #bbb;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.settings-nav-group { padding: 10px 0; flex: 1; }
.settings-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 20px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  color: #555;
  font-size: 14px;
  user-select: none;
}
.settings-nav-item:hover { background: #f5f5f5; color: #333; }
.settings-nav-item.active { background: #edf4ff; color: #1677ff; font-weight: 500; }
.settings-nav-icon { font-size: 16px; width: 20px; text-align: center; flex-shrink: 0; }
.settings-nav-label { font-size: 14px; }

/* 右侧内容区 */
.settings-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
  overflow-y: auto;
  background: #f7f8fa;
}
.settings-panel-header {
  padding: 28px 40px 22px;
  background: #fff;
  border-bottom: 1px solid #ebebeb;
  flex-shrink: 0;
}
.settings-panel-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  display: block;
}
.settings-panel-subtitle {
  font-size: 13px;
  color: #bbb;
  margin-top: 5px;
  display: block;
}
.settings-body {
  padding: 28px 40px;
  display: flex;
  flex-direction: column;
  gap: 28px;
}
.settings-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.section-label {
  font-size: 11px;
  color: #aaa;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  padding-left: 4px;
}

/* 头像编辑区块 */
.avatar-edit-block {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 20px 24px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #ebebeb;
}
.settings-avatar-preview {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  flex-shrink: 0;
  background: #e6e6e6;
}
.avatar-edit-meta { display: flex; flex-direction: column; gap: 10px; }
.avatar-tip { font-size: 13px; color: #bbb; }

/* 表单行 */
.settings-form {
  background: #fff;
  border-radius: 10px;
  border: 1px solid #ebebeb;
  overflow: hidden;
}
.form-row {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #f5f5f5;
  gap: 12px;
}
.form-row.no-border { border-bottom: none; }
.form-field-label {
  font-size: 14px;
  color: #666;
  width: 88px;
  flex-shrink: 0;
}
.form-field-value {
  flex: 1;
  font-size: 14px;
  color: #1a1a1a;
}
.form-field-value.mono {
  font-family: 'SF Mono', 'Consolas', monospace;
  color: #555;
  font-size: 13px;
}
.form-field-value.muted { color: #bbb; }
.form-field-action {
  font-size: 13px;
  color: #1677ff;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 5px;
  transition: background 0.15s;
  flex-shrink: 0;
  user-select: none;
}
.form-field-action:hover { background: #edf4ff; }

/* 通用按鈕 */
.settings-action-btn {
  display: inline-flex;
  align-items: center;
  padding: 6px 16px;
  font-size: 13px;
  color: #1677ff;
  background: #edf4ff;
  border: 1px solid #c7e0ff;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
  white-space: nowrap;
  line-height: 1.5;
}
.settings-action-btn:hover { background: #dceeff; border-color: #a8cefc; }

/* 二维码卡片 */
.qrcode-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px 24px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #ebebeb;
}
.qrcode-placeholder-box {
  width: 68px;
  height: 68px;
  background: #f5f5f5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1px dashed #ddd;
}
.qrcode-placeholder-icon { font-size: 26px; }
.qrcode-card-info { flex: 1; display: flex; flex-direction: column; gap: 6px; }
.qrcode-card-title { font-size: 15px; font-weight: 500; color: #1a1a1a; }
.qrcode-card-desc { font-size: 13px; color: #bbb; }

/* 选项列表（账号安全/通知/隐私/通用） */
.settings-option-list {
  background: #fff;
  border-radius: 10px;
  border: 1px solid #ebebeb;
  overflow: hidden;
}
.settings-option-row {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #f5f5f5;
  gap: 16px;
}
.settings-option-row.no-border { border-bottom: none; }
.settings-option-row.danger-row { cursor: pointer; transition: background 0.15s; }
.settings-option-row.danger-row:hover { background: #fff5f5; }
.option-row-left {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.option-row-title { font-size: 14px; color: #1a1a1a; }
.option-row-title.danger { color: #e53935; }
.option-row-desc { font-size: 12px; color: #bbb; }
.option-row-arrow { font-size: 22px; color: #ccc; line-height: 1; }

/* Toggle 开关 */
.toggle-switch {
  width: 44px;
  height: 24px;
  background: #d9d9d9;
  border-radius: 12px;
  position: relative;
  cursor: pointer;
  transition: background 0.25s;
  flex-shrink: 0;
}
.toggle-switch.on { background: #1677ff; }
.toggle-thumb {
  position: absolute;
  width: 20px;
  height: 20px;
  background: #fff;
  border-radius: 50%;
  top: 2px;
  left: 2px;
  transition: left 0.25s;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.18);
}
.toggle-switch.on .toggle-thumb { left: 22px; }

/* 聊天区域 */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f0f0f0;
  height: 100%;
  min-width: 0;
  position: relative;
}
.chat-header {
  height: 52px;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
  background: #f0f0f0;
}
.chat-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}
.header-tools {
  display: flex;
  gap: 20px;
}
.header-tools .tool-icon {
  font-size: 18px;
  color: #666;
  cursor: pointer;
}
.chat-messages {
  flex: 1;
  background: #f0f0f0;
  display: flex;
  flex-direction: column;
  height: 0;
}
.messages-inner-wrapper {
  padding: 20px;
  width: 100%;
  box-sizing: border-box;
}
.empty-tip {
  text-align: center;
  color: #999;
  font-size: 14px;
  margin-top: 100px;
}

/* 时间分隔线 */
.time-separator {
  text-align: center;
  color: #999;
  font-size: 12px;
  margin: 16px 0;
  line-height: 1;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.time-separator::before,
.time-separator::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e6e6e6;
  max-width: 60px;
  margin: 0 12px;
}

/* 消息气泡 */
.message-item {
  display: flex;
  margin-bottom: 16px;
  width: 100%;
  align-items: flex-start;
}
.message-item.other {
  justify-content: flex-start;
  flex-direction: row;
}
.message-item.self {
  justify-content: flex-end;
  flex-direction: row;
}
.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  flex-shrink: 0;
  background: #cccccc;
}
.message-sender {
  font-size: 12px;
  color: #666;
  margin-bottom: 2px;
}
.message-item.other .message-avatar {
  order: 1;
  margin-right: 10px;
}
.message-item.self .message-avatar {
  order: 2;
  margin-left: 10px;
}
.message-content-wrapper {
  display: flex;
  flex-direction: column;
  max-width: 60%;
}
.message-item.other .message-content-wrapper {
  order: 2;
  align-items: flex-start;
}
.message-item.self .message-content-wrapper {
  order: 1;
  align-items: flex-end;
}
.message-content {
  padding: 9px 12px;
  border-radius: 4px;
  word-break: break-all;
  font-size: 14px;
  line-height: 1.5;
  max-width: 500px;
}
.message-voice {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 16px;
  border-radius: 4px;
  min-width: 60px;
  cursor: pointer;
  position: relative;
}
.message-item.other .message-voice {
  background: #fff;
  border-top-left-radius: 0;
}
.message-item.self .message-voice {
  background: #95ec69;
  border-top-right-radius: 0;
  flex-direction: row-reverse;
}
.voice-icon {
  font-size: 16px;
}
.voice-duration {
  font-size: 12px;
  color: #666;
}
.downloading-text {
  font-size: 10px;
  color: #999;
  position: absolute;
  bottom: -18px;
  right: 0;
}
.message-image {
  max-width: 300px;
  max-height: 300px;
  border-radius: 4px;
  cursor: pointer;
}
.message-video {
  max-width: 300px;
  max-height: 300px;
  border-radius: 4px;
}
.message-file {
  display: flex;
  align-items: center;
  width: 280px;
  padding: 12px 16px;
  border-radius: 4px;
  position: relative;
}
.file-icon {
  font-size: 32px;
  margin-right: 12px;
}
.file-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}
.file-name {
  font-size: 14px;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.file-size {
  font-size: 12px;
  color: #999;
}
.message-item.other .message-content,
.message-item.other .message-file,
.message-item.other .message-voice {
  background: #fff;
  color: #333;
  border-top-left-radius: 0;
}
.message-item.self .message-content,
.message-item.self .message-file,
.message-item.self .message-voice {
  background: #95ec69;
  color: #333;
  border-top-right-radius: 0;
}
.scroll-to-bottom-btn {
  position: absolute;
  right: 20px;
  bottom: 220px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 50;
}

/* 录音弹窗 */
.recording-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.recording-modal {
  background: #fff;
  padding: 40px 60px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}
.recording-wave {
  font-size: 48px;
}
.recording-modal-text {
  font-size: 16px;
  color: #333;
  font-weight: bold;
}
.recording-btns {
  display: flex;
  gap: 20px;
}
.cancel-voice-btn {
  padding: 8px 24px;
  background: #f5f5f5;
  color: #333;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.send-voice-btn {
  padding: 8px 24px;
  background: #07c160;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

/* 底部输入区 */
.chat-input-area {
  border-top: 1px solid #e6e6e6;
  display: flex;
  flex-direction: column;
  padding: 12px 20px;
  flex-shrink: 0;
  background: #f0f0f0;
  min-height: 180px;
  max-height: 300px;
  gap: 8px;
}
.pending-files {
  display: flex;
  gap: 10px;
  padding: 0 0 4px 0;
  flex-wrap: wrap;
}
.pending-file-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e6e6e6;
}
.pending-file-image {
  width: 100%;
  height: 100%;
}
.pending-file-video,
.pending-file-doc {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.remove-btn {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  cursor: pointer;
}
.input-tools {
  display: flex;
  flex-direction: row;
  gap: 24px;
  flex-shrink: 0;
  padding: 0 4px;
  align-items: center;
}
.tool-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 30px;
  height: 30px;
  user-select: none;
}
.recording-btn-active {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #ff3b30;
  border-radius: 4px;
}
.recording-pulse {
  font-size: 12px;
  animation: pulse 1s infinite;
}
@keyframes pulse {
  0% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.2);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}
.emoji-btn-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
}
.input-tools .tool-icon {
  font-size: 20px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}
.input-tools .tool-icon:hover {
  color: #07c160;
}
.input-container {
  position: relative;
  flex: 1;
  width: 100%;
  border: 2px dashed transparent;
  border-radius: 4px;
  transition: all 0.2s;
}
.input-container.drag-over {
  border-color: #07c160;
  background: rgba(7, 193, 96, 0.05);
}
.chat-input {
  width: 100%;
  height: 100%;
  border: none;
  border-radius: 4px;
  padding: 10px 12px 45px 12px;
  font-size: 14px;
  background: #fff;
  outline: none;
  resize: none;
  line-height: 1.5;
  min-height: 80px;
  max-height: 180px;
  box-sizing: border-box;
}
.send-btn-wrapper {
  position: absolute;
  right: 12px;
  bottom: 12px;
  flex-shrink: 0;
}
.send-btn {
  padding: 6px 20px;
  background: #07c160;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
}
.send-btn:disabled {
  background: #cccccc;
  cursor: not-allowed;
}
.emoji-panel {
  position: absolute;
  bottom: 100%;
  left: 0;
  width: 500px;
  height: 280px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  margin-bottom: 8px;
  overflow: hidden;
  z-index: 100;
}
.emoji-section {
  padding: 0 12px;
}
.emoji-title {
  font-size: 12px;
  color: #999;
  margin: 12px 0 8px 0;
}
.emoji-grid {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 8px;
}
.emoji-scroll {
  flex: 1;
  overflow-y: auto;
}
.emoji-item {
  width: 100%;
  aspect-ratio: 1/1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  cursor: pointer;
  border-radius: 4px;
}
.emoji-item:hover {
  background: #f5f5f5;
}

/* 通讯录详情 */
.contact-content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f0f0f0;
  height: 100%;
  min-width: 0;
  align-items: center;
  justify-content: center;
}
.contact-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}
.empty-logo {
  width: 100px;
  height: 100px;
  opacity: 0.3;
}
.contact-detail {
  width: 100%;
  height: 100%;
  padding: 40px 60px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 20px;
}
.detail-avatar {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  background: #cccccc;
}
.detail-name {
  font-size: 24px;
  font-weight: 500;
  color: #333;
}
.detail-desc {
  font-size: 14px;
  color: #999;
}
.detail-btns {
  display: flex;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e6e6e6;
}
.primary-btn {
  padding: 10px 30px;
  background: #07c160;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}
.default-btn {
  padding: 10px 30px;
  background: #fff;
  color: #333;
  border: 1px solid #e6e6e6;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}
.detail-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
  background: #e6e6e6;
  border-radius: 4px;
  overflow: hidden;
}
.detail-item {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background: #fff;
  gap: 20px;
}
.item-label {
  width: 80px;
  font-size: 14px;
  color: #999;
}
.item-value {
  flex: 1;
  font-size: 14px;
  color: #333;
}

/* ========== 移动端样式（H5 响应式 + App 原生） ========== */
.mobile-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fff;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 会话列表页 */
.mobile-session-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.mobile-navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  padding-top: max(12px, env(safe-area-inset-top));
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  height: 44px;
}

.mobile-title {
  font-size: 18px;
  font-weight: 600;
  color: #000;
}

.mobile-nav-icons {
  display: flex;
  gap: 16px;
}

.mobile-icon {
  font-size: 20px;
  cursor: pointer;
  padding: 4px;
}

.mobile-search-box {
  padding: 8px 16px;
  background: #f5f5f5;
}

.mobile-search-input {
  width: 100%;
  height: 36px;
  border: 1px solid #e5e5e5;
  border-radius: 18px;
  padding: 0 12px;
  font-size: 14px;
  background: #fff;
}

.mobile-sessions-scroll {
  flex: 1;
  overflow-y: scroll;
  -webkit-overflow-scrolling: touch;
}

.mobile-session-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  position: relative;
}

.mobile-avatar {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  margin-right: 12px;
  flex-shrink: 0;
}

.mobile-session-content {
  flex: 1;
  min-width: 0;
}

.mobile-session-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.mobile-session-name {
  font-size: 15px;
  font-weight: 500;
  color: #000;
}

.mobile-time {
  font-size: 12px;
  color: #999;
}

.mobile-last-msg {
  font-size: 13px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mobile-unread-badge {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  background: #ff4444;
  color: #fff;
  border-radius: 50%;
  min-width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}

/* 聊天详情页 */
.mobile-chat-detail {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.mobile-chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 44px;
  padding: 0 16px;
  padding-top: max(0, env(safe-area-inset-top));
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.mobile-back-btn {
  font-size: 16px;
  color: #07c160;
  cursor: pointer;
}

.mobile-more-icon {
  font-size: 16px;
  color: #666;
  cursor: pointer;
}

.mobile-messages-container {
  flex: 1;
  overflow-y: scroll;
  -webkit-overflow-scrolling: touch;
  background: #f5f5f5;
}

.mobile-messages-wrapper {
  padding: 12px 16px;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.mobile-empty-tip {
  text-align: center;
  color: #999;
  font-size: 14px;
  margin-top: 60px;
}

.mobile-time-separator {
  text-align: center;
  color: #999;
  font-size: 12px;
  margin: 16px 0;
}

.mobile-message-bubble {
  display: flex;
  margin-bottom: 12px;
  align-items: flex-end;
  gap: 8px;
}
.mobile-message-sender {
  font-size: 12px;
  color: #666;
  margin-bottom: 2px;
}
.mobile-message-bubble.mobile-own {
  justify-content: flex-end;
  flex-direction: row-reverse;
}

.mobile-bubble-avatar {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  flex-shrink: 0;
}

.mobile-bubble-content {
  display: flex;
  flex-direction: column;
  max-width: 70%;
}

.mobile-text-msg {
  background: #fff;
  padding: 8px 12px;
  border-radius: 8px;
  word-wrap: break-word;
  font-size: 14px;
  line-height: 1.4;
  color: #000;
}

.mobile-message-bubble.mobile-own .mobile-text-msg {
  background: #95ec69;
}

.mobile-emoji-msg {
  font-size: 32px;
}

.mobile-image-msg {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  display: block;
}

.mobile-message-file {
  background: #fff;
  padding: 8px 12px;
  border-radius: 8px;
  display: flex;
  gap: 8px;
  align-items: center;
}

.mobile-message-bubble.mobile-own .mobile-message-file {
  background: #95ec69;
}

.mobile-file-icon {
  font-size: 20px;
}

.mobile-file-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mobile-file-name {
  font-size: 12px;
  color: #000;
  font-weight: 500;
}

.mobile-file-size {
  font-size: 11px;
  color: #666;
}

/* 输入栏 */
.mobile-input-bar {
  background: #f5f5f5;
  padding: 8px 16px;
  padding-bottom: max(8px, env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-top: 1px solid #e5e5e5;
  flex-shrink: 0;
}

.mobile-input-tools {
  display: flex;
  gap: 12px;
}

.mobile-tool-icon {
  font-size: 20px;
  cursor: pointer;
  padding: 4px;
}

.mobile-input-field {
  display: flex;
  gap: 8px;
  align-items: center;
}

.mobile-text-input {
  flex: 1;
  height: 36px;
  border: 1px solid #e5e5e5;
  border-radius: 18px;
  padding: 0 12px;
  font-size: 14px;
  background: #fff;
}

.mobile-send-btn {
  padding: 6px 16px;
  background: #07c160;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
}

/* 表情选择器 */
.mobile-emoji-picker {
  background: #fff;
  border-top: 1px solid #e5e5e5;
  padding: 8px 0;
  max-height: 150px;
  overflow: hidden;
}

.mobile-emoji-scroll {
  width: 100%;
}

.mobile-emoji-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 8px 12px;
  gap: 8px;
}

.mobile-emoji-item {
  font-size: 24px;
  cursor: pointer;
  padding: 4px;
}

/* ===== ➕ 按钮与下拉菜单 ===== */
.add-btn-wrapper {
  position: relative;
  flex-shrink: 0;
}
.add-icon {
  font-size: 16px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  user-select: none;
  transition: background 0.15s;
}
.add-icon:hover {
  background: #ebebeb;
}
.add-icon-active {
  background: #dedede;
}
.plus-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  min-width: 130px;
  z-index: 200;
  overflow: hidden;
}
.plus-dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
  font-size: 13px;
  color: #333;
  transition: background 0.1s;
}
.plus-dropdown-item:hover {
  background: #f5f5f5;
}
.plus-dropdown-icon {
  font-size: 16px;
}
.plus-dropdown-text {
  flex: 1;
}

/* ===== 添加朋友弹窗 ===== */
.add-friend-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}
.add-friend-modal {
  background: #fff;
  border-radius: 8px;
  width: 460px;
  max-width: 94vw;
  box-shadow: 0 8px 32px rgba(0,0,0,0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.add-friend-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 12px;
  border-bottom: 1px solid #f0f0f0;
}
.add-friend-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
.add-friend-close {
  font-size: 16px;
  color: #999;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  line-height: 1;
  transition: color 0.15s;
}
.add-friend-close:hover {
  color: #333;
}
.add-friend-search-bar {
  padding: 16px 20px;
  display: flex;
  gap: 8px;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
}
.add-friend-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 0 10px;
  gap: 6px;
  transition: border-color 0.2s;
}
.add-friend-input-wrap:focus-within {
  border-color: #07c160;
}
.add-friend-search-icon {
  font-size: 14px;
  color: #999;
  flex-shrink: 0;
}
.add-friend-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 14px;
  height: 36px;
  color: #333;
}
.add-friend-clear {
  font-size: 14px;
  color: #bbb;
  cursor: pointer;
  flex-shrink: 0;
  line-height: 1;
  padding: 2px;
}
.add-friend-clear:hover {
  color: #666;
}
.add-friend-search-btn {
  height: 36px;
  padding: 0 18px;
  background: #07c160;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s;
  flex-shrink: 0;
}
.add-friend-search-btn:hover {
  background: #06ad56;
}
.add-friend-search-btn:disabled {
  background: #b2dfca;
  cursor: not-allowed;
}
.add-friend-body {
  padding: 20px;
  min-height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.add-friend-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: #aaa;
}
.hint-icon {
  font-size: 40px;
}
.hint-text {
  font-size: 13px;
  text-align: center;
  line-height: 1.5;
}
.add-friend-loading {
  color: #07c160;
  font-size: 14px;
}
.add-friend-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: #aaa;
}
.empty-icon {
  font-size: 36px;
}
.empty-text {
  font-size: 13px;
  text-align: center;
}
.add-friend-result {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 6px 0;
}
.result-avatar {
  width: 52px;
  height: 52px;
  border-radius: 6px;
  flex-shrink: 0;
  background: #eee;
}
.result-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.result-nickname {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.result-username {
  font-size: 12px;
  color: #888;
}
.result-signature {
  font-size: 12px;
  color: #aaa;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.result-action {
  flex-shrink: 0;
}
.result-btn {
  height: 34px;
  padding: 0 16px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s, opacity 0.15s;
}
.result-btn-add {
  background: #07c160;
  color: #fff;
}
.result-btn-add:hover {
  background: #06ad56;
}
.result-btn-add:disabled {
  background: #b2dfca;
  cursor: not-allowed;
}
.result-btn-already {
  background: #f5f5f5;
  color: #aaa;
  cursor: default;
}
.result-btn-applied {
  background: #f0faf5;
  color: #07c160;
  cursor: default;
}

/* ===== 通知 tab ===== */
.notify-tab-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.tab-notify-badge {
  position: absolute;
  top: 6px;
  right: 4px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  background: #ff3b30;
  color: #fff;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 3px;
  box-sizing: border-box;
}
.notify-section {
  border-bottom: 6px solid #f5f5f5;
}
.notify-section-title {
  padding: 10px 16px 6px;
  font-size: 12px;
  color: #999;
  font-weight: 500;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}
.notify-item {
  display: flex;
  align-items: center;
  padding: 14px 12px;
  border-bottom: 1px solid #f5f5f5;
  gap: 12px;
  background: #fff;
}
.notify-item:hover {
  background: #fafafa;
}
.notify-avatar {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  flex-shrink: 0;
  background: #eee;
}
.notify-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.notify-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.notify-group-name {
  font-size: 12px;
  color: #07c160;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.notify-time {
  font-size: 11px;
  color: #aaa;
}
.notify-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.notify-btn {
  height: 30px;
  padding: 0 14px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity 0.15s;
}
.accept-btn {
  background: #07c160;
  color: #fff;
}
.accept-btn:hover {
  background: #06ad56;
}
.reject-btn {
  background: #f5f5f5;
  color: #666;
}
.reject-btn:hover {
  background: #ebebeb;
}
.notify-status-tag {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 12px;
  flex-shrink: 0;
}
.tag-accepted {
  background: #f0faf5;
  color: #07c160;
}
.tag-rejected {
  background: #fff0f0;
  color: #ff3b30;
}
.notify-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 12px;
}
.notify-empty-icon {
  font-size: 40px;
}
.notify-empty-text {
  font-size: 14px;
  color: #aaa;
}
.notify-loading {
  padding: 40px;
  text-align: center;
  color: #aaa;
  font-size: 14px;
}
.contact-empty {
  padding: 40px;
  text-align: center;
}
.contact-empty-text {
  font-size: 13px;
  color: #bbb;
}

/* ===== 加入群聊搜索结果列表 ===== */
.join-group-result-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 0;
}
.join-group-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.join-group-item:last-child {
  border-bottom: none;
}

/* ===== 移动端页面 ===== */
.mobile-contact-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.mobile-contact-tab-bar {
  display: flex;
  border-bottom: 1px solid #e6e6e6;
  background: #fff;
}
.mobile-contact-tab-bar .mobile-tab-item {
  flex: 1;
  text-align: center;
  padding: 10px 0;
  color: #666;
}
.mobile-contact-tab-bar .mobile-tab-item.mobile-active {
  color: #07c160;
  border-bottom: 2px solid #07c160;
}
.mobile-contacts-scroll {
  flex: 1;
  background: #f5f5f5;
}
.mobile-contact-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e6e6e6;
}
.mobile-contact-item .mobile-session-content {
  margin-left: 12px;
  flex: 1;
}

/* 移动端个人资料页面 */
.mobile-profile-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.mobile-profile-content {
  flex: 1;
  overflow-y: scroll;
  -webkit-overflow-scrolling: touch;
  background: #f5f5f5;
}

.mobile-profile-card {
  padding: 20px 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  gap: 16px;
}

.mobile-profile-avatar {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  flex-shrink: 0;
}

.mobile-profile-base-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mobile-profile-nickname {
  font-size: 16px;
  font-weight: 600;
  color: #000;
}

.mobile-profile-wechat-id {
  font-size: 13px;
  color: #999;
}

.mobile-profile-edit-btn {
  padding: 6px 12px;
  background: #07c160;
  color: #fff;
  border-radius: 4px;
  font-size: 12px;
  width: fit-content;
  cursor: pointer;
}

.mobile-profile-section {
  margin-top: 12px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.mobile-profile-item {
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
  gap: 12px;
}

.mobile-profile-item:last-child {
  border-bottom: none;
}

.mobile-profile-label {
  font-size: 14px;
  color: #666;
  white-space: nowrap;
}

.mobile-profile-value {
  font-size: 14px;
  color: #000;
  flex: 1;
  text-align: right;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 底部导航栏 */
.mobile-bottom-tab-bar {
  display: flex;
  justify-content: space-around;
  height: 50px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  padding-bottom: max(0, env(safe-area-inset-bottom));
  flex-shrink: 0;
}

.mobile-tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #999;
}

.mobile-tab-item.mobile-active {
  color: #07c160;
}

.mobile-tab-icon {
  font-size: 20px;
  margin-bottom: 2px;
}

.mobile-tab-label {
  font-size: 11px;
}
</style>
