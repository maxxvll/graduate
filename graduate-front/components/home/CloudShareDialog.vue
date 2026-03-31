<template>
  <view v-if="visible" class="share-layer">
    <view class="share-backdrop" @click="handleClose"></view>
    <view class="share-card" :class="{ mobile }">
      <view class="share-header">
        <text class="share-title">分享文件</text>
        <view class="share-file-info">
          <view class="file-mark" :class="`type-${fileIconType}`">
            <view class="file-mark-text">{{ fileIconText }}</view>
          </view>
          <view class="file-name-row">
            <text class="file-name">{{ file?.name || '未命名文件' }}</text>
            <text class="file-size">{{ formatSize(file?.size) }}</text>
          </view>
        </view>
        <view class="close-btn" @click="handleClose"></view>
      </view>

      <view class="share-body">
        <view class="section-label">创建分享链接</view>

        <view class="form-row">
          <text class="form-label">有效期</text>
          <picker
            class="form-picker"
            mode="selector"
            :range="expireOptions"
            :value="expireIndex"
            @change="onExpireChange"
          >
            <view class="picker-value">{{ expireOptions[expireIndex] }}</view>
          </picker>
        </view>

        <view class="form-row">
          <text class="form-label">提取码</text>
          <input
            v-model="sharePassword"
            class="form-input"
            placeholder="可选，不设置则无需提取码"
            :maxlength="12"
          />
        </view>

        <view class="form-actions">
          <view
            class="action-btn primary"
            :class="{ disabled: creating }"
            @click="handleCreateShare"
          >
            {{ creating ? '创建中...' : '创建分享链接' }}
          </view>
        </view>

        <view v-if="createdShare" class="share-result">
          <view class="result-row">
            <text class="result-label">分享链接</text>
            <view class="result-value-box">
              <text class="result-value full">{{ shareUrl }}</text>
            </view>
          </view>
          <view v-if="createdShare.shareCode" class="result-row">
            <text class="result-label">提取码</text>
            <view class="result-value-box">
              <text class="result-value code">{{ createdShare.shareCode }}</text>
              <view class="copy-btn" @click="copyCode">复制</view>
            </view>
          </view>
          <view class="result-row">
            <text class="result-label">链接复制</text>
            <view class="result-value-box">
              <view class="copy-btn primary" @click="copyUrl">复制链接</view>
            </view>
          </view>
        </view>

        <view v-if="errorMsg" class="error-tip">{{ errorMsg }}</view>

        <view class="section-label" style="margin-top: 32rpx">我的分享记录</view>

        <view v-if="loadingShares" class="shares-loading">
          <text class="shares-loading-text">加载中...</text>
        </view>

        <view v-else-if="!shareList.length" class="shares-empty">
          <text class="shares-empty-text">暂无分享记录</text>
        </view>

        <scroll-view v-else class="shares-list" scroll-y>
          <view
            v-for="item in shareList"
            :key="item.id"
            class="share-item"
          >
            <view class="share-item-info">
              <view class="share-item-row">
                <text class="share-item-code">{{ item.shareCode || '无' }}</text>
                <text v-if="item.password" class="share-item-tag locked">加密</text>
                <text v-else class="share-item-tag open">公开</text>
              </view>
              <text class="share-item-meta">
                {{ item.downloadCount || 0 }} 次下载 · {{ item.expireTime ? formatTime(item.expireTime) : '永久有效' }}
              </text>
            </view>
            <view class="share-item-actions">
              <view class="share-action-btn" @click="copyShareItem(item)">复制</view>
              <view class="share-action-btn danger" @click="handleCancelShare(item)">取消</view>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import service from '@/utils/request'

const props = defineProps({
  visible: { type: Boolean, default: false },
  file: { type: Object, default: null },
  mobile: { type: Boolean, default: false },
})

const emit = defineEmits(['close', 'share-created', 'share-cancelled'])

const shareList = ref([])
const creating = ref(false)
const loadingShares = ref(false)
const errorMsg = ref('')
const createdShare = ref(null)

const sharePassword = ref('')
const expireIndex = ref(0)
const expireOptions = ['永久有效', '1天', '7天', '30天', '90天']
const expireDaysMap = [null, 1, 7, 30, 90]

const fileIconType = computed(() => props.file?.iconType || 'file')
const fileIconText = computed(() => props.file?.iconText || 'FILE')

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let value = Number(bytes)
  let idx = 0
  while (value >= 1024 && idx < units.length - 1) {
    value /= 1024
    idx++
  }
  const digits = idx === 0 ? 0 : value >= 10 ? 1 : 2
  return `${value.toFixed(digits)} ${units[idx]}`
}

const formatTime = (value) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  return `${date.getFullYear()}/${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`
}

const resolveShareUrl = (code) => {
  const base = window?.location?.origin || globalThis?.location?.origin || 'http://127.0.0.1:5100'
  return `${base}/#/pages/share/extract?code=${code}`
}

const shareUrl = computed(() => {
  if (!createdShare.value?.shareCode) return ''
  return resolveShareUrl(createdShare.value.shareCode)
})

const onExpireChange = (e) => {
  expireIndex.value = Number(e.detail.value || 0)
}

const loadShares = async () => {
  if (!props.file?.object) return
  loadingShares.value = true
  try {
    const res = await service.get('/cloud/share/list')
    // Filter shares that match the current file's object
    const allShares = res?.data || []
    shareList.value = allShares.filter((s) => s.fileId === props.file.object)
  } catch (err) {
    console.warn('[share-dialog] load shares failed', err)
  } finally {
    loadingShares.value = false
  }
}

const handleCreateShare = async () => {
  if (!props.file?.object || creating.value) return
  errorMsg.value = ''
  creating.value = true
  createdShare.value = null

  try {
    const res = await service.post('/cloud/share/create', {
      fileId: props.file.object,
      password: sharePassword.value.trim() || null,
      expireDays: expireDaysMap[expireIndex.value],
    })

    if (res?.data) {
      createdShare.value = res.data
      shareList.value.unshift(res.data)
      sharePassword.value = ''
      expireIndex.value = 0
      emit('share-created', res.data)
    }
  } catch (err) {
    errorMsg.value = err?.message || err?.msg || '创建分享失败'
  } finally {
    creating.value = false
  }
}

const copyToClipboard = (text, label = '已复制') => {
  if (typeof uni !== 'undefined' && uni.setClipboardData) {
    uni.setClipboardData({
      data: String(text || ''),
      success: () => {
        uni.showToast({ title: label, icon: 'none' })
      },
      fail: () => {
        uni.showToast({ title: '复制失败', icon: 'none' })
      },
    })
    return
  }
  if (typeof navigator !== 'undefined' && navigator.clipboard) {
    navigator.clipboard.writeText(String(text || '')).then(() => {
      uni?.showToast?.({ title: label, icon: 'none' })
    }).catch(() => {})
    return
  }
}

const copyUrl = () => {
  if (!shareUrl.value) return
  copyToClipboard(shareUrl.value, '链接已复制')
}

const copyCode = () => {
  if (!createdShare.value?.shareCode) return
  copyToClipboard(createdShare.value.shareCode, '提取码已复制')
}

const copyShareItem = (item) => {
  if (!item?.shareCode) return
  copyToClipboard(resolveShareUrl(item.shareCode), '链接已复制')
}

const handleCancelShare = async (item) => {
  if (!item?.id) return

  const confirmed = await new Promise((resolve) => {
    uni.showModal({
      title: '取消分享',
      content: '确定要取消此分享链接吗？',
      success: (result) => resolve(Boolean(result.confirm)),
      fail: () => resolve(false),
    })
  })

  if (!confirmed) return

  try {
    await service.post('/cloud/share/cancel', null, {
      params: { shareId: item.id },
    })
    shareList.value = shareList.value.filter((s) => s.id !== item.id)
    if (createdShare.value?.id === item.id) {
      createdShare.value = null
    }
    emit('share-cancelled', item)
    uni.showToast({ title: '已取消分享', icon: 'none' })
  } catch (err) {
    uni.showToast({ title: '取消失败', icon: 'none' })
  }
}

const handleClose = () => {
  emit('close')
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      createdShare.value = null
      errorMsg.value = ''
      sharePassword.value = ''
      expireIndex.value = 0
      loadShares()
    }
  },
)
</script>

<style scoped>
.share-layer {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28rpx;
}

.share-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.52);
  backdrop-filter: blur(16rpx);
}

.share-card {
  position: relative;
  width: min(680rpx, 100%);
  max-height: 85vh;
  border-radius: 28rpx;
  background: #ffffff;
  box-shadow: 0 32rpx 80rpx rgba(15, 23, 42, 0.2);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.share-card.mobile {
  width: 100%;
  border-radius: 36rpx 36rpx 0 0;
  max-height: 90vh;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  margin: 0;
}

.share-header {
  position: relative;
  padding: 28rpx 28rpx 22rpx;
  border-bottom: 1rpx solid #f0f3f7;
  flex-shrink: 0;
}

.share-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1c2d44;
}

.share-file-info {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-top: 16rpx;
}

.file-mark {
  width: 52rpx;
  height: 52rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14rpx;
  flex-shrink: 0;
}

.file-mark-text {
  font-size: 16rpx;
  font-weight: 700;
}

.file-name-row {
  flex: 1;
  min-width: 0;
}

.file-name {
  display: block;
  font-size: 24rpx;
  font-weight: 600;
  color: #23344c;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.file-size {
  font-size: 18rpx;
  color: #8394a8;
  margin-top: 4rpx;
  display: block;
}

.close-btn {
  position: absolute;
  top: 28rpx;
  right: 28rpx;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: #f4f6f9;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.close-btn::before,
.close-btn::after {
  content: '';
  position: absolute;
  width: 20rpx;
  height: 3rpx;
  background: #6b7280;
  border-radius: 2rpx;
}

.close-btn::before {
  transform: rotate(45deg);
}

.close-btn::after {
  transform: rotate(-45deg);
}

.share-body {
  padding: 24rpx 28rpx 28rpx;
  overflow-y: auto;
  flex: 1;
}

.section-label {
  font-size: 22rpx;
  font-weight: 700;
  color: #8a9ab0;
  margin-bottom: 18rpx;
  letter-spacing: 0.05em;
}

.form-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 18rpx;
}

.form-label {
  width: 96rpx;
  font-size: 22rpx;
  color: #4b5563;
  flex-shrink: 0;
}

.form-input {
  flex: 1;
  height: 64rpx;
  padding: 0 16rpx;
  border-radius: 12rpx;
  border: 1rpx solid #e5e9f0;
  background: #f8fafc;
  font-size: 22rpx;
  color: #1c2d44;
}

.form-picker {
  flex: 1;
}

.picker-value {
  height: 64rpx;
  padding: 0 16rpx;
  border-radius: 12rpx;
  border: 1rpx solid #e5e9f0;
  background: #f8fafc;
  font-size: 22rpx;
  color: #1c2d44;
  display: flex;
  align-items: center;
}

.form-actions {
  margin-top: 20rpx;
}

.action-btn {
  height: 72rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.18s ease;
}

.action-btn.primary {
  background: linear-gradient(135deg, #2f7df6 0%, #5a98ff 100%);
  color: #ffffff;
  box-shadow: 0 10rpx 20rpx rgba(47, 125, 246, 0.18);
}

.action-btn.disabled {
  opacity: 0.5;
  cursor: default;
}

.share-result {
  margin-top: 24rpx;
  padding: 20rpx;
  border-radius: 16rpx;
  background: linear-gradient(135deg, #f0f7ff 0%, #f5f9ff 100%);
  border: 1rpx solid #dde8ff;
}

.result-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-bottom: 14rpx;
}

.result-row:last-child {
  margin-bottom: 0;
}

.result-label {
  font-size: 20rpx;
  color: #698099;
  width: 88rpx;
  flex-shrink: 0;
}

.result-value-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10rpx;
  min-width: 0;
}

.result-value {
  flex: 1;
  min-width: 0;
  font-size: 20rpx;
  color: #1c2d44;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.result-value.code {
  font-weight: 700;
  color: #2f7df6;
  letter-spacing: 0.1em;
}

.result-value.full {
  font-size: 19rpx;
}

.copy-btn {
  height: 44rpx;
  padding: 0 16rpx;
  border-radius: 10rpx;
  background: #2f7df6;
  color: #ffffff;
  font-size: 18rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  cursor: pointer;
}

.copy-btn.primary {
  background: linear-gradient(135deg, #2f7df6 0%, #5a98ff 100%);
}

.error-tip {
  margin-top: 16rpx;
  padding: 12rpx 16rpx;
  border-radius: 10rpx;
  background: #fff3f3;
  color: #dc2626;
  font-size: 20rpx;
}

.shares-loading,
.shares-empty {
  padding: 32rpx 0;
  text-align: center;
}

.shares-loading-text,
.shares-empty-text {
  font-size: 22rpx;
  color: #8394a8;
}

.shares-list {
  max-height: 320rpx;
}

.share-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f0f3f7;
}

.share-item:last-child {
  border-bottom: none;
}

.share-item-info {
  flex: 1;
  min-width: 0;
}

.share-item-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.share-item-code {
  font-size: 26rpx;
  font-weight: 700;
  color: #2f7df6;
  letter-spacing: 0.08em;
}

.share-item-tag {
  font-size: 16rpx;
  padding: 2rpx 8rpx;
  border-radius: 6rpx;
}

.share-item-tag.locked {
  background: #fff3e0;
  color: #e65100;
}

.share-item-tag.open {
  background: #e8f5e9;
  color: #2e7d32;
}

.share-item-meta {
  display: block;
  font-size: 18rpx;
  color: #8394a8;
  margin-top: 6rpx;
}

.share-item-actions {
  display: flex;
  align-items: center;
  gap: 8rpx;
  flex-shrink: 0;
}

.share-action-btn {
  height: 44rpx;
  padding: 0 14rpx;
  border-radius: 10rpx;
  background: #f0f3f7;
  color: #4b5563;
  font-size: 18rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.share-action-btn.danger {
  background: #fff3f3;
  color: #dc2626;
}

/* File type colors */
.type-image {
  color: #317ff6;
  background: #eaf2ff;
}
.type-video {
  color: #5d59e8;
  background: #f0edff;
}
.type-audio {
  color: #8b58f1;
  background: #f3ecff;
}
.type-pdf {
  color: #f05a5a;
  background: #ffecec;
}
.type-doc {
  color: #f09b22;
  background: #fff2dc;
}
.type-file {
  color: #22b883;
  background: #e8fbf4;
}
</style>
