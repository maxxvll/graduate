import { supportsBrowserDom, waitForPlusReady } from './runtime'

const SCREENSHOT_BACKGROUND = '#f5f5f5'

const createTimestamp = () => {
  const now = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return [
    now.getFullYear(),
    pad(now.getMonth() + 1),
    pad(now.getDate()),
    '-',
    pad(now.getHours()),
    pad(now.getMinutes()),
    pad(now.getSeconds()),
  ].join('')
}

const createFileName = () => `chat-screenshot-${createTimestamp()}.png`

const triggerBrowserDownload = ({ fileName, dataUrl }) => {
  if (!supportsBrowserDom() || !dataUrl) {
    throw new Error('当前环境不支持下载截图')
  }

  const anchor = document.createElement('a')
  anchor.href = dataUrl
  anchor.download = fileName || createFileName()
  anchor.rel = 'noopener'
  anchor.style.display = 'none'
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
}

const captureBrowserElement = async ({ element, backgroundColor = SCREENSHOT_BACKGROUND } = {}) => {
  if (!supportsBrowserDom()) {
    throw new Error('当前环境不支持截图')
  }

  const target = element || document.querySelector('.chat-shell')
  if (!target) {
    throw new Error('聊天区域还没有准备好')
  }

  const html2canvasModule = await import('html2canvas')
  const html2canvas = html2canvasModule.default || html2canvasModule
  const canvas = await html2canvas(target, {
    useCORS: true,
    backgroundColor,
    logging: false,
    scale: Math.min(window.devicePixelRatio || 1, 2),
  })

  return {
    type: 'browser',
    previewUrl: canvas.toDataURL('image/png', 1),
    fileName: createFileName(),
    localPath: '',
  }
}

const captureAppWebview = async () => {
  const plusRuntime = await waitForPlusReady()
  const bitmapId = `chat-screenshot-${Date.now()}`
  const bitmap = new plusRuntime.nativeObj.Bitmap(bitmapId)
  const webview = plusRuntime.webview.currentWebview()
  const fileName = createFileName()
  const filePath = `_doc/${fileName}`

  try {
    await new Promise((resolve, reject) => {
      webview.draw(
        bitmap,
        () => resolve(),
        (error) => reject(new Error(error?.message || '截图失败')),
      )
    })

    await new Promise((resolve, reject) => {
      bitmap.save(
        filePath,
        { overwrite: true, quality: 100 },
        () => resolve(),
        (error) => reject(new Error(error?.message || '保存截图失败')),
      )
    })

    const previewUrl =
      typeof plusRuntime.io?.convertLocalFileSystemURL === 'function'
        ? plusRuntime.io.convertLocalFileSystemURL(filePath)
        : filePath

    return {
      type: 'app',
      previewUrl,
      fileName,
      localPath: filePath,
    }
  } finally {
    try {
      bitmap.clear()
    } catch {}
  }
}

export const captureChatScreenshot = async (options = {}) => {
  if (supportsBrowserDom()) {
    return captureBrowserElement(options)
  }
  return captureAppWebview()
}

export const saveCapturedScreenshot = async (capture = {}) => {
  if (capture.type === 'browser') {
    triggerBrowserDownload(capture)
    return {
      savedTo: 'download',
    }
  }

  if (capture.type === 'app') {
    const plusRuntime = await waitForPlusReady()
    await new Promise((resolve, reject) => {
      plusRuntime.gallery.save(
        capture.localPath,
        () => resolve(),
        (error) => reject(new Error(error?.message || '保存到系统相册失败')),
      )
    })
    return {
      savedTo: 'album',
    }
  }

  throw new Error('当前截图结果不可保存')
}
