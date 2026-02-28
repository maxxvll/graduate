import axios from 'axios'
import { adapter } from 'axios-adapter-uniapp'

// ========== 设备类型识别 ==========
const getDeviceType = () => {
  try {
    const systemInfo = uni.getSystemInfoSync()
    if (systemInfo.miniProgram?.envVersion) return 'mp-weixin'
    switch (systemInfo.platform) {
      case 'android': return 'android'
      case 'ios': return 'ios'
      case 'windows':
      case 'mac': return 'pc'
      default: return 'h5'
    }
  } catch (error) {
    return 'unknown'
  }
}

// 创建axios实例
const service = axios.create({
  baseURL: 'http://127.0.0.1:5050/api',
  timeout: 10000,
  adapter: adapter
})

service.interceptors.request.use(
  (config) => {
    // 1. 【核心修复】读取 satoken（匹配后端配置）
    const token = uni.getStorageSync('satoken') // 注意这里改成 'satoken'
    if (token) {
      config.headers['satoken'] = token
    }

    // 2. 如果是 FormData，直接跳过
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
      return config
    }

    // 3. 普通 JSON 请求处理
    if (config.method === 'post') {
      config.headers['Content-Type'] = 'application/json'
    }

    if (['post', 'put', 'patch'].includes(config.method) && config.data) {
      let requestData = config.data
      if (typeof requestData === 'string') {
        try { requestData = JSON.parse(requestData) } catch (e) { return config }
      }
      if (!requestData.deviceType) {
        requestData.deviceType = getDeviceType()
      }
      config.data = JSON.stringify(requestData)
    }

    return config
  },
  (error) => {
    uni.$u.toast('请求参数错误')
    return Promise.reject(error)
  }
)

// ========== 响应拦截器 ==========
service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      uni.$u.toast(res.message || res.msg || '请求失败')
      if (res.code === 401) {
        uni.removeStorageSync('satoken')
        uni.redirectTo({ url: '/pages/login/login' })
      }
      return Promise.reject(res)
    }
    return res
  },
  (error) => {
    uni.$u.toast('网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export default service