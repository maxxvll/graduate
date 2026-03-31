<template>
  <view class="qrcode-mode-wrapper">
    <view class="qrcode-card">
      <!-- Scanner Rings -->
      <view class="qrcode-scanner-ring"></view>
      <view class="qrcode-scanner-ring delay-1"></view>

      <!-- QR Code Display -->
      <view class="qrcode-display">
        <image
          v-if="qrCodeBase64"
          :src="qrCodeBase64"
          class="qrcode-image"
          mode="aspectFit"
        ></image>
        <view v-else class="qrcode-loader">
          <view class="loader-spinner"></view>
          <text class="loader-text">生成中...</text>
        </view>
      </view>

      <!-- Status Panel -->
      <view class="qrcode-status-panel">
        <view class="status-indicator" :class="`status-${qrCodeStatus}`">
          <view class="status-dot"></view>
        </view>
        <text class="status-text" :class="`status-${qrCodeStatus}`">
          {{ statusText }}
        </text>
        <view class="countdown-ring">
          <text class="countdown-number">{{ timeRemaining }}</text>
        </view>
      </view>

      <!-- Action Buttons -->
      <view class="qrcode-actions">
        <SecondaryButton
          text="返回密码登录"
          @click="$emit('switch-mode', 'password')"
        />
        <PrimaryButton
          v-if="qrCodeStatus === 'expired'"
          text="重新生成"
          @click="$emit('regenerate')"
        />
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import SecondaryButton from '@/components/common/SecondaryButton.vue'

const props = defineProps({
  qrCodeBase64: {
    type: String,
    default: ''
  },
  qrCodeStatus: {
    type: String,
    default: 'waiting'
  },
  timeRemaining: {
    type: Number,
    default: 300
  }
})

const emit = defineEmits(['regenerate', 'switch-mode'])

const statusText = computed(() => {
  const statusMap = {
    waiting: '请使用手机App扫码登录',
    scanned: '扫描成功，请在手机上确认',
    confirmed: '登录成功，正在跳转...',
    expired: '二维码已过期'
  }
  return statusMap[props.qrCodeStatus] || '请使用手机App扫码登录'
})
</script>

<style scoped>
.qrcode-mode-wrapper {
  width: 100%;
  display: flex;
  justify-content: center;
}

.qrcode-card {
  position: relative;
  width: 100%;
  max-width: 360px;
  padding: 40px;
  background: var(--bg-secondary);
  border-radius: 24px;
  box-shadow: var(--shadow-lg);
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* Scanner Rings */
.qrcode-scanner-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 280px;
  height: 280px;
  border-radius: 50%;
  border: 2px solid var(--green-primary);
  opacity: 0;
  animation: scanner-pulse 2s ease-in-out infinite;
}

.qrcode-scanner-ring.delay-1 {
  animation-delay: 1s;
}

@keyframes scanner-pulse {
  0% {
    transform: translate(-50%, -50%) scale(0.8);
    opacity: 0.5;
  }
  50% {
    transform: translate(-50%, -50%) scale(1.2);
    opacity: 0.2;
  }
  100% {
    transform: translate(-50%, -50%) scale(0.8);
    opacity: 0.5;
  }
}

/* QR Code Display */
.qrcode-display {
  position: relative;
  width: 200px;
  height: 200px;
  background: var(--bg-primary);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  padding: 16px;
}

.qrcode-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qrcode-loader {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.loader-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border-subtle);
  border-top-color: var(--green-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loader-text {
  font-size: 13px;
  color: var(--text-tertiary);
}

/* Status Panel */
.qrcode-status-panel {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding: 12px 16px;
  background: var(--bg-tertiary);
  border-radius: 12px;
  width: 100%;
  justify-content: space-between;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-tertiary);
  transition: all 0.3s;
}

.status-waiting .status-dot {
  background: var(--text-tertiary);
  animation: pulse 2s ease-in-out infinite;
}

.status-scanned .status-dot {
  background: var(--accent-blue);
}

.status-confirmed .status-dot {
  background: var(--green-primary);
}

.status-expired .status-dot {
  background: var(--accent-red);
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.status-text {
  flex: 1;
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
  text-align: left;
}

.countdown-ring {
  min-width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--border-subtle);
}

.countdown-number {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
}

/* Action Buttons */
.qrcode-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}
</style>
