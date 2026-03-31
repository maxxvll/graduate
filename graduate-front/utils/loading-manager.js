/**
 * 加载状态管理器
 * 统一管理全应用的加载状态，支持多请求并发处理
 */

class LoadingManager {
  constructor() {
    this.loadingSet = new Set() // 存储进行中的请求
    this.loadingCount = 0 // 加载计数器
    this.listeners = new Set() // 监听器集合
    this.minDisplayTime = 300 // 最小显示时间（ms）
    this.maxDisplayTime = 10000 // 最大显示时间（ms）
    this.startTime = 0 // 开始时间
    this.timer = null
  }

  /**
   * 添加监听器
   * @param {Function} listener - 监听函数，接收 isLoading 参数
   */
  addListener(listener) {
    this.listeners.add(listener)
    return () => this.listeners.delete(listener)
  }

  /**
   * 通知所有监听器
   */
  notifyListeners() {
    const isLoading = this.loadingCount > 0
    this.listeners.forEach((listener) => {
      try {
        listener(isLoading, this.loadingCount)
      } catch (error) {
        console.error('[LoadingManager] 监听器执行失败:', error)
      }
    })
  }

  /**
   * 显示加载状态
   * @param {string} key - 加载项的唯一标识
   * @param {Object} options - 配置项
   */
  show(key = 'default', options = {}) {
    const { message = '加载中...', minDisplayTime = this.minDisplayTime } = options

    // 如果已经在加载中，不重复处理
    if (this.loadingSet.has(key)) {
      console.warn(`[LoadingManager] 加载项 ${key} 已存在`)
      return
    }

    this.loadingSet.add(key)
    this.loadingCount++

    // 首次显示加载时
    if (this.loadingCount === 1) {
      this.startTime = Date.now()

      // 清除之前的定时器
      if (this.timer) {
        clearTimeout(this.timer)
      }

      // 显示加载提示
      uni.showLoading({
        title: message,
        mask: true,
      })

      // 设置最小显示时间
      this.timer = setTimeout(() => {
        this.timer = null
      }, minDisplayTime)
    }

    this.notifyListeners()
    console.log(`[LoadingManager] 显示加载: ${key}, 总数: ${this.loadingCount}`)
  }

  /**
   * 隐藏加载状态
   * @param {string} key - 加载项的唯一标识
   */
  hide(key = 'default') {
    if (!this.loadingSet.has(key)) {
      console.warn(`[LoadingManager] 加载项 ${key} 不存在`)
      return
    }

    this.loadingSet.delete(key)
    this.loadingCount--

    // 所有加载都完成时
    if (this.loadingCount === 0) {
      const elapsedTime = Date.now() - this.startTime
      const remainingTime = this.timer ? Math.max(0, this.minDisplayTime - elapsedTime) : 0

      // 确保最小显示时间
      setTimeout(() => {
        if (this.loadingCount === 0) {
          uni.hideLoading()
          this.startTime = 0
        }
      }, remainingTime)
    }

    this.notifyListeners()
    console.log(`[LoadingManager] 隐藏加载: ${key}, 剩余: ${this.loadingCount}`)
  }

  /**
   * 隐藏所有加载状态
   */
  hideAll() {
    this.loadingSet.clear()
    this.loadingCount = 0
    uni.hideLoading()
    this.notifyListeners()
    console.log('[LoadingManager] 隐藏所有加载')
  }

  /**
   * 包装异步函数，自动显示/隐藏加载状态
   * @param {Function} asyncFn - 异步函数
   * @param {Object} options - 配置项
   * @returns {Function} 包装后的函数
   */
  wrap(asyncFn, options = {}) {
    const { key = 'default', showError = true } = options

    return async function wrappedFunction(...args) {
      loadingManager.show(key, options)

      try {
        const result = await asyncFn.apply(this, args)
        return result
      } catch (error) {
        if (showError) {
          uni.$u.toast(error.message || '操作失败')
        }
        throw error
      } finally {
        loadingManager.hide(key)
      }
    }
  }

  /**
   * 获取当前加载状态
   */
  isLoading() {
    return this.loadingCount > 0
  }

  /**
   * 获取当前加载数量
   */
  getLoadingCount() {
    return this.loadingCount
  }

  /**
   * 获取所有加载项
   */
  getLoadingKeys() {
    return Array.from(this.loadingSet)
  }
}

// 创建全局单例
const loadingManager = new LoadingManager()

// 导出单例和类
export default loadingManager
export { LoadingManager }
