<template>
  <view class="wechat-page">
    <view v-if="isDesktop" class="desktop-shell">
      <view class="desktop-rail">
        <view class="rail-profile" @click="openSettings">
          <image class="rail-profile-avatar" :src="userInfo.avatar || defaultAvatar" mode="aspectFill" />
          <view class="rail-profile-status"></view>
        </view>

        <view class="rail-nav">
          <view
            v-for="item in railItems"
            :key="item.key"
            class="rail-item"
            :class="{ active: desktopTab === item.key }"
            @click="desktopTab = item.key"
          >
            <text class="rail-icon">{{ item.icon }}</text>
            <text class="rail-label">{{ item.label }}</text>
          </view>
        </view>

        <view class="rail-footer">
          <view class="rail-quick" @click="openQrModal">
            <text class="rail-quick-text">码</text>
          </view>
        </view>
      </view>

      <keep-alive>
        <DesktopSessionPane
          v-if="desktopTab === 'chat'"
          class="desktop-side"
          :userInfo="userInfo"
          :sessions="sessions"
          :activeSessionId="currentSession?.sessionId || ''"
          :loading="sessionsLoading"
          :hasMore="sessionsHasMore"
          @select-session="selectSession"
          @load-more="loadSessions(false)"
          @refresh="refreshDashboard"
          @open-profile="openSettings"
          @logout="logout"
        />

        <DesktopContactPane
          v-else-if="desktopTab === 'contacts'"
          class="desktop-side"
          :friends="friends"
          :groups="groups"
          :notifications="notifications"
          :pendingCount="pendingNotifyCount"
          :selectedKind="desktopContactSelectionType"
          :selectedId="desktopContactSelectedId"
          @preview-friend="previewDesktopFriend"
          @preview-group="previewDesktopGroup"
          @preview-requests="previewDesktopRequests"
          @open-qr-code="openQrModal"
        />
      </keep-alive>

      <view class="desktop-main" :class="{ cloud: desktopTab === 'cloud', contacts: desktopTab === 'contacts' }">
        <CloudDrive v-if="desktopTab === 'cloud'" />

        <ChatArea
          v-else-if="desktopTab === 'chat'"
          :session="currentSession"
          :messages="messages"
          :groupMembers="groupMembers"
          :currentUserId="currentUserId"
          :showVideoCall="supportsVideoCall"
          :loading="messagesLoading"
          :loadingMore="messagesLoadingMore"
          :hasMore="messagesHasMore"
          :showInfo="showChatInfo"
          :sending="sending"
          @load-more="loadMoreMessages"
          @toggle-info="showChatInfo = !showChatInfo"
          @toggle-top="toggleTop"
          @toggle-mute="toggleMute"
          @clear-history="clearLocalMessages"
          @pick-image="pickImage"
          @pick-file="pickFile"
          @send-text="sendTextMessage"
          @send-audio="sendAudioMessage"
          @send-sticker="sendStickerMessage"
          @retry-failed="retryFailedMessage"
          @start-call="startVoiceCall"
          @forward-message="handleForwardMessage"
          @message-recalled="handleMessageRecalled"
          @message-deleted="handleMessageDeleted"
        />

        <DesktopContactDetail
          v-else
          :selection="desktopContactSelection"
          :selectionType="desktopContactSelectionType"
          :notifications="notifications"
          :defaultAvatar="defaultAvatar"
          :supportsVideoCall="supportsVideoCall"
          @send-message="openSelectedDesktopContactChat"
          @start-audio="startDesktopAudioCall"
          @start-video="startDesktopVideoCall"
          @approve-friend="approveFriend"
          @reject-friend="rejectFriend"
          @approve-group="approveGroup"
          @reject-group="rejectGroup"
          @update-friend-relation="updateDesktopFriendRelation"
          @update-friend-blacklist="updateDesktopFriendBlacklist"
          @delete-friend="deleteDesktopFriend"
        />

      </view>
    </view>

    <view v-else class="mobile-shell">
      <view v-if="!mobileChatOpen" class="mobile-header">
        <view class="mobile-user" @click="openSettings">
          <image class="mobile-avatar" :src="userInfo.avatar || defaultAvatar" mode="aspectFill" />
          <view class="mobile-copy">
            <text class="mobile-title">{{ mobileTitle }}</text>
            <text class="mobile-sub">{{ userInfo.nickname || userInfo.username || '未登录用户' }}</text>
          </view>
        </view>

        <view class="mobile-actions">
          <text class="mobile-action" @click="refreshDashboard">刷新</text>
          <text class="mobile-action" @click="openQrModal">二维码</text>
          <text class="mobile-action" @click="openSettings">设置</text>
        </view>
      </view>

      <view class="mobile-content">
        <ChatArea
          v-if="mobileChatOpen && currentSession"
          mobile
          :session="currentSession"
          :messages="messages"
          :groupMembers="groupMembers"
          :currentUserId="currentUserId"
          :showVideoCall="supportsVideoCall"
          :loading="messagesLoading"
          :loadingMore="messagesLoadingMore"
          :hasMore="messagesHasMore"
          :showInfo="showChatInfo"
          :sending="sending"
          @back="closeMobileChat"
          @load-more="loadMoreMessages"
          @toggle-info="showChatInfo = !showChatInfo"
          @toggle-top="toggleTop"
          @toggle-mute="toggleMute"
          @clear-history="clearLocalMessages"
          @pick-image="pickImage"
          @pick-file="pickFile"
          @send-text="sendTextMessage"
          @send-audio="sendAudioMessage"
          @send-sticker="sendStickerMessage"
          @retry-failed="retryFailedMessage"
          @start-call="startVoiceCall"
          @forward-message="handleForwardMessage"
          @message-recalled="handleMessageRecalled"
          @message-deleted="handleMessageDeleted"
        />

        <keep-alive>
          <MobileSessionListPane
            v-if="mobileTab === 'chat'"
            :sessions="sessions"
            :activeSessionId="currentSession?.sessionId || ''"
            :loading="sessionsLoading"
            :hasMore="sessionsHasMore"
            @select-session="selectSession"
            @load-more="loadSessions(false)"
            @refresh="refreshDashboard"
          />

          <MobileContactPane
            v-else-if="mobileTab === 'contacts'"
            :userInfo="userInfo"
            :currentUserId="currentUserId"
            :friends="friends"
            :groups="groups"
            :notifications="notifications"
            :notifyLoading="notifyLoading"
            :pendingCount="pendingNotifyCount"
            :searchedUser="searchedUser"
            :searchingUser="searchingUser"
            :groupSearchResults="groupSearchResults"
            :searchingGroups="searchingGroups"
            :creatingGroup="creatingGroup"
            @select-friend="openFriendChat"
            @select-group="openGroupChat"
            @approve-friend="approveFriend"
            @reject-friend="rejectFriend"
            @approve-group="approveGroup"
            @reject-group="rejectGroup"
            @search-user="searchUser"
            @apply-friend="applyFriend"
            @search-groups="searchGroups"
            @apply-group="applyGroup"
            @create-group="createGroup"
            @open-qr-code="openQrModal"
          />

          <CloudDrive v-else />
        </keep-alive>
      </view>

      <MobileBottomTabBar
        v-if="!mobileChatOpen"
        v-model="mobileTab"
        :unreadCount="unreadSessionCount"
        :requestCount="pendingNotifyCount"
      />
    </view>

    <SettingsPanel
      :visible="showSettings"
      :userInfo="userInfo"
      :defaultAvatar="defaultAvatar"
      :isDesktop="isDesktop"
      @close="showSettings = false"
      @open-edit-profile="openEditProfile"
      @open-qr-code="openQrModal"
      @open-scan-page="openMobileScanPage"
      @logout="logout"
    />

    <QrCodeModal
      :visible="showQrDialog"
      :userInfo="userInfo"
      :defaultAvatar="defaultAvatar"
      :isDesktop="isDesktop"
      @close="showQrDialog = false"
    />

    <VoiceCall
      :visible="voiceDialogVisible"
      :incoming="voiceIncoming"
      :peer="voicePeer"
      :sessionId="voiceSessionId"
      :mode="voiceMode"
      :transport="voiceTransport"
      :signal="voiceSignal"
      :currentUserId="currentUserId"
      :mobile="!isDesktop"
      @close="closeVoiceDialog"
      @ended="handleVoiceEnded"
    />
  </view>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import service from '@/utils/request'
import { APP_PERMISSION_SCOPE, ensureAnyAppPermissionAccess } from '@/utils/app-permission'
import wsClient from '@/utils/websocket'
import { DEFAULT_AVATAR, getDeviceType } from '@/utils/common'
import { getDeviceId } from '@/utils/device'
import notificationManager from '@/utils/notification-manager'

// #ifdef APP-PLUS
import { showLocalNotification, onNotificationClick, setAppBadge as setPlusBadge } from '@/utils/local-notification.js'
// #endif

import { hasConfiguredNativeWebRtcPlugin, hasRtcStreamConfig } from '@/utils/config'
import { uploadFileWithJSON } from '@/utils/file-upload'
import ChatStorage from '@/utils/chat-storage'
import LocalStateCache from '@/utils/local-state-cache'
import { getViewportWidth, isAppPlusRuntime, supportsBrowserDom } from '@/utils/runtime'
import { useNotifications } from '@/composables/useNotifications'
import {
  getLastSyncTime,
  saveLastSyncTime,
  fetchOfflineMessages,
  ackOfflineMessages,
} from '@/composables/useMessageSync.js'
import DesktopSessionPane from '@/components/home/DesktopSessionPane.vue'
import DesktopContactPane from '@/components/home/DesktopContactPane.vue'
import DesktopContactDetail from '@/components/home/DesktopContactDetail.vue'
import MobileSessionListPane from '@/components/home/MobileSessionListPane.vue'
import MobileContactPane from '@/components/home/MobileContactPane.vue'
import MobileBottomTabBar from '@/components/home/MobileBottomTabBar.vue'
import ChatArea from '@/components/home/ChatArea.vue'
import CloudDrive from '@/components/home/CloudDrive.vue'
import SettingsPanel from '@/components/home/SettingsPanel.vue'
import QrCodeModal from '@/components/home/QrCodeModal.vue'
import VoiceCall from '@/components/home/VoiceCall.vue'

const SESSION_TYPE = { SINGLE: 1, GROUP: 2 }
const MESSAGE_TYPE = { TEXT: 1, IMAGE: 2, VIDEO: 3, AUDIO: 4, FILE: 5, EMOJI: 6 }
const CALL_TYPE = { CALL: '1', ANSWER: '2', REJECT: '3', HANGUP: '4', SDP: '5', ICE: '6' }
const PAGE_SIZE = 50
const DASHBOARD_CACHE_GRACE_MS = 60 * 1000
const GROUP_MEMBERS_CACHE_GRACE_MS = 5 * 60 * 1000
const HOME_CACHE_KEYS = {
  USER_INFO: 'user_info',
  FRIENDS: 'friends',
  GROUPS: 'groups',
  SESSIONS: 'sessions',
  NOTIFICATIONS: 'notifications',
  ACTIVE_SESSION_ID: 'active_session_id',
  ACTIVE_CONTACT_SELECTION: 'active_contact_selection',
}

const defaultAvatar = DEFAULT_AVATAR
const currentDeviceType = getDeviceType()
const currentDeviceId = getDeviceId()

const railItems = [
  { key: 'chat', label: '聊天', icon: '聊' },
  { key: 'contacts', label: '通讯录', icon: '友' },
  { key: 'cloud', label: '云盘', icon: '盘' },
]

const userInfo = ref({})
const friends = ref([])
const groups = ref([])
const sessions = ref([])
const currentSession = ref(null)
const groupMembers = ref([])
const messages = ref([])
const searchedUser = ref(null)
const groupSearchResults = ref([])

const sessionsLoading = ref(false)
const sessionsHasMore = ref(false)
const sessionsCursor = ref('')
const messagesLoading = ref(false)
const messagesLoadingMore = ref(false)
const messagesHasMore = ref(false)
const currentMessagePage = ref(1)
const searchingUser = ref(false)
const searchingGroups = ref(false)
const creatingGroup = ref(false)
const pendingSendCount = ref(0)
const desktopTab = ref('chat')
const mobileTab = ref('chat')
const mobileChatOpen = ref(false)
const isDesktop = ref(true)
const showChatInfo = ref(false)
const showSettings = ref(false)
const showQrDialog = ref(false)
const desktopContactSelection = ref(null)
const desktopContactSelectionType = ref('friend')
const voiceDialogVisible = ref(false)
const voiceIncoming = ref(false)
const voicePeer = ref({})
const voiceMode = ref('audio')
const voiceTransport = ref('webrtc')
const voiceSessionId = ref('')
const voiceSignal = ref(null)

let wsBound = false
let refreshTimer = null
let resizeTimer = null
let dashboardRefreshPromise = null
let lastDashboardRefreshAt = 0
let hasFinishedInitialDashboardLoad = false
let messageLoadToken = 0
let dashboardCacheSkipUntil = 0
let offlineSyncPromise = null
let lastVisibleTime = Date.now() // 记录上次页面可见时间
const DASHBOARD_REFRESH_COOLDOWN_MS = 1200
const BACKGROUND_REFRESH_THRESHOLD_MS = 30000 // 超过30秒后台则强制刷新

const currentUserId = computed(() => String(userInfo.value.id || ''))
const supportsVideoCall = computed(() => {
  if (isAppPlusRuntime()) {
    return hasConfiguredNativeWebRtcPlugin() || hasRtcStreamConfig()
  }
  return true
})
const sending = computed(() => pendingSendCount.value > 0)
const mobileTitle = computed(() => {
  if (mobileTab.value === 'contacts') return '通讯录'
  if (mobileTab.value === 'cloud') return '云盘'
  return '微信'
})

const desktopContactSelectedId = computed(() => {
  if (!desktopContactSelection.value) return ''
  if (desktopContactSelectionType.value === 'group') {
    return String(desktopContactSelection.value.groupId || desktopContactSelection.value.id || '')
  }
  if (desktopContactSelectionType.value === 'friend') {
    return String(desktopContactSelection.value.userId || '')
  }
  return ''
})

const unreadSessionCount = computed(() =>
  sessions.value.reduce((total, item) => total + Number(item.unreadCount || 0), 0),
)

const syncNativeBadge = (count = 0) => {
  // #ifdef APP-PLUS
  setPlusBadge(Math.max(0, Number(count) || 0))
  // #endif
}

const resolveDashboardCacheScope = () => {
  const currentId = String(userInfo.value?.id || '')
  if (currentId) {
    return `dashboard:${currentId}`
  }

  try {
    const stored = uni.getStorageSync('userInfo') || {}
    const storedId = String(stored?.id || '')
    if (storedId) {
      return `dashboard:${storedId}`
    }
  } catch {}

  return 'dashboard:anonymous'
}

const readCacheRecord = (key, options = {}) =>
  LocalStateCache.get(resolveDashboardCacheScope(), key, options)

const readCacheValue = (key, options = {}) =>
  LocalStateCache.getValue(resolveDashboardCacheScope(), key, options)

const writeCacheValue = (key, value) => LocalStateCache.set(resolveDashboardCacheScope(), key, value)

const normalizeCachedArray = (value) => (Array.isArray(value) ? value : [])

const resolveGroupMembersCacheKey = (groupId) => `group_members:${String(groupId || '')}`
const resolveChatDetailStateKey = (sessionId) => `chat_detail_state:${String(sessionId || '')}`

const normalizeGroupMember = (item = {}) => ({
  userId: String(item.userId || item.id || item.senderId || ''),
  nickname: item.nickname || item.senderName || item.username || '',
  avatar: item.avatar || item.senderAvatar || '',
  role: Number(item.role || 0),
})

const normalizeGroupMembers = (list = []) =>
  normalizeCachedArray(list).map(normalizeGroupMember).filter((item) => item.userId)

const readGroupMembersCacheRecord = (groupId, options = {}) => {
  const normalizedGroupId = String(groupId || '')
  if (!normalizedGroupId) return null
  return LocalStateCache.get(resolveDashboardCacheScope(), resolveGroupMembersCacheKey(normalizedGroupId), options)
}

const writeGroupMembersCache = (groupId, members = []) => {
  const normalizedGroupId = String(groupId || '')
  if (!normalizedGroupId) return
  LocalStateCache.set(
    resolveDashboardCacheScope(),
    resolveGroupMembersCacheKey(normalizedGroupId),
    normalizeGroupMembers(members),
  )
}

const readChatDetailState = (sessionId) => {
  const normalizedSessionId = String(sessionId || '')
  if (!normalizedSessionId) return null
  return LocalStateCache.getValue(
    resolveDashboardCacheScope(),
    resolveChatDetailStateKey(normalizedSessionId),
  )
}

const writeChatDetailState = (sessionId, value = {}) => {
  const normalizedSessionId = String(sessionId || '')
  if (!normalizedSessionId) return
  LocalStateCache.set(resolveDashboardCacheScope(), resolveChatDetailStateKey(normalizedSessionId), {
    showInfo: Boolean(value?.showInfo),
  })
}

const getLatestDashboardCacheUpdatedAt = () =>
  [
    HOME_CACHE_KEYS.USER_INFO,
    HOME_CACHE_KEYS.FRIENDS,
    HOME_CACHE_KEYS.GROUPS,
    HOME_CACHE_KEYS.SESSIONS,
    HOME_CACHE_KEYS.NOTIFICATIONS,
  ]
    .map((key) => Number(readCacheRecord(key)?.updatedAt || 0))
    .reduce((latest, current) => Math.max(latest, current), 0)

const isAbortLikeRequestError = (error) =>
  /abort|aborted|cancel/i.test(error?.message || '') || error?.type === 'abort'

const {
  notifications,
  notifyLoading,
  pendingNotifyCount,
  loadNotifications,
  handleFriendApply,
  handleGroupApply,
} = useNotifications({
  onApproved: async () => {
    await Promise.all([loadFriends(), loadGroups(), loadSessions(true)])
  },
})

const sortSessions = (list) =>
  [...list].sort((left, right) => {
    if (Number(left.isTop || 0) !== Number(right.isTop || 0)) {
      return Number(right.isTop || 0) - Number(left.isTop || 0)
    }
    return new Date(right.lastMessageTime || 0) - new Date(left.lastMessageTime || 0)
  })

const normalizeSession = (item) => ({
  sessionId: String(item.sessionId || ''),
  sessionType: Number(item.sessionType || SESSION_TYPE.SINGLE),
  targetId: String(item.targetId || ''),
  sessionName: item.sessionName || '',
  sessionAvatar: item.sessionAvatar || '',
  lastMessageContent: item.lastMessageContent || '',
  lastMessageTime: item.lastMessageTime || '',
  unreadCount: Number(item.unreadCount || 0),
  isTop: Number(item.isTop || 0),
  isMute: Number(item.isMute || 0),
})

const applyCachedFriends = (list = []) => {
  friends.value = normalizeCachedArray(list).map((item) => ({
    userId: String(item.userId || ''),
    username: item.username || item.nickname || String(item.userId || ''),
    nickname: item.nickname || item.username || String(item.userId || ''),
    originNickname: item.originNickname || item.nickname || item.username || String(item.userId || ''),
    avatar: item.avatar || '',
    signature: item.signature || '',
    remarkName: item.remarkName || '',
    tagName: item.tagName || '',
    permissionScope: Number(item.permissionScope ?? 0),
    starred: Boolean(item.starred),
    blacklisted: Boolean(item.blacklisted),
  }))
}

const applyCachedGroups = (list = []) => {
  groups.value = normalizeCachedArray(list).map((item) => ({
    id: item.id,
    groupId: String(item.groupId || item.id || ''),
    groupName: item.groupName || '',
    groupAvatar: item.groupAvatar || '',
    currentMemberCount: Number(item.currentMemberCount || 0),
    myRole: Number(item.myRole || 0),
  }))
}

const applyCachedSessions = (list = []) => {
  sessions.value = sortSessions(normalizeCachedArray(list).map(normalizeSession))
  const cachedActiveSessionId = String(readCacheValue(HOME_CACHE_KEYS.ACTIVE_SESSION_ID) || '')
  if (!cachedActiveSessionId) {
    return
  }
  const matchedSession = sessions.value.find((item) => item.sessionId === cachedActiveSessionId)
  if (matchedSession) {
    currentSession.value = matchedSession
  }
}

const applyCachedNotifications = (payload = {}) => {
  notifications.value = {
    friendApplies: normalizeCachedArray(payload?.friendApplies),
    groupApplies: normalizeCachedArray(payload?.groupApplies),
  }
}

const normalizeCachedFriendSelection = (value = {}) => ({
  userId: String(value.userId || value.applicantId || ''),
  username: value.username || value.nickname || String(value.userId || value.applicantId || ''),
  nickname: value.nickname || value.username || String(value.userId || value.applicantId || ''),
  originNickname:
    value.originNickname || value.nickname || value.username || String(value.userId || value.applicantId || ''),
  avatar: value.avatar || '',
  signature: value.signature || '',
  remarkName: value.remarkName || '',
  tagName: value.tagName || '',
  permissionScope: Number(value.permissionScope ?? 0),
  starred: Boolean(value.starred),
  blacklisted: Boolean(value.blacklisted),
})

const normalizeCachedGroupSelection = (value = {}) => ({
  id: value.id,
  groupId: String(value.groupId || value.id || ''),
  groupName: value.groupName || '',
  groupAvatar: value.groupAvatar || '',
  currentMemberCount: Number(value.currentMemberCount || 0),
  myRole: Number(value.myRole || 0),
})

const serializeDesktopContactSelection = () => {
  const type = String(desktopContactSelectionType.value || 'requests')
  if (type === 'friend' && desktopContactSelection.value) {
    const snapshot = normalizeCachedFriendSelection(desktopContactSelection.value)
    return {
      type,
      id: String(snapshot.userId || ''),
      snapshot,
    }
  }

  if (type === 'group' && desktopContactSelection.value) {
    const snapshot = normalizeCachedGroupSelection(desktopContactSelection.value)
    return {
      type,
      id: String(snapshot.groupId || snapshot.id || ''),
      snapshot,
    }
  }

  return {
    type: 'requests',
    id: '',
    snapshot: null,
  }
}

const restoreCachedDesktopContactSelection = () => {
  const cachedSelection = readCacheValue(HOME_CACHE_KEYS.ACTIVE_CONTACT_SELECTION)
  if (!cachedSelection || typeof cachedSelection !== 'object') {
    return false
  }

  const type = String(cachedSelection.type || '')
  const snapshot = cachedSelection.snapshot && typeof cachedSelection.snapshot === 'object'
    ? cachedSelection.snapshot
    : null
  const id = String(
    cachedSelection.id
      || snapshot?.userId
      || snapshot?.groupId
      || snapshot?.id
      || '',
  )

  if (type === 'requests') {
    desktopContactSelectionType.value = 'requests'
    desktopContactSelection.value = null
    return true
  }

  if (type === 'friend' && id) {
    const matchedFriend = friends.value.find((item) => String(item.userId || '') === id)
    if (matchedFriend) {
      desktopContactSelectionType.value = 'friend'
      desktopContactSelection.value = matchedFriend
      return true
    }

    if (snapshot) {
      desktopContactSelectionType.value = 'friend'
      desktopContactSelection.value = normalizeCachedFriendSelection(snapshot)
      return true
    }
  }

  if (type === 'group' && id) {
    const matchedGroup = groups.value.find(
      (item) => String(item.groupId || item.id || '') === id,
    )
    if (matchedGroup) {
      desktopContactSelectionType.value = 'group'
      desktopContactSelection.value = matchedGroup
      return true
    }

    if (snapshot) {
      desktopContactSelectionType.value = 'group'
      desktopContactSelection.value = normalizeCachedGroupSelection(snapshot)
      return true
    }
  }

  return false
}

const hydrateLocalDashboardState = () => {
  const cachedUserInfo = readCacheValue(HOME_CACHE_KEYS.USER_INFO)
  if (cachedUserInfo && typeof cachedUserInfo === 'object') {
    userInfo.value = {
      ...(userInfo.value || {}),
      ...cachedUserInfo,
    }
  }

  applyCachedFriends(readCacheValue(HOME_CACHE_KEYS.FRIENDS))
  applyCachedGroups(readCacheValue(HOME_CACHE_KEYS.GROUPS))
  applyCachedSessions(readCacheValue(HOME_CACHE_KEYS.SESSIONS))
  applyCachedNotifications(readCacheValue(HOME_CACHE_KEYS.NOTIFICATIONS))
  restoreCachedDesktopContactSelection()
  if (currentSession.value?.sessionId) {
    showChatInfo.value = Boolean(readChatDetailState(currentSession.value.sessionId)?.showInfo)
  }

  const latestCacheUpdatedAt = getLatestDashboardCacheUpdatedAt()
  if (latestCacheUpdatedAt > 0) {
    dashboardCacheSkipUntil = latestCacheUpdatedAt + DASHBOARD_CACHE_GRACE_MS
  }
}

const resolveClientStatus = (item) => {
  if (item.clientStatus) return item.clientStatus
  return Number(item.status || 0) === 5 ? 'failed' : 'sent'
}

const isSuccessCode = (value) => Number(value) === 200

const normalizeMessage = (item) => ({
  id: String(item.id || ''),
  messageNo: item.messageNo || '',
  sessionId: String(item.sessionId || ''),
  sessionType: Number(item.sessionType || SESSION_TYPE.SINGLE),
  senderId: String(item.senderId || ''),
  receiverId: String(item.receiverId || ''),
  messageType: Number(item.messageType || MESSAGE_TYPE.TEXT),
  content: item.content || '',
  fileUrl: item.fileUrl || '',
  fileName: item.fileName || '',
  fileSize: Number(item.fileSize || 0),
  duration: Number(item.duration || 0),
  sendTime: item.sendTime || item.createdAt || '',
  status: Number(item.status || 0),
  contentReplaced: item.contentReplaced || '',
  senderAvatar: item.senderAvatar || '',
  senderName: item.senderName || '',
  clientStatus: resolveClientStatus(item),
  originalFile: item.originalFile || null,
})

const buildMessageIdentityKey = (message) => {
  if (message?.messageNo) return `messageNo:${message.messageNo}`
  if (message?.id) return `id:${message.id}`
  return `local:${message?.sessionId || ''}:${message?.senderId || ''}:${message?.sendTime || ''}`
}

const mergeMessageSnapshot = (current, incoming) =>
  normalizeMessage({
    ...current,
    ...incoming,
    id: incoming?.id || current?.id,
    messageNo: incoming?.messageNo || current?.messageNo,
    content: incoming?.content ?? current?.content,
    fileUrl: incoming?.fileUrl || current?.fileUrl,
    fileName: incoming?.fileName || current?.fileName,
    fileSize: Number(incoming?.fileSize ?? current?.fileSize ?? 0),
    duration: Number(incoming?.duration ?? current?.duration ?? 0),
    sendTime: incoming?.sendTime || current?.sendTime,
    senderAvatar: incoming?.senderAvatar || current?.senderAvatar,
    senderName: incoming?.senderName || current?.senderName,
    clientStatus: incoming?.clientStatus || current?.clientStatus || 'sent',
    originalFile: incoming?.originalFile || current?.originalFile || null,
  })

const sortMessagesAsc = (list) =>
  [...list].sort((left, right) => {
    const delta = new Date(left.sendTime || 0) - new Date(right.sendTime || 0)
    if (delta !== 0) return delta
    return buildMessageIdentityKey(left).localeCompare(buildMessageIdentityKey(right))
  })

const mergeMessageCollections = (...collections) => {
  const merged = new Map()
  collections.flat().filter(Boolean).forEach((message) => {
    const normalized = normalizeMessage(message)
    const key = buildMessageIdentityKey(normalized)
    const current = merged.get(key)
    merged.set(key, current ? mergeMessageSnapshot(current, normalized) : normalized)
  })
  return sortMessagesAsc([...merged.values()])
}

const canPersistMessage = (message) => {
  if (!message?.sessionId) return false
  if (message.clientStatus === 'pending') return false
  if (String(message.fileUrl || '').startsWith('blob:')) return false
  return true
}

const persistMessageToCache = (message) => {
  if (!canPersistMessage(message)) return
  void ChatStorage.insertMessage({
    ...message,
    originalFile: null,
  }).catch((error) => {
    console.warn('[home] persist message cache failed', error)
  })
}

const isActiveMessageLoad = (sessionId, token) =>
  currentSession.value?.sessionId === sessionId && messageLoadToken === token

const fetchAllRemoteMessages = async (sessionId, token) => {
  const remoteMessages = []
  let nextPage = 1
  let totalPages = 1

  while (nextPage <= totalPages) {
    if (!isActiveMessageLoad(sessionId, token)) {
      return null
    }

    const response = await service.get('/chat/message/list', {
      params: {
        sessionId,
        current: nextPage,
        size: PAGE_SIZE,
      },
    })

    if (!isSuccessCode(response?.code)) {
      throw new Error(response?.msg || response?.message || '加载消息失败')
    }

    const page = response.data || {}
    const records = (page.records || []).map(normalizeMessage)
    remoteMessages.push(...records)
    totalPages = Math.max(nextPage, Number(page.pages || page.current || nextPage))
    nextPage += 1
  }

  return mergeMessageCollections(remoteMessages)
}

const buildPreview = (message) => {
  if (message.contentReplaced) return message.contentReplaced
  if (Number(message.messageType) === MESSAGE_TYPE.IMAGE) return '[图片]'
  if (Number(message.messageType) === MESSAGE_TYPE.VIDEO) return '[视频]'
  if (Number(message.messageType) === MESSAGE_TYPE.AUDIO) {
    return message.duration ? `[语音] ${message.duration}秒` : '[语音]'
  }
  if (Number(message.messageType) === MESSAGE_TYPE.FILE) return `[文件] ${message.fileName || ''}`.trim()
  return message.content || ''
}

const enrichRealtimeMessage = (message) => {
  const next = { ...message }
  if (String(next.senderId) === currentUserId.value) {
    next.senderName = next.senderName || userInfo.value.nickname
    next.senderAvatar = next.senderAvatar || userInfo.value.avatar
  } else if (currentSession.value?.sessionType === SESSION_TYPE.SINGLE) {
    next.senderName = next.senderName || currentSession.value.sessionName
    next.senderAvatar = next.senderAvatar || currentSession.value.sessionAvatar
  } else {
    const member = groupMembers.value.find((item) => String(item.userId) === String(next.senderId))
    if (member) {
      next.senderName = next.senderName || member.nickname
      next.senderAvatar = next.senderAvatar || member.avatar
    }
  }
  return next
}

const upsertGroupMemberCacheFromMessage = (message) => {
  if (Number(message?.sessionType) !== SESSION_TYPE.GROUP) {
    return
  }

  const groupId = String(message.receiverId || currentSession.value?.targetId || '')
  const member = normalizeGroupMember({
    userId: message.senderId,
    nickname: message.senderName,
    avatar: message.senderAvatar,
  })

  if (!groupId || !member.userId || String(member.userId) === currentUserId.value) {
    return
  }

  const cachedMembers = normalizeGroupMembers(readGroupMembersCacheRecord(groupId)?.value)
  const nextMembersMap = new Map(cachedMembers.map((item) => [item.userId, item]))
  const current = nextMembersMap.get(member.userId)
  nextMembersMap.set(member.userId, {
    ...(current || {}),
    ...member,
    nickname: member.nickname || current?.nickname || '',
    avatar: member.avatar || current?.avatar || '',
  })
  const nextMembers = [...nextMembersMap.values()]

  writeGroupMembersCache(groupId, nextMembers)

  if (
    currentSession.value?.sessionType === SESSION_TYPE.GROUP &&
    String(currentSession.value?.targetId || '') === groupId
  ) {
    groupMembers.value = nextMembers
  }
}

const updateViewportMode = () => {
  const width = getViewportWidth()
  isDesktop.value = width > 0 ? width >= 960 : true
}

const throttledUpdateViewport = () => {
  if (resizeTimer) return
  resizeTimer = setTimeout(() => {
    resizeTimer = null
    updateViewportMode()
  }, 200)
}

const composeSingleSessionId = (myId, peerId) => {
  try {
    const left = BigInt(myId || '0')
    const right = BigInt(peerId || '0')
    return left < right ? `${myId}_${peerId}` : `${peerId}_${myId}`
  } catch {
    return `${myId}_${peerId}`
  }
}

const openSettings = () => {
  showSettings.value = true
}

const openEditProfile = () => {
  showSettings.value = false
  uni.navigateTo({ url: '/components/EditProfile' })
}

const openQrModal = () => {
  showQrDialog.value = true
}

const openMobileScanPage = () => {
  showSettings.value = false
  uni.navigateTo({ url: '/pages/scan/mobile-scan' })
}

const closeVoiceDialog = () => {
  voiceDialogVisible.value = false
  voiceSignal.value = null
  voiceTransport.value = 'webrtc'
}

const handleVoiceEnded = () => {
  setTimeout(() => {
    voiceDialogVisible.value = false
    voiceSignal.value = null
    voiceTransport.value = 'webrtc'
  }, 500)
}

const logout = async () => {
  await service.post('/user/logout')
  wsClient.close()
  uni.removeStorageSync('satoken')
  uni.removeStorageSync('userInfo')
  uni.reLaunch({ url: '/pages/index/index' })
}

const loadUserInfo = async () => {
  const response = await service.get('/user/info')
  if (response.code === 200) {
    userInfo.value = response.data || {}
    uni.setStorageSync('userInfo', userInfo.value)
  }
}

const handleProfileUpdated = (payload = {}) => {
  if (!payload || typeof payload !== 'object') {
    return
  }

  const profileUserId = String(payload.id || userInfo.value?.id || '')
  const nextUser = {
    ...(userInfo.value || {}),
    ...payload,
    signature:
      payload.signature ?? payload.extInfo?.signature ?? userInfo.value?.signature ?? '',
    region:
      payload.region ?? payload.extInfo?.region ?? userInfo.value?.region ?? '',
    extInfo: {
      ...(userInfo.value?.extInfo || {}),
      ...(payload.extInfo || {}),
    },
  }

  userInfo.value = nextUser

  if (profileUserId) {
    messages.value = messages.value.map((item) =>
      String(item.senderId || '') === profileUserId
        ? {
            ...item,
            senderName: nextUser.nickname || item.senderName,
            senderAvatar: nextUser.avatar || item.senderAvatar,
          }
        : item,
    )

    groupMembers.value = groupMembers.value.map((item) =>
      String(item.userId || '') === profileUserId
        ? {
            ...item,
            nickname: nextUser.nickname || item.nickname,
            avatar: nextUser.avatar || item.avatar,
          }
        : item,
    )
  }
}

const loadFriends = async () => {
  const response = await service.get('/friend/list')
  if (response.code !== 200) return
  friends.value = (response.data || []).map((item) => ({
    userId: String(item.applicantId || ''),
    username: item.applicantUsername || item.applicantNickname || String(item.applicantId || ''),
    nickname: item.remarkName || item.applicantNickname || item.applicantUsername || String(item.applicantId || ''),
    originNickname: item.applicantNickname || item.applicantUsername || String(item.applicantId || ''),
    avatar: item.applicantAvatar || '',
    signature: '',
    remarkName: item.remarkName || '',
    tagName: item.tagName || '',
    permissionScope: Number(item.permissionScope ?? 0),
    starred: Boolean(item.starred),
    blacklisted: Boolean(item.blacklisted),
  }))
}

const loadGroups = async () => {
  const response = await service.get('/group/list')
  if (response.code !== 200) return
  groups.value = (response.data || []).map((item) => ({
    id: item.id,
    groupId: String(item.id || ''),
    groupName: item.groupName || '',
    groupAvatar: item.groupAvatar || '',
    currentMemberCount: Number(item.currentMemberCount || 0),
    myRole: Number(item.myRole || 0),
  }))
}

const previewDesktopFriend = (friend) => {
  if (!friend) return
  desktopContactSelectionType.value = 'friend'
  desktopContactSelection.value = friend
}

const previewDesktopGroup = (group) => {
  if (!group) return
  desktopContactSelectionType.value = 'group'
  desktopContactSelection.value = group
}

const previewDesktopRequests = () => {
  desktopContactSelectionType.value = 'requests'
  desktopContactSelection.value = null
}

const ensureDesktopContactSelection = () => {
  if (restoreCachedDesktopContactSelection()) {
    return
  }

  if (desktopContactSelectionType.value === 'friend' && desktopContactSelection.value) {
    const matchedFriend = friends.value.find(
      (item) => String(item.userId || '') === String(desktopContactSelection.value.userId || ''),
    )
    if (matchedFriend) {
      desktopContactSelection.value = matchedFriend
      return
    }
  }

  if (desktopContactSelectionType.value === 'group' && desktopContactSelection.value) {
    const matchedGroup = groups.value.find(
      (item) =>
        String(item.groupId || item.id || '') ===
        String(desktopContactSelection.value.groupId || desktopContactSelection.value.id || ''),
    )
    if (matchedGroup) {
      desktopContactSelection.value = matchedGroup
      return
    }
  }

  if (friends.value.length) {
    previewDesktopFriend(friends.value[0])
    return
  }

  if (groups.value.length) {
    previewDesktopGroup(groups.value[0])
    return
  }

  previewDesktopRequests()
}

const loadSessions = async (reset = true) => {
  if (reset && sessionsLoading.value) return
  if (!reset && (!sessionsHasMore.value || !sessionsCursor.value || sessionsLoading.value)) return
  if (reset) sessionsLoading.value = true
  const response = await service
    .get('/session/list/page', {
      params: {
        limit: PAGE_SIZE,
        cursor: reset ? undefined : sessionsCursor.value || undefined,
      },
    })
    .finally(() => {
      sessionsLoading.value = false
    })

  if (response.code !== 200) return
  const page = response.data || {}
  const loaded = (page.items || []).map(normalizeSession)
  sessions.value = sortSessions(reset ? loaded : [...sessions.value, ...loaded])
  sessionsCursor.value = page.nextCursor || ''
  sessionsHasMore.value = Boolean(page.hasMore)

  if (currentSession.value) {
    const matched = sessions.value.find((item) => item.sessionId === currentSession.value.sessionId)
    if (matched) {
      currentSession.value = matched
    }
  }
}

const loadGroupMembers = async (groupId) => {
  const normalizedGroupId = String(groupId || '')
  if (!normalizedGroupId) {
    groupMembers.value = []
    return
  }

  const cachedRecord = readGroupMembersCacheRecord(normalizedGroupId)
  const cachedMembers = normalizeGroupMembers(cachedRecord?.value)
  if (cachedMembers.length) {
    groupMembers.value = cachedMembers
  }

  if (
    cachedRecord?.updatedAt &&
    Date.now() - Number(cachedRecord.updatedAt) < GROUP_MEMBERS_CACHE_GRACE_MS
  ) {
    return
  }

  const response = await service.get(`/group/member/list/${normalizedGroupId}`)
  const nextMembers = response.code === 200 ? normalizeGroupMembers(response.data || []) : cachedMembers
  groupMembers.value = nextMembers
  writeGroupMembersCache(normalizedGroupId, nextMembers)
}

const loadMessages = async (reset = true) => {
  if (!currentSession.value?.sessionId) return

  const sessionId = currentSession.value.sessionId
  const requestToken = ++messageLoadToken

  if (reset) {
    messagesLoading.value = true
    currentMessagePage.value = 1
    messagesHasMore.value = false
  } else {
    messagesLoadingMore.value = true
  }

  try {
    const cachedMessages = mergeMessageCollections((await ChatStorage.queryMessages(sessionId)).map(normalizeMessage))
    if (reset && isActiveMessageLoad(sessionId, requestToken)) {
      messages.value = cachedMessages
    }

    const remoteMessages = await fetchAllRemoteMessages(sessionId, requestToken)
    if (!remoteMessages || !isActiveMessageLoad(sessionId, requestToken)) {
      return
    }

    const volatileMessages = messages.value.filter(
      (item) => item.clientStatus === 'pending' || item.clientStatus === 'failed',
    )
    const nextMessages = mergeMessageCollections(remoteMessages, volatileMessages)

    await ChatStorage.replaceSessionMessages(
      sessionId,
      nextMessages.filter(canPersistMessage),
    )

    if (!isActiveMessageLoad(sessionId, requestToken)) {
      return
    }

    messages.value = nextMessages
    currentMessagePage.value = 1
    messagesHasMore.value = false
  } catch (error) {
    console.warn('[home] loadMessages failed', error)
  } finally {
    if (messageLoadToken === requestToken) {
      messagesLoading.value = false
      messagesLoadingMore.value = false
    }
  }
}

const resolveLatestReadMessageId = (sessionId) => {
  if (!sessionId || currentSession.value?.sessionId !== sessionId || !messages.value.length) {
    return null
  }

  for (let index = messages.value.length - 1; index >= 0; index -= 1) {
    const candidateId = Number(messages.value[index]?.id || 0)
    if (Number.isFinite(candidateId) && candidateId > 0) {
      return candidateId
    }
  }

  return null
}

const markSessionRead = async (sessionId, lastReadMessageId = null) => {
  if (!sessionId) return
  sessions.value = sessions.value.map((item) =>
    item.sessionId === sessionId ? { ...item, unreadCount: 0 } : item,
  )
  if (currentSession.value?.sessionId === sessionId) {
    currentSession.value = {
      ...currentSession.value,
      unreadCount: 0,
    }
  }

  try {
    const resolvedLastReadMessageId = Number(lastReadMessageId || resolveLatestReadMessageId(sessionId) || 0)
    if (resolvedLastReadMessageId > 0) {
      await service.post('/read-sync/mark-read', {
        sessionId,
        lastReadMessageId: resolvedLastReadMessageId,
        deviceType: currentDeviceType,
        deviceId: currentDeviceId,
      })
      return
    }

    await service.post(
      `/read-sync/mark-session-read/${sessionId}`,
      null,
      {
        params: {
          deviceType: currentDeviceType,
          deviceId: currentDeviceId,
        },
      },
    )
  } catch (error) {
    console.warn('[home] mark session read failed', error)
  }
}

const selectSession = async (session) => {
  currentSession.value = session
  showChatInfo.value = Boolean(readChatDetailState(session.sessionId)?.showInfo)
  desktopTab.value = 'chat'
  mobileTab.value = 'chat'
  mobileChatOpen.value = true

  const tasks = [loadMessages(true)]
  if (Number(session.sessionType) === SESSION_TYPE.GROUP) {
    tasks.push(loadGroupMembers(session.targetId))
  } else {
    groupMembers.value = []
  }

  await Promise.allSettled(tasks)
  await markSessionRead(session.sessionId, resolveLatestReadMessageId(session.sessionId))
}

const findMessageIndex = (message) =>
  messages.value.findIndex(
    (item) =>
      (item.id && message.id && String(item.id) === String(message.id)) ||
      (item.messageNo && message.messageNo && item.messageNo === message.messageNo),
  )

const createLocalFileUrl = (file) => {
  if (!file || typeof URL === 'undefined' || typeof URL.createObjectURL !== 'function') {
    return ''
  }

  try {
    return URL.createObjectURL(file)
  } catch {
    return ''
  }
}

const getFileNameFromPath = (value = '') => {
  const normalized = String(value || '').split('?')[0]
  const segments = normalized.split('/').filter(Boolean)
  return segments[segments.length - 1] || `file_${Date.now()}`
}

const createUploadAsset = ({ source, name, size = 0, previewUrl = '' }) => ({
  uploadSource: source,
  name: name || getFileNameFromPath(typeof source === 'string' ? source : ''),
  size: Number(size || 0),
  previewUrl: previewUrl || (typeof source === 'string' ? source : ''),
})

const getUploadSource = (file) => file?.uploadSource || file || null
const getUploadName = (file) => file?.name || getFileNameFromPath(file?.uploadSource || '')
const getUploadSize = (file) => Number(file?.size || 0)

const revokeLocalFileUrl = (value) => {
  if (!value || typeof URL === 'undefined' || typeof URL.revokeObjectURL !== 'function') {
    return
  }

  if (!String(value).startsWith('blob:')) {
    return
  }

  URL.revokeObjectURL(value)
}

const upsertMessage = (message) => {
  const normalized = enrichRealtimeMessage(normalizeMessage(message))
  const index = findMessageIndex(normalized)

  if (index === -1) {
    messages.value = mergeMessageCollections(messages.value, [normalized])
    persistMessageToCache(normalized)
    return normalized
  }

  const current = messages.value[index]
  const merged = mergeMessageSnapshot(current, normalized)

  messages.value = mergeMessageCollections(messages.value.map((item, itemIndex) => (itemIndex === index ? merged : item)))
  persistMessageToCache(merged)
  return merged
}

const appendMessage = (message) => {
  upsertMessage(message)
}

const updateSessionFromMessage = (message, incoming = false) => {
  const preview = buildPreview(message)
  const found = sessions.value.find((item) => item.sessionId === message.sessionId)
  if (!found) {
    const fallbackTargetId =
      Number(message.sessionType) === SESSION_TYPE.GROUP
        ? String(message.receiverId || '')
        : String(message.senderId) === currentUserId.value
          ? String(message.receiverId || '')
          : String(message.senderId || '')

    const fallbackSession = normalizeSession({
      sessionId: message.sessionId,
      sessionType: message.sessionType,
      targetId: fallbackTargetId,
      sessionName:
        currentSession.value?.sessionId === message.sessionId
          ? currentSession.value.sessionName
          : message.senderName || (Number(message.sessionType) === SESSION_TYPE.GROUP ? '群聊' : '会话'),
      sessionAvatar:
        currentSession.value?.sessionId === message.sessionId
          ? currentSession.value.sessionAvatar
          : message.senderAvatar || '',
      lastMessageContent: preview,
      lastMessageTime: message.sendTime || new Date().toISOString(),
      unreadCount:
        incoming && String(message.senderId) !== currentUserId.value && currentSession.value?.sessionId !== message.sessionId
          ? 1
          : 0,
      isTop: 0,
      isMute: 0,
    })

    sessions.value = sortSessions([fallbackSession, ...sessions.value])

    if (currentSession.value?.sessionId === message.sessionId) {
      currentSession.value = fallbackSession
    }
    return
  }

  const unreadCount =
    incoming &&
    (!currentSession.value || currentSession.value.sessionId !== message.sessionId) &&
    String(message.senderId) !== currentUserId.value
      ? Number(found.unreadCount || 0) + 1
      : 0

  sessions.value = sortSessions(
    sessions.value.map((item) =>
      item.sessionId === message.sessionId
        ? {
            ...item,
            lastMessageContent: preview,
            lastMessageTime: message.sendTime || new Date().toISOString(),
            unreadCount,
          }
        : item,
    ),
  )
}

const postMessage = async ({ content, messageType, file, duration = 0, retryMessage = null, atUserIds = [], isAtAll = 0, quoteMessageId = null }) => {
  if (!currentSession.value) return

  const activeSession = {
    sessionId: currentSession.value.sessionId,
    sessionType: currentSession.value.sessionType,
    targetId: currentSession.value.targetId,
  }

  const originalFile = file || retryMessage?.originalFile || null
  const uploadSource = getUploadSource(originalFile)
  const isTextMessage = Number(messageType) === MESSAGE_TYPE.TEXT
  const payloadContent = isTextMessage ? content : ''
  const resolvedDuration =
    Number(messageType) === MESSAGE_TYPE.AUDIO
      ? Math.min(300, Math.max(1, Number(duration || retryMessage?.duration || 0)))
      : 0
  const previewUrl =
    retryMessage?.fileUrl ||
    (Number(messageType) === MESSAGE_TYPE.IMAGE
      ? typeof uploadSource === 'string'
        ? uploadSource
        : createLocalFileUrl(uploadSource)
      : '')
  const messageNo = retryMessage?.messageNo || `web_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
  const optimisticMessage = upsertMessage({
    id: retryMessage?.id || `local_${messageNo}`,
    messageNo,
    sessionId: activeSession.sessionId,
    sessionType: activeSession.sessionType,
    senderId: currentUserId.value,
    receiverId: activeSession.targetId,
    messageType,
    content,
    fileUrl: previewUrl,
    fileName: getUploadName(originalFile) || retryMessage?.fileName || '',
    fileSize: Number(getUploadSize(originalFile) || retryMessage?.fileSize || 0),
    duration: resolvedDuration,
    sendTime: new Date().toISOString(),
    status: 0,
    clientStatus: 'pending',
    originalFile,
  })
  updateSessionFromMessage(optimisticMessage, false)

  pendingSendCount.value += 1
  try {
    const payload = {
      messageNo,
      sessionId: activeSession.sessionId,
      sessionType: activeSession.sessionType,
      receiverId: activeSession.targetId,
      messageType,
      content: payloadContent,
      fileName: getUploadName(originalFile) || retryMessage?.fileName,
      fileSize: getUploadSize(originalFile) || retryMessage?.fileSize,
      duration: resolvedDuration || undefined,
      // @提及和引用回复
      atUserIds: atUserIds.length > 0 ? atUserIds : undefined,
      isAtAll: isAtAll || undefined,
      quoteMessageId: quoteMessageId || undefined,
    }
    const response = uploadSource
      ? await uploadFileWithJSON('/chat/message/send', uploadSource, payload, 'sendDTO', {
          showProgress: false,
          fileFieldName: 'files',
        })
      : await service.post('/chat/message/send', payload)
    if (!isSuccessCode(response?.code)) {
      throw new Error(response?.msg || response?.message || '消息发送失败')
    }

    const responseData = response?.data || {}
    const confirmedMessage = normalizeMessage({
      ...optimisticMessage,
      ...responseData,
      id: responseData.id || optimisticMessage.id,
      messageNo: responseData.messageNo || messageNo,
      sessionId: responseData.sessionId || activeSession.sessionId,
      sessionType: responseData.sessionType || activeSession.sessionType,
      senderId: responseData.senderId || optimisticMessage.senderId,
      receiverId: responseData.receiverId || activeSession.targetId,
      messageType: responseData.messageType || messageType,
      content: responseData.content ?? optimisticMessage.content,
      fileUrl: responseData.fileUrl || optimisticMessage.fileUrl,
      fileName: responseData.fileName || optimisticMessage.fileName,
      fileSize: Number(responseData.fileSize ?? optimisticMessage.fileSize ?? 0),
      duration: Number(responseData.duration ?? resolvedDuration ?? optimisticMessage.duration ?? 0),
      sendTime: responseData.sendTime || optimisticMessage.sendTime,
      status: Number(responseData.status ?? 1),
      senderAvatar: responseData.senderAvatar || optimisticMessage.senderAvatar,
      senderName: responseData.senderName || optimisticMessage.senderName,
      clientStatus: 'sent',
      originalFile: null,
    })

    try {
      if (currentSession.value?.sessionId === activeSession.sessionId) {
        upsertMessage(confirmedMessage)
      }
      if (optimisticMessage.fileUrl && optimisticMessage.fileUrl !== confirmedMessage.fileUrl) {
        revokeLocalFileUrl(optimisticMessage.fileUrl)
      }
      updateSessionFromMessage(confirmedMessage, false)
    } catch (reconcileError) {
      console.error('[home] postMessage reconcile failed after server success', {
        error: reconcileError,
        response,
        optimisticMessage,
        confirmedMessage,
      })
      if (currentSession.value?.sessionId === activeSession.sessionId) {
        loadMessages(true).catch((loadError) => {
          console.warn('[home] failed to resync messages after successful send', loadError)
        })
      }
    }
  } catch (error) {
    console.warn('[home] postMessage request failed', error)
    const failedMessage = normalizeMessage({
      ...optimisticMessage,
      status: 5,
      clientStatus: 'failed',
    })
    if (currentSession.value?.sessionId === activeSession.sessionId) {
      upsertMessage(failedMessage)
    } else if (optimisticMessage.fileUrl) {
      revokeLocalFileUrl(optimisticMessage.fileUrl)
    }
    updateSessionFromMessage(failedMessage, false)
  } finally {
    pendingSendCount.value = Math.max(0, pendingSendCount.value - 1)
  }
}

const sendTextMessage = async (content, messageData = {}) => {
  await postMessage({
    content,
    messageType: MESSAGE_TYPE.TEXT,
    atUserIds: messageData.atUserIds || [],
    isAtAll: messageData.isAtAll || 0,
    quoteMessageId: messageData.quoteMessageId || null,
  })
}

const sendAudioMessage = async ({ file, duration = 0 } = {}) => {
  if (!file) return
  await postMessage({
    content: '[语音]',
    messageType: MESSAGE_TYPE.AUDIO,
    file,
    duration,
  })
}

// 发送表情包消息
const sendStickerMessage = async (stickerId, stickerUrl) => {
  await postMessage({
    content: '[表情]',
    messageType: MESSAGE_TYPE.EMOJI,
    fileUrl: stickerUrl,
    stickerId,
  })
}

const retryFailedMessage = async (message) => {
  if (!message || message.clientStatus !== 'failed') return

  const messageType = Number(message.messageType || MESSAGE_TYPE.TEXT)
  if (messageType !== MESSAGE_TYPE.TEXT && !message.originalFile) {
    uni.showToast({
      title: '当前附件已经失效，无法重发',
      icon: 'none',
    })
    return
  }

  await postMessage({
    content: message.content,
    messageType,
    file: message.originalFile || null,
    retryMessage: message,
  })
}

// 处理转发消息
const handleForwardMessage = async (messageIds) => {
  // 显示会话选择器，让用户选择转发目标
  uni.showModal({
    title: '转发消息',
    placeholderText: '请选择转发到的会话',
    editable: false,
    success: async (res) => {
      if (res.confirm) {
        // 用户选择了目标会话
        // 这里需要实现会话选择器功能
        uni.showToast({ title: '请选择转发目标', icon: 'none' })
      }
    }
  })
}

// 处理消息撤回
const handleMessageRecalled = (messageId) => {
  // 更新本地消息状态
  const msg = messages.value.find(m => m.id === messageId)
  if (msg) {
    msg.status = 3
    msg.isRecalled = 1
    msg.contentReplaced = '[消息已撤回]'
  }
}

// 处理消息删除
const handleMessageDeleted = (messageId) => {
  // 从本地消息列表中移除
  const index = messages.value.findIndex(m => m.id === messageId)
  if (index > -1) {
    messages.value.splice(index, 1)
  }
}

const pickFileOnWeb = (accept = '*/*') =>
  new Promise((resolve) => {
    if (!supportsBrowserDom()) {
      resolve(null)
      return
    }

    const input = document.createElement('input')
    input.type = 'file'
    input.accept = accept
    input.onchange = (event) => {
      const file = event.target?.files?.[0]
      resolve(
        file
          ? createUploadAsset({
              source: file,
              name: file.name,
              size: file.size,
            })
          : null,
      )
      input.remove()
    }
    input.click()
  })

const pickImageOnDevice = () =>
  new Promise((resolve) => {
    uni.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      success: (result) => {
        const tempFile = result.tempFiles?.[0]
        const path = tempFile?.path || result.tempFilePaths?.[0]
        resolve(
          path
            ? createUploadAsset({
                source: path,
                name: tempFile?.name || getFileNameFromPath(path),
                size: tempFile?.size || 0,
                previewUrl: path,
              })
            : null,
        )
      },
      fail: () => resolve(null),
    })
  })

const pickGenericFileOnDevice = () =>
  new Promise((resolve) => {
    if (isAppPlusRuntime()) {
      resolve({ unsupportedInApp: true })
      return
    }

    if (typeof uni.chooseFile !== 'function') {
      resolve(null)
      return
    }

    uni.chooseFile({
      count: 1,
      success: (result) => {
        const tempFile = result.tempFiles?.[0]
        const path = tempFile?.path || tempFile?.tempFilePath || result.tempFilePaths?.[0]
        resolve(
          path
            ? createUploadAsset({
                source: path,
                name: tempFile?.name || getFileNameFromPath(path),
                size: tempFile?.size || 0,
              })
            : null,
        )
      },
      fail: () => resolve(null),
    })
  })

const pickImage = async () => {
  if (isAppPlusRuntime()) {
    const permissionResult = await ensureAnyAppPermissionAccess(
      [APP_PERMISSION_SCOPE.CAMERA, APP_PERMISSION_SCOPE.ALBUM],
      {
        title: '需要相机或相册权限',
        content: '发送图片前，请至少开启相机或相册中的一个权限。',
      },
    )
    if (!permissionResult.ok) {
      return
    }
  }

  const file = supportsBrowserDom() ? await pickFileOnWeb('image/*') : await pickImageOnDevice()
  if (!file) {
    uni.showToast({
      title: '当前环境暂不支持选择图片',
      icon: 'none',
    })
    return
  }

  await postMessage({
    content: '[图片]',
    messageType: MESSAGE_TYPE.IMAGE,
    file,
  })
}

const pickFile = async () => {
  const file = supportsBrowserDom() ? await pickFileOnWeb('*/*') : await pickGenericFileOnDevice()
  if (file?.unsupportedInApp) {
    uni.showToast({
      title: 'UniApp App 端不支持通用文件选择，请改用图片发送或百度网盘导入',
      icon: 'none',
    })
    return
  }

  if (!file) {
    uni.showToast({
      title: '当前环境暂不支持选择文件',
      icon: 'none',
    })
    return
  }

  await postMessage({
    content: `[文件] ${getUploadName(file)}`,
    messageType: MESSAGE_TYPE.FILE,
    file,
  })
}

const toggleTop = async () => {
  if (!currentSession.value) return
  const next = currentSession.value.isTop === 1 ? 0 : 1
  const response = await service.put(`/session/top/${currentSession.value.sessionId}`, null, {
    params: { isTop: next },
  })
  if (response.code === 200) {
    currentSession.value = { ...currentSession.value, isTop: next }
    sessions.value = sortSessions(
      sessions.value.map((item) =>
        item.sessionId === currentSession.value.sessionId ? { ...item, isTop: next } : item,
      ),
    )
  }
}

const toggleMute = async () => {
  if (!currentSession.value) return
  const next = currentSession.value.isMute === 1 ? 0 : 1
  const response = await service.put(`/session/mute/${currentSession.value.sessionId}`, null, {
    params: { isMute: next },
  })
  if (response.code === 200) {
    currentSession.value = { ...currentSession.value, isMute: next }
    sessions.value = sessions.value.map((item) =>
      item.sessionId === currentSession.value.sessionId ? { ...item, isMute: next } : item,
    )
  }
}

const clearLocalMessages = () => {
  messageLoadToken += 1
  if (currentSession.value?.sessionId) {
    void ChatStorage.deleteSessionMessages(currentSession.value.sessionId).catch((error) => {
      console.warn('[home] clear session cache failed', error)
    })
  }
  messages.value = []
  messagesLoading.value = false
  messagesLoadingMore.value = false
  messagesHasMore.value = false
  uni.showToast({
    title: '已清空当前设备缓存记录',
    icon: 'none',
  })
}

const searchUser = async (keyword) => {
  if (!keyword) return
  searchingUser.value = true
  try {
    const response = await service.get('/user/search', { params: { keyword } })
    searchedUser.value = response.code === 200 ? response.data || null : null
  } finally {
    searchingUser.value = false
  }
}

const handleScannedContactCard = async (payload = {}) => {
  const keyword = String(payload.username || '').trim()
  desktopTab.value = 'contacts'
  mobileTab.value = 'contacts'
  mobileChatOpen.value = false
  showSettings.value = false

  if (!keyword) {
    uni.showToast({
      title: '名片中缺少可搜索的账号信息',
      icon: 'none',
    })
    return
  }

  await searchUser(keyword)
  uni.showToast({
    title: payload.nickname ? `已添加${payload.nickname}` : '已添加联系人名片',
    icon: 'none',
  })
}

const applyFriend = async (payload) => {
  const response = await service.post('/friend/apply', payload)
  if (response.code === 200) {
    uni.showToast({
      title: '好友申请已发送',
      icon: 'none',
    })
  }
}

const searchGroups = async (keyword) => {
  if (!keyword) return
  searchingGroups.value = true
  try {
    const response = await service.get('/group/search/page', {
      params: { keyword, current: 1, size: 20 },
    })
    groupSearchResults.value = response.code === 200 ? response.data?.records || [] : []
  } finally {
    searchingGroups.value = false
  }
}

const applyGroup = async (group) => {
  const response = await service.post('/group/apply', { groupId: group.id, remark: '' })
  if (response.code === 200) {
    groupSearchResults.value = groupSearchResults.value.map((item) =>
      item.id === group.id ? { ...item, applyStatus: 'pending' } : item,
    )
    uni.showToast({
      title: '入群申请已发送',
      icon: 'none',
    })
  }
}

const createGroup = async (payload) => {
  creatingGroup.value = true
  try {
    const response = await service.post('/group/create', payload)
    if (response.code === 200 && response.data) {
      const session = {
        sessionId: `group_${response.data.id}`,
        sessionType: SESSION_TYPE.GROUP,
        targetId: String(response.data.id),
        sessionName: response.data.groupName,
        sessionAvatar: response.data.groupAvatar || '',
        lastMessageContent: '',
        lastMessageTime: new Date().toISOString(),
        unreadCount: 0,
        isTop: 0,
        isMute: 0,
      }
      sessions.value = sortSessions([
        session,
        ...sessions.value.filter((item) => item.sessionId !== session.sessionId),
      ])
      await Promise.allSettled([loadGroups(), selectSession(session)])
      uni.showToast({
        title: '群聊已创建',
        icon: 'none',
      })
    }
  } finally {
    creatingGroup.value = false
  }
}

const openFriendChat = async (friend) => {
  const sessionId = composeSingleSessionId(currentUserId.value, String(friend.userId))
  const found = sessions.value.find((item) => item.sessionId === sessionId)
  const nextSession =
    found ||
    {
      sessionId,
      sessionType: SESSION_TYPE.SINGLE,
      targetId: String(friend.userId),
      sessionName: friend.nickname || friend.username || friend.userId,
      sessionAvatar: friend.avatar || '',
      lastMessageContent: '',
      lastMessageTime: new Date().toISOString(),
      unreadCount: 0,
      isTop: 0,
      isMute: 0,
    }

  if (!found) {
    sessions.value = sortSessions([nextSession, ...sessions.value])
  }
  await selectSession(nextSession)
}

const openGroupChat = async (group) => {
  const sessionId = `group_${group.groupId || group.id}`
  const found = sessions.value.find((item) => item.sessionId === sessionId)
  const nextSession =
    found ||
    {
      sessionId,
      sessionType: SESSION_TYPE.GROUP,
      targetId: String(group.groupId || group.id),
      sessionName: group.groupName,
      sessionAvatar: group.groupAvatar || '',
      lastMessageContent: '',
      lastMessageTime: new Date().toISOString(),
      unreadCount: 0,
      isTop: 0,
      isMute: 0,
    }

  if (!found) {
    sessions.value = sortSessions([nextSession, ...sessions.value])
  }
  await selectSession(nextSession)
}

const openSelectedDesktopContactChat = async () => {
  if (desktopContactSelectionType.value === 'group' && desktopContactSelection.value) {
    await openGroupChat(desktopContactSelection.value)
    return
  }

  if (desktopContactSelectionType.value === 'friend' && desktopContactSelection.value) {
    await openFriendChat(desktopContactSelection.value)
    return
  }

  uni.showToast({
    title: '先在左侧选择联系人',
    icon: 'none',
  })
}

const startDesktopContactCall = async (mode) => {
  if (desktopContactSelectionType.value !== 'friend' || !desktopContactSelection.value) {
    uni.showToast({
      title: '当前仅支持联系人发起音频通话',
      icon: 'none',
    })
    return
  }

  await openFriendChat(desktopContactSelection.value)
  startVoiceCall({ mode })
}

const startDesktopAudioCall = async () => {
  await startDesktopContactCall('audio')
}

const startDesktopVideoCall = async () => {
  await startDesktopContactCall('video')
}

const updateDesktopFriendRelation = async (payload) => {
  const response = await service.put('/friend/relation', payload)
  if (response.code !== 200) {
    await loadFriends()
    ensureDesktopContactSelection()
    uni.showToast({
      title: response.msg || '好友资料更新失败',
      icon: 'none',
    })
    return
  }

  await loadFriends()
  ensureDesktopContactSelection()
  uni.showToast({
    title: response.msg || '好友资料已更新',
    icon: 'none',
  })
}

const updateDesktopFriendBlacklist = async (payload) => {
  const response = await service.put('/friend/blacklist', payload)
  if (response.code !== 200) {
    await loadFriends()
    ensureDesktopContactSelection()
    uni.showToast({
      title: response.msg || '黑名单更新失败',
      icon: 'none',
    })
    return
  }

  await loadFriends()
  ensureDesktopContactSelection()
  uni.showToast({
    title: response.msg || '黑名单状态已更新',
    icon: 'none',
  })
}

const deleteDesktopFriend = async (friendUserId) => {
  const response = await service.delete(`/friend/${friendUserId}`)
  if (response.code !== 200) {
    await loadFriends()
    ensureDesktopContactSelection()
    uni.showToast({
      title: response.msg || '删除联系人失败',
      icon: 'none',
    })
    return
  }

  await loadFriends()
  ensureDesktopContactSelection()
  uni.showToast({
    title: response.msg || '联系人已移除',
    icon: 'none',
  })
}

const approveFriend = async (item) => {
  await handleFriendApply(Number(item.id), 1)
}

const rejectFriend = async (item) => {
  await handleFriendApply(Number(item.id), 2)
}

const approveGroup = async (item) => {
  await handleGroupApply(Number(item.id), 1)
}

const rejectGroup = async (item) => {
  await handleGroupApply(Number(item.id), 2)
}

const closeMobileChat = () => {
  mobileChatOpen.value = false
  showChatInfo.value = false
}

const loadMoreMessages = async () => {
  if (messagesLoading.value || messagesLoadingMore.value) return
  await loadMessages(true)
}

const refreshDashboard = async ({ force = false } = {}) => {
  if (dashboardRefreshPromise) {
    return dashboardRefreshPromise
  }

  const now = Date.now()
  if (!force && dashboardCacheSkipUntil && now < dashboardCacheSkipUntil) {
    hasFinishedInitialDashboardLoad = true
    return Promise.resolve()
  }

  if (!force && lastDashboardRefreshAt && now - lastDashboardRefreshAt < DASHBOARD_REFRESH_COOLDOWN_MS) {
    return Promise.resolve()
  }

  dashboardRefreshPromise = Promise.allSettled([
    loadUserInfo(),
    loadFriends(),
    loadGroups(),
    loadSessions(true),
    loadNotifications(),
  ])
    .then((results) => {
      const blockingError = results.find(
        (result) => result.status === 'rejected' && !isAbortLikeRequestError(result.reason),
      )
      if (blockingError) {
        console.warn('[home] refreshDashboard encountered a request error', blockingError.reason)
      }
      lastDashboardRefreshAt = Date.now()
      dashboardCacheSkipUntil = lastDashboardRefreshAt + DASHBOARD_CACHE_GRACE_MS
      hasFinishedInitialDashboardLoad = true
    })
    .finally(() => {
      dashboardRefreshPromise = null
    })

  return dashboardRefreshPromise
}

const makeSignalId = () => `signal_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
const resolveCallTransport = ({ mode = 'audio' } = {}) => {
  if (isAppPlusRuntime() && mode === 'video' && hasRtcStreamConfig()) {
    return 'stream'
  }
  if (isAppPlusRuntime() && hasConfiguredNativeWebRtcPlugin()) {
    return 'webrtc'
  }
  if (isAppPlusRuntime() && hasRtcStreamConfig()) {
    return 'stream'
  }
  return 'webrtc'
}

const startVoiceCall = ({ mode = 'audio' } = {}) => {
  if (!currentSession.value || Number(currentSession.value.sessionType) !== SESSION_TYPE.SINGLE) {
    uni.showToast({
      title: '当前仅支持单聊音视频通话',
      icon: 'none',
    })
    return
  }

  if (isAppPlusRuntime() && mode === 'video' && !hasRtcStreamConfig()) {
    uni.showToast({
      title: 'App 视频通话需要先配置推流和拉流地址',
      icon: 'none',
    })
    return
  }

  if (isAppPlusRuntime() && mode === 'audio' && !hasConfiguredNativeWebRtcPlugin() && !hasRtcStreamConfig()) {
    uni.showToast({
      title: '当前 App 未配置可用的语音通话能力',
      icon: 'none',
    })
    return
  }

  voiceIncoming.value = false
  voiceTransport.value = resolveCallTransport({ mode })
  voicePeer.value = {
    id: String(currentSession.value.targetId || ''),
    nickname: currentSession.value.sessionName,
    avatar: currentSession.value.sessionAvatar,
  }
  voiceMode.value = mode
  voiceSessionId.value = currentSession.value.sessionId
  voiceSignal.value = null
  voiceDialogVisible.value = true
}

const forwardVoiceSignal = (payload) => {
  const sessionId = String(payload.sessionId || '')
  const signalType = String(payload.callType || '')

  if (signalType === CALL_TYPE.CALL) {
    if (voiceDialogVisible.value && sessionId && voiceSessionId.value && sessionId !== voiceSessionId.value) {
      uni.showToast({
        title: '已有通话进行中',
        icon: 'none',
      })
      return
    }

    voiceIncoming.value = true
    voicePeer.value = {
      id: String(payload.fromId || ''),
      nickname: payload.fromNickname || '来电',
      avatar: payload.fromAvatar || '',
    }
    voiceMode.value = payload.mode || 'audio'
    voiceTransport.value = payload.transport || 'webrtc'
    voiceSessionId.value =
      sessionId || composeSingleSessionId(currentUserId.value, String(payload.fromId || ''))
    voiceDialogVisible.value = true
  } else if (!voiceDialogVisible.value) {
    return
  }

  if (sessionId && voiceSessionId.value && sessionId !== voiceSessionId.value) {
    return
  }

  voiceSignal.value = {
    ...payload,
    _signalId: makeSignalId(),
  }
}

const applyReadSyncEvent = (payload = {}) => {
  const sessionId = String(payload.sessionId || '')
  if (!sessionId) {
    return
  }

  sessions.value = sessions.value.map((item) =>
    item.sessionId === sessionId ? { ...item, unreadCount: 0 } : item,
  )

  if (currentSession.value?.sessionId === sessionId) {
    currentSession.value = {
      ...currentSession.value,
      unreadCount: 0,
    }
  }
}

const handleSocketMessage = async (payload) => {
  if (!payload || payload.type === 'error' || payload.type === 'pong') return
  if (payload.type === 'read_sync') {
    applyReadSyncEvent(payload)
    return
  }

  const voicePayload = payload.callType ? payload : payload.data?.callType ? payload.data : null
  if (voicePayload) {
    forwardVoiceSignal(voicePayload)
    return
  }

  const raw = payload.sessionId ? payload : payload.data?.sessionId ? payload.data : null
  if (!raw) return

  const message = enrichRealtimeMessage(normalizeMessage(raw))
  upsertGroupMemberCacheFromMessage(message)
  updateSessionFromMessage(message, true)

  // 如果不是当前会话，显示通知并更新角标
  const isOtherSession = currentSession.value?.sessionId !== message.sessionId
  const isNotSelf = String(message.senderId) !== currentUserId.value

  if (isOtherSession && isNotSelf) {
    persistMessageToCache(message)

    // 仅在页面不可见时显示通知
    if (!wsClient.isPageVisible) {
      const notificationTitle = message.senderName || '新消息'
      const notificationBody = buildPreview(message)
      const notificationIcon = message.senderAvatar || ''

      // 显示通知
      await notificationManager.requestPermission()
      notificationManager.showNotification({
        title: notificationTitle,
        body: notificationBody,
        icon: notificationIcon,
        sessionId: message.sessionId,
        onClick: (sessionId) => {
          // 点击通知时切换到该会话
          const targetSession = sessions.value.find((s) => s.sessionId === sessionId)
          if (targetSession) {
            selectSession(targetSession)
          }
        },
      })

      // 播放提示音
      notificationManager.playNotificationSound()

      // #ifdef APP-PLUS
      showLocalNotification({
        title: notificationTitle,
        body: notificationBody,
        sessionId: message.sessionId,
      })
      // #endif
    }

    // 更新角标
    notificationManager.setBadge?.(unreadSessionCount.value + pendingNotifyCount.value)
    syncNativeBadge(unreadSessionCount.value + pendingNotifyCount.value)
  }

  if (currentSession.value?.sessionId === message.sessionId) {
    appendMessage(message)
    await markSessionRead(message.sessionId, message.id)
  }
}

const startRealtime = () => {
  if (wsBound) return
  wsClient.setHandlers({
    onOpen: () => {
      syncOfflineMessages().catch((error) => {
        console.warn('[home] syncOfflineMessages on websocket open failed', error)
      })
    },
  })
  wsClient.addMessageListener(handleSocketMessage)
  wsClient.connect()
  wsBound = true
}

const stopRealtime = () => {
  if (!wsBound) return
  wsClient.removeMessageListener(handleSocketMessage)
  wsClient.close()
  wsBound = false
}

watch(desktopTab, (value) => {
  if (value === 'contacts') {
    ensureDesktopContactSelection()
  }
})

watch(
  [friends, groups],
  () => {
    if (desktopTab.value === 'contacts') {
      ensureDesktopContactSelection()
    }
  },
  { deep: true },
)

watch(mobileTab, () => {
  searchedUser.value = null
  groupSearchResults.value = []
})

watch(
  () => unreadSessionCount.value + pendingNotifyCount.value,
  (count) => {
    notificationManager.setBadge?.(count)
    syncNativeBadge(count)
  },
  { immediate: true },
)

watch(
  userInfo,
  (value) => {
    if (!value || typeof value !== 'object') {
      return
    }
    writeCacheValue(HOME_CACHE_KEYS.USER_INFO, value)
    uni.setStorageSync('userInfo', value)
  },
  { deep: true },
)

watch(
  friends,
  (value) => {
    writeCacheValue(HOME_CACHE_KEYS.FRIENDS, normalizeCachedArray(value))
  },
  { deep: true },
)

watch(
  groups,
  (value) => {
    writeCacheValue(HOME_CACHE_KEYS.GROUPS, normalizeCachedArray(value))
  },
  { deep: true },
)

watch(
  sessions,
  (value) => {
    writeCacheValue(HOME_CACHE_KEYS.SESSIONS, normalizeCachedArray(value))
  },
  { deep: true },
)

watch(
  notifications,
  (value) => {
    writeCacheValue(HOME_CACHE_KEYS.NOTIFICATIONS, {
      friendApplies: normalizeCachedArray(value?.friendApplies),
      groupApplies: normalizeCachedArray(value?.groupApplies),
    })
  },
  { deep: true },
)

watch(
  groupMembers,
  (value) => {
    if (Number(currentSession.value?.sessionType) !== SESSION_TYPE.GROUP) {
      return
    }
    writeGroupMembersCache(currentSession.value?.targetId, value)
  },
  { deep: true },
)

watch(
  () => currentSession.value?.sessionId || '',
  (value) => {
    if (value) {
      writeCacheValue(HOME_CACHE_KEYS.ACTIVE_SESSION_ID, value)
      return
    }
    LocalStateCache.remove(resolveDashboardCacheScope(), HOME_CACHE_KEYS.ACTIVE_SESSION_ID)
  },
)

watch(
  [desktopContactSelectionType, desktopContactSelection],
  () => {
    writeCacheValue(HOME_CACHE_KEYS.ACTIVE_CONTACT_SELECTION, serializeDesktopContactSelection())
  },
  { deep: true },
)

watch(
  () => [currentSession.value?.sessionId || '', showChatInfo.value],
  ([sessionId, visible]) => {
    if (!sessionId) {
      return
    }
    writeChatDetailState(sessionId, { showInfo: visible })
  },
)

onMounted(async () => {
  userInfo.value = uni.getStorageSync('userInfo') || {}
  hydrateLocalDashboardState()
  await ChatStorage.pruneExpiredMessages().catch((error) => {
    console.warn('[home] prune expired chat cache failed', error)
  })
  updateViewportMode()
  if (supportsBrowserDom()) {
    window.addEventListener('resize', throttledUpdateViewport)
  }
  uni.$on('scannedContactCard', handleScannedContactCard)
  uni.$on('profileUpdated', handleProfileUpdated)
  uni.$on('notificationClick', handleNotificationClick)

  // #ifdef APP-PLUS
  // App 平台监听本地推送点击
  onNotificationClick((payload) => {
    console.log('[home] App notification clicked:', payload)
    if (payload.sessionId) {
      const targetSession = sessions.value.find((s) => s.sessionId === payload.sessionId)
      if (targetSession) {
        selectSession(targetSession)
      }
    }
  })
  // #endif

  startRealtime()
  await refreshDashboard()
  await syncOfflineMessages()
})

/**
 * 处理通知点击事件
 * @param {Object} data - 通知数据
 */
const handleNotificationClick = (data) => {
  const { sessionId } = data
  if (!sessionId) return

  const targetSession = sessions.value.find((s) => s.sessionId === sessionId)
  if (targetSession) {
    selectSession(targetSession)
  }
}

const resolveLatestSyncTimestamp = (list = []) =>
  list.reduce((latest, item) => {
    const candidate = new Date(item?.sendTime || item?.send_time || 0).getTime()
    return Number.isFinite(candidate) && candidate > latest ? candidate : latest
  }, getLastSyncTime())

const syncOfflineMessages = async () => {
  if (offlineSyncPromise) {
    return offlineSyncPromise
  }

  offlineSyncPromise = (async () => {
    try {
      const lastSyncTime = getLastSyncTime()
      const offlineMessages = await fetchOfflineMessages(lastSyncTime)

      if (!offlineMessages.length) {
        return
      }

      console.log(`[home] Synced ${offlineMessages.length} offline messages`)

      offlineMessages.forEach((msg) => {
        const enriched = enrichRealtimeMessage(normalizeMessage(msg))
        upsertGroupMemberCacheFromMessage(enriched)
        updateSessionFromMessage(enriched, true)
        persistMessageToCache(enriched)
        if (currentSession.value?.sessionId === enriched.sessionId) {
          appendMessage(enriched)
        }
      })

      if (currentSession.value?.sessionId) {
        await markSessionRead(
          currentSession.value.sessionId,
          resolveLatestReadMessageId(currentSession.value.sessionId),
        )
      }

      const acked = await ackOfflineMessages(offlineMessages.map((item) => item?.id))
      if (acked) {
        saveLastSyncTime(resolveLatestSyncTimestamp(offlineMessages))
      }

      await refreshDashboard(true)
    } catch (error) {
      console.error('[home] Failed to sync offline messages:', error)
    } finally {
      offlineSyncPromise = null
    }
  })()

  return offlineSyncPromise
}

onShow(() => {
  if (!hasFinishedInitialDashboardLoad) {
    return
  }

  console.log('[home] Page onShow triggered')

  // 清除通知角标
  notificationManager.clearBadge()
  syncNativeBadge(0)

  const now = Date.now()
  const backgroundDuration = now - lastVisibleTime
  console.log(`[home] Background duration: ${backgroundDuration}ms`)

  // 更新可见时间
  lastVisibleTime = now

  // 确保 WebSocket 连接正常
  if (wsClient && !wsClient.wsTask) {
    const token = uni.getStorageSync('satoken')
    if (token) {
      console.log('[home] WebSocket disconnected, reconnecting...')
      wsClient.reconnectTimes = 0 // 重置重连计数
      wsClient.connect()
    }
  } else if (wsClient && wsClient.wsTask) {
    // 发送心跳确保连接活跃
    console.log('[home] Sending heartbeat on resume')
    wsClient.send({
      type: 'ping',
      data: 'resume-heartbeat',
    })
  }

  // 如果超过30秒后台时间，强制刷新会话列表
  if (backgroundDuration > BACKGROUND_REFRESH_THRESHOLD_MS) {
    console.log('[home] Background time exceeded threshold, force refreshing dashboard')
    refreshDashboard(true).catch((error) => {
      if (!isAbortLikeRequestError(error)) {
        console.warn('[home] onShow force refreshDashboard failed', error)
      }
    }).finally(() => {
      syncOfflineMessages().catch((error) => {
        console.warn('[home] onShow syncOfflineMessages failed', error)
      })
    })
  } else {
    // 正常刷新仪表板数据
    refreshDashboard().catch((error) => {
      if (!isAbortLikeRequestError(error)) {
        console.warn('[home] onShow refreshDashboard failed', error)
      }
    }).finally(() => {
      syncOfflineMessages().catch((error) => {
        console.warn('[home] onShow syncOfflineMessages failed', error)
      })
    })
  }
})

onBeforeUnmount(() => {
  if (supportsBrowserDom()) {
    window.removeEventListener('resize', throttledUpdateViewport)
  }
  if (resizeTimer) {
    clearTimeout(resizeTimer)
    resizeTimer = null
  }
  uni.$off('scannedContactCard', handleScannedContactCard)
  uni.$off('profileUpdated', handleProfileUpdated)
  uni.$off('notificationClick', handleNotificationClick)
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  stopRealtime()
  notificationManager.destroy()
})
</script>

<style scoped>
.wechat-page {
  height: 100vh;
  height: 100dvh;
  min-height: 100vh;
  min-height: 100dvh;
  overflow: hidden;
  background: linear-gradient(180deg, #ebeef0 0%, #f7f8f8 26%, #f5f6f7 100%);
  color: #111827;
  font-family: 'PingFang SC', 'Noto Sans SC', sans-serif;
}

.desktop-shell {
  display: grid;
  grid-template-columns: 84px 300px 1fr;
  height: 100vh;
  height: 100dvh;
  min-height: 100vh;
  min-height: 100dvh;
  overflow: hidden;
}

.desktop-rail {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
  height: 100%;
  min-height: 0;
  box-sizing: border-box;
  padding: 20px 12px calc(34px + env(safe-area-inset-bottom, 0px));
  background: linear-gradient(180deg, #20262c 0%, #15191d 100%);
  color: #c8d0d7;
  overflow: hidden;
}

.rail-profile {
  position: relative;
  width: 42px;
  height: 42px;
  border-radius: 8px;
  padding: 2px;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.04);
}

.rail-profile-avatar {
  width: 100%;
  height: 100%;
  border-radius: 6px;
  background: #cbd5e1;
}

.rail-profile-status {
  position: absolute;
  right: 1px;
  bottom: 1px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #07c160;
  border: 2px solid #1b2025;
}

.rail-nav {
  width: 100%;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding-bottom: 14px;
  box-sizing: border-box;
}

.rail-item {
  width: 46px;
  padding: 10px 0;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.rail-item.active {
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
}

.rail-icon,
.rail-label {
  font-size: 12px;
}

.rail-footer {
  width: 100%;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  padding-bottom: 2px;
}

.rail-quick {
  width: 46px;
  height: 46px;
  min-height: 46px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.04);
}

.rail-quick-text {
  font-size: 12px;
  line-height: 1;
  color: #d8fee8;
}

.desktop-side {
  height: 100vh;
  height: 100dvh;
  min-height: 0;
  overflow: hidden;
  border-right: 1rpx solid rgba(15, 23, 42, 0.06);
}

.desktop-main {
  height: 100vh;
  height: 100dvh;
  min-height: 0;
  overflow: hidden;
}

.desktop-main.contacts {
  background: #f5f5f5;
}

.desktop-main.cloud {
  grid-column: 2 / 4;
  height: 100vh;
  height: 100dvh;
  min-height: 0;
  overflow: hidden;
}

.contacts-summary {
  display: flex;
  align-items: stretch;
  height: 100%;
  padding: 34rpx;
}

.contacts-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320rpx, 420rpx);
  gap: 22rpx;
  width: 100%;
}

.contacts-overview-card,
.contacts-guide-card {
  padding: 34rpx;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 24rpx 60rpx rgba(15, 23, 42, 0.08);
}

.contacts-overview-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 360rpx;
}

.overview-copy {
  max-width: 840rpx;
}

.summary-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #111827;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  margin-top: 34rpx;
}

.summary-pill {
  padding: 24rpx;
  border-radius: 24rpx;
  background:
    radial-gradient(circle at top right, rgba(7, 193, 96, 0.14), transparent 46%),
    rgba(15, 23, 42, 0.04);
}

.summary-value {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #07c160;
}

.summary-label,
.summary-sub {
  font-size: 24rpx;
  color: #6b7280;
}

.summary-sub {
  display: block;
  margin-top: 12rpx;
  line-height: 1.7;
}

.contacts-guide-card {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 247, 245, 0.96)),
    rgba(255, 255, 255, 0.96);
}

.guide-row {
  display: flex;
  gap: 16rpx;
  padding: 20rpx;
  border-radius: 24rpx;
  background: rgba(15, 23, 42, 0.04);
}

.guide-index {
  width: 56rpx;
  height: 56rpx;
  border-radius: 18rpx;
  background: rgba(7, 193, 96, 0.12);
  color: #07c160;
  font-size: 22rpx;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.guide-copy {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.guide-title {
  font-size: 26rpx;
  font-weight: 700;
  color: #111827;
}

.guide-text {
  font-size: 22rpx;
  line-height: 1.65;
  color: #6b7280;
}

.mobile-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 100vh;
  overflow: hidden;
}

.mobile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: calc(18rpx + env(safe-area-inset-top, 0px)) 18rpx 14rpx;
  background: rgba(247, 248, 248, 0.96);
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
}

.mobile-user {
  display: flex;
  align-items: center;
  gap: 16rpx;
  min-width: 0;
  flex: 1;
}

.mobile-avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 28rpx;
  background: #d1d5db;
}

.mobile-copy {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.mobile-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #111827;
}

.mobile-sub {
  font-size: 22rpx;
  color: #6b7280;
}

.mobile-actions {
  display: flex;
  gap: 12rpx;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.mobile-action {
  min-width: 92rpx;
  height: 64rpx;
  padding: 0 18rpx;
  border-radius: 20rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.05);
  color: #111827;
  font-size: 23rpx;
  transition: background 0.15s ease, transform 0.15s ease;
  /* 触摸反馈 */
  -webkit-tap-highlight-color: transparent;
}

/* 移动端触摸反馈 */
.mobile-action:active {
  background: rgba(15, 23, 42, 0.12);
  transform: scale(0.96);
}

.mobile-user:active {
  opacity: 0.8;
}

.mobile-avatar:active {
  transform: scale(0.95);
}

/* 移动端触摸优化 - 禁用文字选中 */
.mobile-shell text,
.mobile-shell view {
  user-select: none;
  -webkit-user-select: none;
  -webkit-touch-callout: none;
}

/* 移动端滚动优化 */
.mobile-content {
  flex: 1;
  min-height: 0;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
}

/* 移动端安全区域降级方案 */
.mobile-header {
  background: rgba(247, 248, 248, 0.98);
}
/* 支持 backdrop-filter 的设备 */
@supports (backdrop-filter: blur(10px)) or (-webkit-backdrop-filter: blur(10px)) {
  .mobile-header {
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    background: rgba(247, 248, 248, 0.85);
  }
}

.mobile-content {
  flex: 1;
  min-height: 0;
}
</style>

