<template>
  <view class="footer-links">
    <!-- #ifdef H5 -->
    <text
      v-if="showQRCodeLink"
      class="modern-link"
      @click="$emit('switch-to-qrcode')"
    >
      二维码登录
    </text>
    <text
      class="modern-link"
      @click="$emit('navigate-to-register')"
    >
      创建账号
    </text>
    <!-- #endif -->

    <!-- #ifndef H5 -->
    <text
      class="modern-link"
      @click="$emit('navigate-to-register')"
    >
      创建账号
    </text>
    <!-- #endif -->
  </view>
</template>

<script setup>
const props = defineProps({
  showQRCodeLink: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['navigate-to-register', 'switch-to-qrcode'])
</script>

<style scoped>
.footer-links {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  margin-top: 24px;
  align-items: center;
}

.modern-link {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  padding: 4px 8px;
}

.modern-link:hover {
  color: var(--green-primary);
}

.modern-link:active {
  opacity: 0.7;
  transform: scale(0.98);
}
</style>
