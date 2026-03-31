/**
 * 通知管理器
 * H5: Web Notification API + 声音 + 角标
 * App: 由 local-notification.js 处理
 */

const noop = () => {}
const noopAsync = async () => false

// H5 平台使用的变量和函数
let permissionStatus = 'default'
let audioInstance = null
let badgeCount = 0

const requestPermission = async () => {
  // #ifdef H5
  if (typeof Notification === 'undefined') return false
  if (Notification.permission === 'granted') {
    permissionStatus = 'granted'
    return true
  }
  if (Notification.permission === 'denied') {
    permissionStatus = 'denied'
    return false
  }
  try {
    const result = await Notification.requestPermission()
    permissionStatus = result
    return result === 'granted'
  } catch (e) {
    console.warn('[NotificationManager] requestPermission failed', e)
    return false
  }
  // #endif
  // #ifndef H5
  return false
  // #endif
}

const isSupported = () => {
  // #ifdef H5
  return typeof Notification !== 'undefined'
  // #endif
  // #ifndef H5
  return false
  // #endif
}

const showNotification = ({ title, body, icon, tag, sessionId, onClick }) => {
  // #ifdef H5
  if (typeof Notification === 'undefined' || permissionStatus !== 'granted') return
  if (!document.hidden) return

  const notification = new Notification(title, {
    body: body || '',
    icon: icon || '/static/logo.png',
    tag: tag || `chat-${sessionId || Date.now()}`,
  })

  notification.onclick = () => {
    window.focus()
    if (typeof onClick === 'function') {
      onClick(sessionId)
    } else if (sessionId) {
      uni.$emit('notificationClick', { sessionId })
    }
    notification.close()
  }

  setTimeout(() => {
    try { notification.close() } catch (e) {}
  }, 5000)
  // #endif
}

const playNotificationSound = () => {
  // #ifdef H5
  try {
    if (!audioInstance) {
      audioInstance = new Audio('/static/notification.mp3')
      audioInstance.volume = 0.5
    }
    audioInstance.currentTime = 0
    audioInstance.play().catch(() => {})
  } catch (e) {}
  // #endif
}

const updateTitleBadge = () => {
  // #ifdef H5
  const appName = '毕业设计聊天应用'
  if (badgeCount > 0) {
    document.title = `(${badgeCount > 99 ? '99+' : badgeCount}) ${appName}`
  } else {
    document.title = appName
  }
  // #endif
}

const incrementBadge = (count = 1) => {
  badgeCount += count
  updateTitleBadge()
}

const setBadge = (count = 0) => {
  badgeCount = Math.max(0, Number(count) || 0)
  updateTitleBadge()
}

const clearBadge = () => {
  badgeCount = 0
  updateTitleBadge()
}

const destroy = () => {
  badgeCount = 0
  updateTitleBadge()
  // #ifdef H5
  if (audioInstance) {
    audioInstance.pause()
    audioInstance = null
  }
  // #endif
}

export default {
  requestPermission,
  isSupported,
  showNotification,
  playNotificationSound,
  incrementBadge,
  setBadge,
  clearBadge,
  destroy,
}
