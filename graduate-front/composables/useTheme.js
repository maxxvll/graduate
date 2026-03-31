import { ref, computed } from 'vue'
import { safeGetStorage, safeSetStorage } from '@/utils/error-handler'

/**
 * useTheme — 主题切换和持久化管理
 *
 * 支持三种模式：
 * - 'light': 浅色模式
 * - 'dark': 深色模式
 * - 'auto': 跟随系统主题
 *
 * 使用方式：
 *   const { theme, isDark, toggleTheme, setTheme, initTheme } = useTheme()
 */
export function useTheme() {
  // 主题模式：'light' | 'dark' | 'auto'
  const theme = ref('auto')

  // 计算属性：当前是否为深色模式
  const isDark = computed(() => {
    if (theme.value === 'auto') {
      // 检测系统主题
      // #ifdef H5
      return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false
      // #endif
      // #ifndef H5
      // 非H5环境，默认浅色
      return false
      // #endif
    }
    return theme.value === 'dark'
  })

  // 监听系统主题变化的回调
  let mediaQuery = null
  let systemThemeListener = null

  /**
   * 切换主题：light → dark → auto → light
   */
  const toggleTheme = () => {
    const modes = ['light', 'dark', 'auto']
    const currentIndex = modes.indexOf(theme.value)
    theme.value = modes[(currentIndex + 1) % 3]
    saveTheme()
    applyTheme()
  }

  /**
   * 设置指定主题
   * @param {'light' | 'dark' | 'auto'} mode
   */
  const setTheme = (mode) => {
    if (!['light', 'dark', 'auto'].includes(mode)) {
      console.warn(`[useTheme] 无效的主题模式: ${mode}`)
      return
    }
    theme.value = mode
    saveTheme()
    applyTheme()
  }

  /**
   * 保存主题到本地存储
   */
  const saveTheme = () => {
    // 使用安全存储操作
    safeSetStorage('app_theme', theme.value)
  }

  /**
   * 从本地存储加载主题
   */
  const loadTheme = () => {
    // 使用安全存储操作
    const saved = safeGetStorage('app_theme', null)
    if (saved && ['light', 'dark', 'auto'].includes(saved)) {
      theme.value = saved
    }
  }

  /**
   * 应用主题到DOM
   */
  const applyTheme = () => {
    // #ifdef H5
    // 移除旧主题
    document.documentElement.removeAttribute('data-theme')

    // 应用新主题
    if (theme.value !== 'auto') {
      document.documentElement.setAttribute('data-theme', theme.value)
    }
    // #endif

    // #ifndef H5
    // 小程序/App环境通过页面级data-theme实现
    const pages = getCurrentPages()
    if (pages.length > 0) {
      const currentPage = pages[pages.length - 1]
      if (currentPage && currentPage.$vm) {
        currentPage.$vm.themeMode = theme.value
      }
    }
    // #endif
  }

  /**
   * 监听系统主题变化
   */
  const watchSystemTheme = () => {
    // #ifdef H5
    if (!mediaQuery) {
      mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      systemThemeListener = () => {
        // 仅在auto模式下需要重新应用
        if (theme.value === 'auto') {
          applyTheme()
        }
      }
      mediaQuery.addEventListener('change', systemThemeListener)
    }
    // #endif
  }

  /**
   * 初始化主题
   * 1. 加载保存的主题
   * 2. 应用到DOM
   * 3. 监听系统主题变化
   */
  const initTheme = () => {
    loadTheme()
    applyTheme()
    watchSystemTheme()
  }

  /**
   * 清理资源
   */
  const cleanup = () => {
    // #ifdef H5
    if (mediaQuery && systemThemeListener) {
      mediaQuery.removeEventListener('change', systemThemeListener)
      mediaQuery = null
      systemThemeListener = null
    }
    // #endif
  }

  return {
    theme,
    isDark,
    toggleTheme,
    setTheme,
    initTheme,
    cleanup
  }
}
