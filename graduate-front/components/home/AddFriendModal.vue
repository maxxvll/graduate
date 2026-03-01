<template>
  <view v-if="show" class="modal-mask" @click.self="$emit('close')">
    <view class="modal-box" @click.stop>
      <!-- 标题栏 -->
      <view class="modal-header">
        <text class="modal-title">添加朋友</text>
        <text class="modal-close" @click="$emit('close')">✕</text>
      </view>

      <!-- 搜索栏 -->
      <view class="modal-search-bar">
        <view class="search-input-wrap">
          <text class="search-prefix-icon">🔍</text>
          <input
            v-model="keyword"
            class="search-input"
            placeholder="搜索用户名或手机号"
            @keyup.enter="doSearch"
            :disabled="loading"
          />
          <text v-if="keyword" class="search-clear" @click="clearSearch">✕</text>
        </view>
        <button
          class="search-btn"
          :disabled="!keyword.trim() || loading"
          @click="doSearch"
        >{{ loading ? '搜索中...' : '搜索' }}</button>
      </view>

      <!-- 结果区 -->
      <view class="modal-body">
        <!-- 未搜索时的引导提示 -->
        <view v-if="!searched" class="hint-block">
          <text class="hint-icon">👤</text>
          <text class="hint-text">输入用户名或手机号，搜索添加新朋友</text>
        </view>

        <!-- 搜索中骨架 -->
        <view v-else-if="loading" class="loading-block">
          <text class="loading-text">搜索中...</text>
        </view>

        <!-- 无结果 -->
        <view v-else-if="result === null" class="empty-block">
          <text class="empty-icon">🔍</text>
          <text class="empty-text">未找到该用户，请确认用户名是否正确</text>
        </view>

        <!-- 搜索结果卡片 -->
        <view v-else class="result-card">
          <image :src="result.avatar || defaultAvatar" class="result-avatar" mode="aspectFill" />
          <view class="result-info">
            <text class="result-name">{{ result.nickname }}</text>
            <text class="result-username">用户名：{{ result.username }}</text>
            <text v-if="result.signature" class="result-signature">{{ result.signature }}</text>
          </view>
          <view class="result-action">
            <button v-if="result.isFriend"     class="action-btn btn-disabled" disabled>已是好友</button>
            <button v-else-if="result.isApplied" class="action-btn btn-pending" disabled>已申请</button>
            <button v-else class="action-btn btn-add" :disabled="applying" @click="sendApply">
              {{ applying ? '发送中...' : '+加好友' }}
            </button>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'
import service from '@/utils/request'

const props = defineProps({
  show:          { type: Boolean, required: true },
  defaultAvatar: { type: String,  default: '' },
})

const emit = defineEmits(['close', 'applied'])

// 弹窗内部完全自管理的状态，关闭后重置，避免残留脏数据
const keyword  = ref('')
const result   = ref(undefined) // undefined=未搜索, null=无结果, object=找到
const searched = ref(false)
const loading  = ref(false)
const applying = ref(false)

/**
 * 执行用户搜索。
 * 接口返回字段说明：isFriend(已是好友)、isApplied(已申请待审核)
 */
const doSearch = async () => {
  const q = keyword.value.trim()
  if (!q || loading.value) return
  loading.value  = true
  searched.value = true
  result.value   = undefined

  try {
    const res = await service.get('/user/search', { params: { keyword: q } })
    result.value = res.code === 200 && res.data ? res.data : null
  } catch {
    result.value = null
  } finally {
    loading.value = false
  }
}

/**
 * 发送好友申请。成功后把按钮状态改为"已申请"，并通知父组件刷新通知列表。
 */
const sendApply = async () => {
  if (!result.value || applying.value) return
  applying.value = true
  try {
    const res = await service.post('/friend/apply', { targetId: result.value.id })
    if (res.code === 200) {
      result.value = { ...result.value, isApplied: true }
      emit('applied')
    } else {
      uni.showToast({ title: res.msg || '申请失败', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
  } finally {
    applying.value = false
  }
}

const clearSearch = () => {
  keyword.value  = ''
  result.value   = undefined
  searched.value = false
}

// 弹窗关闭时重置所有状态，保证下次打开干净
watch(() => props.show, (visible) => {
  if (!visible) clearSearch()
})
</script>

<style scoped>
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1500;
}
.modal-box {
  background: #fff;
  border-radius: 14px;
  width: 460px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

/* 标题栏 */
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 14px;
  border-bottom: 1px solid #f0f0f0;
}
.modal-title  { font-size: 16px; font-weight: 600; color: #1a1a1a; }
.modal-close  { font-size: 16px; color: #aaa; cursor: pointer; padding: 2px 6px; border-radius: 4px; }
.modal-close:hover { color: #333; }

/* 搜索栏 */
.modal-search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  border-bottom: 1px solid #f5f5f5;
}
.search-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  background: #f7f8fa;
  border-radius: 8px;
  padding: 0 10px;
  gap: 6px;
  border: 1px solid #ebebeb;
}
.search-prefix-icon { font-size: 14px; color: #bbb; }
.search-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #1a1a1a;
  padding: 9px 0;
  outline: none;
}
.search-clear { font-size: 13px; color: #bbb; cursor: pointer; }
.search-btn {
  padding: 8px 18px;
  background: #1677ff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s;
  line-height: 1.5;
}
.search-btn:disabled { background: #b0c9f5; cursor: not-allowed; }
.search-btn:not(:disabled):hover { background: #0f63d9; }

/* 结果区 */
.modal-body { padding: 20px; min-height: 140px; }

.hint-block, .loading-block, .empty-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 24px 0;
}
.hint-icon, .empty-icon { font-size: 32px; }
.hint-text, .empty-text, .loading-text { font-size: 14px; color: #aaa; }

.result-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: #f7f8fa;
  border-radius: 10px;
  border: 1px solid #ebebeb;
}
.result-avatar { width: 52px; height: 52px; border-radius: 8px; flex-shrink: 0; background: #e6e6e6; }
.result-info   { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.result-name   { font-size: 15px; font-weight: 600; color: #1a1a1a; }
.result-username { font-size: 12px; color: #aaa; }
.result-signature { font-size: 12px; color: #888; }

.action-btn {
  padding: 7px 14px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  border: none;
  white-space: nowrap;
  line-height: 1.5;
}
.btn-add      { background: #1677ff; color: #fff; }
.btn-add:not(:disabled):hover { background: #0f63d9; }
.btn-disabled { background: #f0f0f0; color: #aaa; cursor: not-allowed; }
.btn-pending  { background: #fff3cd; color: #b08000; cursor: not-allowed; }
</style>
