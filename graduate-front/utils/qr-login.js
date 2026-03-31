/**
 * 二维码登录工具模块
 * 功能：扫码生成二维码、验证登录、保存 token 等
 */
import service from './request'

/**
 * 生成二维码登录信息
 * @returns {Promise<Object>} 返回 qrCodeBase64 和 qrCodeId
 */
export const generateQrCode = async () => {
  try {
    const res = await service.get('/user/qrcode/generate')
    if (res.code === 200) {
      const { qrCodeBase64, qrCodeId } = res.data
      // 本地存储 qrCodeId 用于后续轮询验证
      uni.setStorageSync('qrCodeId', qrCodeId)
      return { qrCode: qrCodeBase64, qrId: qrCodeId }
    } else {
      throw new Error(res.msg || '生成二维码失败')
    }
  } catch (e) {
    console.error('生成二维码异常', e)
    throw e
  }
}

/**
 * 轮询二维码登录状态
 * 在显示二维码时，定期询问后端用户是否已扫码并登录
 * @param {string} qrId 二维码 ID
 * @param {number} maxRetries 最大重试次数（默认 300 次，即 300 秒）
 * @returns {Promise<Object>} 返回登录结果
 */
export const pollQrLoginStatus = async (qrId, maxRetries = 300) => {
  const interval = 1000 // 每秒轮询一次
  let retries = 0

  return new Promise((resolve) => {
    const poll = async () => {
      try {
        const res = await service.get('/user/qrcode/status', {
          params: { qrCodeId: qrId },
        })
        if (res.code === 200) {
          const { status, token } = res.data

          // 状态说明：
          // 'waiting' - 等待扫码
          // 'scanned' - 已扫码，等待确认
          // 'confirmed' - 已确认登录
          // 'expired' - 二维码已过期

          if (status === 'confirmed') {
            // 登录成功
            saveLoginToken(token)
            resolve({
              success: true,
              message: '登录成功',
              token,
            })
            return
          }

          if (status === 'expired') {
            resolve({
              success: false,
              message: '二维码已过期，请重新生成',
            })
            return
          }

          // 继续轮询
          if (retries < maxRetries) {
            retries++
            setTimeout(poll, interval)
          } else {
            resolve({
              success: false,
              message: '扫码登录超时，请重试',
            })
          }
        } else {
          // 服务端错误
          resolve({
            success: false,
            message: res.msg || '服务器错误',
          })
        }
      } catch (e) {
        console.error('轮询二维码状态异常', e)
        retries++
        if (retries < maxRetries) {
          setTimeout(poll, interval)
        } else {
          resolve({
            success: false,
            message: '网络连接中断，请重试',
          })
        }
      }
    }

    // 立即开始轮询
    poll()
  })
}

/**
 * 保存登录 token 到本地存储
 * @param {string} token satoken 值
 */
export const saveLoginToken = (token) => {
  if (token) {
    uni.setStorageSync('satoken', token)
    // 也可以保存登录时间用于验证 token 过期
    uni.setStorageSync('tokenTime', Date.now())
  }
}

/**
 * 获取本地保存的登录 token
 * @returns {string|null} 返回 satoken 或 null
 */
export const getLoginToken = () => {
  return uni.getStorageSync('satoken') || null
}

/**
 * 清除登录信息（退出登录）
 */
export const clearLoginToken = () => {
  uni.removeStorageSync('satoken')
  uni.removeStorageSync('tokenTime')
  uni.removeStorageSync('qrCodeId')
  uni.removeStorageSync('userInfo')
}

/**
 * 检查 token 是否有效
 * @returns {boolean} token 是否有效
 */
export const isTokenValid = () => {
  const token = getLoginToken()
  if (!token) return false

  // 可以在这里添加 token 过期时间检查
  // 例如：如果超过 24 小时则认为过期
  const tokenTime = uni.getStorageSync('tokenTime')
  if (!tokenTime) return true

  const now = Date.now()
  const maxAge = 24 * 60 * 60 * 1000 // 24 小时
  return now - tokenTime < maxAge
}

/**
 * 刷新或获取当前用户信息
 * @returns {Promise<Object>} 返回用户信息
 */
export const refreshUserInfo = async () => {
  try {
    const res = await service.get('/user/info')
    if (res.code === 200) {
      const userInfo = res.data
      uni.setStorageSync('userInfo', JSON.stringify(userInfo))
      return userInfo
    } else {
      throw new Error('获取用户信息失败')
    }
  } catch (e) {
    console.error('刷新用户信息异常', e)
    return null
  }
}

export default {
  generateQrCode,
  pollQrLoginStatus,
  saveLoginToken,
  getLoginToken,
  clearLoginToken,
  isTokenValid,
  refreshUserInfo,
}
