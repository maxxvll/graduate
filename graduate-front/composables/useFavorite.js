import { ref } from 'vue'

/**
 * useFavorite - 收藏功能 composable
 *
 * 提供收藏消息的添加、删除、列表查询、搜索等完整逻辑。
 *
 * 使用方式：
 *   const { favoriteList, isLoading, hasMore, addFavorite, removeFavorite,
 *           searchFavorites, loadMore, reset } = useFavorite()
 */
export function useFavorite() {
  const favoriteList = ref([])
  const isLoading = ref(false)
  const hasMore = ref(true)
  const currentPage = ref(1)
  const PAGE_SIZE = 20
  const searchKeyword = ref('')

  const getBaseUrl = () => {
    // 优先使用 uni.$http.baseUrl（已在 main.js 中配置）
    if (typeof uni !== 'undefined' && uni.$http?.baseUrl) {
      return uni.$http.baseUrl
    }
    // fallback 到 config
    try {
      const { API_CONFIG } = require('@/utils/config')
      return API_CONFIG.BASE_URL
    } catch {
      return ''
    }
  }

  /**
   * 添加收藏
   * @param {Object} params - 收藏参数
   * @param {number} params.messageId - 消息ID
   * @param {string} params.content - 收藏内容
   * @param {string} params.messageType - 消息类型 TEXT/IMAGE/FILE/VOICE
   * @param {string} params.fileUrl - 文件URL（可选）
   * @param {number} params.senderId - 发送者ID（可选）
   * @param {number} params.sessionId - 会话ID（可选）
   */
  const addFavorite = async ({ messageId, content, messageType, fileUrl, senderId, sessionId }) => {
    try {
      const res = await uni.$http.post('/favorite/add', null, {
        params: {
          messageId,
          content: content || '',
          messageType: messageType || 'TEXT',
          fileUrl: fileUrl || '',
          senderId: senderId || null,
          sessionId: sessionId || null,
        },
      })
      if (res.code === 200) {
        uni.showToast({ title: '已收藏', icon: 'success' })
        return res.data
      }
      return null
    } catch (e) {
      console.error('添加收藏失败', e)
      uni.showToast({ title: '收藏失败', icon: 'none' })
      return null
    }
  }

  /**
   * 取消收藏
   * @param {number} favoriteId - 收藏ID
   */
  const removeFavorite = async (favoriteId) => {
    try {
      const res = await uni.$http.delete(`/favorite/${favoriteId}`)
      if (res.code === 200) {
        favoriteList.value = favoriteList.value.filter((item) => item.id !== favoriteId)
        uni.showToast({ title: '已取消收藏', icon: 'success' })
        return true
      }
      return false
    } catch (e) {
      console.error('取消收藏失败', e)
      uni.showToast({ title: '取消收藏失败', icon: 'none' })
      return false
    }
  }

  /**
   * 获取收藏列表
   * @param {boolean} append - 是否追加（用于加载更多）
   */
  const loadFavoriteList = async (append = false) => {
    if (isLoading.value || (!append && !hasMore.value)) return

    isLoading.value = true
    try {
      const page = append ? currentPage.value + 1 : 1
      const res = await uni.$http.get('/favorite/list', {
        params: { page, size: PAGE_SIZE },
      })
      if (res.code === 200 && res.data) {
        const records = res.data.records || res.data.list || []
        if (append) {
          favoriteList.value.push(...records)
        } else {
          favoriteList.value = records
        }
        const total = res.data.total || 0
        hasMore.value = favoriteList.value.length < total
        currentPage.value = page
      }
    } catch (e) {
      console.error('获取收藏列表失败', e)
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 搜索收藏
   * @param {string} keyword - 搜索关键词
   * @param {boolean} append - 是否追加
   */
  const searchFavorites = async (keyword, append = false) => {
    if (!keyword?.trim()) {
      await loadFavoriteList(false)
      return
    }

    searchKeyword.value = keyword
    if (isLoading.value) return

    isLoading.value = true
    try {
      const page = append ? currentPage.value + 1 : 1
      const res = await uni.$http.get('/favorite/search', {
        params: { keyword: keyword.trim(), page, size: PAGE_SIZE },
      })
      if (res.code === 200 && res.data) {
        const records = res.data.records || res.data.list || []
        if (append) {
          favoriteList.value.push(...records)
        } else {
          favoriteList.value = records
        }
        const total = res.data.total || 0
        hasMore.value = favoriteList.value.length < total
        currentPage.value = page
      }
    } catch (e) {
      console.error('搜索收藏失败', e)
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 检查消息是否已收藏
   * @param {number} messageId - 消息ID
   */
  const checkFavorite = async (messageId) => {
    try {
      const res = await uni.$http.get('/favorite/check', {
        params: { messageId },
      })
      return res.code === 200 && res.data === true
    } catch {
      return false
    }
  }

  /**
   * 加载更多（分页加载）
   */
  const loadMore = async () => {
    if (!hasMore.value || isLoading.value) return
    if (searchKeyword.value) {
      await searchFavorites(searchKeyword.value, true)
    } else {
      await loadFavoriteList(true)
    }
  }

  /**
   * 重置状态
   */
  const reset = () => {
    favoriteList.value = []
    currentPage.value = 1
    hasMore.value = true
    searchKeyword.value = ''
    isLoading.value = false
  }

  // 初始化加载
  loadFavoriteList(false)

  return {
    favoriteList,
    isLoading,
    hasMore,
    searchKeyword,
    addFavorite,
    removeFavorite,
    loadFavoriteList,
    searchFavorites,
    checkFavorite,
    loadMore,
    reset,
  }
}
