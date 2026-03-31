/**
 * 璺ㄥ钩鍙版枃浠朵笂浼犲伐鍏? * 鑷姩鍒ゆ柇鐜锛圚5/鍘熺敓App锛夛紝浣跨敤瀵瑰簲鐨勬渶浣冲疄璺垫柟妗? *
 * 鍔熻兘锛? * - uploadFile: 涓婁紶鍗曚釜鏂囦欢锛堟敮鎸佽繘搴﹀洖璋冿級
 * - uploadWithFormData: 涓婁紶甯︽湁棰濆瀛楁鐨勬枃浠讹紙H5 涓撶敤锛? * - uploadChunk: 鍒嗙墖涓婁紶锛圚5 涓撶敤锛? * - sendJsonMessage: 鍙戦€佺函JSON娑堟伅
 * - validateFileBeforeUpload: 涓婁紶鍓嶆枃浠堕獙璇? * - getUploadErrorMessage: 鑾峰彇鐢ㄦ埛鍙嬪ソ鐨勯敊璇秷鎭? */

import { API_CONFIG } from './config'
import service from './request'

const BASE_URL = String(API_CONFIG.BASE_URL || '').replace(/\/+$/, '')

const buildApiUrl = (path = '') => {
  const normalizedPath = String(path || '')
  if (!normalizedPath) {
    return BASE_URL
  }
  if (/^https?:\/\//i.test(normalizedPath)) {
    return normalizedPath
  }
  if (!BASE_URL) {
    return normalizedPath
  }
  return `${BASE_URL}${normalizedPath.startsWith('/') ? normalizedPath : `/${normalizedPath}`}`
}

const isSuccessCode = (value) => Number(value) === 200

/**
 * 鏂囦欢涓婁紶閿欒鍒嗙被
 */
export const UPLOAD_ERRORS = {
  FILE_TOO_LARGE: {
    code: 'FILE_TOO_LARGE',
    message: '文件过大，请选择小于 100MB 的文件',
    maxSize: 100 * 1024 * 1024
  },
  INVALID_FORMAT: {
    code: 'INVALID_FORMAT',
    message: '文件格式不支持，请选择图片、文档或视频',
    allowedExtensions: ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp',
                       '.mp4', '.avi', '.mov', '.mkv',
                       '.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx',
                       '.txt', '.zip', '.rar']
  },
  NETWORK_ERROR: {
    code: 'NETWORK_ERROR',
    message: '网络连接失败，请检查网络后重试'
  },
  AUTH_ERROR: {
    code: 'AUTH_ERROR',
    message: '登录已过期，请重新登录'
  },
  SERVER_ERROR: {
    code: 'SERVER_ERROR',
    message: '服务器错误，请稍后重试'
  }
}

/**
 * 鑾峰彇鐢ㄦ埛鍙嬪ソ鐨勯敊璇秷鎭? * @param {Error|string} error - 閿欒瀵硅薄鎴栭敊璇秷鎭? * @returns {string} 鐢ㄦ埛鍙嬪ソ鐨勯敊璇秷鎭? */
export const getUploadErrorMessage = (error) => {
  const errorMsg = error?.message || error?.toString() || ''

  if (
    errorMsg.includes('size') ||
    errorMsg.includes('大') ||
    errorMsg.includes('FILE_TOO_LARGE') ||
    error?.code === 'FILE_TOO_LARGE'
  ) {
    return UPLOAD_ERRORS.FILE_TOO_LARGE.message
  }

  if (
    errorMsg.includes('type') ||
    errorMsg.includes('format') ||
    errorMsg.includes('格式') ||
    errorMsg.includes('INVALID_FORMAT') ||
    error?.code === 'INVALID_FORMAT'
  ) {
    return UPLOAD_ERRORS.INVALID_FORMAT.message
  }

  if (
    errorMsg.includes('network') ||
    errorMsg.includes('Network') ||
    errorMsg.includes('fetch') ||
    errorMsg.includes('ERR_NETWORK') ||
    error?.code === 'NETWORK_ERROR'
  ) {
    return UPLOAD_ERRORS.NETWORK_ERROR.message
  }

  if (
    errorMsg.includes('401') ||
    errorMsg.includes('unauthorized') ||
    errorMsg.includes('未授权') ||
    error?.code === 'AUTH_ERROR'
  ) {
    return UPLOAD_ERRORS.AUTH_ERROR.message
  }

  if (
    errorMsg.includes('500') ||
    errorMsg.includes('502') ||
    errorMsg.includes('503') ||
    errorMsg.includes('504') ||
    error?.code === 'SERVER_ERROR'
  ) {
    return UPLOAD_ERRORS.SERVER_ERROR.message
  }

  if (errorMsg && !errorMsg.includes('Upload') && !errorMsg.includes('上传')) {
    return errorMsg
  }

  return '上传失败，请稍后重试'
}

/**
 * 涓婁紶鍓嶉獙璇佹枃浠? * @param {File} file - 瑕侀獙璇佺殑鏂囦欢
 * @returns {boolean} 楠岃瘉閫氳繃杩斿洖true
 * @throws {Error} 楠岃瘉澶辫触鎶涘嚭閿欒
 */
export const validateFileBeforeUpload = (file) => {
  if (!file || file.size > UPLOAD_ERRORS.FILE_TOO_LARGE.maxSize) {
    const error = new Error(UPLOAD_ERRORS.FILE_TOO_LARGE.message)
    error.code = 'FILE_TOO_LARGE'
    throw error
  }

  // 妫€鏌ユ枃浠剁被鍨嬶紙鍩轰簬鎵╁睍鍚嶏級
  const fileName = file.name || file.path || ''
  const fileExt = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()

  if (!fileExt || !UPLOAD_ERRORS.INVALID_FORMAT.allowedExtensions.includes(fileExt)) {
    const error = new Error(UPLOAD_ERRORS.INVALID_FORMAT.message)
    error.code = 'INVALID_FORMAT'
    throw error
  }

  return true
}

/**
 * 鏄剧ず涓婁紶杩涘害
 * @param {number} progress - 杩涘害鐧惧垎姣旓紙0-100锛? * @param {string} message - 鍙€夌殑鑷畾涔夋秷鎭? */
const showUploadProgress = (progress, message = null) => {
  if (progress === 100) {
    uni.hideLoading()
  } else {
    uni.showLoading({
      title: message || `涓婁紶涓?${progress}%`,
      mask: true
    })
  }
}

/**
 * 闅愯棌涓婁紶杩涘害
 */
const hideUploadProgress = () => {
  uni.hideLoading()
}

const parseUploadResponse = (rawData, statusCode) => {
  const data = typeof rawData === 'string' ? JSON.parse(rawData) : rawData

  if (Number(statusCode) !== 200) {
    const error = new Error(data?.message || data?.msg || `HTTP ${statusCode}`)
    error.statusCode = statusCode
    throw error
  }

  if (!data || Number(data.code) !== 200) {
    const error = new Error(data?.message || data?.msg || '涓婁紶澶辫触')
    error.code = data?.code
    error.statusCode = statusCode
    throw error
  }

  return data
}

const createUploadSettler = (resolve, reject, showProgress) => {
  let settled = false

  return {
    resolve: (payload) => {
      if (settled) return
      settled = true
      if (showProgress) hideUploadProgress()
      resolve(payload)
    },
    reject: (payload) => {
      if (settled) return
      settled = true
      if (showProgress) hideUploadProgress()
      reject(payload)
    },
  }
}

const uploadFileInH5 = ({
  url,
  file,
  fieldName = 'file',
  formData = {},
  headers = {},
  onProgress,
  showProgress = true,
}) =>
  new Promise((resolve, reject) => {
    try {
      const xhr = new XMLHttpRequest()
      const payload = new FormData()
      const settle = createUploadSettler(resolve, reject, showProgress)

      payload.append(fieldName, file)
      Object.entries(formData || {}).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          payload.append(key, value)
        }
      })

      xhr.upload.addEventListener('progress', (event) => {
        if (!event.lengthComputable) return
        const progress = Math.round((event.loaded / event.total) * 100)
        if (typeof onProgress === 'function') {
          onProgress(progress)
        }
        if (showProgress) {
          showUploadProgress(progress)
        }
      })

      xhr.addEventListener('load', () => {
        try {
          settle.resolve(parseUploadResponse(xhr.responseText, xhr.status))
        } catch (error) {
          settle.reject(error)
        }
      })

      xhr.addEventListener('error', () => {
        const error = new Error('缃戠粶杩炴帴澶辫触')
        error.code = 'NETWORK_ERROR'
        settle.reject(error)
      })

      xhr.addEventListener('abort', () => {
        const error = new Error('上传已取消')
        error.code = 'UPLOAD_ABORTED'
        settle.reject(error)
      })

      xhr.open('POST', buildApiUrl(url), true)
      Object.entries(headers || {}).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          xhr.setRequestHeader(key, value)
        }
      })
      xhr.send(payload)
    } catch (error) {
      if (showProgress) hideUploadProgress()
      reject(error)
    }
  })

const uploadFileWithJsonInH5 = ({
  url,
  file,
  jsonData,
  jsonFieldName = 'sendDTO',
  fileFieldName = 'file',
  headers = {},
  onProgress,
  showProgress = true,
}) =>
  new Promise((resolve, reject) => {
    try {
      const xhr = new XMLHttpRequest()
      const payload = new FormData()
      const settle = createUploadSettler(resolve, reject, showProgress)

      payload.append(jsonFieldName, JSON.stringify(jsonData))
      payload.append(fileFieldName, file)

      xhr.upload.addEventListener('progress', (event) => {
        if (!event.lengthComputable) return
        const progress = Math.round((event.loaded / event.total) * 100)
        if (typeof onProgress === 'function') {
          onProgress(progress)
        }
        if (showProgress) {
          showUploadProgress(progress)
        }
      })

      xhr.addEventListener('load', () => {
        try {
          settle.resolve(parseUploadResponse(xhr.responseText, xhr.status))
        } catch (error) {
          settle.reject(error)
        }
      })

      xhr.addEventListener('error', () => {
        const error = new Error('网络连接失败')
        error.code = 'NETWORK_ERROR'
        settle.reject(error)
      })

      xhr.addEventListener('abort', () => {
        const error = new Error('上传已取消')
        error.code = 'UPLOAD_ABORTED'
        settle.reject(error)
      })

      xhr.open('POST', url, true)
      Object.entries(headers || {}).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          xhr.setRequestHeader(key, value)
        }
      })
      xhr.send(payload)
    } catch (error) {
      if (showProgress) hideUploadProgress()
      reject(error)
    }
  })

/**
 * 鍒ゆ柇鏄惁涓?H5 鐜
 * 浣跨敤鏉′欢缂栬瘧鎸囦护锛孶niApp 瀹樻柟鎺ㄨ崘鏂瑰紡
 */
export const isH5 = () =>
  typeof window !== 'undefined' &&
  typeof document !== 'undefined' &&
  typeof navigator !== 'undefined'

/**
 * 鍒ゆ柇鏄惁涓哄師鐢?App 鐜
 * 浣跨敤鏉′欢缂栬瘧鎸囦护锛孶niApp 瀹樻柟鎺ㄨ崘鏂瑰紡
 */
export const isNativeApp = () =>
  typeof plus !== 'undefined' &&
  typeof uni !== 'undefined' &&
  typeof uni.getSystemInfoSync === 'function' &&
  uni.getSystemInfoSync().platform !== 'web'

/**
 * 涓婁紶鍗曚釜鏂囦欢锛堣法骞冲彴閫氱敤鏂规锛屾敮鎸佽繘搴﹀洖璋冿級
 * @param {string} url - 涓婁紶鎺ュ彛鍦板潃锛堢浉瀵硅矾寰勶紝濡?/chat/file/upload锛? * @param {File|string} fileData - H5: File 瀵硅薄锛孉pp: 鏂囦欢璺緞瀛楃涓? * @param {object} options - 鍙€夊弬鏁? *   - fieldName: 鏂囦欢瀛楁鍚嶏紙榛樿 'file'锛? *   - formData: 棰濆鐨勮〃鍗曞瓧娈碉紙浠?H5锛? *   - onProgress: 杩涘害鍥炶皟鍑芥暟(progressPercent) => {}
 *   - showProgress: 鏄惁鏄剧ず杩涘害loading锛堥粯璁rue锛? * @returns {Promise} 杩斿洖鏈嶅姟绔搷搴旀暟鎹? */
export const uploadFile = async (url, fileData, options = {}) => {
  const {
    fieldName = 'file',
    formData: extraFormFields = {},
    onProgress,
    showProgress = true
  } = options

  const token = uni.getStorageSync('satoken')
  const headers = {
    satoken: token
  }

  if (isH5()) {
    console.log('[uploadFile] H5 environment, using XMLHttpRequest + FormData')
    return uploadFileInH5({
      url: buildApiUrl(url),
      file: fileData,
      fieldName,
      formData: extraFormFields,
      headers,
      onProgress,
      showProgress,
    })
  }

  // =============== 鍘熺敓 App 鐜锛氫娇鐢?uni.uploadFile ===============
  console.log('[uploadFile] 鍘熺敓 App 鐜锛屼娇鐢?uni.uploadFile')

  return new Promise((resolve, reject) => {
    try {
      const uploadTask = uni.uploadFile({
        url: buildApiUrl(url),
        filePath: fileData, // App 鐜涓簲璇ユ槸鏂囦欢璺緞
        name: fieldName,
        formData: extraFormFields,
        header: headers,
        success: (response) => {
          try {
            const data = typeof response.data === 'string'
              ? JSON.parse(response.data)
              : response.data

            if (response.statusCode === 200 && isSuccessCode(data.code)) {
              if (showProgress) hideUploadProgress()
              resolve(data)
            } else {
              if (showProgress) hideUploadProgress()
              const error = new Error(data.message || `HTTP ${response.statusCode}`)
              error.code = data.code
              error.statusCode = response.statusCode
              reject(error)
            }
          } catch (e) {
            if (showProgress) hideUploadProgress()
            reject(new Error('鍝嶅簲瑙ｆ瀽澶辫触: ' + e.message))
          }
        },
        fail: (error) => {
          if (showProgress) hideUploadProgress()
          const err = new Error('涓婁紶澶辫触: ' + error.errMsg)
          err.code = 'NETWORK_ERROR'
          reject(err)
        }
      })

      if (uploadTask && typeof uploadTask.onProgressUpdate === 'function') {
        uploadTask.onProgressUpdate((res) => {
          const progress = res.progress

          // 璋冪敤杩涘害鍥炶皟
          if (typeof onProgress === 'function') {
            onProgress(progress)
          }

          // 鏄剧ず杩涘害loading
          if (showProgress) {
            showUploadProgress(progress)
          }
        })
      }
    } catch (error) {
      if (showProgress) hideUploadProgress()
      reject(new Error('涓婁紶寮傚父: ' + error.message))
    }
  })
}

/**
 * 涓婁紶甯︽湁 JSON 鏁版嵁鐨勬枃浠讹紙璺ㄥ钩鍙版柟妗堬紝鏀寔杩涘害鍥炶皟锛? * H5锛氫娇鐢?FormData + XMLHttpRequest
 * App锛氫娇鐢?uni.uploadFile锛屽皢 JSON 鏁版嵁閫氳繃 formData 瀛楁浼犻€? *
 * @param {string} url - 涓婁紶鎺ュ彛鍦板潃
 * @param {File|string} file - H5: File 瀵硅薄锛孉pp: 鏂囦欢璺緞瀛楃涓? * @param {object} jsonData - 瑕佸簭鍒楀寲涓?JSON 鐨勬暟鎹? * @param {string} jsonFieldName - JSON 瀛楁鍚嶏紙榛樿 'sendDTO'锛? * @param {object} options - 鍙€夊弬鏁? *   - onProgress: 杩涘害鍥炶皟鍑芥暟(progressPercent) => {}
 *   - showProgress: 鏄惁鏄剧ず杩涘害loading锛堥粯璁rue锛? * @returns {Promise}
 */
export const uploadFileWithJSON = async (url, file, jsonData, jsonFieldName = 'sendDTO', options = {}) => {
  const {
    onProgress,
    showProgress = true,
    fileFieldName = 'file'
  } = options

  const token = uni.getStorageSync('satoken')
  const headers = {
    satoken: token
  }

  if (isH5()) {
    console.log('[uploadFileWithJSON] H5 environment, using XMLHttpRequest + FormData')
    return uploadFileWithJsonInH5({
      url: buildApiUrl(url),
      file,
      jsonData,
      jsonFieldName,
      fileFieldName,
      headers,
      onProgress,
      showProgress,
    })
  }

  // =============== 鍘熺敓 App 鐜锛氫娇鐢?uni.uploadFile ===============
  console.log('[uploadFileWithJSON] 鍘熺敓 App 鐜锛屼娇鐢?uni.uploadFile')

  return new Promise((resolve, reject) => {
    try {
      const uploadTask = uni.uploadFile({
        url: buildApiUrl(url),
        filePath: file, // App 鐜涓?file 鏄枃浠惰矾寰勫瓧绗︿覆
        name: fileFieldName,
        formData: {
          [jsonFieldName]: JSON.stringify(jsonData)
        },
        header: headers,
        success: (response) => {
          try {
            const data = typeof response.data === 'string'
              ? JSON.parse(response.data)
              : response.data

            if (response.statusCode === 200 && isSuccessCode(data.code)) {
              if (showProgress) hideUploadProgress()
              resolve(data)
            } else {
              if (showProgress) hideUploadProgress()
              const error = new Error(data.message || `HTTP ${response.statusCode}`)
              error.code = data.code
              error.statusCode = response.statusCode
              reject(error)
            }
          } catch (e) {
            if (showProgress) hideUploadProgress()
            reject(new Error('鍝嶅簲瑙ｆ瀽澶辫触: ' + e.message))
          }
        },
        fail: (error) => {
          if (showProgress) hideUploadProgress()
          const err = new Error('涓婁紶澶辫触: ' + error.errMsg)
          err.code = 'NETWORK_ERROR'
          reject(err)
        }
      })

      if (uploadTask && typeof uploadTask.onProgressUpdate === 'function') {
        uploadTask.onProgressUpdate((res) => {
          const progress = res.progress

          // 璋冪敤杩涘害鍥炶皟
          if (typeof onProgress === 'function') {
            onProgress(progress)
          }

          // 鏄剧ず杩涘害loading
          if (showProgress) {
            showUploadProgress(progress)
          }
        })
      }
    } catch (error) {
      if (showProgress) hideUploadProgress()
      reject(new Error('涓婁紶寮傚父: ' + error.message))
    }
  })
}

/**
 * 鍒嗙墖涓婁紶锛堜粎 H5锛? * 鍘熺敓 App 鐜搴旇璋冪敤 uploadFileWithJSON 闄嶇骇澶勭悊锛屼笉鏀寔鍒嗙墖
 * 
 * @param {string} url - 鍒嗙墖涓婁紶鎺ュ彛
 * @param {Blob} chunk - 鏂囦欢鍒嗙墖
 * @param {object} chunkInfo - 鍒嗙墖淇℃伅 {md5, chunkIndex, fileName, totalChunks, fileSize}
 * @returns {Promise}
 */
export const uploadChunk = async (url, chunk, chunkInfo) => {
  // =============== H5 鐜锛氫娇鐢?FormData 鍒嗙墖涓婁紶 ===============
  if (!isH5() || typeof FormData === 'undefined') {
    return Promise.reject(
      new Error('[uploadChunk] 鍒嗙墖涓婁紶浠呮敮鎸?H5 鐜锛孉pp 搴斾娇鐢?uploadFileWithJSON 闄嶇骇澶勭悊')
    )
  }

  console.log(`[uploadChunk] 涓婁紶鍒嗙墖 ${chunkInfo.chunkIndex}/${chunkInfo.totalChunks}`)

  try {
    const formData = new FormData()
    formData.append('md5', chunkInfo.md5)
    formData.append('chunkIndex', chunkInfo.chunkIndex)
    formData.append('fileName', chunkInfo.fileName)
    formData.append('totalChunks', chunkInfo.totalChunks)
    formData.append('fileSize', chunkInfo.fileSize)
    formData.append('file', chunk)

    const token = uni.getStorageSync('satoken')
    const response = await fetch(buildApiUrl(url), {
      method: 'POST',
      headers: {
        satoken: token
      },
      body: formData
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    const result = await response.json()
    if (result.code !== 200) {
      throw new Error(result.message || '鍒嗙墖涓婁紶澶辫触')
    }

    return result
  } catch (error) {
    console.error(`[uploadChunk] 鍒嗙墖 ${chunkInfo.chunkIndex} 涓婁紶澶辫触:`, error)
    throw error
  }
}

/**
 * 鍙戦€佺函 JSON 娑堟伅锛堣法骞冲彴鏂规锛? * H5: 浣跨敤 Fetch API + application/json
 * App: 浣跨敤 uni.request
 *
 * @param {string} url - 鎺ュ彛鍦板潃
 * @param {object} jsonData - 娑堟伅鏁版嵁瀵硅薄
 * @param {string} fieldName - JSON 瀛楁鍚嶏紙浠呭湪闇€瑕?multipart 鏃朵娇鐢級
 * @returns {Promise}
 */
export const sendJsonMessage = async (url, jsonData, fieldName = 'sendDTO') => {
  const token = uni.getStorageSync('satoken')

  // =============== H5 鐜锛氫娇鐢?Fetch API + application/json ===============
  if (isH5()) {
    console.log('[sendJsonMessage] H5 鐜锛屼娇鐢?Fetch API')

    try {
      const response = await fetch(buildApiUrl(url), {
        method: 'POST',
        headers: {
          'satoken': token,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(jsonData)
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }

      const result = await response.json()
      if (result.code === 200) {
        return result
      } else {
        throw new Error(result.message || '发送失败')
      }
    } catch (error) {
      console.error('[sendJsonMessage] H5 鍙戦€佸け璐?', error)
      throw error
    }
  }

  // =============== 鍘熺敓 App 鐜锛氫娇鐢?uni.request ===============
  console.log('[sendJsonMessage] 鍘熺敓 App 鐜锛屼娇鐢?uni.request')

  return new Promise((resolve, reject) => {
    try {
      uni.request({
        url: buildApiUrl(url),
        method: 'POST',
        data: jsonData,
        header: {
          'satoken': token,
          'Content-Type': 'application/json'
        },
        success: (response) => {
          try {
            const data = response.data

            if (response.statusCode === 200 && (data.code === 200 || data.code === '200')) {
              console.log('[sendJsonMessage] App 鍙戦€佹垚鍔?', data)
              resolve(data)
            } else {
              console.warn('[sendJsonMessage] App 鏀跺埌闈炴垚鍔熷搷搴?', response.statusCode, data)
              reject(new Error(data.message || `HTTP ${response.statusCode}`))
            }
          } catch (e) {
            console.error('[sendJsonMessage] App 鍝嶅簲瑙ｆ瀽澶辫触:', e, response)
            reject(new Error('鍝嶅簲瑙ｆ瀽澶辫触: ' + e.message))
          }
        },
        fail: (error) => {
          console.error('[sendJsonMessage] App 鍙戦€佸け璐?', error)
          reject(new Error('鍙戦€佸け璐? ' + error.errMsg))
        }
      })
    } catch (error) {
      console.error('[sendJsonMessage] App 鍙戦€佸紓甯?', error)
      reject(new Error('鍙戦€佸紓甯? ' + error.message))
    }
  })
}

export default {
  isH5,
  isNativeApp,
  uploadFile,
  uploadFileWithJSON,
  uploadChunk,
  sendJsonMessage,
  validateFileBeforeUpload,
  getUploadErrorMessage
}


