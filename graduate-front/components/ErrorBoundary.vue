<template>
  <view v-if="hasError" class="error-boundary">
    <view class="error-content">
      <text class="error-icon">⚠️</text>
      <text class="error-title">出错了</text>
      <text class="error-message">{{ errorMessage }}</text>
      <view class="error-actions">
        <button class="retry-btn" @click="retry">重试</button>
        <button class="reload-btn" @click="reload">刷新页面</button>
      </view>
      <view v-if="showDetails" class="error-details">
        <text class="error-stack">{{ errorStack }}</text>
      </view>
      <text class="toggle-details" @click="toggleDetails">
        {{ showDetails ? '隐藏' : '显示' }}详情
      </text>
    </view>
  </view>
  <slot v-else />
</template>

<script setup>
import { ref, onErrorCaptured } from 'vue'

const props = defineProps({
  // 是否显示详细错误信息
  showDetails: {
    type: Boolean,
    default: false,
  },
  // 错误回调函数
  onError: {
    type: Function,
    default: null,
  },
})

const emit = defineEmits(['error'])

const hasError = ref(false)
const errorMessage = ref('')
const errorStack = ref('')
const showDetails = ref(props.showDetails)

/**
 * 捕获子组件错误
 */
onErrorCaptured((error, instance, info) => {
  console.error('[ErrorBoundary] 捕获到错误:', error, info)

  hasError.value = true
  errorMessage.value = error.message || '未知错误'
  errorStack.value = error.stack || ''

  // 调用错误回调
  if (props.onError) {
    props.onError(error, instance, info)
  }

  // 触发错误事件
  emit('error', { error, instance, info })

  // 返回 false 阻止错误继续向上传播
  return false
})

/**
 * 重试（重新渲染）
 */
const retry = () => {
  hasError.value = false
  errorMessage.value = ''
  errorStack.value = ''
}

/**
 * 刷新页面
 */
const reload = () => {
  uni.reLaunch({
    url: getCurrentPages().pop().route,
  })
}

/**
 * 切换显示详情
 */
const toggleDetails = () => {
  showDetails.value = !showDetails.value
}

// 暴露方法供外部调用
defineExpose({
  retry,
  hasError,
})
</script>

<style scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 20px;
  background: #fff5f5;
  border-radius: 8px;
}

.error-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  max-width: 500px;
  text-align: center;
}

.error-icon {
  font-size: 64px;
}

.error-title {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.error-message {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}

.error-actions {
  display: flex;
  gap: 12px;
}

.retry-btn,
.reload-btn {
  padding: 10px 24px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.retry-btn {
  background: #07c160;
  color: #fff;
}

.retry-btn:hover {
  background: #06ad56;
}

.reload-btn {
  background: #f0f0f0;
  color: #333;
}

.reload-btn:hover {
  background: #e0e0e0;
}

.error-details {
  margin-top: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 4px;
  max-height: 200px;
  overflow-y: auto;
}

.error-stack {
  font-size: 12px;
  color: #999;
  font-family: monospace;
  white-space: pre-wrap;
  word-break: break-all;
}

.toggle-details {
  font-size: 12px;
  color: #07c160;
  cursor: pointer;
  text-decoration: underline;
}
</style>
