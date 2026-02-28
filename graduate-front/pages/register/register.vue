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
      
      <u-form :model="formData" :rules="formRules" ref="formRef">
        <!-- 账号 -->
        <!-- #ifdef H5 -->
        <view class="form-row">
          <text class="form-label">账号</text>
          <u-form-item prop="username" class="form-item-wrap" :border="false">
            <u-input
              v-model="formData.username"
              class="form-input"
              bgColor="transparent"
              :custom-style="inputStyle"
            ></u-input>
          </u-form-item>
        </view>
        <!-- #endif -->
        
        <!-- #ifdef MP-WEIXIN -->
        <view class="form-row">
          <text class="form-label">账号</text>
          <u-form-item prop="username" class="form-item-wrap" :border="false">
            <u-input
              v-model="formData.username"
              placeholder="请输入账号"
              class="form-input"
              bgColor="transparent"
              :custom-style="inputStyle"
            ></u-input>
          </u-form-item>
        </view>
        <!-- #endif -->

        <!-- 昵称 -->
        <!-- #ifdef H5 -->
        <view class="form-row">
          <text class="form-label">昵称</text>
          <u-form-item prop="nickname" class="form-item-wrap" :border="false">
            <u-input
              v-model="formData.nickname"
              class="form-input"
              bgColor="transparent"
              :custom-style="inputStyle"
            ></u-input>
          </u-form-item>
        </view>
        <!-- #endif -->
        
        <!-- #ifdef MP-WEIXIN -->
        <view class="form-row">
          <text class="form-label">昵称</text>
          <u-form-item prop="nickname" class="form-item-wrap" :border="false">
            <u-input
              v-model="formData.nickname"
              placeholder="请输入昵称"
              class="form-input"
              bgColor="transparent"
              :custom-style="inputStyle"
            ></u-input>
          </u-form-item>
        </view>
        <!-- #endif -->

        <!-- 手机号 -->
        <!-- #ifdef H5 -->
        <view class="form-row">
          <text class="form-label">手机号</text>
          <u-form-item prop="phone" class="form-item-wrap" :border="false">
            <u-input
              v-model="formData.phone"
              class="form-input"
              bgColor="transparent"
              :custom-style="inputStyle"
            ></u-input>
          </u-form-item>
        </view>
        <!-- #endif -->
        
        <!-- #ifdef MP-WEIXIN -->
        <view class="form-row">
          <text class="form-label">手机号</text>
          <u-form-item prop="phone" class="form-item-wrap" :border="false">
            <u-input
              v-model="formData.phone"
              placeholder="请输入手机号"
              class="form-input"
              bgColor="transparent"
              :custom-style="inputStyle"
            ></u-input>
          </u-form-item>
        </view>
        <!-- #endif -->

        <!-- 邮箱 + 验证码按钮 -->
        <!-- #ifdef H5 -->
        <view class="form-row">
          <text class="form-label">邮箱</text>
          <view class="email-code-box">
            <u-form-item prop="email" class="form-item-wrap form-item-email" :border="false">
              <u-input
                v-model="formData.email"
                class="form-input"
                bgColor="transparent"
                :custom-style="inputStyle"
              ></u-input>
            </u-form-item>
            <!-- 发送验证码按钮 -->
            <u-button 
              type="primary" 
              @click="sendCode" 
              :disabled="isSendingCode || countdown > 0"
              class="code-btn"
            >
              {{ countdown > 0 ? `${countdown}s后重发` : '发送验证码' }}
            </u-button>
          </view>
        </view>
        <!-- #endif -->
        
        <!-- #ifdef MP-WEIXIN -->
        <view class="form-row">
          <text class="form-label">邮箱</text>
          <view class="email-code-box">
            <u-form-item prop="email" class="form-item-wrap form-item-email" :border="false">
              <u-input
                v-model="formData.email"
                placeholder="请输入邮箱"
                class="form-input"
                bgColor="transparent"
                :custom-style="inputStyle"
              ></u-input>
            </u-form-item>
            <u-button 
              type="primary" 
              @click="sendCode" 
              :disabled="isSendingCode || countdown > 0"
              class="code-btn"
            >
              {{ countdown > 0 ? `${countdown}s后重发` : '发送验证码' }}
            </u-button>
          </view>
        </view>
        <!-- #endif -->

        <!-- 验证码输入框 -->
        <!-- #ifdef H5 -->
        <view class="form-row">
          <text class="form-label">验证码</text>
          <u-form-item prop="code" class="form-item-wrap" :border="false">
            <u-input
              v-model="formData.code"
              class="form-input"
              bgColor="transparent"
              :custom-style="inputStyle"
            ></u-input>
          </u-form-item>
        </view>
        <!-- #endif -->
        
        <!-- #ifdef MP-WEIXIN -->
        <view class="form-row">
          <text class="form-label">验证码</text>
          <u-form-item prop="code" class="form-item-wrap" :border="false">
            <u-input
              v-model="formData.code"
              placeholder="请输入验证码"
              class="form-input"
              bgColor="transparent"
              :custom-style="inputStyle"
            ></u-input>
          </u-form-item>
        </view>
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
        <view class="form-row">
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
        </view>
        <!-- #endif -->

        <!-- 确认密码 -->
        <!-- #ifdef H5 -->
        <view class="form-row">
          <text class="form-label">确认密码</text>
          <u-form-item prop="confirmPassword" class="form-item-wrap" :border="false">
            <u-input 
              v-model="formData.confirmPassword"
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
        <view class="form-row">
          <text class="form-label">确认密码</text>
          <u-form-item prop="confirmPassword" class="form-item-wrap" :border="false">
            <u-input
              v-model="formData.confirmPassword"
              placeholder="请再次输入密码"
              class="form-input"
              password  
              show-password
              bgColor="transparent"
              :custom-style="inputStyle"
            ></u-input>
          </u-form-item>
        </view>
        <!-- #endif -->

        <!-- 提交按钮 -->
        <u-button 
          type="primary" 
          @click="submitForm" 
          class="submit-btn"
          :loading="isLoading"
          :disabled="isLoading"
        >
          注册
        </u-button>
      </u-form>
	  
      <view class="login-footer">
	    <text
	       class="login-link-text" 
	       style="margin-left: 20rpx;"
	       @click="() => uni.navigateTo({ url: '/pages/index/index' })"
	     >返回登录</text>
	   </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import service from '@/utils/request'

const isLoading = ref(false)
const isSendingCode = ref(false)
const countdown = ref(0)
const formRef = ref(null)

const formData = ref({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: '',
  email: '',
  code: ''
})

const inputStyle = reactive({
  backgroundColor: 'rgba(255, 255, 255, 0.5)',
  border: '1px solid rgba(200, 200, 200, 0.2)',
  borderRadius: '8px',
  height: '40px',
  padding: '0 10px',
  fontSize: '16px',
  color: '#333'
})

// 自定义校验规则：确认密码必须和密码一致
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== formData.value.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const formRules = ref({
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度为3-20位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 16, message: '密码长度为6-16位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位', trigger: 'blur' }
  ]
})

// 发送邮箱验证码
const sendCode = async () => {
  // 先校验邮箱格式
  if (!formData.value.email) {
    uni.$u.toast('请先输入邮箱')
    return
  }
  const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailReg.test(formData.value.email)) {
    uni.$u.toast('请输入正确的邮箱格式')
    return
  }

  try {
    isSendingCode.value = true
    // 假设发送验证码接口为 /user/sendEmailCode
    await service({
      url: '/user/sendEmailCode',
      method: 'post',
      data: { email: formData.value.email }
    })
    uni.$u.toast('验证码已发送')
    
    // 开始倒计时
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (err) {
    uni.$u.toast(err.message || '发送失败，请稍后重试')
  } finally {
    isSendingCode.value = false
  }
}

const submitForm = async () => {
  try {
    const valid = await formRef.value.validate()
    if (valid) {
      isLoading.value = true
      // 调用注册接口
      await service({
        url: '/user/register',
        method: 'post',
        data: formData.value
      })
      await uni.$u.toast('注册成功，即将跳转到登录页')
      setTimeout(() => {
        uni.redirectTo({ url: '/pages/login/login' })
      }, 1500)
    }
  } catch (err) {
    if (err[0]?.message) {
      uni.$u.toast('表单验证失败：' + err[0].message)
    } else {
      uni.$u.toast(err.message || '注册失败，请稍后重试')
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<!-- 核心：全局样式强制覆盖原生input的浏览器默认白色 -->
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
  overflow-y: auto; /* 注册页面内容多，允许滚动 */
  padding: 40rpx 0;
}

/* 表单容器 */
.form-container {
  width: 80%;
  max-width: 1000rpx;
  min-height: 800rpx; /* 改为最小高度，适应更多输入框 */
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
  margin-bottom: 40rpx; /* 注册页面头像下边距调小 */
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 行布局 */
.form-row {
  display: flex;
  align-items: center;
  width: 100%;
  margin-bottom: 30rpx; /* 注册页面输入框间距调小 */
}

/* 标签样式 */
.form-label {
  display: inline-block;
  width: 80px;
  font-size: 32rpx;
  line-height: 80rpx;
  color: #333;
  margin-right: 16rpx;
  flex-shrink: 0;
}

/* 邮箱+验证码的组合容器 */
.email-code-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.form-item-email {
  flex: 1;
}

/* 验证码按钮样式 */
.code-btn {
  width: 200rpx !important;
  height: 40px !important;
  line-height: 40px !important;
  font-size: 24rpx !important;
  border-radius: 8px !important;
  background: rgba(0, 122, 255, 0.8) !important;
  border: none !important;
  flex-shrink: 0;
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

/* 登录页脚 */
.login-footer {
  display: flex;
  margin-top: 20rpx;
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

/* 核心：覆盖uni-app默认input样式 */
>>> .uni-input-input {
  height: 0;
  padding: 1.2em 0.5em;
  background-clip: content-box;
}
.login-link-text {
  color: #007AFF;
  font-size: 28rpx;
  text-decoration: underline;
}
</style>