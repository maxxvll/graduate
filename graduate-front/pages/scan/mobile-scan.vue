<template>
  <view class="scan-page">
    <view class="scan-header">
      <view class="header-back" @click="goBack">
        <text class="back-icon">‹</text>
        <text class="back-text">返回</text>
      </view>
      <text class="header-title">扫一扫</text>
      <view class="header-spacer"></view>
    </view>

    <view class="scan-body">
      <view class="scan-card">
        <text class="scan-title">{{ titleText }}</text>
        <text class="scan-subtitle">{{ subtitleText }}</text>

        <view class="scan-status" :class="stateClass">
          <text class="status-icon">{{ statusIcon }}</text>
          <text class="status-text">{{ statusText }}</text>
        </view>

        <view v-if="scanState === 'contact-card'" class="result-card">
          <text class="result-title">{{ contactCard.nickname || '联系人名片' }}</text>
          <text class="result-meta">@{{ contactCard.username || 'unknown' }}</text>
          <text class="result-desc">可以返回首页直接搜索并发起添加好友。</text>
        </view>

        <view v-else-if="scanState === 'login-qr'" class="result-card">
          <text class="result-title">检测到设备登录二维码</text>
          <text class="result-meta">二维码 ID：{{ scannedQrCodeId }}</text>
          <text class="result-desc">确认后，新设备会继续使用当前账号完成登录。</text>
        </view>

        <view v-else-if="scanState === 'success'" class="result-card success">
          <text class="result-title">扫码处理完成</text>
          <text class="result-desc">{{ successMessage }}</text>
        </view>

        <view v-else-if="scanState === 'error'" class="result-card danger">
          <text class="result-title">当前二维码无法处理</text>
          <text class="result-desc">{{ errorMessage }}</text>
        </view>

        <view class="action-list">
          <button
            v-if="scanState === 'idle' || scanState === 'error' || scanState === 'unsupported'"
            class="primary-btn"
            :disabled="busy"
            @click="startScan"
          >
            开始扫码
          </button>

          <button
            v-else-if="scanState === 'login-qr'"
            class="primary-btn"
            :disabled="busy"
            @click="confirmLoginQr"
          >
            确认登录
          </button>

          <template v-else-if="scanState === 'contact-card'">
            <button class="primary-btn" :disabled="busy" @click="openContactCard">
              去首页查看
            </button>
            <button class="ghost-btn" :disabled="busy" @click="copyContactUsername">
              复制账号
            </button>
          </template>

          <template v-else-if="scanState === 'success'">
            <button class="primary-btn" @click="goBack">完成</button>
            <button class="ghost-btn" :disabled="busy" @click="startScan">继续扫码</button>
          </template>

          <button
            v-if="scanState !== 'scanning' && scanState !== 'idle'"
            class="ghost-btn"
            :disabled="busy"
            @click="resetState"
          >
            重新识别
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { APP_PERMISSION_SCOPE, ensureAppPermissionAccess } from '@/utils/app-permission'
import service from '@/utils/request'
import { isAppPlusRuntime } from '@/utils/runtime'
import LocalStateCache from '@/utils/local-state-cache'

const scanState = ref('idle')
const busy = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const scannedQrCodeId = ref('')
const contactCard = ref({})
const SCAN_PAGE_CACHE_KEY = 'scan_page_state'
const SCAN_CACHE_TTL_MS = 30 * 60 * 1000

const resolveScanCacheScope = () => {
  try {
    const stored = uni.getStorageSync('userInfo') || {}
    const storedId = String(stored?.id || '')
    if (storedId) {
      return `scan-page:${storedId}`
    }
  } catch {}

  return 'scan-page:anonymous'
}

const normalizeScanSnapshot = (value = {}) => ({
  scanState: String(value?.scanState || 'idle'),
  errorMessage: String(value?.errorMessage || ''),
  successMessage: String(value?.successMessage || ''),
  scannedQrCodeId: String(value?.scannedQrCodeId || ''),
  contactCard: {
    userId: String(value?.contactCard?.userId || ''),
    username: String(value?.contactCard?.username || ''),
    nickname: String(value?.contactCard?.nickname || ''),
  },
})

const readScanSnapshot = () => {
  const cached = LocalStateCache.get(resolveScanCacheScope(), SCAN_PAGE_CACHE_KEY, {
    maxAgeMs: SCAN_CACHE_TTL_MS,
  })
  return cached?.value ? normalizeScanSnapshot(cached.value) : null
}

const writeScanSnapshot = () => {
  LocalStateCache.set(resolveScanCacheScope(), SCAN_PAGE_CACHE_KEY, normalizeScanSnapshot({
    scanState: scanState.value,
    errorMessage: errorMessage.value,
    successMessage: successMessage.value,
    scannedQrCodeId: scannedQrCodeId.value,
    contactCard: contactCard.value,
  }))
}

const clearScanSnapshot = () => {
  LocalStateCache.remove(resolveScanCacheScope(), SCAN_PAGE_CACHE_KEY)
}

const goBack = () => {
  const pageStack =
    typeof getCurrentPages === 'function' && Array.isArray(getCurrentPages()) ? getCurrentPages() : []
  if (pageStack.length > 1) {
    uni.navigateBack()
    return
  }
  uni.reLaunch({ url: '/pages/home/home' })
}

const resetState = () => {
  scanState.value = 'idle'
  errorMessage.value = ''
  successMessage.value = ''
  scannedQrCodeId.value = ''
  contactCard.value = {}
}

const normalizeText = (value) => String(value || '').trim()

const extractQrCodeIdFromText = (value) => {
  const source = normalizeText(value)
  if (!source) {
    return ''
  }

  if (/^[0-9a-zA-Z_-]{16,64}$/.test(source)) {
    return source
  }

  const match = source.match(/[?&](?:qrCodeId|id)=([0-9a-zA-Z_-]{16,64})/i)
  return match?.[1] || ''
}

const parseScanPayload = (value) => {
  const raw = normalizeText(value)
  if (!raw) {
    return { type: 'unknown' }
  }

  try {
    const payload = JSON.parse(raw)
    if (payload?.type === 'graduate-contact-card' && (payload.username || payload.userId)) {
      return {
        type: 'contact-card',
        payload: {
          userId: String(payload.userId || ''),
          username: normalizeText(payload.username),
          nickname: normalizeText(payload.nickname),
        },
      }
    }
  } catch {}

  const qrCodeId = extractQrCodeIdFromText(raw)
  if (qrCodeId) {
    return { type: 'login-qr', qrCodeId }
  }

  return { type: 'unknown' }
}

const titleText = computed(() => {
  if (scanState.value === 'scanning') return '正在调用相机'
  if (scanState.value === 'contact-card') return '识别到联系人名片'
  if (scanState.value === 'login-qr') return '识别到登录二维码'
  if (scanState.value === 'success') return '扫码完成'
  if (scanState.value === 'unsupported') return '当前环境不支持扫码'
  if (scanState.value === 'error') return '扫码结果无法处理'
  return '扫描二维码'
})

const subtitleText = computed(() => {
  if (scanState.value === 'scanning') return '请将二维码放入取景框中央，保持画面稳定。'
  if (scanState.value === 'contact-card') return '这是一个联系人名片码，可以直接回到首页搜索。'
  if (scanState.value === 'login-qr') return '这是一个新设备登录二维码，请确认是否允许当前账号登录。'
  if (scanState.value === 'success') return '结果已经写入当前账号或首页状态。'
  if (scanState.value === 'unsupported') return '请在 UniApp App 真机环境中使用扫一扫。'
  if (scanState.value === 'error') return '当前二维码格式不在支持范围内。'
  return '支持识别登录二维码和联系人名片二维码。'
})

const statusText = computed(() => {
  if (scanState.value === 'scanning') return '扫码窗口已打开'
  if (scanState.value === 'contact-card') return '等待你决定是否回到首页查看'
  if (scanState.value === 'login-qr') return '等待你确认是否登录'
  if (scanState.value === 'success') return successMessage.value || '处理成功'
  if (scanState.value === 'unsupported') return '当前端不支持'
  if (scanState.value === 'error') return errorMessage.value || '二维码内容无法识别'
  return '点击按钮开始扫码'
})

const statusIcon = computed(() => {
  if (scanState.value === 'success') return '✓'
  if (scanState.value === 'error' || scanState.value === 'unsupported') return '!'
  if (scanState.value === 'scanning') return '◌'
  return '⌕'
})

const stateClass = computed(() => `state-${scanState.value}`)

const handleScanResult = (rawValue) => {
  const parsed = parseScanPayload(rawValue)

  if (parsed.type === 'login-qr') {
    scannedQrCodeId.value = parsed.qrCodeId
    scanState.value = 'login-qr'
    return
  }

  if (parsed.type === 'contact-card') {
    contactCard.value = parsed.payload
    scanState.value = 'contact-card'
    return
  }

  errorMessage.value = '只支持登录二维码和联系人名片码。'
  scanState.value = 'error'
}

const startScan = async () => {
  if (busy.value) {
    return
  }

  if (!isAppPlusRuntime() || typeof uni.scanCode !== 'function') {
    scanState.value = 'unsupported'
    return
  }

  const permissionResult = await ensureAppPermissionAccess([APP_PERMISSION_SCOPE.CAMERA], {
    title: '需要相机权限',
    content: '扫一扫前，请先在系统设置中开启相机权限。',
  })
  if (!permissionResult.ok) {
    return
  }

  busy.value = true
  scanState.value = 'scanning'
  errorMessage.value = ''

  uni.scanCode({
    onlyFromCamera: true,
    scanType: ['qrCode'],
    autoDecodeCharset: true,
    success: (result) => {
      handleScanResult(result?.result || '')
    },
    fail: (error) => {
      const message = String(error?.errMsg || '')
      if (/cancel/i.test(message)) {
        scanState.value = 'idle'
        return
      }
      errorMessage.value = '扫码失败，请重试。'
      scanState.value = 'error'
    },
    complete: () => {
      busy.value = false
    },
  })
}

const confirmLoginQr = async () => {
  if (!scannedQrCodeId.value || busy.value) {
    return
  }

  busy.value = true
  try {
    await service.post('/user/qrcode/confirm', null, {
      params: { qrCodeId: scannedQrCodeId.value },
    })
    successMessage.value = '已确认当前账号登录新设备。'
    scanState.value = 'success'
    uni.showToast({
      title: '登录已确认',
      icon: 'none',
    })
  } catch (error) {
    errorMessage.value = String(error?.message || '确认登录失败，请稍后重试。')
    scanState.value = 'error'
  } finally {
    busy.value = false
  }
}

const copyContactUsername = () => {
  const username = normalizeText(contactCard.value.username)
  if (!username) {
    uni.showToast({
      title: '当前名片没有可复制的账号',
      icon: 'none',
    })
    return
  }

  uni.setClipboardData({
    data: username,
    success: () => {
      uni.showToast({
        title: '账号已复制',
        icon: 'none',
      })
    },
  })
}

const openContactCard = () => {
  uni.$emit('scannedContactCard', { ...contactCard.value })
  goBack()
}

onMounted(() => {
  const cachedSnapshot = readScanSnapshot()
  if (cachedSnapshot) {
    scanState.value = cachedSnapshot.scanState
    errorMessage.value = cachedSnapshot.errorMessage
    successMessage.value = cachedSnapshot.successMessage
    scannedQrCodeId.value = cachedSnapshot.scannedQrCodeId
    contactCard.value = cachedSnapshot.contactCard
  }

  if (isAppPlusRuntime()) {
    if (scanState.value !== 'idle') {
      return
    }
    setTimeout(() => {
      startScan()
    }, 120)
  } else {
    scanState.value = cachedSnapshot?.scanState || 'unsupported'
  }
})

watch(
  [scanState, errorMessage, successMessage, scannedQrCodeId, contactCard],
  () => {
    writeScanSnapshot()
  },
  { deep: true },
)
</script>

<style scoped>
.scan-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eef2f3 0%, #f7f8f8 36%, #f4f6f6 100%);
  color: #111827;
  display: flex;
  flex-direction: column;
}

.scan-header {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 16rpx;
  padding: calc(18rpx + env(safe-area-inset-top, 0px)) 18rpx 14rpx;
}

.header-back {
  min-width: 110rpx;
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
}

.back-icon {
  font-size: 40rpx;
  color: #111827;
}

.back-text,
.header-title {
  font-size: 28rpx;
  color: #111827;
}

.header-title {
  text-align: center;
  font-weight: 700;
}

.header-spacer {
  min-width: 110rpx;
}

.scan-body {
  flex: 1;
  padding: 20rpx 18rpx calc(28rpx + env(safe-area-inset-bottom, 0px));
}

.scan-card {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  padding: 28rpx;
  border-radius: 34rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 24rpx 60rpx rgba(15, 23, 42, 0.08);
}

.scan-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #111827;
}

.scan-subtitle {
  font-size: 24rpx;
  line-height: 1.6;
  color: #6b7280;
}

.scan-status {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 20rpx 22rpx;
  border-radius: 24rpx;
  background: rgba(15, 23, 42, 0.04);
}

.scan-status.state-success {
  background: rgba(7, 193, 96, 0.12);
}

.scan-status.state-error,
.scan-status.state-unsupported {
  background: rgba(239, 68, 68, 0.1);
}

.scan-status.state-login-qr,
.scan-status.state-contact-card {
  background: rgba(7, 193, 96, 0.1);
}

.status-icon {
  width: 44rpx;
  height: 44rpx;
  border-radius: 999rpx;
  background: rgba(15, 23, 42, 0.08);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  color: #111827;
}

.status-text {
  font-size: 24rpx;
  color: #475569;
}

.result-card {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  padding: 22rpx;
  border-radius: 26rpx;
  background: rgba(243, 244, 246, 0.9);
}

.result-card.success {
  background: rgba(7, 193, 96, 0.1);
}

.result-card.danger {
  background: rgba(239, 68, 68, 0.1);
}

.result-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #111827;
}

.result-meta,
.result-desc {
  font-size: 23rpx;
  color: #6b7280;
  line-height: 1.6;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.primary-btn,
.ghost-btn {
  width: 100%;
  min-height: 82rpx;
  border-radius: 22rpx;
  font-size: 26rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.primary-btn {
  background: #07c160;
  color: #ffffff;
}

.ghost-btn {
  background: rgba(15, 23, 42, 0.05);
  color: #111827;
}
</style>
