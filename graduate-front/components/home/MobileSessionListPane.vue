<template>
  <view class="mobile-session-pane">
    <view class="pane-hero">
      <view class="hero-copy">
        <text class="hero-title">消息</text>
        <text class="hero-subtitle">{{ unreadCount }} 条未读，最近聊天会优先靠前。</text>
      </view>
      <text class="hero-action" @click="$emit('refresh')">刷新</text>
    </view>

    <view class="pane-search-card">
      <view class="search-shell">
        <text class="search-icon">⌕</text>
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索聊天"
          confirm-type="search"
        />
      </view>

      <view class="filter-row">
        <view
          v-for="item in filters"
          :key="item.key"
          class="filter-chip"
          :class="{ active: filterMode === item.key }"
          @click="filterMode = item.key"
        >
          <text>{{ item.label }}</text>
          <text v-if="item.key === 'unread' && unreadCount" class="filter-badge">
            {{ unreadCount > 99 ? '99+' : unreadCount }}
          </text>
        </view>
      </view>
    </view>

    <scroll-view
      class="pane-scroll"
      scroll-y
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onPullRefresh"
    >
      <view
        v-for="session in filteredSessions"
        :key="session.sessionId"
        class="session-row"
        :class="{ active: session.sessionId === activeSessionId }"
        @click="$emit('select-session', session)"
      >
        <view class="avatar-wrap">
          <image class="session-avatar" :src="session.sessionAvatar || defaultAvatar" mode="aspectFill" />
          <text v-if="session.unreadCount" class="unread-badge">
            {{ session.unreadCount > 99 ? '99+' : session.unreadCount }}
          </text>
        </view>

        <view class="row-main">
          <view class="row-head">
            <text class="row-title">{{ session.sessionName || '未命名会话' }}</text>
            <text class="row-time">{{ formatTime(session.lastMessageTime) }}</text>
          </view>

          <view class="row-foot">
            <text class="row-preview">{{ session.lastMessageContent || '暂无消息' }}</text>
            <view class="row-tags">
              <text v-if="session.isTop === 1" class="row-tag top">置顶</text>
              <text v-if="session.isMute === 1" class="row-tag">免打扰</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="loading" class="empty-block">
        <text class="empty-title">消息加载中</text>
        <text class="empty-text">请稍等一下。</text>
      </view>

      <view v-else-if="!filteredSessions.length" class="empty-block">
        <text class="empty-title">{{ emptyTitle }}</text>
        <text class="empty-text">{{ emptySubtitle }}</text>
      </view>

      <view v-if="hasMore" class="load-more">
        <button class="load-more-btn" @click="$emit('load-more')">加载更多</button>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { DEFAULT_AVATAR as defaultAvatar } from '@/utils/common'

const props = defineProps({
  sessions: { type: Array, default: () => [] },
  activeSessionId: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  hasMore: { type: Boolean, default: false },
})

const emit = defineEmits(['select-session', 'load-more', 'refresh'])

const isRefreshing = ref(false)

const onPullRefresh = async () => {
  isRefreshing.value = true
  emit('refresh')
  // 最少显示刷新动画 600ms
  setTimeout(() => {
    isRefreshing.value = false
  }, 600)
}

const filters = [
  { key: 'all', label: '全部' },
  { key: 'unread', label: '未读' },
]

const keyword = ref('')
const filterMode = ref('all')

const unreadCount = computed(() =>
  props.sessions.reduce((total, item) => total + Number(item.unreadCount || 0), 0),
)

const filteredSessions = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  const source =
    filterMode.value === 'unread'
      ? props.sessions.filter((item) => Number(item.unreadCount || 0) > 0)
      : props.sessions

  if (!query) return source
  return source.filter((item) =>
    `${item.sessionName || ''}${item.lastMessageContent || ''}`.toLowerCase().includes(query),
  )
})

const emptyTitle = computed(() => (filterMode.value === 'unread' ? '没有未读消息' : '还没有聊天记录'))
const emptySubtitle = computed(() =>
  filterMode.value === 'unread' ? '所有会话都已经读完了。' : '从联系人里点开任何好友或群聊，就会在这里出现。',
)

const formatTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const isToday = now.toDateString() === date.toDateString()
  if (isToday) {
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }
  return `${date.getMonth() + 1}/${date.getDate()}`
}
</script>

<style scoped>
/* Disable text selection on mobile */
text {
  user-select: none;
  -webkit-user-select: none;
}

.mobile-session-pane {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f6f7;
}

.pane-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
  padding: 22rpx 18rpx 14rpx;
}

.hero-title {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #111827;
}

.hero-subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  color: #6b7280;
}

.hero-action {
  min-width: 94rpx;
  height: 60rpx;
  padding: 0 18rpx;
  border-radius: 18rpx;
  background: rgba(15, 23, 42, 0.05);
  color: #111827;
  font-size: 22rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.pane-search-card {
  margin: 0 18rpx 18rpx;
  padding: 18rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12rpx 30rpx rgba(15, 23, 42, 0.06);
}

.search-shell {
  display: flex;
  align-items: center;
  gap: 12rpx;
  height: 72rpx;
  padding: 0 20rpx;
  border-radius: 20rpx;
  background: rgba(243, 244, 246, 0.94);
}

.search-icon {
  font-size: 24rpx;
  color: #9ca3af;
}

.search-input {
  flex: 1;
  font-size: 26rpx;
  color: #111827;
}

.filter-row {
  display: flex;
  gap: 12rpx;
  margin-top: 16rpx;
}

.filter-chip {
  min-width: 100rpx;
  height: 52rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: rgba(15, 23, 42, 0.05);
  color: #6b7280;
  font-size: 22rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  transition: background-color 0.1s;
}

.filter-chip:active {
  background-color: rgba(0, 0, 0, 0.08);
}

.filter-chip.active {
  background: rgba(7, 193, 96, 0.14);
  color: #07c160;
}

.filter-badge {
  min-width: 30rpx;
  height: 30rpx;
  padding: 0 8rpx;
  border-radius: 999rpx;
  background: #fa5151;
  color: #ffffff;
  font-size: 18rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.pane-scroll {
  flex: 1;
  min-height: 0;
  padding: 0 14rpx calc(22rpx + env(safe-area-inset-bottom, 0px));
}

.session-row {
  display: flex;
  gap: 16rpx;
  align-items: center;
  padding: 18rpx;
  margin-bottom: 12rpx;
  border-radius: 26rpx;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 10rpx 22rpx rgba(15, 23, 42, 0.05);
  transition: background-color 0.1s, transform 0.1s;
}

.session-row:active {
  background-color: rgba(0, 0, 0, 0.05);
  transform: scale(0.99);
}

.session-row.active {
  background: #ecfff4;
  box-shadow: 0 12rpx 26rpx rgba(7, 193, 96, 0.12);
}

.avatar-wrap {
  position: relative;
}

.session-avatar {
  width: 92rpx;
  height: 92rpx;
  border-radius: 26rpx;
  background: #d1d5db;
}

.unread-badge {
  position: absolute;
  top: -8rpx;
  right: -10rpx;
  min-width: 34rpx;
  height: 34rpx;
  padding: 0 10rpx;
  border-radius: 17rpx;
  background: #fa5151;
  color: #fff;
  font-size: 20rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.row-main {
  flex: 1;
  min-width: 0;
}

.row-head,
.row-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14rpx;
}

.row-head {
  margin-bottom: 8rpx;
}

.row-title {
  flex: 1;
  min-width: 0;
  font-size: 30rpx;
  font-weight: 700;
  color: #111827;
}

.row-time {
  font-size: 22rpx;
  color: #94a3b8;
}

.row-preview {
  flex: 1;
  min-width: 0;
  font-size: 24rpx;
  color: #6b7280;
}

.row-tags {
  display: flex;
  gap: 10rpx;
}

.row-tag {
  padding: 6rpx 10rpx;
  border-radius: 999rpx;
  background: rgba(15, 23, 42, 0.06);
  color: #64748b;
  font-size: 20rpx;
}

.row-tag.top {
  background: rgba(7, 193, 96, 0.14);
  color: #07c160;
}

.empty-block,
.load-more {
  padding: 34rpx 12rpx;
  text-align: center;
}

.empty-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #111827;
  margin-bottom: 10rpx;
}

.empty-text {
  font-size: 24rpx;
  color: #94a3b8;
}

.load-more-btn {
  width: 100%;
  height: 76rpx;
  border-radius: 22rpx;
  background: #ffffff;
  color: #374151;
  font-size: 25rpx;
  transition: all 0.1s;
}

.load-more-btn:active {
  opacity: 0.7;
  transform: scale(0.98);
}
</style>
