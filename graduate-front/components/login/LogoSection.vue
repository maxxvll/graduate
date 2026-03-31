<template>
  <view class="logo-section">
    <view class="logo-container">
      <image
        class="logo-image"
        :src="logoSrc"
        mode="aspectFill"
      ></image>
    </view>
    <text class="app-title">{{ appName }}</text>
    <text class="app-subtitle">{{ appSubtitle }}</text>
  </view>
</template>

<script setup>
const props = defineProps({
  logoSrc: {
    type: String,
    default: '/static/logo.png'
  },
  appName: {
    type: String,
    default: 'Chat'
  },
  appSubtitle: {
    type: String,
    default: 'CONNECT WITH FRIENDS'
  }
})
</script>

<style scoped>
.logo-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 40px;
  text-align: center;
}

.logo-container {
  position: relative;
  width: 80px;
  height: 80px;
  margin-bottom: 20px;
}

.logo-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.app-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
  letter-spacing: -0.5px;
  background: var(--gradient-text);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.app-subtitle {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-tertiary);
  letter-spacing: 2px;
  text-transform: uppercase;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .app-title {
    font-size: 24px;
  }

  .logo-container {
    width: 64px;
    height: 64px;
  }
}
</style>
