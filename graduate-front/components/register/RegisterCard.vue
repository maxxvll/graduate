<template>
  <view class="register-card">
    <!-- 头像容器 -->
    <view class="avatar-wrap">
      <image
        class="avatar-img"
        src="/static/logo.png"
        mode="aspectFill"
      ></image>
    </view>

    <slot></slot>

    <!-- 返回登录链接 -->
    <view class="login-footer">
      <text
        class="login-link-text"
        @click="$emit('navigate-to-login')"
      >
        返回登录
      </text>
    </view>
  </view>
</template>

<script setup>
const emit = defineEmits(['navigate-to-login'])
</script>

<style scoped>
.register-card {
  position: relative;
  width: 100%;
  max-width: 440px;
  padding: 48px;
  background: var(--bg-secondary);
  border-radius: 32px;
  box-shadow: var(--shadow-xl);
  z-index: 10;
  backdrop-filter: blur(10px);
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 头像容器 */
.avatar-wrap {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  margin-bottom: 24px;
  border: 3px solid var(--green-primary);
  box-shadow: var(--shadow-green);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 返回登录链接 */
.login-footer {
  margin-top: 32px;
  text-align: center;
}

.login-link-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color 0.3s;
}

.login-link-text:hover {
  color: var(--green-primary);
}

.login-link-text:active {
  opacity: 0.7;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .register-card {
    padding: 40px 24px;
    border-radius: 24px;
  }

  .avatar-wrap {
    width: 64px;
    height: 64px;
  }
}
</style>
