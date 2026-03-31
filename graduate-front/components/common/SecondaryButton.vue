<template>
  <view
    class="secondary-button"
    :class="{
      'secondary-button--loading': loading,
      'secondary-button--disabled': disabled
    }"
    @click="handleClick"
  >
    <!-- 加载中显示 -->
    <view v-if="loading" class="secondary-button__spinner"></view>

    <!-- 按钮文字 -->
    <text class="secondary-button__text">{{ loading ? loadingText : text }}</text>
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
.secondary-button {
  position: relative;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: var(--bg-secondary);
  border: 1.5px solid var(--border-subtle);
}

.secondary-button:hover:not(.secondary-button--disabled) {
  border-color: var(--border-medium);
  background: var(--bg-tertiary);
}

.secondary-button:active:not(.secondary-button--disabled) {
  transform: scale(0.98);
}

.secondary-button--loading,
.secondary-button--disabled {
  opacity: 0.6;
  pointer-events: none;
}

.secondary-button__spinner {
  width: 20px;
  height: 20px;
  border: 2px solid var(--border-medium);
  border-top-color: var(--green-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-right: 8px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.secondary-button__text {
  position: relative;
  z-index: 1;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}
</style>
