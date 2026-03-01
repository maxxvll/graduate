import { ref, computed } from 'vue'
import service from '@/utils/request'

/**
 * useNotifications — 通知相关逻辑（好友申请 + 群聊申请）
 *
 * @param {Object} options
 * @param {Function} [options.onApproved] - 同意申请成功后的回调（用于刷新会话列表等）
 *
 * 使用方式：
 *   const { notifications, notifyLoading, pendingNotifyCount,
 *           loadNotifications, handleFriendApply, handleGroupApply } = useNotifications({
 *     onApproved: () => loadSessionList()
 *   })
 */
export function useNotifications(options = {}) {
  const { onApproved } = options
  const notifications  = ref({ friendApplies: [], groupApplies: [] })
  const notifyLoading  = ref(false)

  /**
   * 待处理通知总数：
   *  - 好友申请中 status===0 表示待审核
   *  - 群聊申请全部视为待处理（后端只返回未处理的）
   */
  const pendingNotifyCount = computed(() => {
    const fCount = notifications.value.friendApplies.filter(a => a.status === 0).length
    const gCount = notifications.value.groupApplies.length
    return fCount + gCount
  })

  /**
   * 拉取通知列表：好友申请 + 群聊申请并行请求，互不影响。
   * 使用 allSettled 保证即使某一个接口失败也不影响另一个的展示。
   */
  const loadNotifications = async () => {
    notifyLoading.value = true
    try {
      const [friendRes, groupRes] = await Promise.allSettled([
        service.get('/friend/apply/received'),
        service.get('/group/apply/received'),
      ])
      notifications.value = {
        friendApplies: friendRes.status === 'fulfilled' && friendRes.value.code === 200
          ? (friendRes.value.data || [])
          : [],
        groupApplies: groupRes.status === 'fulfilled' && groupRes.value.code === 200
          ? (groupRes.value.data || [])
          : [],
      }
    } catch (e) {
      console.error('useNotifications: 加载通知失败', e)
    } finally {
      notifyLoading.value = false
    }
  }

  /**
   * 处理好友申请。
   * @param {number} applyId - 申请记录 ID
   * @param {number} status  - 1=接受, 2=拒绝
   */
  const handleFriendApply = async (applyId, status) => {
    try {
      const res = await service.post('/friend/apply/handle', { applyId, status, rejectReason: '' })
      if (res.code === 200) {
        // 本地同步更新状态。避免重新请求整个列表
        const idx = notifications.value.friendApplies.findIndex(a => a.id === applyId)
        if (idx !== -1) {
          notifications.value.friendApplies[idx] = {
            ...notifications.value.friendApplies[idx],
            status,
          }
        }
        uni.showToast({ title: status === 1 ? '已添加为好友' : '已拒绝', icon: 'success' })
        // 同意后触发回调（如刷新会话列表）
        if (status === 1 && typeof onApproved === 'function') {
          onApproved()
        }
      }
    } catch (e) {
      console.error('useNotifications: 处理好友申请失败', e)
      uni.showToast({ title: '操作失败，请稍后重试', icon: 'none' })
    }
  }

  /**
   * 处理群聊申请（管理员/群主操作）。
   * @param {number} applyId - 申请记录 ID
   * @param {number} status  - 1=同意, 2=拒绝
   */
  const handleGroupApply = async (applyId, status) => {
    try {
      const res = await service.post('/group/apply/handle', { applyId, status, rejectReason: '' })
      if (res.code === 200) {
        // 处理完成后从列表中移除，因为已处理的申请通常不在此展示
        notifications.value.groupApplies = notifications.value.groupApplies.filter(a => a.id !== applyId)
        uni.showToast({ title: status === 1 ? '已同意入群' : '已拒绝', icon: 'success' })
        // 同意后触发回调（如刷新会话列表）
        if (status === 1 && typeof onApproved === 'function') {
          onApproved()
        }
      }
    } catch (e) {
      console.error('useNotifications: 处理群聊申请失败', e)
      uni.showToast({ title: '操作失败，请稍后重试', icon: 'none' })
    }
  }

  return {
    notifications,
    notifyLoading,
    pendingNotifyCount,
    loadNotifications,
    handleFriendApply,
    handleGroupApply,
  }
}
