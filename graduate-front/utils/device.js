const STORAGE_KEY = 'graduate_device_id'

const createFallbackDeviceId = () =>
  `device_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`

export const getDeviceId = () => {
  try {
    const stored = String(uni.getStorageSync(STORAGE_KEY) || '').trim()
    if (stored) {
      return stored
    }

    const systemInfo = uni.getSystemInfoSync?.() || {}
    const nativeDeviceId = String(systemInfo.deviceId || systemInfo.deviceID || '').trim()
    const generated =
      nativeDeviceId ||
      (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
        ? crypto.randomUUID()
        : createFallbackDeviceId())

    uni.setStorageSync(STORAGE_KEY, generated)
    return generated
  } catch (error) {
    console.warn('[Device] Failed to resolve device id', error)
    return createFallbackDeviceId()
  }
}

export default {
  getDeviceId,
}
