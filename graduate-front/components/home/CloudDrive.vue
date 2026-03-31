<template>
  <view class="cloud-drive">
    <view class="drive-shell">
      <view class="header-card">
        <view class="header-main">
          <view class="header-copy">
            <view class="header-title-row">
              <view class="header-title">云盘</view>
              <view class="sync-badge" :class="{ busy: isBusy }">{{ syncLabel }}</view>
            </view>
            <view class="header-desc">
              支持图片、视频、音频、PDF、文本和常见 Office 文档在线预览，视频与音频支持流式播放。
            </view>
          </view>

          <view class="header-search">
            <view class="search-box">
              <view class="search-icon"></view>
              <input
                v-model="keyword"
                class="search-input"
                placeholder="搜索文件名、后缀或类型"
                confirm-type="search"
              />
            </view>
          </view>

          <view class="header-actions">
            <view class="top-btn ghost" :class="{ disabled: importing }" @click="handleImport">
              {{ importing ? '导入中...' : '导入链接' }}
            </view>
            <view class="top-btn primary" :class="{ disabled: uploading }" @click="handleUpload">
              {{ uploading ? '上传中...' : '上传文件' }}
            </view>
          </view>
        </view>

        <view class="header-meta-row">
          <view class="capacity-card">
            <view class="capacity-top">
              <view class="capacity-label">空间占用</view>
              <view class="capacity-percent">{{ storagePercentLabel }}</view>
            </view>
            <view class="capacity-value">{{ formatFileSize(used) }} / {{ formatFileSize(quota) }}</view>
            <view class="capacity-bar">
              <view class="capacity-fill" :style="{ width: `${storagePercent}%` }"></view>
            </view>
          </view>

          <view class="meta-pill">
            <view class="meta-pill-label">最近更新</view>
            <view class="meta-pill-value">{{ latestActivityLabel }}</view>
          </view>
          <view class="meta-pill">
            <view class="meta-pill-label">在线预览</view>
            <view class="meta-pill-value">{{ previewableCount }} 项</view>
          </view>
          <view class="meta-pill">
            <view class="meta-pill-label">流媒体</view>
            <view class="meta-pill-value">{{ streamableCount }} 项</view>
          </view>
          <view class="meta-pill">
            <view class="meta-pill-label">文件数量</view>
            <view class="meta-pill-value">{{ files.length }} 个</view>
          </view>
        </view>
      </view>

      <view class="overview-card">
        <view class="overview-column recent-column">
          <view class="section-head">
            <view class="section-title">最近文件</view>
            <view class="section-link">{{ recentFiles.length ? '查看更多' : '暂无记录' }}</view>
          </view>

          <view v-if="recentFiles.length" class="recent-grid">
            <view
              v-for="file in recentFiles"
              :key="`recent-${file.object}`"
              class="recent-tile"
              @click="handlePreview(file)"
            >
              <view class="recent-thumb">
                <view class="file-mark" :class="`type-${file.iconType}`">
                  <view class="file-mark-text">{{ file.iconText }}</view>
                </view>
              </view>
              <view class="recent-name">{{ file.name }}</view>
              <view class="recent-sub">{{ file.previewable ? file.previewModeLabel : '下载后查看' }}</view>
            </view>
          </view>

          <view v-else class="empty-panel">
            <view class="empty-title">还没有最近文件</view>
            <view class="empty-text">上传一个文件后，这里会自动展示最近访问的内容。</view>
          </view>
        </view>

        <view class="overview-divider"></view>

        <view class="overview-column tools-column">
          <view class="section-head">
            <view class="section-title">常用工具</view>
            <view class="section-link">快速入口</view>
          </view>

          <view class="tool-row">
            <view
              v-for="tool in toolCards"
              :key="tool.key"
              class="tool-tile"
              @click="runTool(tool)"
            >
              <view :class="['tool-icon', `tone-${tool.tone}`]">{{ tool.short }}</view>
              <view class="tool-name">{{ tool.name }}</view>
              <view class="tool-desc">{{ tool.desc }}</view>
            </view>
          </view>
        </view>
      </view>

      <view class="library-card">
        <view class="library-tabs">
          <view
            v-for="item in displayFilterTabs"
            :key="item.key"
            class="tab-item"
            :class="{ active: activeFilter === item.key }"
            @click="activeFilter = item.key"
          >
            {{ item.label }}
          </view>
        </view>

        <view class="library-toolbar">
          <view class="toolbar-left">
            <view class="toolbar-btn primary" :class="{ disabled: uploading }" @click="handleUpload">
              上传
            </view>
            <view class="toolbar-btn light" :class="{ disabled: importing }" @click="handleImport">
              云添加
            </view>
            <view class="toolbar-btn light" :class="{ disabled: refreshing }" @click="handleRefresh">
              刷新目录
            </view>
          </view>

          <view class="toolbar-right">
            <view class="toolbar-summary">{{ resultLabel }}</view>
          </view>
        </view>

        <view class="table-head">
          <view class="col-name">文件</view>
          <view class="col-size">大小</view>
          <view class="col-type">类型</view>
          <view class="col-time">修改时间</view>
          <view class="col-actions">操作</view>
        </view>

        <scroll-view class="table-scroll" scroll-y>
          <view v-if="loading && !files.length" class="state-shell">
            <view class="empty-title">正在加载目录</view>
            <view class="empty-text">请稍等，我们正在同步文件列表和预览能力。</view>
          </view>

          <view v-else-if="!displayFiles.length" class="state-shell">
            <view class="empty-title">{{ emptyTitle }}</view>
            <view class="empty-text">{{ emptyText }}</view>
          </view>

          <view
            v-for="file in displayFiles"
            :key="file.object"
            class="table-row"
            :class="{ clickable: file.previewable && !file.syncing }"
            @click="handlePreview(file)"
          >
            <view class="name-col">
              <view class="file-mark" :class="`type-${file.iconType}`">
                <view class="file-mark-text">{{ file.iconText }}</view>
              </view>

              <view class="file-copy">
                <view class="file-copy-top">
                  <view class="file-name">{{ file.name }}</view>
                  <view v-if="file.syncing" class="file-chip syncing">同步中</view>
                  <view v-else class="file-chip">{{ extensionLabel(file) }}</view>
                </view>
                <view class="file-note">{{ file.previewable ? file.previewModeLabel : '下载后查看' }}</view>
              </view>
            </view>

            <view class="size-col">{{ formatFileSize(file.size) }}</view>
            <view class="type-col">{{ displayTypeLabel(file) }}</view>
            <view class="time-col">{{ formatTime(file.modifyTime) }}</view>

            <view class="actions-col" @click.stop>
              <view
                class="row-btn ghost"
                :class="{ disabled: !file.previewable || file.syncing || previewLoading }"
                @click="handlePreview(file)"
              >
                {{ previewButtonLabel(file) }}
              </view>
              <view
                class="row-btn ghost"
                :class="{ disabled: file.syncing || isDownloading(file.object) }"
                @click="handleDownload(file)"
              >
                {{ isDownloading(file.object) ? '处理中...' : '下载' }}
              </view>
              <view
                class="row-btn share-btn"
                :class="{ disabled: file.syncing }"
                @click="handleShare(file)"
              >
                分享
              </view>
              <view
                class="row-btn danger"
                :class="{ disabled: isDeleting(file.object) }"
                @click="handleDelete(file)"
              >
                {{ isDeleting(file.object) ? '删除中...' : '删除' }}
              </view>
            </view>
          </view>

          <view v-if="hasMore" class="load-more">
            <view class="toolbar-btn light" :class="{ disabled: loadingMore }" @click="handleLoadMore">
              {{ loadingMore ? '加载中...' : '加载更多' }}
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <CloudFilePreviewer
      :visible="previewVisible"
      :loading="previewLoading"
      :file="previewFile"
      :state="previewState"
      @close="closePreview"
      @download="downloadCurrentFile"
      @open-system="previewCurrentFileInSystem"
    />

    <!-- Share Dialog -->
    <view v-if="shareVisible" class="share-overlay" @click.self="handleCloseShare">
      <view class="share-dialog">
        <view class="share-header">
          <view class="share-title">分享文件</view>
          <view class="share-close" @click="handleCloseShare">×</view>
        </view>

        <view class="share-body">
          <view class="share-file-info">
            <view class="share-file-name">{{ shareFile?.name }}</view>
            <view class="share-file-meta">{{ shareFile ? formatFileSize(shareFile.size) : '' }}</view>
          </view>

          <!-- Created share record -->
          <view v-if="shareRecord" class="share-result">
            <view class="share-url-row">
              <view class="share-url-label">分享链接</view>
              <view class="share-url-value">{{ shareRecord.shareUrl }}</view>
            </view>
            <view v-if="shareRecord.password" class="share-url-row">
              <view class="share-url-label">提取密码</view>
              <view class="share-url-value">{{ shareRecord.password }}</view>
            </view>
            <view class="share-url-row">
              <view class="share-url-label">失效时间</view>
              <view class="share-url-value">{{ shareRecord.expireTime }}</view>
            </view>
            <view class="share-actions">
              <view class="share-action-btn" @click="copyShareUrl(shareRecord)">复制链接</view>
            </view>
          </view>

          <!-- Share list for current file -->
          <view v-if="fileShareList.length" class="share-list-section">
            <view class="share-list-title">已创建的分享</view>
            <view
              v-for="item in fileShareList"
              :key="item.id"
              class="share-list-item"
            >
              <view class="share-list-info">
                <view class="share-list-url">{{ item.shareUrl }}</view>
                <view v-if="item.password" class="share-list-code">提取码: {{ item.password }}</view>
                <view class="share-list-meta">
                  {{ item.expireTime ? '至 ' + item.expireTime : '永久' }} · {{ item.downloadCount || 0 }}次下载
                </view>
              </view>
              <view class="share-list-ops">
                <view class="share-copy-btn" @click="copyShareUrl(item)">复制</view>
                <view class="share-cancel-btn" @click="cancelShare(item.id)">取消</view>
              </view>
            </view>
          </view>

          <!-- Create share form -->
          <view v-if="!shareRecord" class="share-form">
            <view class="share-field">
              <view class="share-field-label">有效期</view>
              <view class="share-field-options">
                <view
                  v-for="opt in expireOptions"
                  :key="opt.value"
                  class="share-option"
                  :class="{ active: shareExpireDays === opt.value }"
                  @click="shareExpireDays = opt.value"
                >
                  {{ opt.label }}
                </view>
              </view>
            </view>
            <view class="share-field">
              <view class="share-field-label">提取密码（可选）</view>
              <input
                v-model="sharePassword"
                class="share-input"
                placeholder="不设置则无需密码"
                maxlength="12"
              />
            </view>
            <view class="share-submit">
              <view class="share-submit-btn" :class="{ disabled: shareLoading }" @click="handleCreateShare">
                {{ shareLoading ? '创建中...' : '创建分享' }}
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import CloudFilePreviewer from './CloudFilePreviewer.vue'
import { useCloudDrive } from '@/composables/useCloudDrive'

const {
  quota,
  filterTabs,
  files,
  used,
  hasMore,
  loading,
  loadingMore,
  refreshing,
  uploading,
  importing,
  keyword,
  activeFilter,
  previewVisible,
  previewLoading,
  previewFile,
  previewState,
  isBusy,
  syncLabel,
  displayFiles,
  formatFileSize,
  formatTime,
  isDeleting,
  isDownloading,
  loadFiles,
  refreshInBackground,
  chooseAndUpload,
  openImportPrompt,
  downloadFile,
  deleteFile,
  openPreview,
  closePreview,
  previewCurrentFileInSystem,
  downloadCurrentFile,
  previewButtonLabel,
  shareVisible,
  shareLoading,
  shareFile,
  shareRecord,
  shareList,
  shareExpireDays,
  sharePassword,
  openShareDialog,
  closeShareDialog,
  createShare,
  loadShareList,
  cancelShare,
  copyShareUrl,
} = useCloudDrive()

const expireOptions = [
  { label: '7天', value: 7 },
  { label: '30天', value: 30 },
  { label: '永久', value: 365 },
]

const fileShareList = computed(() => {
  if (!shareFile.value?.object) return []
  return shareList.value.filter((item) => item.fileId === shareFile.value.object)
})

const FILTER_LABELS = {
  all: '全部',
  image: '图片',
  video: '视频',
  document: '文档',
}

const displayFilterTabs = computed(() =>
  filterTabs.map((item) => ({
    ...item,
    label: FILTER_LABELS[item.key] || item.label,
  })),
)

const sortedFiles = computed(() =>
  [...files.value].sort((left, right) => new Date(right.modifyTime || 0).getTime() - new Date(left.modifyTime || 0).getTime()),
)

const recentFiles = computed(() => sortedFiles.value.slice(0, 4))
const previewableCount = computed(() => files.value.filter((item) => item.previewable).length)
const streamableCount = computed(() => files.value.filter((item) => item.streamable).length)
const latestFile = computed(() => sortedFiles.value[0] || null)
const latestActivityLabel = computed(() => (latestFile.value?.modifyTime ? formatTime(latestFile.value.modifyTime) : '暂无记录'))
const resultLabel = computed(() => `已加载 ${displayFiles.value.length} / ${files.value.length} 个文件`)
const emptyTitle = computed(() => (keyword.value.trim() ? '没有匹配的文件' : '云盘还是空的'))
const emptyText = computed(() =>
  keyword.value.trim()
    ? '换个关键词试试，或者切换上方分类。'
    : '先上传一个文件，我们会自动准备在线预览、流媒体和下载入口。',
)

const storagePercent = computed(() => {
  if (!quota) {
    return 0
  }
  const percent = (Number(used.value || 0) / Number(quota || 1)) * 100
  return Math.max(0, Math.min(100, Number(percent.toFixed(2))))
})

const storagePercentLabel = computed(() => {
  if (!used.value) {
    return '0%'
  }
  if (storagePercent.value < 1) {
    return '<1%'
  }
  return `${storagePercent.value.toFixed(storagePercent.value < 10 ? 1 : 0)}%`
})

const toolCards = computed(() => [
  {
    key: 'preview',
    short: '文',
    tone: 'blue',
    name: '文档预览',
    desc: `${previewableCount.value} 项可在线查看`,
    action: () => {
      activeFilter.value = 'document'
    },
  },
  {
    key: 'stream',
    short: '播',
    tone: 'violet',
    name: '流媒体播放',
    desc: `${streamableCount.value} 个媒体文件`,
    action: () => {
      activeFilter.value = 'video'
    },
  },
  {
    key: 'import',
    short: '链',
    tone: 'cyan',
    name: '链接导入',
    desc: '导入远程资源',
    action: () => {
      if (!importing.value) {
        openImportPrompt()
      }
    },
  },
  {
    key: 'upload',
    short: '传',
    tone: 'emerald',
    name: '本地上传',
    desc: '快速入库',
    action: () => {
      if (!uploading.value) {
        chooseAndUpload()
      }
    },
  },
])

const extensionLabel = (file) => String(file?.extension || file?.iconText || 'FILE').toUpperCase().slice(0, 6)

const displayTypeLabel = (file) => {
  const ext = extensionLabel(file)

  if (file?.previewMode === 'pdf') return 'PDF 文档'
  if (file?.previewMode === 'image') return ext ? `${ext} 图片` : '图片'
  if (file?.previewMode === 'video') return ext ? `${ext} 视频` : '视频'
  if (file?.previewMode === 'audio') return ext ? `${ext} 音频` : '音频'
  if (file?.previewMode === 'text') return ext ? `${ext} 文本` : '文本'
  if (file?.previewMode === 'document') {
    if (ext === 'DOC' || ext === 'DOCX') return 'Word 文档'
    if (ext === 'XLS' || ext === 'XLSX') return 'Excel 表格'
    if (ext === 'PPT' || ext === 'PPTX') return 'PPT 演示'
    return `${ext || 'Office'} 文档`
  }

  if (file?.categoryLabel) {
    return file.categoryLabel
  }

  if (file?.contentType && file.contentType !== 'application/octet-stream') {
    return file.contentType
  }

  return '文件'
}

const handleUpload = () => {
  if (uploading.value) {
    return
  }
  chooseAndUpload()
}

const handleImport = () => {
  if (importing.value) {
    return
  }
  openImportPrompt()
}

const handleRefresh = () => {
  if (refreshing.value) {
    return
  }
  refreshInBackground()
}

const handleLoadMore = () => {
  if (loadingMore.value) {
    return
  }
  loadFiles({ reset: false })
}

const handlePreview = (file) => {
  if (!file || !file.previewable || file.syncing || previewLoading.value) {
    return
  }
  openPreview(file)
}

const handleDownload = (file) => {
  if (!file || file.syncing || isDownloading(file.object)) {
    return
  }
  downloadFile(file)
}

const handleDelete = (file) => {
  if (!file || isDeleting(file.object)) {
    return
  }
  deleteFile(file)
}

const handleShare = (file) => {
  if (!file || file.syncing) {
    return
  }
  openShareDialog(file)
}

const handleCloseShare = () => {
  closeShareDialog()
}

const handleCreateShare = () => {
  createShare()
}

const runTool = (tool) => {
  if (typeof tool?.action === 'function') {
    tool.action()
  }
}
</script>

<style scoped>
.cloud-drive,
.cloud-drive view,
.cloud-drive input {
  box-sizing: border-box;
}

.cloud-drive {
  height: 100%;
  min-height: 0;
  padding: 16rpx 18rpx;
  font-family: 'HarmonyOS Sans SC', 'Source Han Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background:
    radial-gradient(circle at top right, rgba(86, 152, 255, 0.14), transparent 28%),
    linear-gradient(180deg, #f7faff 0%, #f2f6fc 100%);
}

.drive-shell {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.header-card,
.overview-card,
.library-card {
  background: #ffffff;
  border: 1rpx solid rgba(223, 232, 244, 0.92);
  border-radius: 18rpx;
  box-shadow: 0 10rpx 28rpx rgba(31, 70, 134, 0.045);
}

.header-card {
  padding: 16rpx 18rpx 14rpx;
}

.header-main {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.header-copy {
  width: 312rpx;
  flex-shrink: 0;
}

.header-title-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.header-title {
  font-size: 36rpx;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #17283f;
}

.sync-badge {
  height: 38rpx;
  padding: 0 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  background: #eff4fb;
  color: #698099;
  font-size: 18rpx;
  white-space: nowrap;
}

.sync-badge.busy {
  background: #e9f2ff;
  color: #2f7df6;
}

.header-desc {
  margin-top: 8rpx;
  font-size: 19rpx;
  line-height: 1.65;
  color: #75869b;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.header-search {
  flex: 1;
  min-width: 0;
}

.search-box {
  width: 100%;
  height: 68rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 0 18rpx;
  border-radius: 16rpx;
  background: linear-gradient(180deg, #f8fbff 0%, #f4f7fc 100%);
  border: 1rpx solid #e6edf7;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.search-box:focus-within {
  border-color: rgba(47, 125, 246, 0.32);
  box-shadow: 0 0 0 6rpx rgba(47, 125, 246, 0.06);
}

.search-icon {
  width: 24rpx;
  height: 24rpx;
  border: 3rpx solid #8a9ab0;
  border-radius: 50%;
  position: relative;
  flex-shrink: 0;
}

.search-icon::after {
  content: '';
  position: absolute;
  right: -8rpx;
  bottom: -7rpx;
  width: 10rpx;
  height: 3rpx;
  border-radius: 999rpx;
  background: #8a9ab0;
  transform: rotate(45deg);
}

.search-input {
  flex: 1;
  width: 100%;
  height: 68rpx;
  font-size: 23rpx;
  color: #1d2e45;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10rpx;
  flex-shrink: 0;
}

.top-btn,
.toolbar-btn,
.row-btn {
  height: 50rpx;
  padding: 0 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx;
  font-size: 20rpx;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  user-select: none;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease, color 0.18s ease;
}

.top-btn.primary,
.toolbar-btn.primary {
  color: #ffffff;
  background: linear-gradient(135deg, #2f7df6 0%, #5a98ff 100%);
  box-shadow: 0 10rpx 20rpx rgba(47, 125, 246, 0.16);
}

.top-btn.ghost,
.toolbar-btn.light,
.row-btn.ghost {
  color: #43607d;
  background: #f6f8fc;
  border: 1rpx solid #e8eef6;
}

.row-btn.danger {
  color: #f05d63;
  background: #fff3f3;
  border: 1rpx solid #ffe6e7;
}

.row-btn.share-btn {
  color: #2f7df6;
  background: #eaf2ff;
  border: 1rpx solid #d9ebff;
}

.disabled {
  opacity: 0.45;
  cursor: default;
  pointer-events: none;
}

.header-meta-row {
  display: grid;
  grid-template-columns: 1.22fr 0.92fr 0.92fr 0.78fr 0.78fr;
  gap: 12rpx;
  margin-top: 14rpx;
}

.capacity-card,
.meta-pill {
  min-width: 0;
  border-radius: 15rpx;
  background: linear-gradient(180deg, #fbfdff 0%, #f7faff 100%);
  border: 1rpx solid #edf2f8;
}

.capacity-card {
  width: auto;
  padding: 13rpx 15rpx;
}

.capacity-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10rpx;
}

.capacity-label,
.meta-pill-label {
  font-size: 18rpx;
  color: #8a9ab0;
}

.capacity-percent {
  font-size: 18rpx;
  font-weight: 700;
  color: #2f7df6;
}

.capacity-value,
.meta-pill-value {
  margin-top: 7rpx;
  font-size: 24rpx;
  font-weight: 700;
  color: #1d2e45;
}

.capacity-bar {
  margin-top: 10rpx;
  height: 7rpx;
  border-radius: 999rpx;
  overflow: hidden;
  background: #e8eef7;
}

.capacity-fill {
  height: 100%;
  min-width: 10rpx;
  border-radius: inherit;
  background: linear-gradient(90deg, #2f7df6 0%, #66adff 100%);
}

.meta-pill {
  flex: 1;
  padding: 13rpx 14rpx;
}

.overview-card {
  display: flex;
  align-items: stretch;
  gap: 0;
  padding: 14rpx 16rpx;
}

.overview-column {
  min-width: 0;
}

.recent-column {
  flex: 1.08;
}

.tools-column {
  flex: 0.92;
}

.overview-divider {
  width: 1rpx;
  margin: 0 16rpx;
  background: linear-gradient(180deg, transparent 0%, #edf2f8 12%, #edf2f8 88%, transparent 100%);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-bottom: 12rpx;
}

.section-title {
  font-size: 26rpx;
  font-weight: 700;
  color: #1c2d44;
}

.section-link {
  font-size: 18rpx;
  color: #8ea0b5;
}

.recent-grid {
  display: flex;
  gap: 10rpx;
}

.recent-tile {
  flex: 1;
  min-width: 0;
  padding: 14rpx 12rpx;
  border-radius: 16rpx;
  background: linear-gradient(180deg, #fbfdff 0%, #f7faff 100%);
  border: 1rpx solid #edf2f8;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.recent-thumb {
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.file-mark {
  width: 58rpx;
  height: 58rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 18rpx;
}

.file-mark-text {
  font-size: 17rpx;
  font-weight: 700;
}

.recent-name,
.tool-name,
.file-name {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.recent-name {
  margin-top: 14rpx;
  font-size: 20rpx;
  font-weight: 600;
  color: #23344c;
}

.recent-sub,
.tool-desc,
.file-note,
.size-col,
.type-col,
.time-col,
.toolbar-summary {
  font-size: 18rpx;
  color: #8394a8;
}

.recent-sub {
  margin-top: 6rpx;
}

.tool-row {
  display: flex;
  gap: 10rpx;
}

.tool-tile {
  flex: 1;
  min-width: 0;
  padding: 14rpx 12rpx;
  border-radius: 16rpx;
  background: linear-gradient(180deg, #fbfdff 0%, #f7faff 100%);
  border: 1rpx solid #edf2f8;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.tool-icon {
  width: 54rpx;
  height: 54rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 15rpx;
  font-size: 20rpx;
  font-weight: 700;
}

.tool-icon.tone-blue {
  color: #2f7df6;
  background: #eaf2ff;
}

.tool-icon.tone-violet {
  color: #7a52ef;
  background: #f1ebff;
}

.tool-icon.tone-cyan {
  color: #1d9fcc;
  background: #e8f8ff;
}

.tool-icon.tone-emerald {
  color: #20a66c;
  background: #e9fbf3;
}

.tool-name {
  margin-top: 12rpx;
  font-size: 20rpx;
  font-weight: 600;
  color: #23344c;
}

.tool-desc {
  margin-top: 6rpx;
}

.empty-panel,
.state-shell {
  padding: 38rpx 28rpx;
  text-align: center;
}

.empty-title {
  font-size: 26rpx;
  font-weight: 700;
  color: #223348;
}

.empty-text {
  margin-top: 10rpx;
  font-size: 20rpx;
  line-height: 1.7;
  color: #8d9caf;
}

.library-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.library-tabs {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 0 18rpx;
  height: 62rpx;
  border-bottom: 1rpx solid #edf2f8;
}

.tab-item {
  position: relative;
  height: 62rpx;
  display: flex;
  align-items: center;
  font-size: 22rpx;
  color: #586d85;
  cursor: pointer;
}

.tab-item.active {
  color: #2f7df6;
  font-weight: 700;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 6rpx;
  border-radius: 999rpx;
  background: #2f7df6;
}

.library-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 12rpx 18rpx;
  border-bottom: 1rpx solid #edf2f8;
}

.toolbar-left,
.actions-col,
.file-copy-top {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.table-head,
.table-row {
  display: flex;
  align-items: center;
}

.table-head {
  height: 56rpx;
  padding: 0 18rpx;
  font-size: 18rpx;
  color: #8ea0b4;
  background: #fbfcfe;
  border-bottom: 1rpx solid #edf2f8;
}

.table-scroll {
  flex: 1;
  min-height: 0;
  background: #ffffff;
}

.table-row {
  min-height: 82rpx;
  padding: 0 18rpx;
  border-bottom: 1rpx solid #f1f4f8;
  transition: background 0.18s ease;
}

.table-row.clickable {
  cursor: pointer;
}

.col-name,
.name-col {
  flex: 1;
  min-width: 0;
}

.name-col {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 12rpx 0;
}

.file-copy {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 22rpx;
  font-weight: 600;
  color: #21324a;
}

.file-note {
  margin-top: 2rpx;
}

.file-chip {
  height: 28rpx;
  padding: 0 10rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 999rpx;
  font-size: 17rpx;
  color: #63778f;
  background: #eff4fa;
}

.file-chip.syncing {
  color: #2f7df6;
  background: #e8f0ff;
}

.size-col,
.col-size {
  width: 132rpx;
  flex-shrink: 0;
}

.type-col,
.col-type {
  width: 148rpx;
  flex-shrink: 0;
}

.time-col,
.col-time {
  width: 148rpx;
  flex-shrink: 0;
}

.actions-col,
.col-actions {
  width: 270rpx;
  flex-shrink: 0;
  justify-content: flex-end;
}

.actions-col {
  gap: 8rpx;
}

.row-btn {
  height: 42rpx;
  padding: 0 14rpx;
  border-radius: 12rpx;
  font-size: 18rpx;
}

.type-col {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 24rpx 0 30rpx;
}

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

@media (hover: hover) {
  .top-btn:not(.disabled):hover,
  .toolbar-btn:not(.disabled):hover,
  .row-btn:not(.disabled):hover,
  .recent-tile:hover,
  .tool-tile:hover {
    transform: translateY(-2rpx);
  }

  .recent-tile:hover,
  .tool-tile:hover {
    border-color: #dfe8f5;
    box-shadow: 0 10rpx 22rpx rgba(31, 76, 152, 0.07);
  }

  .table-row.clickable:hover {
    background: linear-gradient(90deg, #fbfdff 0%, #f7fbff 100%);
  }
}

@media (max-width: 1380px) {
  .header-main,
  .overview-card,
  .library-toolbar {
    flex-wrap: wrap;
  }

  .header-meta-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .capacity-card {
    grid-column: 1 / -1;
  }

  .header-copy,
  .header-search,
  .header-actions,
  .meta-pill,
  .overview-column,
  .toolbar-left,
  .toolbar-right {
    width: 100%;
  }

  .overview-divider {
    display: none;
  }
}

@media (max-width: 980px) {
  .header-meta-row {
    grid-template-columns: 1fr;
  }

  .capacity-card {
    grid-column: auto;
  }

  .recent-grid,
  .tool-row {
    flex-wrap: wrap;
  }

  .recent-tile,
  .tool-tile {
    min-width: calc(50% - 6rpx);
  }

  .type-col,
  .col-type {
    display: none;
  }
}

@media (max-width: 768px) {
  .cloud-drive {
    padding: 14rpx;
  }

  .recent-tile,
  .tool-tile,
  .capacity-card,
  .meta-pill {
    min-width: 100%;
    width: 100%;
  }

  .library-tabs {
    gap: 18rpx;
    overflow-x: auto;
    white-space: nowrap;
  }

  .table-head {
    display: none;
  }

  .table-row {
    flex-wrap: wrap;
    padding-top: 14rpx;
    padding-bottom: 14rpx;
  }

  .name-col {
    width: 100%;
    padding: 0;
  }

  .size-col,
  .time-col,
  .actions-col {
    width: auto;
  }

  .actions-col {
    width: 100%;
    justify-content: flex-start;
    padding-left: 74rpx;
    flex-wrap: wrap;
  }
}

.share-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
}

.share-dialog {
  width: 100%;
  max-width: 580rpx;
  background: #ffffff;
  border-radius: 22rpx;
  box-shadow: 0 24rpx 60rpx rgba(0, 0, 0, 0.18);
  overflow: hidden;
}

.share-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx 22rpx;
  border-bottom: 1rpx solid #edf2f8;
}

.share-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #17283f;
}

.share-close {
  width: 44rpx;
  height: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  color: #8a9ab0;
  cursor: pointer;
}

.share-body {
  padding: 28rpx 32rpx;
}

.share-file-info {
  padding: 20rpx 22rpx;
  background: linear-gradient(180deg, #fbfdff 0%, #f7faff 100%);
  border: 1rpx solid #edf2f8;
  border-radius: 14rpx;
  margin-bottom: 24rpx;
}

.share-file-name {
  font-size: 24rpx;
  font-weight: 600;
  color: #1d2e45;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.share-file-meta {
  font-size: 19rpx;
  color: #8a9ab0;
  margin-top: 6rpx;
}

.share-form {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
}

.share-field {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.share-field-label {
  font-size: 21rpx;
  color: #586d85;
  font-weight: 500;
}

.share-field-options {
  display: flex;
  gap: 12rpx;
}

.share-option {
  flex: 1;
  height: 58rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx;
  font-size: 21rpx;
  color: #43607d;
  background: #f6f8fc;
  border: 1rpx solid #e8eef6;
  cursor: pointer;
  transition: all 0.15s ease;
}

.share-option.active {
  color: #2f7df6;
  background: #eaf2ff;
  border-color: #2f7df6;
  font-weight: 700;
}

.share-input {
  width: 100%;
  height: 68rpx;
  padding: 0 18rpx;
  border-radius: 12rpx;
  border: 1rpx solid #e6edf7;
  background: linear-gradient(180deg, #f8fbff 0%, #f4f7fc 100%);
  font-size: 23rpx;
  color: #1d2e45;
  outline: none;
}

.share-input:focus {
  border-color: rgba(47, 125, 246, 0.32);
  box-shadow: 0 0 0 6rpx rgba(47, 125, 246, 0.06);
}

.share-submit {
  margin-top: 8rpx;
}

.share-submit-btn {
  width: 100%;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14rpx;
  font-size: 25rpx;
  font-weight: 700;
  color: #ffffff;
  background: linear-gradient(135deg, #2f7df6 0%, #5a98ff 100%);
  box-shadow: 0 10rpx 20rpx rgba(47, 125, 246, 0.22);
  cursor: pointer;
}

.share-submit-btn.disabled {
  opacity: 0.45;
  cursor: default;
}

.share-result {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.share-url-row {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
  padding: 16rpx 18rpx;
  background: linear-gradient(180deg, #fbfdff 0%, #f7faff 100%);
  border: 1rpx solid #edf2f8;
  border-radius: 12rpx;
}

.share-url-label {
  flex-shrink: 0;
  font-size: 20rpx;
  color: #8a9ab0;
  width: 120rpx;
}

.share-url-value {
  flex: 1;
  min-width: 0;
  font-size: 20rpx;
  color: #1d2e45;
  word-break: break-all;
}

.share-actions {
  display: flex;
  gap: 14rpx;
  margin-top: 8rpx;
}

.share-action-btn {
  flex: 1;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx;
  font-size: 22rpx;
  font-weight: 600;
  color: #2f7df6;
  background: #eaf2ff;
  border: 1rpx solid #d9ebff;
  cursor: pointer;
}

.share-list-section {
  margin-top: 16rpx;
  border-top: 1rpx solid #edf2f8;
  padding-top: 20rpx;
}

.share-list-title {
  font-size: 21rpx;
  font-weight: 600;
  color: #586d85;
  margin-bottom: 14rpx;
}

.share-list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  padding: 14rpx 16rpx;
  background: #f8fbff;
  border: 1rpx solid #edf2f8;
  border-radius: 12rpx;
  margin-bottom: 10rpx;
}

.share-list-info {
  flex: 1;
  min-width: 0;
}

.share-list-url {
  font-size: 19rpx;
  color: #1d2e45;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.share-list-code {
  font-size: 18rpx;
  color: #2f7df6;
  font-weight: 600;
  margin-top: 4rpx;
}

.share-list-meta {
  font-size: 17rpx;
  color: #8a9ab0;
  margin-top: 4rpx;
}

.share-list-ops {
  display: flex;
  gap: 8rpx;
  flex-shrink: 0;
}

.share-copy-btn,
.share-cancel-btn {
  height: 46rpx;
  padding: 0 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10rpx;
  font-size: 18rpx;
  font-weight: 600;
  cursor: pointer;
}

.share-copy-btn {
  color: #2f7df6;
  background: #eaf2ff;
  border: 1rpx solid #d9ebff;
}

.share-cancel-btn {
  color: #f05d63;
  background: #fff3f3;
  border: 1rpx solid #ffe6e7;
}
</style>
