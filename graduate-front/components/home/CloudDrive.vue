<template>
  <view class="cloud-drive-main">
    <!-- 标签栏 + 搜索 + 工具栏 -->
    <view class="cloud-drive-header-section">
      <!-- 标签栏 -->
      <view class="cloud-drive-tabs">
        <view class="tab-item" :class="{ active: activeTab === 'all' }" @click="activeTab = 'all'">全部</view>
        <view class="tab-item" :class="{ active: activeTab === 'recent' }" @click="activeTab = 'recent'">最近</view>
        <view class="tab-item" :class="{ active: activeTab === 'images' }" @click="activeTab = 'images'">图片</view>
        <view class="tab-item" :class="{ active: activeTab === 'videos' }" @click="activeTab = 'videos'">视频</view>
        <view class="tab-item" :class="{ active: activeTab === 'docs' }" @click="activeTab = 'docs'">文档</view>
      </view>

      <!-- 搜索框 + 工具栏 -->
      <view class="cloud-drive-toolbar-section">
        <view class="cloud-drive-search">
          <text class="search-icon">🔍</text>
          <input
            type="text"
            class="search-input"
            placeholder="搜索文件"
            v-model="searchText"
            @input="filterFilesBySearch"
          />
          <text v-if="searchText" class="search-clear" @click="clearSearch">✕</text>
        </view>

        <view class="cloud-drive-actions">
          <button class="action-btn" @click="chooseAndUpload">📤 上传</button>
          <button class="action-btn" @click="loadFiles">🔄 刷新</button>
        </view>
      </view>

      <!-- 配额信息 -->
      <view class="quota-info">
        <text class="quota-text">已使用 {{ usedReadable }} / {{ quotaReadable }}</text>
        <view class="quota-bar">
          <view class="quota-fill" :style="{ width: percent + '%' }"></view>
        </view>
      </view>
    </view>

    <!-- 文件列表 -->
    <view class="cloud-drive-content">
      <view class="file-header">
        <view class="col name">文件名</view>
        <view class="col size">大小</view>
        <view class="col time">修改时间</view>
        <view class="col actions">操作</view>
      </view>

      <view v-if="loading" class="file-loading">
        <text>加载中…</text>
      </view>
      <view v-else-if="displayFiles.length === 0" class="file-empty">
        <text>{{ searchText ? '未找到相关文件' : '网盘为空' }}</text>
      </view>
      <view v-else>
        <view v-for="f in displayFiles" :key="f.name" class="file-item">
          <view class="col name">
            <text class="file-icon">{{ getFileIcon(f.name) }}</text>
            <text class="file-name">{{ f.name }}</text>
          </view>
          <view class="col size"><text>{{ f.sizeReadable }}</text></view>
          <view class="col time"><text>{{ formatTime(f.modifyTime) }}</text></view>
          <view class="col actions">
            <button class="item-btn" @click="downloadFile(f)">下载</button>
            <button class="item-btn" @click="deleteFile(f)">删除</button>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

    <script>
import service from '@/utils/request'

export default {
  name: 'CloudDrive',
  props: {
    quota: { type: Number, default: 10 * 1024 * 1024 * 1024 }
  },
  data() {
    return {
      files: [],
      used: 0,
      loading: false,
      activeTab: 'all',
      searchText: ''
    }
  },
  computed: {
    percent() { return Math.min(100, Math.round((this.used / this.quota) * 100)) },
    usedReadable() { return this.readableSize(this.used) },
    quotaReadable() { return this.readableSize(this.quota) },
    filteredFiles() {
      if (this.activeTab === 'all') return this.files
      if (this.activeTab === 'recent') {
        return this.files.slice().sort((a, b) => {
          const aTime = new Date(a.modifyTime || 0).getTime()
          const bTime = new Date(b.modifyTime || 0).getTime()
          return bTime - aTime
        })
      }
      if (this.activeTab === 'images') return this.files.filter(f => /\.(jpg|jpeg|png|gif|webp|bmp)$/i.test(f.name))
      if (this.activeTab === 'videos') return this.files.filter(f => /\.(mp4|avi|mkv|mov|flv|wmv)$/i.test(f.name))
      if (this.activeTab === 'docs') return this.files.filter(f => /\.(pdf|doc|docx|xls|xlsx|ppt|pptx|txt)$/i.test(f.name))
      return this.files
    },
    displayFiles() {
      if (!this.searchText.trim()) return this.filteredFiles
      const keyword = this.searchText.toLowerCase()
      return this.filteredFiles.filter(f => f.name.toLowerCase().includes(keyword))
    }
  },
  methods: {
    readableSize(bytes) {
      if (!bytes) return '0 B'
      const units = ['B','KB','MB','GB','TB']
      let i = 0
      let v = bytes
      while (v >= 1024 && i < units.length -1) { v /= 1024; i++ }
      return `${v.toFixed(2)} ${units[i]}`
    },
    getFileIcon(fileName) {
      if (!fileName) return '📄'
      const lower = fileName.toLowerCase()
      if (/\.(jpg|jpeg|png|gif|webp|bmp)$/i.test(lower)) return '🖼️'
      if (/\.(mp4|avi|mkv|mov|flv|wmv)$/i.test(lower)) return '🎬'
      if (/\.(pdf)$/i.test(lower)) return '📕'
      if (/\.(doc|docx)$/i.test(lower)) return '📝'
      if (/\.(xls|xlsx)$/i.test(lower)) return '📊'
      if (/\.(ppt|pptx)$/i.test(lower)) return '🎯'
      if (/\.(zip|rar|7z|tar|gz)$/i.test(lower)) return '📦'
      return '📄'
    },
    formatTime(timeStr) {
      if (!timeStr) return '-'
      try {
        const d = new Date(timeStr)
        return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN').slice(0,5)
      } catch {
        return timeStr
      }
    },
    filterFilesBySearch() {
      // 搜索过滤由 computed displayFiles 自动处理
    },
    clearSearch() {
      this.searchText = ''
    },
    async loadFiles() {
      this.loading = true
      try {
        const res = await service.get('/cloud/list')
        if (res && res.code === 200) {
          this.files = (res.data.files || []).map(f => ({
            ...f,
            sizeReadable: this.readableSize(f.size),
            modifyTime: f.modifyTime || ''
          }))
          this.used = res.data.used || 0
        } else {
          this.files = []
          this.used = 0
        }
      } catch (e) {
        console.error('加载网盘文件失败', e)
        uni.showToast({ title: '加载网盘失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    async downloadFile(f) {
      if (!f || !f.name) return
      try {
        const res = await service.get('/cloud/download', { params: { object: f.object || f.name } })
        if (res && res.code === 200 && res.data) {
          window.open(res.data)
        }
      } catch (e) {
        console.error('获取下载链接失败', e)
        uni.showToast({ title: '下载失败', icon: 'none' })
      }
    },
    async deleteFile(f) {
      if (!f || !f.name) return
      const ok = confirm('确定删除 ' + f.name + ' ?')
      if (!ok) return
      try {
        const res = await service.post('/cloud/delete', { object: f.object || f.name })
        if (res && res.code === 200) {
          uni.showToast({ title: '删除成功', icon: 'success' })
          this.loadFiles()
        } else {
          uni.showToast({ title: res.message || '删除失败', icon: 'none' })
        }
      } catch (e) {
        console.error('删除失败', e)
        uni.showToast({ title: '删除失败', icon: 'none' })
      }
    },
    chooseAndUpload() {
      // #ifdef H5
      const input = document.createElement('input')
      input.type = 'file'
      input.onchange = (e) => {
        const file = e.target.files[0]
        if (!file) return
        this.uploadByFile(file)
      }
      input.click()
      // #endif

      // #ifndef H5
      uni.chooseMessageFile({
        count: 1,
        type: 'file',
        success: (res) => {
          const tempFile = res.tempFiles && res.tempFiles[0]
          if (tempFile) this.uploadByPath(tempFile.path, tempFile.name)
        },
        fail: () => uni.showToast({ title: '选择文件失败', icon: 'none' })
      })
      // #endif
    },
    async uploadByFile(file) {
      try {
        const fd = new FormData()
        fd.append('file', file, file.name)
        const res = await service.post('/cloud/upload', fd)
        if (res && res.code === 200) {
          uni.showToast({ title: '上传成功', icon: 'success' })
          this.loadFiles()
        } else {
          uni.showToast({ title: res.message || '上传失败', icon: 'none' })
        }
      } catch (e) {
        console.error('上传失败', e)
        uni.showToast({ title: '上传失败', icon: 'none' })
      }
    },
    uploadByPath(path, name) {
      uni.uploadFile({
        url: (uni.getStorageSync('baseUrl') || '') + '/cloud/upload',
        filePath: path,
        name: 'file',
        formData: { fileName: name },
        success: (uploadRes) => {
          try {
            const d = JSON.parse(uploadRes.data)
            if (d && d.code === 200) {
              uni.showToast({ title: '上传成功', icon: 'success' })
              this.loadFiles()
            } else {
              uni.showToast({ title: d.message || '上传失败', icon: 'none' })
            }
          } catch (e) {
            uni.showToast({ title: '上传失败', icon: 'none' })
          }
        },
        fail: () => uni.showToast({ title: '上传失败', icon: 'none' })
      })
    }
  },
  mounted() {
    this.loadFiles()
  }
}
</script>

<style scoped>
.cloud-drive-main { 
  display: flex; 
  flex-direction: column; 
  background: #fff;
  flex: 1;
  min-width: 0;
  min-height: 0;
  position: relative;
}
.cloud-drive-header-section { padding: 12px 16px; border-bottom: 1px solid #f0f0f0; }

.cloud-drive-tabs { display: flex; gap: 16px; padding-bottom: 12px; border-bottom: 1px solid #f0f0f0; }
.tab-item { font-size: 14px; color: #666; cursor: pointer; padding: 6px 0; border-bottom: 2px solid transparent; transition: all 0.3s }
.tab-item.active { color: #1890ff; border-bottom-color: #1890ff }

.cloud-drive-toolbar-section { display: flex; gap: 12px; align-items: center; margin-top: 8px; }
.cloud-drive-search { flex: 1; display: flex; align-items: center; background: #f5f5f5; border-radius: 4px; padding: 6px 12px; }
.search-icon { margin-right: 8px; font-size: 14px; }
.search-input { flex: 1; border: none; background: transparent; outline: none; font-size: 13px; }
.search-clear { cursor: pointer; font-size: 14px; color: #ccc; }

.cloud-drive-actions { display: flex; gap: 8px; }
.action-btn { padding: 6px 12px; border: 1px solid #d9d9d9; background: #fff; border-radius: 4px; cursor: pointer; font-size: 13px; }
.action-btn:hover { background: #f5f5f5 }

.quota-info { margin-top: 12px; }
.quota-text { font-size: 12px; color: #666; }
.quota-bar { height: 6px; background: #f0f0f0; border-radius: 4px; overflow: hidden; margin-top: 6px; }
.quota-fill { height: 100%; background: #1890ff; }

.cloud-drive-content { flex: 1; overflow-y: auto; }
.file-header { display: flex; align-items: center; padding: 12px 16px; background: #fafafa; font-weight: 600; font-size: 13px; border-bottom: 1px solid #f0f0f0; }
.file-header .col { padding: 0 8px; }
.file-header .col.name { flex: 2 }
.file-header .col.size { flex: 1 }
.file-header .col.time { flex: 1.2 }
.file-header .col.actions { flex: 1 }

.file-loading { padding: 32px; text-align: center; color: #888; }
.file-empty { padding: 48px 16px; text-align: center; color: #999; font-size: 13px; }

.file-item { display: flex; align-items: center; padding: 10px 16px; border-bottom: 1px solid #f6f6f6; font-size: 13px; }
.file-item:hover { background: #f9f9f9 }
.file-item .col { padding: 0 8px; display: flex; align-items: center; }
.file-item .col.name { flex: 2 }
.file-item .col.size { flex: 1 }
.file-item .col.time { flex: 1.2; color: #999 }
.file-item .col.actions { flex: 1; display: flex; gap: 8px; }

.file-icon { margin-right: 8px; font-size: 14px; }
.file-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.item-btn { padding: 4px 8px; font-size: 12px; border: 1px solid #d9d9d9; background: #fff; border-radius: 4px; cursor: pointer; }
.item-btn:hover { background: #f5f5f5; border-color: #1890ff; color: #1890ff }
</style>
