<template>
  <view class="qr-login-container">
    <!-- 顶部标题 -->
    <view class="qr-header">
      <text class="qr-title">扫码登录</text>
      <text class="qr-subtitle">使用已登录设备扫描二维码</text>
    </view>

    <!-- 二维码显示区域 -->
    <view class="qr-code-area">
      <view v-if="loading" class="loading-spinner">
        <text>生成二维码中...</text>
      </view>

      <view v-else-if="qrCode" class="qr-code-wrapper">
        <!-- 显示二维码图片 -->
        <image :src="qrCode" mode="aspectFit" class="qr-code-image" />

        <!-- 二维码下方的提示信息 -->
        <view v-if="scanningStatus === 'pending'" class="status-pending">
          <text class="status-icon">📱</text>
          <text class="status-text">请在已登录的设备上扫描二维码</text>
          <text class="status-time">{{ timeRemaining }}秒后过期</text>
        </view>

        <view v-else-if="scanningStatus === 'scanned'" class="status-scanned">
          <text class="status-icon">✓</text>
          <text class="status-text">已扫码，请在设备上确认登录</text>
          <text class="user-info">{{ scannedUserInfo }}</text>
        </view>

        <view v-else-if="scanningStatus === 'expired'" class="status-expired">
          <text class="status-icon">⏰</text>
          <text class="status-text">二维码已过期</text>
        </view>

        <view v-else-if="scanningStatus === 'rejected'" class="status-rejected">
          <text class="status-icon">✗</text>
          <text class="status-text">登录被拒绝</text>
        </view>
      </view>

      <view v-else class="error-area">
        <text class="error-icon">⚠️</text>
        <text class="error-text">{{ errorMessage }}</text>
      </view>
    </view>

    <!-- 操作按钮 -->
    <view class="qr-actions">
      <button
        v-if="!qrCode || ['expired', 'rejected'].includes(scanningStatus)"
        @click="regenerateQrCode"
        class="btn-primary"
      >
        {{ qrCode ? '重新生成' : '生成二维码' }}
      </button>

      <button
        v-if="scanningStatus === 'pending'"
        @click="cancelQrLogin"
        class="btn-secondary"
      >
        取消
      </button>
    </view>

    <!-- 其他登录方式链接 -->
    <view class="other-login-methods">
      <text @click="switchToPasswordLogin" class="link"> 返回密码登录 </text>
    </view>
  </view>
</template>

<script>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import service from '@/utils/request'
import {
  generateQrCode,
  pollQrLoginStatus,
  saveLoginToken,
} from '@/utils/qr-login'

/**
 * QRLogin 组件 Props 类型定义
 * @typedef {Object} QRLoginProps
 * @property {boolean} [autoStart] - 是否自动开始轮询（默认true）
 * @property {number} [pollInterval] - 轮询间隔毫秒数（默认1000）
 * @property {number} [qrCodeExpireTime] - 二维码过期时间秒数（默认300）
 */

/**
 * 二维码扫描状态类型
 * @typedef {'pending'|'scanned'|'expired'|'rejected'|'confirmed'} ScanningStatus
 */

export default {
  name: 'QRLogin',
  emits: ['login-success', 'switch-method'],
  props: {
    /**
     * 是否自动开始轮询
     * @type {boolean}
     * @default true
     */
    autoStart: {
      type: Boolean,
      default: true,
    },
    /**
     * 轮询间隔（毫秒）
     * @type {number}
     * @default 1000
     */
    pollInterval: {
      type: Number,
      default: 1000,
      validator: (value) => value >= 500,
    },
    /**
     * 二维码过期时间（秒）
     * @type {number}
     * @default 300
     */
    qrCodeExpireTime: {
      type: Number,
      default: 300,
      validator: (value) => value > 0,
    },
  },
  setup(props, { emit }) {
    // 响应式数据
    const loading = ref(false)
    const qrCode = ref('')
    const qrId = ref('')
    const errorMessage = ref('')
    const scanningStatus = ref('pending') // pending, scanned, expired, rejected, confirmed
    const scannedUserInfo = ref('')
    const timeRemaining = ref(300) // 剩余时间（秒）
    let countdownTimer = null
    let pollTimer = null

    /**
     * 生成二维码
     */
    const regenerateQrCode = async () => {
      loading.value = true
      errorMessage.value = ''
      scanningStatus.value = 'pending'
      timeRemaining.value = 300

      try {
        const result = await generateQrCode()
        qrCode.value = result.qrCode
        qrId.value = result.qrId
        startCountdown()
        startPolling()
      } catch (e) {
        errorMessage.value = '生成二维码失败，请重试'
        console.error('生成二维码异常', e)
      } finally {
        loading.value = false
      }
    }

    /**
     * 开始倒计时
     */
    const startCountdown = () => {
      if (countdownTimer) clearInterval(countdownTimer)
      countdownTimer = setInterval(() => {
        timeRemaining.value--
        if (timeRemaining.value <= 0) {
          clearInterval(countdownTimer)
          scanningStatus.value = 'expired'
          stopPolling()
        }
      }, 1000)
    }

    /**
     * 开始轮询登录状态
     */
    const startPolling = () => {
      if (pollTimer) clearInterval(pollTimer)

      // 立即检查一次
      checkLoginStatus()

      // 然后每 1 秒检查一次
      pollTimer = setInterval(() => {
        if (
          qrId.value &&
          !['confirmed', 'expired', 'rejected'].includes(scanningStatus.value)
        ) {
          checkLoginStatus()
        }
      }, 1000)
    }

    /**
     * 检查登录状态
     */
    const checkLoginStatus = async () => {
      try {
        const result = await pollQrLoginStatus(qrId.value, 1) // 只检查一次，不重试
        if (result && result.success) {
          // 登录成功
          scanningStatus.value = 'confirmed'
          stopPolling()
          stopCountdown()
          saveLoginToken(result.token)

          // 延迟跳转，让用户看到确认界面
          setTimeout(() => {
            emit('login-success', {
              userInfo: result.userInfo,
              token: result.token,
            })
          }, 1000)
        } else if (result && result.message) {
          // 检查状态但未登载，可能是 pending 或 scanned
          // 从后端获取更详细的状态信息
          await checkDetailedStatus()
        }
      } catch (e) {
        console.warn('轮询登录状态异常', e)
      }
    }

    /**
     * 检查详细的二维码状态（从后端获取）
     */
    const checkDetailedStatus = async () => {
      try {
        const res = await service.get(
          `/user/qrcode/status?qrCodeId=${qrId.value}`,
        )

        if (res && res.code === 200) {
          const { status, token } = res.data

          if (status === 'scanned') {
            scanningStatus.value = 'scanned'
            scannedUserInfo.value = '某用户'
          } else if (status === 'confirmed') {
            scanningStatus.value = 'confirmed'
            stopPolling()
            stopCountdown()
            saveLoginToken(token)

            setTimeout(() => {
              emit('login-success', { token })
            }, 500)
          } else if (status === 'expired') {
            scanningStatus.value = 'expired'
            stopPolling()
            stopCountdown()
          }
        }
      } catch (e) {
        console.warn('检查二维码状态失败', e)
      }
    }

    /**
     * 停止轮询
     */
    const stopPolling = () => {
      if (pollTimer) {
        clearInterval(pollTimer)
        pollTimer = null
      }
    }

    /**
     * 停止倒计时
     */
    const stopCountdown = () => {
      if (countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }

    /**
     * 取消二维码登录
     */
    const cancelQrLogin = () => {
      stopPolling()
      stopCountdown()
      qrCode.value = ''
      qrId.value = ''
      scanningStatus.value = 'pending'
      emit('switch-method', 'password')
    }

    /**
     * 切换到密码登录
     */
    const switchToPasswordLogin = () => {
      stopPolling()
      stopCountdown()
      emit('switch-method', 'password')
    }

    // 组件挂载时生成二维码
    onMounted(() => {
      regenerateQrCode()
    })

    // 组件卸载前清理定时器
    onBeforeUnmount(() => {
      stopPolling()
      stopCountdown()
    })

    return {
      loading,
      qrCode,
      qrId,
      errorMessage,
      scanningStatus,
      scannedUserInfo,
      timeRemaining,
      regenerateQrCode,
      cancelQrLogin,
      switchToPasswordLogin,
    }
  },
}
</script>

<style scoped lang="scss">
.qr-login-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 40rpx 30rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.qr-header {
  text-align: center;
  margin-bottom: 50rpx;
  color: white;
}

.qr-title {
  display: block;
  font-size: 40rpx;
  font-weight: 600;
  margin-bottom: 10rpx;
}

.qr-subtitle {
  display: block;
  font-size: 28rpx;
  opacity: 0.8;
}

.qr-code-area {
  background: white;
  border-radius: 16rpx;
  padding: 40rpx;
  margin-bottom: 40rpx;
  box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.3);
  min-width: 300rpx;
}

.loading-spinner {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400rpx;
  font-size: 28rpx;
  color: #999;
}

.qr-code-wrapper {
  text-align: center;
}

.qr-code-image {
  width: 300rpx;
  height: 300rpx;
  margin-bottom: 30rpx;
  border: 2rpx solid #eee;
  border-radius: 8rpx;
}

.status-pending,
.status-scanned,
.status-expired,
.status-rejected {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 30rpx;
  padding: 20rpx;
  border-radius: 8rpx;
}

.status-pending {
  background: #f0f7ff;
  border: 1rpx solid #b3d8ff;
}

.status-scanned {
  background: #f6ffed;
  border: 1rpx solid #b7eb8f;
}

.status-expired {
  background: #fff7e6;
  border: 1rpx solid #ffd591;
}

.status-rejected {
  background: #fff1f0;
  border: 1rpx solid #ffccc7;
}

.status-icon {
  font-size: 60rpx;
  margin-bottom: 10rpx;
}

.status-text {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
  margin-bottom: 5rpx;
}

.status-time {
  font-size: 24rpx;
  color: #999;
}

.user-info {
  font-size: 26rpx;
  color: #666;
  margin-top: 10rpx;
}

.error-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400rpx;
}

.error-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.error-text {
  font-size: 28rpx;
  color: #d9534f;
  text-align: center;
}

.qr-actions {
  display: flex;
  gap: 20rpx;
  width: 100%;
  max-width: 400rpx;
  margin: 0 auto 30rpx;
}

.btn-primary,
.btn-secondary {
  flex: 1;
  padding: 16rpx 30rpx;
  border-radius: 8rpx;
  font-size: 28rpx;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:active {
  opacity: 0.8;
}

.btn-secondary {
  background: white;
  color: #667eea;
  border: 2rpx solid #667eea;
}

.btn-secondary:active {
  background: #f5f5f5;
}

.other-login-methods {
  text-align: center;
  margin-top: 20rpx;
}

.link {
  color: white;
  font-size: 26rpx;
  text-decoration: underline;
  cursor: pointer;
}

.link:active {
  opacity: 0.7;
}
</style>
