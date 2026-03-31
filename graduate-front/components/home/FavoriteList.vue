<template>
  <view class="favorite-list" :class="{ mobile }">
    <!-- Header -->
    <view class="favorite-header">
      <view class="header-left">
        <text class="header-title">我的收藏</text>
        <text class="header-count" v-if="totalCount > 0">{{ totalCount }}</text>
      </view>
      <view class="header-right">
        <text class="header-btn" @click="handleManage">{{ isSelectingMode ? '完成' : '管理' }}</text>
      </view>
    </view>

    <!-- Search Bar -->
    <view class="favorite-search">
      <view class="search-shell">
        <view class="search-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <path d="m21 21-4.35-4.35"/>
          </svg>
        </view>
        <input
          v-model="searchKeyword"
          class="search-input"
          placeholder="搜索收藏内容"
          confirm-type="search"
          @confirm="handleSearch"
          @input="handleSearchInput"
        />
        <view v-if="searchKeyword" class="search-clear" @click="clearSearch">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </view>
      </view>
    </view>

    <!-- Select Bar -->
    <view v-if="isSelectingMode" class="select-bar">
      <view class="select-bar-left">
        <text class="select-count">已选择 {{ selectedIds.length }} 项</text>
      </view>
      <view class="select-bar-right">
        <text class="select-action" @click="toggleSelectAll">
          {{ isAllSelected ? '取消全选' : '全选' }}
        </text>
        <view class="select-delete" @click="batchDelete">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM8 9h8v10H8V9zm7.5-5l-1-1h-5l-1 1H5v2h14V4h-3.5z"/>
          </svg>
          <text class="delete-text">删除</text>
        </view>
      </view>
    </view>

    <!-- Content -->
    <scroll-view
      class="favorite-scroll"
      scroll-y
      :refresher-enabled="true"
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="loadMore"
    >
      <!-- Loading -->
      <view v-if="isLoading && !favorites.length" class="favorite-state">
        <view class="state-loading">
          <view class="loading-spinner"></view>
        </view>
        <text class="state-text">加载中...</text>
      </view>

      <!-- Empty -->
      <view v-else-if="!isLoading && !favorites.length" class="favorite-state">
        <view class="state-icon-wrap">
          <svg class="state-icon-svg" viewBox="0 0 48 48" fill="none">
            <path d="M24 42l-1.45-.73C12.8 34.16 6 27.68 6 20c0-5 4-9 9-9 2.83 0 5.45 1.3 7.1 3.47.42.55.94 1.03 1.56 1.42C24.36 16.2 25 17.02 25 18c0 3.87-3.13 7-7 7a7.003 7.003 0 01-7-7c0-3.87 3.13-7 7-7 1.8 0 3.42.69 4.65 1.83.54.5 1.02 1.07 1.42 1.71C32.3 8.17 34.04 9 36 9c4.42 0 8-3.58 8-8 0-5.52-4.48-10-10-10C28.48 7 24 11.48 24 17c0 1.68.39 3.32 1.12 4.83.17.35.32.72.43 1.09C25.25 23.38 25 23.95 25 24c0 .27.05.54.12.8.22.9.34 1.85.34 2.83 0 5.37-4.26 9.84-10.54 13.38L24 42z" fill="#ccc"/>
          </svg>
        </view>
        <text class="state-title">暂无收藏</text>
        <text class="state-desc">长按消息可收藏重要内容</text>
      </view>

      <!-- List -->
      <view v-else class="favorite-items">
        <view
          v-for="item in favorites"
          :key="item.id"
          class="favorite-item"
          :class="{ selected: selectedIds.includes(item.id) }"
          @click="handleItemClick(item)"
          @longpress="handleLongPress(item)"
        >
          <!-- Checkbox -->
          <view v-if="isSelectingMode" class="item-checkbox">
            <view class="checkbox-box" :class="{ checked: selectedIds.includes(item.id) }"></view>
          </view>

          <!-- Content -->
          <view class="item-content">
            <!-- Sender -->
            <view class="item-header">
              <image v-if="item.senderAvatar" class="item-avatar" :src="item.senderAvatar" mode="aspectFill" />
              <view v-else class="item-avatar default">{{ (item.senderName || '?').charAt(0) }}</view>
              <view class="item-meta">
                <text class="item-sender">{{ item.senderName || '未知用户' }}</text>
                <text class="item-time">{{ formatTime(item.createTime) }}</text>
              </view>
            </view>

            <!-- Body -->
            <view class="item-body">
              <!-- Image -->
              <view v-if="isImageType(item)" class="item-image-wrap">
                <image class="item-image" :src="item.fileUrl" mode="aspectFill" @click.stop="previewImage(item.fileUrl)" />
              </view>

              <!-- File -->
              <view v-else-if="isFileType(item)" class="item-file-wrap">
                <view class="file-icon-box">
                  <svg viewBox="0 0 24 24" fill="currentColor">
                    <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6zm4 18H6V4h7v5h5v11z"/>
                  </svg>
                </view>
                <view class="file-info-box">
                  <text class="file-name-text">{{ item.content || '[文件]' }}</text>
                </view>
              </view>

              <!-- Text -->
              <text v-else class="item-text">{{ item.content || '[空内容]' }}</text>
            </view>
          </view>

          <!-- Actions -->
          <view v-if="!isSelectingMode" class="item-actions">
            <view v-if="canQuickUse(item)" class="action-btn use-btn" @click.stop="quickUse(item)">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
              </svg>
            </view>
            <view class="action-btn delete-btn" @click.stop="deleteFavorite(item)">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM8 9h8v10H8V9zm7.5-5l-1-1h-5l-1 1H5v2h14V4h-3.5z"/>
              </svg>
            </view>
          </view>
        </view>

        <!-- Loading More -->
        <view v-if="isLoading && favorites.length" class="loading-more">
          <view class="loading-spinner small"></view>
          <text class="loading-text">加载中...</text>
        </view>

        <!-- No More -->
        <view v-if="!isLoading && !hasMore && favorites.length" class="no-more">
          <text class="no-more-text">已加载全部</text>
        </view>
      </view>
    </scroll-view>

    <!-- Quick Send Panel -->
    <view v-if="quickUseTarget" class="quick-send-panel">
      <view class="panel-header">
        <text class="panel-title">发送给</text>
        <view class="panel-close" @click="quickUseTarget = null">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </view>
      </view>
      <view class="panel-search">
        <input v-model="quickSendKeyword" class="quick-send-input" placeholder="搜索会话..." />
      </view>
      <scroll-view class="quick-send-list" scroll-y>
        <view
          v-for="session in filteredSessions"
          :key="session.sessionId"
          class="quick-send-item"
          @click="confirmQuickUse(session)"
        >
          <image v-if="session.sessionAvatar" class="quick-send-avatar" :src="session.sessionAvatar" mode="aspectFill" />
          <view v-else class="quick-send-avatar default">{{ (session.sessionName || '?').charAt(0) }}</view>
          <text class="quick-send-name">{{ session.sessionName || '未知会话' }}</text>
        </view>
        <view v-if="!filteredSessions.length" class="quick-send-empty">
          <text class="empty-text">没有找到会话</text>
        </view>
      </scroll-view>
    </view>

    <!-- Mask -->
    <view v-if="quickUseTarget" class="panel-mask" @click="quickUseTarget = null"></view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'

const props = defineProps({
  sessions: { type: Array, default: () => [] },
  showSourceSession: { type: Boolean, default: false },
  mobile: { type: Boolean, default: false },
})

const emit = defineEmits(['use', 'delete', 'close'])

const favorites = ref([])
const isLoading = ref(false)
const hasMore = ref(false)
const isRefreshing = ref(false)
const totalCount = ref(0)
const searchKeyword = ref('')
const isSelectingMode = ref(false)
const selectedIds = ref([])
const currentPage = ref(1)
const PAGE_SIZE = 20

const quickUseTarget = ref(null)
const quickSendKeyword = ref('')

const filteredSessions = computed(() => {
  if (!quickSendKeyword.value.trim()) return props.sessions
  const keyword = quickSendKeyword.value.toLowerCase()
  return props.sessions.filter(s => (s.sessionName || '').toLowerCase().includes(keyword))
})

const isAllSelected = computed(() => selectedIds.value.length === favorites.value.length)

const isImageType = (item) => item.messageType === 'IMAGE' || item.messageType === '2'
const isFileType = (item) => item.messageType === 'FILE' || item.messageType === '5' || (item.fileUrl && !isImageType(item))

const canQuickUse = (item) => item.messageType === 'TEXT' || item.messageType === '1' || item.messageType === 'IMAGE' || item.messageType === '2'

const formatTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (isNaN(date.getTime())) return ''
  const now = new Date()
  const dayDiff = Math.floor((new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime() - new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()) / 86400000)

  if (dayDiff === 0) return `今天 ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  if (dayDiff === 1) return '昨天'
  if (dayDiff < 7) return `${dayDiff}天前`
  return `${date.getFullYear()}/${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`
}

const loadFavoriteList = async (append = false) => {
  if (isLoading.value || (!append && !hasMore.value)) return
  isLoading.value = true
  try {
    const page = append ? currentPage.value + 1 : 1
    let res
    if (searchKeyword.value.trim()) {
      res = await uni.$http.get('/favorite/search', { params: { keyword: searchKeyword.value.trim(), page, size: PAGE_SIZE } })
    } else {
      res = await uni.$http.get('/favorite/list', { params: { page, size: PAGE_SIZE } })
    }
    if (res.code === 200 && res.data) {
      const records = res.data.records || res.data.list || []
      if (append) favorites.value.push(...records)
      else favorites.value = records
      totalCount.value = res.data.total || favorites.value.length
      hasMore.value = favorites.value.length < totalCount.value
      currentPage.value = page
    }
  } catch (e) {
    console.error('获取收藏列表失败', e)
  } finally {
    isLoading.value = false
  }
}

const handleSearch = () => { currentPage.value = 1; hasMore.value = false; loadFavoriteList(false) }
const handleSearchInput = () => { if (!searchKeyword.value.trim()) { currentPage.value = 1; hasMore.value = false; loadFavoriteList(false) } }
const clearSearch = () => { searchKeyword.value = ''; currentPage.value = 1; hasMore.value = false; loadFavoriteList(false) }
const loadMore = () => { if (!hasMore.value || isLoading.value) return; loadFavoriteList(true) }
const onRefresh = async () => { isRefreshing.value = true; currentPage.value = 1; hasMore.value = false; await loadFavoriteList(false); isRefreshing.value = false }
const previewImage = (url) => { if (!url) return; uni.previewImage({ urls: [url], current: url }) }
const handleItemClick = (item) => { if (!isSelectingMode.value) return; toggleSelect(item) }
const handleLongPress = (item) => { if (!isSelectingMode.value) { isSelectingMode.value = true; selectedIds.value = [item.id] } }

const toggleSelect = (item) => {
  const idx = selectedIds.value.indexOf(item.id)
  if (idx >= 0) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(item.id)
}

const toggleSelectAll = () => {
  if (isAllSelected.value) selectedIds.value = []
  else selectedIds.value = favorites.value.map(item => item.id)
}

const handleManage = () => {
  if (isSelectingMode.value) { isSelectingMode.value = false; selectedIds.value = [] }
  else isSelectingMode.value = true
}

const deleteFavorite = async (item) => {
  try {
    const res = await uni.$http.delete(`/favorite/${item.id}`)
    if (res.code === 200) {
      favorites.value = favorites.value.filter(f => f.id !== item.id)
      uni.showToast({ title: '已取消收藏', icon: 'success' })
      emit('delete', item.id)
    }
  } catch (e) { console.error('删除收藏失败', e) }
}

const batchDelete = async () => {
  if (!selectedIds.value.length) return
  try {
    uni.showModal({
      title: '确认删除',
      content: `确定要删除选中的 ${selectedIds.value.length} 条收藏吗？`,
      success: async (modalRes) => {
        if (modalRes.confirm) {
          let successCount = 0
          for (const id of selectedIds.value) {
            try {
              const res = await uni.$http.delete(`/favorite/${id}`)
              if (res.code === 200) successCount++
            } catch {}
          }
          favorites.value = favorites.value.filter(f => !selectedIds.value.includes(f.id))
          uni.showToast({ title: `已删除${successCount}条收藏`, icon: 'success' })
          isSelectingMode.value = false
          selectedIds.value = []
        }
      }
    })
  } catch (e) { console.error('批量删除失败', e) }
}

const quickUse = (item) => { quickUseTarget.value = item; quickSendKeyword.value = '' }
const confirmQuickUse = (session) => {
  if (!quickUseTarget.value) return
  emit('use', { favorite: quickUseTarget.value, session })
  quickUseTarget.value = null
}

onMounted(() => { loadFavoriteList(false) })
</script>

<style scoped>
/* ===== CSS Variables - WeChat Theme ===== */
.favorite-list {
  --wx-green: #07c160;
  --wx-green-hover: #06ad56;
  --wx-bg: #f5f5f5;
  --wx-white: #ffffff;
  --wx-text: #191919;
  --wx-text-2: #888888;
  --wx-text-3: #b3b3b3;
  --wx-border: #e5e5e5;
  --wx-divider: rgba(0, 0, 0, 0.1);
  --wx-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  --wx-radius: 8px;
  --wx-radius-lg: 12px;
  --wx-tap: 44px;
  --wx-safe-bottom: env(safe-area-inset-bottom, 0px);
  --wx-tap-pad: max(0px, env(safe-area-inset-bottom));

  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--wx-white);
  position: relative;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* ===== Header ===== */
.favorite-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 14px;
}

.header-left { display: flex; align-items: baseline; gap: 8px; }

.header-title { font-size: 20px; font-weight: 600; color: var(--wx-text); letter-spacing: -0.3px; }

.header-count { font-size: 13px; color: var(--wx-text-2); }

.header-right { display: flex; align-items: center; }

.header-btn { font-size: 16px; color: var(--wx-green); padding: 8px 4px; min-height: var(--wx-tap); display: flex; align-items: center; }

/* ===== Search ===== */
.favorite-search { padding: 0 16px 14px; }

.search-shell {
  display: flex; align-items: center; gap: 8px;
  height: 40px; padding: 0 12px;
  background: var(--wx-bg); border-radius: 10px;
}

.search-icon { width: 18px; height: 18px; color: var(--wx-text-3); flex-shrink: 0; }
.search-icon svg { width: 100%; height: 100%; }

.search-input { flex: 1; height: 100%; border: none; background: transparent; font-size: 15px; color: var(--wx-text); outline: none; }
.search-input::placeholder { color: var(--wx-text-3); }

.search-clear { width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; color: var(--wx-text-3); flex-shrink: 0; }
.search-clear svg { width: 16px; height: 16px; }

/* ===== Select Bar ===== */
.select-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px;
  background: var(--wx-white);
  border-bottom: 1px solid var(--wx-border);
}

.select-bar-left { flex: 1; }
.select-count { font-size: 14px; color: var(--wx-text-2); }

.select-bar-right { display: flex; align-items: center; gap: 20px; }

.select-action { font-size: 14px; color: var(--wx-green); min-height: var(--wx-tap); display: flex; align-items: center; }

.select-delete {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 16px; min-height: var(--wx-tap);
  background: #f2f2f2; border-radius: 18px;
  display: flex; align-items: center; justify-content: center;
}

.select-delete svg { width: 16px; height: 16px; color: var(--wx-text-2); }
.delete-text { font-size: 14px; color: var(--wx-text-2); }

/* ===== Scroll ===== */
.favorite-scroll { flex: 1; min-height: 0; }

/* ===== State ===== */
.favorite-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 100px 20px; gap: 12px;
}

.state-loading { margin-bottom: 8px; }

.loading-spinner { width: 32px; height: 32px; border: 3px solid var(--wx-border); border-top-color: var(--wx-green); border-radius: 50%; animation: spin 0.8s linear infinite; }
.loading-spinner.small { width: 20px; height: 20px; border-width: 2px; }

@keyframes spin { to { transform: rotate(360deg); } }

.state-icon-wrap { margin-bottom: 12px; }
.state-icon-svg { width: 80px; height: 80px; }

.state-title { font-size: 17px; font-weight: 500; color: var(--wx-text); }
.state-desc { font-size: 14px; color: var(--wx-text-2); }
.state-text { font-size: 14px; color: var(--wx-text-2); }

/* ===== Items ===== */
.favorite-items { padding: 8px 12px; }

.favorite-item {
  display: flex; align-items: flex-start; gap: 12px;
  padding: 16px; margin-bottom: 10px;
  background: var(--wx-bg); border-radius: var(--wx-radius-lg);
}

.favorite-item:last-child { margin-bottom: 0; }
.favorite-item.selected { background: #e8f8ee; box-shadow: 0 0 0 1.5px var(--wx-green); }
.favorite-item:active { opacity: 0.95; }

/* Checkbox */
.item-checkbox { padding-top: 2px; flex-shrink: 0; min-width: var(--wx-tap); display: flex; align-items: flex-start; }
.checkbox-box { width: 22px; height: 22px; border: 2px solid #ccc; border-radius: 50%; transition: all 0.2s; }
.checkbox-box.checked { background: var(--wx-green); border-color: var(--wx-green); }

/* Content */
.item-content { flex: 1; min-width: 0; }

.item-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }

.item-avatar { width: 42px; height: 42px; border-radius: 8px; background: #e8e8e8; flex-shrink: 0; }
.item-avatar.default { display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: 500; color: var(--wx-white); background: var(--wx-green); }

.item-meta { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.item-sender { font-size: 14px; font-weight: 500; color: var(--wx-text); }
.item-time { font-size: 12px; color: var(--wx-text-3); }

.item-body { padding-left: 52px; }

.item-text {
  font-size: 14px; color: var(--wx-text-2); line-height: 1.5;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; word-break: break-all;
}

.item-image-wrap { display: inline-block; }
.item-image { width: 120px; height: 120px; border-radius: var(--wx-radius); object-fit: cover; }

.item-file-wrap { display: flex; align-items: center; gap: 10px; padding: 12px; background: var(--wx-white); border-radius: var(--wx-radius); }
.file-icon-box { width: 32px; height: 32px; color: var(--wx-green); flex-shrink: 0; }
.file-icon-box svg { width: 100%; height: 100%; }
.file-info-box { flex: 1; min-width: 0; }
.file-name-text { font-size: 14px; color: var(--wx-text); display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* Actions */
.item-actions { display: flex; flex-direction: column; gap: 8px; flex-shrink: 0; }

.action-btn { width: var(--wx-tap); height: var(--wx-tap); display: flex; align-items: center; justify-content: center; border-radius: 50%; }
.action-btn svg { width: 20px; height: 20px; }

.use-btn { background: var(--wx-green); color: var(--wx-white); }
.use-btn svg { fill: var(--wx-white); }

.delete-btn { background: #f2f2f2; color: var(--wx-text-2); }
.delete-btn svg { fill: var(--wx-text-2); }

/* ===== Loading & No More ===== */
.loading-more, .no-more { display: flex; justify-content: center; align-items: center; padding: 14px; }
.loading-text, .no-more-text { font-size: 12px; color: var(--wx-text-3); }

/* ===== Quick Send Panel ===== */
.panel-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.5); z-index: 998; }

.quick-send-panel {
  position: fixed; bottom: 0; left: 0; right: 0;
  max-height: 65vh;
  background: var(--wx-white); border-radius: 16px 16px 0 0;
  z-index: 999; display: flex; flex-direction: column;
  padding-bottom: var(--wx-tap-pad);
}

.panel-header { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-bottom: 1px solid var(--wx-border); }
.panel-title { font-size: 17px; font-weight: 600; color: var(--wx-text); }

.panel-close {
  width: 32px; height: 32px; border-radius: 50%; background: var(--wx-bg);
  display: flex; align-items: center; justify-content: center;
}
.panel-close svg { width: 16px; height: 16px; color: var(--wx-text-2); }

.panel-search { padding: 12px 16px; }
.quick-send-input { height: 44px; padding: 0 14px; background: var(--wx-bg); border-radius: 10px; font-size: 16px; outline: none; border: none; }

.quick-send-list { flex: 1; min-height: 0; max-height: 40vh; }

.quick-send-item {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 20px; min-height: var(--wx-tap);
}
.quick-send-item:active { background: var(--wx-bg); }

.quick-send-avatar { width: 44px; height: 44px; border-radius: 8px; background: #e8e8e8; flex-shrink: 0; }
.quick-send-avatar.default { display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: 500; color: var(--wx-white); background: var(--wx-green); }

.quick-send-name { font-size: 15px; color: var(--wx-text); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.quick-send-empty { display: flex; justify-content: center; padding: 24px; }
.empty-text { font-size: 14px; color: var(--wx-text-2); }

/* ===== Mobile Optimizations ===== */
@media (max-width: 768px) {
  .favorite-header { padding: 14px 16px 12px; }
  .header-title { font-size: 22px; }

  .favorite-search { padding: 0 16px 12px; }
  .search-shell { height: 44px; border-radius: 10px; }
  .search-input { font-size: 16px; }

  .select-bar { padding: 10px 16px; }
  .select-bar-right { gap: 16px; }
  .select-delete { padding: 10px 16px; }

  .favorite-items { padding: 6px 12px; }
  .favorite-item { padding: 14px; margin-bottom: 8px; }

  .item-avatar { width: 40px; height: 40px; border-radius: 8px; }
  .item-avatar.default { font-size: 16px; }
  .item-meta { gap: 2px; }
  .item-sender { font-size: 14px; }
  .item-time { font-size: 11px; }

  .item-body { padding-left: 50px; }
  .item-text { font-size: 14px; -webkit-line-clamp: 2; }

  .item-image { width: 100px; height: 100px; }

  .item-file-wrap { padding: 10px 12px; gap: 8px; }
  .file-icon-box { width: 28px; height: 28px; }
  .file-name-text { font-size: 13px; }

  .item-actions { flex-direction: row; gap: 6px; }
  .action-btn { width: 40px; height: 40px; }
  .action-btn svg { width: 18px; height: 18px; }

  .quick-send-panel { max-height: 70vh; }
  .panel-header { padding: 16px 20px; }
  .panel-title { font-size: 17px; }
  .panel-search { padding: 12px 16px; }
  .quick-send-input { height: 48px; }
  .quick-send-item { padding: 14px 20px; min-height: 50px; }
  .quick-send-avatar { width: 44px; height: 44px; }
}
</style>