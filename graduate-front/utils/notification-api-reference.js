/**
 * 通知 API 参考文档
 * 来源: crawler 团队收集
 * 用途: 任务 #10 (PC 网页后台消息通知) 和 任务 #8 (移动端息屏/后台消息接收)
 */

// ============================================
// Web Notification API (H5 桌面通知)
// ============================================
// 适用平台: H5 (桌面浏览器)
// 权限: 需要用户授权

/**
 * 请求通知权限
 * @returns {Promise<string>} 权限状态: 'granted' | 'denied' | 'default'
 */
export const requestNotificationPermission = () => {
  if (!('Notification' in window)) {
    return Promise.resolve('unsupported')
  }
  return Notification.requestPermission()
}

/**
 * 检查通知权限状态
 * @returns {string} 'granted' | 'denied' | 'default' | 'unsupported'
 */
export const getNotificationPermission = () => {
  if (!('Notification' in window)) {
    return 'unsupported'
  }
  return Notification.permission
}

/**
 * 显示桌面通知
 * @param {Object} options - 通知选项
 * @param {string} options.title - 通知标题
 * @param {string} options.body - 通知内容
 * @param {string} options.icon - 通知图标 URL
 * @param {string} options.tag - 唯一标识，相同 tag 会覆盖旧通知
 * @param {*} options.data - 自定义数据
 * @param {Function} options.onClick - 点击回调
 */
export const showDesktopNotification = (options) => {
  if (!('Notification' in window) || Notification.permission !== 'granted') {
    return null
  }

  const notification = new Notification(options.title, {
    body: options.body,
    icon: options.icon,
    tag: options.tag,
    data: options.data,
  })

  if (options.onClick) {
    notification.onclick = () => {
      options.onClick(notification)
      notification.close()
    }
  }

  // 自动关闭（可选）
  if (options.autoClose !== false) {
    setTimeout(() => notification.close(), options.duration || 5000)
  }

  return notification
}

// ============================================
// plus.push 本地通知 (App 平台)
// ============================================
// 适用平台: App (iOS/Android)
// 配置: 需要在 manifest.json 中配置 Push 权限

/**
 * 创建本地推送消息 (App 平台)
 * @param {Object} options - 推送选项
 * @param {string} options.content - 消息内容
 * @param {string|Object} options.payload - 自定义数据
 * @param {string} options.title - 消息标题
 * @param {boolean} options.cover - 是否覆盖上一条消息
 * @param {number} options.delay - 延迟发送时间（秒）
 * @param {string} options.icon - 消息图标
 * @param {string} options.sound - 提示音
 * @param {number} options.when - 消息显示时间
 */
export const createLocalPushMessage = (options) => {
  // #ifdef APP-PLUS
  if (typeof plus === 'undefined' || !plus.push) {
    console.warn('[Notification] plus.push not available')
    return
  }

  plus.push.createMessage(
    options.content || '',
    options.payload || {},
    {
      title: options.title,
      cover: options.cover !== false,
      delay: options.delay || 0,
      icon: options.icon,
      sound: options.sound,
      when: options.when,
    }
  )
  // #endif
}

/**
 * 监听推送消息点击事件 (App 平台)
 * @param {Function} callback - 点击回调函数
 */
export const onPushMessageClick = (callback) => {
  // #ifdef APP-PLUS
  if (typeof plus !== 'undefined' && plus.push) {
    plus.push.addEventListener('click', (msg) => {
      callback({
        content: msg.content,
        payload: msg.payload || msg.extra,
        payloadString: msg.payload,
      })
    })
  }
  // #endif
}

// ============================================
// uni.createPushMessage (推荐新 API)
// ============================================
// 适用平台: App, 小程序 (部分)
// 说明: UniApp 统一推送消息 API

/**
 * 创建推送消息 (UniApp 统一 API)
 * @param {Object} options - 推送选项
 * @param {string} options.title - 主标题
 * @param {string} options.content - 消息内容
 * @param {boolean} options.cover - 是否覆盖上一条
 * @param {number} options.delay - 延迟显示时间
 * @param {string} options.sound - 提示音 (system|none)
 * @param {Object|string} options.payload - 自定义数据
 */
export const createUniPushMessage = (options) => {
  uni.createPushMessage({
    title: options.title || '',
    content: options.content || '',
    cover: options.cover !== false,
    delay: options.delay || 0,
    sound: options.sound || 'system',
    payload: options.payload || {},
    success: options.onSuccess,
    fail: options.onFail,
    complete: options.onComplete,
  })
}

// ============================================
// uni.setAppBadgeNumber (角标)
// ============================================
// 适用平台: iOS, 部分 Android

/**
 * 设置应用角标数字
 * @param {number} count - 角标数字，0 表示清空
 */
export const setAppBadge = (count) => {
  uni.setAppBadgeNumber({
    index: count,
  })
}

/**
 * 清空应用角标
 */
export const clearAppBadge = () => {
  uni.setAppBadgeNumber({
    index: 0,
  })
}

// ============================================
// 统一通知管理器接口
// ============================================

/**
 * 通知管理器
 * 根据平台自动选择最佳通知方案
 */
class NotificationManager {
  constructor() {
    this.platform = this.detectPlatform()
    this.permission = 'default'
    this.init()
  }

  detectPlatform() {
    // #ifdef H5
    return 'h5'
    // #endif
    // #ifdef APP-PLUS
    return 'app'
    // #endif
    // #ifdef MP-WEIXIN
    return 'mp-weixin'
    // #endif
    return 'unknown'
  }

  async init() {
    if (this.platform === 'h5') {
      this.permission = await requestNotificationPermission()
    } else if (this.platform === 'app') {
      // App 平台通常默认有权限
      this.permission = 'granted'
    }
  }

  /**
   * 显示通知
   * @param {Object} options - 通知选项
   * @returns {Promise<boolean>} 是否成功
   */
  async notify(options) {
    if (this.permission !== 'granted') {
      console.warn('[NotificationManager] Permission not granted')
      return false
    }

    try {
      if (this.platform === 'h5') {
        showDesktopNotification(options)
        return true
      } else if (this.platform === 'app') {
        createUniPushMessage(options)
        return true
      }
      return false
    } catch (error) {
      console.error('[NotificationManager] Failed to show notification:', error)
      return false
    }
  }

  /**
   * 设置角标
   * @param {number} count - 角标数字
   */
  setBadge(count) {
    setAppBadge(count)
  }

  /**
   * 清空角标
   */
  clearBadge() {
    clearAppBadge()
  }
}

export default new NotificationManager()
