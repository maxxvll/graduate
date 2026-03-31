import { getWebSocketUrl, log } from './config'
import { showToast, getDeviceType } from './common'
import { getDeviceId } from './device'

class WebSocketClient {
  constructor(options = {}) {
    this.urlFactory = options.urlFactory || (() => '')
    this.reconnectInterval = options.reconnectInterval || 3000
    this.heartbeatInterval = options.heartbeatInterval || 10000
    this.maxReconnectTimes = options.maxReconnectTimes || 10
    this.wsTask = null
    this.heartbeatTimer = null
    this.reconnectTimer = null
    this.reconnectTimes = 0
    this.isReconnecting = false
    this.isManualClose = false
    this.messageQueue = []
    this.maxQueueSize = 100
    this.onOpen = () => {}
    this.onClose = () => {}
    this.onError = () => {}
    this.messageHandlers = new Set()
    // 可见性感知相关状态
    this.isPageVisible = true
    this.visibilityHandler = null
    this.appShowHandler = null
    this.appHideHandler = null
    this.visibilityHandlers = new Set() // 可见性变化监听器

    // 自动初始化可见性监听
    this.setupVisibilityListeners()
  }

  setHandlers(handlers = {}) {
    this.onOpen = handlers.onOpen || (() => {})
    this.onClose = handlers.onClose || (() => {})
    this.onError = handlers.onError || (() => {})
  }

  // 初始化可见性监听
  setupVisibilityListeners() {
    if (this.visibilityHandler || this.appShowHandler) {
      return // 已设置监听
    }

    // H5 环境：使用 Page Visibility API
    // #ifdef H5
    if (typeof document !== 'undefined') {
      this.visibilityHandler = () => {
        this.isPageVisible = !document.hidden
        log.debug('WebSocket', `Page visibility changed: ${this.isPageVisible ? 'visible' : 'hidden'}`)

        if (this.isPageVisible) {
          this.handlePageVisible()
        } else {
          this.handlePageHidden()
        }
      }
      document.addEventListener('visibilitychange', this.visibilityHandler)
      log.debug('WebSocket', 'Visibility listener added (H5)')
    }
    // #endif

    // 小程序/App 环境：使用 uni.onAppShow/uni.onAppHide
    // #ifdef MP-WEIXIN || MP-ALIPAY || MP-BAIDU || MP-TOUTIAO || MP-QQ || APP-PLUS
    this.appShowHandler = () => {
      this.isPageVisible = true
      log.debug('WebSocket', 'App shown')
      this.handlePageVisible()
    }
    this.appHideHandler = () => {
      this.isPageVisible = false
      log.debug('WebSocket', 'App hidden')
      this.handlePageHidden()
    }
    uni.onAppShow(this.appShowHandler)
    uni.onAppHide(this.appHideHandler)
    log.debug('WebSocket', 'App visibility listeners added (Native)')
    // #endif
  }

  // 移除可见性监听
  removeVisibilityListeners() {
    // #ifdef H5
    if (this.visibilityHandler && typeof document !== 'undefined') {
      document.removeEventListener('visibilitychange', this.visibilityHandler)
      this.visibilityHandler = null
      log.debug('WebSocket', 'Visibility listener removed (H5)')
    }
    // #endif

    // #ifdef MP-WEIXIN || MP-ALIPAY || MP-BAIDU || MP-TOUTIAO || MP-QQ || APP-PLUS
    if (this.appShowHandler && this.appHideHandler) {
      uni.offAppShow(this.appShowHandler)
      uni.offAppHide(this.appHideHandler)
      this.appShowHandler = null
      this.appHideHandler = null
      log.debug('WebSocket', 'App visibility listeners removed (Native)')
    }
    // #endif
  }

  // 页面变为可见时的处理
  handlePageVisible() {
    log.debug('WebSocket', 'Handling page visible')

    // 通知所有可见性监听器
    this.visibilityHandlers.forEach((handler) => {
      try {
        handler({ visible: true })
      } catch (error) {
        console.error('[WebSocket] Visibility handler failed', error)
      }
    })

    // 如果 WebSocket 未连接且有 token，尝试重连
    if (!this.wsTask) {
      const token = uni.getStorageSync('satoken')
      if (token) {
        log.debug('WebSocket', 'Reconnecting after page visible')
        this.reconnectTimes = 0 // 重置重连计数
        this.connect()
      }
    } else {
      // 连接存在，发送心跳确保活跃
      log.debug('WebSocket', 'Sending heartbeat after page visible')
      this.send({
        type: 'ping',
        data: 'heartbeat-resume',
      })
    }

    // 刷新消息队列（防止在后台时消息丢失）
    if (this.messageQueue.length > 0) {
      log.debug('WebSocket', `Flushing ${this.messageQueue.length} queued messages after resume`)
      // 不立即刷新，等待连接稳定
      setTimeout(() => this.flushMessageQueue(), 500)
    }
  }

  // 页面变为隐藏时的处理
  handlePageHidden() {
    log.debug('WebSocket', 'Handling page hidden')

    // 通知所有可见性监听器
    this.visibilityHandlers.forEach((handler) => {
      try {
        handler({ visible: false })
      } catch (error) {
        console.error('[WebSocket] Visibility handler failed', error)
      }
    })

    // 页面隐藏时，可以选择性地暂停某些操作
    // 但不关闭连接，以便接收后台消息
    // 心跳继续运行以保持连接活跃

    // 如果长期无操作，后端可能会断开连接，恢复时会自动重连
  }

  addMessageListener(listener) {
    if (typeof listener === 'function') {
      this.messageHandlers.add(listener)
    }
  }

  removeMessageListener(listener) {
    this.messageHandlers.delete(listener)
  }

  // 添加可见性变化监听器
  addVisibilityListener(listener) {
    if (typeof listener === 'function') {
      this.visibilityHandlers.add(listener)
    }
  }

  // 移除可见性变化监听器
  removeVisibilityListener(listener) {
    this.visibilityHandlers.delete(listener)
  }

  connect() {
    this.setupVisibilityListeners()

    if (this.wsTask) {
      return
    }

    const token = uni.getStorageSync('satoken')
    if (!token) {
      return
    }

    this.isManualClose = false
    const url = this.urlFactory(token)
    const queryParams = []
    if (url.indexOf('deviceType=') < 0) {
      queryParams.push(`deviceType=${encodeURIComponent(getDeviceType())}`)
    }
    if (url.indexOf('deviceId=') < 0) {
      queryParams.push(`deviceId=${encodeURIComponent(getDeviceId())}`)
    }
    const currentUrl =
      queryParams.length > 0 ? `${url}${url.includes('?') ? '&' : '?'}${queryParams.join('&')}` : url

    this.wsTask = uni.connectSocket({
      url: currentUrl,
      multiple: true,
      success: () => {
        log.debug('WebSocket', 'Connect requested', { url: currentUrl })
      },
      fail: (error) => {
        this.cleanupSocket(false)
        this.onError(error)
        // 连接失败时也要递增计数
        this.reconnectTimes += 1
        if (this.reconnectTimes <= this.maxReconnectTimes) {
          this.scheduleReconnect()
        }
      },
    })

    this.bindSocketEvents()
  }

  bindSocketEvents() {
    if (!this.wsTask) {
      return
    }

    this.wsTask.onOpen(() => {
      this.reconnectTimes = 0
      this.isReconnecting = false
      this.startHeartbeat()
      this.flushMessageQueue()
      this.onOpen()
    })

    this.wsTask.onMessage((event) => {
      const payload = this.parseMessage(event?.data)

      if (payload?.type === 'pong') {
        return
      }

      this.messageHandlers.forEach((handler) => {
        try {
          handler(payload)
        } catch (error) {
          console.error('[WebSocket] Message handler failed', error)
        }
      })
    })

    this.wsTask.onClose((event) => {
      this.cleanupSocket(false)
      this.onClose(event)
      if (!this.isManualClose) {
        this.scheduleReconnect()
      }
    })

    this.wsTask.onError((error) => {
      this.cleanupSocket(false)
      this.onError(error)
      if (!this.isManualClose) {
        this.scheduleReconnect()
      }
    })
  }

  parseMessage(data) {
    if (typeof data !== 'string') {
      return data
    }

    try {
      return JSON.parse(data)
    } catch {
      return data
    }
  }

  startHeartbeat() {
    this.clearHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      this.send({
        type: 'ping',
        data: 'heartbeat',
      })
    }, this.heartbeatInterval)
  }

  clearHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  send(payload) {
    if (!this.wsTask) {
      this.enqueueMessage(payload)
      this.connect()
      return false
    }

    try {
      const data = typeof payload === 'string' ? payload : JSON.stringify(payload)
      this.wsTask.send({
        data,
        fail: () => {
          this.enqueueMessage(payload)
          this.scheduleReconnect()
        },
      })
      return true
    } catch (error) {
      console.error('[WebSocket] Failed to send message', error)
      this.enqueueMessage(payload)
      this.scheduleReconnect()
      return false
    }
  }

  enqueueMessage(payload) {
    if (this.messageQueue.length >= this.maxQueueSize) {
      this.messageQueue.shift()
    }

    this.messageQueue.push(payload)
  }

  flushMessageQueue() {
    if (!this.messageQueue.length) {
      return
    }

    const queued = [...this.messageQueue]
    this.messageQueue = []
    queued.forEach((item, index) => {
      setTimeout(() => {
        this.send(item)
      }, index * 80)
    })
  }

  scheduleReconnect() {
    if (this.isReconnecting || this.isManualClose) {
      return
    }

    // Check if token still exists - if not, stop reconnection
    const token = uni.getStorageSync('satoken')
    if (!token) {
      console.warn('[WebSocket] Token expired, stopping reconnection')
      this.cleanupSocket()
      // Import and redirect to login
      import('./error-handler').then(({ handleAuthError }) => {
        handleAuthError()
      })
      return
    }

    if (this.reconnectTimes >= this.maxReconnectTimes) {
      showToast('WebSocket 重连失败，请稍后再试')
      return
    }

    this.isReconnecting = true
    // 正确：先递增计数，然后显示，再发起连接
    this.reconnectTimes += 1
    showToast(`WS 重连中（${this.reconnectTimes}/${this.maxReconnectTimes}）`)

    this.reconnectTimer = setTimeout(() => {
      this.isReconnecting = false
      this.connect()
    }, this.reconnectInterval)
  }

  cleanupSocket(resetManualClose = true) {
    this.clearHeartbeat()

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    if (resetManualClose) {
      this.isManualClose = false
    }

    this.isReconnecting = false
    this.wsTask = null
  }

  close() {
    this.isManualClose = true
    this.clearHeartbeat()
    this.removeVisibilityListeners()

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    if (this.wsTask && typeof this.wsTask.close === 'function') {
      try {
        this.wsTask.close({
          code: 1000,
          reason: 'manual-close',
        })
      } catch (error) {
        console.warn('[WebSocket] Failed to close socket', error)
      }
    }

    this.wsTask = null
    this.isReconnecting = false
    this.reconnectTimes = 0
  }
}

export const wsClient = new WebSocketClient({
  urlFactory: (token) => getWebSocketUrl(token),
})

export default wsClient
