<template>
  <view class="mobile-session-list">
    <!-- 顶部导航栏 -->
    <view class="mobile-header">
      <text class="header-title">微信({{ totalUnread }})</text>
      <view class="header-actions">
        <text class="action-icon" @click="onSearch">🔍</text>
        <text class="action-icon" @click="onAdd">➕</text>
      </view>
    </view>

    <!-- 设备登录提示 -->
    <view class="device-tip" v-if="showDeviceTip">
      <text class="device-icon">💻</text>
      <text class="device-text">Windows微信已登录</text>
      <text class="device-close" @click="showDeviceTip = false">✕</text>
    </view>

    <!-- 会话列表 -->
    <scroll-view class="session-scroll" scroll-y="true">
      <view 
        class="session-item" 
        v-for="item in sessions" 
        :key="item.sessionId"
        @click="selectSession(item)"
      >
        <view class="avatar-wrapper">
          <image :src="item.sessionAvatar || defaultAvatar" class="session-avatar" mode="aspectFill"></image>
          <view class="unread-badge" v-if="item.unreadCount > 0">
            <text v-if="item.unreadCount > 99">99+</text>
            <text v-else>{{ item.unreadCount }}</text>
          </view>
        </view>
        <view class="session-content">
          <view class="session-row">
            <text class="session-name">{{ item.sessionName }}</text>
            <text class="session-time">{{ formatTime(item.lastMessageTime) }}</text>
          </view>
          <view class="session-row">
            <text class="session-msg">{{ item.lastMessageContent || '' }}</text>
            <text class="mute-icon" v-if="item.isMuted">🔕</text>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  sessions: { type: Array, default: () => [] },
  totalUnread: { type: Number, default: 0 },
  defaultAvatar: { type: String, default: '' }
})

const emit = defineEmits(['select', 'search', 'add'])

const showDeviceTip = ref(true)

const selectSession = (item) => {
  emit('select', item)
}

const onSearch = () => {
  emit('search')
}

const onAdd = () => {
  emit('add')
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const msgTime = new Date(timeStr)
  if (isNaN(msgTime.getTime())) return ''
  
  const now = new Date()
  const oneDay = 24 * 60 * 60 * 1000

  const isToday = msgTime.toDateString() === now.toDateString()
  if (isToday) return msgTime.toTimeString().slice(0, 5)

  const yesterday = new Date(now.getTime() - oneDay)
  const isYesterday = msgTime.toDateString() === yesterday.toDateString()
  if (isYesterday) return '昨天'

  const isThisYear = msgTime.getFullYear() === now.getFullYear()
  if (isThisYear) return `${msgTime.getMonth() + 1}月${msgTime.getDate()}日`

  return `${msgTime.getFullYear()}/${msgTime.getMonth() + 1}/${msgTime.getDate()}`
}
</script>

<style scoped>
.mobile-session-list {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.mobile-header {
  height: 44px;
  background: #ededed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
  border-bottom: 0.5px solid #d9d9d9;
}

.header-title {
  font-size: 17px;
  font-weight: 600;
  color: #000;
}

.header-actions {
  display: flex;
  gap: 20px;
}

.action-icon {
  font-size: 22px;
  color: #000;
}

.device-tip {
  height: 44px;
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 15px;
  border-bottom: 0.5px solid #e5e5e5;
}

.device-icon {
  font-size: 18px;
  margin-right: 8px;
}

.device-text {
  flex: 1;
  font-size: 14px;
  color: #576b95;
}

.device-close {
  font-size: 14px;
  color: #999;
  padding: 5px;
}

.session-scroll {
  flex: 1;
  background: #fff;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 12px 15px;
  background: #fff;
  border-bottom: 0.5px solid #e5e5e5;
}

.avatar-wrapper {
  position: relative;
  margin-right: 12px;
}

.session-avatar {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  background: #ccc;
}

.unread-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 18px;
  height: 18px;
  background: #ff3b30;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
  box-sizing: border-box;
}

.unread-badge text {
  color: #fff;
  font-size: 11px;
  font-weight: 500;
}

.session-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.session-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.session-name {
  font-size: 16px;
  color: #000;
  font-weight: 500;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  font-size: 12px;
  color: #b2b2b2;
}

.session-msg {
  font-size: 14px;
  color: #999;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 8px;
}

.mute-icon {
  font-size: 12px;
  color: #b2b2b2;
}
</style>
