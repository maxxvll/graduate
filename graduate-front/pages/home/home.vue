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
        <text class="add-icon">➕</text>
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
          @click="selectedFriendTab = 'friends'"
        >
          <text>好友</text>
        </view>
        <view
          class="tab-item"
          :class="{ active: selectedFriendTab === 'groups' }"
          @click="selectedFriendTab = 'groups'"
        >
          <text>群聊</text>
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
              :src="group.groupAvatar"
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
        </view>
      </view>
    </view>

    <!-- 个人主页 -->
    <view class="profile-area" v-else-if="currentSidebarTab === 'profile'">
      <view class="profile-header-card">
        <image
          :src="userInfo.avatar || defaultAvatar"
          class="profile-avatar"
          mode="aspectFill"
        ></image>
        <view class="profile-base-info">
          <text class="profile-nickname">{{ userInfo.nickname }}</text>
          <view class="wechat-id-row" @click.stop="openEditProfile">
            <text class="profile-wechat-id"
              >微信号：{{ userInfo.wechatId }}</text
            >
            <text class="edit-icon">✏️</text>
          </view>
          <view class="qrcode-row">
            <text class="qrcode-text">我的二维码</text>
            <text class="qrcode-icon">📱</text>
          </view>
        </view>
      </view>

      <view class="profile-content">
        <view class="profile-card">
          <view class="profile-item" @click="openEditProfile">
            <text class="item-label">昵称</text>
            <text class="item-value">{{ userInfo.nickname }}</text>
            <text class="arrow-icon">></text>
          </view>
          <view class="profile-item" @click="openEditProfile">
            <text class="item-label">头像</text>
            <image
              :src="userInfo.avatar || defaultAvatar"
              class="item-avatar"
              mode="aspectFill"
            ></image>
            <text class="arrow-icon">></text>
          </view>
          <view class="profile-item" @click="openEditProfile">
            <text class="item-label">个性签名</text>
            <text class="item-value">{{
              userInfo.signature || '这个人很懒，什么都没写'
            }}</text>
            <text class="arrow-icon">></text>
          </view>
          <view class="profile-item" @click="openEditProfile">
            <text class="item-label">地区</text>
            <text class="item-value">{{ userInfo.region || '未设置' }}</text>
            <text class="arrow-icon">></text>
          </view>
        </view>

        <view class="profile-card">
          <view class="profile-item">
            <text class="item-label">我的账号与安全</text>
            <text class="arrow-icon">></text>
          </view>
          <view class="profile-item">
            <text class="item-label">新消息通知</text>
            <text class="arrow-icon">></text>
          </view>
          <view class="profile-item">
            <text class="item-label">隐私设置</text>
            <text class="arrow-icon">></text>
          </view>
          <view class="profile-item">
            <text class="item-label">通用设置</text>
            <text class="arrow-icon">></text>
          </view>
        </view>
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
const CURRENT_USER_ID = '1'
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

// 【修复1：用户信息持久化初始化】
const userInfo = ref({
  id: CURRENT_USER_ID,
  nickname: '我的昵称',
  avatar: selfAvatar,
  wechatId: 'wx_12345678',
  region: '中国 福建',
  signature: '这个人很懒，什么都没写',
})

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

// 好友和群聊模拟数据
const friends = ref([
  {
    userId: '2',
    nickname: '李四',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=James',
    wechatId: 'wx_22222',
    signature: '一个开心的人',
    status: 1,
    isFriend: true,
  },
  {
    userId: '3',
    nickname: '王五',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix',
    wechatId: 'wx_33333',
    signature: '前端开发工程师',
    status: 1,
    isFriend: true,
  },
  {
    userId: '4',
    nickname: '赵六',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Sophie',
    wechatId: 'wx_44444',
    signature: '喜欢编程和音乐',
    status: 1,
    isFriend: true,
  },
  {
    userId: '5',
    nickname: '张三',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Susan',
    wechatId: 'wx_55555',
    signature: '后端开发工程师',
    status: 1,
    isFriend: true,
  },
  {
    userId: '6',
    nickname: '陈七',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Frank',
    wechatId: 'wx_66666',
    signature: '产品经理',
    status: 1,
    isFriend: true,
  },
  {
    userId: '7',
    nickname: '刘八',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Kate',
    wechatId: 'wx_77777',
    signature: '设计师',
    status: 1,
    isFriend: true,
  },
])

const groups = ref([
  {
    groupId: 'g1',
    groupName: '开发团队',
    groupAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Group1',
    creatorId: '1',
    memberCount: 8,
    members: [
      { userId: '1', nickname: '我', role: 1 },
      { userId: '2', nickname: '李四', role: 2 },
      { userId: '3', nickname: '王五', role: 3 },
      { userId: '4', nickname: '赵六', role: 3 },
      { userId: '5', nickname: '张三', role: 3 },
    ],
    status: 1,
  },
  {
    groupId: 'g2',
    groupName: '设计小组',
    groupAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Group2',
    creatorId: '3',
    memberCount: 5,
    members: [
      { userId: '1', nickname: '我', role: 3 },
      { userId: '3', nickname: '王五', role: 1 },
      { userId: '6', nickname: '陈七', role: 3 },
      { userId: '7', nickname: '刘八', role: 3 },
    ],
    status: 1,
  },
  {
    groupId: 'g3',
    groupName: '项目讨论组',
    groupAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Group3',
    creatorId: '2',
    memberCount: 12,
    members: [
      { userId: '1', nickname: '我', role: 3 },
      { userId: '2', nickname: '李四', role: 1 },
      { userId: '3', nickname: '王五', role: 3 },
      { userId: '4', nickname: '赵六', role: 3 },
      { userId: '5', nickname: '张三', role: 3 },
      { userId: '6', nickname: '陈七', role: 3 },
      { userId: '7', nickname: '刘八', role: 3 },
    ],
    status: 1,
  },
])

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
  const currentUserId = String(CURRENT_USER_ID).trim()
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
    : String(CURRENT_USER_ID).trim()
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
  // 优先关闭头像弹出层
  if (showProfilePopover.value) {
    showProfilePopover.value = false
    return
  }
  // 关闭聊天相关面板
  showChatMenu.value = false
  showChatInfoPanel.value = false
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
  const sessionId = `${Math.min(CURRENT_USER_ID, friend.userId)}_${Math.max(CURRENT_USER_ID, friend.userId)}`
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

// 数据加载
const loadSessionList = async () => {
  try {
    // 使用假数据初始化会话列表（混合单聊和群聊）
    const mockSessions = [
      {
        sessionId: '1_2',
        sessionName: '李四',
        sessionAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=James',
        lastMessageContent: '好的，没问题',
        lastMessageTime: new Date(Date.now() - 5 * 60000).toISOString(),
        unreadCount: 2,
        targetType: 'person',
        targetId: '2',
        sessionType: SESSION_TYPE.SINGLE,
      },
      {
        sessionId: 'group_g1',
        sessionName: '开发团队',
        sessionAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Group1',
        lastMessageContent: '张三：我会准时参加会议',
        lastMessageTime: new Date(Date.now() - 15 * 60000).toISOString(),
        unreadCount: 0,
        targetType: 'group',
        targetId: 'g1',
        sessionType: SESSION_TYPE.GROUP,
      },
      {
        sessionId: '1_3',
        sessionName: '王五',
        sessionAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix',
        lastMessageContent: '图片',
        lastMessageTime: new Date(Date.now() - 30 * 60000).toISOString(),
        unreadCount: 1,
        targetType: 'person',
        targetId: '3',
        sessionType: SESSION_TYPE.SINGLE,
      },
      {
        sessionId: 'group_g2',
        sessionName: '设计小组',
        sessionAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Group2',
        lastMessageContent: '刘八：设计稿已上传',
        lastMessageTime: new Date(Date.now() - 2 * 3600000).toISOString(),
        unreadCount: 0,
        targetType: 'group',
        targetId: 'g2',
        sessionType: SESSION_TYPE.GROUP,
      },
      {
        sessionId: '1_5',
        sessionName: '张三',
        sessionAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Susan',
        lastMessageContent: '稍后再聊',
        lastMessageTime: new Date(Date.now() - 1 * 3600000).toISOString(),
        unreadCount: 0,
        targetType: 'person',
        targetId: '5',
        sessionType: SESSION_TYPE.SINGLE,
      },
    ]

    sessions.value = mockSessions.sort(
      (a, b) => new Date(b.lastMessageTime) - new Date(a.lastMessageTime),
    )

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
      lastMessageSenderId: CURRENT_USER_ID,
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
    let msgList = []
    const res = await service.get('/chat/message/list', {
      params: { sessionId },
    })
    if (res.code === 200 && Array.isArray(res.data)) {
      msgList = res.data
      await ChatStorage.insertMessages(sessionId, msgList)
    } else {
      msgList = await ChatStorage.queryMessages(sessionId)
    }
    const cleanedList = msgList.map((item) => cleanMessage(item))
    messages.value = cleanedList
    autoDownloadFiles(cleanedList)
  } catch (e) {
    console.error('加载消息失败', e)
    const localMsgs = await ChatStorage.queryMessages(sessionId)
    messages.value = localMsgs.map((item) => cleanMessage(item))
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
    sender_id: CURRENT_USER_ID,
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
          senderId: CURRENT_USER_ID,
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
        senderId: CURRENT_USER_ID,
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
onMounted(() => {
  // #ifdef H5
  initDeviceDetection()
  // #endif

  // 【修复6：初始化时从本地存储读取用户信息】
  const savedUserInfo = getUserInfoFromStorage()
  if (savedUserInfo) {
    userInfo.value = { ...userInfo.value, ...savedUserInfo }
  }
  loadSessionList()
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
>>> uni-button::after {
  content: '';
}
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
  padding: 4px;
  font-size: 18px;
  line-height: 1;
  font-weight: 400;
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
  background: #07c160;
  color: #fff;
  border: none;
}
.popover-actions .default-btn {
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

/* 个人主页 */
.profile-area {
  width: 260px;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  height: 100%;
  border-right: 1px solid #e6e6e6;
  overflow-y: auto;
}
.profile-header-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 30px 20px;
  background: #fff;
  margin-bottom: 10px;
}
.profile-avatar {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  background: #cccccc;
}
.profile-base-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  min-width: 0;
}
.profile-nickname {
  font-size: 20px;
  font-weight: 500;
  color: #333;
}
.profile-wechat-id {
  font-size: 14px;
  color: #999;
}
.qrcode-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}
.qrcode-text {
  font-size: 14px;
  color: #333;
}
.qrcode-icon {
  font-size: 16px;
  color: #999;
}
.profile-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.profile-card {
  background: #fff;
  display: flex;
  flex-direction: column;
}
.profile-item {
  height: 54px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.2s;
}
.profile-item:hover {
  background: #f5f5f5;
}
.profile-item:last-child {
  border-bottom: none;
}
.profile-item .item-label {
  font-size: 14px;
  color: #333;
  width: 80px;
  flex-shrink: 0;
}
.profile-item .item-value {
  flex: 1;
  font-size: 14px;
  color: #999;
  text-align: right;
  margin-right: 8px;
}
.item-avatar {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  margin-left: auto;
  margin-right: 8px;
}

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

/* 移动端通讯录页面 */
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
