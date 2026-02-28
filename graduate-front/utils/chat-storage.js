class ChatStorage {
  constructor() {
    this.dbName = 'chatDB'
    this.dbPath = '_doc/chat.db'
    this.isApp = false
    // 统一使用一个 Storage Key，避免分 Key 导致的查询混乱
    this.UNIFIED_STORAGE_KEY = 'chat_unified_message_history'
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
        // 去重
        const existIndex = allMessages.findIndex(item => item.id === normalizedMsg.id || item.messageNo === normalizedMsg.messageNo)
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

  // 2. 批量插入消息
  insertMessages(sessionId, msgs) {
    return Promise.all(msgs.map(msg => this.insertMessage({ ...msg, session_id: sessionId })))
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

  // 内部方法：获取所有消息
  _getAllStorageMessages() {
    try {
      const raw = uni.getStorageSync(this.UNIFIED_STORAGE_KEY)
      return raw ? JSON.parse(raw) : []
    } catch (e) {
      console.error('ChatStorage: 读取Storage失败', e)
      return []
    }
  }
}

// 导出单例
export default new ChatStorage()