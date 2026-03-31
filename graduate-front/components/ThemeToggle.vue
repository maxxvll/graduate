<template>
  <view
    class="theme-toggle"
    :class="{ 'theme-toggle--dark': isDark }"
    @click="handleToggle"
  >
    <view class="theme-toggle__icon">
      <!-- 浅色模式：显示太阳 -->
      <text v-if="theme === 'light'" class="icon">☀️</text>
      <!-- 深色模式：显示月亮 -->
      <text v-else-if="theme === 'dark'" class="icon">🌙</text>
      <!-- 自动模式：显示自动图标 -->
      <text v-else class="icon">🌓</text>
    </view>

    <!-- 显示当前模式文字（可选） -->
    <text v-if="showLabel" class="theme-toggle__label">{{ themeLabel }}</text>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useTheme } from '@/composables/useTheme'

const props = defineProps({
  showLabel: {
    type: Boolean,
    default: false
  }
})

const { theme, isDark, toggleTheme } = useTheme()

const themeLabel = computed(() => {
  const labels = { light: '浅色', dark: '深色', auto: '自动' }
  return labels[theme.value] || ''
})

const handleToggle = () => {
  toggleTheme()
}
</script>

<style scoped>
.theme-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: var(--bg-tertiary, #F5F5F4);
  border: 1px solid var(--border-subtle, #E7E5E4);
}

.theme-toggle:active {
  transform: scale(0.95);
}

.theme-toggle__icon {
  font-size: 20px;
  line-height: 1;
  transition: transform 0.3s;
}

.theme-toggle:hover .theme-toggle__icon {
  transform: rotate(20deg);
}

.theme-toggle__label {
  font-size: 13px;
  color: var(--text-secondary, #57534E);
  font-weight: 500;
}

/* 深色模式下的样式调整 */
.theme-toggle--dark {
  background: var(--bg-secondary, #1A1A1A);
  border-color: var(--border-medium, #3A3A3A);
}

.theme-toggle--dark .theme-toggle__label {
  color: var(--text-secondary, #D6D3D1);
}
</style>
