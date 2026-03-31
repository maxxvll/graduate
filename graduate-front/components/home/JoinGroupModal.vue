<template>
  <view v-if="show" class="modal-mask" @click.self="$emit('close')">
    <view class="modal-box" @click.stop>
      <view class="modal-header">
        <text class="modal-title">加入群聊</text>
        <text class="modal-close" @click="$emit('close')">×</text>
      </view>

      <view class="modal-search-bar">
        <view class="search-input-wrap">
          <input
            v-model="keyword"
            class="search-input"
            placeholder="输入群名称搜索"
            @keyup.enter="doSearch"
            :disabled="loading"
          />
          <text v-if="keyword" class="search-clear" @click="clearSearch">清空</text>
        </view>
        <button
          class="search-btn"
          :disabled="!keyword.trim() || loading"
          @click="doSearch"
        >
          {{ loading ? '搜索中...' : '搜索' }}
        </button>
      </view>

      <view class="modal-body">
        <view v-if="!searched" class="state-block">
          <text class="state-text">输入群名称后搜索并申请加入群聊</text>
        </view>

        <view v-else-if="loading" class="state-block">
          <text class="state-text">搜索中...</text>
        </view>

        <view v-else-if="!results.length" class="state-block">
          <text class="state-text">没有找到匹配的群聊</text>
        </view>

        <scroll-view v-else scroll-y class="result-list">
          <view
            v-for="group in results"
            :key="group.id"
            class="group-card"
          >
            <image
              :src="group.groupAvatar || defaultAvatar"
              class="group-avatar"
              mode="aspectFill"
            />
            <view class="group-info">
              <text class="group-name">{{ group.groupName }}</text>
              <text class="group-count">
                {{ displayMemberCount(group) }}/{{ group.maxMember || '-' }} 人
              </text>
              <text class="group-join-type" :class="joinTypeClass(group.joinType)">
                {{ joinTypeLabel(group.joinType) }}
              </text>
            </view>
            <view class="group-action">
              <button
                v-if="group.applyStatus === 'member'"
                class="action-btn btn-disabled"
                disabled
              >
                已加入
              </button>
              <button
                v-else-if="group.applyStatus === 'pending'"
                class="action-btn btn-pending"
                disabled
              >
                已申请
              </button>
              <button
                v-else-if="group.joinType === 3"
                class="action-btn btn-disabled"
                disabled
              >
                仅邀请
              </button>
              <button
                v-else
                class="action-btn btn-apply"
                :disabled="applying === group.id"
                @click="applyJoin(group)"
              >
                {{ applying === group.id ? '提交中...' : '申请加入' }}
              </button>
            </view>
          </view>

          <view v-if="hasMore" class="result-more">
            <button
              class="result-more-btn"
              :disabled="loadingMore"
              @click="loadMore"
            >
              {{ loadingMore ? '加载中...' : '加载更多' }}
            </button>
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
  show: {
    type: Boolean,
    required: true,
  },
  currentUserId: {
    type: [String, Number],
    default: null,
  },
  defaultAvatar: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['close', 'joined'])

const keyword = ref('')
const results = ref([])
const searched = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const applying = ref(null)
const currentPage = ref(1)
const pageSize = 20
const hasMore = ref(false)

const doSearch = async () => {
  const q = keyword.value.trim()
  if (!q || loading.value) {
    return
  }

  loading.value = true
  searched.value = true
  results.value = []
  currentPage.value = 1
  hasMore.value = false

  try {
    const res = await service.get('/group/search/page', {
      params: {
        keyword: q,
        current: 1,
        size: pageSize,
      },
    })
    if (res.code === 200 && res.data) {
      const records = Array.isArray(res.data.records) ? res.data.records : []
      results.value = records
      currentPage.value = Number(res.data.current || 1)
      hasMore.value = currentPage.value < Number(res.data.pages || 0)
      return
    }
    results.value = []
    hasMore.value = false
  } catch {
    results.value = []
    hasMore.value = false
    uni.showToast({ title: '搜索失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  const q = keyword.value.trim()
  if (!q || loading.value || loadingMore.value || !hasMore.value) {
    return
  }

  loadingMore.value = true
  try {
    const nextPage = currentPage.value + 1
    const res = await service.get('/group/search/page', {
      params: {
        keyword: q,
        current: nextPage,
        size: pageSize,
      },
    })
    if (res.code === 200 && res.data) {
      const records = Array.isArray(res.data.records) ? res.data.records : []
      results.value = [...results.value, ...records]
      currentPage.value = Number(res.data.current || nextPage)
      hasMore.value = currentPage.value < Number(res.data.pages || 0)
      return
    }
    hasMore.value = false
  } catch {
    uni.showToast({ title: '加载更多失败', icon: 'none' })
  } finally {
    loadingMore.value = false
  }
}

const applyJoin = async (group) => {
  if (applying.value) {
    return
  }

  applying.value = group.id
  try {
    const res = await service.post('/group/apply', {
      groupId: group.id,
    })
    if (res.code === 200) {
      const newStatus = group.joinType === 2 ? 'member' : 'pending'
      const index = results.value.findIndex((item) => item.id === group.id)
      if (index !== -1) {
        results.value[index] = {
          ...results.value[index],
          applyStatus: newStatus,
        }
      }
      uni.showToast({
        title: group.joinType === 2 ? '已加入群聊' : '申请已提交',
        icon: 'none',
      })
      emit('joined')
      return
    }
    uni.showToast({ title: res.message || '申请失败', icon: 'none' })
  } catch {
    uni.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
  } finally {
    applying.value = null
  }
}

const clearSearch = () => {
  keyword.value = ''
  results.value = []
  searched.value = false
  currentPage.value = 1
  hasMore.value = false
  loadingMore.value = false
}

const displayMemberCount = (group) =>
  Number(group.currentMemberCount ?? group.memberCount ?? 0)

const joinTypeLabel = (type) => {
  const map = {
    1: '需要审核',
    2: '直接加入',
    3: '仅邀请',
  }
  return map[type] || '未知'
}

const joinTypeClass = (type) => ({
  'type-open': type === 2,
  'type-verify': type === 1,
  'type-invite': type === 3,
})

watch(
  () => keyword.value,
  (value) => {
    if (!value.trim()) {
      clearSearch()
    }
  },
)

watch(
  () => props.show,
  (visible) => {
    if (!visible) {
      clearSearch()
    }
  },
)
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
  width: 520px;
  max-height: 620px;
  background: #fff;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.18);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 14px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.modal-close {
  font-size: 20px;
  color: #999;
  cursor: pointer;
  line-height: 1;
}

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
  gap: 8px;
  padding: 0 12px;
  background: #f7f8fa;
  border: 1px solid #ebebeb;
  border-radius: 8px;
}

.search-input {
  flex: 1;
  height: 38px;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #1a1a1a;
}

.search-clear {
  font-size: 12px;
  color: #999;
}

.search-btn {
  padding: 8px 18px;
  background: #1677ff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.5;
}

.search-btn:disabled {
  background: #b0c9f5;
}

.modal-body {
  flex: 1;
  overflow: hidden;
  padding: 16px 20px;
}

.state-block {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.state-text {
  font-size: 14px;
  color: #999;
}

.result-list {
  height: 100%;
}

.group-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 10px;
  border: 1px solid #ebebeb;
  border-radius: 10px;
  background: #f7f8fa;
}

.group-avatar {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: #e6e6e6;
  flex-shrink: 0;
}

.group-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.group-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.group-count {
  font-size: 12px;
  color: #888;
}

.group-join-type {
  display: inline-block;
  width: fit-content;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
}

.type-open {
  background: #e8f5e9;
  color: #2e7d32;
}

.type-verify {
  background: #fff8e1;
  color: #f57f17;
}

.type-invite {
  background: #f5f5f5;
  color: #999;
}

.action-btn {
  min-width: 84px;
  padding: 6px 14px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.5;
}

.btn-apply {
  background: #1677ff;
  color: #fff;
}

.btn-disabled {
  background: #f0f0f0;
  color: #999;
}

.btn-pending {
  background: #fff3cd;
  color: #b08000;
}

.result-more {
  display: flex;
  justify-content: center;
  padding: 12px 0 2px;
}

.result-more-btn {
  min-width: 120px;
  padding: 7px 16px;
  border-radius: 18px;
  border: 1px solid #dbe8ff;
  background: #f4f8ff;
  color: #1677ff;
  font-size: 13px;
}

.result-more-btn:disabled {
  color: #9eb5de;
  border-color: #edf2fb;
  background: #f7f9fc;
}
</style>
