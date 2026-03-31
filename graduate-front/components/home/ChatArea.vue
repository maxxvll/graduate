<template>
  <view class="chat-area" :class="{ mobile }">
    <view v-if="session" ref="chatShellRef" class="chat-shell">
      <view class="chat-header">
        <view class="header-main">
          <view v-if="mobile" class="mobile-back" @click="$emit('back')">‹</view>
          <view class="header-copy">
            <text class="header-title">{{ session.sessionName || '未命名会话' }}</text>
            <text class="header-sub">
              {{ Number(session.sessionType) === 2 ? `${groupMembers.length || 0} 位成员` : '单聊' }}
            </text>
          </view>
        </view>

        <view class="header-tools">
          <view
            v-if="mobile && isSingleChat"
            class="tool-btn"
            @click="$emit('start-call', { mode: 'audio' })"
          >
            <view class="tool-icon icon-phone"></view>
          </view>
          <view
            v-if="mobile && isSingleChat && showVideoCall"
            class="tool-btn"
            @click="$emit('start-call', { mode: 'video' })"
          >
            <view class="tool-icon icon-video"></view>
          </view>
          <view
            v-if="mobile"
            class="tool-btn"
            :class="{ active: showSearchPanel }"
            @click="handleHeaderTool('search')"
          >
            <view class="tool-icon icon-search-chat"></view>
          </view>
          <view
            v-if="!mobile"
            class="tool-btn"
            :class="{ active: showSearchPanel }"
            @click="handleHeaderTool('search')"
          >
            <view class="tool-icon icon-search-chat"></view>
          </view>
          <view class="tool-btn" :class="{ active: showInfo }" @click="handleHeaderTool('more')">
            <view class="tool-icon icon-dots"></view>
          </view>
        </view>
      </view>

      <view class="chat-body" :class="{ 'with-info': showInfo && !mobile }">
        <scroll-view
          class="message-scroll"
          scroll-y
          scroll-with-animation
          :scroll-into-view="scrollIntoViewId"
          @scroll="handleScroll"
        >
          <view class="message-viewport">
            <view v-if="loading && !messages.length" class="state-block">
              <text class="state-text">消息加载中...</text>
            </view>

            <view v-else-if="!messages.length" class="state-block">
              <text class="state-text">发一条消息，开始这段对话。</text>
            </view>

            <view v-else class="message-stream">
              <view v-if="hasMore" class="history-entry">
                <button class="history-btn" :disabled="loadingMore" @click="$emit('load-more')">
                  {{ loadingMore ? '加载中...' : '查看更多消息' }}
                </button>
              </view>

              <template v-for="entry in visibleEntries" :key="entry.key">
                <view v-if="entry.type === 'divider'" class="message-divider">
                  <text class="message-divider-text">{{ entry.label }}</text>
                </view>

                <view
                  v-else
                  :id="`message-${entry.message.id || entry.message.messageNo}`"
                  class="message-row"
                  :class="{ self: isSelf(entry.message) }"
                >
                  <image
                    class="message-avatar"
                    :src="resolveAvatar(entry.message)"
                    mode="aspectFill"
                  />

                  <view class="message-stack">
                    <!-- 引用回复气泡 -->
                    <view
                      v-if="entry.message.extInfo && entry.message.extInfo.quoteMessageId"
                      class="quote-inline"
                    >
                      <text class="quote-inline-label">{{ entry.message.extInfo.quoteSenderName || '引用消息' }}</text>
                      <text class="quote-inline-content">{{ entry.message.extInfo.quoteContent || '[图片]' }}</text>
                    </view>

                    <!-- @提及高亮 -->
                    <view v-if="entry.message.isAtAll === 1" class="at-highlight">
                      <text class="at-highlight-text">@所有人</text>
                    </view>
                    <view v-else-if="entry.message.atUserIds && hasAtCurrentUser(entry.message) && !isSelf(entry.message)" class="at-highlight">
                      <text class="at-highlight-text">有人@我</text>
                    </view>

                    <text
                      v-if="Number(session.sessionType) === 2 && !isSelf(entry.message) && entry.message.senderName"
                      class="sender-name"
                    >
                      {{ entry.message.senderName }}
                    </text>

                    <view class="bubble-wrap" :class="{ self: isSelf(entry.message) }">
                      <view
                        class="message-bubble"
                        :class="bubbleClass(entry.message)"
                        @click="handleBubbleClick(entry.message)"
                        @contextmenu.prevent="e => handleContextMenu(e, entry.message)"
                        @longpress="e => handleLongPress(e, entry.message)"
                      >
                        <template v-if="isImage(entry.message)">
                          <image
                            class="bubble-image"
                            :src="entry.message.fileUrl"
                            mode="aspectFill"
                            lazy-load
                            @click.stop="previewImage(entry.message.fileUrl)"
                          />
                        </template>
                        <template v-else-if="isAudio(entry.message)">
                          <view class="audio-card" :class="{ playing: playingMessageKey === messageKey(entry.message) }">
                            <view class="audio-wave">
                              <text class="wave-bar"></text>
                              <text class="wave-bar"></text>
                              <text class="wave-bar"></text>
                            </view>
                            <text class="audio-duration">{{ formatAudioDuration(entry.message.duration) }}</text>
                          </view>
                        </template>
                        <template v-else-if="isFile(entry.message)">
                          <view class="file-card">
                            <view class="file-copy">
                              <text class="file-name">{{ entry.message.fileName || '附件' }}</text>
                              <text class="file-meta">{{ formatFileSize(entry.message.fileSize) }}</text>
                            </view>
                            <view class="file-arrow">›</view>
                          </view>
                        </template>
                        <template v-else>
                          <text class="bubble-text">{{ resolveContent(entry.message) }}</text>
                        </template>
                      </view>
                    </view>

                    <view
                      v-if="shouldShowMessageMeta(entry.message)"
                      class="message-meta"
                      :class="{ self: isSelf(entry.message) }"
                    >
                      <text v-if="formatMessageMetaTime(entry.message)" class="meta-time">
                        {{ formatMessageMetaTime(entry.message) }}
                      </text>
                      <text v-if="entry.message.clientStatus === 'pending'" class="meta-state pending">发送中</text>
                      <template v-else-if="entry.message.clientStatus === 'failed' || entry.message.status === 5">
                        <text class="meta-state failed">发送失败</text>
                        <text class="meta-action" @click.stop="$emit('retry-failed', entry.message)">重发</text>
                      </template>
                    </view>
                  </view>
                </view>
              </template>

              <view :id="MESSAGE_BOTTOM_ANCHOR_ID" class="message-bottom-anchor"></view>
            </view>
          </view>
        </scroll-view>

        <!-- 跳到最新按钮 -->
        <view v-if="showJumpToBottom" class="jump-to-bottom" @click="jumpToLatest">
          <text class="jump-to-bottom-icon">↓</text>
          <text class="jump-to-bottom-text">有新消息</text>
        </view>

        <view v-if="showInfo" class="info-panel" :class="{ mobile }">
          <view class="info-members">
            <view
              v-for="member in infoMembers"
              :key="member.userId"
              class="member-card"
            >
              <image class="member-avatar" :src="member.avatar || defaultAvatar" mode="aspectFill" />
              <text class="member-name">{{ member.nickname || member.userId }}</text>
            </view>

            <view class="member-card add-card" @click="handleInfoAction('add')">
              <view class="member-add">+</view>
              <text class="member-name">添加</text>
            </view>
          </view>

          <view class="info-list">
            <view class="info-row clickable" @click="handleInfoAction('search')">
              <text class="info-label">查找聊天内容</text>
              <text class="info-arrow">›</text>
            </view>

            <view class="info-row">
              <text class="info-label">消息免打扰</text>
              <view class="switch" :class="{ active: session.isMute === 1 }" @click="$emit('toggle-mute')">
                <view class="switch-knob"></view>
              </view>
            </view>

            <view class="info-row">
              <text class="info-label">置顶聊天</text>
              <view class="switch" :class="{ active: session.isTop === 1 }" @click="$emit('toggle-top')">
                <view class="switch-knob"></view>
              </view>
            </view>
          </view>

          <view class="danger-row" @click="$emit('clear-history')">清空聊天记录</view>
        </view>
      </view>

      <view v-if="showSearchPanel" class="search-panel-mask" @click.self="closeSearchPanel">
        <view class="search-panel" :class="{ mobile }">
          <view class="search-panel-header">
            <text class="search-panel-title">与“{{ session.sessionName || '当前会话' }}”的聊天记录</text>
            <view class="search-panel-close" @click="closeSearchPanel">×</view>
          </view>

          <view class="search-input-shell">
            <view class="search-input-icon"></view>
            <input
              v-model="searchKeyword"
              class="search-panel-input"
              placeholder="搜索"
              confirm-type="search"
            />
          </view>

          <scroll-view class="search-tabs" scroll-x enable-flex>
            <view class="search-tabs-row">
              <view
                v-for="tab in searchTabs"
                :key="tab.key"
                class="search-tab"
                :class="{ active: searchTab === tab.key }"
                @click="searchTab = tab.key"
              >
                {{ tab.label }}
              </view>
            </view>
          </scroll-view>

          <scroll-view class="search-results" scroll-y>
            <view v-if="searchTab === 'date'">
              <view v-if="searchDateResults.length" class="search-result-list">
                <view
                  v-for="item in searchDateResults"
                  :key="item.key"
                  class="search-date-item"
                >
                  <view class="search-date-copy">
                    <text class="search-date-label">{{ item.label }}</text>
                    <text class="search-date-meta">{{ item.count }} 条记录</text>
                  </view>
                  <text class="search-result-link" @click="locateMessage(item.message)">定位到聊天位置</text>
                </view>
              </view>
              <view v-else class="search-empty">
                <text class="search-empty-title">没有匹配的日期</text>
                <text class="search-empty-sub">试试别的关键词，或者切换到其它分类。</text>
              </view>
            </view>

            <view v-else-if="searchResults.length" class="search-result-list">
              <view
                v-for="item in searchResults"
                :key="messageKey(item)"
                class="search-result-item"
              >
                <image class="search-result-avatar" :src="resolveAvatar(item)" mode="aspectFill" />

                <view class="search-result-main">
                  <view class="search-result-top">
                    <text class="search-result-name">{{ resolveSearchSender(item) }}</text>
                    <text class="search-result-time">{{ formatSearchDateTime(item.sendTime || item.createdAt) }}</text>
                  </view>

                  <view v-if="isImage(item)" class="search-result-media">
                    <image class="search-result-thumb" :src="item.fileUrl" mode="aspectFill" />
                  </view>
                  <text v-else class="search-result-text">{{ resolveSearchPreview(item) }}</text>
                </view>

                <text class="search-result-link" @click="locateMessage(item)">定位到聊天位置</text>
              </view>
            </view>

            <view v-else class="search-empty">
              <text class="search-empty-title">没有找到相关聊天记录</text>
              <text class="search-empty-sub">当前搜索仅覆盖本机缓存的 15 天聊天记录。</text>
            </view>
          </scroll-view>
        </view>
      </view>

      <view v-if="screenshotPreviewVisible" class="capture-mask" @click.self="closeScreenshotPreview">
        <view class="capture-panel" :class="{ mobile }">
          <view class="capture-header">
            <text class="capture-title">聊天截图</text>
            <view class="capture-close" @click="closeScreenshotPreview">×</view>
          </view>

          <view class="capture-body">
            <image
              v-if="screenshotPreviewUrl"
              class="capture-image"
              :src="screenshotPreviewUrl"
              mode="widthFix"
            />
          </view>

          <view class="capture-actions">
            <view class="capture-btn ghost" @click="closeScreenshotPreview">关闭</view>
            <view
              class="capture-btn primary"
              :class="{ disabled: screenshotSaving }"
              @click="saveScreenshotPreview"
            >
              {{ screenshotSaving ? '处理中...' : screenshotSaveLabel }}
            </view>
          </view>
        </view>
      </view>

      <view class="composer">
        <view v-if="isRecording" class="recording-strip">
          <view class="recording-main">
            <view class="recording-dot"></view>
            <text class="recording-label">正在录音 {{ formatAudioDuration(recordingDuration) }}</text>
          </view>
          <view class="recording-actions">
            <view class="recording-action secondary" @click="cancelRecording()">取消</view>
            <view class="recording-action primary" @click="sendRecordedVoice()">发送</view>
          </view>
        </view>

        <view class="composer-toolbar">
          <view class="toolbar-left">
            <view class="toolbar-btn" :class="{ active: showEmojiPanel }" @click="toggleEmojiPanel">
              <view class="toolbar-icon icon-smile"></view>
            </view>
            <view class="toolbar-btn" @click="$emit('pick-image')">
              <view class="toolbar-icon icon-image"></view>
            </view>
            <view class="toolbar-btn" @click="$emit('pick-file')">
              <view class="toolbar-icon icon-folder"></view>
            </view>
            <view class="toolbar-btn" @click="handleToolbarAction('cut')">
              <view class="toolbar-icon icon-scissors"></view>
            </view>
            <view class="toolbar-btn" :class="{ active: isRecording }" @click="handleToolbarAction('voice')">
              <view class="toolbar-icon icon-mic"></view>
            </view>
          </view>

          <view v-if="!mobile && isSingleChat" class="toolbar-right">
            <view class="toolbar-btn call-btn" @click="$emit('start-call', { mode: 'audio' })">
              <view class="toolbar-icon icon-phone"></view>
            </view>
            <view
              v-if="showVideoCall"
              class="toolbar-btn call-btn"
              @click="$emit('start-call', { mode: 'video' })"
            >
              <view class="toolbar-icon icon-video"></view>
            </view>
          </view>
        </view>

        <view v-if="showEmojiPanel" class="emoji-panel">
          <!-- Tab切换 -->
          <view class="emoji-tabs">
            <view class="emoji-tab" :class="{ active: emojiTab === 'emoji' }" @click="emojiTab = 'emoji'">表情</view>
            <view class="emoji-tab" :class="{ active: emojiTab === 'sticker' }" @click="switchToStickerTab">收藏</view>
          </view>

          <!-- 表情Tab -->
          <view v-if="emojiTab === 'emoji'">
            <view v-if="recentEmojis.length" class="emoji-section">
              <text class="emoji-section-title">最近使用</text>
              <view class="emoji-grid">
                <view
                  v-for="emoji in recentEmojis"
                  :key="`recent-${emoji}`"
                  class="emoji-cell"
                  @click="insertEmoji(emoji)"
                >
                  <text class="emoji-char">{{ emoji }}</text>
                </view>
              </view>
            </view>

            <view class="emoji-section">
              <text class="emoji-section-title">全部表情</text>
              <view class="emoji-grid">
                <view
                  v-for="emoji in allEmojis"
                  :key="emoji"
                  class="emoji-cell"
                  @click="insertEmoji(emoji)"
                >
                  <text class="emoji-char">{{ emoji }}</text>
                </view>
              </view>
            </view>
          </view>

          <!-- 收藏表情Tab -->
          <view v-if="emojiTab === 'sticker'" class="sticker-panel">
            <view v-if="loadingStickers" class="emoji-section">
              <text class="emoji-section-title">加载中...</text>
            </view>
            <view v-else-if="!stickerList.length" class="emoji-section">
              <text class="emoji-section-title">暂无收藏</text>
              <view class="sticker-empty">
                <text class="sticker-empty-text">点击图片可以收藏表情包</text>
                <view class="sticker-add-btn" @click="chooseStickerImage">+ 添加表情</view>
              </view>
            </view>
            <view v-else class="emoji-section">
              <text class="emoji-section-title">我的收藏 ({{ stickerList.length }})</text>
              <view class="sticker-grid">
                <view
                  v-for="sticker in stickerList"
                  :key="sticker.id"
                  class="sticker-cell"
                  @click="insertSticker(sticker)"
                >
                  <image class="sticker-image" :src="sticker.url" mode="aspectFill" />
                </view>
                <view class="sticker-cell add-sticker" @click="chooseStickerImage">
                  <text class="sticker-add-icon">+</text>
                </view>
              </view>
            </view>
          </view>
        </view>

        <view class="composer-input-row">
          <view class="composer-editor" :class="{ mobile }">
            <textarea
              v-model="draft"
              class="composer-input"
              :auto-height="mobile"
              maxlength="5000"
              placeholder="输入消息"
              confirm-type="send"
              :confirm-hold="false"
              :show-confirm-bar="false"
              :disabled="isRecording"
              @input="handleInput"
              @keydown="handleComposerKeydown"
              @confirm="submitDraft"
            />
          </view>
          <button
            class="send-btn"
            :disabled="isRecording || !draft.trim()"
            :class="{ busy: sending }"
            @click="submitDraft"
          >
            {{ mobile ? '发送' : '发送(S)' }}
          </button>
        </view>

        <!-- @提及选择器 -->
        <view v-if="showAtPicker" class="at-picker">
          <view class="at-picker-header">
            <text class="at-picker-title">选择要@的人</text>
            <view class="at-picker-close" @click="closeAtPicker">×</view>
          </view>
          <input
            v-model="atSearchKeyword"
            class="at-search-input"
            placeholder="搜索成员..."
            @input="filterAtMembers"
          />
          <scroll-view class="at-member-list" scroll-y>
            <view
              v-if="isGroupChat && !atSearchKeyword"
              class="at-member-item"
              @click="selectAtAll"
            >
              <view class="at-member-avatar all-avatar">@</view>
              <text class="at-member-name">所有人</text>
            </view>
            <view
              v-for="member in filteredAtMembers"
              :key="member.userId"
              class="at-member-item"
              @click="selectAtMember(member)"
            >
              <image class="at-member-avatar" :src="member.avatar || defaultAvatar" mode="aspectFill" />
              <text class="at-member-name">{{ member.nickname || member.userId }}</text>
            </view>
            <view v-if="!filteredAtMembers.length && !isGroupChat" class="at-empty">
              <text class="at-empty-text">该会话不支持@提及</text>
            </view>
            <view v-else-if="!filteredAtMembers.length && atSearchKeyword" class="at-empty">
              <text class="at-empty-text">没有找到匹配的成员</text>
            </view>
          </scroll-view>
        </view>

        <!-- 引用回复气泡 -->
        <view v-if="quoteMessage" class="quote-bubble">
          <view class="quote-info">
            <text class="quote-label">回复 {{ quoteSenderName }}</text>
            <view class="quote-content">{{ quoteMessage.content || '[图片]' }}</view>
          </view>
          <view class="quote-close" @click="clearQuote">×</view>
        </view>
      </view>
    </view>

    <view v-else class="chat-empty">
      <text class="chat-empty-title">选择一个会话开始聊天</text>
      <text class="chat-empty-sub">左侧选择会话，右侧会显示当前聊天内容。</text>
    </view>
  </view>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { APP_PERMISSION_SCOPE, ensureAnyAppPermissionAccess } from '@/utils/app-permission'
import { captureChatScreenshot, saveCapturedScreenshot } from '@/utils/chat-screenshot'
import { downloadRemoteFileToLocalPath } from '@/utils/file-runtime'
import LocalStateCache from '@/utils/local-state-cache'
import { isAppPlusRuntime, supportsBrowserDom } from '@/utils/runtime'
import { DEFAULT_AVATAR as defaultAvatar } from '@/utils/common'

const props = defineProps({
  session: { type: Object, default: null },
  messages: { type: Array, default: () => [] },
  groupMembers: { type: Array, default: () => [] },
  currentUserId: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  loadingMore: { type: Boolean, default: false },
  hasMore: { type: Boolean, default: false },
  showInfo: { type: Boolean, default: false },
  sending: { type: Boolean, default: false },
  mobile: { type: Boolean, default: false },
  showVideoCall: { type: Boolean, default: false },
})

const emit = defineEmits([
  'back',
  'load-more',
  'toggle-info',
  'toggle-top',
  'toggle-mute',
  'clear-history',
  'pick-image',
  'pick-file',
  'send-text',
  'send-audio',
  'send-sticker',
  'retry-failed',
  'start-call',
  'forward-message',
  'message-recalled',
  'message-deleted',
  'add-favorite',
])

const AUDIO_MESSAGE_TYPE = 4
const IMAGE_MESSAGE_TYPE = 2
const VIDEO_MESSAGE_TYPE = 3
const FILE_MESSAGE_TYPE = 5
const MAX_AUDIO_DURATION = 300
const TIMELINE_GAP_MS = 5 * 60 * 1000
const RECENT_EMOJI_STORAGE_KEY = 'chat_recent_emojis'
const WEEKDAY_LABELS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
const MESSAGE_BOTTOM_ANCHOR_ID = 'message-bottom-anchor'

const isAtBottom = ref(true)
const showJumpToBottom = ref(false)
const allEmojis = [
  '馃榾',
  '馃榿',
  '馃槀',
  '馃ぃ',
  '馃槉',
  '馃槏',
  '馃槝',
  '馃槑',
  '馃槒',
  '馃槶',
  '馃槨',
  '馃槾',
  '馃',
  '馃檶',
  '馃憦',
  '馃憤',
  '馃憖',
  '鉂わ笍',
  '馃敟',
  '馃帀',
  '馃寵',
  '☀️',
  '馃崁',
  '馃嵒',
]

const draft = ref('')
const draftCursor = ref(-1)
const showEmojiPanel = ref(false)
const showSearchPanel = ref(false)
const chatShellRef = ref(null)
const searchKeyword = ref('')
const searchTab = ref('all')
const recentEmojis = ref([])
const isRecording = ref(false)
const recordingDuration = ref(0)
const playingMessageKey = ref('')
const scrollTarget = ref('')
const screenshotPreviewVisible = ref(false)
const screenshotPreviewUrl = ref('')
const screenshotLocalPath = ref('')

// @提及相关状态
const showAtPicker = ref(false)
const atSearchKeyword = ref('')
const atMembers = ref([])
const selectedAtUsers = ref([])

// 引用回复相关状态
const quoteMessage = ref(null)
const quoteSenderName = ref('')
const screenshotFileName = ref('')
const screenshotType = ref('')
const screenshotSaving = ref(false)

// 表情包相关状态
const emojiTab = ref('emoji')
const stickerList = ref([])
const loadingStickers = ref(false)

let browserMediaRecorder = null
let browserAudioStream = null
let browserAudioChunks = []
let browserAudioPlayer = null
let appAudioPlayer = null
let recorderManager = null
let recorderListenersBound = false
let recordingStopAction = 'send'
let recordingTimer = null
let recordingStartedAt = 0
let scrollResetTimer = null

const CHAT_UI_CACHE_KEY_PREFIX = 'chat_ui_state'

const resolveChatUiCacheScope = () => {
  const currentUserId = String(props.currentUserId || '')
  if (currentUserId) {
    return `chat-ui:${currentUserId}`
  }

  try {
    const storedUser = uni.getStorageSync('userInfo') || {}
    const storedId = String(storedUser?.id || '')
    if (storedId) {
      return `chat-ui:${storedId}`
    }
  } catch {}

  return 'chat-ui:anonymous'
}

const resolveChatUiCacheKey = (sessionId = '') => `${CHAT_UI_CACHE_KEY_PREFIX}:${String(sessionId || '')}`

const restoreChatUiState = (sessionId) => {
  const normalizedSessionId = String(sessionId || '')
  if (!normalizedSessionId) {
    showSearchPanel.value = false
    searchKeyword.value = ''
    searchTab.value = 'all'
    return
  }

  const snapshot = LocalStateCache.getValue(
    resolveChatUiCacheScope(),
    resolveChatUiCacheKey(normalizedSessionId),
  )

  if (!snapshot || typeof snapshot !== 'object') {
    showSearchPanel.value = false
    searchKeyword.value = ''
    searchTab.value = 'all'
    return
  }

  showSearchPanel.value = Boolean(snapshot.showSearchPanel)
  searchKeyword.value = String(snapshot.searchKeyword || '')
  searchTab.value = String(snapshot.searchTab || 'all')
}

const persistChatUiState = (sessionId) => {
  const normalizedSessionId = String(sessionId || '')
  if (!normalizedSessionId) {
    return
  }

  LocalStateCache.set(resolveChatUiCacheScope(), resolveChatUiCacheKey(normalizedSessionId), {
    showSearchPanel: showSearchPanel.value,
    searchKeyword: searchKeyword.value,
    searchTab: searchTab.value,
  })
}

const loadRecentEmojis = () => {
  try {
    const stored = uni.getStorageSync(RECENT_EMOJI_STORAGE_KEY)
    recentEmojis.value = Array.isArray(stored) ? stored.filter(Boolean).slice(0, 12) : []
  } catch {
    recentEmojis.value = []
  }
}

loadRecentEmojis()

watch(
  () => props.session?.sessionId,
  () => {
    draft.value = ''
    draftCursor.value = -1
    showEmojiPanel.value = false
    restoreChatUiState(props.session?.sessionId)
    closeScreenshotPreview()
    cancelRecording({ silent: true })
    stopAudioPlayback()
    isAtBottom.value = true
    showJumpToBottom.value = false
    scrollToBottom()
  },
)

watch(
  () => [props.session?.sessionId, showSearchPanel.value, searchKeyword.value, searchTab.value],
  ([sessionId]) => {
    if (!sessionId) {
      return
    }
    persistChatUiState(sessionId)
  },
)

onBeforeUnmount(() => {
  closeScreenshotPreview()
  cancelRecording({ silent: true })
  stopAudioPlayback()
  if (scrollResetTimer) {
    clearTimeout(scrollResetTimer)
    scrollResetTimer = null
  }
})

const scrollIntoViewId = computed(() => scrollTarget.value)
const isSingleChat = computed(() => Number(props.session?.sessionType) === 1)
const isGroupChat = computed(() => Number(props.session?.sessionType) === 2)
const screenshotSaveLabel = computed(() => (isAppPlusRuntime() ? '保存到相册' : '下载图片'))

const buildChronologicalKey = (item) => String(item?.id || item?.messageNo || '')
const getChronologicalMessageTime = (message) => {
  const rawValue = message?.sendTime || message?.createdAt
  if (!rawValue) return 0
  const timestamp = new Date(rawValue).getTime()
  return Number.isFinite(timestamp) ? timestamp : 0
}

function getChronologicalMessages() {
  const messages = Array.isArray(props.messages) ? props.messages : []
  return [...messages].sort((left, right) => {
    const timeDelta = getChronologicalMessageTime(left) - getChronologicalMessageTime(right)
    if (timeDelta !== 0) return timeDelta

    const keyDelta = buildChronologicalKey(left).localeCompare(buildChronologicalKey(right))
    if (keyDelta !== 0) return keyDelta

    return String(left?.senderId || '').localeCompare(String(right?.senderId || ''))
  })
}

const timelineEntries = computed(() => {
  const entries = []
  const chronologicalMessages = getChronologicalMessages()
  let previousTime = 0

  chronologicalMessages.forEach((message, index) => {
    const currentTime = toTimestamp(message.sendTime || message.createdAt)
    const shouldShowDivider =
      index === 0 ||
      (!Number.isNaN(currentTime) &&
        currentTime > 0 &&
        (!previousTime || currentTime - previousTime >= TIMELINE_GAP_MS))

    if (shouldShowDivider) {
      entries.push({
        type: 'divider',
        key: `divider-${messageKey(message) || index}`,
        label: formatTimelineTime(message.sendTime || message.createdAt),
      })
    }

    entries.push({
      type: 'message',
      key: `message-${messageKey(message) || index}`,
      message,
    })

    if (!Number.isNaN(currentTime) && currentTime > 0) {
      previousTime = currentTime
    }
  })

  return entries
})

// 保持完整时间线渲染，确保消息顺序、滚动到底和搜索定位稳定一致。
const visibleEntries = computed(() => timelineEntries.value)

const searchTabs = [
  { key: 'all', label: '全部' },
  { key: 'text', label: '文本' },
  { key: 'image', label: '图片与视频' },
  { key: 'audio', label: '语音' },
  { key: 'file', label: '文件' },
  { key: 'date', label: '日期' },
]

const infoMembers = computed(() => {
  if (Number(props.session?.sessionType) === 2) {
    return props.groupMembers.slice(0, 8)
  }
  return [
    {
      userId: String(props.session?.targetId || ''),
      nickname: props.session?.sessionName || '联系人',
      avatar: props.session?.sessionAvatar || defaultAvatar,
    },
  ]
})

const isSelf = (item) => String(item.senderId || '') === String(props.currentUserId || '')
const messageKey = (item) => String(item?.id || item?.messageNo || '')

// @提及相关计算属性和方法
const filteredAtMembers = computed(() => {
  if (!atSearchKeyword.value.trim()) {
    return atMembers.value
  }
  const keyword = atSearchKeyword.value.toLowerCase()
  return atMembers.value.filter(
    (m) =>
      (m.nickname && m.nickname.toLowerCase().includes(keyword)) ||
      (m.userId && m.userId.toLowerCase().includes(keyword)),
  )
})

const openAtPicker = () => {
  if (!isGroupChat.value) return
  atMembers.value = [...props.groupMembers]
  atSearchKeyword.value = ''
  showAtPicker.value = true
}

const closeAtPicker = () => {
  showAtPicker.value = false
  atSearchKeyword.value = ''
}

const filterAtMembers = () => {
  // 搜索逻辑通过 computed 属性自动处理
}

const selectAtMember = (member) => {
  const atText = `@${member.nickname || member.userId} `
  const cursor = draftCursor.value >= 0 ? draftCursor.value : draft.value.length
  // 找到 @ 符号的位置并替换
  const textBeforeAt = draft.value.substring(0, cursor)
  const atIndex = textBeforeAt.lastIndexOf('@')
  if (atIndex >= 0) {
    draft.value = draft.value.substring(0, atIndex) + atText + draft.value.substring(cursor)
    draftCursor.value = atIndex + atText.length
  }
  selectedAtUsers.value.push(member.userId)
  closeAtPicker()
}

const selectAtAll = () => {
  const atText = '@所有人 '
  const cursor = draftCursor.value >= 0 ? draftCursor.value : draft.value.length
  const textBeforeAt = draft.value.substring(0, cursor)
  const atIndex = textBeforeAt.lastIndexOf('@')
  if (atIndex >= 0) {
    draft.value = draft.value.substring(0, atIndex) + atText + draft.value.substring(cursor)
    draftCursor.value = atIndex + atText.length
  }
  selectedAtUsers.value.push('__at_all__')
  closeAtPicker()
}

// 引用回复相关方法
const setQuoteMessage = (message) => {
  quoteMessage.value = message
  quoteSenderName.value = message.senderName || message.senderId || '未知'
}

const clearQuote = () => {
  quoteMessage.value = null
  quoteSenderName.value = ''
}

// 检查消息是否@了当前用户
const hasAtCurrentUser = (message) => {
  if (!message.atUserIds) return false
  const atUserIds = typeof message.atUserIds === 'string' ? JSON.parse(message.atUserIds) : message.atUserIds
  return atUserIds.includes(props.currentUserId)
}

// 从消息内容中解析@提及的用户名（用于发送到后端前的预处理）
const parseAtMentions = (content, members) => {
  const atUserIds = []
  const isAtAll = content.includes('@所有人') || content.includes('@all')
  // 匹配 @username 格式
  const atPattern = /@([^\s@]+)/g
  let match
  while ((match = atPattern.exec(content)) !== null) {
    const username = match[1]
    const member = members.find(m =>
      m.nickname === username || m.userId === username
    )
    if (member) {
      atUserIds.push(member.userId)
    }
  }
  return { atUserIds, isAtAll: isAtAll ? 1 : 0 }
}

const scheduleAfterRender = (callback) => {
  nextTick(() => {
    if (typeof requestAnimationFrame === 'function') {
      requestAnimationFrame(() => callback())
      return
    }
    setTimeout(callback, 16)
  })
}

const setScrollTarget = (targetId) => {
  if (!targetId) return
  if (scrollResetTimer) {
    clearTimeout(scrollResetTimer)
    scrollResetTimer = null
  }

  scrollTarget.value = ''
  scheduleAfterRender(() => {
    scrollTarget.value = targetId
    scrollResetTimer = setTimeout(() => {
      if (scrollTarget.value === targetId) {
        scrollTarget.value = ''
      }
      scrollResetTimer = null
    }, 420)
  })
}

const scrollToBottom = () => {
  setScrollTarget(MESSAGE_BOTTOM_ANCHOR_ID)
}

const handleScroll = (e) => {
  const { scrollTop = 0, scrollHeight = 0, clientHeight = 0 } = e.detail || {}
  const distanceToBottom = scrollHeight - scrollTop - clientHeight
  isAtBottom.value = distanceToBottom <= 56
  showJumpToBottom.value = !isAtBottom.value
}

const jumpToLatest = () => {
  isAtBottom.value = true
  showJumpToBottom.value = false
  scrollToBottom()
}

const lastMessageSignature = computed(() => {
  const chronologicalMessages = getChronologicalMessages()
  const last = chronologicalMessages[chronologicalMessages.length - 1]
  if (!last) return ''
  return [
    props.session?.sessionId || '',
    chronologicalMessages.length,
    messageKey(last),
    String(last.senderId || ''),
    String(last.clientStatus || ''),
    String(last.status || ''),
  ].join(':')
})

watch(
  lastMessageSignature,
  (value, previousValue) => {
    if (!value) return
    const chronologicalMessages = getChronologicalMessages()
    const last = chronologicalMessages[chronologicalMessages.length - 1]
    if (!last) return

    if (!previousValue) {
      scrollToBottom()
      return
    }

    if (isAtBottom.value || isSelf(last)) {
      isAtBottom.value = true
      showJumpToBottom.value = false
      scrollToBottom()
    } else {
      showJumpToBottom.value = true
    }
  },
  { flush: 'post' },
)

const resolveAvatar = (item) => {
  if (isSelf(item)) {
    return item.senderAvatar || defaultAvatar
  }
  return item.senderAvatar || props.session?.sessionAvatar || defaultAvatar
}

const isImage = (item) => Number(item.messageType) === IMAGE_MESSAGE_TYPE && item.fileUrl
const isVideo = (item) => Number(item.messageType) === VIDEO_MESSAGE_TYPE && item.fileUrl
const isAudio = (item) => Number(item.messageType) === AUDIO_MESSAGE_TYPE
const isFile = (item) =>
  Number(item.messageType) === FILE_MESSAGE_TYPE ||
  (!!item.fileUrl && !!item.fileName && !isImage(item) && !isVideo(item) && !isAudio(item))

const isVisualMessage = (item) => isImage(item) || isVideo(item)

const bubbleClass = (item) => {
  const classes = [isSelf(item) ? 'self-bubble' : 'other-bubble']
  if (isAudio(item)) classes.push('audio-bubble')
  if (isFile(item)) classes.push('file-bubble')
  if (isVisualMessage(item)) classes.push('visual-bubble')
  if (item.clientStatus === 'pending') classes.push('pending-bubble')
  if (item.clientStatus === 'failed' || item.status === 5) classes.push('failed-bubble')
  return classes
}

const resolveContent = (item) => item.contentReplaced || item.content || '[空消息]'

const resolveSearchSender = (item) => {
  if (isSelf(item)) return '我'
  return item.senderName || props.session?.sessionName || '联系人'
}

const formatMessageMetaTime = (item) => {
  const rawValue = item?.sendTime || item?.createdAt
  if (!rawValue) return ''
  const date = new Date(rawValue)
  if (Number.isNaN(date.getTime())) return ''
  return formatHourMinute(date)
}

const shouldShowMessageMeta = (item) =>
  Boolean(
    formatMessageMetaTime(item) ||
      item?.clientStatus === 'pending' ||
      item?.clientStatus === 'failed' ||
      Number(item?.status) === 5,
  )

const toTimestamp = (value) => {
  if (!value) return NaN
  const date = new Date(value)
  return date.getTime()
}

const formatHourMinute = (date) =>
  `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`

const getDayStart = (date) => new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()

const formatTimelineTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''

  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMinutes = Math.floor(diffMs / 60000)
  const dayDiff = Math.floor((getDayStart(now) - getDayStart(date)) / 86400000)

  if (diffMs >= 0 && diffMinutes >= 1 && diffMinutes < 60) {
    return `${diffMinutes}分钟前`
  }

  if (dayDiff === 0) {
    return formatHourMinute(date)
  }

  if (dayDiff > 0 && dayDiff < 7) {
    return `${WEEKDAY_LABELS[date.getDay()]} ${formatHourMinute(date)}`
  }

  if (date.getFullYear() === now.getFullYear()) {
    return `${date.getMonth() + 1}月${date.getDate()}日 ${formatHourMinute(date)}`
  }

  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${formatHourMinute(date)}`
}

const formatSearchDateTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${String(
    date.getHours(),
  ).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const formatSearchDate = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

const formatFileSize = (size) => {
  if (!size && size !== 0) return '--'
  const units = ['B', 'KB', 'MB', 'GB']
  let value = Number(size)
  let index = 0
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index += 1
  }
  return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`
}

const clampAudioDuration = (seconds) => {
  const value = Math.round(Number(seconds || 0))
  if (!Number.isFinite(value) || value <= 0) return 1
  return Math.min(MAX_AUDIO_DURATION, value)
}

const formatAudioDuration = (seconds) => {
  const total = clampAudioDuration(seconds)
  if (total >= 60) {
    const minutes = Math.floor(total / 60)
    const remain = total % 60
    return `${minutes}:${String(remain).padStart(2, '0')}`
  }
  return `${total}"`
}

const previewImage = (url) => {
  if (!url) return
  // #ifdef APP-PLUS
  uni.previewImage({
    urls: [url],
    current: url,
    longPressActions: {
      itemList: ['发送给朋友', '保存图片', '收藏'],
      success: (res) => {
        if (res.tapIndex === 1) {
          // 保存图片
          handleSaveImage(url)
        }
      },
      fail: (err) => {
        console.warn('[previewImage] longPressActions failed:', err)
      }
    }
  })
  // #endif
  // #ifndef APP-PLUS
  uni.previewImage({ urls: [url], current: url })
  // #endif
}

// 保存图片到本地（App 环境下）
const handleSaveImage = async (url) => {
  if (!url) return
  // #ifdef APP-PLUS
  uni.downloadFile({
    url: url,
    success: (res) => {
      if (res.statusCode === 200) {
        uni.saveImageToPhotosAlbum({
          filePath: res.tempFilePath,
          success: () => {
            uni.showToast({ title: '保存成功', icon: 'success' })
          },
          fail: (err) => {
            console.error('[handleSaveImage] save failed:', err)
            uni.showToast({ title: '保存失败', icon: 'none' })
          }
        })
      }
    },
    fail: (err) => {
      console.error('[handleSaveImage] download failed:', err)
      uni.showToast({ title: '下载失败', icon: 'none' })
    }
  })
  // #endif
  // #ifdef H5
  // H5 环境：使用 a 标签下载
  const a = document.createElement('a')
  a.href = url
  a.download = url.split('/').pop() || 'image.jpg'
  a.click()
  // #endif
}

const openFileUrl = (url) => {
  if (!url) return
  if (supportsBrowserDom()) {
    window.open(url, '_blank')
    return
  }
  downloadRemoteFileToLocalPath(url)
    .then(({ localPath }) => {
      if (localPath && typeof uni.openDocument === 'function') {
        uni.openDocument({
          filePath: localPath,
          showMenu: true,
          fail: () => {
            uni.setClipboardData({ data: url })
          },
        })
        return
      }
      uni.setClipboardData({ data: url })
    })
    .catch(() => {
      uni.setClipboardData({ data: url })
    })
}

const handleBubbleClick = (item) => {
  if (isImage(item) && item.fileUrl) {
    previewImage(item.fileUrl)
    return
  }
  if (isAudio(item) && item.fileUrl) {
    playAudioMessage(item)
    return
  }
  if (isFile(item) && item.fileUrl) {
    openFileUrl(item.fileUrl)
  }
}

// 判断是否为PC端
const isPC = () => {
  const platform = uni.getSystemInfoSync().platform.toLowerCase()
  return platform === 'windows' || platform === 'mac' || platform === 'linux'
}

// 处理右键菜单（PC端）
const handleContextMenu = (e, message) => {
  const { clientX, clientY } = e
  showMessageMenu(clientX, clientY, message)
}

// 处理长按菜单（移动端）
const handleLongPress = (e, message) => {
  let clientX, clientY
  if (e.touches && e.touches[0]) {
    clientX = e.touches[0].clientX
    clientY = e.touches[0].clientY
  } else if (e.detail && e.detail.clientX) {
    clientX = e.detail.clientX
    clientY = e.detail.clientY
  } else {
    // 使用元素的包围盒
    return
  }
  showMessageMenu(clientX, clientY, message)
}

// 显示消息操作菜单
const showMessageMenu = (clientX, clientY, message) => {
  const currentUserId = String(uni.getStorageSync('userId'))
  const isSelfMessage = String(message.senderId) === currentUserId
  const isRecalled = message.status === 3 || message.isRecalled === 1

  // 构建菜单项
  const menuItems = []

  // 根据消息类型添加菜单项
  if (!isRecalled) {
    // 文本消息：复制
    if (message.messageType === 1 && message.content) {
      menuItems.push({ label: '复制', key: 'copy' })
    }

    // 文件/图片：保存到云盘
    if ((message.messageType === 2 || message.messageType === 5) && message.fileUrl) {
      menuItems.push({ label: '保存到云盘', key: 'saveToCloud' })
    }

    // 转发
    menuItems.push({ label: '转发', key: 'forward' })

    // 回复（引用）
    if (isGroupChat.value) {
      menuItems.push({ label: '回复', key: 'quote' })
    }

    // 收藏
    menuItems.push({ label: '收藏', key: 'favorite' })

    // 撤回（仅自己发的消息）
    if (isSelfMessage) {
      menuItems.push({ label: '撤回', key: 'recall' })
    }
  }

  // 删除（自己和接收者都可以删除）
  if (isSelfMessage || String(message.receiverId) === currentUserId) {
    menuItems.push({ label: '删除', key: 'delete' })
  }

  if (menuItems.length === 0) return

  // 显示操作菜单
  uni.showActionSheet({
    itemList: menuItems.map(item => item.label),
    success: (res) => {
      const selected = menuItems[res.tapIndex]
      handleMenuAction(selected.key, message)
    }
  })
}

// 处理菜单操作
const handleMenuAction = async (action, message) => {
  switch (action) {
    case 'copy':
      uni.setClipboardData({ data: message.content })
      break

    case 'saveToCloud':
      handleSaveToCloud(message)
      break

    case 'forward':
      handleForwardMessage(message)
      break

    case 'quote':
      // 设置引用回复
      setQuoteMessage(message)
      // 聚焦到输入框
      nextTick(() => {
        const inputEl = document.querySelector('.composer-input')
        if (inputEl) inputEl.focus()
      })
      break

    case 'favorite':
      handleFavoriteMessage(message)
      break

    case 'recall':
      handleRecallMessage(message)
      break

    case 'delete':
      handleDeleteMessage(message)
      break
  }
}

// 保存到云盘
const handleSaveToCloud = async (message) => {
  uni.showLoading({ title: '保存中...' })
  try {
    const res = await uni.$http.post('/cloud/file/saveFromChat', {
      messageId: message.id,
      targetType: message.sessionType === 2 ? 'group' : 'personal',
      sessionId: message.sessionId
    })
    if (res.code === 200) {
      uni.showToast({ title: '已保存到云盘', icon: 'success' })
    }
  } finally {
    uni.hideLoading()
  }
}

// 转发消息
const handleForwardMessage = (message) => {
  // 触发转发事件，让父组件处理
  emit('forward-message', [message.id])
}

// 撤回消息
const handleRecallMessage = async (message) => {
  const res = await uni.$http.put('/chat/message/revoke', null, {
    params: { messageId: message.id }
  })
  if (res.code === 200) {
    uni.showToast({ title: '已撤回', icon: 'success' })
    emit('message-recalled', message.id)
  }
}

// 删除消息
const handleDeleteMessage = async (message) => {
  const res = await uni.$http.delete(`/chat/message/${message.id}`)
  if (res.code === 200) {
    uni.showToast({ title: '已删除', icon: 'success' })
    emit('message-deleted', message.id)
  }
}

// 收藏消息
const handleFavoriteMessage = async (message) => {
  try {
    const res = await uni.$http.post('/favorite/add', null, {
      params: {
        messageId: message.id,
        content: message.content || '',
        messageType: String(message.messageType || 1),
        fileUrl: message.fileUrl || '',
        senderId: message.senderId || null,
        sessionId: message.sessionId || null,
      },
    })
    if (res.code === 200) {
      uni.showToast({ title: '已收藏', icon: 'success' })
      emit('add-favorite', message)
    }
  } catch (e) {
    console.error('收藏消息失败', e)
    uni.showToast({ title: '收藏失败', icon: 'none' })
  }
}

const openSearchPanel = () => {
  showSearchPanel.value = true
}

const handleInfoAction = (type) => {
  if (type === 'search') {
    openSearchPanel()
    return
  }
  uni.showToast({
    title: Number(props.session?.sessionType) === 2 ? '成员管理待接入' : '联系人扩展待接入',
    icon: 'none',
  })
}

const handleHeaderTool = (type) => {
  if (type === 'search') {
    openSearchPanel()
    return
  }
  if (type === 'more') {
    emit('toggle-info')
  }
}

const closeSearchPanel = () => {
  showSearchPanel.value = false
  searchKeyword.value = ''
  searchTab.value = 'all'
}

const matchesSearchKeyword = (item) => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return true
  const source = [
    resolveSearchSender(item),
    resolveContent(item),
    item.fileName,
    formatSearchDateTime(item.sendTime || item.createdAt),
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
  return source.includes(keyword)
}

const matchesSearchTab = (item, tabKey) => {
  switch (tabKey) {
    case 'text':
      return !isVisualMessage(item) && !isAudio(item) && !isFile(item)
    case 'image':
      return isVisualMessage(item)
    case 'audio':
      return isAudio(item)
    case 'file':
      return isFile(item)
    default:
      return true
  }
}

const orderedMessages = computed(() => [...getChronologicalMessages()].reverse())

const searchResults = computed(() => {
  if (searchTab.value === 'date') return []
  return orderedMessages.value.filter(
    (item) => matchesSearchTab(item, searchTab.value) && matchesSearchKeyword(item),
  )
})

const searchDateResults = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  const grouped = new Map()

  orderedMessages.value.forEach((item) => {
    const rawTime = item.sendTime || item.createdAt
    const label = formatSearchDate(rawTime)
    if (!label) return
    if (keyword && !label.toLowerCase().includes(keyword)) return
    if (!grouped.has(label)) {
      grouped.set(label, {
        key: label,
        label,
        count: 0,
        message: item,
      })
    }
    grouped.get(label).count += 1
  })

  return [...grouped.values()]
})

const resolveSearchPreview = (item) => {
  if (isImage(item)) return item.fileName || '[图片]'
  if (isVideo(item)) return item.fileName || '[视频]'
  if (isAudio(item)) {
    return item.duration ? `[语音] ${formatAudioDuration(item.duration)}` : '[语音]'
  }
  if (isFile(item)) return item.fileName || '[文件]'
  return resolveContent(item)
}

const locateMessage = async (item) => {
  const targetId = `message-${messageKey(item)}`
  if (!targetId) return
  closeSearchPanel()
  isAtBottom.value = false
  showJumpToBottom.value = true
  setScrollTarget(targetId)
}

const handleInput = (event) => {
  const cursor = Number(event?.detail?.cursor)
  draftCursor.value = Number.isFinite(cursor) ? cursor : draft.value.length

  // 检测输入中是否包含 @ 触发选择器
  const text = draft.value
  if (isGroupChat.value) {
    const textBeforeCursor = text.substring(0, draftCursor.value)
    const atIndex = textBeforeCursor.lastIndexOf('@')
    if (atIndex >= 0) {
      // 检查 @ 后面是否有空格或其他分隔符
      const textAfterAt = textBeforeCursor.substring(atIndex + 1)
      if (!textAfterAt.includes(' ') && !textAfterAt.includes('\n')) {
        // @后面没有分隔符，可能正在输入用户名
        openAtPicker()
        atSearchKeyword.value = textAfterAt
        return
      }
    }
  }

  // 如果光标前有空格或其他字符，关闭 @ 选择器
  if (showAtPicker.value) {
    const textBeforeCursor = text.substring(0, draftCursor.value)
    if (!textBeforeCursor.endsWith('@')) {
      closeAtPicker()
    }
  }
}

const handleComposerKeydown = (event) => {
  if (props.mobile) return

  const nativeEvent = event?.detail?.event || event
  const key = nativeEvent?.key
  const keyCode = Number(nativeEvent?.keyCode ?? nativeEvent?.which)
  const isEnter = key === 'Enter' || keyCode === 13

  if (!isEnter) return
  if (nativeEvent?.shiftKey || nativeEvent?.ctrlKey || nativeEvent?.altKey || nativeEvent?.metaKey) {
    return
  }
  if (nativeEvent?.isComposing || keyCode === 229) {
    return
  }

  if (typeof nativeEvent?.preventDefault === 'function') {
    nativeEvent.preventDefault()
  }

  submitDraft()
}

const rememberEmoji = (emoji) => {
  const next = [emoji, ...recentEmojis.value.filter((item) => item !== emoji)].slice(0, 12)
  recentEmojis.value = next
  try {
    uni.setStorageSync(RECENT_EMOJI_STORAGE_KEY, next)
  } catch {
    // Ignore storage failures.
  }
}

const insertEmoji = (emoji) => {
  if (!emoji) return
  const cursor = draftCursor.value >= 0 ? draftCursor.value : draft.value.length
  draft.value = `${draft.value.slice(0, cursor)}${emoji}${draft.value.slice(cursor)}`
  draftCursor.value = cursor + emoji.length
  rememberEmoji(emoji)
}

// 切换到表情包Tab
const switchToStickerTab = async () => {
  emojiTab.value = 'sticker'
  if (!stickerList.value.length && !loadingStickers.value) {
    await loadStickers()
  }
}

// 加载收藏表情列表
const loadStickers = async () => {
  loadingStickers.value = true
  try {
    const res = await uni.$http.get('/sticker/list')
    if (res.code === 200 && res.data) {
      stickerList.value = res.data
    }
  } catch (e) {
    console.error('加载表情列表失败', e)
  } finally {
    loadingStickers.value = false
  }
}

// 插入收藏表情（作为特殊消息发送）
const insertSticker = (sticker) => {
  if (!sticker) return
  // 发送表情消息
  emit('send-sticker', sticker)
  showEmojiPanel.value = false
}

// 选择并添加收藏表情
const chooseStickerImage = () => {
  // #ifdef H5
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (file) {
      await uploadSticker(file)
    }
  }
  input.click()
  // #endif

  // #ifndef H5
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'], // 压缩图片，节省流量
    sourceType: ['album', 'camera'], // 相册和相机
    success: async (res) => {
      const filePath = res.tempFilePaths[0]
      await uploadStickerFromPath(filePath)
    }
  })
  // #endif
}

// 上传收藏表情（从File对象）
const uploadSticker = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('name', file.name || '表情')
  formData.append('category', 'custom')

  try {
    uni.showLoading({ title: '上传中...' })
    const res = await uni.$http.post('/sticker/upload', formData, {
      header: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.code === 200) {
      stickerList.value.unshift(res.data)
      uni.showToast({ title: '表情添加成功', icon: 'success' })
    }
  } catch (e) {
    uni.showToast({ title: '上传失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

// 上传收藏表情（从本地路径）
const uploadStickerFromPath = async (filePath) => {
  uni.showLoading({ title: '上传中...' })
  try {
    const res = await uni.uploadFile({
      url: `${uni.$http.baseUrl}/sticker/upload`,
      filePath,
      name: 'file',
      formData: { name: '表情', category: 'custom' },
      header: { satoken: uni.getStorageSync('satoken') }
    })
    const data = JSON.parse(res.data)
    if (data.code === 200) {
      stickerList.value.unshift(data.data)
      uni.showToast({ title: '表情添加成功', icon: 'success' })
    }
  } catch (e) {
    uni.showToast({ title: '上传失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

const toggleEmojiPanel = () => {
  if (isRecording.value) {
    cancelRecording({ silent: true })
  }
  showEmojiPanel.value = !showEmojiPanel.value
}

const stopAudioPlayback = () => {
  if (browserAudioPlayer) {
    browserAudioPlayer.pause()
    browserAudioPlayer.currentTime = 0
    browserAudioPlayer = null
  }
  if (appAudioPlayer) {
    appAudioPlayer.stop()
  }
  playingMessageKey.value = ''
}

const playAudioMessage = (item) => {
  if (!item?.fileUrl) return
  const key = messageKey(item)

  if (playingMessageKey.value === key) {
    stopAudioPlayback()
    return
  }

  stopAudioPlayback()

  if (supportsBrowserDom() && typeof Audio !== 'undefined') {
    const player = new Audio(item.fileUrl)
    browserAudioPlayer = player
    playingMessageKey.value = key
    player.onended = () => {
      if (browserAudioPlayer === player) {
        browserAudioPlayer = null
      }
      playingMessageKey.value = ''
    }
    player.onerror = () => {
      if (browserAudioPlayer === player) {
        browserAudioPlayer = null
      }
      playingMessageKey.value = ''
      uni.showToast({
        title: '璇煶鎾斁澶辫触',
        icon: 'none',
      })
    }
    player.play().catch(() => {
      browserAudioPlayer = null
      playingMessageKey.value = ''
      uni.showToast({
        title: '璇煶鎾斁澶辫触',
        icon: 'none',
      })
    })
    return
  }

  if (typeof uni.createInnerAudioContext === 'function') {
    if (!appAudioPlayer) {
      appAudioPlayer = uni.createInnerAudioContext()
      appAudioPlayer.obeyMuteSwitch = false
      appAudioPlayer.onEnded(() => {
        playingMessageKey.value = ''
      })
      appAudioPlayer.onStop(() => {
        playingMessageKey.value = ''
      })
      appAudioPlayer.onError(() => {
        playingMessageKey.value = ''
        uni.showToast({
          title: '璇煶鎾斁澶辫触',
          icon: 'none',
        })
      })
    }
    appAudioPlayer.src = item.fileUrl
    appAudioPlayer.play()
    playingMessageKey.value = key
    return
  }

  openFileUrl(item.fileUrl)
}

const startRecordingTicker = () => {
  if (recordingTimer) {
    clearInterval(recordingTimer)
  }
  recordingStartedAt = Date.now()
  recordingDuration.value = 0
  recordingTimer = setInterval(() => {
    recordingDuration.value = clampAudioDuration((Date.now() - recordingStartedAt) / 1000)
  }, 1000)
}

const stopRecordingTicker = () => {
  if (recordingTimer) {
    clearInterval(recordingTimer)
    recordingTimer = null
  }
}

const resetRecordingState = () => {
  isRecording.value = false
  stopRecordingTicker()
  recordingStartedAt = 0
  recordingDuration.value = 0
}

const getRecordingDuration = (fallbackMs = 0) => {
  if (recordingDuration.value > 0) {
    return clampAudioDuration(recordingDuration.value)
  }
  if (fallbackMs > 0) {
    return clampAudioDuration(fallbackMs / 1000)
  }
  if (recordingStartedAt > 0) {
    return clampAudioDuration((Date.now() - recordingStartedAt) / 1000)
  }
  return 1
}

const getVoiceFileName = (path = '') => {
  const cleanedPath = String(path || '').split('?')[0]
  const extension = cleanedPath.includes('.') ? cleanedPath.split('.').pop() : 'mp3'
  return `voice_${Date.now()}.${extension || 'mp3'}`
}

const getPreferredRecorderMimeType = () => {
  if (typeof MediaRecorder === 'undefined' || typeof MediaRecorder.isTypeSupported !== 'function') {
    return ''
  }
  const preferred = [
    'audio/webm;codecs=opus',
    'audio/webm',
    'audio/mp4',
    'audio/ogg;codecs=opus',
  ]
  return preferred.find((type) => MediaRecorder.isTypeSupported(type)) || ''
}

const buildBrowserAudioFile = (blob, mimeType = '') => {
  const type = mimeType || blob.type || 'audio/webm'
  const extension = type.includes('mp4') ? 'm4a' : type.includes('ogg') ? 'ogg' : 'webm'
  return new File([blob], `voice_${Date.now()}.${extension}`, { type })
}

const teardownBrowserRecorder = () => {
  if (browserAudioStream) {
    browserAudioStream.getTracks().forEach((track) => track.stop())
    browserAudioStream = null
  }
  browserMediaRecorder = null
  browserAudioChunks = []
}

const finishBrowserRecording = () => {
  const action = recordingStopAction
  const mimeType = browserMediaRecorder?.mimeType || getPreferredRecorderMimeType()
  const duration = getRecordingDuration()
  const blob =
    browserAudioChunks.length > 0 ? new Blob(browserAudioChunks, { type: mimeType || 'audio/webm' }) : null

  teardownBrowserRecorder()
  resetRecordingState()

  if (action !== 'send' || !blob || !blob.size) {
    return
  }

  emit('send-audio', {
    file: buildBrowserAudioFile(blob, mimeType),
    duration,
  })
}

const ensureRecorderManager = () => {
  if (recorderManager || typeof uni.getRecorderManager !== 'function') {
    return recorderManager
  }

  recorderManager = uni.getRecorderManager()
  if (!recorderListenersBound) {
    recorderManager.onStop((result = {}) => {
      const action = recordingStopAction
      const duration = getRecordingDuration(result.duration)
      const fileAsset = result.tempFilePath
        ? {
            uploadSource: result.tempFilePath,
            name: getVoiceFileName(result.tempFilePath),
            size: Number(result.fileSize || 0),
          }
        : null

      resetRecordingState()

      if (action !== 'send' || !fileAsset) {
        return
      }

      emit('send-audio', {
        file: fileAsset,
        duration,
      })
    })

    recorderManager.onError(() => {
      resetRecordingState()
      uni.showToast({
        title: '语音录制失败',
        icon: 'none',
      })
    })
    recorderListenersBound = true
  }

  return recorderManager
}

const startBrowserRecording = async () => {
  if (
    !supportsBrowserDom() ||
    typeof navigator === 'undefined' ||
    !navigator.mediaDevices?.getUserMedia ||
    typeof MediaRecorder === 'undefined'
  ) {
    throw new Error('当前浏览器不支持语音录制')
  }

  const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
  const mimeType = getPreferredRecorderMimeType()
  const recorder = mimeType ? new MediaRecorder(stream, { mimeType }) : new MediaRecorder(stream)

  browserAudioStream = stream
  browserMediaRecorder = recorder
  browserAudioChunks = []
  recordingStopAction = 'send'

  recorder.ondataavailable = (event) => {
    if (event.data && event.data.size > 0) {
      browserAudioChunks.push(event.data)
    }
  }

  recorder.onerror = () => {
    teardownBrowserRecorder()
    resetRecordingState()
    uni.showToast({
      title: '语音录制失败',
      icon: 'none',
    })
  }

  recorder.onstop = finishBrowserRecording

  recorder.start()
  isRecording.value = true
  startRecordingTicker()
}

const startAppRecording = async () => {
  if (isAppPlusRuntime()) {
    const permissionResult = await ensureAnyAppPermissionAccess([APP_PERMISSION_SCOPE.MICROPHONE], {
      title: '需要麦克风权限',
      content: '发送语音消息前，请先开启麦克风权限。',
    })
    if (!permissionResult.ok) {
      return
    }
  }

  const manager = ensureRecorderManager()
  if (!manager) {
    throw new Error('当前环境不支持语音录制')
  }

  recordingStopAction = 'send'
  manager.start({
    duration: MAX_AUDIO_DURATION * 1000,
    sampleRate: 16000,
    numberOfChannels: 1,
    encodeBitRate: 96000,
    format: 'mp3',
  })
  isRecording.value = true
  startRecordingTicker()
}

const startVoiceRecording = async () => {
  if (isRecording.value) return

  showEmojiPanel.value = false
  stopAudioPlayback()

  try {
    if (supportsBrowserDom()) {
      await startBrowserRecording()
      return
    }
    await startAppRecording()
  } catch (error) {
    resetRecordingState()
    uni.showToast({
      title: error?.message || '当前环境不支持语音录制',
      icon: 'none',
    })
  }
}

const stopRecordingCore = (action = 'send') => {
  if (!isRecording.value) return

  recordingStopAction = action
  stopRecordingTicker()

  if (browserMediaRecorder && browserMediaRecorder.state !== 'inactive') {
    browserMediaRecorder.stop()
    return
  }

  const manager = ensureRecorderManager()
  if (manager) {
    manager.stop()
    return
  }

  resetRecordingState()
}

const cancelRecording = ({ silent = false } = {}) => {
  if (!isRecording.value) return
  stopRecordingCore(silent ? 'discard' : 'cancel')
}

const sendRecordedVoice = () => {
  if (!isRecording.value) return
  stopRecordingCore('send')
}

const closeScreenshotPreview = () => {
  screenshotPreviewVisible.value = false
  screenshotPreviewUrl.value = ''
  screenshotLocalPath.value = ''
  screenshotFileName.value = ''
  screenshotType.value = ''
  screenshotSaving.value = false
}

const takeChatScreenshot = async () => {
  if (!props.session || screenshotSaving.value) {
    return
  }

  showEmojiPanel.value = false
  closeSearchPanel()

  uni.showLoading({
    title: '正在截图',
    mask: true,
  })

  try {
    const capture = await captureChatScreenshot({
      element: chatShellRef.value,
    })

    screenshotPreviewUrl.value = capture.previewUrl
    screenshotLocalPath.value = capture.localPath || ''
    screenshotFileName.value = capture.fileName || ''
    screenshotType.value = capture.type || ''
    screenshotPreviewVisible.value = true
  } catch (error) {
    uni.showToast({
      title: error?.message || '截图失败',
      icon: 'none',
    })
  } finally {
    uni.hideLoading()
  }
}

const saveScreenshotPreview = async () => {
  if (!screenshotPreviewUrl.value || screenshotSaving.value) {
    return
  }

  screenshotSaving.value = true

  try {
    if (isAppPlusRuntime()) {
      const permission = await ensureAnyAppPermissionAccess([APP_PERMISSION_SCOPE.ALBUM], {
        title: '需要相册权限',
        content: '开启相册权限后，才能将聊天截图保存到系统相册。',
      })

      if (!permission.ok) {
        return
      }
    }

    await saveCapturedScreenshot({
      type: screenshotType.value,
      previewUrl: screenshotPreviewUrl.value,
      localPath: screenshotLocalPath.value,
      fileName: screenshotFileName.value,
    })

    uni.showToast({
      title: isAppPlusRuntime() ? '已保存到相册' : '截图已下载',
      icon: 'none',
    })
  } catch (error) {
    uni.showToast({
      title: error?.message || '保存截图失败',
      icon: 'none',
    })
  } finally {
    screenshotSaving.value = false
  }
}

const handleToolbarAction = (type) => {
  if (type === 'emoji') {
    toggleEmojiPanel()
    return
  }
  if (type === 'cut') {
    void takeChatScreenshot()
    return
  }
  if (type === 'voice') {
    if (isRecording.value) {
      sendRecordedVoice()
      return
    }
    startVoiceRecording()
  }
}

const submitDraft = () => {
  const content = draft.value.trim()
  if (!content) return

  // 构建包含@提及和引用信息的消息数据
  const messageData = {
    content,
    atUserIds: selectedAtUsers.value.filter((id) => id !== '__at_all__'),
    isAtAll: selectedAtUsers.value.includes('__at_all__') ? 1 : 0,
    quoteMessageId: quoteMessage.value?.id || null,
  }

  emit('send-text', content, messageData)
  draft.value = ''
  draftCursor.value = -1
  showEmojiPanel.value = false
  selectedAtUsers.value = []
  clearQuote()
}
</script>

<style scoped>
/* ========== CSS Variables - WeChat Theme ========== */
.chat-area {
  --wechat-green: #07c160;
  --wechat-green-light: #95ec69;
  --wechat-bg: #ededed;
  --wechat-bg-chat: #f5f5f5;
  --wechat-bg-white: #ffffff;
  --wechat-bg-toolbar: #f7f7f7;
  --wechat-text: #191919;
  --wechat-text-secondary: #888888;
  --wechat-text-tertiary: #b2b2b2;
  --wechat-border: #e5e5e5;
  --wechat-divider: rgba(0, 0, 0, 0.08);
  --wechat-radius: 8px;
  --wechat-radius-lg: 12px;
  --wechat-radius-xl: 16px;
  --wechat-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--wechat-bg-chat);
}

.chat-shell {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: var(--wechat-bg-chat);
}

/* ========== Header ========== */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 56px;
  padding: 0 16px;
  background: var(--wechat-bg-white);
  border-bottom: 1px solid var(--wechat-divider);
}

.header-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

.mobile-back {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: var(--wechat-text);
  cursor: pointer;
}

.header-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--wechat-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-sub {
  font-size: 12px;
  color: var(--wechat-text-tertiary);
}

.header-tools {
  display: flex;
  align-items: center;
  gap: 4px;
}

.tool-btn {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: var(--wechat-radius);
}

.tool-btn:active {
  background: rgba(0, 0, 0, 0.05);
}

.tool-btn.active {
  background: rgba(7, 193, 96, 0.08);
}

.tool-icon {
  width: 24px;
  height: 24px;
  color: var(--wechat-text);
}

/* SVG Icons */
.icon-search-chat {
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23191919' stroke-width='2'%3E%3Ccircle cx='11' cy='11' r='8'/%3E%3Cpath d='m21 21-4.35-4.35'/%3E%3C/svg%3E") center/contain no-repeat;
}

.icon-phone {
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23191919' stroke-width='2'%3E%3Cpath d='M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72 12.84 12.84 0 00.7 2.81 2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45 12.84 12.84 0 002.81.7A2 2 0 0122 16.92z'/%3E%3C/svg%3E") center/contain no-repeat;
}

.icon-video {
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23191919' stroke-width='2'%3E%3Cpolygon points='23 7 16 12 23 17 23 7'/%3E%3Crect x='1' y='5' width='15' height='14' rx='2' ry='2'/%3E%3C/svg%3E") center/contain no-repeat;
}

.icon-dots {
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23191919'%3E%3Ccircle cx='12' cy='5' r='2'/%3E%3Ccircle cx='12' cy='12' r='2'/%3E%3Ccircle cx='12' cy='19' r='2'/%3E%3C/svg%3E") center/contain no-repeat;
}

/* ========== Chat Body ========== */
.chat-body {
  flex: 1;
  min-height: 0;
  display: grid;
  background: var(--wechat-bg-chat);
  position: relative;
}

.chat-body.with-info {
  grid-template-columns: minmax(0, 1fr) 300px;
}

.message-scroll {
  height: 100%;
  min-height: 0;
  position: relative;
  background: var(--wechat-bg-chat);
  -webkit-overflow-scrolling: touch; /* iOS 惯性滚动 */
  overscroll-behavior: contain; /* 防止滚动穿透 */
}

/* ========== 跳到最新按钮 ========== */
.jump-to-bottom {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  background: var(--wechat-green);
  color: #fff;
  border-radius: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  z-index: 100;
  animation: jump-to-bottom-enter 0.2s ease-out;
}

.jump-to-bottom:active {
  transform: translateX(-50%) scale(0.95);
}

.jump-to-bottom-icon {
  font-size: 16px;
  font-weight: bold;
}

.jump-to-bottom-text {
  font-size: 13px;
}

@keyframes jump-to-bottom-enter {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

.message-viewport {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  padding: 20px 24px 24px;
  box-sizing: border-box;
  background: var(--wechat-bg-chat);
}

.message-stream {
  display: flex;
  flex-direction: column;
  margin-top: auto;
}

.message-bottom-anchor {
  width: 100%;
  height: 2px;
}

.history-entry,
.state-block {
  display: flex;
  justify-content: center;
  padding: 12px 0 20px;
}

.state-block {
  flex: 1;
  align-items: center;
}

.message-divider {
  display: flex;
  justify-content: center;
  padding: 10px 0 18px;
}

.message-divider-text {
  font-size: 12px;
  color: var(--wechat-text-tertiary);
  background: rgba(0, 0, 0, 0.05);
  padding: 5px 12px;
  border-radius: 999px;
}

.history-btn {
  min-width: 140px;
  height: 32px;
  padding: 0 16px;
  border: none;
  border-radius: 16px;
  background: var(--wechat-bg-white);
  color: var(--wechat-text-secondary);
  font-size: 13px;
}

.state-text {
  font-size: 13px;
  color: var(--wechat-text-tertiary);
}

/* ========== Message Row ========== */
.message-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 18px;
  will-change: transform, opacity; /* 动画性能优化 */
}

/* 进入动画 */
.message-enter-active {
  transition: all 0.2s ease-out;
}

.message-row.self {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  background: #d8dee6;
  flex-shrink: 0;
}

.message-stack {
  max-width: min(620px, 72%);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sender-name {
  padding: 0 2px;
  font-size: 12px;
  color: var(--wechat-text-tertiary);
}

.bubble-wrap {
  display: flex;
}

.bubble-wrap.self {
  justify-content: flex-end;
}

/* ========== WeChat Style Bubbles ========== */
.message-bubble {
  max-width: 100%;
  padding: 10px 12px;
  border-radius: 4px;
  background: var(--wechat-bg-white);
  position: relative;
  box-shadow: none;
}

/* Triangle tail - left side */
.message-bubble::before {
  content: '';
  position: absolute;
  top: 12px;
  left: -7px;
  width: 0;
  height: 0;
  border-top: 5px solid transparent;
  border-bottom: 5px solid transparent;
  border-right: 7px solid var(--wechat-bg-white);
}

.self-bubble {
  background: var(--wechat-green-light);
}

.self-bubble::before {
  left: auto;
  right: -7px;
  border-right: none;
  border-left: 7px solid var(--wechat-green-light);
}

.visual-bubble {
  padding: 0;
  background: transparent;
  border-radius: 10px;
  overflow: hidden;
}

.visual-bubble::before {
  display: none;
}

.file-bubble {
  min-width: 200px;
}

.pending-bubble {
  opacity: 0.7;
}

.failed-bubble {
  background: #fff0f0;
}

/* ========== Bubble Content ========== */
.bubble-text {
  font-size: 14px;
  line-height: 1.45;
  color: var(--wechat-text);
  word-break: break-word;
}

.self-bubble .bubble-text {
  color: var(--wechat-text);
}

.bubble-image {
  display: block;
  max-width: 220px;
  max-height: 240px;
  border-radius: 10px;
  object-fit: cover;
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.bubble-image:active {
  opacity: 0.8;
  transform: scale(0.99);
}

/* Audio Bubble */
.audio-bubble {
  min-width: 120px;
  cursor: pointer;
}

.audio-card {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.audio-wave {
  display: inline-flex;
  align-items: flex-end;
  gap: 3px;
  min-width: 28px;
  height: 20px;
}

.wave-bar {
  width: 3px;
  border-radius: 2px;
  background: var(--wechat-text-secondary);
}

.wave-bar:nth-child(1) { height: 10px; }
.wave-bar:nth-child(2) { height: 16px; }
.wave-bar:nth-child(3) { height: 12px; }

.self-bubble .wave-bar {
  background: rgba(17, 24, 39, 0.5);
}

.audio-card.playing .wave-bar {
  animation: wavePulse 0.8s ease-in-out infinite;
}

.audio-card.playing .wave-bar:nth-child(2) {
  animation-delay: 0.1s;
}

.audio-card.playing .wave-bar:nth-child(3) {
  animation-delay: 0.2s;
}

.audio-duration {
  font-size: 14px;
  font-weight: 500;
  color: var(--wechat-text);
}

/* File Bubble */
.file-card {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 180px;
}

.file-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--wechat-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  font-size: 12px;
  color: var(--wechat-text-tertiary);
}

.file-arrow {
  font-size: 18px;
  color: var(--wechat-text-tertiary);
}

/* Message Meta */
.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 6px;
  min-height: 18px;
}

.message-meta.self {
  justify-content: flex-end;
}

.meta-time,
.meta-state {
  font-size: 11px;
}

.meta-time,
.meta-state.pending,
.meta-state.failed {
  color: var(--wechat-text-tertiary);
}

.meta-action {
  font-size: 11px;
  color: var(--wechat-green);
}

/* Quote & At styles */
.quote-inline {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 6px 10px;
  background: rgba(7, 193, 96, 0.08);
  border-left: 2px solid var(--wechat-green);
  border-radius: 4px;
  margin-bottom: 6px;
}

.quote-inline-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--wechat-green);
}

.quote-inline-content {
  font-size: 13px;
  color: var(--wechat-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.at-highlight {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  background: rgba(245, 108, 108, 0.1);
  border-radius: 4px;
  margin-bottom: 4px;
}

.at-highlight-text {
  font-size: 12px;
  font-weight: 600;
  color: #f56c6c;
}

/* ========== Info Panel ========== */
.info-panel {
  padding: 20px 16px;
  background: var(--wechat-bg-white);
  border-left: 1px solid var(--wechat-divider);
}

.info-panel.mobile {
  position: absolute;
  top: 60px;
  right: 12px;
  left: 12px;
  border-left: none;
  border-radius: var(--wechat-radius-xl);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

.info-members {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--wechat-divider);
}

.member-card {
  width: 64px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.member-avatar,
.member-add {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
}

.member-add {
  border: 1px dashed #c7ccd3;
  background: var(--wechat-bg-white);
  color: var(--wechat-text-secondary);
  font-size: 24px;
}

.member-name {
  width: 100%;
  text-align: center;
  font-size: 11px;
  color: var(--wechat-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.info-list {
  padding-top: 12px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 52px;
  border-bottom: 1px solid var(--wechat-divider);
}

.info-row.clickable:active {
  background: rgba(0, 0, 0, 0.03);
}

.info-label {
  font-size: 14px;
  color: var(--wechat-text);
}

.info-arrow {
  font-size: 16px;
  color: var(--wechat-text-tertiary);
}

/* WeChat Style Switch */
.switch {
  width: 48px;
  height: 28px;
  padding: 3px;
  border-radius: 14px;
  background: var(--wechat-border);
  display: flex;
  align-items: center;
  transition: background 0.2s;
}

.switch.active {
  background: var(--wechat-green);
}

.switch-knob {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--wechat-bg-white);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
  transition: transform 0.2s;
}

.switch.active .switch-knob {
  transform: translateX(20px);
}

.danger-row {
  padding: 20px 0;
  text-align: center;
  font-size: 14px;
  color: #fa5151;
  margin-top: 12px;
}

.danger-row:active {
  background: rgba(250, 81, 81, 0.05);
}

/* ========== Search Panel ========== */
.search-panel-mask {
  position: absolute;
  inset: 0;
  z-index: 40;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 12px;
  background: rgba(0, 0, 0, 0.1);
}

.search-panel {
  width: min(720px, 100%);
  max-height: calc(100% - 24px);
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 20px;
  border-radius: var(--wechat-radius-xl);
  background: var(--wechat-bg-white);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.15);
}

.search-panel.mobile {
  width: 100%;
  max-height: calc(100% - 16px);
  padding: 16px;
  border-radius: var(--wechat-radius-lg);
}

.search-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.search-panel-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--wechat-text);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-panel-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: var(--wechat-text-tertiary);
  cursor: pointer;
}

.search-input-shell {
  position: relative;
  margin-top: 16px;
}

.search-input-shell::before {
  content: '';
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  width: 16px;
  height: 16px;
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23b2b2b2' stroke-width='2'%3E%3Ccircle cx='11' cy='11' r='8'/%3E%3Cpath d='m21 21-4.35-4.35'/%3E%3C/svg%3E") center/contain no-repeat;
}

.search-panel-input {
  width: 100%;
  height: 44px;
  padding: 0 16px 0 44px;
  border-radius: 8px;
  border: 1px solid var(--wechat-border);
  background: var(--wechat-bg);
  font-size: 15px;
  color: var(--wechat-text);
  outline: none;
}

.search-panel-input:focus {
  border-color: var(--wechat-green);
}

.search-tabs {
  margin-top: 14px;
  white-space: nowrap;
}

.search-tabs-row {
  display: inline-flex;
  align-items: center;
  gap: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--wechat-divider);
}

.search-tab {
  position: relative;
  padding: 4px 0;
  font-size: 14px;
  color: var(--wechat-text-secondary);
}

.search-tab.active {
  color: var(--wechat-text);
  font-weight: 600;
}

.search-tab.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -13px;
  height: 3px;
  border-radius: 2px;
  background: var(--wechat-green);
}

.search-results {
  flex: 1;
  min-height: 0;
  padding-top: 12px;
}

.search-result-list {
  display: flex;
  flex-direction: column;
}

.search-result-item,
.search-date-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--wechat-divider);
}

.search-result-avatar {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  background: var(--wechat-bg);
  flex-shrink: 0;
}

.search-result-main,
.search-date-copy {
  flex: 1;
  min-width: 0;
}

.search-result-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.search-result-name,
.search-date-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--wechat-text);
}

.search-result-time,
.search-date-meta {
  font-size: 12px;
  color: var(--wechat-text-tertiary);
}

.search-result-text {
  margin-top: 6px;
  font-size: 14px;
  line-height: 1.5;
  color: var(--wechat-text-secondary);
  word-break: break-word;
}

.search-result-media {
  margin-top: 8px;
}

.search-result-thumb {
  max-width: 200px;
  height: 120px;
  border-radius: var(--wechat-radius);
  object-fit: cover;
}

.search-result-link {
  flex-shrink: 0;
  padding-top: 2px;
  font-size: 14px;
  color: var(--wechat-green);
}

.search-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 200px;
  text-align: center;
}

.search-empty-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--wechat-text);
}

.search-empty-sub {
  font-size: 13px;
  color: var(--wechat-text-tertiary);
}

/* ========== Capture Panel ========== */
.capture-mask {
  position: absolute;
  inset: 0;
  z-index: 46;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.5);
}

.capture-panel {
  width: min(680px, 100%);
  max-height: calc(100% - 32px);
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  border-radius: var(--wechat-radius-xl);
  background: var(--wechat-bg-white);
}

.capture-panel.mobile {
  width: 100%;
  max-height: calc(100% - 16px);
  padding: 16px;
  border-radius: var(--wechat-radius-lg);
}

.capture-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.capture-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--wechat-text);
}

.capture-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: var(--wechat-text-tertiary);
  cursor: pointer;
}

.capture-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  border-radius: var(--wechat-radius-lg);
  background: var(--wechat-bg);
}

.capture-image {
  display: block;
  width: 100%;
  border-radius: var(--wechat-radius);
}

.capture-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.capture-btn {
  min-width: 100px;
  height: 44px;
  padding: 0 18px;
  border-radius: var(--wechat-radius);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.capture-btn.ghost {
  color: var(--wechat-text-secondary);
  background: var(--wechat-bg);
}

.capture-btn.primary {
  color: var(--wechat-bg-white);
  background: var(--wechat-green);
}

.capture-btn.disabled {
  opacity: 0.5;
  pointer-events: none;
}

/* ========== Composer (Input Area) ========== */
.composer {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 208px;
  padding: 10px 16px calc(14px + env(safe-area-inset-bottom, 0px));
  background: var(--wechat-bg-white);
  border-top: 1px solid var(--wechat-divider);
  transform: translateZ(0); /* 启用硬件加速 */
}

.recording-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  padding: 14px 16px;
  border-radius: var(--wechat-radius-lg);
  background: rgba(7, 193, 96, 0.1);
}

.recording-main {
  display: flex;
  align-items: center;
  gap: 10px;
}

.recording-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #fa5151;
  animation: pulseDot 1s ease-in-out infinite;
}

.recording-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--wechat-green);
}

.recording-actions {
  display: flex;
  gap: 10px;
}

.recording-action {
  min-width: 70px;
  height: 34px;
  padding: 0 16px;
  border-radius: 17px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.recording-action.secondary {
  background: var(--wechat-bg-white);
  color: var(--wechat-text);
}

.recording-action.primary {
  background: var(--wechat-green);
  color: var(--wechat-bg-white);
}

/* Toolbar */
.composer-toolbar {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 42px;
  padding: 0 2px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.toolbar-right {
  margin-left: auto;
}

.toolbar-btn {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: var(--wechat-radius);
}

.toolbar-btn:active {
  background: rgba(0, 0, 0, 0.05);
}

.toolbar-btn.call-btn {
  width: 48px;
  height: 44px;
}

.toolbar-icon {
  width: 24px;
  height: 24px;
}

.icon-smile {
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23191919' stroke-width='2'%3E%3Ccircle cx='12' cy='12' r='10'/%3E%3Cpath d='M8 14s1.5 2 4 2 4-2 4-2'/%3E%3Cline x1='9' y1='9' x2='9.01' y2='9'/%3E%3Cline x1='15' y1='9' x2='15.01' y2='9'/%3E%3C/svg%3E") center/contain no-repeat;
}

.icon-image {
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23191919' stroke-width='2'%3E%3Crect x='3' y='3' width='18' height='18' rx='2' ry='2'/%3E%3Ccircle cx='8.5' cy='8.5' r='1.5'/%3E%3Cpolyline points='21 15 16 10 5 21'/%3E%3C/svg%3E") center/contain no-repeat;
}

.icon-folder {
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23191919' stroke-width='2'%3E%3Cpath d='M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z'/%3E%3C/svg%3E") center/contain no-repeat;
}

.icon-scissors {
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23191919' stroke-width='2'%3E%3Ccircle cx='6' cy='6' r='3'/%3E%3Ccircle cx='6' cy='18' r='3'/%3E%3Cline x1='20' y1='4' x2='8.12' y2='15.88'/%3E%3Cline x1='14.47' y1='14.48' x2='20' y2='20'/%3E%3Cline x1='8.12' y1='8.12' x2='12' y2='12'/%3E%3C/svg%3E") center/contain no-repeat;
}

.icon-mic {
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23191919' stroke-width='2'%3E%3Cpath d='M12 1a3 3 0 00-3 3v8a3 3 0 006 0V4a3 3 0 00-3-3z'/%3E%3Cpath d='M19 10v2a7 7 0 01-14 0v-2'/%3E%3Cline x1='12' y1='19' x2='12' y2='23'/%3E%3Cline x1='8' y1='23' x2='16' y2='23'/%3E%3C/svg%3E") center/contain no-repeat;
}

/* Input Row */
.composer-input-row {
  display: flex;
  align-items: stretch;
  gap: 12px;
  flex: 1;
  min-height: 172px;
  padding-top: 10px;
  border-top: 1px solid rgba(0, 0, 0, 0.04);
}

.composer-editor {
  flex: 1;
  width: 100%;
  min-height: 172px;
  padding: 10px 2px 2px;
  box-sizing: border-box;
  display: flex;
  align-items: stretch;
}

.composer-input {
  flex: 1;
  width: 100%;
  height: 100%;
  min-height: 156px;
  max-height: none;
  padding: 2px 6px 0 2px;
  border: none;
  border-radius: 0;
  background: transparent;
  box-sizing: border-box;
  font-size: 16px;
  line-height: 1.8;
  color: var(--wechat-text);
  outline: none;
  resize: none;
  overflow-y: auto;
  appearance: none;
}

.composer-input::placeholder {
  color: var(--wechat-text-tertiary);
}

.composer-input :deep(.uni-textarea-wrapper),
.composer-input :deep(.uni-textarea-textarea),
.composer-input :deep(.uni-textarea-placeholder) {
  min-height: 156px;
  height: 100% !important;
  box-sizing: border-box;
}

.composer-input :deep(.uni-textarea-wrapper) {
  overflow-y: auto;
}

.composer-input :deep(.uni-textarea-textarea) {
  padding: 2px 6px 0 2px;
}

.composer-input :deep(.uni-textarea-placeholder) {
  padding: 2px 6px 0 2px;
}

.send-btn {
  align-self: flex-end;
  min-width: 88px;
  height: 40px;
  padding: 0 20px;
  border: none;
  border-radius: var(--wechat-radius);
  background: var(--wechat-green);
  color: var(--wechat-bg-white);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, opacity 0.2s ease;
}

.send-btn.busy {
  opacity: 0.72;
  pointer-events: none;
}

.send-btn[disabled] {
  background: var(--wechat-border);
  color: var(--wechat-text-tertiary);
}

/* ========== Emoji Panel ========== */
.emoji-panel {
  margin-top: 10px;
  padding: 12px;
  border-radius: var(--wechat-radius-lg);
  background: var(--wechat-bg-white);
  border: 1px solid var(--wechat-divider);
}

.emoji-section + .emoji-section {
  margin-top: 14px;
}

.emoji-section-title {
  display: block;
  margin-bottom: 10px;
  font-size: 12px;
  color: var(--wechat-text-tertiary);
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(36px, 1fr));
  gap: 8px;
}

.emoji-cell {
  height: 36px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.emoji-cell:active {
  background: var(--wechat-bg);
}

.emoji-char {
  font-size: 22px;
  line-height: 1;
}

/* Emoji Tabs */
.emoji-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--wechat-divider);
}

.emoji-tab {
  padding: 6px 14px;
  font-size: 14px;
  color: var(--wechat-text-secondary);
  border-radius: 6px;
  cursor: pointer;
}

.emoji-tab.active {
  color: var(--wechat-green);
  background: rgba(7, 193, 96, 0.1);
  font-weight: 600;
}

/* Sticker Panel */
.sticker-panel {
  min-height: 100px;
}

.sticker-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.sticker-cell {
  aspect-ratio: 1;
  border-radius: var(--wechat-radius);
  overflow: hidden;
  background: var(--wechat-bg);
  cursor: pointer;
}

.sticker-cell:active {
  opacity: 0.8;
}

.sticker-cell.add-sticker {
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed var(--wechat-border);
}

.sticker-image {
  width: 100%;
  height: 100%;
}

.sticker-add-icon {
  font-size: 28px;
  color: var(--wechat-text-tertiary);
}

.sticker-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px;
}

.sticker-empty-text {
  font-size: 13px;
  color: var(--wechat-text-tertiary);
}

.sticker-add-btn {
  padding: 8px 16px;
  font-size: 14px;
  color: var(--wechat-green);
  background: rgba(7, 193, 96, 0.1);
  border-radius: 6px;
}

/* ========== At Picker ========== */
.at-picker {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  max-height: 280px;
  background: var(--wechat-bg-white);
  border-radius: var(--wechat-radius-lg);
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.08);
  z-index: 100;
  display: flex;
  flex-direction: column;
}

.at-picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--wechat-divider);
}

.at-picker-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--wechat-text);
}

.at-picker-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: var(--wechat-text-tertiary);
}

.at-search-input {
  margin: 10px 14px;
  padding: 0 12px;
  height: 40px;
  background: var(--wechat-bg);
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  border: none;
}

.at-member-list {
  flex: 1;
  min-height: 0;
}

.at-member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
}

.at-member-item:active {
  background: var(--wechat-bg);
}

.at-member-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--wechat-bg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.at-member-avatar.all-avatar {
  background: linear-gradient(135deg, #07c160, #95ec69);
  color: var(--wechat-bg-white);
  font-size: 16px;
  font-weight: 700;
}

.at-member-name {
  font-size: 15px;
  color: var(--wechat-text);
}

.at-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.at-empty-text {
  font-size: 14px;
  color: var(--wechat-text-tertiary);
}

/* ========== Quote Bubble ========== */
.quote-bubble {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  background: var(--wechat-bg);
  border-left: 3px solid var(--wechat-green);
  border-radius: 4px;
  margin-bottom: 6px;
}

.quote-info {
  flex: 1;
  min-width: 0;
}

.quote-label {
  font-size: 12px;
  color: var(--wechat-green);
  font-weight: 600;
}

.quote-content {
  margin-top: 4px;
  font-size: 13px;
  color: var(--wechat-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quote-close {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: var(--wechat-text-tertiary);
  cursor: pointer;
}

/* ========== Chat Empty ========== */
.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  height: 100%;
  color: var(--wechat-text-tertiary);
  text-align: center;
  background: var(--wechat-bg-chat);
}

.chat-empty-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--wechat-text);
}

.chat-empty-sub {
  font-size: 13px;
  line-height: 1.6;
}

/* ========== Animations ========== */
@keyframes pulseDot {
  0%, 100% {
    opacity: 0.4;
    transform: scale(0.85);
  }
  50% {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes wavePulse {
  0%, 100% {
    transform: scaleY(0.6);
    opacity: 0.6;
  }
  50% {
    transform: scaleY(1);
    opacity: 1;
  }
}

/* ========== Mobile Overrides ========== */
.chat-area.mobile .chat-header {
  min-height: 50px;
  padding: 0 12px;
}

.chat-area.mobile .header-title {
  font-size: 18px;
}

.chat-area.mobile .tool-btn {
  width: 44px;
  height: 44px;
}

.chat-area.mobile .tool-icon {
  width: 24px;
  height: 24px;
}

.chat-area.mobile .message-viewport {
  padding: 14px 12px 24px;
}

.chat-area.mobile .message-stack {
  max-width: 80%;
}

.chat-area.mobile .message-avatar {
  width: 34px;
  height: 34px;
}

.chat-area.mobile .bubble-text {
  font-size: 14px;
}

.chat-area.mobile .bubble-image {
  max-width: 190px;
  max-height: 190px;
}

.chat-area.mobile .composer {
  min-height: 0;
  padding: 8px 10px calc(8px + env(safe-area-inset-bottom, 0px));
  background: var(--wechat-bg-toolbar);
}

.chat-area.mobile .composer-input-row {
  min-height: 0;
  padding-top: 8px;
  border-top: none;
  align-items: flex-end;
}

.chat-area.mobile .composer-editor {
  min-height: 0;
  padding: 0;
}

.chat-area.mobile .composer-input {
  min-height: 40px;
  max-height: 100px;
  height: auto;
  padding: 10px 12px;
  border-radius: var(--wechat-radius-lg);
  background: var(--wechat-bg-white);
  font-size: 16px;
  line-height: 1.5;
}

.chat-area.mobile .composer-input :deep(.uni-textarea-wrapper),
.chat-area.mobile .composer-input :deep(.uni-textarea-textarea),
.chat-area.mobile .composer-input :deep(.uni-textarea-placeholder) {
  min-height: 40px;
  height: auto !important;
}

.chat-area.mobile .composer-input :deep(.uni-textarea-textarea),
.chat-area.mobile .composer-input :deep(.uni-textarea-placeholder) {
  padding: 10px 12px;
}

.chat-area.mobile .send-btn {
  min-width: 64px;
  height: 38px;
  font-size: 14px;
}

.chat-area.mobile .composer-toolbar {
  gap: 2px;
}

.chat-area.mobile .toolbar-btn {
  width: 44px;
  height: 44px;
}

.chat-area.mobile .toolbar-icon {
  width: 24px;
  height: 24px;
}

.chat-area.mobile .recording-strip {
  flex-direction: column;
  align-items: stretch;
}

.chat-area.mobile .recording-actions {
  justify-content: flex-end;
  margin-top: 10px;
}

.chat-area.mobile .emoji-panel {
  margin: 8px 0 0;
  padding: 10px;
}

.chat-area.mobile .emoji-cell {
  height: 36px;
}

.chat-area.mobile .emoji-char {
  font-size: 22px;
}

.chat-area.mobile .search-panel-mask {
  padding: 8px;
}

.chat-area.mobile .capture-mask {
  padding: 12px;
}

.chat-area.mobile .info-panel.mobile {
  top: 56px;
  right: 8px;
  left: 8px;
}

.chat-area.mobile .at-picker {
  max-height: 260px;
}
</style>
