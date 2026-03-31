<template>
  <view class="form-input" :class="{ 'form-input--error': error, 'form-input--focused': isFocused }">
    <!-- 左侧图标 -->
    <view v-if="icon" class="form-input__icon">
      <!-- #ifdef H5 || APP-PLUS -->
      <svg class="icon-svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <use :xlink:href="`#icon-${icon}`"></use>
      </svg>
      <!-- #endif -->
      <!-- #ifdef MP-WEIXIN || MP-ALIPAY || MP-BAIDU || MP-TOUTIAO -->
      <text class="icon-emoji">{{ emojiMap[icon] }}</text>
      <!-- #endif -->
    </view>

    <!-- 输入框 -->
    <u-input
      :modelValue="modelValue"
      @update:modelValue="$emit('update:modelValue', $event)"
      @focus="handleFocus"
      @blur="handleBlur"
      :type="type"
      :placeholder="placeholder"
      :password="password"
      :show-password="showPassword"
      :disabled="disabled"
      bgColor="transparent"
      :custom-style="inputStyle"
      :placeholderStyle="placeholderStyle"
      class="form-input__field"
    />

    <!-- 底部焦点线 -->
    <view class="form-input__line"></view>

    <!-- 错误提示 -->
    <text v-if="error" class="form-input__error">{{ error }}</text>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  type: { type: String, default: 'text' },
  placeholder: { type: String, default: '' },
  icon: { type: String, default: '' },
  password: { type: Boolean, default: false },
  showPassword: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  error: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'focus', 'blur'])

const isFocused = ref(false)

const inputStyle = computed(() => ({
  height: '56px',
  padding: props.icon ? '0 20px 0 52px' : '0 20px',
  fontSize: '16px',
  color: 'var(--text-primary)',
  fontWeight: '500',
  backgroundColor: 'transparent'
}))

const placeholderStyle = computed(() => ({
  color: 'var(--text-placeholder)'
}))

const emojiMap = {
  user: '👤',
  lock: '🔒',
  email: '📧',
  phone: '📱',
  search: '🔍',
  edit: '✏️'
}

const handleFocus = () => {
  isFocused.value = true
  emit('focus')
}

const handleBlur = () => {
  isFocused.value = false
  emit('blur')
}
</script>

<style scoped>
.form-input {
  position: relative;
  margin-bottom: 24px;
}

.form-input__icon {
  position: absolute;
  left: 0;
  top: 0;
  width: 52px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
  color: var(--text-tertiary);
  transition: color 0.3s;
}

.form-input--focused .form-input__icon {
  color: var(--green-primary);
}

.icon-svg {
  width: 20px;
  height: 20px;
  stroke: currentColor;
}

.icon-emoji {
  font-size: 20px;
  line-height: 1;
}

.form-input__field {
  width: 100%;
  height: 56px;
  padding: 0 20px 0 52px;
  background: var(--bg-secondary);
  border: 1.5px solid var(--border-subtle);
  border-radius: 16px;
  transition: all 0.3s;
  color: var(--text-primary);
}

.form-input--focused .form-input__field {
  border-color: var(--green-primary);
  box-shadow: 0 0 0 4px var(--green-faint);
}

.form-input--error .form-input__field {
  border-color: var(--accent-red);
}

.form-input__line {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  height: 2px;
  width: 0;
  background: var(--gradient-green);
  transition: width 0.3s;
  border-radius: 0 0 16px 16px;
}

.form-input--focused .form-input__line {
  width: calc(100% - 32px);
}

.form-input--error .form-input__line {
  background: var(--accent-red);
}

.form-input__error {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: var(--accent-red);
  padding-left: 12px;
  font-weight: 500;
}
</style>
