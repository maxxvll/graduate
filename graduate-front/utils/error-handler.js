import { showToast } from './common'

export const ErrorType = {
  AUTH: 'AUTH',
  NETWORK: 'NETWORK',
  SERVER: 'SERVER',
  VALIDATION: 'VALIDATION',
  NOT_FOUND: 'NOT_FOUND',
  STORAGE: 'STORAGE',
  TIMEOUT: 'TIMEOUT',
  UNKNOWN: 'UNKNOWN',
}

export const isAuthError = (statusCode, businessCode) =>
  Number(statusCode) === 401 || Number(businessCode) === 401

export const handleAuthError = () => {
  try {
    uni.removeStorageSync('satoken')
    uni.removeStorageSync('userInfo')
  } catch (error) {
    console.warn('[ErrorHandler] Failed to clear auth state', error)
  }

  try {
    const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : []
    const currentRoute = pages.length ? pages[pages.length - 1].route : ''

    if (currentRoute !== 'pages/index/index') {
      uni.reLaunch({
        url: '/pages/index/index',
      })
    }
  } catch (error) {
    console.warn('[ErrorHandler] Failed to redirect to login', error)
  }
}

export const handleError = (error, options = {}) => {
  const {
    showErrorMessage = true,
    fallbackMessage = '请求失败，请稍后重试',
  } = options

  const statusCode = error?.response?.status ?? error?.statusCode ?? null
  const businessCode = error?.data?.code ?? error?.code ?? null

  let type = ErrorType.UNKNOWN
  let message = error?.message || error?.data?.message || fallbackMessage

  if (isAuthError(statusCode, businessCode)) {
    type = ErrorType.AUTH
    message = '登录已过期，请重新登录'
    handleAuthError()
  } else if (statusCode === 408 || /timeout/i.test(message)) {
    type = ErrorType.TIMEOUT
    message = '请求超时，请检查网络连接'
  } else if (statusCode === 404) {
    type = ErrorType.NOT_FOUND
    message = '请求的资源不存在'
  } else if ([500, 502, 503, 504].includes(Number(statusCode))) {
    type = ErrorType.SERVER
    message = '服务器繁忙，请稍后再试'
  } else if (statusCode === 400) {
    type = ErrorType.VALIDATION
    message = error?.data?.message || '请求参数有误'
  } else if (/network|fail/i.test(message)) {
    type = ErrorType.NETWORK
    message = '网络连接失败，请检查网络设置'
  }

  if (showErrorMessage) {
    showToast(message)
  }

  return {
    type,
    message,
    originalError: error,
  }
}

export const safeStorageOperation = (
  operation,
  errorMessage = '存储操作失败',
  fallbackValue = null,
) => {
  try {
    return operation()
  } catch (error) {
    console.warn(`[ErrorHandler] ${errorMessage}`, error)
    return fallbackValue
  }
}

export const safeGetStorage = (key, defaultValue = null) =>
  safeStorageOperation(
    () => {
      const value = uni.getStorageSync(key)
      return value === '' || value === undefined ? defaultValue : value
    },
    `读取存储 ${key} 失败`,
    defaultValue,
  )

export const safeSetStorage = (key, value) =>
  safeStorageOperation(
    () => {
      uni.setStorageSync(key, value)
      return true
    },
    `写入存储 ${key} 失败`,
    false,
  )

export const safeRemoveStorage = (key) =>
  safeStorageOperation(
    () => {
      uni.removeStorageSync(key)
      return true
    },
    `删除存储 ${key} 失败`,
    false,
  )

export default {
  ErrorType,
  isAuthError,
  handleAuthError,
  handleError,
  safeStorageOperation,
  safeGetStorage,
  safeSetStorage,
  safeRemoveStorage,
}
