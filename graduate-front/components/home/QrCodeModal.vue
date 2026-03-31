<template>
  <view v-if="visible" class="qr-layer">
    <view class="qr-backdrop" @click="$emit('close')"></view>

    <view class="qr-card" :class="{ mobile: !isDesktop }">
      <view class="qr-header">
        <view>
          <text class="qr-title">二维码中心</text>
          <text class="qr-subtitle">名片分享和跨设备登录都放在这里。</text>
        </view>
        <text class="qr-close" @click="$emit('close')">关闭</text>
      </view>

      <view class="qr-tabs">
        <view
          v-for="item in tabs"
          :key="item.key"
          class="qr-tab"
          :class="{ active: activeTab === item.key }"
          @click="activeTab = item.key"
        >
          {{ item.label }}
        </view>
      </view>

      <view v-if="activeTab === 'card'" class="qr-body">
        <view class="qr-profile-card">
          <image class="profile-avatar" :src="userAvatar" mode="aspectFill" />
          <view class="profile-copy">
            <text class="profile-name">{{ userName }}</text>
            <text class="profile-meta">@{{ userIdentity }}</text>
          </view>
        </view>

        <view class="qr-board">
          <image v-if="cardQrDataUrl" class="qr-image" :src="cardQrDataUrl" mode="aspectFit" />
          <view v-else class="qr-placeholder">
            <text>正在生成二维码…</text>
          </view>
        </view>

        <view class="qr-hint">
          分享给朋友后，对方可以直接识别你的账号名片。
        </view>

        <view class="qr-actions">
          <button class="ghost-btn" @click="copyCardPayload">复制名片内容</button>
          <button class="primary-btn" @click="saveImage(cardQrDataUrl, 'contact-card')">保存二维码</button>
        </view>
      </view>

      <view v-else class="qr-body">
        <view class="login-card">
          <text class="login-title">登录此账号到新设备</text>
          <text class="login-desc">
            使用另一台已登录的设备扫码并确认，新设备就能继续登录当前账号。
          </text>
        </view>

        <view class="qr-board">
          <image v-if="loginQrBase64" class="qr-image" :src="loginQrBase64" mode="aspectFit" />
          <view v-else class="qr-placeholder">
            <text>{{ loginLoading ? '正在获取登录二维码…' : '点击重新生成' }}</text>
          </view>
        </view>

        <view class="login-status" :class="`status-${loginStatus}`">
          <text class="status-title">{{ loginStatusText }}</text>
          <text class="status-subtitle">{{ loginStatusHint }}</text>
          <text v-if="loginStatus === 'waiting'" class="status-timer">
            {{ countdownText }}
          </text>
        </view>

        <view class="qr-actions">
          <button class="ghost-btn" @click="generateLoginQrCode">重新生成</button>
          <button
            class="primary-btn"
            :disabled="!loginQrBase64"
            @click="saveImage(loginQrBase64, 'device-login')"
          >
            保存二维码
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import QRCode from 'qrcode'
import service from '@/utils/request'
import { APP_PERMISSION_SCOPE, ensureAppPermissionAccess } from '@/utils/app-permission'
import LocalStateCache from '@/utils/local-state-cache'
import { isAppPlusRuntime, supportsBrowserDom, waitForPlusReady } from '@/utils/runtime'

const props = defineProps({
  visible: { type: Boolean, default: false },
  userInfo: { type: Object, default: () => ({}) },
  defaultAvatar: { type: String, default: '' },
  isDesktop: { type: Boolean, default: true },
})

defineEmits(['close'])

const QR_CACHE_KEYS = {
  ACTIVE_TAB: 'active_tab',
  CARD_SNAPSHOT: 'card_snapshot',
  LOGIN_SNAPSHOT: 'login_snapshot',
}

const LOGIN_QR_TTL_MS = 5 * 60 * 1000

const tabs = [
  { key: 'card', label: '我的名片码' },
  { key: 'login', label: '设备登录码' },
]

const activeTab = ref('card')
const cardQrDataUrl = ref('')
const loginQrBase64 = ref('')
const loginQrId = ref('')
const loginStatus = ref('idle')
const loginLoading = ref(false)
const loginRemaining = ref(300)

let pollTimer = null
let countdownTimer = null
let loginExpiresAt = 0
let restoringQrCache = false

const userAvatar = computed(() => props.userInfo?.avatar || props.defaultAvatar)
const userName = computed(
  () => props.userInfo?.nickname || props.userInfo?.username || '未命名用户',
)
const userIdentity = computed(() => props.userInfo?.username || props.userInfo?.id || 'unknown')
const cardPayload = computed(() =>
  JSON.stringify({
    type: 'graduate-contact-card',
    userId: props.userInfo?.id || '',
    username: props.userInfo?.username || '',
    nickname: props.userInfo?.nickname || '',
  }),
)
const countdownText = computed(() => {
  const total = Number(loginRemaining.value || 0)
  const minutes = String(Math.floor(total / 60)).padStart(2, '0')
  const seconds = String(total % 60).padStart(2, '0')
  return `剩余 ${minutes}:${seconds}`
})
const loginStatusText = computed(() => {
  if (loginStatus.value === 'waiting') return '等待扫码'
  if (loginStatus.value === 'scanned') return '已扫码，等待确认'
  if (loginStatus.value === 'confirmed') return '登录已确认'
  if (loginStatus.value === 'expired') return '二维码已过期'
  if (loginStatus.value === 'error') return '二维码获取失败'
  return '尚未生成二维码'
})
const loginStatusHint = computed(() => {
  if (loginStatus.value === 'waiting') return '请使用另一台已登录设备扫码。'
  if (loginStatus.value === 'scanned') return '请在扫码设备上点击确认登录。'
  if (loginStatus.value === 'confirmed') return '新设备现在可以继续当前登录流程。'
  if (loginStatus.value === 'expired') return '为了安全，二维码已经失效，请重新生成。'
  if (loginStatus.value === 'error') return '网络异常或二维码服务不可用。'
  return '切换到此标签后会自动生成。'
})

const resolveQrCacheScope = () => {
  const currentId = String(props.userInfo?.id || '')
  if (currentId) {
    return `qr:${currentId}`
  }

  try {
    const stored = uni.getStorageSync('userInfo') || {}
    const storedId = String(stored?.id || '')
    if (storedId) {
      return `qr:${storedId}`
    }
  } catch {}

  return 'qr:anonymous'
}

const readQrCacheValue = (key, options = {}) =>
  LocalStateCache.getValue(resolveQrCacheScope(), key, options)

const writeQrCacheValue = (key, value) =>
  LocalStateCache.set(resolveQrCacheScope(), key, value)

const removeQrCacheValue = (key) =>
  LocalStateCache.remove(resolveQrCacheScope(), key)

const restoreCardSnapshot = () => {
  const cached = readQrCacheValue(QR_CACHE_KEYS.CARD_SNAPSHOT)
  if (!cached || typeof cached !== 'object') {
    return false
  }

  if (
    String(cached.userId || '') !== String(props.userInfo?.id || '')
    || String(cached.payload || '') !== String(cardPayload.value || '')
    || !String(cached.dataUrl || '')
  ) {
    return false
  }

  cardQrDataUrl.value = String(cached.dataUrl || '')
  return true
}

const persistCardSnapshot = () => {
  if (!cardQrDataUrl.value) return
  writeQrCacheValue(QR_CACHE_KEYS.CARD_SNAPSHOT, {
    userId: String(props.userInfo?.id || ''),
    payload: String(cardPayload.value || ''),
    dataUrl: String(cardQrDataUrl.value || ''),
  })
}

const clearLoginSnapshot = () => {
  removeQrCacheValue(QR_CACHE_KEYS.LOGIN_SNAPSHOT)
}

const persistLoginSnapshot = () => {
  if (!loginQrBase64.value || !loginQrId.value || !loginExpiresAt) {
    return
  }

  writeQrCacheValue(QR_CACHE_KEYS.LOGIN_SNAPSHOT, {
    qrCodeBase64: String(loginQrBase64.value || ''),
    qrCodeId: String(loginQrId.value || ''),
    status: String(loginStatus.value || 'waiting'),
    expiresAt: Number(loginExpiresAt || 0),
  })
}

const restoreLoginSnapshot = () => {
  const cached = readQrCacheValue(QR_CACHE_KEYS.LOGIN_SNAPSHOT)
  if (!cached || typeof cached !== 'object') {
    return false
  }

  const expiresAt = Number(cached.expiresAt || 0)
  const status = String(cached.status || 'waiting')
  const remaining = Math.max(0, Math.ceil((expiresAt - Date.now()) / 1000))

  if (
    !String(cached.qrCodeBase64 || '')
    || !String(cached.qrCodeId || '')
    || !expiresAt
    || remaining <= 0
    || ['confirmed', 'expired', 'error'].includes(status)
  ) {
    clearLoginSnapshot()
    return false
  }

  loginQrBase64.value = String(cached.qrCodeBase64 || '')
  loginQrId.value = String(cached.qrCodeId || '')
  loginStatus.value = status
  loginRemaining.value = remaining
  loginLoading.value = false
  loginExpiresAt = expiresAt
  return true
}

const stopLoginTimers = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

const renderCardQrCode = async () => {
  if (!props.userInfo?.id) {
    cardQrDataUrl.value = ''
    return
  }

  try {
    cardQrDataUrl.value = await QRCode.toDataURL(cardPayload.value, {
      width: 360,
      margin: 1,
      color: {
        dark: '#111827',
        light: '#FFFFFF',
      },
    })
    persistCardSnapshot()
  } catch (error) {
    console.error('[qr-modal] render card qr failed', error)
    cardQrDataUrl.value = ''
  }
}

const pollLoginStatus = async () => {
  if (!loginQrId.value) return

  try {
    const response = await service.get('/user/qrcode/status', {
      params: { qrCodeId: loginQrId.value },
    })

    const status = response?.data?.status || 'waiting'
    loginStatus.value = status

    if (status === 'confirmed' || status === 'expired') {
      clearLoginSnapshot()
      stopLoginTimers()
    } else {
      persistLoginSnapshot()
    }
  } catch (error) {
    loginStatus.value = 'error'
    clearLoginSnapshot()
    stopLoginTimers()
    console.warn('[qr-modal] poll login status failed', error)
  }
}

const startLoginCountdown = (expiresAt = Date.now() + LOGIN_QR_TTL_MS) => {
  stopLoginTimers()
  loginExpiresAt = Number(expiresAt || 0)
  loginRemaining.value = Math.max(0, Math.ceil((loginExpiresAt - Date.now()) / 1000))

  countdownTimer = setInterval(() => {
    loginRemaining.value = Math.max(0, Math.ceil((loginExpiresAt - Date.now()) / 1000))
    if (loginRemaining.value <= 0) {
      loginRemaining.value = 0
      loginStatus.value = 'expired'
      clearLoginSnapshot()
      stopLoginTimers()
    }
  }, 1000)

  pollTimer = setInterval(() => {
    pollLoginStatus()
  }, 1500)
}

const generateLoginQrCode = async () => {
  try {
    loginLoading.value = true
    loginStatus.value = 'waiting'
    loginRemaining.value = 300
    loginExpiresAt = 0
    clearLoginSnapshot()
    const response = await service.get('/user/qrcode/generate')
    loginQrBase64.value = response?.data?.qrCodeBase64 || ''
    loginQrId.value = response?.data?.qrCodeId || ''

    if (!loginQrBase64.value || !loginQrId.value) {
      loginStatus.value = 'error'
      clearLoginSnapshot()
      return
    }

    loginExpiresAt = Date.now() + LOGIN_QR_TTL_MS
    persistLoginSnapshot()
    await pollLoginStatus()
    if (loginStatus.value !== 'confirmed') {
      startLoginCountdown(loginExpiresAt)
    }
  } catch (error) {
    console.error('[qr-modal] generate login qr failed', error)
    loginStatus.value = 'error'
    loginQrBase64.value = ''
    loginQrId.value = ''
    loginExpiresAt = 0
    clearLoginSnapshot()
  } finally {
    loginLoading.value = false
  }
}

const copyCardPayload = () => {
  uni.setClipboardData({
    data: cardPayload.value,
    success: () => {
      uni.showToast({
        title: '名片内容已复制',
        icon: 'none',
      })
    },
  })
}

const normalizeBase64Image = (source) => {
  const value = String(source || '').trim()
  if (!value) return ''
  return value.startsWith('data:') ? value : `data:image/png;base64,${value}`
}

const removeTemporaryAppFile = async (filePath) => {
  if (!filePath || typeof plus?.io?.resolveLocalFileSystemURL !== 'function') {
    return
  }

  await new Promise((resolve) => {
    plus.io.resolveLocalFileSystemURL(
      filePath,
      (entry) => {
        if (typeof entry?.remove !== 'function') {
          resolve()
          return
        }
        entry.remove(
          () => resolve(),
          () => resolve(),
        )
      },
      () => resolve(),
    )
  })
}

const saveImageToAlbumOnApp = async (source, prefix) => {
  await waitForPlusReady()

  const bitmap = new plus.nativeObj.Bitmap(`qr-${Date.now()}`)
  const filePath = `_doc/${prefix}-${Date.now()}.png`
  let savedPath = ''

  try {
    await new Promise((resolve, reject) => {
      bitmap.loadBase64Data(
        normalizeBase64Image(source),
        () => resolve(),
        (error) => reject(new Error(error?.message || '二维码写入失败')),
      )
    })

    savedPath = await new Promise((resolve, reject) => {
      bitmap.save(
        filePath,
        { overwrite: true, format: 'png', quality: 100 },
        (result) => resolve(result?.target || filePath),
        (error) => reject(new Error(error?.message || '二维码保存失败')),
      )
    })
    const albumPath =
      typeof plus?.io?.convertLocalFileSystemURL === 'function'
        ? plus.io.convertLocalFileSystemURL(savedPath)
        : savedPath

    await new Promise((resolve, reject) => {
      uni.saveImageToPhotosAlbum({
        filePath: albumPath,
        success: () => resolve(),
        fail: (error) => reject(error),
      })
    })
  } finally {
    bitmap.clear()
    await removeTemporaryAppFile(savedPath || filePath)
  }
}

const saveImage = async (source, prefix) => {
  if (!source) return

  if (supportsBrowserDom()) {
    const link = document.createElement('a')
    link.href = source
    link.download = `${prefix}-${Date.now()}.png`
    link.click()
    return
  }

  if (isAppPlusRuntime()) {
    uni.showLoading({
      title: '保存中...',
      mask: true,
    })

    try {
      const permissionResult = await ensureAppPermissionAccess([APP_PERMISSION_SCOPE.ALBUM], {
        title: '需要相册权限',
        content: '保存二维码到系统相册前，请先在系统设置中开启相册权限。',
      })
      if (!permissionResult.ok) {
        return
      }

      await saveImageToAlbumOnApp(source, prefix)
      uni.showToast({
        title: '二维码已保存到相册',
        icon: 'none',
      })
    } catch (error) {
      const message = String(error?.errMsg || error?.message || '')
      uni.showToast({
        title: /auth|permission|deny|denied|authorized/i.test(message)
          ? '保存失败，请检查相册权限'
          : '保存失败，请稍后重试',
        icon: 'none',
      })
    } finally {
      uni.hideLoading()
    }
    return
  }

  uni.setClipboardData({
    data: source,
    success: () => {
      uni.showToast({
        title: '当前环境已复制图片链接',
        icon: 'none',
      })
    },
  })
}

watch(
  () => props.visible,
  async (visible) => {
    if (!visible) {
      stopLoginTimers()
      return
    }

    restoringQrCache = true
    const cachedTab = String(readQrCacheValue(QR_CACHE_KEYS.ACTIVE_TAB) || '')
    if (cachedTab === 'card' || cachedTab === 'login') {
      activeTab.value = cachedTab
    }

    if (!restoreCardSnapshot()) {
      await renderCardQrCode()
    }
    restoringQrCache = false

    if (activeTab.value === 'login') {
      if (restoreLoginSnapshot()) {
        startLoginCountdown(loginExpiresAt)
        await pollLoginStatus()
      } else {
        await generateLoginQrCode()
      }
    }
  },
  { immediate: true },
)

watch(activeTab, async (tab) => {
  writeQrCacheValue(QR_CACHE_KEYS.ACTIVE_TAB, tab)

  if (!props.visible || restoringQrCache) return

  if (tab === 'card') {
    stopLoginTimers()
    if (!restoreCardSnapshot()) {
      await renderCardQrCode()
    }
    return
  }

  if (restoreLoginSnapshot()) {
    startLoginCountdown(loginExpiresAt)
    await pollLoginStatus()
    return
  }

  await generateLoginQrCode()
})

onBeforeUnmount(() => {
  stopLoginTimers()
})
</script>

<style scoped>
.qr-layer {
  position: fixed;
  inset: 0;
  z-index: 75;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28rpx;
}

.qr-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.54);
  backdrop-filter: blur(22rpx);
}

.qr-card {
  position: relative;
  width: min(860rpx, 100%);
  padding: 30rpx;
  border-radius: 40rpx;
  background: linear-gradient(180deg, rgba(252, 253, 253, 0.98), rgba(244, 246, 247, 0.98));
  box-shadow: 0 36rpx 90rpx rgba(15, 23, 42, 0.22);
}

.qr-card.mobile {
  width: 100%;
  border-radius: 34rpx;
  padding: 26rpx;
}

.qr-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.qr-title {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #111827;
}

.qr-subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #6b7280;
}

.qr-close {
  font-size: 24rpx;
  color: #07c160;
}

.qr-tabs {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12rpx;
  margin-top: 24rpx;
  padding: 8rpx;
  border-radius: 24rpx;
  background: rgba(15, 23, 42, 0.04);
}

.qr-tab {
  height: 74rpx;
  border-radius: 20rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 25rpx;
  color: #64748b;
}

.qr-tab.active {
  background: #ffffff;
  color: #111827;
  box-shadow: 0 10rpx 24rpx rgba(15, 23, 42, 0.08);
}

.qr-body {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
  margin-top: 24rpx;
}

.qr-profile-card,
.login-card,
.login-status {
  padding: 22rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.84);
  border: 1rpx solid rgba(15, 23, 42, 0.05);
}

.qr-profile-card {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.profile-avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 32rpx;
  background: #d1d5db;
}

.profile-copy {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.profile-name,
.login-title,
.status-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #111827;
}

.profile-meta,
.login-desc,
.status-subtitle,
.qr-hint,
.status-timer {
  font-size: 23rpx;
  color: #6b7280;
}

.qr-board {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 34rpx;
  border-radius: 34rpx;
  background:
    radial-gradient(circle at top, rgba(7, 193, 96, 0.14), transparent 38%),
    rgba(255, 255, 255, 0.95);
  min-height: 420rpx;
}

.qr-image {
  width: 360rpx;
  height: 360rpx;
  border-radius: 32rpx;
}

.qr-placeholder {
  font-size: 24rpx;
  color: #94a3b8;
}

.qr-hint {
  padding: 0 8rpx;
}

.login-status.status-confirmed {
  background: rgba(7, 193, 96, 0.12);
}

.login-status.status-expired,
.login-status.status-error {
  background: rgba(239, 68, 68, 0.1);
}

.status-timer {
  display: inline-flex;
  margin-top: 10rpx;
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(15, 23, 42, 0.04);
}

.qr-actions {
  display: flex;
  gap: 16rpx;
}

.ghost-btn,
.primary-btn {
  flex: 1;
  height: 84rpx;
  border-radius: 24rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: 600;
}

.ghost-btn {
  background: rgba(15, 23, 42, 0.06);
  color: #111827;
}

.primary-btn {
  background: linear-gradient(135deg, #07c160 0%, #29d17c 100%);
  color: #ffffff;
}
</style>
