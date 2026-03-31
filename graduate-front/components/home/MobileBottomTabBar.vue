<template>
  <view class="mobile-bottom-tabbar">
    <view
      v-for="item in tabs"
      :key="item.key"
      class="tab-item"
      :class="{ active: modelValue === item.key }"
      @click="$emit('update:modelValue', item.key)"
    >
      <view class="tab-icon-wrap">
        <text class="tab-icon">{{ item.icon }}</text>
        <text v-if="resolveBadge(item.key)" class="tab-badge">
          {{ resolveBadge(item.key) > 99 ? '99+' : resolveBadge(item.key) }}
        </text>
      </view>
      <text class="tab-label">{{ item.label }}</text>
      <view class="tab-indicator"></view>
    </view>
  </view>
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: String,
    default: 'chat',
  },
  unreadCount: {
    type: Number,
    default: 0,
  },
  requestCount: {
    type: Number,
    default: 0,
  },
})

defineEmits(['update:modelValue'])

const tabs = [
  { key: 'chat', label: '聊天', icon: '💬' },
  { key: 'contacts', label: '通讯录', icon: '📇' },
  { key: 'cloud', label: '云盘', icon: '☁️' },
]

const resolveBadge = (key) => {
  if (key === 'chat') return Number(props.unreadCount || 0)
  if (key === 'contacts') return Number(props.requestCount || 0)
  return 0
}
</script>

<style scoped>
.mobile-bottom-tabbar {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  align-items: center;
  height: calc(122rpx + env(safe-area-inset-bottom, 0px));
  padding: 10rpx 18rpx calc(12rpx + env(safe-area-inset-bottom, 0px));
  background: rgba(249, 250, 251, 0.98);
  border-top: 1rpx solid rgba(15, 23, 42, 0.08);
}
/* backdrop-filter fallback for unsupported devices */
@supports (backdrop-filter: blur(10px)) or (-webkit-backdrop-filter: blur(10px)) {
  .mobile-bottom-tabbar {
    backdrop-filter: blur(18px);
    -webkit-backdrop-filter: blur(18px);
  }
}

.tab-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  color: #94a3b8;
  border-radius: 20rpx;
  transition: background-color 0.1s;
}

.tab-item:active {
  background-color: rgba(0, 0, 0, 0.05);
}

.tab-badge:active {
  opacity: 0.7;
  transform: scale(0.98);
  transition: all 0.1s;
}

.tab-item.active {
  color: #111827;
}

.tab-icon-wrap {
  position: relative;
}

.tab-icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: 20rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.05);
  font-size: 25rpx;
  transition: background 0.2s ease, color 0.2s ease;
}

.tab-item.active .tab-icon {
  background: rgba(7, 193, 96, 0.18);
  color: #07c160;
}

.tab-badge {
  position: absolute;
  top: -10rpx;
  right: -14rpx;
  min-width: 34rpx;
  height: 34rpx;
  padding: 0 10rpx;
  border-radius: 999rpx;
  background: #fa5151;
  color: #ffffff;
  font-size: 18rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 16rpx rgba(250, 81, 81, 0.24);
}

.tab-label {
  font-size: 22rpx;
}

.tab-indicator {
  width: 34rpx;
  height: 6rpx;
  border-radius: 999rpx;
  background: transparent;
}

.tab-item.active .tab-indicator {
  background: #07c160;
}
</style>
