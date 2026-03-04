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
          <view class="red-dot" v-if="pendingNotifyCount > 0">{{
            pendingNotifyCount
          }}</view>
        </view>
        <view class="sidebar-item" @click.stop="openCloudDrive">
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
    <view class="session-list" v-if="currentSidebarTab === 'chat' && !showCloudDrive">
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
          <text class="add-icon" :class="{ 'add-icon-active': showPlusMenu }"
            >➕</text
          >
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
            :src="item.sessionAvatar || defaultAvatar"
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
          <view class="tab-notify-badge" v-if="pendingNotifyCount > 0">{{
            pendingNotifyCount
          }}</view>
        </view>
      </view>
      <view class="contact-scroll">
        <!-- 好友列表 -->
        <view v-if="selectedFriendTab === 'friends'">
          <!-- 添加朋友入口：与好友项同高、同结构，点击显示悬浮窗 -->
          <view
            class="contact-item friend-item add-friend-entry"
            @click.stop="openAddFriendPopover"
          >
            <view class="person-avatar add-friend-avatar">
              <text class="add-friend-plus">+</text>
            </view>
            <view class="contact-info">
              <view class="contact-name">添加朋友</view>
              <view class="contact-desc">搜索用户名或手机号添加</view>
            </view>
          </view>
          <view
            class="contact-item friend-item"
            v-for="friend in filteredFriends"
            :key="friend.userId"
            @click="selectFriend(friend)"
          >
            <image
              :src="friend.avatar || defaultAvatar"
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
            <view
              class="notify-section"
              v-if="notifications.friendApplies.length > 0"
            >
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
                  <text class="notify-time">{{
                    formatMessageTime(apply.createTime)
                  }}</text>
                </view>
                <view class="notify-actions" v-if="apply.status === 0">
                  <button
                    class="notify-btn accept-btn"
                    @click.stop="handleFriendApply(apply.id, 1)"
                  >
                    接受
                  </button>
                  <button
                    class="notify-btn reject-btn"
                    @click.stop="handleFriendApply(apply.id, 2)"
                  >
                    拒绝
                  </button>
                </view>
                <text
                  class="notify-status-tag"
                  :class="apply.status === 1 ? 'tag-accepted' : 'tag-rejected'"
                  v-else
                >
                  {{ apply.status === 1 ? '已接受' : '已拒绝' }}
                </text>
              </view>
            </view>

            <!-- 群聊申请（作为群主/管理员收到的） -->
            <view
              class="notify-section"
              v-if="notifications.groupApplies.length > 0"
            >
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
                  <text class="notify-group-name"
                    >申请加入 「{{ apply.groupName }}」</text
                  >
                  <text class="notify-time">{{
                    formatMessageTime(apply.createTime)
                  }}</text>
                </view>
                <view class="notify-actions">
                  <button
                    class="notify-btn accept-btn"
                    @click.stop="handleGroupApply(apply.id, 1)"
                  >
                    同意
                  </button>
                  <button
                    class="notify-btn reject-btn"
                    @click.stop="handleGroupApply(apply.id, 2)"
                  >
                    拒绝
                  </button>
                </view>
              </view>
            </view>

            <!-- 空状态 -->
            <view
              class="notify-empty"
              v-if="
                !notifications.friendApplies.length &&
                !notifications.groupApplies.length
              "
            >
              <text class="notify-empty-icon">🔔</text>
              <text class="notify-empty-text">暂无新通知</text>
            </view>
          </template>
        </view>
      </view>
    </view>

    <!-- 添加朋友悬浮窗：带搜索框，符合 PC 端习惯，点击遮罩或关闭按钮关闭 -->
    <view
      v-if="showAddFriendPopover"
      class="add-friend-popover-mask"
      @click.self="closeAddFriendPopover"
    >
      <view class="add-friend-popover" @click.stop>
        <view class="add-friend-popover-header">
          <text class="add-friend-popover-title">添加朋友</text>
          <text class="add-friend-popover-close" @click="closeAddFriendPopover"
            >✕</text
          >
        </view>
        <view class="add-friend-popover-search">
          <text class="add-friend-search-icon">🔍</text>
          <input
            v-model="addFriendKeyword"
            class="add-friend-search-input"
            placeholder="搜索用户名或手机号"
            @keyup.enter="doAddFriendSearch"
          />
          <text
            v-if="addFriendKeyword"
            class="add-friend-search-clear"
            @click="clearAddFriendSearch"
            >✕</text
          >
        </view>
        <view class="add-friend-popover-actions">
          <button
            class="add-friend-search-btn"
            :disabled="!addFriendKeyword.trim() || addFriendPopoverLoading"
            @click="doAddFriendSearch"
          >
            {{ addFriendPopoverLoading ? '搜索中...' : '搜索' }}
          </button>
        </view>
        <view class="add-friend-popover-body">
          <view v-if="!addFriendSearched" class="add-friend-hint">
            <text class="add-friend-hint-icon">👤</text>
            <text class="add-friend-hint-text"
              >输入用户名或手机号，搜索添加新朋友</text
            >
          </view>
          <view v-else-if="addFriendPopoverLoading" class="add-friend-loading">
            <text>搜索中...</text>
          </view>
          <view
            v-else-if="addFriendSearchResult === null"
            class="add-friend-empty"
          >
            <text class="add-friend-empty-icon">🔍</text>
            <text class="add-friend-empty-text"
              >未找到该用户，请确认用户名是否正确</text
            >
          </view>
          <view v-else class="add-friend-result">
            <image
              :src="addFriendSearchResult.avatar || defaultAvatar"
              class="add-friend-result-avatar"
              mode="aspectFill"
            />
            <view class="add-friend-result-info">
              <text class="add-friend-result-name">{{
                addFriendSearchResult.nickname
              }}</text>
              <text class="add-friend-result-username"
                >用户名：{{ addFriendSearchResult.username }}</text
              >
              <text
                v-if="addFriendSearchResult.signature"
                class="add-friend-result-sig"
                >{{ addFriendSearchResult.signature }}</text
              >
            </view>
            <view class="add-friend-result-action">
              <button
                v-if="addFriendSearchResult.isFriend"
                class="add-friend-btn disabled"
                disabled
              >
                已是好友
              </button>
              <button
                v-else-if="addFriendSearchResult.isApplied"
                class="add-friend-btn pending"
                disabled
              >
                已申请
              </button>
              <button
                v-else
                class="add-friend-btn add"
                :disabled="addFriendApplying"
                @click="showAddFriendRemark = true"
              >
                {{ addFriendApplying ? '发送中...' : '+加好友' }}
              </button>
            </view>
          </view>
          <view
            v-if="
              showAddFriendRemark &&
              addFriendSearchResult &&
              !addFriendSearchResult.isFriend &&
              !addFriendSearchResult.isApplied
            "
            class="add-friend-remark-panel"
          >
            <text class="add-friend-remark-label">验证信息（选填）</text>
            <textarea
              v-model="addFriendRemark"
              class="add-friend-remark-input"
              placeholder="请输入验证信息"
              maxlength="100"
            />
            <view class="add-friend-remark-footer">
              <text class="add-friend-remark-count"
                >{{ addFriendRemark.length }}/100</text
              >
              <view class="add-friend-remark-btns">
                <button
                  class="add-friend-btn default"
                  @click="showAddFriendRemark = false"
                >
                  取消
                </button>
                <button
                  class="add-friend-btn add"
                  :disabled="addFriendApplying"
                  @click="sendAddFriendApply"
                >
                  发送申请
                </button>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 个人主页设置页，提取为 SettingsPanel 独立组件，双栏布局维护性更好 -->
    <view class="profile-area" v-else-if="currentSidebarTab === 'profile'">
      <SettingsPanel
        :userInfo="userInfo"
        :defaultAvatar="defaultAvatar"
        @openEditProfile="openEditProfile"
        @openQrCode="showQrCode = true"
        @logout="handleLogout"
      />
    </view>

    <!-- 网盘区域：根据 showCloudDrive 状态显示 -->
    <CloudDrive
      v-if="currentSidebarTab === 'chat' && showCloudDrive"
      :key="'cloud-drive-' + CURRENT_USER_ID"
    />

    <!-- 聊天区域：输入/发送/录音/拖拽由 ChatArea 组件 + useSendMessage composable 统一管理 -->
    <ChatArea
      v-else-if="currentSidebarTab === 'chat' && !showCloudDrive && currentSession"
      :currentSession="currentSession"
      :messagesWithTime="messagesWithTime"
      :showChatInfoPanel="showChatInfoPanel"
      :friendForSession="getFriendForCurrentSession()"
      :groupMembers="groupMembers"
      :currentUserRole="getCurrentUserRole()"
      :doNotDisturb="doNotDisturb"
      :pinned="pinned"
      :scrollToView="scrollToView"
      :showScrollToBottom="showScrollToBottom"
      :isRecording="isRecording"
      :recordingDuration="recordingDuration"
      :pendingFiles="pendingFiles"
      v-model:inputMsg="inputMsg"
      :isSending="isSending"
      :isInputDragOver="isInputDragOver"
      :showEmojiPanel="showEmojiPanel"
      :recentEmojis="recentEmojis"
      :allEmojis="allEmojis"
      :defaultAvatar="defaultAvatar"
      :MESSAGE_TYPE="MESSAGE_TYPE"
      :SESSION_TYPE="SESSION_TYPE"
      :currentUserId="CURRENT_USER_ID"
      @toggleChatInfo="toggleChatInfoPanel"
      @openProfile="openProfileFromPanel"
      @findContent="findChatContent"
      @toggleDoNotDisturb="toggleDoNotDisturb"
      @togglePin="togglePinChat"
      @clearRecords="clearChatRecords"
      @chatScroll="onChatScroll"
      @scrollToBottom="scrollToBottom"
      @imageClick="handleImageClick"
      @voiceClick="handleVoiceClick"
      @fileClick="handleFileClick"
      @cancelVoice="cancelVoice"
      @stopAndSendVoice="stopAndSendVoice"
      @toggleVoice="toggleVoiceRecording"
      @toggleEmoji="toggleEmojiPanel"
      @insertEmoji="insertEmoji"
      @chooseImage="chooseImage"
      @chooseFile="chooseFile"
      @removePendingFile="removePendingFile"
      @dragEnter="onInputDragEnter"
      @dragOver="onInputDragOver"
      @dragLeave="onInputDragLeave"
      @drop="onInputDrop"
      @enterKey="handleEnterKey"
      @ctrlEnter="handleCtrlEnter"
      @send="sendMessageWithFiles"
      @revokeMsg="revokeMessage"
      @copyMsg="handlePcCopy"
      @addMember="openAddMemberModal"
      @removeMember="removeGroupMember"
      @voiceCall="handleVoiceCall"
      @videoCall="handleVideoCall"
      @uploadToCloud="handleUploadToCloud"
    />

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

    <!-- 添加好友弹窗：弹窗内部的搜索/申请状态完全由子组件自管理 -->
    <AddFriendModal
      :show="showAddFriendModal"
      :defaultAvatar="defaultAvatar"
      @close="showAddFriendModal = false"
      @applied="loadNotifications"
    />

    <!-- 加入群聊弹窗：弹窗内部的搜索/申请状态完全由子组件自管理 -->
    <JoinGroupModal
      :show="showJoinGroupModal"
      :currentUserId="CURRENT_USER_ID"
      :defaultAvatar="defaultAvatar"
      @close="showJoinGroupModal = false"
      @joined="loadGroups"
    />

    <!-- 添加成员：PC 端悬浮窗（联系人 + 顶部搜索），符合 PC 习惯 -->
    <view
      v-if="showAddMemberModal && !isMobileView"
      class="add-member-popover-mask"
      @click.self="showAddMemberModal = false"
    >
      <view class="add-member-popover" @click.stop>
        <view class="add-member-popover-header">
          <text class="add-member-popover-title">{{
            currentSession?.sessionType === SESSION_TYPE.GROUP ? '邀请入群' : '发起群聊'
          }}</text>
          <text class="add-member-popover-close" @click="showAddMemberModal = false">✕</text>
        </view>
        <view class="add-member-popover-search">
          <text class="add-member-search-icon">🔍</text>
          <input
            v-model="addMemberSearchKeyword"
            class="add-member-search-input"
            placeholder="搜索联系人"
            type="text"
          />
        </view>
        <scroll-view scroll-y class="add-member-popover-list">
          <view
            v-for="friend in filteredAddMemberCandidates"
            :key="friend.userId"
            class="add-member-popover-item"
            @click="toggleAddMemberSelect(friend.userId)"
          >
            <view
              class="add-member-checkbox"
              :class="{ 'cg-checked': addMemberSelectedFriends.includes(friend.userId) }"
            >
              <text v-if="addMemberSelectedFriends.includes(friend.userId)" class="cg-check-mark">✓</text>
            </view>
            <image
              :src="friend.avatar || defaultAvatar"
              class="add-member-avatar"
              mode="aspectFill"
            />
            <text class="add-member-name">{{ friend.nickname }}</text>
          </view>
          <view v-if="filteredAddMemberCandidates.length === 0" class="add-member-empty">
            <text>暂无可添加的好友</text>
          </view>
        </scroll-view>
        <view class="add-member-popover-footer">
          <button
            class="add-member-confirm-btn"
            :class="{ 'add-member-confirm-active': addMemberSelectedFriends.length > 0 }"
            :disabled="addMemberSelectedFriends.length === 0 || isAddingMember"
            @click="confirmAddMember"
          >
            {{ isAddingMember ? '添加中...' : '确定' }}{{ addMemberSelectedFriends.length > 0 ? `（${addMemberSelectedFriends.length}）` : '' }}
          </button>
        </view>
      </view>
    </view>

    <!-- 添加成员：移动端全屏弹窗 -->
    <view v-if="showAddMemberModal && isMobileView" class="mobile-create-group-page">
      <view class="mobile-chat-header">
        <text class="mobile-back-btn" @click="showAddMemberModal = false">取消</text>
        <text class="mobile-chat-title">{{
          currentSession?.sessionType === SESSION_TYPE.GROUP ? '邀请入群' : '发起群聊'
        }}</text>
        <text
          class="mobile-confirm-btn"
          :class="{ 'mobile-confirm-active': addMemberSelectedFriends.length > 0 }"
          @click="confirmAddMember"
        >完成({{ addMemberSelectedFriends.length }})</text>
      </view>
      <scroll-view scroll-y class="mobile-cg-scroll">
        <view
          v-for="friend in addMemberCandidates"
          :key="friend.userId"
          class="mobile-cg-item"
          @click="toggleAddMemberSelect(friend.userId)"
        >
          <view
            class="mobile-cg-checkbox"
            :class="{ 'cg-checked': addMemberSelectedFriends.includes(friend.userId) }"
          >
            <text v-if="addMemberSelectedFriends.includes(friend.userId)" class="cg-check-mark">✓</text>
          </view>
          <image
            :src="friend.avatar || defaultAvatar"
            class="mobile-cg-avatar"
            mode="aspectFill"
          />
          <text class="mobile-cg-name">「{{ friend.nickname }}」</text>
        </view>
        <view v-if="addMemberCandidates.length === 0" class="mobile-cg-empty">
          <text>暂无可添加的好友</text>
        </view>
      </scroll-view>
      <view class="mobile-cg-footer">
        <view
          class="mobile-cg-submit-btn"
          :class="{
            'cg-submit-disabled': addMemberSelectedFriends.length === 0 || isAddingMember,
          }"
          @click="confirmAddMember"
        >
          {{
            isAddingMember
              ? '添加中...'
              : (currentSession?.sessionType === SESSION_TYPE.GROUP ? '邀请加入群聊' : '发起群聊') +
                (addMemberSelectedFriends.length > 0 ? `（已选 ${addMemberSelectedFriends.length} 人）` : '')
          }}
        </view>
      </view>
    </view>

    <!-- 我的二维码弹窗：将 wechatId 编码为 QR 供他人扫码添加好友 -->
    <QrCodeModal
      :show="showQrCode"
      :wechatId="userInfo.wechatId"
      :nickname="userInfo.nickname"
      :avatar="userInfo.avatar || defaultAvatar"
      @close="showQrCode = false"
    />
    
    <!-- 语音通话组件 -->
    <VoiceCall
      v-if="showVoiceCall"
      :visible="showVoiceCall"
      :direction="voiceCallDirection"
      :peer-info="voiceCallPeerInfo"
      :session-id="voiceCallSessionId"
      :mode="voiceCallMode"
      @close="handleVoiceCallClose"
      @state-change="handleVoiceCallStateChange"
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
      @click="showMobilePlusMenu = false"
    >
      <!-- 顶部导航栏 -->
      <view class="mobile-navbar">
        <text class="mobile-title">微信</text>
        <view class="mobile-nav-icons">
          <text class="mobile-icon" @click.stop="toggleSearch">🔍</text>
          <!-- ➕ 下拉菜单：添加朋友 / 加入群聊 -->
          <view class="mobile-plus-wrapper" @click.stop>
            <text
              class="mobile-icon"
              :class="{ 'mobile-icon-active': showMobilePlusMenu }"
              @click="showMobilePlusMenu = !showMobilePlusMenu"
              >➕</text
            >
            <view v-if="showMobilePlusMenu" class="mobile-plus-dropdown">
              <view class="mobile-plus-item" @click="openMobileAddFriend">
                <text class="mobile-plus-icon">👤</text>
                <text class="mobile-plus-text">添加好友</text>
              </view>
              <view class="mobile-plus-item" @click="openMobileCreateGroup">
                <text class="mobile-plus-icon">👥</text>
                <text class="mobile-plus-text">发起群聊</text>
              </view>
              <view class="mobile-plus-item" @click="mobileScan">
                <text class="mobile-plus-icon">📷</text>
                <text class="mobile-plus-text">扫一扫</text>
              </view>
            </view>
          </view>
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
      v-else-if="
        mobileCurrentTab === 'chat' && currentMobileChat && !mobileChatInfoPage
      "
      class="mobile-chat-detail"
    >
      <!-- 聊天顶部栏：返回按钮 + 会话名称 + 信息页入口 -->
      <view class="mobile-chat-header">
        <text class="mobile-back-btn" @click="backToMobileList"
          >← {{ currentMobileChat.sessionName }}</text
        >
        <!-- ··· 直接进入全屏聊天信息页 -->
        <text class="mobile-more-icon" @click="enterMobileChatInfo"
          >···</text
        >
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
              @longpress="showMsgContextMenu(item.msg)"
            >
              <image
                :src="item.msg.avatar || defaultAvatar"
                class="mobile-bubble-avatar"
              />

              <view class="mobile-bubble-content">
                <!-- 已撤回消息 -->
                <view v-if="item.msg.status === 3" class="mobile-revoke-msg">
                  {{ item.msg.content_replaced || '[消息已撤回]' }}
                </view>

                <!-- 文本消息 -->
                <view
                  v-else-if="
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

                <!-- 语音消息 -->
                <text
                  v-else-if="item.msg.message_type === MESSAGE_TYPE.AUDIO"
                  class="mobile-emoji-msg"
                >
                  🎤 语音
                </text>

                <!-- 文件消息 -->
                <view
                  v-else-if="
                    item.msg.message_type === MESSAGE_TYPE.FILE
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

      <!-- 输入栏：仿微信移动端风格 -->
      <view class="mobile-input-bar">
        <!-- 主输入行：[语音] [输入框] [表情] [更多/发送] -->
        <view class="mobile-input-row">
          <text class="mobile-input-action" @click="startMobileRecord">🎤</text>
          <input
            v-model="inputMsg"
            type="text"
            placeholder="发送消息"
            class="mobile-text-input"
            @keyup.enter="sendMessageWithFiles"
            @focus="onMobileInputFocus"
          />
          <text class="mobile-input-action" @click="toggleMobileEmoji">😊</text>
          <text
            v-if="hasContentToSend"
            class="mobile-send-btn"
            @click="sendMessageWithFiles"
          >发送</text>
          <text
            v-else
            class="mobile-input-action mobile-input-more"
            @click="showMobileTools = !showMobileTools; showEmojiMobile = false"
          >➕</text>
        </view>

        <!-- 表情面板 -->
        <view v-if="showEmojiMobile" class="mobile-emoji-picker">
          <scroll-view scroll-y class="mobile-emoji-scroll">
            <view class="mobile-emoji-grid">
              <text
                v-for="emoji in allEmojis"
                :key="emoji"
                class="mobile-emoji-item"
                @click="insertMobileEmoji(emoji)"
                >{{ emoji }}</text
              >
            </view>
          </scroll-view>
        </view>

        <!-- 工具面板：图片、文件 -->
        <view v-if="showMobileTools" class="mobile-tools-grid">
          <view class="mobile-tool-item" @click="onChooseMobileImage">
            <view class="mobile-tool-icon-wrap">🖼️</view>
            <text class="mobile-tool-label">图片</text>
          </view>
          <view class="mobile-tool-item" @click="showMobileTools = false">
            <view class="mobile-tool-icon-wrap">📁</view>
            <text class="mobile-tool-label">文件</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 聊天信息页 (H5)：全屏设置页，仿微信风格 -->
    <view
      v-else-if="
        mobileCurrentTab === 'chat' && currentMobileChat && mobileChatInfoPage
      "
      class="mobile-chat-info-full"
    >
      <!-- 顶部导航 -->
      <view class="mobile-chat-header">
        <text class="mobile-back-btn" @click="mobileChatInfoPage = false"
          >← 返回</text
        >
        <text class="mobile-chat-title">聊天信息</text>
        <view style="width: 60px"></view>
      </view>

      <scroll-view scroll-y class="mobile-chat-info-scroll">
        <!-- 成员头像区 -->
        <view class="mobile-chat-info-members">
          <!-- 当前会话头像 -->
          <view class="mobile-chat-info-member">
            <image
              :src="currentMobileChat.sessionAvatar || defaultAvatar"
              class="mobile-info-member-avatar"
              mode="aspectFill"
            />
            <text class="mobile-info-member-name">{{
              currentMobileChat.sessionName
            }}</text>
          </view>
          
          <!-- 群成员头像（仅群聊显示） -->
          <template v-if="currentMobileChat?.sessionType === SESSION_TYPE.GROUP">
            <view
              class="mobile-chat-info-member"
              v-for="member in groupMembers"
              :key="member.userId"
              @click="openProfileFromMobile(member)"
            >
              <image
                :src="member.avatar || defaultAvatar"
                class="mobile-info-member-avatar"
                mode="aspectFill"
              />
              <text class="mobile-info-member-name">{{ member.nickname }}</text>
              <!-- 移除成员按钮（仅管理员和群主显示） -->
              <view 
                v-if="canRemoveMemberInMobile(member)"
                class="mobile-remove-member-btn"
                @click.stop="removeGroupMember(member, currentMobileChat.targetId)"
              >
                ×
              </view>
            </view>
          </template>
          
          <!-- 添加成员按钮 -->
          <view class="mobile-chat-info-member" @click="openAddMemberModal">
            <view class="mobile-chat-info-add">
              <text class="mobile-chat-info-add-icon">+</text>
            </view>
            <text class="mobile-info-member-name">添加</text>
          </view>
        </view>

        <!-- 查找聊天记录 -->
        <view class="mobile-chat-info-section">
          <view class="mobile-chat-info-row" @click="findChatContent">
            <text class="mobile-chat-info-label">查找聊天记录</text>
            <text class="mobile-chat-info-arrow">›</text>
          </view>
        </view>

        <!-- 开关选项 -->
        <view class="mobile-chat-info-section">
          <view class="mobile-chat-info-row">
            <text class="mobile-chat-info-label">消息免打扰</text>
            <view
              class="mobile-switch"
              :class="{ 'mobile-switch-on': doNotDisturb }"
              @click="toggleDoNotDisturb"
            >
              <view class="mobile-switch-thumb"></view>
            </view>
          </view>
          <view class="mobile-chat-info-row">
            <text class="mobile-chat-info-label">置顶聊天</text>
            <view
              class="mobile-switch"
              :class="{ 'mobile-switch-on': pinned }"
              @click="togglePinChat"
            >
              <view class="mobile-switch-thumb"></view>
            </view>
          </view>
        </view>

        <!-- 清空记录 -->
        <view class="mobile-chat-info-section">
          <view class="mobile-chat-info-row" @click="onClearChatRecordsMobile">
            <text class="mobile-chat-info-label mobile-chat-info-danger">清空聊天记录</text>
            <text class="mobile-chat-info-arrow">&gt;</text>
          </view>
        </view>
      </scroll-view>
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
      <!-- 好友/群聊/通知切换 -->
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
        <!-- 通知 tab：待处理数量大于 0 时展示红点 -->
        <view
          class="mobile-tab-item mobile-notify-tab"
          :class="{ 'mobile-active': selectedFriendTab === 'notify' }"
          @click="selectedFriendTab = 'notify'"
        >
          <text>通知</text>
          <view class="mobile-tab-badge" v-if="pendingNotifyCount > 0">{{
            pendingNotifyCount
          }}</view>
        </view>
      </view>
      <scroll-view scroll-y class="mobile-contacts-scroll">
        <!-- 好友列表 -->
        <view v-if="selectedFriendTab === 'friends'">
          <view
            class="mobile-contact-item"
            v-for="friend in filteredFriends"
            :key="friend.userId"
            @click="chatWithFriend(friend)"
          >
            <image
              :src="friend.avatar || defaultAvatar"
              class="mobile-avatar"
              mode="aspectFill"
            />
            <view class="mobile-session-content">
              <text class="mobile-session-name">{{ friend.nickname }}</text>
              <text class="mobile-last-msg">{{ friend.signature }}</text>
            </view>
          </view>
        </view>
        <!-- 群聊列表 -->
        <view v-else-if="selectedFriendTab === 'groups'">
          <view
            class="mobile-contact-item"
            v-for="group in filteredGroups"
            :key="group.groupId"
            @click="chatWithGroup(group)"
          >
            <image
              :src="group.groupAvatar || defaultAvatar"
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
        <!-- 通知列表：复用与 PC 端相同的通知模板，接受处理函数也共用 -->
        <view v-else-if="selectedFriendTab === 'notify'">
          <view class="notify-loading" v-if="notifyLoading">
            <text>加载中...</text>
          </view>
          <template v-else>
            <!-- 好友申请 -->
            <view
              class="notify-section"
              v-if="notifications.friendApplies.length > 0"
            >
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
                  <text class="notify-time">{{
                    formatMessageTime(apply.createTime)
                  }}</text>
                </view>
                <view class="notify-actions" v-if="apply.status === 0">
                  <button
                    class="notify-btn accept-btn"
                    @click.stop="handleFriendApply(apply.id, 1)"
                  >
                    接受
                  </button>
                  <button
                    class="notify-btn reject-btn"
                    @click.stop="handleFriendApply(apply.id, 2)"
                  >
                    拒绝
                  </button>
                </view>
                <text
                  class="notify-status-tag"
                  :class="apply.status === 1 ? 'tag-accepted' : 'tag-rejected'"
                  v-else
                >
                  {{ apply.status === 1 ? '已接受' : '已拒绝' }}
                </text>
              </view>
            </view>
            <!-- 群聊申请（作为群主/管理员收到的） -->
            <view
              class="notify-section"
              v-if="notifications.groupApplies.length > 0"
            >
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
                  <text class="notify-group-name"
                    >申请加入 「{{ apply.groupName }}」</text
                  >
                  <text class="notify-time">{{
                    formatMessageTime(apply.createTime)
                  }}</text>
                </view>
                <view class="notify-actions">
                  <button
                    class="notify-btn accept-btn"
                    @click.stop="handleGroupApply(apply.id, 1)"
                  >
                    同意
                  </button>
                  <button
                    class="notify-btn reject-btn"
                    @click.stop="handleGroupApply(apply.id, 2)"
                  >
                    拒绝
                  </button>
                </view>
              </view>
            </view>
            <!-- 空状态 -->
            <view
              class="notify-empty"
              v-if="
                !notifications.friendApplies.length &&
                !notifications.groupApplies.length
              "
            >
              <text class="notify-empty-icon">🔔</text>
              <text class="notify-empty-text">暂无新通知</text>
            </view>
          </template>
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

    <view
      class="mobile-bottom-tab-bar"
      v-show="!(mobileCurrentTab === 'chat' && currentMobileChat)"
    >
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
        :class="{ 'mobile-active': mobileCurrentTab === 'cloud' }"
        @click="setMobileTab('cloud')"
      >
        <text class="mobile-tab-icon">📁</text>
        <text class="mobile-tab-label">网盘</text>
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

    <!-- 发起群聊：全屏选人弹窗（position absolute 覆盖整个容器）-->
    <view v-if="showMobileCreateGroup" class="mobile-create-group-page">
      <!-- 顶部操作栏 -->
      <view class="mobile-chat-header">
        <text class="mobile-back-btn" @click="showMobileCreateGroup = false"
          >取消</text
        >
        <text class="mobile-chat-title">发起群聊</text>
        <text
          class="mobile-confirm-btn"
          :class="{
            'mobile-confirm-active': createGroupSelectedFriends.length > 0,
          }"
          @click="submitCreateGroup"
          >完成({{ createGroupSelectedFriends.length }})</text
        >
      </view>

      <!-- 群头像上传 -->
      <view class="mobile-cg-avatar-section" @click="chooseGroupAvatar">
        <image
          v-if="createGroupAvatarUrl"
          :src="createGroupAvatarUrl"
          class="mobile-cg-avatar-preview"
          mode="aspectFill"
        />
        <view v-else class="mobile-cg-avatar-placeholder">
          <text class="mobile-cg-avatar-icon">📷</text>
          <text class="mobile-cg-avatar-hint">{{
            isUploadingGroupAvatar ? '上传中...' : '点击设置群头像'
          }}</text>
        </view>
      </view>

      <!-- 群名输入框 -->
      <view class="mobile-cg-name-wrap">
        <input
          v-model="createGroupName"
          placeholder="输入群名（不填则自动生成）"
          class="mobile-cg-name-input"
          maxlength="30"
        />
      </view>

      <!-- 好友列表多选 -->
      <scroll-view scroll-y class="mobile-cg-scroll">
        <view
          v-for="friend in friends"
          :key="friend.userId"
          class="mobile-cg-item"
          @click="toggleFriendSelect(friend.userId)"
        >
          <!-- 自定义圆形 checkbox -->
          <view
            class="mobile-cg-checkbox"
            :class="{
              'cg-checked': createGroupSelectedFriends.includes(friend.userId),
            }"
          >
            <text
              v-if="createGroupSelectedFriends.includes(friend.userId)"
              class="cg-check-mark"
              >✓</text
            >
          </view>
          <image
            :src="friend.avatar || defaultAvatar"
            class="mobile-cg-avatar"
            mode="aspectFill"
          />
          <text class="mobile-cg-name">「{{ friend.nickname }}」</text>
        </view>
        <view v-if="friends.length === 0" class="mobile-cg-empty">
          <text>暂无好友，先添加好友吧</text>
        </view>
      </scroll-view>

      <!-- 底部进入群聊按钮 -->
      <view class="mobile-cg-footer">
        <view
          class="mobile-cg-submit-btn"
          :class="{
            'cg-submit-disabled':
              createGroupSelectedFriends.length === 0 || isCreatingGroup,
          }"
          @click="submitCreateGroup"
        >
          {{
            isCreatingGroup
              ? '创建中...'
              : `发起群聊${createGroupSelectedFriends.length > 0 ? '（已选 ' + createGroupSelectedFriends.length + ' 人）' : ''}`
          }}
        </view>
      </view>
    </view>

    <!-- 添加成员：全屏选人弹窗（H5）-->
    <view v-if="showAddMemberModal" class="mobile-create-group-page">
      <view class="mobile-chat-header">
        <text class="mobile-back-btn" @click="showAddMemberModal = false"
          >取消</text
        >
        <text class="mobile-chat-title">{{
          currentMobileChat?.sessionType === SESSION_TYPE.GROUP
            ? '邀请入群'
            : '发起群聊'
        }}</text>
        <text
          class="mobile-confirm-btn"
          :class="{
            'mobile-confirm-active': addMemberSelectedFriends.length > 0,
          }"
          @click="confirmAddMember"
          >完成({{ addMemberSelectedFriends.length }})</text
        >
      </view>
      <scroll-view scroll-y class="mobile-cg-scroll">
        <view
          v-for="friend in addMemberCandidates"
          :key="friend.userId"
          class="mobile-cg-item"
          @click="toggleAddMemberSelect(friend.userId)"
        >
          <view
            class="mobile-cg-checkbox"
            :class="{
              'cg-checked': addMemberSelectedFriends.includes(friend.userId),
            }"
          >
            <text
              v-if="addMemberSelectedFriends.includes(friend.userId)"
              class="cg-check-mark"
              >✓</text
            >
          </view>
          <image
            :src="friend.avatar || defaultAvatar"
            class="mobile-cg-avatar"
            mode="aspectFill"
          />
          <text class="mobile-cg-name">「{{ friend.nickname }}」</text>
        </view>
        <view v-if="addMemberCandidates.length === 0" class="mobile-cg-empty">
          <text>暂无可添加的好友</text>
        </view>
      </scroll-view>
      <view class="mobile-cg-footer">
        <view
          class="mobile-cg-submit-btn"
          :class="{
            'cg-submit-disabled':
              addMemberSelectedFriends.length === 0 || isAddingMember,
          }"
          @click="confirmAddMember"
        >
          {{
            isAddingMember
              ? '添加中...'
              : (currentMobileChat?.sessionType === SESSION_TYPE.GROUP
                  ? '邀请加入群聊'
                  : '发起群聊') +
                (addMemberSelectedFriends.length > 0
                  ? `（已选 ${addMemberSelectedFriends.length} 人）`
                  : '')
          }}
        </view>
      </view>
    </view>

    <!-- 添加好友：全屏搜索页（H5）-->
    <view v-if="showMobileAddFriend" class="mobile-add-friend-page">
      <view class="mobile-chat-header">
        <text class="mobile-back-btn" @click="showMobileAddFriend = false"
          >← 返回</text
        >
        <text class="mobile-chat-title">添加好友</text>
        <view style="width: 60px"></view>
      </view>
      <view class="mobile-af-search-bar">
        <input
          v-model="mobileAddFriendKeyword"
          type="text"
          placeholder="搜索用户名 / 手机号"
          class="mobile-af-search-input"
          @confirm="searchAddFriend"
          @keyup.enter="searchAddFriend"
        />
        <view class="mobile-af-search-btn" @click="searchAddFriend">
          <text>{{ isSearchingFriend ? '搜索中...' : '搜索' }}</text>
        </view>
      </view>
      <view v-if="isSearchingFriend" class="mobile-af-loading">
        <text>正在搜索...</text>
      </view>
      <view v-else-if="addFriendResult" class="mobile-af-result">
        <view class="mobile-af-user-card">
          <image
            :src="addFriendResult.avatar || defaultAvatar"
            class="mobile-af-avatar"
            mode="aspectFill"
          />
          <view class="mobile-af-user-info">
            <text class="mobile-af-nickname">{{
              addFriendResult.nickname
            }}</text>
            <text class="mobile-af-username">{{
              addFriendResult.username
            }}</text>
          </view>
          <view
            class="mobile-af-apply-btn"
            @click="sendFriendApply(addFriendResult.id)"
          >
            <text>{{ isSendingApply ? '发送中...' : '添加好友' }}</text>
          </view>
        </view>
      </view>
      <view
        v-else-if="mobileAddFriendKeyword && !isSearchingFriend"
        class="mobile-af-empty"
      >
        <text>未找到用户，换个关键词试试</text>
      </view>
    </view>

    <!-- 移动端网盘页面（H5） -->
    <view v-else-if="mobileCurrentTab === 'cloud'" class="mobile-cloud-drive-page">
      <!-- 网盘头部栏 -->
      <view class="mobile-chat-header">
        <text class="mobile-back-btn" @click="mobileCurrentTab = 'chat'">← 文件</text>
        <text class="mobile-more-icon">···</text>
      </view>
      <!-- 网盘内容容器 -->
      <view class="mobile-cloud-content">
        <CloudDrive :key="'mobile-cloud-h5-' + CURRENT_USER_ID" />
      </view>
    </view>

    <!-- 消息长按上下文菜单（H5）-->
    <view
      v-if="mobileContextMenu.visible"
      class="mobile-ctx-overlay"
      @click="closeMsgContextMenu"
    >
      <view class="mobile-ctx-menu" @click.stop>
        <view
          v-if="mobileContextMenu.msg?.message_type === MESSAGE_TYPE.TEXT"
          class="mobile-ctx-item"
          @click="copyMobileMsg"
          >复制</view
        >
        <view
          v-if="isMyMessage(mobileContextMenu.msg)"
          class="mobile-ctx-item mobile-ctx-revoke"
          @click="revokeMessage(mobileContextMenu.msg)"
          >撤回</view
        >
        <view
          class="mobile-ctx-item mobile-ctx-cancel"
          @click="closeMsgContextMenu"
          >取消</view
        >
      </view>
    </view>
  </view>
  <!-- #endif -->

  <!-- #ifndef H5 -->
  <!-- App 原生移动端（安卓和苹果） -->
  <view class="mobile-container">
    <!-- 使用相同的移动端布局，因为原生 APP 本身就是移动端 -->
    <!-- 会话列表页面 -->
    <view
      v-if="!currentMobileChat"
      class="mobile-session-list"
      @click="showMobilePlusMenu = false"
    >
      <!-- 顶部导航栏 -->
      <view class="mobile-navbar">
        <text class="mobile-title">微信</text>
        <view class="mobile-nav-icons">
          <text class="mobile-icon" @click.stop="toggleSearch">🔍</text>
          <!-- ➕ 下拉菜单：添加朋友 / 加入群聊 -->
          <view class="mobile-plus-wrapper" @click.stop>
            <text
              class="mobile-icon"
              :class="{ 'mobile-icon-active': showMobilePlusMenu }"
              @click="showMobilePlusMenu = !showMobilePlusMenu"
              >➕</text
            >
            <view v-if="showMobilePlusMenu" class="mobile-plus-dropdown">
              <view class="mobile-plus-item" @click="openMobileAddFriend">
                <text class="mobile-plus-icon">👤</text>
                <text class="mobile-plus-text">添加好友</text>
              </view>
              <view class="mobile-plus-item" @click="openMobileCreateGroup">
                <text class="mobile-plus-icon">👥</text>
                <text class="mobile-plus-text">发起群聊</text>
              </view>
              <view class="mobile-plus-item" @click="mobileScan">
                <text class="mobile-plus-icon">📷</text>
                <text class="mobile-plus-text">扫一扫</text>
              </view>
            </view>
          </view>
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
    <view v-else-if="!mobileChatInfoPage" class="mobile-chat-detail">
      <!-- 聊天顶部栏：返回按钮 + 会话名称 + 信息页入口 -->
      <view class="mobile-chat-header">
        <text class="mobile-back-btn" @click="backToMobileList"
          >← {{ currentMobileChat.sessionName }}</text
        >
        <!-- ··· 直接进入全屏聊天信息页 -->
        <text class="mobile-more-icon" @click="enterMobileChatInfo"
          >···</text
        >
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
              @longpress="showMsgContextMenu(item.msg)"
            >
              <image
                :src="item.msg.avatar || defaultAvatar"
                class="mobile-bubble-avatar"
              />
              <view class="mobile-bubble-content">
                <!-- 已撤回消息 -->
                <view v-if="item.msg.status === 3" class="mobile-revoke-msg">
                  {{ item.msg.content_replaced || '[消息已撤回]' }}
                </view>
                <!-- 文本消息 -->
                <view
                  v-else-if="
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
                <!-- 语音消息 -->
                <text
                  v-else-if="item.msg.message_type === MESSAGE_TYPE.AUDIO"
                  class="mobile-emoji-msg"
                  >🎤 语音</text
                >
                <!-- 文件消息 -->
                <view
                  v-else-if="
                    item.msg.message_type === MESSAGE_TYPE.FILE
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

      <!-- 输入栏：仿微信移动端风格 -->
      <view class="mobile-input-bar">
        <view class="mobile-input-row">
          <text class="mobile-input-action" @click="startMobileRecord">🎤</text>
          <input
            v-model="inputMsg"
            type="text"
            placeholder="发送消息"
            class="mobile-text-input"
            @keyup.enter="sendMessageWithFiles"
            @focus="onMobileInputFocus"
          />
          <text class="mobile-input-action" @click="toggleMobileEmoji">😊</text>
          <text
            v-if="hasContentToSend"
            class="mobile-send-btn"
            @click="sendMessageWithFiles"
          >发送</text>
          <text
            v-else
            class="mobile-input-action mobile-input-more"
            @click="showMobileTools = !showMobileTools; showEmojiMobile = false"
          >➕</text>
        </view>
        <view v-if="showEmojiMobile" class="mobile-emoji-picker">
          <scroll-view scroll-y class="mobile-emoji-scroll">
            <view class="mobile-emoji-grid">
              <text
                v-for="emoji in allEmojis"
                :key="emoji"
                class="mobile-emoji-item"
                @click="insertMobileEmoji(emoji)"
                >{{ emoji }}</text
              >
            </view>
          </scroll-view>
        </view>
        <view v-if="showMobileTools" class="mobile-tools-grid">
          <view class="mobile-tool-item" @click="onChooseMobileImage">
            <view class="mobile-tool-icon-wrap">🖼️</view>
            <text class="mobile-tool-label">图片</text>
          </view>
          <view class="mobile-tool-item" @click="showMobileTools = false">
            <view class="mobile-tool-icon-wrap">📁</view>
            <text class="mobile-tool-label">文件</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 聊天信息页（Native）-->
    <view v-else-if="mobileChatInfoPage" class="mobile-chat-info-full">
      <view class="mobile-chat-header">
        <text class="mobile-back-btn" @click="mobileChatInfoPage = false"
          >← 返回</text
        >
        <text class="mobile-chat-title">聊天信息</text>
        <view style="width: 60px"></view>
      </view>
      <scroll-view scroll-y class="mobile-chat-info-scroll">
        <view class="mobile-chat-info-members">
          <view class="mobile-chat-info-member">
            <image
              :src="currentMobileChat.sessionAvatar || defaultAvatar"
              class="mobile-info-member-avatar"
              mode="aspectFill"
            />
            <text class="mobile-info-member-name">{{
              currentMobileChat.sessionName
            }}</text>
          </view>
          <view class="mobile-chat-info-member" @click="openAddMemberModal">
            <view class="mobile-chat-info-add">
              <text class="mobile-chat-info-add-icon">+</text>
            </view>
            <text class="mobile-info-member-name">添加</text>
          </view>
        </view>
        <view class="mobile-chat-info-section">
          <view class="mobile-chat-info-row" @click="findChatContent">
            <text class="mobile-chat-info-label">查找聊天记录</text>
            <text class="mobile-chat-info-arrow">›</text>
          </view>
        </view>
        <view class="mobile-chat-info-section">
          <view class="mobile-chat-info-row">
            <text class="mobile-chat-info-label">消息免打扰</text>
            <view
              class="mobile-switch"
              :class="{ 'mobile-switch-on': doNotDisturb }"
              @click="toggleDoNotDisturb"
            >
              <view class="mobile-switch-thumb"></view>
            </view>
          </view>
          <view class="mobile-chat-info-row">
            <text class="mobile-chat-info-label">置顶聊天</text>
            <view
              class="mobile-switch"
              :class="{ 'mobile-switch-on': pinned }"
              @click="togglePinChat"
            >
              <view class="mobile-switch-thumb"></view>
            </view>
          </view>
        </view>
        <view class="mobile-chat-info-section">
          <view class="mobile-chat-info-row" @click="onClearChatRecordsMobile">
            <text class="mobile-chat-info-label mobile-chat-info-danger">清空聊天记录</text>
            <text class="mobile-chat-info-arrow">&gt;</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- App端通讯录页面 -->
    <view v-else-if="mobileCurrentTab === 'contact'" class="mobile-contact-page">
      <!-- 顶部栏 -->
      <view class="mobile-chat-header">
        <text class="mobile-back-btn" @click="mobileCurrentTab = 'chat'">← 通讯录</text>
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
      <!-- 好友/群聊/通知切换 -->
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
        <view
          class="mobile-tab-item mobile-notify-tab"
          :class="{ 'mobile-active': selectedFriendTab === 'notify' }"
          @click="selectedFriendTab = 'notify'"
        >
          <text>通知</text>
          <view class="mobile-tab-badge" v-if="pendingNotifyCount > 0">{{
            pendingNotifyCount
          }}</view>
        </view>
      </view>
      <scroll-view scroll-y class="mobile-contacts-scroll">
        <!-- 好友列表 -->
        <view v-if="selectedFriendTab === 'friends'">
          <view
            class="mobile-contact-item"
            v-for="friend in filteredFriends"
            :key="friend.userId"
            @click="chatWithFriend(friend)"
          >
            <image
              :src="friend.avatar || defaultAvatar"
              class="mobile-avatar"
              mode="aspectFill"
            />
            <view class="mobile-session-content">
              <text class="mobile-session-name">{{ friend.nickname }}</text>
              <text class="mobile-last-msg">{{ friend.signature }}</text>
            </view>
          </view>
        </view>
        <!-- 群聊列表 -->
        <view v-else-if="selectedFriendTab === 'groups'">
          <view
            class="mobile-contact-item"
            v-for="group in filteredGroups"
            :key="group.groupId"
            @click="chatWithGroup(group)"
          >
            <image
              :src="group.groupAvatar || defaultAvatar"
              class="mobile-avatar"
              mode="aspectFill"
            />
            <view class="mobile-session-content">
              <text class="mobile-session-name">{{ group.groupName }}</text>
              <text class="mobile-last-msg">{{ group.memberCount }} 个成员</text>
            </view>
          </view>
        </view>
        <!-- 通知列表 -->
        <view v-else-if="selectedFriendTab === 'notify'">
          <view class="notify-loading" v-if="notifyLoading">
            <text>加载中...</text>
          </view>
          <template v-else>
            <view
              class="notify-section"
              v-if="notifications.friendApplies.length > 0"
            >
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
                  <text class="notify-time">{{
                    formatMessageTime(apply.createTime)
                  }}</text>
                </view>
                <view class="notify-actions" v-if="apply.status === 0">
                  <button
                    class="notify-btn accept-btn"
                    @click.stop="handleFriendApply(apply.id, 1)"
                  >
                    接受
                  </button>
                  <button
                    class="notify-btn reject-btn"
                    @click.stop="handleFriendApply(apply.id, 2)"
                  >
                    拒绝
                  </button>
                </view>
              </view>
            </view>
            <view
              class="notify-section"
              v-if="notifications.groupApplies.length > 0"
            >
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
                  <text class="notify-time">{{
                    formatMessageTime(apply.createTime)
                  }}</text>
                </view>
                <view class="notify-actions">
                  <button
                    class="notify-btn accept-btn"
                    @click.stop="handleGroupApply(apply.id, 1)"
                  >
                    同意
                  </button>
                  <button
                    class="notify-btn reject-btn"
                    @click.stop="handleGroupApply(apply.id, 2)"
                  >
                    拒绝
                  </button>
                </view>
              </view>
            </view>
            <view
              class="notify-empty"
              v-if="
                !notifications.friendApplies.length &&
                !notifications.groupApplies.length
              "
            >
              <text class="notify-empty-icon">🔔</text>
              <text class="notify-empty-text">暂无新通知</text>
            </view>
          </template>
        </view>
      </scroll-view>
    </view>

    <!-- App端个人资料页面 -->
    <view v-else-if="mobileCurrentTab === 'profile'" class="mobile-profile-page">
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
            <text class="mobile-profile-wechat-id">微信号：{{ userInfo.wechatId }}</text>
            <view class="mobile-profile-edit-btn" @click="openEditProfile">编辑资料</view>
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
            <text class="mobile-profile-value">{{ userInfo.signature || '这个人很懒，什么都没写' }}</text>
          </view>
          <view class="mobile-profile-item">
            <text class="mobile-profile-label">地区</text>
            <text class="mobile-profile-value">{{ userInfo.region || '未设置' }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- App端网盘页面 -->
    <view v-else-if="mobileCurrentTab === 'cloud'" class="mobile-cloud-drive-page">
      <!-- 网盘头部栏 -->
      <view class="mobile-chat-header">
        <text class="mobile-back-btn" @click="mobileCurrentTab = 'chat'">← 文件</text>
        <text class="mobile-more-icon">···</text>
      </view>
      <!-- 网盘内容容器 -->
      <view class="mobile-cloud-content">
        <CloudDrive :key="'mobile-cloud-app-' + CURRENT_USER_ID" />
      </view>
    </view>

    <!-- 底部导航栏 -->
    <view class="mobile-bottom-tab-bar" v-if="mobileCurrentTab !== 'chat' || !currentMobileChat">
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
        :class="{ 'mobile-active': mobileCurrentTab === 'cloud' }"
        @click="setMobileTab('cloud')"
      >
        <text class="mobile-tab-icon">📁</text>
        <text class="mobile-tab-label">网盘</text>
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

    <!-- 发起群聊：全屏选人弹窗（同 H5，position absolute 覆盖整个容器）-->
    <view v-if="showMobileCreateGroup" class="mobile-create-group-page">
      <view class="mobile-chat-header">
        <text class="mobile-back-btn" @click="showMobileCreateGroup = false"
          >取消</text
        >
        <text class="mobile-chat-title">发起群聊</text>
        <text
          class="mobile-confirm-btn"
          :class="{
            'mobile-confirm-active': createGroupSelectedFriends.length > 0,
          }"
          @click="submitCreateGroup"
          >完成({{ createGroupSelectedFriends.length }})</text
        >
      </view>
      <!-- 群头像上传 -->
      <view class="mobile-cg-avatar-section" @click="chooseGroupAvatar">
        <image
          v-if="createGroupAvatarUrl"
          :src="createGroupAvatarUrl"
          class="mobile-cg-avatar-preview"
          mode="aspectFill"
        />
        <view v-else class="mobile-cg-avatar-placeholder">
          <text class="mobile-cg-avatar-icon">📷</text>
          <text class="mobile-cg-avatar-hint">{{
            isUploadingGroupAvatar ? '上传中...' : '点击设置群头像'
          }}</text>
        </view>
      </view>
      <view class="mobile-cg-name-wrap">
        <input
          v-model="createGroupName"
          placeholder="输入群名（不填则自动生成）"
          class="mobile-cg-name-input"
          maxlength="30"
        />
      </view>
      <scroll-view scroll-y class="mobile-cg-scroll">
        <view
          v-for="friend in friends"
          :key="friend.userId"
          class="mobile-cg-item"
          @click="toggleFriendSelect(friend.userId)"
        >
          <view
            class="mobile-cg-checkbox"
            :class="{
              'cg-checked': createGroupSelectedFriends.includes(friend.userId),
            }"
          >
            <text
              v-if="createGroupSelectedFriends.includes(friend.userId)"
              class="cg-check-mark"
              >✓</text
            >
          </view>
          <image
            :src="friend.avatar || defaultAvatar"
            class="mobile-cg-avatar"
            mode="aspectFill"
          />
          <text class="mobile-cg-name">「{{ friend.nickname }}」</text>
        </view>
        <view v-if="friends.length === 0" class="mobile-cg-empty">
          <text>暂无好友，先添加好友吧</text>
        </view>
      </scroll-view>
      <view class="mobile-cg-footer">
        <view
          class="mobile-cg-submit-btn"
          :class="{
            'cg-submit-disabled':
              createGroupSelectedFriends.length === 0 || isCreatingGroup,
          }"
          @click="submitCreateGroup"
        >
          {{
            isCreatingGroup
              ? '创建中...'
              : `发起群聊${createGroupSelectedFriends.length > 0 ? '（已选 ' + createGroupSelectedFriends.length + ' 人）' : ''}`
          }}
        </view>
      </view>
    </view>

    <!-- 添加成员：全屏选人弹窗（Native）-->
    <view v-if="showAddMemberModal" class="mobile-create-group-page">
      <view class="mobile-chat-header">
        <text class="mobile-back-btn" @click="showAddMemberModal = false"
          >取消</text
        >
        <text class="mobile-chat-title">{{
          currentMobileChat?.sessionType === SESSION_TYPE.GROUP
            ? '邀请入群'
            : '发起群聊'
        }}</text>
        <text
          class="mobile-confirm-btn"
          :class="{
            'mobile-confirm-active': addMemberSelectedFriends.length > 0,
          }"
          @click="confirmAddMember"
          >完成({{ addMemberSelectedFriends.length }})</text
        >
      </view>
      <scroll-view scroll-y class="mobile-cg-scroll">
        <view
          v-for="friend in addMemberCandidates"
          :key="friend.userId"
          class="mobile-cg-item"
          @click="toggleAddMemberSelect(friend.userId)"
        >
          <view
            class="mobile-cg-checkbox"
            :class="{
              'cg-checked': addMemberSelectedFriends.includes(friend.userId),
            }"
          >
            <text
              v-if="addMemberSelectedFriends.includes(friend.userId)"
              class="cg-check-mark"
              >✓</text
            >
          </view>
          <image
            :src="friend.avatar || defaultAvatar"
            class="mobile-cg-avatar"
            mode="aspectFill"
          />
          <text class="mobile-cg-name">「{{ friend.nickname }}」</text>
        </view>
        <view v-if="addMemberCandidates.length === 0" class="mobile-cg-empty">
          <text>暂无可添加的好友</text>
        </view>
      </scroll-view>
      <view class="mobile-cg-footer">
        <view
          class="mobile-cg-submit-btn"
          :class="{
            'cg-submit-disabled':
              addMemberSelectedFriends.length === 0 || isAddingMember,
          }"
          @click="confirmAddMember"
        >
          {{
            isAddingMember
              ? '添加中...'
              : (currentMobileChat?.sessionType === SESSION_TYPE.GROUP
                  ? '邀请加入群聊'
                  : '发起群聊') +
                (addMemberSelectedFriends.length > 0
                  ? `（已选 ${addMemberSelectedFriends.length} 人）`
                  : '')
          }}
        </view>
      </view>
    </view>

    <!-- 添加好友：全屏搜索页（Native）-->
    <view v-if="showMobileAddFriend" class="mobile-add-friend-page">
      <view class="mobile-chat-header">
        <text class="mobile-back-btn" @click="showMobileAddFriend = false"
          >← 返回</text
        >
        <text class="mobile-chat-title">添加好友</text>
        <view style="width: 60px"></view>
      </view>
      <view class="mobile-af-search-bar">
        <input
          v-model="mobileAddFriendKeyword"
          type="text"
          placeholder="搜索用户名 / 手机号"
          class="mobile-af-search-input"
          @confirm="searchAddFriend"
        />
        <view class="mobile-af-search-btn" @click="searchAddFriend">
          <text>{{ isSearchingFriend ? '搜索中...' : '搜索' }}</text>
        </view>
      </view>
      <view v-if="isSearchingFriend" class="mobile-af-loading">
        <text>正在搜索...</text>
      </view>
      <view v-else-if="addFriendResult" class="mobile-af-result">
        <view class="mobile-af-user-card">
          <image
            :src="addFriendResult.avatar || defaultAvatar"
            class="mobile-af-avatar"
            mode="aspectFill"
          />
          <view class="mobile-af-user-info">
            <text class="mobile-af-nickname">{{
              addFriendResult.nickname
            }}</text>
            <text class="mobile-af-username">{{
              addFriendResult.username
            }}</text>
          </view>
          <view
            class="mobile-af-apply-btn"
            @click="sendFriendApply(addFriendResult.id)"
          >
            <text>{{ isSendingApply ? '发送中...' : '添加好友' }}</text>
          </view>
        </view>
      </view>
      <view
        v-else-if="mobileAddFriendKeyword && !isSearchingFriend"
        class="mobile-af-empty"
      >
        <text>未找到用户，换个关键词试试</text>
      </view>
    </view>

    <!-- 消息长按上下文菜单（Native）-->
    <view
      v-if="mobileContextMenu.visible"
      class="mobile-ctx-overlay"
      @click="closeMsgContextMenu"
    >
      <view class="mobile-ctx-menu" @click.stop>
        <view
          v-if="mobileContextMenu.msg?.message_type === MESSAGE_TYPE.TEXT"
          class="mobile-ctx-item"
          @click="copyMobileMsg"
          >复制</view
        >
        <view
          v-if="isMyMessage(mobileContextMenu.msg)"
          class="mobile-ctx-item mobile-ctx-revoke"
          @click="revokeMessage(mobileContextMenu.msg)"
          >撤回</view
        >
        <view
          class="mobile-ctx-item mobile-ctx-cancel"
          @click="closeMsgContextMenu"
          >取消</view
        >
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
import QrCodeModal from '@/components/home/QrCodeModal.vue'
import AddFriendModal from '@/components/home/AddFriendModal.vue'
import JoinGroupModal from '@/components/home/JoinGroupModal.vue'
import SettingsPanel from '@/components/home/SettingsPanel.vue'
import ChatArea from '@/components/home/ChatArea.vue'
import VoiceCall from '@/components/home/VoiceCall.vue'
import CloudDrive from '@/components/home/CloudDrive.vue'
import { useNotifications } from '@/composables/useNotifications'
import { useContacts } from '@/composables/useContacts'
import { useSendMessage } from '@/composables/useSendMessage'
import {
  handleMsgDownload,
  getUserInfoFromStorage,
  saveUserInfoToStorage,
} from '@/utils/download-util'
import { wsClient } from '@/utils/websocket'

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

// 枚举
const MESSAGE_TYPE = {
  TEXT: 1,
  IMAGE: 2,
  VIDEO: 3,
  AUDIO: 4,
  FILE: 5,
  EMOJI: 6,
  SYSTEM: 7,
}
const SESSION_TYPE = { SINGLE: 1, GROUP: 2 }
const SEND_STATUS = { PENDING: 'pending', SUCCESS: 'success', FAILED: 'failed' }
const CHUNK_SIZE = 10 * 1024 * 1024

// H5 响应式布局：通过 UA 和窗口宽度动态判断是否为移动端
// #ifdef H5
const isMobileView = ref(false)

const detectDevice = () => {
  if (typeof window !== 'undefined') {
    const ua = navigator.userAgent.toLowerCase()
    const isMobileUA =
      /mobile|tablet|ios|android|iphone|ipad|windows phone/.test(ua)
    const isMobileWidth = window.innerWidth < 768
    isMobileView.value = isMobileUA || isMobileWidth
  }
}

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

// 用户信息：启动时先尝试读取本地缓存，登录后由 loadCurrentUser 刷新
const userInfo = ref({
  id: '',
  nickname: '我的昵称',
  avatar: selfAvatar,
  wechatId: '',
  region: '',
  signature: '',
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

// 消息列表、滚动定位状态由 home.vue 持有；输入区相关状态由 useSendMessage 提供
const messages = ref([])
const showScrollToBottom = ref(false)
const scrollToView = ref('')
// 聊天右上角菜单状态
const showChatMenu = ref(false)
// 黑名单（本地模拟）
const blacklist = ref([])
// 聊天信息面板与配置
const showChatInfoPanel = ref(false)
const doNotDisturb = ref(false)
const pinned = ref(false)
const showProfileModal = ref(false)
// 群成员列表
const groupMembers = ref([])
const loadingGroupMembers = ref(false)
// 头像右侧小弹出层
const showProfilePopover = ref(false)
const showMoreMenu = ref(false)
const showPlusMenu = ref(false)
const popoverSide = ref('right')
const popoverStyle = ref({})
// 添加好友 / 加入群聊弹窗：展开关由父组件控制，弹窗内部状态由子组件自管理
const showAddFriendModal = ref(false)
const showJoinGroupModal = ref(false)
// 通讯录内「添加朋友」悬浮窗（带搜索框）
const showAddFriendPopover = ref(false)
const addFriendKeyword = ref('')
const addFriendSearchResult = ref(undefined) // undefined=未搜索, null=无结果, object=找到
const addFriendSearched = ref(false)
const addFriendPopoverLoading = ref(false)
const addFriendApplying = ref(false)
const addFriendRemark = ref('')
const showAddFriendRemark = ref(false)
// 我的二维码弹窗显隐
const showQrCode = ref(false)
// 语音/视频通话弹窗显隐
const showVoiceCall = ref(false)
const voiceCallPeerInfo = ref({}) // 对方用户信息
const voiceCallDirection = ref('outgoing') // 'outgoing' - 呼出，'incoming' - 呼入
const voiceCallSessionId = ref('') // 通话 sessionId
const voiceCallMode = ref('audio') // 'audio' 或 'video'
// 网盘界面
const showCloudDrive = ref(false)
// 通知状态：由 useNotifications composable 提供，包含好友申请 + 群聊申请
// onApproved 回调：同意申请成功后立刻刷新会话列表
const {
  notifications,
  notifyLoading,
  pendingNotifyCount,
  loadNotifications,
  handleFriendApply,
  handleGroupApply,
} = useNotifications({
  onApproved: () => loadSessionList(),
})

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

// 好友和群聊列表、搜索、切换 Tab 由 useContacts composable 提供，在 switchSession 定义后初始化

// 移动端状态
const currentMobileChat = ref(null)
const mobileCurrentTab = ref('chat') // 'chat' | 'contact' | 'profile'
// 移动端聊天列表顶部 ➕ 下拉菜单状态
const showMobilePlusMenu = ref(false)
const showMobileSearch = ref(false)
const mobileSearchText = ref('')
const showEmojiMobile = ref(false)
// 移动端输入栏工具格（图片/文件）展开状态
const showMobileTools = ref(false)

// msgIdCounter 由 cleanMessage/generateUniqueId 使用，需保留在 home.vue 范围内
let msgIdCounter = 0

// 计算属性
// 会话列表所有未读数之和，用于侧边栏切换按鈕上的徽章数字
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
    content_replaced: rawMsg.contentReplaced || rawMsg.content_replaced || '',
    status: Number(rawMsg.status || 0),
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
      : // 优先使用后端返回的真实发送人头像（senderAvatar）
        // 如果后端没有带（如旧消息缓存），单聊时用会话头像兜底
        rawMsg.senderAvatar ||
        rawMsg.sender_avatar ||
        (currentSession.value?.sessionType === SESSION_TYPE.SINGLE
          ? currentSession.value?.sessionAvatar || selfAvatar
          : selfAvatar),
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

/**
 * 获取当前群聊的成员列表
 * 用于群聊聊天信息面板显示群成员
 */
const loadGroupMembers = async (groupId) => {
  if (!groupId || loadingGroupMembers.value) return
  
  loadingGroupMembers.value = true
  try {
    const res = await service.get(`/group/member/list/${groupId}`)
    if (res.code === 200 && Array.isArray(res.data)) {
      groupMembers.value = res.data
    } else {
      groupMembers.value = []
    }
  } catch (e) {
    console.error('获取群成员列表失败', e)
    groupMembers.value = []
  } finally {
    loadingGroupMembers.value = false
  }
}

/**
 * 获取当前用户在当前群聊中的角色
 * @returns {number} 角色：1-群主，2-管理员，3-普通成员
 */
const getCurrentUserRole = () => {
  if (!currentSession.value || currentSession.value.sessionType !== SESSION_TYPE.GROUP) {
    return 3 // 非群聊默认普通成员
  }
  
  const currentUserMember = groupMembers.value.find(m => m.userId === CURRENT_USER_ID.value)
  return currentUserMember ? currentUserMember.role : 3
}

/**
 * 判断移动端当前用户是否有权限移除指定成员
 * @param {Object} member - 要检查的成员对象
 * @returns {boolean} 是否有权限移除
 */
const canRemoveMemberInMobile = (member) => {
  if (!currentMobileChat.value || currentMobileChat.value.sessionType !== SESSION_TYPE.GROUP) {
    return false
  }
  
  const currentUserMember = groupMembers.value.find(m => m.userId === CURRENT_USER_ID.value)
  const currentUserRole = currentUserMember ? currentUserMember.role : 3
  
  // 普通成员无权限
  if (currentUserRole === 3) return false
  
  // 管理员不能移除群主和其他管理员
  if (currentUserRole === 2 && member.role <= 2) return false
  
  // 不能移除自己
  if (member.userId === CURRENT_USER_ID.value) return false
  
  return true
}

/**
 * 移除群成员
 * @param {Object} member - 要移除的成员对象
 * @param {string} groupId - 群ID
 */
const removeGroupMember = async (member, groupId) => {
  if (!member || !groupId) return
  
  // 获取当前用户在群中的角色
  const currentUserMember = groupMembers.value.find(m => m.userId === CURRENT_USER_ID.value)
  const currentUserRole = currentUserMember ? currentUserMember.role : 3 // 默认普通成员
  
  // 权限检查
  if (currentUserRole === 3) {
    uni.showToast({ title: '普通成员无权限移除他人', icon: 'none' })
    return
  }
  
  if (currentUserRole === 2 && member.role <= 2) {
    uni.showToast({ title: '管理员无法移除群主或其他管理员', icon: 'none' })
    return
  }
  
  if (member.userId === CURRENT_USER_ID.value) {
    uni.showToast({ title: '不能移除自己，请使用退出群聊功能', icon: 'none' })
    return
  }
  
  // 确认对话框
  uni.showModal({
    title: '确认移除',
    content: `确定要将 ${member.nickname} 移出群聊吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          const removeRes = await service.post('/group/member/remove', {
            groupId: Number(groupId),
            userId: member.userId,
            reason: '管理员移除'
          })
          
          if (removeRes.code === 200) {
            // 从本地列表中移除
            groupMembers.value = groupMembers.value.filter(m => m.userId !== member.userId)
            uni.showToast({ title: '移除成功', icon: 'success' })
            
            // 发送系统消息通知（可选）
            // 这里可以调用发送系统消息的函数
          } else {
            uni.showToast({ title: removeRes.msg || '移除失败', icon: 'none' })
          }
        } catch (error) {
          console.error('移除成员失败', error)
          uni.showToast({ title: '移除失败', icon: 'none' })
        }
      }
    }
  })
}

/**
 * 获取当前会话的成员信息（单聊好友或群成员）
 * 用于聊天信息面板显示
 */
const getSessionMembers = () => {
  if (!currentSession.value) return []
  
  if (currentSession.value.sessionType === SESSION_TYPE.SINGLE) {
    // 单聊：返回对方好友信息
    const friend = getFriendForCurrentSession()
    return friend ? [friend] : []
  } else if (currentSession.value.sessionType === SESSION_TYPE.GROUP) {
    // 群聊：返回群成员列表
    return groupMembers.value
  }
  return []
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
const toggleChatInfoPanel = async () => {
  showChatInfoPanel.value = !showChatInfoPanel.value
  
  // 如果是群聊且面板打开，加载群成员列表
  if (showChatInfoPanel.value && currentSession.value?.sessionType === SESSION_TYPE.GROUP) {
    await loadGroupMembers(currentSession.value.targetId)
  }
  
  // 关闭个人菜单，避免冲突
  showChatMenu.value = false
}

/**
 * 进入移动端聊天信息页
 */
const enterMobileChatInfo = async () => {
  mobileChatInfoPage.value = true
  
  // 如果是群聊，加载群成员列表
  if (currentMobileChat.value?.sessionType === SESSION_TYPE.GROUP) {
    await loadGroupMembers(currentMobileChat.value.targetId)
  }
}

/**
 * 从移动端打开用户资料
 */
const openProfileFromMobile = (member) => {
  // 复用 PC 端的逻辑
  currentContact.value = {
    sessionName: member.nickname,
    avatar: member.avatar,
    type: 'person',
    targetId: member.userId,
    desc: member.roleName || '群成员'
  }
  showProfileModal.value = true
  mobileChatInfoPage.value = false
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

/** 移动端聊天信息页「清空聊天记录」：执行清空并关闭信息页，避免模板内多行 @click 导致编译解析错误 */
const onClearChatRecordsMobile = async () => {
  await clearChatRecords()
  mobileChatInfoPage.value = false
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

/**
 * 自动下载功能已封却，空函数保d为占位备用。
 */
const autoDownloadFiles = (_msgList) => {}

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
  showAddFriendPopover.value = false
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

/**
 * 开启添加朋友弹窗。
 * 弹窗内部的搜索/申请状态由 AddFriendModal 组件完全自管理，父组件只需控制显隐。
 */
const openAddFriend = () => {
  showPlusMenu.value = false
  showAddFriendModal.value = true
}

/** 通讯录内「添加朋友」行点击：打开带搜索的悬浮窗 */
const openAddFriendPopover = () => {
  showAddFriendPopover.value = true
}

/** 关闭添加朋友悬浮窗并重置状态 */
const closeAddFriendPopover = () => {
  showAddFriendPopover.value = false
  addFriendKeyword.value = ''
  addFriendSearchResult.value = undefined
  addFriendSearched.value = false
  addFriendRemark.value = ''
  showAddFriendRemark.value = false
}

/** 悬浮窗内执行用户搜索（与 AddFriendModal 同一接口） */
const doAddFriendSearch = async () => {
  const q = addFriendKeyword.value.trim()
  if (!q || addFriendPopoverLoading.value) return
  addFriendPopoverLoading.value = true
  addFriendSearched.value = true
  addFriendSearchResult.value = undefined
  try {
    const res = await service.get('/user/search', { params: { keyword: q } })
    addFriendSearchResult.value = res.code === 200 && res.data ? res.data : null
  } catch {
    addFriendSearchResult.value = null
  } finally {
    addFriendPopoverLoading.value = false
  }
}

const clearAddFriendSearch = () => {
  addFriendKeyword.value = ''
  addFriendSearchResult.value = undefined
  addFriendSearched.value = false
  addFriendRemark.value = ''
  showAddFriendRemark.value = false
}

/** 悬浮窗内发送好友申请 */
const sendAddFriendApply = async () => {
  if (!addFriendSearchResult.value || addFriendApplying.value) return
  addFriendApplying.value = true
  try {
    const res = await service.post('/friend/apply', {
      targetId: addFriendSearchResult.value.id,
      remark: addFriendRemark.value.trim() || null,
    })
    if (res.code === 200) {
      addFriendSearchResult.value = {
        ...addFriendSearchResult.value,
        isApplied: true,
      }
      showAddFriendRemark.value = false
      loadNotifications()
    } else {
      uni.showToast({ title: res.msg || '申请失败', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
  } finally {
    addFriendApplying.value = false
  }
}

const openCreateGroup = () => {
  showPlusMenu.value = false
  uni.showToast({ title: '发起群聊功能开发中', icon: 'none' })
}

// 切换通讯录 tab—— useContacts 内部已处理各 tab 的数据刷新逗辑
const openJoinGroup = () => {
  showPlusMenu.value = false
  showJoinGroupModal.value = true
}

const openChatFiles = () => uni.showToast({ title: '功能开发中', icon: 'none' })
const manageChatHistory = () =>
  uni.showToast({ title: '功能开发中', icon: 'none' })
const loadHistoryChat = () =>
  uni.showToast({ title: '功能开发中', icon: 'none' })
const lockApp = () => uni.showToast({ title: '功能开发中', icon: 'none' })
const openFeedback = () => uni.showToast({ title: '功能开发中', icon: 'none' })
const openSettings = () => uni.showToast({ title: '功能开发中', icon: 'none' })

// 退出登录：尝试调用后端接口并清除本地 token
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
  // 切换标签页时关闭网盘面板
  showCloudDrive.value = false
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

/**
 * 展示好友详情（右侧面板）—— 仅更新当前联系人，不自行进入聊天
 */
const selectFriend = (friend) => {
  currentContact.value = {
    sessionName: friend.nickname,
    avatar: friend.avatar,
    type: 'person',
    targetId: friend.userId,
    desc: friend.signature,
  }
}

/** 展示群聊详情（右侧面板） */
const selectGroup = (group) => {
  currentContact.value = {
    sessionName: group.groupName,
    avatar: group.groupAvatar,
    type: 'group',
    targetId: group.groupId,
    desc: `${group.memberCount} 个成员`,
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

/**
 * useContacts: 好友 / 群聊列表、搜索过滤、切换 Tab、进入聊天的完整封装。
 * switchSession 必须先于此调用，作为回调传入。
 */
const {
  friends,
  groups,
  selectedFriendTab,
  filteredFriends,
  filteredGroups,
  loadFriends,
  loadGroups,
  switchContactTab,
  chatWithFriend,
  chatWithGroup,
} = useContacts({
  sessions,
  currentSidebarTab,
  CURRENT_USER_ID,
  switchSession,
  contactSearchText,
  SESSION_TYPE,
  onSwitchNotify: loadNotifications,
})

/**
 * 通用「发消息」按鈕：根据当前联系人类型调用对应的 chatWith 方法
 */
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

/**
 * 更新会话列表中指定会话的最新消息，并按置顶优先、时间逆序重新排序。
 * 发送消息后即时调用，确保会话列表实时显示最新状态。
 */
const updateSessionLastMsg = (sessionId, content, sendTime) => {
  const idx = sessions.value.findIndex((s) => s.sessionId === sessionId)
  if (idx !== -1) {
    const updatedSession = {
      ...sessions.value[idx],
      lastMessageContent: content,
      lastMessageTime: sendTime,
      lastMessageSenderId: CURRENT_USER_ID.value,
    }
    sessions.value.splice(idx, 1, updatedSession)
    sessions.value = [...sessions.value].sort((a, b) => {
      if (a.isTop !== b.isTop) return b.isTop - a.isTop
      return new Date(b.lastMessageTime) - new Date(a.lastMessageTime)
    })
  }
}

/**
 * 加载指定会话的消息列表，符合「先本地后服务端」规范：
 *  1. 检测是否当前设备首次登录：首次无本地缓存，直接请求服务端；非首次先读本地并渲染，再拉取服务端离线消息
 *  2. 非首次：先读本地缓存并立即渲染（避免空白页），再从服务端拉取最新/离线消息作为权威源刷新视图并回写缓存
 *  3. 首次或服务端请求成功后标记本设备已初始化，下次进入视为非首次
 *  4. 网络失败时若已有本地数据则保留展示，不重复读本地
 *  5. 【新增】如果查询到 0 条消息，说明是新会话，清空之前会话残留的消息
 */
const loadMessages = async (sessionId) => {
  if (!sessionId) return
  const userId = String(CURRENT_USER_ID.value || '').trim()
  const isFirstTime = ChatStorage.isFirstTimeOnDevice(userId)

  try {
    // 切换会话时，先清空消息列表，避免显示上一个会话的消息
    messages.value = []
    
    if (!isFirstTime) {
      const localMsgs = await ChatStorage.queryMessages(sessionId)
      if (localMsgs.length > 0) {
        messages.value = localMsgs.map((item) => cleanMessage(item))
        await nextTick()
        scrollToBottom()
      }
    }

    const res = await service.get('/chat/message/list', {
      params: { sessionId },
    })
    if (res.code === 200 && Array.isArray(res.data)) {
      if (res.data.length > 0) {
        await ChatStorage.insertMessages(sessionId, res.data)
        messages.value = res.data.map((item) => cleanMessage(item))
      }
      if (userId) ChatStorage.setDeviceInitialized(userId)
    }
  } catch (e) {
    console.error('加载消息失败', e)
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
  // 手动点击下载，非自动触发（传 false）
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

const performContactSearch = () => {
  contactSearchQuery.value = contactSearchText.value.trim()
}
const performSessionSearch = () => {
  sidebarSearchQuery.value = sidebarSearchText.value.trim()
}

/**
 * 处理 WebSocket 实时消息（核心方法）
 * 接收后端 Netty 推送的新消息，并实时更新到聊天界面
 */
const handleWebSocketMessage = (data) => {
  try {
    // 1. 解析消息数据
    const message = typeof data === 'string' ? JSON.parse(data) : data
    
    // 2. 判断消息类型
    // 心跳包直接忽略
    if (message.type === 'ping') {
      return
    }
    
    // 语音通话信令消息处理（callType: 1-发起呼叫，2-接听，3-拒绝，4-挂断，5-SDP 交换，6-ICE 交换）
    if (message.callType && ['1', '2', '3', '4', '5', '6'].includes(message.callType)) {
      handleVoiceCallSignal(message)
      return
    }
    
    // 普通聊天消息处理
    if (!message.id) {
      return
    }

    console.log('收到新消息:', message)

    // 3. 获取消息的会话 ID
    const messageSessionId = message.sessionId
    
    // 4. 判断是否是当前正在查看的会话
    const isCurrentSession = currentSession.value && 
                            currentSession.value.sessionId === messageSessionId
    
    // 5. 判断是否是自己发送的消息
    const isSelfMessage = message.senderId === String(CURRENT_USER_ID.value)
    
    if (isSelfMessage) {
      // 自己发送的消息已经在本地显示，更新状态为已读/成功
      updateMessageStatus(message.id, SEND_STATUS.SUCCESS)
      // 但依然需要更新会话列表（更新最后一条消息和时间）
    } else {
      // 6. 如果是当前会话，实时更新消息列表（他人消息）
      if (isCurrentSession) {
        // 将消息添加到当前消息列表
        const newMessage = {
          ...message,
          senderName: message.senderName || '',
          senderAvatar: message.senderAvatar || '',
        }
        
        messages.value.push(newMessage)
        
        // 自动滚动到底部
        setTimeout(() => {
          scrollToBottom()
        }, 100)
      } else {
        // 7. 播放提示音或显示通知（可选）
        // 收到非当前会话的消息，显示通知
        showNewMessageNotification(message)
      }
    }

    // 8. 更新会话列表（置顶并更新最后一条消息）- 自己和对方的消息都要更新
    updateSessionWithNewMessage(messageSessionId, message)

  } catch (error) {
    console.error('处理 WebSocket 消息失败:', error)
  }
}

/**
 * 更新会话列表中的指定会话（收到新消息时调用）
 * 功能：更新最后一条消息、更新未读数、重新排序
 */
const updateSessionWithNewMessage = (sessionId, message) => {
  const idx = sessions.value.findIndex((s) => s.sessionId === sessionId)
  if (idx !== -1) {
    // 1. 判断是否是当前正在查看的会话
    const isCurrentSession = currentSession.value && 
                            currentSession.value.sessionId === sessionId
    
    // 2. 更新会话信息
    const updatedSession = {
      ...sessions.value[idx],
      lastMessageContent: message.content,
      lastMessageTime: message.sendTime,
      lastMessageSenderId: message.senderId,
      // 如果不是当前会话，增加未读数
      unreadCount: isCurrentSession 
        ? 0 
        : (sessions.value[idx].unreadCount || 0) + 1,
    }
    
    // 3. 更新会话列表中的该条目
    sessions.value.splice(idx, 1, updatedSession)
    
    // 4. 重新排序：置顶优先 + 时间倒序
    sessions.value = [...sessions.value].sort((a, b) => {
      if (a.isTop !== b.isTop) return b.isTop - a.isTop
      return new Date(b.lastMessageTime) - new Date(a.lastMessageTime)
    })
    
    console.log('会话列表已更新:', sessionId, '未读数:', updatedSession.unreadCount)
  } else {
    // 5. 如果是不存在的会话（可能是新会话），从服务端刷新会话列表
    console.log('新会话，刷新会话列表:', sessionId)
    loadSessionList()
  }
}

/**
 * 显示新消息通知（可选功能）
 */
const showNewMessageNotification = (message) => {
  // 这里可以集成 uni.showToast 或者浏览器 Notification API
  // 暂时只打印日志，后续可扩展
  console.log('新消息通知:', message.senderName || '有人', '发来一条消息')
}

/**
 * 处理语音通话信令消息
 */
const handleVoiceCallSignal = (message) => {
  console.log('📞 home.vue 收到语音通话信令:', message)
  
  const { callType, targetId, sessionId } = message
  
  // 1. 收到对方发起的语音呼叫（callType = 1）- 只有呼入才需要处理
  if (callType === '1') {
    // signal may include caller info injected by server: fromId/fromNickname/fromAvatar
    const callerId = message.fromId || message.from || message.callerId || targetId
    // 检查当前是否已经有语音通话组件在显示
    if (!showVoiceCall.value) {
      handleIncomingVoiceCall(callerId, sessionId, message)
    } else {
      console.log('⚠️ 语音通话组件已在显示，忽略此次呼叫')
    }
    return
  }
  
  // 2. 其他信令（接听、拒绝、挂断、SDP、ICE）由 VoiceCall 组件自己处理
  // home.vue 不干预，避免重复处理
  console.log('ℹ️ 语音通话信令将交由 VoiceCall 组件处理，类型:', callType)
}

/**
 * 处理收到的语音呼叫（被叫方）
 */
const handleIncomingVoiceCall = async (targetUserId, sessionId, rawMessage = {}) => {
  try {
    console.log('📞 收到语音呼叫，主叫用户:', targetUserId, '会话 ID:', sessionId, 'raw:', rawMessage)
    
    // 1. 获取主叫方（对方）用户信息 - 从好友列表或会话列表中查找
    let callerUserInfo = null

    // 如果信令里直接带了 from 信息，优先使用
    if (rawMessage && (rawMessage.fromId || rawMessage.fromNickname || rawMessage.fromAvatar)) {
      callerUserInfo = {
        id: rawMessage.fromId || targetUserId,
        nickname: rawMessage.fromNickname || '未知用户',
        avatar: rawMessage.fromAvatar || defaultAvatar,
        wechatId: '',
        signature: ''
      }
      console.log('✅ 使用信令中的主叫信息:', callerUserInfo)
    }
    
    // 1.1 尝试从好友列表中查找（最可靠的数据源）
    const friend = friends.value.find(f => f.userId === targetUserId || (callerUserInfo && f.userId === callerUserInfo.id))
    if (friend) {
      callerUserInfo = callerUserInfo || {
        id: friend.userId,
        nickname: friend.nickname || '未知用户',
        avatar: friend.avatar || defaultAvatar,
        wechatId: friend.wechatId || '',
        signature: friend.signature || ''
      }
      console.log('✅ 从好友列表找到主叫方信息:', callerUserInfo)
    }
    
    // 1.2 如果好友列表没有，再尝试从当前会话中获取（兼容旧数据）
    if (!callerUserInfo) {
      const currentSessionTarget = sessions.value.find(s => 
        s.sessionType === SESSION_TYPE.SINGLE && (s.targetId === targetUserId || (rawMessage && s.targetId === rawMessage.fromId))
      )
      if (currentSessionTarget) {
        callerUserInfo = {
          id: currentSessionTarget.targetId,
          nickname: currentSessionTarget.sessionName || '未知用户',
          avatar: currentSessionTarget.sessionAvatar || defaultAvatar,
          wechatId: '',
          signature: ''
        }
        console.log('✅ 从会话列表找到主叫方信息:', callerUserInfo)
      }
    }
    
    // 1.3 如果都没有，使用默认信息
    if (!callerUserInfo) {
      callerUserInfo = {
        id: targetUserId,
        nickname: '未知用户',
        avatar: defaultAvatar,
        wechatId: '',
        signature: ''
      }
      console.warn('⚠️ 未找到主叫方信息，使用默认值:', callerUserInfo)
    }
    
    // 2. 查找或创建对应的会话（sessionId 格式：single -> 1_{userId}）
    const expectedSessionId = `1_${callerUserInfo.id}`
    let targetSession = sessions.value.find(s => s.sessionId === expectedSessionId)
    
    if (!targetSession) {
      // 如果不存在，创建临时会话信息
      targetSession = {
        sessionId: expectedSessionId,
        sessionType: SESSION_TYPE.SINGLE,
        targetId: callerUserInfo.id,
        sessionName: callerUserInfo.nickname,
        sessionAvatar: callerUserInfo.avatar,
        lastMessageContent: '[语音通话]',
        lastMessageTime: Date.now(),
        unreadCount: 0
      }
      console.log('🆕 创建新会话:', targetSession)
    }
    
    // 3. 切换到该会话的聊天界面
    // PC 端：设置 currentSession
    currentSession.value = targetSession
    
    // 移动端：设置 currentMobileChat 和 mobileCurrentTab
    if (isMobileView.value) {
      currentMobileChat.value = targetSession
      mobileCurrentTab.value = 'chat'
    }
    
    // 4. 加载该会话的消息历史
    await switchSession(targetSession)
    
    // 5. 显示语音通话组件（呼入模式）- 确保传递完整的用户信息和 sessionId
    voiceCallPeerInfo.value = { ...callerUserInfo }
    voiceCallDirection.value = 'incoming'
    voiceCallSessionId.value = sessionId // 使用后端生成的 sessionId
    voiceCallMode.value = rawMessage.mode === 'video' ? 'video' : 'audio' // 根据信令选择
    showVoiceCall.value = true
    
    console.log('✅ 已切换到语音呼叫会话，显示语音通话界面')
    console.log('👤 主叫方用户信息:', voiceCallPeerInfo.value)
    console.log('🔖 使用的 sessionId:', voiceCallSessionId.value)
    
  } catch (error) {
    console.error('❌ 处理语音呼叫失败:', error)
    uni.showToast({
      title: '语音呼叫处理失败',
      icon: 'none'
    })
  }
}

/**
 * 更新消息状态（用于确认自己发送的消息已成功送达）
 */
const updateMessageStatus = (messageId, status) => {
  const msg = messages.value.find((m) => m.id === messageId)
  if (msg) {
    msg.status = status
  }
}

/**
 * useSendMessage: 输入、发送、文件、语音、拖拽所有逻辑的完整封装。
 * 依赖 currentSession / CURRENT_USER_ID / messages / cleanMessage 等父级状态，
 * 展开后的全部输入区状态和操作函数由 ChatArea 组件通过 props/events 使用。
 */
const {
  inputMsg,
  pendingFiles,
  isSending,
  isRecording,
  recordingDuration,
  isInputDragOver,
  showEmojiPanel,
  recentEmojis,
  allEmojis,
  sendMessageWithFiles,
  handleEnterKey,
  handleCtrlEnter,
  toggleEmojiPanel,
  insertEmoji,
  chooseImage,
  chooseFile,
  removePendingFile,
  onInputDragEnter,
  onInputDragOver,
  onInputDragLeave,
  onInputDrop,
  toggleVoiceRecording,
  stopAndSendVoice,
  cancelVoice,
  cleanup: cleanupSendMessage,
} = useSendMessage({
  currentSession,
  CURRENT_USER_ID,
  messages,
  cleanMessage,
  scrollToBottom,
  updateSessionLastMsg,
  SESSION_TYPE,
  MESSAGE_TYPE,
  SEND_STATUS,
  CHUNK_SIZE,
})

/** 是否有内容可发送（输入文字或待发文件），用于移动端发送/展开按钮切换，避免模板内复杂表达式解析问题 */
const hasContentToSend = computed(() => {
  const text = (inputMsg.value || '').trim()
  const files = pendingFiles.value || []
  return text.length > 0 || files.length > 0
})

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

// 移动端输入框获焦时收起工具/表情面板，避免模板内多行表达式导致编译器解析错误
const onMobileInputFocus = () => {
  showMobileTools.value = false
  showEmojiMobile.value = false
}

// 移动端点击「图片」：选图并收起工具面板
const onChooseMobileImage = () => {
  chooseMobileImage()
  showMobileTools.value = false
}

// H5/Mobile 底部 tab 切换统一方法
const setMobileTab = (tab) => {
  showMobilePlusMenu.value = false
  mobileCurrentTab.value = tab
  if (tab === 'chat') {
    currentMobileChat.value = null
  }
  if (tab === 'cloud') {
    // 进入网盘时确保网盘面板显示
    showCloudDrive.value = true
  } else {
    // 离开网盘时关闭网盘面板
    showCloudDrive.value = false
  }
}

// 移动端聊天信息页（点击···进入全屏界面）
const mobileChatInfoPage = ref(false)

// ========== 移动端添加好友 ==========
const showMobileAddFriend = ref(false)
const mobileAddFriendKeyword = ref('')
const addFriendResult = ref(null)
const isSearchingFriend = ref(false)
const isSendingApply = ref(false)

/** 打开添加好友全屏页 */
const openMobileAddFriend = () => {
  showMobilePlusMenu.value = false
  mobileAddFriendKeyword.value = ''
  addFriendResult.value = null
  showMobileAddFriend.value = true
}

/** 搜索用户 */
const searchAddFriend = async () => {
  const keyword = mobileAddFriendKeyword.value.trim()
  if (!keyword) {
    uni.showToast({ title: '请输入搜索关键词', icon: 'none' })
    return
  }
  isSearchingFriend.value = true
  addFriendResult.value = null
  try {
    const res = await service.get('/user/search', { params: { keyword } })
    if (res.code === 200) {
      addFriendResult.value = res.data
    } else {
      uni.showToast({ title: res.message || '未找到用户', icon: 'none' })
    }
  } catch (e) {
    uni.showToast({ title: '网络异常，请重试', icon: 'none' })
  } finally {
    isSearchingFriend.value = false
  }
}

/** 发送好友申请 */
const sendFriendApply = async (targetId) => {
  if (isSendingApply.value) return
  isSendingApply.value = true
  try {
    const res = await service.post('/friend/apply', { targetId })
    if (res.code === 200) {
      uni.showToast({ title: '好友申请已发送', icon: 'success' })
      showMobileAddFriend.value = false
    } else {
      uni.showToast({ title: res.message || '发送申请失败', icon: 'none' })
    }
  } catch (e) {
    uni.showToast({ title: '网络异常，请重试', icon: 'none' })
  } finally {
    isSendingApply.value = false
  }
}

const openMobileJoinGroup = () => {
  showMobilePlusMenu.value = false
  openJoinGroup()
}

// ========== 发起群聊 ==========
const showMobileCreateGroup = ref(false)
const createGroupName = ref('')
const createGroupSelectedFriends = ref([])
const isCreatingGroup = ref(false)
// 群头像上传
const createGroupAvatarPath = ref('') // 短路径（提交用）
const createGroupAvatarUrl = ref('') // 预览 URL（展示用）
const isUploadingGroupAvatar = ref(false)

/** 打开发起群聊弹窗：重置所有状态展示 */
const openMobileCreateGroup = () => {
  showMobilePlusMenu.value = false
  createGroupName.value = ''
  createGroupSelectedFriends.value = []
  createGroupAvatarPath.value = ''
  createGroupAvatarUrl.value = ''
  showMobileCreateGroup.value = true
}

/** 选择并上传群头像 */
const chooseGroupAvatar = () => {
  // #ifdef H5
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    isUploadingGroupAvatar.value = true
    try {
      const fd = new FormData()
      fd.append('file', file)
      const res = await service.post('/group/avatar/upload', fd)
      if (res.code === 200) {
        createGroupAvatarPath.value = res.data.filePath
        createGroupAvatarUrl.value = res.data.previewUrl
      } else {
        uni.showToast({ title: res.message || '上传失败', icon: 'none' })
      }
    } catch (err) {
      uni.showToast({ title: '上传失败，请重试', icon: 'none' })
    } finally {
      isUploadingGroupAvatar.value = false
    }
  }
  input.click()
  // #endif
  // #ifndef H5
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (chooseRes) => {
      const tempFilePath = chooseRes.tempFilePaths[0]
      createGroupAvatarUrl.value = tempFilePath // 先本地预览
      isUploadingGroupAvatar.value = true
      // 获取后端地址和认证 token
      const baseUrl = service.defaults?.baseURL || 'http://127.0.0.1:5050/api'
      const token = uni.getStorageSync('satoken') || ''
      uni.uploadFile({
        url: baseUrl + '/group/avatar/upload',
        filePath: tempFilePath,
        name: 'file',
        header: token ? { satoken: token } : {},
        success: (uploadRes) => {
          try {
            const data = JSON.parse(uploadRes.data)
            if (data.code === 200) {
              createGroupAvatarPath.value = data.data.filePath
              createGroupAvatarUrl.value = data.data.previewUrl
            } else {
              uni.showToast({ title: data.message || '上传失败', icon: 'none' })
              createGroupAvatarUrl.value = ''
            }
          } catch {
            uni.showToast({ title: '上传失败，请重试', icon: 'none' })
            createGroupAvatarUrl.value = ''
          }
        },
        fail: () => {
          uni.showToast({ title: '上传失败，请重试', icon: 'none' })
          createGroupAvatarUrl.value = ''
        },
        complete: () => {
          isUploadingGroupAvatar.value = false
        },
      })
    },
  })
  // #endif
}

/** 切换好友选中状态 */
const toggleFriendSelect = (userId) => {
  const idx = createGroupSelectedFriends.value.indexOf(userId)
  if (idx === -1) {
    createGroupSelectedFriends.value.push(userId)
  } else {
    createGroupSelectedFriends.value.splice(idx, 1)
  }
}

/**
 * 提交创建群聊：
 *  - 群名为空时自动取前三位好友昵称
 *  - 创建成功后刷新会话列表 + 群聊列表
 */
const submitCreateGroup = async () => {
  if (createGroupSelectedFriends.value.length === 0) {
    uni.showToast({ title: '请至少选择 1 位好友', icon: 'none' })
    return
  }
  if (isCreatingGroup.value) return
  isCreatingGroup.value = true
  try {
    // 自动生成群名：将所有成员昵称按拼音/字典序排序，取前 3 位
    const memberNames = createGroupSelectedFriends.value
      .map((id) => {
        const friend = friends.value.find((f) => f.userId === id)
        return friend?.nickname || ''
      })
      .filter(Boolean)
      .sort((a, b) => a.localeCompare(b, 'zh-Hans-CN', { sensitivity: 'accent' }))
    
    const autoName =
      memberNames
        .slice(0, 3)
        .join('、') + '的群聊'
    
    const res = await service.post('/group/create', {
      groupName: createGroupName.value.trim() || autoName,
      groupAvatar: createGroupAvatarPath.value || '',
      joinType: 2, // 免审核
      memberIds: createGroupSelectedFriends.value,
    })
    if (res.code === 200) {
      uni.showToast({ title: '群聊创建成功', icon: 'success' })
      showMobileCreateGroup.value = false
      createGroupName.value = ''
      createGroupSelectedFriends.value = []
      // 刷新会话列表和群聊列表
      await loadSessionList()
      loadGroups()
    } else {
      uni.showToast({ title: res.message || '创建群聊失败', icon: 'none' })
    }
  } catch (e) {
    console.error('创建群聊异常', e)
    uni.showToast({ title: '网络异常，请重试', icon: 'none' })
  } finally {
    isCreatingGroup.value = false
  }
}

// ========== 聊天信息页“添加成员”==========
const showAddMemberModal = ref(false)
const addMemberSelectedFriends = ref([])
const isAddingMember = ref(false)

/**
 * 可选成员列表：
 *  - 单聊：全部好友（最终和对方一起发起新群聊）
 *  - 群聊：好友列表（后端 addMembers 会自动去重已在群内的成员）
 */
const addMemberCandidates = computed(() => {
  return friends.value || []
})

/** PC 端添加成员面板内搜索关键词 */
const addMemberSearchKeyword = ref('')

/** 根据搜索关键词过滤的可选成员（PC 端列表） */
const filteredAddMemberCandidates = computed(() => {
  const list = addMemberCandidates.value
  const kw = (addMemberSearchKeyword.value || '').trim().toLowerCase()
  if (!kw) return list
  return list.filter(
    (f) =>
      (f.nickname || '').toLowerCase().includes(kw) ||
      (f.signature || '').toLowerCase().includes(kw)
  )
})

/** 打开“添加成员”弹窗，重置选中状态与搜索 */
const openAddMemberModal = () => {
  addMemberSelectedFriends.value = []
  addMemberSearchKeyword.value = ''
  showAddMemberModal.value = true
}

/** 切换成员选中状态 */
const toggleAddMemberSelect = (userId) => {
  const idx = addMemberSelectedFriends.value.indexOf(userId)
  if (idx === -1) {
    addMemberSelectedFriends.value.push(userId)
  } else {
    addMemberSelectedFriends.value.splice(idx, 1)
  }
}

/**
 * 确认添加成员：
 *  - 群聊会话：调用 /group/member/add 接口邀请好友入群
 *  - 单聊会话：将对方 + 选中好友合并，发起新群聊
 */
const confirmAddMember = async () => {
  if (addMemberSelectedFriends.value.length === 0) {
    uni.showToast({ title: '请至少选择 1 位好友', icon: 'none' })
    return
  }
  if (isAddingMember.value) return
  isAddingMember.value = true
  try {
    // 使用当前会话（PC 或 Mobile）
    const currentSessionForAdd = currentSession.value || currentMobileChat.value
    const isGroupChat = currentSessionForAdd?.sessionType === SESSION_TYPE.GROUP
    if (isGroupChat) {
      // 群聊：邀请选中好友加入当前群
      const res = await service.post('/group/member/add', {
        groupId: currentSessionForAdd.targetId,
        userIds: addMemberSelectedFriends.value,
        addType: 2, // 邀请加入
      })
      if (res.code === 200) {
        uni.showToast({ title: '邀请发送成功', icon: 'success' })
        showAddMemberModal.value = false
        addMemberSelectedFriends.value = []
      } else {
        uni.showToast({ title: res.message || '邀请失败', icon: 'none' })
      }
    } else {
      // 单聊：将对方 + 选中好友一起发起新群聊
      const allMembers = [
        currentSessionForAdd.targetId,
        ...addMemberSelectedFriends.value,
      ]
      
      // 获取所有成员昵称并按拼音/字典序排序
      const memberNames = allMembers
        .map((id) => {
          if (id === currentSessionForAdd.targetId) {
            return currentSessionForAdd.sessionName
          }
          const friend = friends.value.find((f) => f.userId === id)
          return friend?.nickname || ''
        })
        .filter(Boolean)
        .sort((a, b) => a.localeCompare(b, 'zh-Hans-CN', { sensitivity: 'accent' }))
      
      const autoName =
        memberNames
          .slice(0, 3)
          .join('、') + '的群聊'
      const res = await service.post('/group/create', {
        groupName: autoName,
        groupAvatar: '',
        joinType: 2, // 免审核
        memberIds: allMembers,
      })
      if (res.code === 200) {
        uni.showToast({ title: '群聊创建成功', icon: 'success' })
        showAddMemberModal.value = false
        addMemberSelectedFriends.value = []
        // 如果是移动端，关闭聊天信息页
        if (currentMobileChat.value) {
          mobileChatInfoPage.value = false
        }
        await loadSessionList()
        loadGroups()
      } else {
        uni.showToast({ title: res.message || '发起群聊失败', icon: 'none' })
      }
    }
  } catch (e) {
    console.error('添加成员异常', e)
    uni.showToast({ title: '网络异常，请重试', icon: 'none' })
  } finally {
    isAddingMember.value = false
  }
}

// ========== 消息长按菜单 + 撤回 ==========
const mobileContextMenu = ref({ visible: false, msg: null })

/** 长按消息气泡，显示上下文菜单 */
const showMsgContextMenu = (msg) => {
  if (!msg || msg.status === 3) return // 已撤回的消息不展示菜单
  mobileContextMenu.value = { visible: true, msg }
}

/** 关闭上下文菜单 */
const closeMsgContextMenu = () => {
  mobileContextMenu.value.visible = false
}

/** 复制消息文本 */
const copyMobileMsg = () => {
  const msg = mobileContextMenu.value.msg
  if (!msg) return
  const text = msg.content || ''
  // #ifdef H5
  if (navigator.clipboard) {
    navigator.clipboard.writeText(text).then(() => {
      uni.showToast({ title: '已复制', icon: 'success' })
    })
  } else {
    uni.showToast({ title: '复制失败', icon: 'none' })
  }
  // #endif
  // #ifndef H5
  uni.setClipboardData({
    data: text,
    success: () => uni.showToast({ title: '已复制', icon: 'success' }),
  })
  // #endif
  closeMsgContextMenu()
}

/**
 * 撤回消息：调用后端接口，成功后就地更新消息状态
 * 同时支持 PC 端右键撤回和移动端长按撤回
 */
const revokeMessage = async (msg) => {
  if (!msg || !msg.id) return
  // 关闭移动端菜单（如果已开启）
  closeMsgContextMenu()
  try {
    const res = await service.put(
      '/chat/message/revoke?messageId=' + encodeURIComponent(msg.id),
    )
    if (res.code === 200) {
      // 就地更新消息状态，无需重新拉取
      const idx = messages.value.findIndex(
        (m) => String(m.id) === String(msg.id),
      )
      if (idx !== -1) {
        messages.value[idx] = {
          ...messages.value[idx],
          status: 3,
          content_replaced: '[消息已撤回]',
        }
      }
      uni.showToast({ title: '消息已撤回', icon: 'success' })
    } else {
      uni.showToast({ title: res.message || '撤回失败', icon: 'none' })
    }
  } catch (e) {
    const errMsg = e?.message || e?.data?.message || '撤回失败'
    uni.showToast({ title: errMsg, icon: 'none' })
  }
}

/** PC 端复制消息提示 */
const handlePcCopy = (text) => {
  uni.showToast({ title: '已复制', icon: 'success' })
}

/**
 * 处理语音通话请求
 * 仅支持单聊，通过 WebSocket 发送呼叫请求
 */
const handleVoiceCall = async (session) => {
  if (!session) return
  
  // 检查是否为单聊
  if (session.sessionType === SESSION_TYPE.GROUP) {
    uni.showToast({
      title: '群聊暂不支持语音通话',
      icon: 'none'
    })
    return
  }
  
  try {
    // 获取好友详细信息（用于显示头像等）
    const friend = getFriendForCurrentSession()
    if (!friend) {
      uni.showToast({
        title: '未找到好友信息',
        icon: 'none'
      })
      return
    }
    
    // 生成统一的 sessionId，确保整个通话过程中使用同一个 ID
    const callSessionId = `voice_${Date.now()}`
    
    // 设置通话参数（优先使用会话名，其次好友昵称，避免显示“未知用户”）
    voiceCallPeerInfo.value = {
      id: session.targetId,
      nickname: session.sessionName || friend.nickname || '未知用户',
      avatar: session.sessionAvatar || friend.avatar || defaultAvatar
    }
    voiceCallDirection.value = 'outgoing' // 呼出
    voiceCallSessionId.value = callSessionId // 传递 sessionId
    voiceCallMode.value = 'audio' // 默认语音
    
    // 显示语音通话组件
    showVoiceCall.value = true
    
    console.log('📞 发起语音通话，sessionId:', callSessionId, '对方信息:', voiceCallPeerInfo.value)
  } catch (error) {
    console.error('发起语音通话失败:', error)
    uni.showToast({
      title: '发起通话失败',
      icon: 'none'
    })
  }
}

/**
 * 处理视频通话请求
 * 仅支持单聊，通过 WebSocket 发送呼叫请求
 */
const handleVideoCall = async (session) => {
  if (!session) return
  
  // 检查是否为单聊
  if (session.sessionType === SESSION_TYPE.GROUP) {
    uni.showToast({
      title: '群聊暂不支持视频通话',
      icon: 'none'
    })
    return
  }
  
  try {
    // 获取好友详细信息（用于显示头像等）
    const friend = getFriendForCurrentSession()
    if (!friend) {
      uni.showToast({
        title: '未找到好友信息',
        icon: 'none'
      })
      return
    }
    
    // 设置通话参数，优先会话名称再好友昵称
    voiceCallPeerInfo.value = {
      id: session.targetId,
      nickname: session.sessionName || friend.nickname || '未知用户',
      avatar: session.sessionAvatar || friend.avatar || defaultAvatar
    }
    voiceCallDirection.value = 'outgoing' // 呼出
    voiceCallMode.value = 'video'
    
    // 显示通话组件（语音/视频共用）
    showVoiceCall.value = true
    
    console.log('发起视频通话，目标:', voiceCallPeerInfo.value)
  } catch (error) {
    console.error('发起视频通话失败:', error)
    uni.showToast({
      title: '发起通话失败',
      icon: 'none'
    })
  }
}

/**
 * 取消语音通话
 */
const cancelVoiceCall = (session) => {
  console.log('取消语音通话:', session)
  // 关闭通话组件
  showVoiceCall.value = false
  // TODO: 发送取消信号到 WebSocket
}

// 打开网盘
const openCloudDrive = () => {
  // 切换到聊天标签页
  currentSidebarTab.value = 'chat'
  // 切换网盘显示状态
  showCloudDrive.value = !showCloudDrive.value
}

// 从消息上传到网盘
const handleUploadToCloud = async (msg) => {
  if (!msg || !msg.file_url) return uni.showToast({ title: '无可上传文件', icon: 'none' })
  try {
    const res = await service.post('/cloud/import', { fileUrl: msg.file_url })
    if (res.code === 200) {
      uni.showToast({ title: '已发送到网盘', icon: 'success' })
      // 自动切换到聊天标签页并打开网盘
      currentSidebarTab.value = 'chat'
      showCloudDrive.value = true
    } else {
      uni.showToast({ title: res.message || '上传失败', icon: 'none' })
    }
  } catch (e) {
    console.error('上传到网盘异常', e)
    uni.showToast({ title: '上传失败', icon: 'none' })
  }
}

/**
 * 取消视频通话
 */
const cancelVideoCall = (session) => {
  console.log('取消视频通话:', session)
  // 关闭通话组件
  showVoiceCall.value = false
  // TODO: 发送取消信号到 WebSocket
}

/**
 * 语音通话组件关闭回调
 */
const handleVoiceCallClose = () => {
  showVoiceCall.value = false
  voiceCallPeerInfo.value = {}
  voiceCallDirection.value = 'outgoing'
  voiceCallSessionId.value = '' // 清空 sessionId
}

/**
 * 语音通话状态变化回调
 */
const handleVoiceCallStateChange = (state) => {
  console.log('语音通话状态变化:', state)
  // 如果通话结束，关闭组件
  if (state === 'ended') {
    handleVoiceCallClose()
  }
}

/**
 * 扫一扫：原生 APP 使用 uni.scanCode；
 * H5 环境调用浏览器摄像头 API（需要 HTTPS）或提示引导安装 APP
 */
const mobileScan = () => {
  showMobilePlusMenu.value = false
  // #ifndef H5
  uni.scanCode({
    success: (res) => {
      uni.showModal({
        title: '扫码结果',
        content: res.result,
        showCancel: false,
      })
    },
    fail: () => uni.showToast({ title: '扫码失败', icon: 'none' }),
  })
  // #endif
  // #ifdef H5
  uni.showToast({
    title: '请在手机 APP 中使用扫一扫',
    icon: 'none',
    duration: 2000,
  })
  // #endif
}

// ========== 移动端方法（H5 + Native 共用） ==========

// 进入聊天详情
const enterMobileChat = (session) => {
  currentMobileChat.value = session
  currentSession.value = session
  messages.value = []
  loadMobileMessages(session.sessionId)
  scrollToView.value = ''
}

// 返回上一级：聊天信息页 → 聊天详情 → 会话列表
const backToMobileList = () => {
  if (mobileChatInfoPage.value) {
    mobileChatInfoPage.value = false
    return
  }
  if (currentMobileChat.value) {
    currentMobileChat.value = null
    messages.value = []
    inputMsg.value = ''
    showEmojiMobile.value = false
    showMobileTools.value = false
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
  uni.showToast({ title: '语音功能开发中', icon: 'none' })
}

// ========== 生命周期 ==========
let wsMessageListener = null

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

  // 使用 nextTick 确保 DOM 更新完成后再连接WebSocket
  // 并使用延时确保token 存储完成
  setTimeout(() => {
    try {
      // 确保token已存储后再连接
      const token = uni.getStorageSync('satoken')
      if (token && typeof token === 'string' && token.trim() !== '') {
        wsClient.connect()
      } else {
        console.warn('用户未登录，无法连接WebSocket')
      }
    } catch (error) {
      console.error('WebSocket 连接失败:', error)
    }
  }, 500) // 延迟 500 毫秒，确保token 存储完成

  // 监听 WebSocket 消息（实时接收新消息）
  wsMessageListener = (data) => {
    handleWebSocketMessage(data)
  }
  uni.$on('wsMessage', wsMessageListener)
})

onUnmounted(() => {
  // #ifdef H5
  cleanupDeviceDetection()
  // #endif

  cleanupSendMessage()
  
  // 移除 WebSocket 消息监听
  if (wsMessageListener) {
    uni.$off('wsMessage', wsMessageListener)
  }
  
  // 关闭 WebSocket 连接
  wsClient.close()
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

/* 通讯录「添加朋友」入口：与好友项同高、同结构，文字与好友名对齐 */
.add-friend-entry {
  border-bottom: 1px solid #e6e6e6;
}
.add-friend-entry .add-friend-avatar {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  margin-right: 10px;
  flex-shrink: 0;
  background: #f0f0f0;
  border: 1px dashed #ccc;
  display: flex;
  align-items: center;
  justify-content: center;
}
.add-friend-entry .add-friend-plus {
  font-size: 22px;
  color: #666;
  line-height: 1;
}
.add-friend-entry .contact-info {
  flex: 1;
  min-width: 0;
  margin-left: 10px;
}
.add-friend-entry .contact-name,
.add-friend-entry .contact-desc {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 2px;
}
.add-friend-entry .contact-desc {
  font-size: 12px;
  font-weight: 400;
  color: #999;
}

/* 添加朋友悬浮窗：PC 端习惯的浮层 + 搜索框 */
.add-friend-popover-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1600;
}
.add-friend-popover {
  background: #fff;
  border-radius: 12px;
  width: 420px;
  max-width: 90vw;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}
.add-friend-popover-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}
.add-friend-popover-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
.add-friend-popover-close {
  font-size: 16px;
  color: #999;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}
.add-friend-popover-close:hover {
  color: #333;
  background: #f5f5f5;
}
.add-friend-popover-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: #f7f8fa;
  border-bottom: 1px solid #f0f0f0;
}
.add-friend-search-icon {
  font-size: 14px;
  color: #999;
  flex-shrink: 0;
}
.add-friend-search-input {
  flex: 1;
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 14px;
  outline: none;
  background: #fff;
}
.add-friend-search-input:focus {
  border-color: #07c160;
}
.add-friend-search-clear {
  font-size: 13px;
  color: #999;
  cursor: pointer;
  padding: 4px;
}
.add-friend-popover-actions {
  padding: 0 20px 12px;
}
.add-friend-search-btn {
  width: 100%;
  padding: 10px;
  background: #07c160;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}
.add-friend-search-btn:disabled {
  background: #b8e6cf;
  cursor: not-allowed;
}
.add-friend-search-btn:not(:disabled):hover {
  background: #06ad56;
}
.add-friend-popover-body {
  padding: 16px 20px 20px;
  min-height: 100px;
}
.add-friend-hint,
.add-friend-loading,
.add-friend-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 0;
  color: #999;
  font-size: 14px;
}
.add-friend-hint-icon,
.add-friend-empty-icon {
  font-size: 28px;
}
.add-friend-result {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: #f7f8fa;
  border-radius: 10px;
  border: 1px solid #ebebeb;
}
.add-friend-result-avatar {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  flex-shrink: 0;
  background: #e6e6e6;
}
.add-friend-result-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.add-friend-result-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}
.add-friend-result-username,
.add-friend-result-sig {
  font-size: 12px;
  color: #888;
}
.add-friend-result-action .add-friend-btn {
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  border: none;
  cursor: pointer;
  white-space: nowrap;
}
.add-friend-btn.add {
  background: #07c160;
  color: #fff;
}
.add-friend-btn.add:not(:disabled):hover {
  background: #06ad56;
}
.add-friend-btn.disabled,
.add-friend-btn.pending {
  background: #f0f0f0;
  color: #999;
  cursor: not-allowed;
}
.add-friend-btn.default {
  background: #fff;
  color: #333;
  border: 1px solid #d9d9d9;
}
.add-friend-remark-panel {
  margin-top: 12px;
  padding: 12px 14px;
  background: #f7f8fa;
  border-radius: 8px;
  border: 1px solid #ebebeb;
}
.add-friend-remark-label {
  font-size: 13px;
  color: #666;
  display: block;
  margin-bottom: 8px;
}
.add-friend-remark-input {
  width: 100%;
  min-height: 56px;
  font-size: 14px;
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  padding: 8px 10px;
  box-sizing: border-box;
  resize: none;
  outline: none;
  margin-bottom: 10px;
}
.add-friend-remark-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.add-friend-remark-count {
  font-size: 12px;
  color: #999;
}
.add-friend-remark-btns {
  display: flex;
  gap: 8px;
}

/* PC 端添加成员悬浮窗：顶部搜索 + 联系人列表 */
.add-member-popover-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1600;
}
.add-member-popover {
  background: #fff;
  border-radius: 12px;
  width: 380px;
  max-height: 80vh;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.add-member-popover-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}
.add-member-popover-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
.add-member-popover-close {
  font-size: 16px;
  color: #999;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}
.add-member-popover-close:hover {
  color: #333;
  background: #f5f5f5;
}
.add-member-popover-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: #f7f8fa;
  border-bottom: 1px solid #f0f0f0;
}
.add-member-search-icon {
  font-size: 14px;
  color: #999;
}
.add-member-search-input {
  flex: 1;
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 14px;
  outline: none;
}
.add-member-popover-list {
  flex: 1;
  min-height: 200px;
  max-height: 360px;
  overflow-y: auto;
}
.add-member-popover-item {
  display: flex;
  align-items: center;
  padding: 10px 20px;
  cursor: pointer;
  gap: 12px;
  border-bottom: 1px solid #f5f5f5;
}
.add-member-popover-item:hover {
  background: #f7f8fa;
}
.add-member-checkbox {
  width: 20px;
  height: 20px;
  border: 1px solid #ddd;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.add-member-checkbox.cg-checked {
  background: #07c160;
  border-color: #07c160;
}
.add-member-checkbox .cg-check-mark {
  color: #fff;
  font-size: 12px;
}
.add-member-avatar {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  flex-shrink: 0;
  background: #eee;
}
.add-member-name {
  flex: 1;
  font-size: 14px;
  color: #333;
}
.add-member-empty {
  padding: 24px;
  text-align: center;
  color: #999;
  font-size: 14px;
}
.add-member-popover-footer {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
}
.add-member-confirm-btn {
  width: 100%;
  padding: 10px;
  font-size: 14px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  background: #e0e0e0;
  color: #999;
}
.add-member-confirm-btn.add-member-confirm-active {
  background: #07c160;
  color: #fff;
}
.add-member-confirm-btn.add-member-confirm-active:hover:not(:disabled) {
  background: #06ad56;
}
.add-member-confirm-btn:disabled {
  cursor: not-allowed;
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
  transition: all 0.2s ease;
}
.header-tools .tool-icon:hover {
  transform: scale(1.1);
  opacity: 0.8;
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
  position: relative; /* 使内部 absolute 子元素相对定位正确 */
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

/* 放大镜与 + 号同一条水平线对齐 */
.mobile-nav-icons {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 28px;
}

.mobile-icon {
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  padding: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
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
  margin-bottom: 16px;
  /* 头像与气泡对齐顶部 */
  align-items: flex-start;
  gap: 10px;
}
.mobile-message-sender {
  font-size: 12px;
  color: #666;
  margin-bottom: 3px;
}
/* 自己的消息：反转排列，头像在右、气泡在右对齐 */
.mobile-message-bubble.mobile-own {
  flex-direction: row-reverse;
}

.mobile-bubble-avatar {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  flex-shrink: 0;
}

.mobile-bubble-content {
  display: flex;
  flex-direction: column;
  max-width: 65%;
}
/* 自己的气泡内容右对齐 */
.mobile-message-bubble.mobile-own .mobile-bubble-content {
  align-items: flex-end;
}

/* 其他人：白底气泡 */
.mobile-text-msg {
  background: #fff;
  padding: 10px 14px;
  border-radius: 10px;
  border-top-left-radius: 2px;
  word-wrap: break-word;
  font-size: 16px;
  line-height: 1.55;
  color: #111;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}
/* 自己：绿色气泡 */
.mobile-message-bubble.mobile-own .mobile-text-msg {
  background: #95ec69;
  border-radius: 10px;
  border-top-right-radius: 2px;
  border-top-left-radius: 10px;
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

/* 输入栏：仿微信移动端风格 */
.mobile-input-bar {
  background: #f5f5f5;
  padding: 8px 12px;
  padding-bottom: max(8px, env(safe-area-inset-bottom));
  border-top: 1px solid #e0e0e0;
  flex-shrink: 0;
}

/* 主输入行：[语音] [输入框] [表情] [发送/更多] */
.mobile-input-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 工具图标按鈕 */
.mobile-input-action {
  font-size: 24px;
  cursor: pointer;
  padding: 4px;
  flex-shrink: 0;
  user-select: none;
}
.mobile-input-more {
  font-size: 22px;
}

.mobile-text-input {
  flex: 1;
  min-width: 0;
  height: 38px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 0 12px;
  font-size: 16px;
  background: #fff;
  outline: none;
}

.mobile-send-btn {
  padding: 8px 16px;
  background: #07c160;
  color: #fff;
  border-radius: 5px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  user-select: none;
}

/* 表情选择器 */
.mobile-emoji-picker {
  background: #fff;
  border-top: 1px solid #e5e5e5;
  height: 200px;
  margin-top: 8px;
  border-radius: 8px;
  overflow: hidden;
}

.mobile-emoji-scroll {
  width: 100%;
  height: 100%;
}

.mobile-emoji-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 8px 12px;
  gap: 6px;
}

.mobile-emoji-item {
  font-size: 26px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
}
.mobile-emoji-item:active {
  background: #f0f0f0;
}

/* 工具面板：图片、文件等 */
.mobile-tools-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 16px 4px 8px;
  gap: 24px;
}

.mobile-tool-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.mobile-tool-icon-wrap {
  width: 56px;
  height: 56px;
  background: #fff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}
.mobile-tool-icon-wrap:active {
  background: #f5f5f5;
}

.mobile-tool-label {
  font-size: 11px;
  color: #666;
  text-align: center;
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
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
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

/* 移动端 ➕ 下拉菜单：白底圆角卡片风格，与 PC 端一致 */
.mobile-plus-wrapper {
  position: relative;
  flex-shrink: 0;
}
.mobile-icon-active {
  opacity: 0.6;
}
.mobile-plus-dropdown {
  position: absolute;
  top: calc(100% + 10px);
  right: -4px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  min-width: 160px;
  z-index: 1000;
  overflow: hidden;
  border: 1px solid #f0f0f0;
}
/* 小三角箭头 */
.mobile-plus-dropdown::before {
  content: '';
  position: absolute;
  top: -6px;
  right: 14px;
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-bottom: 6px solid #f0f0f0;
}
.mobile-plus-dropdown::after {
  content: '';
  position: absolute;
  top: -5px;
  right: 14px;
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-bottom: 6px solid #fff;
}
.mobile-plus-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid #f5f5f5;
}
.mobile-plus-item:last-child {
  border-bottom: none;
}
.mobile-plus-item:active {
  background: #f5f5f5;
}
.mobile-plus-icon {
  font-size: 18px;
  flex-shrink: 0;
  width: 24px;
  text-align: center;
}
.mobile-plus-text {
  font-size: 14px;
  color: #333;
  flex: 1;
}

/* 全屏聊天信息页 */
.mobile-chat-info-full {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
  overflow: hidden;
}
.mobile-chat-title {
  flex: 1;
  text-align: center;
  font-size: 17px;
  font-weight: 600;
  color: #000;
}
.mobile-chat-info-scroll {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}
/* 成员头像区 */
.mobile-chat-info-members {
  background: #fff;
  padding: 24px 16px 20px;
  display: flex;
  gap: 16px;
  align-items: flex-start;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 10px;
}
.mobile-chat-info-member {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.mobile-info-member-avatar {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  background: #ddd;
}
.mobile-info-member-name {
  font-size: 12px;
  color: #555;
  max-width: 64px;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mobile-chat-info-add {
  width: 56px;
  height: 56px;
  border: 1.5px dashed #ccc;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.mobile-chat-info-add-icon {
  font-size: 30px;
  color: #ccc;
  line-height: 1;
}

/* 移动端移除成员按钮 */
.mobile-remove-member-btn {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #ff4757;
  color: white;
  font-size: 12px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  z-index: 10;
}

.mobile-chat-info-member:hover .mobile-remove-member-btn,
.mobile-chat-info-member:active .mobile-remove-member-btn {
  opacity: 1;
}

.mobile-remove-member-btn:active {
  background: #ff2e42;
  transform: scale(1.1);
}
/* 设置列表分组 */
.mobile-chat-info-section {
  background: #fff;
  margin-bottom: 10px;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}
.mobile-chat-info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.15s;
}
.mobile-chat-info-row:last-child {
  border-bottom: none;
}
.mobile-chat-info-row:active {
  background: #f5f5f5;
}
.mobile-chat-info-label {
  font-size: 16px;
  color: #111;
}
.mobile-chat-info-danger {
  color: #ff3b30 !important;
}
.mobile-chat-info-arrow {
  font-size: 20px;
  color: #c7c7cc;
  font-weight: 300;
}
/* 自定义 Toggle 开关（仳 iOS 风格） */
.mobile-switch {
  width: 51px;
  height: 31px;
  border-radius: 16px;
  background: #e5e5ea;
  position: relative;
  cursor: pointer;
  transition: background 0.25s;
  flex-shrink: 0;
}
.mobile-switch.mobile-switch-on {
  background: #34c759;
}
.mobile-switch-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 27px;
  height: 27px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
  transition: left 0.25s;
}
.mobile-switch.mobile-switch-on .mobile-switch-thumb {
  left: 22px;
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
  font-size: 14px;
  cursor: pointer;
  position: relative;
}
.mobile-contact-tab-bar .mobile-tab-item.mobile-active {
  color: #07c160;
  border-bottom: 2px solid #07c160;
  font-weight: 500;
}

/* 通知 tab 红点徽标 */
.mobile-notify-tab {
  position: relative;
}
.mobile-tab-badge {
  position: absolute;
  top: 6px;
  right: 6px;
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
  pointer-events: none;
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

.mobile-cloud-drive-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.mobile-cloud-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
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

/* ===== 发起群聊全屏选人弹窗 ===== */
/* position absolute 覆盖整个 mobile-container，实现全屏安碪页 */
.mobile-create-group-page {
  position: absolute;
  inset: 0;
  z-index: 200;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

/* 顶部操作栏中的“完成”按钮 */
.mobile-confirm-btn {
  font-size: 15px;
  color: #c7c7cc;
  cursor: pointer;
  padding: 4px 0;
  white-space: nowrap;
  min-width: 60px;
  text-align: right;
}
.mobile-confirm-active {
  color: #07c160 !important;
  font-weight: 500;
}

/* 群名输入框区域 */
.mobile-cg-name-wrap {
  background: #fff;
  padding: 0 16px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 10px;
  border-top: 1px solid #f0f0f0;
}
.mobile-cg-name-input {
  width: 100%;
  height: 50px;
  font-size: 15px;
  color: #111;
  border: none;
  outline: none;
  background: transparent;
  box-sizing: border-box;
}

/* 好友列表滚动区 */
.mobile-cg-scroll {
  flex: 1;
  overflow-y: auto;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

/* 单个好友条目 */
.mobile-cg-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.15s;
  user-select: none;
}
.mobile-cg-item:active {
  background: #f5f5f5;
}

/* 自定义圆形 Checkbox */
.mobile-cg-checkbox {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid #d0d0d0;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 14px;
  flex-shrink: 0;
  transition:
    border-color 0.2s,
    background 0.2s;
}
.cg-checked {
  border-color: #07c160 !important;
  background: #07c160 !important;
}
.cg-check-mark {
  color: #fff;
  font-size: 13px;
  font-weight: bold;
  line-height: 1;
}

/* 好友头像 */
.mobile-cg-avatar {
  width: 46px;
  height: 46px;
  border-radius: 6px;
  background: #e0e0e0;
  margin-right: 12px;
  flex-shrink: 0;
}

/* 好友名称 */
.mobile-cg-name {
  font-size: 15px;
  color: #111;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 空状态提示 */
.mobile-cg-empty {
  text-align: center;
  padding: 50px 16px;
  color: #999;
  font-size: 14px;
}

/* 底部确认按钮区 */
.mobile-cg-footer {
  background: #fff;
  padding: 12px 16px;
  padding-bottom: max(12px, env(safe-area-inset-bottom));
  border-top: 1px solid #f0f0f0;
}
.mobile-cg-submit-btn {
  background: #07c160;
  color: #fff;
  border-radius: 8px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
  user-select: none;
}
.mobile-cg-submit-btn:active {
  opacity: 0.8;
}
.cg-submit-disabled {
  background: #b2b2b2 !important;
  cursor: not-allowed;
  pointer-events: none;
}

/* ===== 群头像上传区 ===== */
.mobile-cg-avatar-section {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  flex-shrink: 0;
}
.mobile-cg-avatar-section:active {
  opacity: 0.8;
}
.mobile-cg-avatar-preview {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  object-fit: cover;
  display: block;
}
.mobile-cg-avatar-placeholder {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  border: 2px dashed #c8c8c8;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  gap: 4px;
}
.mobile-cg-avatar-icon {
  font-size: 22px;
  line-height: 1;
}
.mobile-cg-avatar-hint {
  font-size: 11px;
  color: #999;
  text-align: center;
  line-height: 1.3;
}

/* ===== 添加好友全屏页 ===== */
.mobile-add-friend-page {
  position: absolute;
  inset: 0;
  z-index: 200;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}
.mobile-af-search-bar {
  display: flex;
  align-items: center;
  margin: 12px 16px;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e8e8e8;
  flex-shrink: 0;
}
.mobile-af-search-input {
  flex: 1;
  height: 44px;
  padding: 0 12px;
  font-size: 15px;
  color: #111;
  border: none;
  outline: none;
  background: transparent;
  box-sizing: border-box;
}
.mobile-af-search-btn {
  padding: 0 16px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #07c160;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
}
.mobile-af-search-btn:active {
  opacity: 0.8;
}
.mobile-af-loading {
  text-align: center;
  padding: 40px 16px;
  color: #999;
  font-size: 14px;
}
.mobile-af-empty {
  text-align: center;
  padding: 50px 16px;
  color: #999;
  font-size: 14px;
}
.mobile-af-result {
  flex: 1;
  padding: 0 16px;
  overflow-y: auto;
}
.mobile-af-user-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-top: 4px;
  gap: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.mobile-af-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: #e0e0e0;
  flex-shrink: 0;
}
.mobile-af-user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow: hidden;
}
.mobile-af-nickname {
  font-size: 16px;
  font-weight: 600;
  color: #111;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mobile-af-username {
  font-size: 13px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mobile-af-apply-btn {
  padding: 8px 16px;
  background: #07c160;
  color: #fff;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: opacity 0.15s;
}
.mobile-af-apply-btn:active {
  opacity: 0.75;
}

/* ===== 消息撤回显示 ===== */
.mobile-revoke-msg {
  font-size: 13px;
  color: #999;
  padding: 8px 12px;
  background: transparent;
  font-style: italic;
}

/* ===== 消息长按上下文菜单 ===== */
.mobile-ctx-overlay {
  position: fixed;
  inset: 0;
  z-index: 999;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.mobile-ctx-menu {
  width: 100%;
  max-width: 480px;
  background: #fff;
  border-radius: 14px 14px 0 0;
  padding-bottom: max(16px, env(safe-area-inset-bottom));
  overflow: hidden;
}
.mobile-ctx-item {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 54px;
  font-size: 17px;
  color: #111;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.1s;
  user-select: none;
}
.mobile-ctx-item:last-child {
  border-bottom: none;
}
.mobile-ctx-item:active {
  background: #f5f5f5;
}
.mobile-ctx-revoke {
  color: #e74c3c;
}
.mobile-ctx-cancel {
  color: #999;
  font-size: 15px;
  border-top: 6px solid #f5f5f5;
  margin-top: 2px;
}
</style>
