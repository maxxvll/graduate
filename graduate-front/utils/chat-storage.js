class ChatStorage {
  constructor() {
    this.dbName = 'chatDB'
    this.dbPath = '_doc/chat.db'
    this.isApp = false
    // 统一使用一个 Storage Key，避免分 Key 导致的查询混乱
    this.UNIFIED_STORAGE_KEY = 'chat_unified_message_history'
    // 每个会话最多缓存的消息条数，防止 Storage 超出 5MB 上限
    this.MAX_MESSAGES_PER_SESSION = 200
    this.init()
  }

  // 初始化
  init() {
    // #ifdef APP-PLUS
    this.isApp = true
    plus.sqlite.openDatabase({
      name: this.dbName,
      path: this.dbPath,
      success: () => {
        console.log('ChatStorage: App端数据库打开成功')
        this.createTable()
      },
      fail: (err) => {
        console.error('ChatStorage: App端数据库打开失败，降级为Storage', err)
        this.isApp = false
      }
    })
    // #endif
  }

  // App端表结构：增加完整字段，支持文件/语音/图片
  createTable() {
    if (!this.isApp) return
    plus.sqlite.executeSql({
      name: this.dbName,
      sql: `CREATE TABLE IF NOT EXISTS messages (
        id TEXT PRIMARY KEY,
        messageNo TEXT,
        session_id TEXT,
        session_type INTEGER,
        sender_id TEXT,
        receiver_id TEXT,
        message_type INTEGER,
        content TEXT,
        send_time TEXT,
        file_url TEXT,
        file_name TEXT,
        file_size INTEGER,
        file_type TEXT,
        duration INTEGER,
        avatar TEXT,
        send_status TEXT,
        extra_json TEXT
      )`,
      success: () => console.log('ChatStorage: 消息表创建成功'),
      fail: (err) => console.error('ChatStorage: 创建表失败', err)
    })
  }

  // 统一字段名，兼容驼峰和下划线，完整保存所有字段
  _normalizeMessage(msg) {
    if (!msg) return null
    // 兼容 sessionId 和 session_id
    const sessionId = msg.sessionId || msg.session_id
    return {
      id: String(msg.id || msg.messageId || Date.now()),
      messageNo: msg.messageNo || '',
      session_id: String(sessionId || ''), // 【核心】强制转字符串
      session_type: msg.sessionType || msg.session_type || 1,
      sender_id: msg.senderId || msg.sender_id || msg.sender || '',
      receiver_id: msg.receiverId || msg.receiver_id || '',
      message_type: Number(msg.messageType || msg.message_type || 1),
      content: msg.content || '',
      send_time: msg.sendTime || msg.send_time || msg.time || new Date().toISOString(),
      file_url: msg.fileUrl || msg.file_url || '',
      file_name: msg.fileName || msg.file_name || '',
      file_size: msg.fileSize || msg.file_size || 0,
      file_type: msg.fileType || msg.file_type || '',
      duration: msg.duration ? Number(msg.duration) : null,
      avatar: msg.avatar || '',
      send_status: msg.send_status || 'success',
      // 把其他可能的字段存到 extra_json 里，防止丢失
      extra_json: JSON.stringify(msg)
    }
  }

  // 1. 插入单条消息（核心方法）
  insertMessage(msg) {
    return new Promise((resolve, reject) => {
      try {
        const normalizedMsg = this._normalizeMessage(msg)
        if (!normalizedMsg.session_id) {
          console.error('ChatStorage: 消息缺少 session_id，无法存储', msg)
          return reject(new Error('缺少 session_id'))
        }

        // #ifdef APP-PLUS
        if (this.isApp) {
          plus.sqlite.executeSql({
            name: this.dbName,
            sql: `INSERT OR REPLACE INTO messages 
              (id, messageNo, session_id, session_type, sender_id, receiver_id, message_type, content, send_time, file_url, file_name, file_size, file_type, duration, avatar, send_status, extra_json) 
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
            args: [
              normalizedMsg.id, normalizedMsg.messageNo, normalizedMsg.session_id, normalizedMsg.session_type,
              normalizedMsg.sender_id, normalizedMsg.receiver_id, normalizedMsg.message_type, normalizedMsg.content,
              normalizedMsg.send_time, normalizedMsg.file_url, normalizedMsg.file_name, normalizedMsg.file_size,
              normalizedMsg.file_type, normalizedMsg.duration, normalizedMsg.avatar, normalizedMsg.send_status,
              normalizedMsg.extra_json
            ],
            success: () => {
              console.log('ChatStorage: App端消息插入成功', normalizedMsg.id)
              resolve(true)
            },
            fail: (err) => {
              console.error('ChatStorage: App端插入失败', err)
              reject(err)
            }
          })
          return
        }
        // #endif

        // H5/小程序端：统一存储，完整保存
        const allMessages = this._getAllStorageMessages()
        // 去重：messageNo 为空时不参与匹配，防止空值误匹配
        const existIndex = allMessages.findIndex(item =>
          item.id === normalizedMsg.id ||
          (normalizedMsg.messageNo && item.messageNo === normalizedMsg.messageNo)
        )
        if (existIndex !== -1) {
          allMessages[existIndex] = { ...allMessages[existIndex], ...normalizedMsg }
        } else {
          allMessages.push(normalizedMsg)
        }
        // 写入本地磁盘（真正的持久化）
        uni.setStorageSync(this.UNIFIED_STORAGE_KEY, JSON.stringify(allMessages))
        console.log('ChatStorage: Storage端消息插入成功', normalizedMsg.id)
        resolve(true)
      } catch (err) {
        console.error('ChatStorage: 插入消息异常', err)
        reject(err)
      }
    })
  }

  // 2. 批量插入消息（H5 端 O(N) 单次读写优化，App 端沿用逐条 SQLite 插入）
  insertMessages(sessionId, msgs) {
    if (!msgs || msgs.length === 0) return Promise.resolve([])

    // #ifdef APP-PLUS
    if (this.isApp) {
      return Promise.all(msgs.map(msg => this.insertMessage({ ...msg, session_id: sessionId })))
    }
    // #endif

    // H5/小程序端：批量 upsert，全程只读写一次 Storage
    return new Promise((resolve, reject) => {
      try {
        const normalizedList = msgs
          .map(msg => this._normalizeMessage({ ...msg, session_id: sessionId }))
          .filter(m => m && m.session_id)
        if (normalizedList.length === 0) return resolve([])
        this._batchUpsertStorage(normalizedList)
        resolve(normalizedList)
      } catch (err) {
        console.error('ChatStorage: 批量插入失败', err)
        reject(err)
      }
    })
  }

  // 3. 查询指定会话的所有消息（核心方法）
  queryMessages(sessionId) {
    return new Promise((resolve, reject) => {
      try {
        const targetSessionId = String(sessionId)
        console.log('ChatStorage: 查询会话消息，sessionId:', targetSessionId)

        // #ifdef APP-PLUS
        if (this.isApp) {
          plus.sqlite.selectSql({
            name: this.dbName,
            sql: `SELECT * FROM messages WHERE session_id = ? ORDER BY send_time ASC`,
            args: [targetSessionId],
            success: (res) => {
              const list = (res.data || []).map(item => {
                try { return { ...JSON.parse(item.extra_json), ...item } } 
                catch (e) { return item }
              })
              console.log('ChatStorage: App端查询到消息', list.length, '条')
              resolve(list)
            },
            fail: (err) => {
              console.error('ChatStorage: App端查询失败', err)
              reject(err)
            }
          })
          return
        }
        // #endif

        // H5/小程序端：从统一存储中过滤
        const allMessages = this._getAllStorageMessages()
        const sessionMessages = allMessages.filter(item => {
          return String(item.session_id) === targetSessionId
        }).sort((a, b) => {
          return new Date(a.send_time) - new Date(b.send_time)
        })
        
        console.log('ChatStorage: Storage端查询到消息', sessionMessages.length, '条')
        resolve(sessionMessages)
      } catch (err) {
        console.error('ChatStorage: 查询消息异常', err)
        reject(err)
      }
    })
  }

  // 4. 删除指定会话的所有消息
  deleteSessionMessages(sessionId) {
    return new Promise((resolve, reject) => {
      try {
        const targetSessionId = String(sessionId)
        
        // #ifdef APP-PLUS
        if (this.isApp) {
          plus.sqlite.executeSql({
            name: this.dbName,
            sql: `DELETE FROM messages WHERE session_id = ?`,
            args: [targetSessionId],
            success: () => resolve(true),
            fail: (err) => reject(err)
          })
          return
        }
        // #endif

        // H5/小程序端
        const allMessages = this._getAllStorageMessages().filter(item => {
          return String(item.session_id) !== targetSessionId
        })
        uni.setStorageSync(this.UNIFIED_STORAGE_KEY, JSON.stringify(allMessages))
        resolve(true)
      } catch (err) {
        reject(err)
      }
    })
  }

  // ─────────────────────────────────────────────────────────────
  // 内部辅助方法
  // ─────────────────────────────────────────────────────────────

  // 获取所有消息（容错处理损坏的 JSON）
  _getAllStorageMessages() {
    try {
      const raw = uni.getStorageSync(this.UNIFIED_STORAGE_KEY)
      if (!raw) return []
      const parsed = JSON.parse(raw)
      return Array.isArray(parsed) ? parsed : []
    } catch (e) {
      console.error('ChatStorage: 读取Storage失败，已重置缓存', e)
      // JSON 损坏时清除防止持续报错
      try { uni.removeStorageSync(this.UNIFIED_STORAGE_KEY) } catch (_) {}
      return []
    }
  }

  /**
   * H5 端批量 upsert：用 Map 索引实现 O(1) 查找，全程只读写一次 Storage
   * @param {Array} normalizedList - 已经过 _normalizeMessage 处理的消息列表
   */
  _batchUpsertStorage(normalizedList) {
    if (!normalizedList || normalizedList.length === 0) return
    const allMessages = this._getAllStorageMessages()

    // 构建 id/messageNo 双索引，O(1) 查重
    const idMap = new Map()
    const msgNoMap = new Map()
    allMessages.forEach((item, idx) => {
      if (item.id) idMap.set(String(item.id), idx)
      if (item.messageNo) msgNoMap.set(item.messageNo, idx)
    })

    for (const normalized of normalizedList) {
      const byId = idMap.get(String(normalized.id))
      const byMsgNo = normalized.messageNo ? msgNoMap.get(normalized.messageNo) : undefined
      const existIdx = byId !== undefined ? byId : byMsgNo

      if (existIdx !== undefined) {
        // 更新已有消息
        allMessages[existIdx] = { ...allMessages[existIdx], ...normalized }
      } else {
        // 追加新消息，并更新索引
        const newIdx = allMessages.length
        allMessages.push(normalized)
        if (normalized.id) idMap.set(String(normalized.id), newIdx)
        if (normalized.messageNo) msgNoMap.set(normalized.messageNo, newIdx)
      }
    }

    const trimmed = this._trimOldMessages(allMessages)
    uni.setStorageSync(this.UNIFIED_STORAGE_KEY, JSON.stringify(trimmed))
    console.log(`ChatStorage: 批量写入完成，共 ${trimmed.length} 条`)
  }

  /**
   * 按会话裁剪超出上限的旧消息，防止 Storage 无限增长
   * 每个会话保留最新的 MAX_MESSAGES_PER_SESSION 条
   */
  _trimOldMessages(allMessages) {
    // 按 session_id 分组
    const sessionMap = new Map()
    for (const msg of allMessages) {
      const sid = String(msg.session_id)
      if (!sessionMap.has(sid)) sessionMap.set(sid, [])
      sessionMap.get(sid).push(msg)
    }
    const result = []
    for (const [, msgs] of sessionMap) {
      if (msgs.length <= this.MAX_MESSAGES_PER_SESSION) {
        result.push(...msgs)
      } else {
        // 按时间升序后截取最新的 N 条
        msgs.sort((a, b) => new Date(a.send_time) - new Date(b.send_time))
        result.push(...msgs.slice(-this.MAX_MESSAGES_PER_SESSION))
      }
    }
    return result
  }

  /**
   * 获取指定会话本地缓存中最新一条消息的时间，用于增量同步
   * @param {string|number} sessionId
   * @returns {Promise<string|null>} ISO 时间字符串，无缓存时返回 null
   */
  getLatestMessageTime(sessionId) {
    return new Promise((resolve) => {
      try {
        const targetSessionId = String(sessionId)

        // #ifdef APP-PLUS
        if (this.isApp) {
          plus.sqlite.selectSql({
            name: this.dbName,
            sql: `SELECT MAX(send_time) AS latest FROM messages WHERE session_id = ?`,
            args: [targetSessionId],
            success: (res) => resolve(res.data?.[0]?.latest || null),
            fail: () => resolve(null)
          })
          return
        }
        // #endif

        const allMessages = this._getAllStorageMessages()
        const sessionMsgs = allMessages.filter(m => String(m.session_id) === targetSessionId)
        if (sessionMsgs.length === 0) return resolve(null)
        const latest = sessionMsgs.reduce((max, m) =>
          new Date(m.send_time) > new Date(max) ? m.send_time : max,
          sessionMsgs[0].send_time
        )
        resolve(latest)
      } catch (e) {
        console.error('ChatStorage: getLatestMessageTime 异常', e)
        resolve(null)
      }
    })
  }

  /**
   * 检测当前用户是否为本设备首次登录（从未在本设备成功同步过消息）
   * 用于决定是否先读本地缓存：首次登录无本地数据，直接走服务端；非首次先读本地再拉离线消息
   * @param {string} userId - 当前用户 ID
   * @returns {boolean} true=首次在本设备登录，false=非首次（已有本地同步记录）
   */
  isFirstTimeOnDevice(userId) {
    if (!userId || String(userId).trim() === '') return true
    try {
      const key = `chat_device_initialized_${String(userId).trim()}`
      const val = typeof uni !== 'undefined' ? uni.getStorageSync(key) : null
      return val !== true && val !== '1'
    } catch (e) {
      console.warn('ChatStorage: isFirstTimeOnDevice 读取失败', e)
      return true
    }
  }

  /**
   * 标记当前用户在本设备已完成首次消息同步，后续进入视为非首次
   * @param {string} userId - 当前用户 ID
   */
  setDeviceInitialized(userId) {
    if (!userId || String(userId).trim() === '') return
    try {
      const key = `chat_device_initialized_${String(userId).trim()}`
      if (typeof uni !== 'undefined') {
        uni.setStorageSync(key, true)
      }
    } catch (e) {
      console.warn('ChatStorage: setDeviceInitialized 写入失败', e)
    }
  }
}

// 导出单例
export default new ChatStorage()