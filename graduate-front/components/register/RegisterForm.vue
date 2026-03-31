<template>
  <view class="register-form">
    <u-form :model="formData" :rules="formRules" ref="formRef" class="modern-form">
      <!-- 账号 -->
      <FormInput
        v-model="formData.username"
        icon="user"
        placeholder="请输入账号"
        :error="errors.username"
      />

      <!-- 昵称 -->
      <FormInput
        v-model="formData.nickname"
        icon="edit"
        placeholder="请输入昵称"
        :error="errors.nickname"
      />

      <!-- 手机号 -->
      <FormInput
        v-model="formData.phone"
        icon="phone"
        type="tel"
        placeholder="请输入手机号"
        :error="errors.phone"
      />

      <!-- 邮箱 + 验证码按钮 -->
      <view class="form-row-email">
        <view class="email-input-wrapper">
          <FormInput
            v-model="formData.email"
            icon="email"
            type="email"
            placeholder="请输入邮箱"
            :error="errors.email"
          />
        </view>
        <SecondaryButton
          :text="countdown > 0 ? `${countdown}s后重发` : '发送验证码'"
          :disabled="isSendingCode || countdown > 0"
          :loading="isSendingCode"
          @click="$emit('send-code')"
          class="code-button"
        />
      </view>

      <!-- 验证码 -->
      <FormInput
        v-model="formData.code"
        icon="search"
        placeholder="请输入验证码"
        :error="errors.code"
      />

      <!-- 密码 -->
      <FormInput
        v-model="formData.password"
        icon="lock"
        type="password"
        placeholder="请输入密码"
        :show-password="true"
        :error="errors.password"
      />

      <!-- 确认密码 -->
      <FormInput
        v-model="formData.confirmPassword"
        icon="lock"
        type="password"
        placeholder="请再次输入密码"
        :show-password="true"
        :error="errors.confirmPassword"
      />

      <!-- 提交按钮 -->
      <PrimaryButton
        :text="isLoading ? '注册中...' : '注册'"
        :loading="isLoading"
        @click="$emit('submit')"
        class="submit-button"
      />
    </u-form>
  </view>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import FormInput from '@/components/common/FormInput.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import SecondaryButton from '@/components/common/SecondaryButton.vue'

const props = defineProps({
  modelValue: {
    type: Object,
    required: true
  },
  isLoading: {
    type: Boolean,
    default: false
  },
  isSendingCode: {
    type: Boolean,
    default: false
  },
  countdown: {
    type: Number,
    default: 0
  },
  formRules: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits([
  'update:modelValue',
  'submit',
  'send-code',
  'update:formRef'
])

const formData = ref({ ...props.modelValue })
const errors = ref({})
const formRef = ref(null)

// Watch for changes in parent
watch(() => props.modelValue, (newVal) => {
  formData.value = { ...newVal }
}, { deep: true })

// Emit changes
watch(formData, (newVal) => {
  emit('update:modelValue', newVal)
}, { deep: true })

// Expose formRef to parent
watch(formRef, (newVal) => {
  if (newVal) {
    emit('update:formRef', newVal)
  }
})
</script>

<style scoped>
.register-form {
  width: 100%;
}

.modern-form {
  width: 100%;
}

.form-row-email {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 24px;
}

.email-input-wrapper {
  flex: 1;
}

.code-button {
  flex-shrink: 0;
  width: 120px;
  height: 56px;
  margin-top: 0;
}

.submit-button {
  margin-top: 8px;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .form-row-email {
    flex-direction: column;
    gap: 16px;
  }

  .code-button {
    width: 100%;
  }
}
</style>
