<template>
  <view class="page-wrap">
    <view class="form-container">
      <!-- 头像容器 -->
      <view class="avatar-wrap">
        <image 
          class="avatar-img"
          src="/static/logo.png" 
          mode="aspectFill"
        ></image>
      </view>
      
      <!-- 密码登录模式 -->
      <view v-if="loginMode === 'password'" class="login-form-wrapper">
        <u-form :model="formData" :rules="formRules" ref="formRef">
          <!-- 账号 -->
          <!-- #ifdef H5 -->
          <view class="form-row">
            <text class="form-label">账号</text>
            <u-form-item prop="account" class="form-item-wrap" :border="false">
              <u-input
                v-model="formData.account"
                class="form-input"
                bgColor="transparent"
                :custom-style="inputStyle"
              ></u-input>
            </u-form-item>
          </view>
          <!-- #endif -->
          
          <!-- #ifdef MP-WEIXIN -->
          <text class="form-label">账号</text>
          <u-form-item prop="account" class="form-item-wrap" :border="false">
            <u-input
              v-model="formData.account"
              placeholder="请输入账号"
              class="form-input"
              bgColor="transparent"
              :custom-style="inputStyle"
            ></u-input>
          </u-form-item>
          <!-- #endif -->

          <!-- 密码 -->
          <!-- #ifdef H5 -->
          <view class="form-row">
            <text class="form-label">密码</text>
            <u-form-item prop="password" class="form-item-wrap" :border="false">
              <u-input 
                v-model="formData.password"
                class="form-input"
                password  
                show-password
                bgColor="transparent"
                :custom-style="inputStyle"
              ></u-input>
            </u-form-item>
          </view>
          <!-- #endif -->
          <!-- #ifdef MP-WEIXIN -->
          <text class="form-label">密码</text>
          <u-form-item prop="password" class="form-item-wrap" :border="false">
            <u-input
              v-model="formData.password"
              placeholder="请输入密码"
              class="form-input"
              password  
              show-password
              bgColor="transparent"
              :custom-style="inputStyle"
            ></u-input>
          </u-form-item>
          <!-- #endif -->

          <!-- 提交按钮 -->
          <u-button 
            type="primary" 
            @click="submitForm" 
            class="submit-btn"
            :loading="isLoading"
            :disabled="isLoading"
          >
            登录
          </u-button>
        </u-form>

        <!-- 登录页脚 -->
        <view class="login-footer">
          <text 
            class="login-link-text" 
            @click="switchLoginMode('qrcode')"
          >扫码登录</text>
          
          <text 
            class="login-link-text" 
            style="margin-left: 20rpx;"
            @click="() => uni.navigateTo({ url: '/pages/register/register' })"
          >注册账号</text>
        </view>
      </view>

      <!-- 二维码登录模式 -->
      <view v-else class="qrcode-login-wrapper">
        <!-- 二维码显示区域 -->
        <view class="qrcode-container">
          <image 
            v-if="qrCodeBase64" 
            :src="qrCodeBase64"
            class="qrcode-image"
            mode="aspectFit"
          ></image>
          <view v-else class="qrcode-placeholder">
            <text class="qrcode-loading">生成二维码中...</text>
          </view>
        </view>

        <!-- 倒计时和状态提示 -->
        <view class="qrcode-status">
          <text v-if="qrCodeStatus === 'waiting'" class="status-text">
            请使用已登录设备扫描二维码
          </text>
          <text v-else-if="qrCodeStatus === 'scanned'" class="status-text scanned">
            ✓ 已扫描，请在设备上确认
          </text>
          <text v-else-if="qrCodeStatus === 'confirmed'" class="status-text confirmed">
            ✓ 登录成功，即将跳转...
          </text>
          <text v-else-if="qrCodeStatus === 'expired'" class="status-text expired">
            二维码已过期，请重新生成
          </text>

          <!-- 倒计时显示 -->
          <view class="countdown">
            <text class="countdown-text">{{ timeRemaining }}s</text>
          </view>
        </view>

        <!-- 返回密码登录按钮 -->
        <u-button 
          type="primary" 
          @click="switchLoginMode('password')"
          class="submit-btn"
        >
          返回密码登录
        </u-button>

        <!-- 重新生成二维码按钮 -->
        <u-button 
          v-if="qrCodeStatus === 'expired'" 
          type="primary" 
          @click="regenerateQrCode"
          class="submit-btn"
          style="margin-top: 20rpx;"
        >
          重新生成二维码
        </u-button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import service from '@/utils/request'

// 登录模式：'password' 或 'qrcode'
const loginMode = ref('password')

// 密码登录相关
const isLoading = ref(false)
const formData = ref({ account: '', password: '' })
const inputStyle = reactive({
  backgroundColor: 'rgba(255, 255, 255, 0.5)',
  border: '1px solid rgba(200, 200, 200, 0.2)',
  borderRadius: '8px',
  height: '40px',
  padding: '0 10px',
  fontSize: '16px',
  color: '#333'
})

const formRules = ref({
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度为3-20位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 16, message: '密码长度为6-16位', trigger: 'blur' }
  ]
})
const formRef = ref(null)

// 二维码登录相关
const qrCodeBase64 = ref('')
const qrCodeId = ref('')
const qrCodeStatus = ref('waiting') // waiting, scanned, confirmed, expired
const timeRemaining = ref(300)
const pollingTimer = ref(null)
const countdownTimer = ref(null)

/**
 * 切换登录模式
 */
const switchLoginMode = (mode) => {
  if (mode === 'qrcode') {
    loginMode.value = 'qrcode'
    generateQrCode()
  } else {
    loginMode.value = 'password'
    // 停止轮询和倒计时
    stopPolling()
    stopCountdown()
    qrCodeStatus.value = 'waiting'
    timeRemaining.value = 300
  }
}

/**
 * 生成二维码
 */
const generateQrCode = async () => {
  try {
    qrCodeStatus.value = 'waiting'
    timeRemaining.value = 300
    
    const res = await service({
      url: '/user/qrcode/generate',
      method: 'get'
    })
    
    qrCodeBase64.value = res.data.qrCodeBase64
    qrCodeId.value = res.data.qrCodeId
    
    // 开始轮询和倒计时
    startPolling()
    startCountdown()
  } catch (err) {
    uni.$u.toast('生成二维码失败：' + (err.message || '未知错误'))
  }
}

/**
 * 重新生成二维码
 */
const regenerateQrCode = () => {
  stopPolling()
  stopCountdown()
  generateQrCode()
}

/**
 * 开始轮询检查二维码状态
 */
const startPolling = () => {
  stopPolling() // 先清除之前的轮询
  
  pollingTimer.value = setInterval(async () => {
    try {
      const res = await service({
        url: `/user/qrcode/status?qrCodeId=${qrCodeId.value}`,
        method: 'get'
      })
      
      const status = res.data.status
      qrCodeStatus.value = status
      
      if (status === 'confirmed') {
        // 登录成功，保存token
        const token = res.data.token
        uni.setStorageSync('satoken', token)
        
        stopPolling()
        stopCountdown()
        
        uni.$u.toast('登录成功，即将进入首页')
        setTimeout(() => {
          uni.redirectTo({ url: '/pages/home/home' })
        }, 1500)
      } else if (status === 'expired') {
        stopPolling()
        stopCountdown()
      }
    } catch (err) {
      console.error('轮询失败:', err)
    }
  }, 1000) // 每1秒轮询一次
}

/**
 * 停止轮询
 */
const stopPolling = () => {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value)
    pollingTimer.value = null
  }
}

/**
 * 开始倒计时
 */
const startCountdown = () => {
  stopCountdown() // 先清除之前的倒计时
  
  countdownTimer.value = setInterval(() => {
    timeRemaining.value--
    
    if (timeRemaining.value <= 0) {
      qrCodeStatus.value = 'expired'
      stopCountdown()
      stopPolling()
    }
  }, 1000)
}

/**
 * 停止倒计时
 */
const stopCountdown = () => {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value)
    countdownTimer.value = null
  }
}

/**
 * 密码登录提交
 */
const submitForm = async () => {
  try {
    const valid = await formRef.value.validate()
    if (valid) {
      isLoading.value = true
      const res = await service({
        url: '/user/login',
        method: 'post',
        data: {
          username: formData.value.account,
          password: formData.value.password
        }
      })
      uni.setStorageSync('satoken', res.data)
      await uni.$u.toast('登录成功，即将进入首页')
      setTimeout(() => {
        uni.redirectTo({ url: '/pages/home/home' })
      }, 1500)
    }
  } catch (err) {
    if (err[0]?.message) {
      uni.$u.toast('表单验证失败：' + err[0].message)
    } else {
      uni.$u.toast(err.message || '登录失败，请检查账号密码')
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<!-- 核心修改3：全局样式强制覆盖原生input的浏览器默认白色 -->
<style>
.form-input .u-input__content input {
  background-color: inherit !important;
  -webkit-appearance: none !important;
  appearance: none !important;
  outline: none !important;
}
</style>

<style scoped>
/* 重置全局样式 */
page, view, text, input {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 页面底层容器 */
.page-wrap {
  height: 100vh;
  display: flex;
  align-items: center; 
  justify-content: center; 
  background: url('https://cn.bing.com/th?id=OHR.LoughriggTarn_ZH-CN1404327665_1920x1080.jpg') no-repeat center;
  background-size: cover;
}

/* 表单容器 */
.form-container {
  width: 80%;
  max-width: 1000rpx;
  min-height: 800rpx;
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(12rpx);
  -webkit-backdrop-filter: blur(12rpx);
  border-radius: 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
  padding: 40rpx 30rpx;
  display: flex;
  flex-direction: column; 
  align-items: center;
}

/* 头像容器 */
.avatar-wrap {
  width: 150rpx;
  height: 150rpx;
  border-radius: 50%;
  overflow: hidden;
  margin-bottom: 60rpx;
  visibility: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 登录模式容器 */
.login-form-wrapper,
.qrcode-login-wrapper {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 行布局 */
.form-row {
  display: flex;
  align-items: center;
  width: 100%;
  margin-bottom: 40rpx;
}

/* 标签样式 */
.form-label {
  display: inline-block;
  width: 80px;
  font-size: 32rpx;
  line-height: 80rpx;
  color: #333;
  margin-right: 16rpx;
}

/* 表单项容器 */
.form-item-wrap {
  flex: 1;
  margin: 0 !important;
  padding: 0 !important;
}

/* 清空u-form-item的默认样式 */
:deep(.form-item-wrap .u-form-item),
:deep(.form-item-wrap .u-form-item__wrapper),
:deep(.form-item-wrap .u-form-item__body),
:deep(.form-item-wrap .u-form-item__content) {
  background: transparent !important;
  border: none !important;
  padding: 0 !important;
  margin: 0 !important;
  width: 100% !important;
}

/* 清空u-input的外层容器样式 */
:deep(.form-input .u-input),
:deep(.form-input .u-input__wrapper),
:deep(.form-input .u-input__content-wrapper) {
  background: transparent !important;
  border: none !important;
  padding: 0 !important;
  margin: 0 !important;
  width: 100% !important;
}

/* 占位符样式 */
:deep(.form-input .u-input__content::placeholder) {
  color: #999 !important;
}

/* 密码框眼睛图标 */
:deep(.form-input .u-input__right-icon) {
  background: transparent !important;
  border: none !important;
  height: 40px !important;
  display: flex;
  align-items: center;
  padding-right: 10px !important;
}

/* 错误提示样式 */
:deep(.u-form-item__error) {
  display: block !important;
  margin-top: 8rpx !important;
  font-size: 24rpx !important;
  color: #fa3534 !important;
  line-height: 1.2 !important;
  padding-left: 20rpx !important;
}

/* 提交按钮样式 */
.submit-btn {
  margin-top: 20rpx;
  width: 100%;
  height: 88rpx !important;
  line-height: 88rpx !important;
  font-size: 32rpx !important;
  border-radius: 12rpx !important;
  background: rgba(0, 122, 255, 0.8) !important;
  border: none !important;
}

/* 登录页脚 */
.login-footer {
  display: flex;
  margin-top: 20rpx;
}

.login-link-text {
  color: #007AFF;
  font-size: 28rpx;
  text-decoration: underline;
  cursor: pointer;
}

/* 二维码相关样式 */
.qrcode-container {
  width: 300rpx;
  height: 300rpx;
  border: 2rpx solid #ddd;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  margin-bottom: 40rpx;
}

.qrcode-image {
  width: 100%;
  height: 100%;
  border-radius: 6rpx;
}

.qrcode-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.qrcode-loading {
  font-size: 28rpx;
  color: #999;
}

/* 二维码状态信息 */
.qrcode-status {
  width: 100%;
  text-align: center;
  margin-bottom: 40rpx;
}

.status-text {
  display: block;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 20rpx;
}

.status-text.scanned {
  color: #07c160;
}

.status-text.confirmed {
  color: #07c160;
}

.status-text.expired {
  color: #fa5151;
}

.countdown {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100rpx;
  height: 100rpx;
  border: 2rpx solid #ddd;
  border-radius: 50%;
  margin: 0 auto;
}

.countdown-text {
  font-size: 48rpx;
  font-weight: bold;
  color: #007AFF;
}

/* 自动填充样式 */
:deep(u-input:-webkit-autofill) {
  color: white !important;
  background-color: transparent !important;
  -webkit-text-fill-color: white !important;
  transition: background-color 5000s ease-in-out 0s !important;
  -webkit-box-shadow: 0 0 0 400px transparent inset;
}

u-input:-webkit-autofill {
  transition: background-color 5000s ease-in-out 0s;
}

.uni-input-input {
  height: 0;
  padding: 1.2em 0.5em;
  background-clip: content-box;
}
</style>