/**
 * 跨平台上传工具 - 统一处理 FormData 在不同环境的兼容性
 * 
 * 特别处理：
 * - H5 Web: 使用 Fetch API + FormData
 * - 原生 App/小程序: 使用 uni.uploadFile API
 * 
 * 避免直接使用 FormData，此文件提供安全的上传接口
 */

import { API_CONFIG } from './config'
import service from './request'

const BASE_URL = String(API_CONFIG.BASE_URL || '').replace(/\/+$/, '')

/**
 * 检查是否为 H5 环境（支持 FormData）
 */
const isH5Environment = () => {
  try {
    const systemInfo = uni.getSystemInfoSync()
    return systemInfo.platform === 'web' || systemInfo.platform === 'h5'
  } catch (e) {
    return false
  }
}

/**
 * 安全创建 FormData（仅在 H5 环境）
 * @returns {FormData|null}
 */
const createFormData = () => {
  if (typeof FormData === 'undefined') {
    return null
  }
  try {
    return new FormData()
  } catch (e) {
    console.error('创建 FormData 失败:', e)
    return null
  }
}

/**
 * 强大的跨平台上传工具
 * 
 * @param {string} url - 上传接口 URL 路径（不含 BASE_URL）
 * @param {File|Blob|string} file - 文件对象或文件路径
 * @param {object} options - 配置选项
 *   - name: 文件字段名（默认 'file'）
 *   - formData: 额外的表单数据
 *   - onProgress: 上传进度回调
 * 
 * @returns {Promise} 返回 {code, data, message} 统一格式
 */
const uploadFile = async (url, file, options = {}) => {
  const {
    name = 'file',
    formData = {},
    onProgress = null,
  } = options

  if (!file) {
    throw new Error('文件不存在')
  }

  const token = uni.getStorageSync('satoken')
  const headers = {}
  if (token) {
    headers['satoken'] = token
  }

  // ========== H5 环境: 使用 Fetch API ==========
  // #ifdef H5
  if (typeof FormData !== 'undefined') {
    try {
      console.log(`[H5上传] GET ${BASE_URL}${url}`)
      const fd = new FormData()
      
      // 添加文件
      if (file instanceof File || file instanceof Blob) {
        fd.append(name, file)
      } else if (typeof file === 'string') {
        // 如果是路径字符串（H5中少见），直接失败提示
        throw new Error('H5 环境不支持文件路径，请使用 File 对象')
      }

      // 添加额外字段
      Object.entries(formData).forEach(([key, value]) => {
        fd.append(key, value)
      })

      const response = await fetch(`${BASE_URL}${url}`, {
        method: 'POST',
        headers: headers,
        body: fd,
        // 注意：不设置 Content-Type，让浏览器自动设置
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      const data = await response.json()
      if (data.code !== 200) {
        throw new Error(data.message || '上传失败')
      }

      return data
    } catch (error) {
      console.error('[H5上传错误]', error)
      throw error
    }
  }
  // #endif

  // ========== 原生 App/小程序: 使用 uni.uploadFile ==========
  return new Promise((resolve, reject) => {
    try {
      // 如果 file 是 File/Blob 对象而不是路径，需要转换
      // 在真实的原生 App 环境中，文件应该通过选择器获得，已经是路径了
      let filePath = file

      if (typeof file === 'object' && (file instanceof File || file instanceof Blob)) {
        // 如果收到了 File/Blob 对象但在 App 环境，需要特殊处理
        // 这种情况一般不会发生，但为了安全起见要处理
        console.warn('[App上传] 收到 File/Blob 对象，需要使用选择器获得路径')
        reject(new Error('原生 App 环境需要使用文件选择器，不支持直接 File 对象'))
        return
      }

      console.log(`[App上传] POST ${BASE_URL}${url}`)

      uni.uploadFile({
        url: `${BASE_URL}${url}`,
        filePath: filePath,
        name: name,
        formData: formData,
        header: headers,
        success: (response) => {
          try {
            // 解析响应数据
            let data
            if (typeof response.data === 'string') {
              try {
                data = JSON.parse(response.data)
              } catch (e) {
                console.error('响应数据解析失败:', response.data)
                throw new Error('服务端返回数据格式错误')
              }
            } else {
              data = response.data
            }

            // 检查响应状态
            if (response.statusCode === 200 && data && data.code === 200) {
              resolve(data)
            } else {
              const message = data?.message || data?.msg || `上传失败 (HTTP ${response.statusCode})`
              reject(new Error(message))
            }
          } catch (e) {
            reject(e)
          }
        },
        fail: (error) => {
          console.error('[App上传失败]', error)
          reject(new Error(error.errMsg || '上传失败'))
        },
        complete: () => {
          // 上传完成
        },
      })
    } catch (error) {
      reject(error)
    }
  })
}

/**
 * 发送文件消息（当前会话）
 * 在所有环境下都能正常工作
 */
const sendMessageWithFile = async (
  url,
  file,
  messageData = {},
  options = {}
) => {
  // 只有在 H5 或支持 FormData 的环境才能这样做
  const supportsFormData = typeof FormData !== 'undefined'

  if (supportsFormData) {
    // H5: 用 FormData 一次性发送消息 + 文件
    const fd = new FormData()
    fd.append('sendDTO', JSON.stringify(messageData))
    
    if (file instanceof File || file instanceof Blob) {
      fd.append('files', file)
    }

    return service.post(url, fd)
  } else {
    // App: 先上传文件，再发送消息
    console.log('[App发送消息] 分两步：1)上传文件 2)发送消息')
    
    // 步骤 1: 上传文件
    const uploadRes = await uploadFile('/chat/file/upload', file, {
      name: 'file',
      formData: {},
    })

    const fileUrl = uploadRes?.data?.fileUrl || uploadRes?.data?.url || null

    // 步骤 2: 发送消息（带文件 URL）
    const finalData = {
      ...messageData,
      fileUrl: fileUrl,
    }

    return service.post(url, finalData)
  }
}

/**
 * 导出所有工具
 */
export {
  isH5Environment,
  createFormData,
  uploadFile,
  sendMessageWithFile,
}

export default {
  isH5Environment,
  createFormData,
  uploadFile,
  sendMessageWithFile,
}
