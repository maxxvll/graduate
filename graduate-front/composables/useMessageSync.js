/**
 * 离线消息同步工具
 * 负责记录同步时间戳和拉取离线消息
 */
import service from '@/utils/request'

const STORAGE_KEY = 'lastMessageSyncTime'

export function getLastSyncTime() {
  try {
    const stored = uni.getStorageSync(STORAGE_KEY)
    if (stored && typeof stored === 'number' && stored > 0) {
      return stored
    }
  } catch (e) {
    console.warn('[MessageSync] getLastSyncTime failed', e)
  }
  return 0
}

export function saveLastSyncTime(timestamp = Date.now()) {
  try {
    const nextValue = Number(timestamp)
    uni.setStorageSync(STORAGE_KEY, Number.isFinite(nextValue) && nextValue > 0 ? nextValue : Date.now())
  } catch (e) {
    console.warn('[MessageSync] saveLastSyncTime failed', e)
  }
}

export async function fetchOfflineMessages(afterTimestamp = 0) {
  try {
    const params = {}
    if (afterTimestamp > 0) {
      params.afterTimestamp = afterTimestamp
    }

    const res = await service.get('/offline-sync/messages/offline', {
      params,
    })

    if (res.code === 200 && Array.isArray(res.data)) {
      return res.data
    }
    return []
  } catch (e) {
    console.error('[MessageSync] fetchOfflineMessages failed', e)
    return []
  }
}

export async function ackOfflineMessages(messageIds = []) {
  const normalizedIds = Array.isArray(messageIds)
    ? messageIds
        .map((item) => Number(item))
        .filter((item) => Number.isFinite(item) && item > 0)
    : []

  if (!normalizedIds.length) {
    return true
  }

  try {
    const res = await service.post('/offline-sync/messages/ack', normalizedIds)
    return res.code === 200
  } catch (e) {
    console.error('[MessageSync] ackOfflineMessages failed', e)
    return false
  }
}

export async function syncAllSessions() {
  try {
    const res = await service.get('/session/list/sync')
    if (res.code === 200) {
      return res.data
    }
    return null
  } catch (e) {
    console.error('[MessageSync] syncAllSessions failed', e)
    return null
  }
}

export default {
  getLastSyncTime,
  saveLastSyncTime,
  fetchOfflineMessages,
  ackOfflineMessages,
  syncAllSessions,
}
