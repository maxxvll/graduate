<template>
  <view
    class="form-checkbox"
    :class="{
      'form-checkbox--checked': modelValue,
      'form-checkbox--disabled': disabled
    }"
    @click="handleClick"
  >
    <view class="form-checkbox__box">
      <view v-if="modelValue" class="form-checkbox__check">
        <text class="check-icon">✓</text>
      </view>
    </view>

    <text v-if="label" class="form-checkbox__label">{{ label }}</text>
  </view>
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  label: {
    type: String,
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

const handleClick = () => {
  if (!props.disabled) {
    emit('update:modelValue', !props.modelValue)
  }
}
</script>

<style scoped>
.form-checkbox {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  user-select: none;
}

.form-checkbox__box {
  position: relative;
  width: 20px;
  height: 20px;
  border: 2px solid var(--border-subtle);
  border-radius: 6px;
  background: var(--bg-secondary);
  transition: all 0.3s;
  flex-shrink: 0;
}

.form-checkbox--checked .form-checkbox__box {
  background: var(--green-primary);
  border-color: var(--green-primary);
}

.form-checkbox--disabled .form-checkbox__box {
  opacity: 0.5;
  cursor: not-allowed;
}

.form-checkbox__check {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.check-icon {
  color: #FFFFFF;
  font-size: 14px;
  font-weight: bold;
  line-height: 1;
}

.form-checkbox__label {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.form-checkbox--disabled .form-checkbox__label {
  opacity: 0.5;
}
</style>
