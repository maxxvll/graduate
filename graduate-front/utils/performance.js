/**
 * 性能优化工具集
 * 包含防抖、节流、缓存等常用性能优化函数
 */

/**
 * 防抖函数 - 延迟执行，在指定时间内多次触发只执行最后一次
 * @param {Function} func - 要执行的函数
 * @param {number} wait - 等待时间（ms）
 * @param {boolean} immediate - 是否立即执行
 * @returns {Function} 防抖后的函数
 */
export const debounce = (func, wait = 300, immediate = false) => {
  let timeout = null

  return function executedFunction(...args) {
    const context = this

    const later = () => {
      timeout = null
      if (!immediate) func.apply(context, args)
    }

    const callNow = immediate && !timeout

    if (timeout) clearTimeout(timeout)
    timeout = setTimeout(later, wait)

    if (callNow) func.apply(context, args)
  }
}

/**
 * 节流函数 - 在指定时间内只执行一次
 * @param {Function} func - 要执行的函数
 * @param {number} wait - 等待时间（ms）
 * @param {Object} options - 配置项
 * @param {boolean} options.leading - 是否在开始时执行
 * @param {boolean} options.trailing - 是否在结束时执行
 * @returns {Function} 节流后的函数
 */
export const throttle = (func, wait = 300, options = {}) => {
  let timeout = null
  let previous = 0

  const { leading = true, trailing = true } = options

  return function executedFunction(...args) {
    const context = this
    const now = Date.now()

    // 如果首次不执行，设置 previous 为当前时间
    if (!leading) previous = now

    const remaining = wait - (now - previous)

    if (remaining <= 0 || remaining > wait) {
      if (timeout) {
        clearTimeout(timeout)
        timeout = null
      }

      previous = now
      func.apply(context, args)
    } else if (trailing && !timeout) {
      timeout = setTimeout(() => {
        previous = leading ? Date.now() : 0
        timeout = null
        func.apply(context, args)
      }, remaining)
    }
  }
}

/**
 * 缓存函数 - 缓存函数的计算结果
 * @param {Function} func - 要缓存的函数
 * @param {Function} keyGenerator - 生成缓存 key 的函数
 * @returns {Function} 带缓存的函数
 */
export const memoize = (func, keyGenerator = (...args) => JSON.stringify(args)) => {
  const cache = new Map()

  return function executedFunction(...args) {
    const key = keyGenerator(...args)

    if (cache.has(key)) {
      return cache.get(key)
    }

    const result = func.apply(this, args)
    cache.set(key, result)
    return result
  }
}

/**
 * 清除缓存
 * @param {Function} memoizedFunc - 通过 memoize 生成的函数
 */
export const clearMemoizeCache = (memoizedFunc) => {
  if (memoizedFunc && memoizedFunc.cache) {
    memoizedFunc.cache.clear()
  }
}

/**
 * 请求缓存装饰器 - 缓存 API 请求结果
 * @param {Function} requestFunc - 请求函数
 * @param {number} cacheTime - 缓存时间（ms）
 * @returns {Function} 带缓存的请求函数
 */
export const cacheRequest = (requestFunc, cacheTime = 60000) => {
  const cache = new Map()

  return async function cachedRequest(...args) {
    const key = JSON.stringify(args)
    const cached = cache.get(key)

    if (cached && Date.now() - cached.timestamp < cacheTime) {
      console.log(`[Cache] 命中缓存: ${key}`)
      return cached.data
    }

    try {
      const result = await requestFunc.apply(this, args)
      cache.set(key, {
        data: result,
        timestamp: Date.now(),
      })
      return result
    } catch (error) {
      // 请求失败时，如果有缓存数据，返回缓存数据
      if (cached) {
        console.warn(`[Cache] 请求失败，返回过期缓存: ${key}`)
        return cached.data
      }
      throw error
    }
  }
}

/**
 * 批量执行函数 - 将多次调用合并为一次执行
 * @param {Function} func - 要执行的函数
 * @param {number} wait - 等待时间（ms）
 * @returns {Function} 批量执行的函数
 */
export const batchExecute = (func, wait = 100) => {
  let queue = []
  let timeout = null

  const flush = () => {
    if (queue.length === 0) return

    const args = queue
    queue = []

    func.apply(this, [args])
  }

  return function batchedFunction(...args) {
    queue.push(args)

    if (timeout) clearTimeout(timeout)
    timeout = setTimeout(flush, wait)
  }
}

/**
 * RAF 节流 - 使用 requestAnimationFrame 进行节流
 * 适用于动画和视觉更新
 * @param {Function} func - 要执行的函数
 * @returns {Function} 节流后的函数
 */
export const rafThrottle = (func) => {
  let rafId = null

  return function executedFunction(...args) {
    if (rafId) return

    rafId = requestAnimationFrame(() => {
      func.apply(this, args)
      rafId = null
    })
  }
}

/**
 * 优雅地处理异步错误
 * @param {Promise} promise - 要处理的 Promise
 * @returns {Promise} 返回 [error, data] 元组
 */
export const safeAsync = (promise) => {
  return promise
    .then((data) => [null, data])
    .catch((error) => [error, null])
}

/**
 * 性能监控 - 测量函数执行时间
 * @param {Function} func - 要测量的函数
 * @param {string} name - 函数名称
 * @returns {Function} 包装后的函数
 */
export const measurePerformance = (func, name = 'Function') => {
  return function measuredFunction(...args) {
    const start = performance.now()
    const result = func.apply(this, args)

    if (result instanceof Promise) {
      return result.then((value) => {
        const end = performance.now()
        console.log(`[Performance] ${name} took ${(end - start).toFixed(2)}ms`)
        return value
      })
    } else {
      const end = performance.now()
      console.log(`[Performance] ${name} took ${(end - start).toFixed(2)}ms`)
      return result
    }
  }
}

export default {
  debounce,
  throttle,
  memoize,
  clearMemoizeCache,
  cacheRequest,
  batchExecute,
  rafThrottle,
  safeAsync,
  measurePerformance,
}
