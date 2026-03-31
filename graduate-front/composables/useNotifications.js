import { computed, ref } from 'vue'
import service from '@/utils/request'

export function useNotifications(options = {}) {
  const { onApproved } = options

  const notifications = ref({
    friendApplies: [],
    groupApplies: [],
  })
  const notifyLoading = ref(false)

  const pendingNotifyCount = computed(() => {
    const friendCount = notifications.value.friendApplies.filter((item) => item.status === 0).length
    const groupCount = notifications.value.groupApplies.filter((item) => item.status === 0).length
    return friendCount + groupCount
  })

  const loadNotifications = async () => {
    notifyLoading.value = true

    try {
      const [friendResult, groupResult] = await Promise.allSettled([
        service.get('/friend/apply/received'),
        service.get('/group/apply/received'),
      ])

      notifications.value = {
        friendApplies:
          friendResult.status === 'fulfilled' && friendResult.value.code === 200
            ? friendResult.value.data || []
            : [],
        groupApplies:
          groupResult.status === 'fulfilled' && groupResult.value.code === 200
            ? groupResult.value.data || []
            : [],
      }
    } catch (error) {
      console.error('[Notifications] Failed to load notifications', error)
    } finally {
      notifyLoading.value = false
    }
  }

  const runApprovedHook = async () => {
    if (typeof onApproved === 'function') {
      await Promise.resolve(onApproved())
    }
  }

  const handleFriendApply = async (applyId, status) => {
    const response = await service.post('/friend/apply/handle', {
      applyId,
      status,
      rejectReason: '',
    })

    if (response.code === 200) {
      notifications.value.friendApplies = notifications.value.friendApplies.map((item) =>
        String(item.id) === String(applyId)
          ? {
              ...item,
              status,
            }
          : item,
      )

      uni.showToast({
        title: status === 1 ? '已处理好友申请' : '已拒绝好友申请',
        icon: 'none',
      })

      if (status === 1) {
        await runApprovedHook()
      }
    }

    return response
  }

  const handleGroupApply = async (applyId, status) => {
    const response = await service.post('/group/apply/handle', {
      applyId,
      status,
      rejectReason: '',
    })

    if (response.code === 200) {
      notifications.value.groupApplies = notifications.value.groupApplies.filter(
        (item) => String(item.id) !== String(applyId),
      )

      uni.showToast({
        title: status === 1 ? '已处理入群申请' : '已拒绝入群申请',
        icon: 'none',
      })

      if (status === 1) {
        await runApprovedHook()
      }
    }

    return response
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
