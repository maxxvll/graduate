<template>
  <view class="login-card">
    <slot></slot>
  </view>
</template>

<script setup>
// LoginCard - 登录卡片容器组件
// 提供统一的卡片样式和布局
</script>

<style scoped>
.login-card {
  position: relative;
  width: 100%;
  max-width: 440px;
  padding: 48px;
  background: var(--bg-secondary);
  border-radius: 32px;
  box-shadow: var(--shadow-xl);
  z-index: 10;
  backdrop-filter: blur(10px);
}

/* 响应式设计 */
@media (max-width: 640px) {
  .login-card {
    padding: 40px 24px;
    border-radius: 24px;
  }
}
</style>
