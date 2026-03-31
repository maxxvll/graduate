/**
 * 通用工具函数库
 * 提取项目中重复使用的工具函数，避免代码冗余
 */

/**
 * 显示 Toast 提示
 * @param {string} title - 提示内容
 */
export const showToast = (title) => {
  if (!title) {
    return
  }

  try {
    uni.showToast({
      title,
      icon: 'none',
    })
  } catch (error) {
    console.error('[Common] Failed to show toast', error)
  }
}

/**
 * 获取设备类型
 * @returns {string} 设备类型: 'android' | 'ios' | 'pc' | 'h5' | 'mp-weixin' | 'unknown'
 */
export const getDeviceType = () => {
  try {
    const systemInfo = uni.getSystemInfoSync()

    // 检测微信小程序环境
    if (systemInfo.miniProgram?.envVersion) {
      return 'mp-weixin'
    }

    switch (systemInfo.platform) {
      case 'android':
        return 'android'
      case 'ios':
        return 'ios'
      case 'windows':
      case 'mac':
        return 'pc'
      default:
        return 'h5'
    }
  } catch {
    return 'unknown'
  }
}

/**
 * 格式化文件大小
 * @param {number} size - 文件大小（字节）
 * @returns {string} 格式化后的文件大小，如 "1.5 MB"
 */
export const formatFileSize = (size) => {
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let value = Number(size || 0)
  let index = 0

  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index++
  }

  const digits = index === 0 ? 0 : value >= 10 ? 1 : 2
  return `${value.toFixed(digits)} ${units[index]}`
}

/**
 * 格式化时间
 * @param {number|string|Date} value - 时间值
 * @returns {string} 格式化后的时间，如 "3/31 14:30"
 */
export const formatTime = (value) => {
  if (!value) return '--'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--'

  const month = date.getMonth() + 1
  const day = date.getDate()
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')

  return `${month}/${day} ${hours}:${minutes}`
}

/**
 * 默认头像（72x72）
 * 用于聊天列表等小尺寸场景
 */
export const DEFAULT_AVATAR_SMALL =
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNzIiIGhlaWdodD0iNzIiIHZpZXdCb3g9IjAgMCA3MiA3MiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNzIiIGhlaWdodD0iNzIiIHJ4PSIzNiIgZmlsbD0iI0Q0REFERSIvPjxwYXRoIGQ9Ik0zNiAzNkM0Mi42Mjc0IDM2IDQ4IDMwLjYyNzQgNDggMjRDNCAxNy4zNzI2IDQyLjYyNzQgMTIgMzYgMTJDMjkuMzcyNiAxMiAyNCAxNy4zNzI2IDI0IDI0QzI0IDMwLjYyNzQgMjkuMzcyNiAzNiAzNiAzNloiIGZpbGw9IiM4NDkwQTAiLz48cGF0aCBkPSJNMjAgNTdDMjMuMDQwMSA1MC4wNSAzMC4wMSA0NSAzNiA0NUM0MS45OSA0NSA0OC45NTk5IDUwLjA1IDUyIDU3IiBmaWxsPSIjODQ5MEEwIi8+PC9zdmc+'

/**
 * 默认头像（96x96）
 * 用于个人资料页等大尺寸场景
 */
export const DEFAULT_AVATAR_LARGE =
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iOTYiIGhlaWdodD0iOTYiIHZpZXdCb3g9IjAgMCA5NiA5NiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iOTYiIGhlaWdodD0iOTYiIHJ4PSI0OCIgZmlsbD0iI0UyRTZFQSIvPjxwYXRoIGQ9Ik00OCA0OEM1NS43MzIgNDggNjIgNDEuNzMyIDYyIDM0QzYyIDI2LjI2OCA1NS43MzIgMjAgNDggMjBDNDAuMjY4IDIwIDM0IDI2LjI2OCAzNCAzNEMzNCA0MS43MzIgNDAuMjY4IDQ4IDQ4IDQ4WiIgZmlsbD0iIzlBQUVBNyIvPjxwYXRoIGQ9Ik0yOSA3NEMzMi43NTk5IDY1LjY5NzEgNDAuOTQ3OSA2MCA0OCA2MEM1NS4wNTIxIDYwIDYzLjI0MDEgNjUuNjk3MSA2NyA3NCIgZmlsbD0iIzlBQUVBNyIvPjwvc3ZnPg=='

/**
 * 默认头像（通用）
 * 默认使用小尺寸头像
 */
export const DEFAULT_AVATAR = DEFAULT_AVATAR_SMALL

/**
 * 判断是否为取消类错误
 * @param {Error} error - 错误对象
 * @returns {boolean} 是否为取消类错误
 */
export const isAbortLikeError = (error) =>
  /abort|aborted|cancel/i.test(error?.message || '') || error?.type === 'abort'

/**
 * 延迟执行
 * @param {number} ms - 延迟毫秒数
 * @returns {Promise} Promise 对象
 */
export const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

export default {
  showToast,
  getDeviceType,
  formatFileSize,
  formatTime,
  DEFAULT_AVATAR,
  DEFAULT_AVATAR_SMALL,
  DEFAULT_AVATAR_LARGE,
  isAbortLikeError,
  delay,
}
