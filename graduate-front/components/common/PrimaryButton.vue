<template>
  <view
    class="primary-button"
    :class="{
      'primary-button--loading': loading,
      'primary-button--disabled': disabled
    }"
    @click="handleClick"
  >
    <!-- 加载中显示 -->
    <view v-if="loading" class="primary-button__spinner"></view>

    <!-- 按钮文字 -->
    <text class="primary-button__text">{{ loading ? loadingText : text }}</text>
  </view>
</template>

<script setup>
const props = defineProps({
  text: {
    type: String,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  loadingText: {
    type: String,
    default: '加载中...'
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

const handleClick = () => {
  if (!props.loading && !props.disabled) {
    emit('click')
  }
}
</script>

<style scoped>
.primary-button {
  position: relative;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: var(--gradient-green);
  box-shadow: var(--shadow-green);
  border: none;
}

.primary-button:hover:not(.primary-button--disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-green-hover);
}

.primary-button:active:not(.primary-button--disabled) {
  transform: translateY(0);
}

.primary-button--loading,
.primary-button--disabled {
  opacity: 0.7;
  pointer-events: none;
}

.primary-button__spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #FFFFFF;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-right: 8px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.primary-button__text {
  position: relative;
  z-index: 1;
  font-size: 16px;
  font-weight: 600;
  color: #FFFFFF;
}
</style>
