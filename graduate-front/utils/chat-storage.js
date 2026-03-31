import { isAppPlusRuntime, supportsBrowserDom, waitForPlusReady } from './runtime'

const RETENTION_DAYS = 15
const RETENTION_MS = RETENTION_DAYS * 24 * 60 * 60 * 1000

const SQLITE_DB_NAME = 'graduateChatCache'
const SQLITE_DB_PATH = '_doc/graduate-chat-cache.db'
const SQLITE_TABLE = 'chat_message_cache'

const IDB_NAME = 'graduate-chat-cache'
const IDB_VERSION = 1
const IDB_STORE = 'messages'
const IDB_INDEX_SESSION = 'sessionId'
const IDB_INDEX_SEND_TIME = 'sendTimeMs'

const FALLBACK_STORAGE_KEY = 'chat_message_cache_fallback_v2'
const DEVICE_FLAG_PREFIX = 'chat_device_initialized_'
const MAX_FALLBACK_MESSAGES_PER_SESSION = 400

const isFiniteNumber = (value) => Number.isFinite(Number(value))

const normalizeTimestamp = (value) => {
  if (value instanceof Date && !Number.isNaN(value.getTime())) {
    return value.getTime()
  }

  if (typeof value === 'number' && Number.isFinite(value)) {
    return value > 1e12 ? value : value * 1000
  }

  if (typeof value === 'string') {
    const trimmed = value.trim()
    if (!trimmed) return Date.now()

    if (/^\d+$/.test(trimmed)) {
      const numeric = Number(trimmed)
      return numeric > 1e12 ? numeric : numeric * 1000
    }

    const parsed = Date.parse(trimmed)
    if (!Number.isNaN(parsed)) {
      return parsed
    }
  }

  return Date.now()
}

const normalizeText = (value) => String(value ?? '').trim()

const buildCacheKey = (message) => {
  const messageNo = normalizeText(message.messageNo || message.message_no)
  if (messageNo) {
    return `messageNo:${messageNo}`
  }

  const messageId = normalizeText(message.id || message.messageId)
  if (messageId) {
    return `id:${messageId}`
  }

  const sessionId = normalizeText(message.sessionId || message.session_id)
  const senderId = normalizeText(message.senderId || message.sender_id)
  const sendTime = normalizeTimestamp(message.sendTime || message.send_time || message.createdAt)
  return `local:${sessionId}:${senderId}:${sendTime}:${Math.random().toString(36).slice(2, 8)}`
}

const sortMessagesAsc = (list) =>
  [...list].sort((left, right) => {
    const delta = normalizeTimestamp(left.sendTime) - normalizeTimestamp(right.sendTime)
    if (delta !== 0) return delta
    return String(left.messageNo || left.id || '').localeCompare(String(right.messageNo || right.id || ''))
  })

const dedupeMessages = (list) => {
  const byCacheKey = new Map()
  list.forEach((item) => {
    if (!item) return
    byCacheKey.set(item.cacheKey, item)
  })
  return [...byCacheKey.values()]
}

const toPersistedPayload = (message, sessionIdOverride = '') => {
  const sessionId = normalizeText(sessionIdOverride || message.sessionId || message.session_id)
  if (!sessionId) return null

  const sendTimeMs = normalizeTimestamp(message.sendTime || message.send_time || message.createdAt)
  const sendTime = new Date(sendTimeMs).toISOString()
  const messageNo = normalizeText(message.messageNo || message.message_no)
  const messageId = normalizeText(message.id || message.messageId)

  const payload = {
    id: messageId,
    messageNo,
    sessionId,
    sessionType: Number(message.sessionType || message.session_type || 1),
    senderId: normalizeText(message.senderId || message.sender_id),
    receiverId: normalizeText(message.receiverId || message.receiver_id),
    messageType: Number(message.messageType || message.message_type || 1),
    content: String(message.content || ''),
    fileUrl: String(message.fileUrl || message.file_url || ''),
    fileName: String(message.fileName || message.file_name || ''),
    fileSize: Number(message.fileSize || message.file_size || 0),
    duration: Number(message.duration || 0),
    sendTime,
    status: Number(message.status || 0),
    contentReplaced: String(message.contentReplaced || message.content_replaced || ''),
    senderAvatar: String(message.senderAvatar || message.sender_avatar || ''),
    senderName: String(message.senderName || message.sender_name || ''),
    clientStatus: String(message.clientStatus || message.send_status || ''),
  }

  return {
    cacheKey: buildCacheKey(payload),
    sessionId,
    messageNo: messageNo || null,
    messageId: messageId || null,
    sendTimeMs,
    updatedAtMs: Date.now(),
    payload,
  }
}

const isExpiredRecord = (record, cutoffMs) => Number(record?.sendTimeMs || 0) < cutoffMs

class ChatStorage {
  constructor() {
    this.sqliteReady = false
    this.sqliteReadyPromise = null
    this.sqliteDisabled = false
    this.indexedDbPromise = null
    this.indexedDbDisabled = false
  }

  getRetentionCutoffMs() {
    return Date.now() - RETENTION_MS
  }

  normalizeMessage(message, sessionId = '') {
    return toPersistedPayload(message, sessionId)
  }

  normalizeMessages(sessionId, messages = []) {
    const cutoffMs = this.getRetentionCutoffMs()
    return dedupeMessages(
      messages
        .map((message) => this.normalizeMessage(message, sessionId))
        .filter((record) => record && !isExpiredRecord(record, cutoffMs)),
    )
  }

  async resolveDriver() {
    if (isAppPlusRuntime() && !this.sqliteDisabled) {
      const ready = await this.ensureSqliteReady()
      if (ready) return 'sqlite'
    }

    if (supportsBrowserDom() && !this.indexedDbDisabled) {
      const db = await this.ensureIndexedDbReady()
      if (db) return 'indexeddb'
    }

    return 'storage'
  }

  async ensureSqliteReady() {
    if (!isAppPlusRuntime() || this.sqliteDisabled) {
      return false
    }

    if (this.sqliteReady) {
      return true
    }

    if (this.sqliteReadyPromise) {
      return this.sqliteReadyPromise
    }

    this.sqliteReadyPromise = (async () => {
      await waitForPlusReady()

      if (!plus?.sqlite) {
        throw new Error('plus.sqlite unavailable')
      }

      let opened = false
      try {
        opened =
          typeof plus.sqlite.isOpenDatabase === 'function' &&
          plus.sqlite.isOpenDatabase({
            name: SQLITE_DB_NAME,
            path: SQLITE_DB_PATH,
          })
      } catch (error) {
        console.warn('[ChatStorage] Failed to inspect sqlite status, retry opening directly', error)
      }

      if (!opened) {
        await new Promise((resolve, reject) => {
          plus.sqlite.openDatabase({
            name: SQLITE_DB_NAME,
            path: SQLITE_DB_PATH,
            success: resolve,
            fail: reject,
          })
        })
      }

      await this.executeSqliteRaw(
        `CREATE TABLE IF NOT EXISTS ${SQLITE_TABLE} (
          cache_key TEXT PRIMARY KEY,
          session_id TEXT NOT NULL,
          message_no TEXT,
          message_id TEXT,
          send_time INTEGER NOT NULL,
          payload_json TEXT NOT NULL,
          updated_at INTEGER NOT NULL
        )`,
      )

      await this.executeSqliteRaw(
        `CREATE INDEX IF NOT EXISTS idx_${SQLITE_TABLE}_session_time ON ${SQLITE_TABLE} (session_id, send_time)`,
      )

      await this.executeSqliteRaw(
        `CREATE INDEX IF NOT EXISTS idx_${SQLITE_TABLE}_send_time ON ${SQLITE_TABLE} (send_time)`,
      )

      this.sqliteReady = true
      return true
    })().catch((error) => {
      console.warn('[ChatStorage] SQLite init failed, fallback to browser/local storage', error)
      this.sqliteDisabled = true
      this.sqliteReadyPromise = null
      return false
    })

    return this.sqliteReadyPromise
  }

  async ensureIndexedDbReady() {
    if (!supportsBrowserDom() || this.indexedDbDisabled || typeof indexedDB === 'undefined') {
      return null
    }

    if (this.indexedDbPromise) {
      return this.indexedDbPromise
    }

    this.indexedDbPromise = new Promise((resolve) => {
      try {
        const request = indexedDB.open(IDB_NAME, IDB_VERSION)

        request.onupgradeneeded = () => {
          const db = request.result
          const store = db.objectStoreNames.contains(IDB_STORE)
            ? request.transaction.objectStore(IDB_STORE)
            : db.createObjectStore(IDB_STORE, { keyPath: 'cacheKey' })

          if (!store.indexNames.contains(IDB_INDEX_SESSION)) {
            store.createIndex(IDB_INDEX_SESSION, 'sessionId', { unique: false })
          }
          if (!store.indexNames.contains(IDB_INDEX_SEND_TIME)) {
            store.createIndex(IDB_INDEX_SEND_TIME, 'sendTimeMs', { unique: false })
          }
        }

        request.onsuccess = () => resolve(request.result)
        request.onerror = () => {
          console.warn('[ChatStorage] IndexedDB open failed, fallback to storage', request.error)
          this.indexedDbDisabled = true
          this.indexedDbPromise = null
          resolve(null)
        }
      } catch (error) {
        console.warn('[ChatStorage] IndexedDB unavailable, fallback to storage', error)
        this.indexedDbDisabled = true
        this.indexedDbPromise = null
        resolve(null)
      }
    })

    return this.indexedDbPromise
  }

  executeSqliteRaw(sql, args = []) {
    return new Promise((resolve, reject) => {
      plus.sqlite.executeSql({
        name: SQLITE_DB_NAME,
        sql,
        args,
        success: resolve,
        fail: reject,
      })
    })
  }

  selectSqliteRaw(sql, args = []) {
    return new Promise((resolve, reject) => {
      plus.sqlite.selectSql({
        name: SQLITE_DB_NAME,
        sql,
        args,
        success: (rows) => resolve(Array.isArray(rows) ? rows : rows?.data || []),
        fail: reject,
      })
    })
  }

  async withIndexedDbStore(mode, handler) {
    const db = await this.ensureIndexedDbReady()
    if (!db) return null

    return new Promise((resolve, reject) => {
      const transaction = db.transaction(IDB_STORE, mode)
      const store = transaction.objectStore(IDB_STORE)
      let result

      transaction.oncomplete = () => resolve(result)
      transaction.onerror = () => reject(transaction.error)
      transaction.onabort = () => reject(transaction.error)

      Promise.resolve(handler(store, transaction))
        .then((value) => {
          result = value
        })
        .catch((error) => {
          try {
            transaction.abort()
          } catch {}
          reject(error)
        })
    })
  }

  readFallbackStore() {
    try {
      const raw = uni.getStorageSync(FALLBACK_STORAGE_KEY)
      if (!raw) return {}
      const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
      return parsed && typeof parsed === 'object' ? parsed : {}
    } catch (error) {
      console.warn('[ChatStorage] Failed to read fallback store, resetting cache', error)
      try {
        uni.removeStorageSync(FALLBACK_STORAGE_KEY)
      } catch {}
      return {}
    }
  }

  writeFallbackStore(store) {
    uni.setStorageSync(FALLBACK_STORAGE_KEY, JSON.stringify(store))
  }

  trimFallbackRecords(records) {
    const cutoffMs = this.getRetentionCutoffMs()
    return dedupeMessages(records)
      .filter((record) => record && !isExpiredRecord(record, cutoffMs))
      .sort((left, right) => left.sendTimeMs - right.sendTimeMs)
      .slice(-MAX_FALLBACK_MESSAGES_PER_SESSION)
  }

  async pruneExpiredMessages() {
    const driver = await this.resolveDriver()
    const cutoffMs = this.getRetentionCutoffMs()

    if (driver === 'sqlite') {
      await this.executeSqliteRaw(`DELETE FROM ${SQLITE_TABLE} WHERE send_time < ?`, [cutoffMs])
      return true
    }

    if (driver === 'indexeddb') {
      await this.withIndexedDbStore('readwrite', (store) => {
        const index = store.index(IDB_INDEX_SEND_TIME)
        const range = IDBKeyRange.upperBound(cutoffMs - 1)
        index.openKeyCursor(range).onsuccess = (event) => {
          const cursor = event.target.result
          if (!cursor) return
          store.delete(cursor.primaryKey)
          cursor.continue()
        }
      })
      return true
    }

    const nextStore = {}
    const store = this.readFallbackStore()
    Object.keys(store).forEach((sessionId) => {
      const normalized = this.trimFallbackRecords(Array.isArray(store[sessionId]) ? store[sessionId] : [])
      if (normalized.length) {
        nextStore[sessionId] = normalized
      }
    })
    this.writeFallbackStore(nextStore)
    return true
  }

  async insertMessage(message) {
    const normalized = this.normalizeMessage(message)
    if (!normalized) return null
    await this.upsertRecords([normalized])
    return normalized.payload
  }

  async insertMessages(sessionId, messages = []) {
    const normalized = this.normalizeMessages(sessionId, messages)
    if (!normalized.length) return []
    await this.upsertRecords(normalized)
    return normalized.map((record) => record.payload)
  }

  async replaceSessionMessages(sessionId, messages = []) {
    const targetSessionId = normalizeText(sessionId)
    if (!targetSessionId) return []

    const normalized = this.normalizeMessages(targetSessionId, messages)
    const driver = await this.resolveDriver()

    if (driver === 'sqlite') {
      await this.executeSqliteRaw(`DELETE FROM ${SQLITE_TABLE} WHERE session_id = ?`, [targetSessionId])
      if (normalized.length) {
        await this.upsertRecords(normalized)
      }
      return normalized.map((record) => record.payload)
    }

    if (driver === 'indexeddb') {
      await this.withIndexedDbStore('readwrite', (store) => {
        const sessionIndex = store.index(IDB_INDEX_SESSION)
        const range = IDBKeyRange.only(targetSessionId)
        sessionIndex.openCursor(range).onsuccess = (event) => {
          const cursor = event.target.result
          if (!cursor) return
          cursor.delete()
          cursor.continue()
        }
      })
      if (normalized.length) {
        await this.upsertRecords(normalized)
      }
      return normalized.map((record) => record.payload)
    }

    const store = this.readFallbackStore()
    if (normalized.length) {
      store[targetSessionId] = this.trimFallbackRecords(normalized)
    } else {
      delete store[targetSessionId]
    }
    this.writeFallbackStore(store)
    return normalized.map((record) => record.payload)
  }

  async queryMessages(sessionId) {
    const targetSessionId = normalizeText(sessionId)
    if (!targetSessionId) return []

    await this.pruneExpiredMessages()
    const driver = await this.resolveDriver()

    if (driver === 'sqlite') {
      const rows = await this.selectSqliteRaw(
        `SELECT payload_json FROM ${SQLITE_TABLE} WHERE session_id = ? ORDER BY send_time ASC`,
        [targetSessionId],
      )
      return rows
        .map((row) => {
          try {
            return JSON.parse(row.payload_json)
          } catch {
            return null
          }
        })
        .filter(Boolean)
    }

    if (driver === 'indexeddb') {
      const records = await this.withIndexedDbStore('readonly', (store) => {
        const sessionIndex = store.index(IDB_INDEX_SESSION)
        const request = sessionIndex.getAll(IDBKeyRange.only(targetSessionId))
        return new Promise((resolve, reject) => {
          request.onsuccess = () => resolve(request.result || [])
          request.onerror = () => reject(request.error)
        })
      })

      return sortMessagesAsc((records || []).map((record) => record.payload).filter(Boolean))
    }

    const store = this.readFallbackStore()
    const records = this.trimFallbackRecords(Array.isArray(store[targetSessionId]) ? store[targetSessionId] : [])
    if (records.length !== (store[targetSessionId] || []).length) {
      if (records.length) {
        store[targetSessionId] = records
      } else {
        delete store[targetSessionId]
      }
      this.writeFallbackStore(store)
    }
    return sortMessagesAsc(records.map((record) => record.payload).filter(Boolean))
  }

  async deleteSessionMessages(sessionId) {
    const targetSessionId = normalizeText(sessionId)
    if (!targetSessionId) return false

    const driver = await this.resolveDriver()

    if (driver === 'sqlite') {
      await this.executeSqliteRaw(`DELETE FROM ${SQLITE_TABLE} WHERE session_id = ?`, [targetSessionId])
      return true
    }

    if (driver === 'indexeddb') {
      await this.withIndexedDbStore('readwrite', (store) => {
        const sessionIndex = store.index(IDB_INDEX_SESSION)
        const range = IDBKeyRange.only(targetSessionId)
        sessionIndex.openCursor(range).onsuccess = (event) => {
          const cursor = event.target.result
          if (!cursor) return
          cursor.delete()
          cursor.continue()
        }
      })
      return true
    }

    const store = this.readFallbackStore()
    delete store[targetSessionId]
    this.writeFallbackStore(store)
    return true
  }

  async getLatestMessageTime(sessionId) {
    const records = await this.queryMessages(sessionId)
    if (!records.length) return null
    return records[records.length - 1].sendTime || null
  }

  async upsertRecords(records = []) {
    const normalized = dedupeMessages(records)
    if (!normalized.length) return []

    await this.pruneExpiredMessages()
    const driver = await this.resolveDriver()

    if (driver === 'sqlite') {
      for (const record of normalized) {
        await this.executeSqliteRaw(
          `INSERT OR REPLACE INTO ${SQLITE_TABLE}
            (cache_key, session_id, message_no, message_id, send_time, payload_json, updated_at)
           VALUES (?, ?, ?, ?, ?, ?, ?)`,
          [
            record.cacheKey,
            record.sessionId,
            record.messageNo,
            record.messageId,
            record.sendTimeMs,
            JSON.stringify(record.payload),
            record.updatedAtMs,
          ],
        )
      }
      return normalized
    }

    if (driver === 'indexeddb') {
      await this.withIndexedDbStore('readwrite', (store) => {
        normalized.forEach((record) => store.put(record))
      })
      return normalized
    }

    const store = this.readFallbackStore()
    normalized.forEach((record) => {
      const sessionId = record.sessionId
      const current = Array.isArray(store[sessionId]) ? store[sessionId] : []
      const next = current.filter((item) => item.cacheKey !== record.cacheKey)
      next.push(record)
      store[sessionId] = this.trimFallbackRecords(next)
    })
    this.writeFallbackStore(store)
    return normalized
  }

  isFirstTimeOnDevice(userId) {
    if (!normalizeText(userId)) return true
    try {
      const value = uni.getStorageSync(`${DEVICE_FLAG_PREFIX}${normalizeText(userId)}`)
      return value !== true && value !== '1'
    } catch (error) {
      console.warn('[ChatStorage] Failed to read device init flag', error)
      return true
    }
  }

  setDeviceInitialized(userId) {
    if (!normalizeText(userId)) return
    try {
      uni.setStorageSync(`${DEVICE_FLAG_PREFIX}${normalizeText(userId)}`, true)
    } catch (error) {
      console.warn('[ChatStorage] Failed to persist device init flag', error)
    }
  }
}

export { RETENTION_DAYS, RETENTION_MS }
export default new ChatStorage()
