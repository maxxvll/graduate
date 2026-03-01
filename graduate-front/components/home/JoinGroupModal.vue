<template>
  <view v-if="show" class="modal-mask" @click.self="$emit('close')">
    <view class="modal-box" @click.stop>
      <!-- 标题栏 -->
      <view class="modal-header">
        <text class="modal-title">加入群聊</text>
        <text class="modal-close" @click="$emit('close')">✕</text>
      </view>

      <!-- 搜索栏 -->
      <view class="modal-search-bar">
        <view class="search-input-wrap">
          <text class="search-prefix-icon">🔍</text>
          <input
            v-model="keyword"
            class="search-input"
            placeholder="输入群名称搜索"
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
        <!-- 引导提示 -->
        <view v-if="!searched" class="hint-block">
          <text class="hint-icon">👥</text>
          <text class="hint-text">输入群名称，搜索并申请加入群聊</text>
        </view>

        <view v-else-if="loading" class="loading-block">
          <text class="loading-text">搜索中...</text>
        </view>

        <view v-else-if="!results.length" class="empty-block">
          <text class="empty-icon">🔍</text>
          <text class="empty-text">未找到相关群聊</text>
        </view>

        <!-- 群聊结果列表 -->
        <scroll-view v-else scroll-y class="result-list">
          <view
            class="group-card"
            v-for="group in results"
            :key="group.id"
          >
            <image :src="group.groupAvatar || defaultAvatar" class="group-avatar" mode="aspectFill" />
            <view class="group-info">
              <text class="group-name">{{ group.groupName }}</text>
              <text class="group-count">{{ group.currentMemberCount }}/{{ group.maxMember }} 人</text>
              <!-- joinType: 1=需审核, 2=直接加入, 3=仅邀请 -->
              <text class="group-join-type" :class="joinTypeClass(group.joinType)">
                {{ joinTypeLabel(group.joinType) }}
              </text>
            </view>
            <view class="group-action">
              <button v-if="group.applyStatus === 'member'"  class="action-btn btn-disabled" disabled>已加入</button>
              <button v-else-if="group.applyStatus === 'pending'" class="action-btn btn-pending" disabled>已申请</button>
              <button v-else-if="group.joinType === 3"        class="action-btn btn-disabled" disabled>仅邀请</button>
              <button
                v-else
                class="action-btn btn-apply"
                :disabled="applying === group.id"
                @click="applyJoin(group)"
              >{{ applying === group.id ? '申请中...' : '申请加入' }}</button>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'
import service from '@/utils/request'

const props = defineProps({
  show:          { type: Boolean, required: true },
  currentUserId: { type: [String, Number], default: null },
  defaultAvatar: { type: String, default: '' },
})

const emit = defineEmits(['close', 'joined'])

const keyword  = ref('')
const results  = ref([])
const searched = ref(false)
const loading  = ref(false)
/** 当前正在申请的群 ID，用于精准控制按钮 loading 状态 */
const applying = ref(null)

/**
 * 搜索群聊。
 * 后端返回的每条 GroupInfoVO 包含 applyStatus 字段：
 *   - null     = 未申请
 *   - 'pending' = 已申请待审核
 *   - 'member'  = 已是成员
 */
const doSearch = async () => {
  const q = keyword.value.trim()
  if (!q || loading.value) return
  loading.value  = true
  searched.value = true
  results.value  = []

  try {
    const res = await service.get('/group/search', {
      params: { keyword: q, userId: props.currentUserId },
    })
    results.value = res.code === 200 && Array.isArray(res.data) ? res.data : []
  } catch {
    results.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 申请加入群聊。
 * joinType=2（免审核）时后端直接拉入，前端将状态更新为 member。
 * joinType=1（需审核）时后端创建申请记录，前端将状态更新为 pending。
 */
const applyJoin = async (group) => {
  if (applying.value) return
  applying.value = group.id
  try {
    const res = await service.post('/group/apply', {
      groupId: group.id,
      userId:  props.currentUserId,
    })
    if (res.code === 200) {
      const newStatus = group.joinType === 2 ? 'member' : 'pending'
      const idx = results.value.findIndex(g => g.id === group.id)
      if (idx !== -1) {
        results.value[idx] = { ...results.value[idx], applyStatus: newStatus }
      }
      const msg = group.joinType === 2 ? '已成功加入群聊' : '申请已发送，等待管理员审核'
      uni.showToast({ title: msg, icon: 'none' })
      emit('joined')
    } else {
      uni.showToast({ title: res.msg || '申请失败', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
  } finally {
    applying.value = null
  }
}

const clearSearch = () => {
  keyword.value  = ''
  results.value  = []
  searched.value = false
}

const joinTypeLabel = (type) => {
  const map = { 1: '需审核加入', 2: '免审核加入', 3: '仅邀请加入' }
  return map[type] ?? '未知'
}

const joinTypeClass = (type) => ({
  'type-open':    type === 2,
  'type-verify':  type === 1,
  'type-invite':  type === 3,
})

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
  width: 500px;
  max-height: 600px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 14px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.modal-title { font-size: 16px; font-weight: 600; color: #1a1a1a; }
.modal-close { font-size: 16px; color: #aaa; cursor: pointer; padding: 2px 6px; border-radius: 4px; }
.modal-close:hover { color: #333; }

.modal-search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  border-bottom: 1px solid #f5f5f5;
  flex-shrink: 0;
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

.modal-body {
  flex: 1;
  overflow: hidden;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
}

.hint-block, .loading-block, .empty-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 24px 0;
  flex: 1;
}
.hint-icon, .empty-icon { font-size: 32px; }
.hint-text, .empty-text, .loading-text { font-size: 14px; color: #aaa; }

.result-list { flex: 1; }
.group-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 10px;
  border: 1px solid #ebebeb;
  background: #f7f8fa;
  margin-bottom: 10px;
}
.group-avatar { width: 48px; height: 48px; border-radius: 8px; flex-shrink: 0; background: #e6e6e6; }
.group-info   { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.group-name   { font-size: 14px; font-weight: 600; color: #1a1a1a; }
.group-count  { font-size: 12px; color: #aaa; }
.group-join-type { font-size: 11px; padding: 2px 6px; border-radius: 4px; display: inline-block; width: fit-content; }
.type-open    { background: #e8f5e9; color: #2e7d32; }
.type-verify  { background: #fff8e1; color: #f57f17; }
.type-invite  { background: #f5f5f5; color: #aaa; }

.action-btn {
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  border: none;
  white-space: nowrap;
  line-height: 1.5;
}
.btn-apply    { background: #1677ff; color: #fff; transition: background 0.15s; }
.btn-apply:not(:disabled):hover { background: #0f63d9; }
.btn-disabled { background: #f0f0f0; color: #aaa; cursor: not-allowed; }
.btn-pending  { background: #fff3cd; color: #b08000; cursor: not-allowed; }
</style>
