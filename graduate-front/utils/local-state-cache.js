const CACHE_PREFIX = 'graduate_local_state_cache_v1'

const normalizeText = (value) => String(value ?? '').trim()

const buildStorageKey = (scope = '', key = '') => {
  const normalizedScope = normalizeText(scope) || 'global'
  const normalizedKey = normalizeText(key)
  if (!normalizedKey) {
    throw new Error('cache key is required')
  }
  return `${CACHE_PREFIX}:${normalizedScope}:${normalizedKey}`
}

const parseRecord = (payload) => {
  if (!payload || typeof payload !== 'object') {
    return null
  }

  if (!('value' in payload)) {
    return null
  }

  return {
    updatedAt: Number(payload.updatedAt || 0),
    value: payload.value,
  }
}

class LocalStateCache {
  get(scope, key, { maxAgeMs = 0 } = {}) {
    try {
      const raw = uni.getStorageSync(buildStorageKey(scope, key))
      const record = parseRecord(raw)
      if (!record) {
        return null
      }

      if (maxAgeMs > 0 && record.updatedAt > 0 && Date.now() - record.updatedAt > maxAgeMs) {
        return null
      }

      return record
    } catch (error) {
      console.warn('[LocalStateCache] read failed', error)
      return null
    }
  }

  getValue(scope, key, options = {}) {
    return this.get(scope, key, options)?.value ?? null
  }

  set(scope, key, value) {
    const record = {
      updatedAt: Date.now(),
      value,
    }

    try {
      uni.setStorageSync(buildStorageKey(scope, key), record)
      return record
    } catch (error) {
      console.warn('[LocalStateCache] write failed', error)
      return null
    }
  }

  remove(scope, key) {
    try {
      uni.removeStorageSync(buildStorageKey(scope, key))
    } catch (error) {
      console.warn('[LocalStateCache] remove failed', error)
    }
  }
}

export default new LocalStateCache()

