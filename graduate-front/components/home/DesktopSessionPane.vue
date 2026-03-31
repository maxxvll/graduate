<template>
  <DesktopSideShell>
    <template #toolbar>
      <view class="toolbar-row">
        <view class="search-shell">
          <text class="search-icon">⌕</text>
          <input
            v-model="keyword"
            class="search-input"
            placeholder="搜索"
            confirm-type="search"
          />
        </view>

        <view class="toolbar-plus" @click="$emit('refresh')">
          <text class="plus-text">+</text>
        </view>
      </view>
    </template>

    <view
      v-for="session in filteredSessions"
      :key="session.sessionId"
      class="session-item"
      :class="{ active: session.sessionId === activeSessionId }"
      @click="$emit('select-session', session)"
    >
      <view class="avatar-wrap">
        <image class="session-avatar" :src="session.sessionAvatar || defaultAvatar" mode="aspectFill" />
        <text v-if="session.unreadCount" class="unread-badge">
          {{ session.unreadCount > 99 ? '99+' : session.unreadCount }}
        </text>
      </view>

      <view class="session-main">
        <view class="session-top">
          <text class="session-name">{{ session.sessionName || '未命名会话' }}</text>
          <text class="session-time">{{ formatTime(session.lastMessageTime) }}</text>
        </view>

        <view class="session-bottom">
          <text class="session-preview">{{ session.lastMessageContent || '暂无消息' }}</text>
          <text v-if="session.isMute === 1" class="session-muted">静音</text>
        </view>
      </view>
    </view>

    <view v-if="loading" class="session-state">
      <text class="state-text">会话加载中...</text>
    </view>

    <view v-else-if="!filteredSessions.length" class="session-state">
      <text class="state-title">没有匹配的会话</text>
      <text class="state-text">试试别的关键词，或者从通讯录发起聊天。</text>
    </view>

    <view v-if="hasMore" class="load-more">
      <button class="load-more-btn" @click="$emit('load-more')">加载更多</button>
    </view>
  </DesktopSideShell>
</template>

<script setup>
import { computed, ref } from 'vue'
import DesktopSideShell from './DesktopSideShell.vue'
import { DEFAULT_AVATAR as defaultAvatar } from '@/utils/common'

defineEmits(['select-session', 'load-more', 'refresh', 'open-profile', 'logout'])

const props = defineProps({
  userInfo: { type: Object, default: () => ({}) },
  sessions: { type: Array, default: () => [] },
  activeSessionId: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  hasMore: { type: Boolean, default: false },
})

const keyword = ref('')

const filteredSessions = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  if (!query) {
    return props.sessions
  }
  return props.sessions.filter((item) =>
    `${item.sessionName || ''}${item.lastMessageContent || ''}`.toLowerCase().includes(query),
  )
})

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
.toolbar-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.search-shell {
  flex: 1;
  min-width: 0;
  height: 64rpx;
  padding: 0 18rpx;
  border-radius: 14rpx;
  background: #ebebeb;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.search-icon {
  flex-shrink: 0;
  font-size: 24rpx;
  color: #9ca3af;
}

.search-input {
  flex: 1;
  min-width: 0;
  font-size: 24rpx;
  color: #111827;
}

.toolbar-plus {
  width: 64rpx;
  height: 64rpx;
  border-radius: 14rpx;
  background: #ebebeb;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #7c8189;
}

.plus-text {
  font-size: 34rpx;
  line-height: 1;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  min-height: 104rpx;
  padding: 16rpx 18rpx;
  background: #ffffff;
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.05);
}

.session-item.active {
  background: #e9e9e9;
}

.avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.session-avatar {
  width: 66rpx;
  height: 66rpx;
  border-radius: 16rpx;
  background: #d1d5db;
}

.unread-badge {
  position: absolute;
  top: -8rpx;
  right: -10rpx;
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

.session-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.session-top,
.session-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.session-name {
  flex: 1;
  min-width: 0;
  font-size: 24rpx;
  font-weight: 500;
  color: #111827;
}

.session-time {
  flex-shrink: 0;
  font-size: 20rpx;
  color: #9ca3af;
}

.session-preview {
  flex: 1;
  min-width: 0;
  font-size: 21rpx;
  color: #9aa0a6;
}

.session-muted {
  flex-shrink: 0;
  font-size: 19rpx;
  color: #c1c7ce;
}

.session-state,
.load-more {
  padding: 28rpx 18rpx 34rpx;
  text-align: center;
  background: #ffffff;
}

.state-title {
  display: block;
  margin-bottom: 10rpx;
  font-size: 24rpx;
  font-weight: 600;
  color: #111827;
}

.state-text {
  font-size: 21rpx;
  color: #9ca3af;
}

.load-more-btn {
  width: 100%;
  height: 64rpx;
  border-radius: 14rpx;
  background: #f0f0f0;
  color: #6b7280;
  font-size: 22rpx;
  border: none;
}
</style>
