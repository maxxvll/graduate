<template>
  <view class="edit-profile-mask" v-if="visible" @click.self="handleClose">
    <view class="edit-profile-modal" @click.stop>
      <view class="modal-header">
        <text class="modal-title">编辑个人信息</text>
        <text class="close-btn" @click="handleClose">×</text>
      </view>
      
      <view class="modal-body">
        <!-- 1. 头像 (点击即上传) -->
        <view class="form-item">
          <text class="form-label">头像</text>
          <view class="avatar-upload" @click="chooseAvatar">
            <image :src="formData.avatar || defaultAvatar" class="edit-avatar" mode="aspectFill"></image>
            <view class="avatar-overlay">
              <text v-if="isUploadingAvatar" class="loading-text">上传中</text>
              <text v-else class="camera-icon">📷</text>
            </view>
          </view>
        </view>

        <!-- 2. 昵称 -->
        <view class="form-item">
          <text class="form-label">昵称</text>
          <input v-model="formData.nickname" class="form-input" placeholder="请输入昵称" maxlength="20" />
        </view>

        <!-- 3. 用户名 (只读) -->
        <view class="form-item">
          <text class="form-label">用户名</text>
          <input v-model="formData.username" class="form-input" disabled style="background:#f5f5f5;color:#999;" />
        </view>

        <!-- 4. 个性签名 (存入 extInfo) -->
        <view class="form-item">
          <text class="form-label">个性签名</text>
          <textarea v-model="formData.signature" class="form-textarea" placeholder="这个人很懒..." maxlength="100" />
        </view>

        <!-- 5. 地区 (存入 extInfo) -->
        <view class="form-item">
          <text class="form-label">地区</text>
          <input v-model="formData.region" class="form-input" placeholder="请输入地区" />
        </view>

        <!-- 6. 手机号 -->
        <view class="form-item">
          <text class="form-label">手机号</text>
          <input v-model="formData.phone" class="form-input" placeholder="请输入手机号" type="number" maxlength="11" />
        </view>

        <!-- 7. 邮箱 -->
        <view class="form-item">
          <text class="form-label">邮箱</text>
          <input v-model="formData.email" class="form-input" placeholder="请输入邮箱" type="email" />
        </view>
      </view>

      <view class="modal-footer">
        <button class="cancel-btn" @click="handleClose">取消</button>
        <button class="save-btn" @click="saveProfile" :disabled="isSaving">
          {{ isSaving ? '保存中...' : '保存' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'
import service from '@/utils/request'

const props = defineProps({
  visible: { type: Boolean, default: false },
  userInfo: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['close', 'update:visible', 'update:userInfo'])

const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iMjAiIGN5PSIyMCIgcj0iMjAiIGZpbGw9IiNjY2NjY2MiLz48L3N2Zz4='
const isSaving = ref(false)
const isUploadingAvatar = ref(false)

// 表单数据：新增shortAvatarPath存储短路径
const formData = ref({
  id: '',
  username: '',
  nickname: '',
  avatar: '', // 预览URL（长，用于显示）
  shortAvatarPath: '', // 短路径（用于提交更新）
  signature: '', 
  region: '',    
  phone: '',
  email: ''
})

// 监听打开弹窗
watch(() => props.visible, (newVal) => {
  if (newVal && props.userInfo) {
    formData.value = {
      id: props.userInfo.id || '',
      username: props.userInfo.username || '',
      nickname: props.userInfo.nickname || '',
      avatar: props.userInfo.avatar || defaultAvatar, // 初始显示预览URL
      shortAvatarPath: props.userInfo.avatar || '', // 初始短路径（从后端info接口返回的是URL，后端会兜底处理）
      signature: props.userInfo.signature || '',
      region: props.userInfo.region || '',
      phone: props.userInfo.phone || '',
      email: props.userInfo.email || ''
    }
  }
}, { immediate: true, deep: true })

const handleClose = () => {
  emit('update:visible', false)
  emit('close')
}

// 选择头像（核心修复：存储短路径+预览URL）
const chooseAvatar = () => {
  // #ifdef H5
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (!file) return

    isUploadingAvatar.value = true
    try {
      const uploadFormData = new FormData()
      uploadFormData.append('file', file)

      // 调用头像上传接口
      const res = await service.post('/user/avatar/upload', uploadFormData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })

      if (res && res.code === 200) {
        // 存储预览URL（显示用）和短路径（提交用）
        formData.value.avatar = res.data.previewUrl
        formData.value.shortAvatarPath = res.data.filePath
        uni.showToast({ title: '头像上传成功', icon: 'success' })
      } else {
        uni.showToast({ title: res?.message || '头像上传失败', icon: 'none' })
      }
    } catch (err) {
      console.error('头像上传异常', err)
      uni.showToast({ title: `上传失败：${err.message}`, icon: 'none' })
    } finally {
      isUploadingAvatar.value = false
    }
  }
  input.click()
  // #endif
}

// 保存个人信息（核心修复：提交短路径）
const saveProfile = async () => {
  if (!formData.value.nickname.trim()) {
    uni.showToast({ title: '昵称不能为空', icon: 'none' })
    return
  }

  isSaving.value = true
  try {
    // 构建提交数据：avatar传短路径
    const submitData = {
      nickname: formData.value.nickname,
      avatar: formData.value.shortAvatarPath || formData.value.avatar, // 兜底：如果没上传新头像，传原有值
      phone: formData.value.phone,
      email: formData.value.email,
      extInfo: {
        signature: formData.value.signature,
        region: formData.value.region
      }
    }

    // 调用更新接口
    const res = await service.post('/user/update', submitData)
    
    if (res.code === 200) {
      uni.showToast({ title: '保存成功', icon: 'success' })
      // 更新父组件用户信息（显示用的还是预览URL）
      emit('update:userInfo', { 
        ...formData.value,
        avatar: formData.value.avatar // 给父组件的是预览URL
      })
      handleClose()
    } else {
      uni.showToast({ title: res.message || '保存失败', icon: 'none' })
    }
  } catch (e) {
    console.error('保存失败', e)
    uni.showToast({ title: `保存失败：${e.message || '网络异常'}`, icon: 'none' })
  } finally {
    isSaving.value = false
  }
}
</script>

<style scoped>
.edit-profile-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 9999; }
.edit-profile-modal { width: 480px; max-height: 85vh; background: #fff; border-radius: 8px; display: flex; flex-direction: column; overflow: hidden; }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-bottom: 1px solid #e6e6e6; }
.modal-title { font-size: 16px; font-weight: 500; color: #333; }
.close-btn { font-size: 24px; color: #999; cursor: pointer; line-height: 1; }
.modal-body { flex: 1; padding: 20px; overflow-y: auto; }
.form-item { display: flex; align-items: flex-start; margin-bottom: 18px; }
.form-label { width: 70px; font-size: 14px; color: #333; padding-top: 8px; flex-shrink: 0; }
.form-input { flex: 1; height: 36px; border: 1px solid #e6e6e6; border-radius: 4px; padding: 0 12px; font-size: 14px; outline: none; }
.form-textarea { flex: 1; min-height: 60px; border: 1px solid #e6e6e6; border-radius: 4px; padding: 8px 12px; font-size: 14px; outline: none; resize: none; line-height: 1.5; }
.avatar-upload { position: relative; width: 70px; height: 70px; cursor: pointer; }
.edit-avatar { width: 70px; height: 70px; border-radius: 6px; background: #f5f5f5; }
.avatar-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.3); border-radius: 6px; display: flex; align-items: center; justify-content: center; transition: opacity 0.2s; }
.avatar-upload:hover .avatar-overlay { opacity: 1; }
.camera-icon, .loading-text { font-size: 22px; color: #fff; }
.loading-text { font-size: 12px; }
.modal-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 16px 20px; border-top: 1px solid #e6e6e6; }
.cancel-btn, .save-btn { padding: 8px 28px; border-radius: 4px; font-size: 14px; cursor: pointer; border: none; }
.cancel-btn { background: #f5f5f5; color: #333; }
.save-btn { background: #07c160; color: #fff; }
.save-btn:disabled { background: #cccccc; cursor: not-allowed; }
</style>