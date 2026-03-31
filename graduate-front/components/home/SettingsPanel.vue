<template>
  <view v-if="visible" class="settings-layer">
    <view class="settings-backdrop" @click="$emit('close')"></view>

    <view class="settings-sheet" :class="{ mobile: !isDesktop }">
      <view class="settings-header">
        <view class="header-user">
          <image class="header-avatar" :src="userAvatar" mode="aspectFill" />
          <view class="header-copy">
            <text class="header-name">{{ userName }}</text>
            <text class="header-meta">{{ userIdentity }}</text>
          </view>
        </view>
        <text class="header-close" @click="$emit('close')">关闭</text>
      </view>

      <scroll-view class="settings-scroll" scroll-y>
        <view class="hero-card">
          <view class="hero-copy">
            <text class="hero-title">账号与外观</text>
            <text class="hero-subtitle">
              把个人资料、通知偏好和设备入口集中在一个地方，桌面端和移动端保持同样的结构。
            </text>
          </view>

          <view class="hero-actions">
            <button class="hero-btn primary" @click="$emit('open-edit-profile')">编辑资料</button>
            <button class="hero-btn" @click="$emit('open-qr-code')">二维码中心</button>
          </view>
        </view>

        <view class="settings-section">
          <text class="section-title">个人信息</text>
          <view class="cell-list">
            <view class="cell-row">
              <view class="cell-main">
                <text class="cell-label">昵称</text>
                <text class="cell-value">{{ userInfo.nickname || '未设置' }}</text>
              </view>
              <text class="cell-action" @click="$emit('open-edit-profile')">修改</text>
            </view>
            <view class="cell-row">
              <view class="cell-main">
                <text class="cell-label">签名</text>
                <text class="cell-value muted">{{ userInfo.signature || '写一句介绍自己吧' }}</text>
              </view>
            </view>
            <view class="cell-row">
              <view class="cell-main">
                <text class="cell-label">地区</text>
                <text class="cell-value muted">{{ userInfo.region || '未填写' }}</text>
              </view>
            </view>
            <view class="cell-row">
              <view class="cell-main">
                <text class="cell-label">邮箱</text>
                <text class="cell-value muted">{{ userInfo.email || '未绑定' }}</text>
              </view>
            </view>
          </view>
        </view>

        <view class="settings-section">
          <text class="section-title">消息与通话</text>
          <view class="toggle-list">
            <view class="toggle-row">
              <view class="toggle-main">
                <text class="toggle-title">新消息横幅</text>
                <text class="toggle-desc">在首页保留即时提醒，更接近微信的安静提示方式。</text>
              </view>
              <view class="toggle-switch" :class="{ on: settings.banner }" @click="settings.banner = !settings.banner">
                <view class="toggle-thumb"></view>
              </view>
            </view>
            <view class="toggle-row">
              <view class="toggle-main">
                <text class="toggle-title">消息提示音</text>
                <text class="toggle-desc">接收新消息时给出更明显的音频反馈。</text>
              </view>
              <view class="toggle-switch" :class="{ on: settings.sound }" @click="settings.sound = !settings.sound">
                <view class="toggle-thumb"></view>
              </view>
            </view>
            <view class="toggle-row">
              <view class="toggle-main">
                <text class="toggle-title">语音通话悬浮入口</text>
                <text class="toggle-desc">通话时保留更稳定的悬浮状态和更显眼的挂断操作。</text>
              </view>
              <view class="toggle-switch" :class="{ on: settings.callOverlay }" @click="settings.callOverlay = !settings.callOverlay">
                <view class="toggle-thumb"></view>
              </view>
            </view>
          </view>
        </view>

        <view class="settings-section">
          <text class="section-title">隐私与联系人</text>
          <view class="toggle-list">
            <view class="toggle-row">
              <view class="toggle-main">
                <text class="toggle-title">加好友需要验证</text>
                <text class="toggle-desc">默认保留验证步骤，减少陌生消息直接进入会话。</text>
              </view>
              <view class="toggle-switch" :class="{ on: settings.friendVerify }" @click="settings.friendVerify = !settings.friendVerify">
                <view class="toggle-thumb"></view>
              </view>
            </view>
            <view class="toggle-row">
              <view class="toggle-main">
                <text class="toggle-title">资料页展示地区</text>
                <text class="toggle-desc">联系人查看你的资料时是否显示地区信息。</text>
              </view>
              <view class="toggle-switch" :class="{ on: settings.showRegion }" @click="settings.showRegion = !settings.showRegion">
                <view class="toggle-thumb"></view>
              </view>
            </view>
          </view>
        </view>

        <view class="settings-section">
          <text class="section-title">设备与安全</text>
          <view class="action-card">
            <view class="action-main">
              <text class="action-title">二维码中心</text>
              <text class="action-desc">查看登录二维码和个人名片二维码。</text>
            </view>
            <button class="action-btn" @click="$emit('open-qr-code')">打开</button>
          </view>
          <view v-if="!isDesktop" class="action-card">
            <view class="action-main">
              <text class="action-title">扫一扫</text>
              <text class="action-desc">扫描登录二维码或联系人名片码。</text>
            </view>
            <button class="action-btn" @click="$emit('open-scan-page')">打开</button>
          </view>
          <view class="action-card danger">
            <view class="action-main">
              <text class="action-title">退出当前账号</text>
              <text class="action-desc">退出后需要重新登录，聊天数据不会丢失。</text>
            </view>
            <button class="action-btn danger" @click="$emit('logout')">退出</button>
          </view>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import LocalStateCache from '@/utils/local-state-cache'

const props = defineProps({
  visible: { type: Boolean, default: false },
  userInfo: { type: Object, default: () => ({}) },
  defaultAvatar: { type: String, default: '' },
  isDesktop: { type: Boolean, default: true },
})

defineEmits(['close', 'open-edit-profile', 'open-qr-code', 'open-scan-page', 'logout'])

const settings = reactive({
  banner: true,
  sound: true,
  callOverlay: true,
  friendVerify: true,
  showRegion: true,
})

const SETTINGS_CACHE_KEY = 'settings_panel_preferences'

const resolveSettingsCacheScope = () => {
  const currentId = String(props.userInfo?.id || '')
  if (currentId) {
    return `settings:${currentId}`
  }

  try {
    const stored = uni.getStorageSync('userInfo') || {}
    const storedId = String(stored?.id || '')
    if (storedId) {
      return `settings:${storedId}`
    }
  } catch {}

  return 'settings:anonymous'
}

const normalizeSettingsSnapshot = (value = {}) => ({
  banner: value?.banner !== false,
  sound: value?.sound !== false,
  callOverlay: value?.callOverlay !== false,
  friendVerify: value?.friendVerify !== false,
  showRegion: value?.showRegion !== false,
})

const applyCachedSettings = (value = {}) => {
  const next = normalizeSettingsSnapshot(value)
  Object.keys(next).forEach((key) => {
    settings[key] = next[key]
  })
}

let settingsHydrating = false

watch(
  () => props.userInfo?.id || '',
  () => {
    settingsHydrating = true
    applyCachedSettings(LocalStateCache.getValue(resolveSettingsCacheScope(), SETTINGS_CACHE_KEY) || {})
    settingsHydrating = false
  },
  { immediate: true },
)

watch(
  settings,
  (value) => {
    if (settingsHydrating) {
      return
    }
    LocalStateCache.set(resolveSettingsCacheScope(), SETTINGS_CACHE_KEY, normalizeSettingsSnapshot(value))
  },
  { deep: true },
)

const userAvatar = computed(() => props.userInfo?.avatar || props.defaultAvatar)
const userName = computed(
  () => props.userInfo?.nickname || props.userInfo?.username || '未命名用户',
)
const userIdentity = computed(() => {
  if (props.userInfo?.username) {
    return `@${props.userInfo.username}`
  }
  if (props.userInfo?.id) {
    return `ID ${props.userInfo.id}`
  }
  return '未绑定账号标识'
})
</script>

<style scoped>
.settings-layer {
  position: fixed;
  inset: 0;
  z-index: 65;
  display: flex;
  justify-content: flex-end;
}

.settings-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(20rpx);
}

.settings-sheet {
  position: relative;
  width: min(760rpx, 100%);
  height: 100%;
  background: linear-gradient(180deg, rgba(251, 252, 252, 0.98), rgba(243, 245, 246, 0.98));
  box-shadow: -24rpx 0 60rpx rgba(15, 23, 42, 0.16);
  padding: 28rpx;
}

.settings-sheet.mobile {
  width: 100%;
  padding: 22rpx 18rpx calc(22rpx + env(safe-area-inset-bottom, 0px));
}

.settings-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.header-avatar {
  width: 92rpx;
  height: 92rpx;
  border-radius: 28rpx;
  background: #d1d5db;
}

.header-copy {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.header-name {
  font-size: 32rpx;
  font-weight: 700;
  color: #111827;
}

.header-meta,
.hero-subtitle,
.toggle-desc,
.action-desc,
.cell-value.muted {
  font-size: 22rpx;
  color: #6b7280;
}

.header-close,
.cell-action {
  font-size: 24rpx;
  color: #07c160;
}

.settings-scroll {
  height: calc(100vh - 120rpx);
  min-height: 0;
}

.hero-card,
.settings-section {
  margin-bottom: 20rpx;
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.88);
  border: 1rpx solid rgba(15, 23, 42, 0.05);
  box-shadow: 0 16rpx 38rpx rgba(15, 23, 42, 0.06);
}

.hero-card {
  padding: 26rpx;
}

.hero-title,
.section-title,
.action-title,
.toggle-title,
.cell-label {
  font-size: 28rpx;
  font-weight: 700;
  color: #111827;
}

.hero-actions,
.action-card {
  display: flex;
  gap: 14rpx;
  align-items: center;
}

.hero-actions {
  margin-top: 20rpx;
}

.hero-btn,
.action-btn {
  height: 78rpx;
  padding: 0 26rpx;
  border-radius: 22rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 25rpx;
  font-weight: 600;
  background: rgba(15, 23, 42, 0.06);
  color: #111827;
}

.hero-btn.primary {
  background: linear-gradient(135deg, #07c160 0%, #29d17c 100%);
  color: #ffffff;
}

.cell-list,
.toggle-list {
  padding: 12rpx 22rpx 22rpx;
}

.section-title {
  display: block;
  padding: 22rpx 22rpx 6rpx;
}

.cell-row,
.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  padding: 18rpx 0;
}

.cell-row + .cell-row,
.toggle-row + .toggle-row {
  border-top: 1rpx solid rgba(15, 23, 42, 0.05);
}

.cell-main,
.toggle-main,
.action-main {
  flex: 1;
  min-width: 0;
}

.cell-value {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #111827;
}

.toggle-desc {
  display: block;
  margin-top: 8rpx;
}

.toggle-switch {
  width: 92rpx;
  height: 56rpx;
  padding: 6rpx;
  border-radius: 999rpx;
  background: rgba(15, 23, 42, 0.1);
  display: flex;
  align-items: center;
}

.toggle-switch.on {
  justify-content: flex-end;
  background: rgba(7, 193, 96, 0.26);
}

.toggle-thumb {
  width: 44rpx;
  height: 44rpx;
  border-radius: 999rpx;
  background: #ffffff;
  box-shadow: 0 6rpx 14rpx rgba(15, 23, 42, 0.12);
}

.action-card {
  padding: 22rpx;
}

.action-card + .action-card {
  margin-top: 14rpx;
}

.action-card.danger {
  background: rgba(254, 242, 242, 0.88);
}

.action-btn.danger {
  background: #ef4444;
  color: #ffffff;
}
</style>
