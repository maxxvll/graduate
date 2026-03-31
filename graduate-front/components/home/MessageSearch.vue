<template>
  <view class="message-search" :class="{ 'is-mobile': isMobile }">
    <!-- 头部搜索区 -->
    <view class="search-header">
      <view class="search-box">
        <view class="search-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <circle cx="11" cy="11" r="7"/>
            <path d="m21 21-3.5-3.5"/>
          </svg>
        </view>
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索聊天记录"
          confirm-type="search"
          :focus="autoFocus"
          @confirm="handleSearch"
          @input="handleInput"
        />
        <view v-if="keyword" class="search-clear" @click="clearKeyword">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </view>
      </view>
      <view class="search-cancel" @click="$emit('close')">取消</view>
    </view>

    <!-- 分类Tab -->
    <view class="search-tabs">
      <scroll-view class="tabs-scroll" scroll-x enable-flex>
        <view class="tabs-row">
          <view
            v-for="tab in searchTabs"
            :key="tab.key"
            class="tab-item"
            :class="{ active: activeTab === tab.key }"
            @click="switchTab(tab.key)"
          >
            {{ tab.label }}
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 搜索结果 -->
    <scroll-view class="results-area" scroll-y @scrolltolower="loadMore">
      <!-- 加载中 -->
      <view v-if="isLoading && !results.length" class="state-wrap">
        <view class="state-loading">
          <view class="loading-spinner"/>
        </view>
        <text class="state-text">搜索中...</text>
      </view>

      <!-- 空结果 -->
      <view v-else-if="!isLoading && !results.length && hasSearched" class="state-wrap">
        <view class="state-icon">
          <svg viewBox="0 0 48 48" fill="none">
            <circle cx="24" cy="24" r="18" stroke="currentColor" stroke-width="2"/>
            <circle cx="24" cy="24" r="6" stroke="currentColor" stroke-width="2"/>
            <line x1="36" y1="36" x2="44" y2="44" stroke="currentColor" stroke-width="2"/>
          </svg>
        </view>
        <text class="state-title">没有找到相关聊天记录</text>
        <text class="state-desc">试试换个关键词</text>
      </view>

      <!-- 初始状态 -->
      <view v-else-if="!hasSearched" class="state-wrap">
        <view class="state-icon">
          <svg viewBox="0 0 48 48" fill="none">
            <circle cx="24" cy="24" r="18" stroke="currentColor" stroke-width="2"/>
            <circle cx="24" cy="24" r="6" stroke="currentColor" stroke-width="2"/>
            <line x1="36" y1="36" x2="44" y2="44" stroke="currentColor" stroke-width="2"/>
          </svg>
        </view>
        <text class="state-title">搜索聊天记录</text>
        <text class="state-desc">输入关键词，快速找到想找的消息</text>
      </view>

      <!-- 结果列表 -->
      <view v-else class="result-list">
        <view
          v-for="(item, index) in results"
          :key="`${item.id || item.messageNo || index}`"
          class="result-item"
          @click="handleResultClick(item)"
        >
          <!-- 发送者信息 -->
          <view class="item-header">
            <image v-if="item.senderAvatar" class="item-avatar" :src="item.senderAvatar" mode="aspectFill"/>
            <view v-else class="item-avatar default">{{ (item.senderName || '?').charAt(0) }}</view>
            <view class="item-info">
              <text class="item-name">{{ item.senderName || '未知用户' }}</text>
              <text class="item-time">{{ formatTime(item.sendTime || item.createTime) }}</text>
            </view>
          </view>

          <!-- 消息内容 -->
          <view class="item-body">
            <view v-if="isImageMessage(item)" class="msg-image">
              <image class="msg-thumb" :src="item.fileUrl" mode="aspectFill"/>
            </view>
            <view v-else-if="isAudioMessage(item)" class="msg-audio">
              <view class="audio-icon">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.91-3c-.49 0-.9.36-.98.85C16.52 14.2 14.47 16 12 16s-4.52-1.8-4.93-4.15a.998.998 0 00-.98-.85c-.61 0-1.09.54-1 1.14.49 3 2.89 5.35 5.91 5.78V20c0 .55.45 1 1 1s1-.45 1-1v-2.08c3.02-.43 5.42-2.78 5.91-5.78.1-.6-.39-1.14-1-1.14z"/>
                </svg>
              </view>
              <text class="audio-duration">{{ formatDuration(item.duration) }}</text>
            </view>
            <view v-else-if="isFileMessage(item)" class="msg-file">
              <view class="file-icon">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6zm-1 9h-2v2h2v2h-2v2h-2v-2h2v-2h-2V9h2V7h-2v2h2v2z"/>
                </svg>
              </view>
              <view class="file-info">
                <text class="file-name">{{ item.fileName || '[文件]' }}</text>
                <text v-if="item.fileSize" class="file-size">{{ formatFileSize(item.fileSize) }}</text>
              </view>
            </view>
            <text v-else class="msg-text">{{ item.content || '[空消息]' }}</text>
          </view>

          <!-- 会话上下文 -->
          <view v-if="showSessionContext && item.sessionName" class="item-source">
            <text class="source-text">来自：{{ item.sessionName }}</text>
          </view>
        </view>

        <!-- 加载更多 -->
        <view v-if="isLoading && results.length" class="load-more">
          <view class="loading-spinner small"/>
          <text class="loading-text">加载中...</text>
        </view>

        <!-- 没有更多 -->
        <view v-if="!isLoading && !hasMore && results.length" class="no-more">
          <text class="no-more-text">已加载全部</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, watch, computed } from 'vue'

const props = defineProps({
  sessionId: { type: [String, Number], default: '' },
  showSessionContext: { type: Boolean, default: true },
  mobile: { type: Boolean, default: false },
  autoFocus: { type: Boolean, default: true },
})

const emit = defineEmits(['close', 'result-click'])

const keyword = ref('')
const activeTab = ref('all')
const results = ref([])
const isLoading = ref(false)
const hasMore = ref(false)
const hasSearched = ref(false)
const currentPage = ref(1)
const PAGE_SIZE = 20

// 检测移动端
const isMobile = computed(() => props.mobile || typeof uni !== 'undefined' && uni.getSystemInfoSync?.().platform !== 'pc')

const searchTabs = [
  { key: 'all', label: '全部' },
  { key: 'text', label: '文本' },
  { key: 'image', label: '图片' },
  { key: 'audio', label: '语音' },
  { key: 'file', label: '文件' },
]

const isImageMessage = (item) => Number(item.messageType) === 2
const isAudioMessage = (item) => Number(item.messageType) === 4
const isFileMessage = (item) => Number(item.messageType) === 5 || (item.fileUrl && item.fileName)

const formatTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (isNaN(date.getTime())) return ''
  const now = new Date()
  const dayDiff = Math.floor((new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime() -
    new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()) / 86400000)

  if (dayDiff === 0) return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  if (dayDiff === 1) return '昨天'
  if (dayDiff < 7) return `${date.getMonth() + 1}月${date.getDate()}日`
  return `${date.getFullYear()}/${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`
}

const formatDuration = (seconds) => {
  if (!seconds) return '[语音]'
  const total = Math.round(Number(seconds))
  if (total >= 60) return `${Math.floor(total / 60)}'${total % 60}"`
  return `${total}"`
}

const formatFileSize = (size) => {
  if (!size) return ''
  const units = ['B', 'KB', 'MB', 'GB']
  let value = Number(size)
  let index = 0
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index += 1
  }
  return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`
}

const buildSearchParams = (page) => {
  const params = { keyword: keyword.value.trim(), page, size: PAGE_SIZE }
  if (props.sessionId) params.sessionId = props.sessionId
  if (activeTab.value !== 'all') params.messageType = activeTab.value
  return params
}

const doSearch = async (append = false) => {
  if (!keyword.value.trim() || isLoading.value) return

  const page = append ? currentPage.value + 1 : 1
  isLoading.value = true
  hasSearched.value = true

  try {
    const res = await uni.$http.get('/chat/search', { params: buildSearchParams(page) })
    if (res.code === 200 && res.data) {
      const records = res.data.records || res.data.list || []
      results.value = append ? [...results.value, ...records] : records
      const total = res.data.total || 0
      hasMore.value = results.value.length < total
      currentPage.value = page
    }
  } catch (e) {
    console.error('搜索聊天记录失败', e)
    if (!append) results.value = []
  } finally {
    isLoading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  hasMore.value = false
  doSearch(false)
}

const handleInput = () => {
  if (!keyword.value.trim()) {
    results.value = []
    hasSearched.value = false
    hasMore.value = false
    currentPage.value = 1
  }
}

const handleResultClick = (item) => emit('result-click', item)

const switchTab = (tabKey) => {
  if (activeTab.value === tabKey) return
  activeTab.value = tabKey
  if (hasSearched.value) handleSearch()
}

const loadMore = () => {
  if (!hasMore.value || isLoading.value) return
  doSearch(true)
}

const clearKeyword = () => {
  keyword.value = ''
  results.value = []
  hasSearched.value = false
  hasMore.value = false
  currentPage.value = 1
}

watch(() => props.sessionId, clearKeyword)
</script>

<style scoped>
/* ========== CSS Variables ========== */
.message-search {
  --wc-green: #07c160;
  --wc-green-hover: #06ad56;
  --wc-bg: #f5f5f5;
  --wc-bg-white: #ffffff;
  --wc-text: #191919;
  --wc-text-sec: #888888;
  --wc-text-ter: #b3b3b3;
  --wc-border: #e5e5e5;
  --wc-divider: rgba(0, 0, 0, 0.1);
  --wc-radius: 8px;
  --wc-radius-lg: 12px;
  --wc-touch-min: 44px;

  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--wc-bg);
}

/* ========== Header ========== */
.search-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  padding-top: max(12px, env(safe-area-inset-top));
  background: var(--wc-bg-white);
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  height: var(--wc-touch-min);
  padding: 0 12px;
  background: var(--wc-bg);
  border-radius: var(--wc-radius);
}

.search-icon {
  width: 18px;
  height: 18px;
  color: var(--wc-text-ter);
  flex-shrink: 0;
  margin-right: 8px;
}

.search-icon svg { width: 100%; height: 100%; }

.search-input {
  flex: 1;
  height: 100%;
  border: none;
  background: transparent;
  font-size: 16px;
  color: var(--wc-text);
  outline: none;
}

.search-input::placeholder { color: var(--wc-text-ter); }

.search-clear {
  width: var(--wc-touch-min);
  height: var(--wc-touch-min);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--wc-text-ter);
  flex-shrink: 0;
}

.search-clear svg { width: 16px; height: 16px; }

.search-cancel {
  font-size: 16px;
  color: var(--wc-green);
  padding: 8px;
  flex-shrink: 0;
}

/* ========== Tabs ========== */
.search-tabs {
  background: var(--wc-bg-white);
  border-bottom: 1px solid var(--wc-border);
}

.tabs-scroll { width: 100%; }

.tabs-row {
  display: flex;
  gap: 8px;
  padding: 0 16px 12px;
  min-width: max-content;
}

.tab-item {
  padding: 8px 16px;
  font-size: 14px;
  color: var(--wc-text-sec);
  border-radius: var(--wc-radius);
  background: var(--wc-bg);
  white-space: nowrap;
  min-height: var(--wc-touch-min);
  display: flex;
  align-items: center;
}

.tab-item.active {
  background: var(--wc-green);
  color: #ffffff;
}

/* ========== Results ========== */
.results-area {
  flex: 1;
  min-height: 0;
}

.state-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 12px;
}

.state-loading { margin-bottom: 8px; }

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--wc-border);
  border-top-color: var(--wc-green);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.loading-spinner.small {
  width: 20px;
  height: 20px;
  border-width: 2px;
}

@keyframes spin { to { transform: rotate(360deg); } }

.state-icon {
  width: 64px;
  height: 64px;
  color: var(--wc-text-ter);
  margin-bottom: 8px;
}

.state-icon svg { width: 100%; height: 100%; }

.state-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--wc-text);
}

.state-desc {
  font-size: 13px;
  color: var(--wc-text-sec);
}

.state-text {
  font-size: 14px;
  color: var(--wc-text-sec);
}

/* ========== Result List ========== */
.result-list { padding: 8px 0; }

.result-item {
  background: var(--wc-bg-white);
  padding: 14px 16px;
  border-bottom: 1px solid var(--wc-divider);
  min-height: var(--wc-touch-min);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.result-item:active { background: #f0f0f0; }

.item-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-avatar {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  background: var(--wc-bg);
  flex-shrink: 0;
}

.item-avatar.default {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 500;
  color: #ffffff;
  background: var(--wc-green);
}

.item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.item-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--wc-text);
}

.item-time {
  font-size: 12px;
  color: var(--wc-text-ter);
}

.item-body {
  padding-left: 52px;
}

.msg-text {
  font-size: 14px;
  color: var(--wc-text-sec);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Image */
.msg-image { display: inline-block; }

.msg-thumb {
  width: 72px;
  height: 72px;
  border-radius: var(--wc-radius);
  object-fit: cover;
}

/* Audio */
.msg-audio {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--wc-bg);
  border-radius: var(--wc-radius);
}

.audio-icon {
  width: 22px;
  height: 22px;
  color: var(--wc-green);
}

.audio-icon svg { width: 100%; height: 100%; }

.audio-duration {
  font-size: 13px;
  color: var(--wc-text-sec);
}

/* File */
.msg-file {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--wc-bg);
  border-radius: var(--wc-radius);
}

.file-icon {
  width: 28px;
  height: 28px;
  color: var(--wc-green);
}

.file-icon svg { width: 100%; height: 100%; }

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  color: var(--wc-text);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 12px;
  color: var(--wc-text-ter);
}

.item-source {
  padding-left: 52px;
}

.source-text {
  font-size: 12px;
  color: var(--wc-text-ter);
}

.load-more {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  padding: 16px;
}

.loading-text {
  font-size: 13px;
  color: var(--wc-text-sec);
}

.no-more {
  display: flex;
  justify-content: center;
  padding: 16px;
}

.no-more-text {
  font-size: 12px;
  color: var(--wc-text-ter);
}

/* ========== Mobile Responsive ========== */
.is-mobile .search-header {
  padding: 12px 12px 12px;
  padding-left: max(12px, env(safe-area-inset-left));
  padding-right: max(12px, env(safe-area-inset-right));
}

.is-mobile .search-cancel {
  padding: 8px 12px;
}

.is-mobile .tabs-row {
  padding: 0 12px 12px;
  padding-left: max(12px, env(safe-area-inset-left));
}

.is-mobile .result-item {
  padding: 16px 12px;
  padding-left: max(16px, env(safe-area-inset-left));
  padding-right: max(16px, env(safe-area-inset-right));
}

.is-mobile .item-body,
.is-mobile .item-source {
  padding-left: 0;
  padding-top: 8px;
}
</style>