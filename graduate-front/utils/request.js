import { API_CONFIG } from './config'
import { handleError as handleApiError, isAuthError } from './error-handler'
import { showToast, getDeviceType, isAbortLikeError, delay } from './common'
import { getDeviceId } from './device'

const BASE_URL = API_CONFIG.BASE_URL
const REQUEST_TIMEOUT = API_CONFIG.TIMEOUT

class RequestManager {
  constructor() {
    this.pendingRequests = new Map()
  }

  generateRequestKey(config) {
    return `${config.method || 'GET'}_${config.url}_${JSON.stringify(config.params || {})}`
  }

  addRequest(key, task, options = {}) {
    const existingTask = this.pendingRequests.get(key)
    if (options.cancelPrevious && existingTask && typeof existingTask.abort === 'function') {
      existingTask.abort()
    }
    this.pendingRequests.set(key, task)
  }

  removeRequest(key) {
    this.pendingRequests.delete(key)
  }

  cancelAll() {
    this.pendingRequests.forEach((task) => {
      if (task && typeof task.abort === 'function') {
        task.abort()
      }
    })
    this.pendingRequests.clear()
  }

  cancelByUrl(urlPattern) {
    this.pendingRequests.forEach((task, key) => {
      if (key.includes(urlPattern) && task && typeof task.abort === 'function') {
        task.abort()
        this.pendingRequests.delete(key)
      }
    })
  }
}

const requestManager = new RequestManager()

class NetworkError extends Error {
  constructor(message, type, originalError = null) {
    super(message)
    this.name = 'NetworkError'
    this.type = type
    this.originalError = originalError
  }
}

const RETRY_CONFIG = {
  maxRetries: 3,
  retryDelay: 1000,
  retryableStatusCodes: [408, 429, 500, 502, 503, 504],
  retryableMethods: ['GET', 'PUT', 'DELETE'],
}

const appendParams = (url, params = {}) => {
  const queryString = Object.entries(params)
    .filter(([, value]) => value !== null && value !== undefined)
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join('&')

  if (!queryString) {
    return url
  }
  return `${url}${url.includes('?') ? '&' : '?'}${queryString}`
}

const shouldRetry = (config, retryCount, statusCode = null, networkError = false) => {
  if (retryCount >= RETRY_CONFIG.maxRetries) {
    return false
  }
  const method = String(config.method || 'GET').toUpperCase()
  if (!RETRY_CONFIG.retryableMethods.includes(method)) {
    return false
  }
  if (networkError) {
    return true
  }
  return RETRY_CONFIG.retryableStatusCodes.includes(Number(statusCode))
}

const withRetry = async (factory, config, retryCount = 0) => {
  try {
    return await factory()
  } catch (error) {
    if (isAbortLikeError(error)) {
      throw error
    }

    const statusCode = error?.statusCode || error?.response?.status || null
    const networkError =
      /network|timeout|abort|fail/i.test(error?.message || '') ||
      error?.type === 'network' ||
      error?.type === 'timeout'

    if (shouldRetry(config, retryCount, statusCode, networkError)) {
      await delay(RETRY_CONFIG.retryDelay * (retryCount + 1))
      return withRetry(factory, config, retryCount + 1)
    }
    throw error
  }
}

const normalizeResponse = (response) => {
  const { statusCode, data, headers } = response

  if (statusCode < 200 || statusCode >= 300) {
    const error = new NetworkError(
      data?.message || data?.msg || `HTTP ${statusCode}`,
      'server',
      data,
    )
    error.statusCode = statusCode
    throw error
  }

  // 如果是 blob 类型，直接返回（用于文件下载）
  if (data instanceof Blob) {
    return {
      data,
      headers: headers || {},
    }
  }

  if (data?.code !== 200) {
    const message = data?.message || data?.msg || '请求失败'
    const error = new NetworkError(message, 'client', data)
    error.code = data?.code
    throw error
  }

  return data
}

const requestWithFetch = async (config) => {
  const token = uni.getStorageSync('satoken')
  const headers = {
    ...(config.headers || {}),
  }
  if (token) {
    headers.satoken = token
  }
  delete headers['Content-Type']

  const url = `${BASE_URL}${appendParams(config.url, config.params)}`
  const response = await fetch(url, {
    method: config.method || 'POST',
    headers,
    body: config.data,
  })

  // 处理 blob 响应类型
  if (config.responseType === 'blob') {
    const blob = await response.blob()
    // 提取 headers
    const responseHeaders = {}
    response.headers.forEach((value, key) => {
      responseHeaders[key] = value
    })
    return normalizeResponse({
      statusCode: response.status,
      data: blob,
      headers: responseHeaders,
    })
  }

  const data = await response.json()
  return normalizeResponse({
    statusCode: response.status,
    data,
  })
}

const request = async (config) => {
  const isFormData =
    typeof FormData !== 'undefined' && config.data instanceof FormData

  // #ifdef H5
  if (isFormData || config.responseType === 'blob') {
    return withRetry(() => requestWithFetch(config), config)
  }
  // #endif

  return withRetry(
    () =>
      new Promise((resolve, reject) => {
        const token = uni.getStorageSync('satoken')
        const headers = {
          ...(config.headers || {}),
        }
        if (token) {
          headers.satoken = token
        }

        let data = config.data
        const method = String(config.method || 'GET').toUpperCase()
        const url = `${BASE_URL}${appendParams(config.url, config.params)}`

        if (!isFormData) {
          headers['Content-Type'] = 'application/json'
        }

        if (['POST', 'PUT', 'PATCH'].includes(method)) {
          if (data && typeof data === 'object' && !Array.isArray(data) && !isFormData) {
            data = {
              ...data,
              deviceType: data.deviceType || getDeviceType(),
              deviceId: data.deviceId || getDeviceId(),
            }
          }
        }

        const requestKey = requestManager.generateRequestKey(config)
        const task = uni.request({
          url,
          method,
          data,
          header: headers,
          timeout: config.timeout || REQUEST_TIMEOUT,
          responseType: config.responseType || 'text',
          success: (response) => {
            try {
              // 处理 blob 响应
              if (config.responseType === 'blob') {
                const normalized = {
                  data: response.data,
                  headers: response.header || {},
                }
                requestManager.removeRequest(requestKey)
                resolve(normalized)
                return
              }
              const normalized = normalizeResponse(response)
              requestManager.removeRequest(requestKey)
              resolve(normalized)
            } catch (error) {
              requestManager.removeRequest(requestKey)
              reject(error)
            }
          },
          fail: (error) => {
            requestManager.removeRequest(requestKey)
            reject(
              new NetworkError(
                error?.errMsg || '网络请求失败',
                /timeout/i.test(error?.errMsg || '') ? 'timeout' : 'network',
                error,
              ),
            )
          },
        })

        requestManager.addRequest(requestKey, task, {
          cancelPrevious: Boolean(config.cancelPrevious),
        })
      }),
    config,
  ).catch((error) => {
    if (isAbortLikeError(error)) {
      throw error
    }

    if (isAuthError(error?.statusCode, error?.code)) {
      handleApiError(
        {
          response: {
            status: error?.statusCode,
          },
          data: error?.originalError,
          code: error?.code,
          message: error?.message,
        },
        {
          showErrorMessage: true,
        },
      )
    } else {
      showToast(error?.message || '请求失败，请稍后重试')
    }
    throw error
  })
}

const service = {
  get(url, config = {}) {
    return request({
      url,
      method: 'GET',
      ...config,
    })
  },

  post(url, data, config = {}) {
    return request({
      url,
      method: 'POST',
      data,
      ...config,
    })
  },

  put(url, data, config = {}) {
    return request({
      url,
      method: 'PUT',
      data,
      ...config,
    })
  },

  delete(url, config = {}) {
    return request({
      url,
      method: 'DELETE',
      ...config,
    })
  },

  patch(url, data, config = {}) {
    return request({
      url,
      method: 'PATCH',
      data,
      ...config,
    })
  },

  upload(url, filePath, config = {}) {
    return new Promise((resolve, reject) => {
      const token = uni.getStorageSync('satoken')
      uni.uploadFile({
        url: `${BASE_URL}${url}`,
        filePath,
        name: config.name || 'file',
        formData: config.formData || {},
        header: token ? { satoken: token } : {},
        success: (response) => {
          try {
            const parsed =
              typeof response.data === 'string'
                ? JSON.parse(response.data)
                : response.data
            if (parsed?.code === 200) {
              resolve(parsed)
            } else {
              const error = new NetworkError(
                parsed?.message || parsed?.msg || '上传失败',
                'client',
                parsed,
              )
              showToast(error.message)
              reject(error)
            }
          } catch (error) {
            reject(error)
          }
        },
        fail: (error) => {
          const networkError = new NetworkError('上传失败', 'network', error)
          showToast(networkError.message)
          reject(networkError)
        },
      })
    })
  },
}

export { NetworkError, requestManager }
export default service
