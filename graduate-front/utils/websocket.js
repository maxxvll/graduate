// utils/websocket.js
class WebSocketClient {
  constructor(options) {
    this.url = options.url // WS服务地址（如wss://xxx.com/ws）
    this.header = options.header || {} // 请求头（如token）
    this.reconnectInterval = options.reconnectInterval || 3000 // 重连间隔（ms）
    this.heartbeatInterval = options.heartbeatInterval || 10000 // 心跳间隔（ms）
    this.maxReconnectTimes = options.maxReconnectTimes || 10 // 最大重连次数
    this.reconnectTimes = 0 // 当前重连次数
    this.wsTask = null // WS任务对象
    this.heartbeatTimer = null // 心跳定时器
    this.reconnectTimer = null // 重连定时器
    this.onMessage = options.onMessage || (() => {}) // 消息监听回调
    this.onOpen = options.onOpen || (() => {}) // 连接成功回调
    this.onClose = options.onClose || (() => {}) // 连接关闭回调
    this.onError = options.onError || (() => {}) // 错误回调
  }

  // 1. 建立WS连接
  connect() {
    // 清空原有定时器
    this.clearTimer()
    // 关闭已有连接
    if (this.wsTask) {
      this.wsTask.close()
    }

    // 创建新连接
    this.wsTask = uni.connectSocket({
      url: this.url,
      header: this.header,
      success: () => {
        console.log('WS连接请求已发送')
      },
      fail: (err) => {
        this.onError(err)
        this.reconnect() // 连接失败则重连
      }
    })

    // 监听连接成功
    this.wsTask.onOpen(() => {
      this.onOpen()
      this.reconnectTimes = 0 // 重置重连次数
      this.startHeartbeat() // 启动心跳
    })

    // 监听消息
    this.wsTask.onMessage((res) => {
      this.onMessage(res.data)
    })

    // 监听连接关闭
    this.wsTask.onClose((res) => {
      this.onClose(res)
      this.clearTimer()
      this.reconnect() // 连接关闭则重连
    })

    // 监听错误
    this.wsTask.onError((err) => {
      this.onError(err)
      this.clearTimer()
      this.reconnect() // 连接错误则重连
    })
  }

  // 2. 启动心跳（定时发送ping，防止连接断开）
  startHeartbeat() {
    this.heartbeatTimer = setInterval(() => {
      this.send({ type: 'ping', data: 'heartbeat' }) // 发送心跳包（格式按服务端要求）
    }, this.heartbeatInterval)
  }

  // 3. 发送消息
  send(data) {
    if (!this.wsTask) {
      uni.$u.toast('WS未连接')
      return
    }
    // 统一转JSON（根据服务端要求调整）
    const sendData = typeof data === 'object' ? JSON.stringify(data) : data
    this.wsTask.send({
      data: sendData,
      fail: (err) => {
        this.onError(err)
      }
    })
  }

  // 4. 自动重连
  reconnect() {
    // 超过最大重连次数则停止
    if (this.reconnectTimes >= this.maxReconnectTimes) {
      uni.$u.toast(`WS重连${this.maxReconnectTimes}次失败，停止重连`)
      this.clearTimer()
      return
    }

    // 启动重连定时器
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimes++
      uni.$u.toast(`WS重连中（${this.reconnectTimes}/${this.maxReconnectTimes}）`)
      this.connect()
    }, this.reconnectInterval)
  }

  // 5. 清空所有定时器
  clearTimer() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  // 6. 主动关闭连接（取消重连）
  close() {
    this.clearTimer()
    this.reconnectTimes = this.maxReconnectTimes // 阻止重连
    if (this.wsTask) {
      this.wsTask.close({ code: 1000, reason: '用户主动关闭' })
      this.wsTask = null
    }
  }
}

// 导出实例（单例模式，全局唯一WS连接）
export const wsClient = new WebSocketClient({
  url: 'wss://echo.websocket.org', // 替换为你的WS服务地址
  header: {
    'Authorization': 'Bearer ' + uni.getStorageSync('token')
  },
  // 消息回调：页面中可重写
  onMessage: (data) => {
    console.log('收到WS消息：', data)
    // 可在这里统一处理消息（如分发到不同页面）
    uni.$emit('wsMessage', data)
  },
  onOpen: () => {
    uni.$u.toast('WS连接成功')
  },
  onClose: (res) => {
    uni.$u.toast('WS连接关闭：' + res.reason)
  },
  onError: (err) => {
    uni.$u.toast('WS错误：' + err.errMsg)
  }
})