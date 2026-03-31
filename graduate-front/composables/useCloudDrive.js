import { computed, onMounted, ref } from 'vue'
import { API_CONFIG } from '@/utils/config'
import { downloadRemoteFileToLocalPath } from '@/utils/file-runtime'
import LocalStateCache from '@/utils/local-state-cache'
import service from '@/utils/request'
import { getUploadErrorMessage, uploadFile, validateFileBeforeUpload } from '@/utils/file-upload'
import { isAppPlusRuntime, supportsBrowserDom } from '@/utils/runtime'
import { formatFileSize, formatTime } from '@/utils/common'

const QUOTA_BYTES = 10 * 1024 * 1024 * 1024
const PAGE_SIZE = 30
const CLOUD_CACHE_KEY = 'cloud_drive_snapshot'
const CLOUD_CACHE_GRACE_MS = 60 * 1000

const IMAGE_EXTENSIONS = new Set(['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg'])
const VIDEO_EXTENSIONS = new Set(['mp4', 'webm', 'mov', 'm4v', 'ogg', 'ogv', 'avi', 'mkv'])
const AUDIO_EXTENSIONS = new Set(['mp3', 'wav', 'ogg', 'oga', 'm4a', 'aac', 'flac'])
const TEXT_EXTENSIONS = new Set([
  'txt',
  'md',
  'markdown',
  'json',
  'js',
  'ts',
  'tsx',
  'jsx',
  'vue',
  'html',
  'htm',
  'css',
  'scss',
  'less',
  'xml',
  'yml',
  'yaml',
  'csv',
  'log',
  'sql',
  'properties',
  'conf',
  'ini',
  'java',
  'kt',
  'py',
  'go',
  'sh',
  'bat',
  'cmd',
])
const DOCUMENT_EXTENSIONS = new Set(['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'])

const FILTER_TABS = [
  { key: 'all', label: '全部' },
  { key: 'image', label: '图片' },
  { key: 'video', label: '视频' },
  { key: 'document', label: '文档' },
]

const PREVIEW_ACTION_LABEL = {
  image: '预览',
  video: '播放',
  audio: '播放',
  pdf: '预览',
  text: '预览',
  document: '预览',
}

const PREVIEW_MODE_LABEL = {
  image: '图片预览',
  video: '视频流',
  audio: '音频播放',
  pdf: 'PDF 预览',
  text: '文本预览',
  document: '文档预览',
  unsupported: '下载查看',
}

const FILE_CATEGORY_LABEL = {
  image: '图片',
  video: '视频',
  audio: '音频',
  document: '文档',
  archive: '压缩包',
  file: '文件',
}

const resolveExtension = (name = '') => {
  const normalizedName = String(name || '').trim()
  const dotIndex = normalizedName.lastIndexOf('.')
  if (dotIndex < 0 || dotIndex === normalizedName.length - 1) {
    return ''
  }
  return normalizedName.slice(dotIndex + 1).toLowerCase()
}

const inferDescriptor = (file = {}) => {
  const extension = resolveExtension(file.name || file.fileName)
  const contentType = String(file.contentType || '').toLowerCase()

  if (extension === 'pdf' || contentType === 'application/pdf') {
    return {
      extension,
      contentType: file.contentType || 'application/pdf',
      category: 'document',
      previewMode: 'pdf',
      previewable: true,
      streamable: false,
      iconType: 'pdf',
      iconText: 'PDF',
    }
  }

  if (IMAGE_EXTENSIONS.has(extension) || contentType.startsWith('image/')) {
    return {
      extension,
      contentType: file.contentType || 'image/*',
      category: 'image',
      previewMode: 'image',
      previewable: true,
      streamable: false,
      iconType: 'image',
      iconText: 'IMG',
    }
  }

  if (VIDEO_EXTENSIONS.has(extension) || contentType.startsWith('video/')) {
    return {
      extension,
      contentType: file.contentType || 'video/*',
      category: 'video',
      previewMode: 'video',
      previewable: true,
      streamable: true,
      iconType: 'video',
      iconText: 'MOV',
    }
  }

  if (AUDIO_EXTENSIONS.has(extension) || contentType.startsWith('audio/')) {
    return {
      extension,
      contentType: file.contentType || 'audio/*',
      category: 'audio',
      previewMode: 'audio',
      previewable: true,
      streamable: true,
      iconType: 'audio',
      iconText: 'AUD',
    }
  }

  if (DOCUMENT_EXTENSIONS.has(extension)) {
    return {
      extension,
      contentType: file.contentType || 'application/octet-stream',
      category: 'document',
      previewMode: extension === 'pdf' ? 'pdf' : TEXT_EXTENSIONS.has(extension) ? 'text' : 'document',
      previewable: true,
      streamable: false,
      iconType: extension === 'pdf' ? 'pdf' : 'doc',
      iconText: extension.toUpperCase().slice(0, 4) || 'DOC',
    }
  }

  if (TEXT_EXTENSIONS.has(extension) || contentType.startsWith('text/')) {
    return {
      extension,
      contentType: file.contentType || 'text/plain',
      category: 'document',
      previewMode: 'text',
      previewable: true,
      streamable: false,
      iconType: 'doc',
      iconText: extension.toUpperCase().slice(0, 4) || 'TXT',
    }
  }

  return {
    extension,
    contentType: file.contentType || 'application/octet-stream',
    category: 'file',
    previewMode: 'unsupported',
    previewable: false,
    streamable: false,
    iconType: 'file',
    iconText: 'FILE',
  }
}

const buildApiUrl = (path = '') => {
  const baseUrl = String(API_CONFIG.BASE_URL || '').replace(/\/+$/, '')
  const normalizedPath = String(path || '')
  if (!normalizedPath) {
    return baseUrl
  }
  if (/^https?:\/\//i.test(normalizedPath)) {
    return normalizedPath
  }
  return `${baseUrl}${normalizedPath.startsWith('/') ? normalizedPath : `/${normalizedPath}`}`
}

const normalizeFile = (file = {}) => {
  const descriptor = inferDescriptor(file)
  const previewMode = file.previewMode || descriptor.previewMode
  const category = file.category || descriptor.category
  const contentType = file.contentType || descriptor.contentType
  const extension = file.extension || descriptor.extension
  const previewable = typeof file.previewable === 'boolean' ? file.previewable : descriptor.previewable
  const streamable = typeof file.streamable === 'boolean' ? file.streamable : descriptor.streamable

  return {
    object: file.object || '',
    name: file.name || '未命名文件',
    size: Number(file.size || 0),
    modifyTime: file.modifyTime || new Date().toISOString(),
    syncing: Boolean(file.syncing),
    contentType,
    extension,
    category,
    previewMode,
    previewable,
    streamable,
    downloadUrl: file.downloadUrl || '',
    previewUrl: file.previewUrl || '',
    streamUrl: file.streamUrl || '',
    iconType: descriptor.iconType,
    iconText: descriptor.iconText,
    previewModeLabel: PREVIEW_MODE_LABEL[previewMode] || '下载查看',
    categoryLabel: FILE_CATEGORY_LABEL[category] || '文件',
  }
}

const mergeUniqueFiles = (baseFiles, appendedFiles) => {
  const merged = new Map()
  ;[...baseFiles, ...appendedFiles].forEach((item) => {
    const normalized = normalizeFile(item)
    if (normalized.object) {
      merged.set(normalized.object, normalized)
    }
  })
  return Array.from(merged.values())
}

const getFileNameFromPath = (value = '') => {
  const normalized = String(value || '').split('?')[0]
  const segments = normalized.split('/').filter(Boolean)
  return segments[segments.length - 1] || `file_${Date.now()}`
}

const createUploadAsset = ({ source, name, size = 0 }) => ({
  uploadSource: source,
  name: name || getFileNameFromPath(typeof source === 'string' ? source : ''),
  size: Number(size || 0),
})

const resolveCloudObjectName = (response = {}) => {
  if (typeof response?.data === 'string' && response.data) return response.data
  if (typeof response?.msg === 'string' && response.msg.startsWith('cloud/')) return response.msg
  if (typeof response?.data?.object === 'string' && response.data.object) return response.data.object
  if (typeof response?.data?.path === 'string' && response.data.path) return response.data.path
  return ''
}

const resolveAssetUrl = (file = {}) => {
  if (file.previewMode === 'video' || file.previewMode === 'audio') {
    return buildApiUrl(file.streamUrl || file.previewUrl || file.downloadUrl)
  }
  return buildApiUrl(file.previewUrl || file.downloadUrl)
}

const resolveDownloadUrl = (file = {}) => buildApiUrl(file.downloadUrl || '')

export function useCloudDrive() {
  const files = ref([])
  const used = ref(0)
  const nextCursor = ref('')
  const hasMore = ref(false)
  const loading = ref(false)
  const loadingMore = ref(false)
  const refreshing = ref(false)
  const uploading = ref(false)
  const importing = ref(false)
  const keyword = ref('')
  const activeFilter = ref('all')
  const lastSyncedAt = ref(null)
  const deletingObjects = ref({})
  const downloadingObjects = ref({})
  const previewVisible = ref(false)
  const previewLoading = ref(false)
  const previewFile = ref(null)
  const previewState = ref({})

  const shareVisible = ref(false)
  const shareLoading = ref(false)
  const shareFile = ref(null)
  const shareRecord = ref(null)
  const shareList = ref([])
  const shareExpireDays = ref(7)
  const sharePassword = ref('')

  let refreshEpoch = 0
  let backgroundRefreshPromise = null
  let optimisticSeed = 0
  let pendingSilentRefresh = false

  const resolveCloudCacheScope = () => {
    try {
      const storedUser = uni.getStorageSync('userInfo') || {}
      const userId = String(storedUser?.id || '')
      if (userId) {
        return `cloud:${userId}`
      }
    } catch {}

    return 'cloud:anonymous'
  }

  const persistSnapshot = () => {
    LocalStateCache.set(resolveCloudCacheScope(), CLOUD_CACHE_KEY, {
      files: files.value,
      used: used.value,
      nextCursor: nextCursor.value,
      hasMore: hasMore.value,
      lastSyncedAt: lastSyncedAt.value,
    })
  }

  const restoreSnapshot = () => {
    const scope = resolveCloudCacheScope()
    const snapshot = LocalStateCache.getValue(scope, CLOUD_CACHE_KEY)
    if (!snapshot || typeof snapshot !== 'object') {
      return { restored: false, isFresh: false }
    }

    files.value = (Array.isArray(snapshot.files) ? snapshot.files : []).map(normalizeFile)
    used.value = Number(snapshot.used || 0)
    nextCursor.value = snapshot.nextCursor || ''
    hasMore.value = Boolean(snapshot.hasMore)
    lastSyncedAt.value = snapshot.lastSyncedAt || null

    const updatedAt = Number(LocalStateCache.get(scope, CLOUD_CACHE_KEY)?.updatedAt || 0)

    return {
      restored: true,
      isFresh: updatedAt > 0 && Date.now() - updatedAt < CLOUD_CACHE_GRACE_MS,
    }
  }

  const usedPercent = computed(() => Math.min(100, Math.round((used.value / QUOTA_BYTES) * 100)))
  const isBusy = computed(() => loading.value || loadingMore.value || refreshing.value || uploading.value || importing.value)
  const syncLabel = computed(() => {
    if (uploading.value) return '上传中'
    if (importing.value) return '导入中'
    if (refreshing.value) return '同步中'
    if (!lastSyncedAt.value) return '待同步'
    return `已同步 ${formatTime(lastSyncedAt.value)}`
  })

  const filteredFiles = computed(() => {
    const query = keyword.value.trim().toLowerCase()
    return files.value.filter((item) => {
      if (activeFilter.value !== 'all' && item.category !== activeFilter.value) {
        return false
      }
      if (!query) {
        return true
      }
      return String(item.name || '').toLowerCase().includes(query)
    })
  })

  const displayFiles = computed(() =>
    [...filteredFiles.value].sort((left, right) => new Date(right.modifyTime || 0).getTime() - new Date(left.modifyTime || 0).getTime()),
  )

  const setActionState = (target, key, value) => {
    target.value = {
      ...target.value,
      [key]: value,
    }
  }

  const isDeleting = (object) => Boolean(deletingObjects.value[object])
  const isDownloading = (object) => Boolean(downloadingObjects.value[object])

  const applyPage = (page, reset) => {
    const normalizedFiles = (page.files || []).map(normalizeFile)
    files.value = reset ? normalizedFiles : mergeUniqueFiles(files.value, normalizedFiles)
    used.value = Number(page.used || 0)
    nextCursor.value = page.nextCursor || ''
    hasMore.value = Boolean(page.hasMore)
    lastSyncedAt.value = new Date().toISOString()
    persistSnapshot()
  }

  const loadFiles = async ({ reset = true, silent = false } = {}) => {
    if (!reset && (loadingMore.value || !hasMore.value || !nextCursor.value)) {
      return
    }

    const requestEpoch = reset ? ++refreshEpoch : refreshEpoch

    if (reset) {
      if (silent) refreshing.value = true
      else loading.value = true
    } else {
      loadingMore.value = true
    }

    try {
      const response = await service.get('/cloud/list', {
        params: {
          limit: PAGE_SIZE,
          cursor: reset ? undefined : nextCursor.value || undefined,
          _t: Date.now(),
        },
      })

      if (requestEpoch !== refreshEpoch) {
        return
      }

      applyPage(response.data || {}, reset)
    } finally {
      if (reset) {
        loading.value = false
        refreshing.value = false
      } else {
        loadingMore.value = false
      }

      if (pendingSilentRefresh && !loading.value && !loadingMore.value && !refreshing.value) {
        pendingSilentRefresh = false
        refreshInBackground()
      }
    }
  }

  const refreshInBackground = () => {
    if (loading.value || loadingMore.value || refreshing.value) {
      pendingSilentRefresh = true
      return backgroundRefreshPromise || Promise.resolve()
    }

    if (backgroundRefreshPromise) {
      return backgroundRefreshPromise
    }

    backgroundRefreshPromise = loadFiles({ reset: true, silent: true })
      .catch(() => {})
      .finally(() => {
        backgroundRefreshPromise = null
      })

    return backgroundRefreshPromise
  }

  const pushOptimisticFile = (file) => {
    files.value = mergeUniqueFiles([normalizeFile(file)], files.value)
    persistSnapshot()
  }

  const removeOptimisticFile = (object) => {
    files.value = files.value.filter((item) => item.object !== object)
    persistSnapshot()
  }

  const replaceOptimisticFile = (object, nextFile) => {
    const normalized = normalizeFile(nextFile)
    files.value = files.value.map((item) => (item.object === object ? normalized : item))
    persistSnapshot()
  }

  const createOptimisticFile = ({ name, size = 0 }) => ({
    object: `temp:${Date.now()}:${++optimisticSeed}`,
    name,
    size,
    modifyTime: new Date().toISOString(),
    syncing: true,
  })

  const syncFilesAfterMutation = async () => {
    try {
      await loadFiles({ reset: true, silent: true })
    } catch (error) {
      console.warn('[cloud-drive] silent sync failed', error)
    }
  }

  const uploadFileOnWeb = async (file) => {
    const formData = new FormData()
    const token = uni.getStorageSync('satoken')
    formData.append('file', file)

    const response = await fetch(buildApiUrl('/cloud/upload'), {
      method: 'POST',
      headers: token ? { satoken: token } : {},
      body: formData,
    })

    const data = await response.json()
    if (!response.ok || Number(data?.code) !== 200) {
      const error = new Error(data?.message || data?.msg || `HTTP ${response.status}`)
      error.code = data?.code || 'NETWORK_ERROR'
      throw error
    }

    return data
  }

  const pickFileOnWeb = () =>
    new Promise((resolve) => {
      if (!supportsBrowserDom()) {
        resolve(null)
        return
      }

      const input = document.createElement('input')
      input.type = 'file'
      input.onchange = (event) => {
        const file = event.target?.files?.[0]
        resolve(
          file
            ? createUploadAsset({
                source: file,
                name: file.name,
                size: file.size,
              })
            : null,
        )
        input.remove()
      }
      input.click()
    })

  const pickFileOnDevice = () =>
    new Promise((resolve) => {
      if (isAppPlusRuntime()) {
        resolve({ unsupportedInApp: true })
        return
      }

      if (typeof uni.chooseFile !== 'function') {
        resolve(null)
        return
      }

      uni.chooseFile({
        count: 1,
        success: (result) => {
          const tempFile = result.tempFiles?.[0]
          const path = tempFile?.path || tempFile?.tempFilePath || result.tempFilePaths?.[0]
          resolve(
            path
              ? createUploadAsset({
                  source: path,
                  name: tempFile?.name || getFileNameFromPath(path),
                  size: tempFile?.size || 0,
                })
              : null,
          )
        },
        fail: () => resolve(null),
      })
    })

  const uploadFileByObject = async (file) => {
    if (!file || uploading.value) {
      return
    }

    try {
      const fileForValidation =
        file?.uploadSource && typeof file.uploadSource === 'object' ? file.uploadSource : file
      validateFileBeforeUpload(fileForValidation)
    } catch (error) {
      uni.showToast({
        title: getUploadErrorMessage(error),
        icon: 'none',
      })
      return
    }

    const optimisticFile = createOptimisticFile({
      name: file.name,
      size: file.size,
    })

    uploading.value = true
    pushOptimisticFile(optimisticFile)

    try {
      const response =
        supportsBrowserDom() && file?.uploadSource && typeof file.uploadSource === 'object'
          ? await uploadFileOnWeb(file.uploadSource)
          : await uploadFile('/cloud/upload', file.uploadSource || file, {
              showProgress: false,
            })

      const objectName = resolveCloudObjectName(response) || optimisticFile.object
      replaceOptimisticFile(optimisticFile.object, {
        object: objectName,
        name: file?.name || getFileNameFromPath(objectName),
        size: Number(file?.size || 0),
        modifyTime: new Date().toISOString(),
        syncing: false,
      })
      used.value += Number(file?.size || 0)
      persistSnapshot()
      uni.showToast({ title: '上传完成', icon: 'none' })
      await syncFilesAfterMutation()
    } catch (error) {
      removeOptimisticFile(optimisticFile.object)
      uni.showToast({
        title: getUploadErrorMessage(error),
        icon: 'none',
      })
    } finally {
      uploading.value = false
    }
  }

  const chooseAndUpload = async () => {
    const file = supportsBrowserDom() ? await pickFileOnWeb() : await pickFileOnDevice()
    if (file?.unsupportedInApp) {
      uni.showToast({
        title: '当前 App 端请使用导入链接上传通用文件',
        icon: 'none',
      })
      return
    }

    if (file) {
      await uploadFileByObject(file)
      return
    }

    uni.showToast({
      title: '当前环境暂不支持直接上传',
      icon: 'none',
    })
  }

  const decodeUrlPart = (value = '') => {
    try {
      return decodeURIComponent(String(value || '').replace(/\+/g, ' '))
    } catch {
      return String(value || '')
    }
  }

  const resolveImportFileName = (value = '') => {
    const rawValue = String(value || '').trim()
    if (!rawValue) {
      return '远程文件'
    }

    const fileNameMatch =
      rawValue.match(/(?:[?&]filename=)([^&#]+)/i) ||
      rawValue.match(/filename\*=UTF-8''([^;]+)/i) ||
      rawValue.match(/filename=\"?([^\";]+)\"?/i)
    const explicitName = decodeUrlPart(fileNameMatch?.[1] || '')
    if (explicitName) {
      return explicitName
    }

    const cleanPath = rawValue.split('#')[0].split('?')[0]
    const pathName = decodeUrlPart(cleanPath.split('/').filter(Boolean).pop() || '')
    return pathName || '远程文件'
  }

  const openImportPrompt = () => {
    if (importing.value) {
      return
    }

    uni.showModal({
      title: '导入文件链接',
      editable: true,
      placeholderText: '输入可访问的文件 URL',
      success: async (result) => {
        const fileUrl = result.content?.trim()
        if (!result.confirm || !fileUrl) {
          return
        }

        const optimisticFile = createOptimisticFile({
          name: resolveImportFileName(fileUrl),
        })

        importing.value = true
        pushOptimisticFile(optimisticFile)

        try {
          const response = await service.post('/cloud/import', { fileUrl })
          const objectName = resolveCloudObjectName(response) || optimisticFile.object
          replaceOptimisticFile(optimisticFile.object, {
            object: objectName,
            name: optimisticFile.name,
            size: 0,
            modifyTime: new Date().toISOString(),
            syncing: false,
          })
          uni.showToast({ title: '导入完成', icon: 'none' })
          persistSnapshot()
          await syncFilesAfterMutation()
        } catch (error) {
          removeOptimisticFile(optimisticFile.object)
        } finally {
          importing.value = false
        }
      },
    })
  }

  const openUrlInBrowser = (url) => {
    if (!supportsBrowserDom() || !url) {
      return false
    }
    window.open(url, '_blank', 'noopener,noreferrer')
    return true
  }

  const openLocalDocument = async (url) => {
    const { localPath } = await downloadRemoteFileToLocalPath(url)
    if (localPath && typeof uni.openDocument === 'function') {
      uni.openDocument({
        filePath: localPath,
        showMenu: true,
        fail: () => {
          uni.setClipboardData({ data: url })
        },
      })
      return true
    }
    return false
  }

  const openFileInSystem = async (file) => {
    if (!file?.object) {
      return
    }

    const assetUrl = resolveAssetUrl(file)
    const downloadUrl = resolveDownloadUrl(file)
    const targetUrl = assetUrl || downloadUrl
    if (!targetUrl) {
      uni.showToast({ title: '文件地址无效', icon: 'none' })
      return
    }

    if (file.previewMode === 'image' && typeof uni.previewImage === 'function') {
      uni.previewImage({ urls: [targetUrl], current: targetUrl })
      return
    }

    if (openUrlInBrowser(targetUrl)) {
      return
    }

    try {
      if (file.previewMode === 'pdf' || file.previewMode === 'document' || file.previewMode === 'text') {
        const opened = await openLocalDocument(targetUrl)
        if (opened) {
          return
        }
      }

      if (isAppPlusRuntime() && typeof plus !== 'undefined' && plus.runtime?.openURL) {
        plus.runtime.openURL(targetUrl)
        return
      }

      uni.setClipboardData({ data: targetUrl })
    } catch (error) {
      console.warn('[cloud-drive] open system preview failed', error)
      uni.setClipboardData({ data: targetUrl })
    }
  }

  const downloadFile = async (file) => {
    if (!file?.object || isDownloading(file.object)) {
      return
    }

    const targetUrl = resolveDownloadUrl(file)
    if (!targetUrl) {
      uni.showToast({ title: '下载地址无效', icon: 'none' })
      return
    }

    setActionState(downloadingObjects, file.object, true)

    try {
      if (supportsBrowserDom()) {
        const anchor = document.createElement('a')
        anchor.href = targetUrl
        anchor.download = file.name || getFileNameFromPath(targetUrl)
        anchor.rel = 'noopener'
        anchor.style.display = 'none'
        document.body.appendChild(anchor)
        anchor.click()
        document.body.removeChild(anchor)
      } else {
        await openLocalDocument(targetUrl)
      }
    } finally {
      setActionState(downloadingObjects, file.object, false)
    }
  }

  const fetchPreviewContent = async (file) => {
    const response = await service.get('/cloud/preview-content', {
      params: { object: file.object },
    })
    return response.data || {}
  }

  const openPreview = async (file) => {
    if (!file?.object || file.syncing) {
      return
    }

    const normalized = normalizeFile(file)
    previewFile.value = normalized
    previewVisible.value = true
    previewLoading.value = true

    try {
      if (normalized.previewMode === 'image') {
        previewState.value = {
          mode: 'image',
          assetUrl: resolveAssetUrl(normalized),
        }
        return
      }

      if (normalized.previewMode === 'video') {
        previewState.value = {
          mode: 'video',
          assetUrl: resolveAssetUrl(normalized),
        }
        return
      }

      if (normalized.previewMode === 'audio') {
        previewState.value = {
          mode: 'audio',
          assetUrl: resolveAssetUrl(normalized),
        }
        return
      }

      if (normalized.previewMode === 'pdf') {
        if (!supportsBrowserDom()) {
          previewVisible.value = false
          previewLoading.value = false
          await openFileInSystem(normalized)
          return
        }

        previewState.value = {
          mode: 'pdf',
          assetUrl: resolveAssetUrl(normalized),
        }
        return
      }

      if (normalized.previewMode === 'text' || normalized.previewMode === 'document') {
        const payload = await fetchPreviewContent(normalized)
        previewState.value = {
          mode: payload.mode || 'unsupported',
          title: payload.title || normalized.name,
          message: payload.message || '',
          textContent: payload.textContent || '',
          htmlContent: payload.htmlContent || '',
          truncated: Boolean(payload.truncated),
        }
        return
      }

      previewState.value = {
        mode: 'unsupported',
        message: '当前文件暂不支持在线预览，请下载后查看。',
      }
    } catch (error) {
      console.error('[cloud-drive] preview failed', error)
      previewState.value = {
        mode: 'unsupported',
        message: error?.message || '预览加载失败，请稍后重试。',
      }
    } finally {
      previewLoading.value = false
    }
  }

  const closePreview = () => {
    previewVisible.value = false
    previewLoading.value = false
    previewFile.value = null
    previewState.value = {}
  }

  const previewCurrentFileInSystem = async () => {
    if (previewFile.value) {
      await openFileInSystem(previewFile.value)
    }
  }

  const downloadCurrentFile = async () => {
    if (previewFile.value) {
      await downloadFile(previewFile.value)
    }
  }

  const deleteFile = async (file) => {
    if (!file?.object || isDeleting(file.object)) {
      return
    }

    const confirmed = await new Promise((resolve) => {
      uni.showModal({
        title: '删除文件',
        content: `确定删除 ${file.name} 吗？`,
        success: (result) => resolve(Boolean(result.confirm)),
        fail: () => resolve(false),
      })
    })

    if (!confirmed) {
      return
    }

    const snapshotFiles = [...files.value]
    const snapshotUsed = used.value

    setActionState(deletingObjects, file.object, true)
    files.value = files.value.filter((item) => item.object !== file.object)
    used.value = Math.max(0, used.value - Number(file.size || 0))
    persistSnapshot()

    try {
      await service.post('/cloud/delete', null, {
        params: { object: file.object },
      })
      if (previewFile.value?.object === file.object) {
        closePreview()
      }
      uni.showToast({ title: '已删除', icon: 'none' })
      await syncFilesAfterMutation()
    } catch (error) {
      files.value = snapshotFiles
      used.value = snapshotUsed
      persistSnapshot()
    } finally {
      setActionState(deletingObjects, file.object, false)
    }
  }

  const previewButtonLabel = (file) => PREVIEW_ACTION_LABEL[file.previewMode] || '预览'

  const openShareDialog = (file) => {
    if (!file?.object || file.syncing) return
    shareFile.value = normalizeFile(file)
    shareRecord.value = null
    sharePassword.value = ''
    shareExpireDays.value = 7
    shareVisible.value = true
    void loadShareList()
  }

  const closeShareDialog = () => {
    shareVisible.value = false
    shareFile.value = null
    shareRecord.value = null
    sharePassword.value = ''
    shareExpireDays.value = 7
  }

  const createShare = async () => {
    if (!shareFile.value || shareLoading.value) return
    shareLoading.value = true
    try {
      const response = await service.post('/cloud/share/create', {
        fileId: shareFile.value.object,
        password: sharePassword.value.trim() || null,
        expireDays: shareExpireDays.value,
      })
      if (response.data) {
        shareRecord.value = response.data
        uni.showToast({ title: '分享创建成功', icon: 'none' })
      }
    } catch (error) {
      uni.showToast({
        title: error?.message || '分享创建失败',
        icon: 'none',
      })
    } finally {
      shareLoading.value = false
    }
  }

  const loadShareList = async () => {
    try {
      const response = await service.get('/cloud/share/list')
      shareList.value = response.data || []
    } catch (error) {
      console.warn('[cloud-drive] load share list failed', error)
    }
  }

  const cancelShare = async (shareId) => {
    try {
      await service.post('/cloud/share/cancel', null, { params: { shareId } })
      shareList.value = shareList.value.filter((item) => item.id !== shareId)
      uni.showToast({ title: '已取消分享', icon: 'none' })
    } catch (error) {
      uni.showToast({
        title: error?.message || '取消分享失败',
        icon: 'none',
      })
    }
  }

  const copyShareUrl = (record) => {
    if (!record?.shareUrl) return
    uni.setClipboardData({
      data: record.shareUrl,
      success: () => uni.showToast({ title: '链接已复制', icon: 'none' }),
    })
  }

  onMounted(() => {
    const { restored, isFresh } = restoreSnapshot()
    if (!restored) {
      loadFiles({ reset: true })
      return
    }

    if (!isFresh) {
      refreshInBackground()
    }
  })

  return {
    quota: QUOTA_BYTES,
    filterTabs: FILTER_TABS,
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
    usedPercent,
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
  }
}
