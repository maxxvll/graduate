<template>
  <view class="password-login-form">
    <u-form :model="formData" :rules="formRules" ref="formRef" class="modern-form">
      <!-- Account Input -->
      <FormInput
        :modelValue="formData.account"
        @update:modelValue="handleInput('account', $event)"
        icon="user"
        placeholder="输入账号"
        :error="accountError"
      />

      <!-- Password Input -->
      <FormInput
        :modelValue="formData.password"
        @update:modelValue="handleInput('password', $event)"
        icon="lock"
        type="password"
        placeholder="输入密码"
        :show-password="true"
        :error="passwordError"
      />

      <!-- Save Password (Mobile Only) -->
      <!-- #ifndef H5 -->
      <view class="save-password-wrapper">
        <FormCheckbox
          v-model="savePassword"
          label="记住登录状态"
        />
      </view>
      <!-- #endif -->

      <!-- Login Button -->
      <view class="button-group">
        <PrimaryButton
          :text="isLoading ? '连接中...' : '登录'"
          :loading="isLoading"
          @click="handleSubmit"
        />
      </view>
    </u-form>
  </view>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'
import FormInput from '@/components/common/FormInput.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import FormCheckbox from '@/components/common/FormCheckbox.vue'

const props = defineProps({
  modelValue: {
    type: Object,
    required: true
  },
  isLoading: {
    type: Boolean,
    default: false
  },
  savePassword: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'update:savePassword', 'submit'])

// Handle both ref objects and plain objects from parent - use a reactive wrapper
const formData = reactive({})

// Initialize formData from props
const initFormData = () => {
  const val = props.modelValue?.value ?? props.modelValue
  if (val && typeof val === 'object') {
    formData.account = val.account ?? ''
    formData.password = val.password ?? ''
  }
}

// Watch for changes in parent and update formData
watch(() => props.modelValue, initFormData, { immediate: true, deep: true })

// Save password (handle ref or plain)
const savePassword = computed({
  get: () => {
    const val = props.savePassword?.value ?? props.savePassword
    return val ?? false
  },
  set: (newVal) => {
    emit('update:savePassword', newVal)
  }
})

const formRef = ref(null)
const accountError = ref('')
const passwordError = ref('')

// Form validation rules
const formRules = reactive({
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度为3-20位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 16, message: '密码长度为6-16位', trigger: 'blur' }
  ]
})

// Handle input changes - emit to parent
const handleInput = (field, value) => {
  // Get current formData value
  const currentVal = props.modelValue?.value ?? props.modelValue
  const newFormData = { ...currentVal, [field]: value }
  emit('update:modelValue', newFormData)
}

const handleSavePasswordChange = (value) => {
  emit('update:savePassword', value)
}

const handleSubmit = async () => {
  // Clear previous errors
  accountError.value = ''
  passwordError.value = ''

  // Validate
  if (!formData.account) {
    accountError.value = '请输入账号'
    return
  }
  if (formData.account.length < 3 || formData.account.length > 20) {
    accountError.value = '账号长度为3-20位'
    return
  }
  if (!formData.password) {
    passwordError.value = '请输入密码'
    return
  }
  if (formData.password.length < 6 || formData.password.length > 16) {
    passwordError.value = '密码长度为6-16位'
    return
  }

  emit('submit')
}
</script>

<style scoped>
.password-login-form {
  width: 100%;
}

.modern-form {
  width: 100%;
}

.save-password-wrapper {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  padding-left: 4px;
}

.button-group {
  margin-top: 8px;
}
</style>
