export const getBuildRuntime = () => {
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

export const isH5Runtime = () => {
  // #ifdef H5
  return true
  // #endif
  return false
}

export const isAppPlusRuntime = () => {
  // #ifdef APP-PLUS
  return true
  // #endif
  return false
}

export const supportsBrowserDom = () =>
  isH5Runtime() && typeof window !== 'undefined' && typeof document !== 'undefined'

export const hasPlusRuntime = () => isAppPlusRuntime() && typeof plus !== 'undefined'

export const waitForPlusReady = ({ timeoutMs = 5000, pollIntervalMs = 50 } = {}) =>
  new Promise((resolve, reject) => {
    if (!isAppPlusRuntime()) {
      reject(new Error('plus runtime unavailable'))
      return
    }

    if (typeof plus !== 'undefined') {
      resolve(plus)
      return
    }

    let settled = false
    let pollTimer = null
    let timeoutTimer = null

    const cleanup = () => {
      if (pollTimer) {
        clearInterval(pollTimer)
        pollTimer = null
      }
      if (timeoutTimer) {
        clearTimeout(timeoutTimer)
        timeoutTimer = null
      }
      if (typeof document !== 'undefined' && typeof document.removeEventListener === 'function') {
        document.removeEventListener('plusready', handlePlusReady)
      }
    }

    const finish = (handler) => {
      if (settled) {
        return
      }
      settled = true
      cleanup()
      handler()
    }

    const handlePlusReady = () => {
      finish(() => resolve(typeof plus !== 'undefined' ? plus : null))
    }

    if (typeof document !== 'undefined' && typeof document.addEventListener === 'function') {
      document.addEventListener('plusready', handlePlusReady, { once: true })
    }

    pollTimer = setInterval(() => {
      if (typeof plus !== 'undefined') {
        handlePlusReady()
      }
    }, Math.max(16, Number(pollIntervalMs) || 50))

    timeoutTimer = setTimeout(() => {
      finish(() => reject(new Error('plus runtime timeout')))
    }, Math.max(200, Number(timeoutMs) || 1800))
  })

export const getViewportWidth = () => {
  try {
    const width = Number(uni.getSystemInfoSync().windowWidth || 0)
    if (width > 0) {
      return width
    }
  } catch {}

  return supportsBrowserDom() ? Number(window.innerWidth || 0) : 0
}

export const getSystemPlatform = () => {
  try {
    return uni.getSystemInfoSync().platform || 'unknown'
  } catch {
    return 'unknown'
  }
}
