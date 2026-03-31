<template>
  <view class="profile-page">
    <view class="profile-header">
      <view class="header-left" @click="goBack">
        <text class="back-icon">‹</text>
        <text class="back-text">返回</text>
      </view>
      <text class="header-title">编辑资料</text>
      <text class="header-action" @click="submitProfile">
        {{ saving ? '保存中' : '保存' }}
      </text>
    </view>

    <scroll-view class="profile-scroll" scroll-y>
      <view class="profile-card avatar-card">
        <text class="card-label">头像</text>
        <view class="avatar-area" @click="chooseAvatar">
          <image class="avatar-image" :src="form.avatar || defaultAvatar" mode="aspectFill" />
          <view class="avatar-tip">
            <text class="avatar-tip-text">{{ uploadingAvatar ? '上传中...' : '更换头像' }}</text>
          </view>
        </view>
      </view>

      <view class="profile-card">
        <view class="field-row">
          <text class="field-label">昵称</text>
          <input v-model="form.nickname" class="field-input" maxlength="20" placeholder="请输入昵称" />
        </view>
        <view class="field-row">
          <text class="field-label">用户名</text>
          <input :value="form.username" class="field-input is-disabled" disabled />
        </view>
        <view class="field-row">
          <text class="field-label">手机号</text>
          <input v-model="form.phone" class="field-input" type="number" maxlength="11" placeholder="请输入手机号" />
        </view>
        <view class="field-row">
          <text class="field-label">邮箱</text>
          <input v-model="form.email" class="field-input" type="text" placeholder="请输入邮箱" />
        </view>
        <view class="field-row is-textarea">
          <text class="field-label">签名</text>
          <textarea
            v-model="form.signature"
            class="field-textarea"
            maxlength="100"
            placeholder="介绍一下你自己"
          />
        </view>
        <view class="field-row">
          <text class="field-label">地区</text>
          <input v-model="form.region" class="field-input" maxlength="32" placeholder="请输入地区" />
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import service from '@/utils/request'
import { APP_PERMISSION_SCOPE, ensureAnyAppPermissionAccess } from '@/utils/app-permission'
import { uploadFile } from '@/utils/file-upload'
import { isAppPlusRuntime, supportsBrowserDom } from '@/utils/runtime'
import LocalStateCache from '@/utils/local-state-cache'
import { DEFAULT_AVATAR_LARGE as defaultAvatar } from '@/utils/common'

const saving = ref(false)
const uploadingAvatar = ref(false)
const EDIT_PROFILE_CACHE_KEY = 'edit_profile_draft'

const form = reactive({
  username: '',
  nickname: '',
  avatar: '',
  avatarPath: '',
  phone: '',
  email: '',
  signature: '',
  region: '',
})

let profileHydrating = false
let profileFormReady = false
let restoredDirtyDraft = false

const resolveProfileCacheScope = () => {
  try {
    const stored = uni.getStorageSync('userInfo') || {}
    const storedId = String(stored?.id || '')
    if (storedId) {
      return `edit-profile:${storedId}`
    }
  } catch {}

  return 'edit-profile:anonymous'
}

const normalizeProfileDraftForm = (value = {}) => ({
  username: String(value?.username || ''),
  nickname: String(value?.nickname || ''),
  avatar: String(value?.avatar || ''),
  avatarPath: String(value?.avatarPath || value?.avatar || ''),
  phone: String(value?.phone || ''),
  email: String(value?.email || ''),
  signature: String(value?.signature || value?.extInfo?.signature || ''),
  region: String(value?.region || value?.extInfo?.region || ''),
})

const readProfileDraft = () => {
  const cached = LocalStateCache.getValue(resolveProfileCacheScope(), EDIT_PROFILE_CACHE_KEY)
  if (!cached || typeof cached !== 'object') {
    return null
  }

  return {
    dirty: cached?.dirty !== false,
    form: normalizeProfileDraftForm(cached?.form || cached),
  }
}

const writeProfileDraft = (dirty = true) => {
  if (!profileFormReady) {
    return
  }

  LocalStateCache.set(resolveProfileCacheScope(), EDIT_PROFILE_CACHE_KEY, {
    dirty,
    form: normalizeProfileDraftForm(form),
  })
}

const hydrateForm = (userInfo = {}) => {
  form.username = userInfo.username || ''
  form.nickname = userInfo.nickname || ''
  form.avatar = userInfo.avatar || ''
  form.avatarPath = userInfo.avatar || ''
  form.phone = userInfo.phone || ''
  form.email = userInfo.email || ''
  form.signature = userInfo.signature || userInfo.extInfo?.signature || ''
  form.region = userInfo.region || userInfo.extInfo?.region || ''
}

const applyProfileSnapshot = (value = {}) => {
  profileHydrating = true
  hydrateForm(value)
  profileHydrating = false
}

const loadProfile = async ({ allowOverwrite = true } = {}) => {
  const response = await service.get('/user/info')
  if (response.code === 200) {
    const nextProfile = response.data || {}
    if (allowOverwrite) {
      applyProfileSnapshot(nextProfile)
      LocalStateCache.set(resolveProfileCacheScope(), EDIT_PROFILE_CACHE_KEY, {
        dirty: false,
        form: normalizeProfileDraftForm(nextProfile),
      })
    }
  }
}

const uploadAvatar = async (file) => {
  if (!file) {
    return
  }

  uploadingAvatar.value = true

  try {
    const fileSource = file.uploadSource || file
    const response = await uploadFile('/user/avatar/upload', fileSource, {
      showProgress: false,
    })

    if (response.code === 200) {
      form.avatar = response.data?.previewUrl || form.avatar
      form.avatarPath = response.data?.filePath || form.avatarPath
      writeProfileDraft(true)
      uni.showToast({
        title: '头像已更新',
        icon: 'none',
      })
    }
  } finally {
    uploadingAvatar.value = false
  }
}

const chooseAvatarFromWeb = () =>
  new Promise((resolve) => {
    if (!supportsBrowserDom()) {
      resolve(null)
      return
    }

    const input = document.createElement('input')
    input.type = 'file'
    input.accept = 'image/*'
    input.addEventListener('change', (event) => {
      resolve(event.target?.files?.[0] || null)
      input.remove()
    })
    input.click()
  })

const chooseAvatarFromDevice = () =>
  new Promise((resolve) => {
    uni.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      success: (result) => {
        const tempFile = result.tempFiles?.[0]
        const path = tempFile?.path || result.tempFilePaths?.[0]
        resolve(
          path
            ? {
                uploadSource: path,
                name: tempFile?.name || path.split('/').pop() || `avatar_${Date.now()}.jpg`,
              }
            : null,
        )
      },
      fail: () => resolve(null),
    })
  })

const chooseAvatar = async () => {
  if (isAppPlusRuntime()) {
    const permissionResult = await ensureAnyAppPermissionAccess(
      [APP_PERMISSION_SCOPE.CAMERA, APP_PERMISSION_SCOPE.ALBUM],
      {
        title: '需要相机或相册权限',
        content: '更换头像前，请至少开启相机或相册中的一个权限。',
      },
    )
    if (!permissionResult.ok) {
      return
    }
  }

  const file = supportsBrowserDom() ? await chooseAvatarFromWeb() : await chooseAvatarFromDevice()

  if (!file) {
    uni.showToast({
      title: '当前环境暂不支持直接上传',
      icon: 'none',
    })
    return
  }

  await uploadAvatar(file)
}

const submitProfile = async () => {
  if (!form.nickname.trim()) {
    uni.showToast({
      title: '昵称不能为空',
      icon: 'none',
    })
    return
  }

  saving.value = true

  try {
    const response = await service.post('/user/update', {
      nickname: form.nickname.trim(),
      avatar: form.avatarPath || form.avatar,
      phone: form.phone.trim(),
      email: form.email.trim(),
      extInfo: {
        signature: form.signature.trim(),
        region: form.region.trim(),
      },
    })

    if (response.code === 200) {
      const nextUser = {
        ...uni.getStorageSync('userInfo'),
        username: form.username,
        nickname: form.nickname,
        avatar: form.avatar,
        phone: form.phone,
        email: form.email,
        signature: form.signature,
        region: form.region,
      }

      uni.setStorageSync('userInfo', nextUser)
      LocalStateCache.set(resolveProfileCacheScope(), EDIT_PROFILE_CACHE_KEY, {
        dirty: false,
        form: normalizeProfileDraftForm(nextUser),
      })
      uni.$emit('profileUpdated', nextUser)
      uni.showToast({
        title: '资料已保存',
        icon: 'none',
      })

      setTimeout(() => {
        goBack()
      }, 240)
    }
  } finally {
    saving.value = false
  }
}

const goBack = () => {
  const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : []
  if (pages.length > 1) {
    uni.navigateBack()
  } else {
    uni.reLaunch({
      url: '/pages/home/home',
    })
  }
}

onMounted(() => {
  try {
    const storedUser = uni.getStorageSync('userInfo') || {}
    if (storedUser && typeof storedUser === 'object') {
      applyProfileSnapshot(storedUser)
    }
  } catch {}

  const cachedDraft = readProfileDraft()
  if (cachedDraft?.form) {
    applyProfileSnapshot(cachedDraft.form)
    restoredDirtyDraft = Boolean(cachedDraft.dirty)
  }

  profileFormReady = true
  loadProfile({ allowOverwrite: !restoredDirtyDraft })
})

watch(
  form,
  () => {
    if (profileHydrating || !profileFormReady) {
      return
    }

    restoredDirtyDraft = true
    writeProfileDraft(true)
  },
  { deep: true },
)
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #ecefef 0%, #f6f7f7 22%, #f8f8f8 100%);
  color: #1f2329;
}

.profile-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: 96rpx 1fr 120rpx;
  align-items: center;
  height: calc(96rpx + env(safe-area-inset-top, 0px));
  padding: env(safe-area-inset-top, 0px) 28rpx 0;
  background: rgba(247, 248, 248, 0.94);
  backdrop-filter: blur(16px);
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.back-icon {
  font-size: 40rpx;
  line-height: 1;
}

.back-text,
.header-action {
  font-size: 28rpx;
  color: #07c160;
}

.header-title {
  text-align: center;
  font-size: 32rpx;
  font-weight: 600;
  color: #111827;
}

.profile-scroll {
  height: calc(100vh - 96rpx - env(safe-area-inset-top, 0px));
  padding: 28rpx;
}

.profile-card {
  margin-bottom: 24rpx;
  padding: 28rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18rpx 44rpx rgba(15, 23, 42, 0.08);
}

.avatar-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-label,
.field-label {
  font-size: 28rpx;
  color: #4b5563;
}

.avatar-area {
  position: relative;
  width: 152rpx;
  height: 152rpx;
  border-radius: 40rpx;
  overflow: hidden;
}

.avatar-image {
  width: 100%;
  height: 100%;
}

.avatar-tip {
  position: absolute;
  inset: auto 0 0 0;
  padding: 10rpx 0;
  background: rgba(17, 24, 39, 0.55);
  text-align: center;
}

.avatar-tip-text {
  font-size: 22rpx;
  color: #fff;
}

.field-row {
  display: grid;
  grid-template-columns: 120rpx 1fr;
  align-items: center;
  min-height: 96rpx;
  gap: 16rpx;
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
}

.field-row:last-child {
  border-bottom: none;
}

.field-row.is-textarea {
  align-items: flex-start;
  padding: 24rpx 0;
}

.field-input,
.field-textarea {
  width: 100%;
  font-size: 28rpx;
  color: #111827;
  background: transparent;
}

.field-input.is-disabled {
  color: #9ca3af;
}

.field-textarea {
  min-height: 180rpx;
  line-height: 1.6;
}
</style>
