<template>
  <view class="desktop-contact-detail">
    <view v-if="selectionType === 'requests'" class="requests-shell">
      <view class="requests-header">
        <text class="requests-title">新的朋友</text>
        <text class="requests-sub">好友申请和群聊申请都会显示在这里，处理完成后会自动从列表移除。</text>
      </view>

      <view class="requests-panels">
        <view class="request-panel">
          <view class="panel-head">
            <text class="panel-title">好友申请</text>
            <text class="panel-count">{{ notifications.friendApplies.length }}</text>
          </view>

          <view
            v-for="item in notifications.friendApplies"
            :key="`friend-${item.id}`"
            class="request-row"
          >
            <image class="request-avatar" :src="item.applicantAvatar || defaultAvatar" mode="aspectFill" />
            <view class="request-copy">
              <text class="request-name">{{ item.applicantNickname || item.applicantId }}</text>
              <text class="request-desc">{{ item.remark || '申请添加你为好友' }}</text>
            </view>
            <view class="request-actions">
              <button class="ghost-btn" @click="$emit('reject-friend', item)">拒绝</button>
              <button class="primary-btn" @click="$emit('approve-friend', item)">通过</button>
            </view>
          </view>

          <view v-if="!notifications.friendApplies.length" class="request-empty">暂无好友申请</view>
        </view>

        <view class="request-panel">
          <view class="panel-head">
            <text class="panel-title">群聊申请</text>
            <text class="panel-count">{{ notifications.groupApplies.length }}</text>
          </view>

          <view
            v-for="item in notifications.groupApplies"
            :key="`group-${item.id}`"
            class="request-row"
          >
            <image class="request-avatar" :src="item.applicantAvatar || defaultAvatar" mode="aspectFill" />
            <view class="request-copy">
              <text class="request-name">{{ item.applicantNickname || item.applicantId }}</text>
              <text class="request-desc">{{ item.groupName || item.remark || '申请加入群聊' }}</text>
            </view>
            <view class="request-actions">
              <button class="ghost-btn" @click="$emit('reject-group', item)">拒绝</button>
              <button class="primary-btn" @click="$emit('approve-group', item)">通过</button>
            </view>
          </view>

          <view v-if="!notifications.groupApplies.length" class="request-empty">暂无群聊申请</view>
        </view>
      </view>
    </view>

    <view v-else-if="selection" class="detail-shell">
      <view v-if="menuOpen" class="menu-scrim" @click="closeMenu"></view>

      <view class="detail-wrap">
        <view class="profile-row">
          <image class="profile-avatar" :src="avatarUrl" mode="aspectFill" />

          <view class="profile-copy">
            <view class="name-row">
              <text class="profile-name">{{ title }}</text>
              <text v-if="selectionType === 'friend'" class="profile-badge">联系人</text>
            </view>
            <text class="profile-line">{{ subtitle }}</text>
            <text class="profile-line">{{ secondaryMeta }}</text>
          </view>

          <view class="profile-tools">
            <view
              v-if="showMenuButton"
              class="profile-more-btn"
              hover-class="profile-more-btn-hover"
              @click.stop="toggleMenu"
            >
              <view class="more-dot"></view>
              <view class="more-dot"></view>
              <view class="more-dot"></view>
            </view>

            <view v-if="menuOpen" class="more-menu" @click.stop>
              <view
                v-for="item in menuItems"
                :key="item.key"
                class="menu-item"
                :class="{ danger: item.danger }"
                hover-class="menu-item-hover"
                @click="handleMenuAction(item.key)"
              >
                <text>{{ item.label }}</text>
              </view>
            </view>
          </view>
        </view>

        <view class="detail-table">
          <view v-for="row in detailRows" :key="row.label" class="detail-row">
            <text class="detail-label">{{ row.label }}</text>
            <text class="detail-value">{{ row.value }}</text>
          </view>
        </view>

        <view class="action-row">
          <view class="action-item" hover-class="action-item-hover" @click="handleSendMessage">
            <view class="action-icon">
              <svg class="action-svg" viewBox="0 0 48 48" aria-hidden="true">
                <path
                  d="M14 14.5C14 11.74 16.24 9.5 19 9.5H29C33.14 9.5 36.5 12.86 36.5 17V22C36.5 26.14 33.14 29.5 29 29.5H23.5L18 35V29.5H19C16.24 29.5 14 27.26 14 24.5V14.5Z"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.8"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </view>
            <text class="action-label">发消息</text>
          </view>

          <view
            v-if="selectionType === 'friend'"
            class="action-item"
            hover-class="action-item-hover"
            @click="handleStartAudio"
          >
            <view class="action-icon">
              <svg class="action-svg" viewBox="0 0 48 48" aria-hidden="true">
                <path
                  d="M19.5 14.5C20.74 13.26 22.78 13.26 24.02 14.5L26.84 17.32C27.94 18.42 28.09 20.16 27.2 21.42L25.4 23.98C28.03 28.27 31.74 31.97 36.02 34.6L38.58 32.8C39.84 31.91 41.58 32.06 42.68 33.16L45.5 35.98C46.74 37.22 46.74 39.26 45.5 40.5L43.78 42.22C42.29 43.71 40.08 44.24 38.08 43.58C30.61 41.09 23.94 36.57 18.89 31.11C13.43 26.06 8.91 19.39 6.42 11.92C5.76 9.92 6.29 7.71 7.78 6.22L9.5 4.5C10.74 3.26 12.78 3.26 14.02 4.5L16.84 7.32C17.94 8.42 18.09 10.16 17.2 11.42L15.4 13.98C16.47 15.72 17.85 17.67 19.5 19.5"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.8"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </view>
            <text class="action-label">语音聊天</text>
          </view>

          <view
            v-if="selectionType === 'friend' && supportsVideoCall"
            class="action-item"
            hover-class="action-item-hover"
            @click="handleStartVideo"
          >
            <view class="action-icon">
              <svg class="action-svg" viewBox="0 0 48 48" aria-hidden="true">
                <path
                  d="M10.5 15.5C10.5 13.57 12.07 12 14 12H27C28.93 12 30.5 13.57 30.5 15.5V32.5C30.5 34.43 28.93 36 27 36H14C12.07 36 10.5 34.43 10.5 32.5V15.5Z"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.8"
                  stroke-linejoin="round"
                />
                <path
                  d="M30.5 21.2L39.54 16.34C40.87 15.63 42.5 16.59 42.5 18.09V29.91C42.5 31.41 40.87 32.37 39.54 31.66L30.5 26.8V21.2Z"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.8"
                  stroke-linejoin="round"
                />
              </svg>
            </view>
            <text class="action-label">视频聊天</text>
          </view>
        </view>
      </view>
    </view>

    <view v-else class="empty-shell">
      <text class="empty-title">选择左侧联系人</text>
      <text class="empty-sub">这里会显示联系人详情和快捷操作入口。</text>
    </view>
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import LocalStateCache from '@/utils/local-state-cache'

const props = defineProps({
  selection: { type: Object, default: null },
  selectionType: { type: String, default: 'requests' },
  notifications: {
    type: Object,
    default: () => ({
      friendApplies: [],
      groupApplies: [],
    }),
  },
  defaultAvatar: { type: String, default: '' },
  supportsVideoCall: { type: Boolean, default: false },
})

const emit = defineEmits([
  'send-message',
  'start-audio',
  'start-video',
  'approve-friend',
  'reject-friend',
  'approve-group',
  'reject-group',
  'update-friend-relation',
  'update-friend-blacklist',
  'delete-friend',
])

const menuOpen = ref(false)
const starred = ref(false)
const localRemark = ref('')
const localPermissionScope = ref(0)

const CONTACT_DETAIL_CACHE_KEY_PREFIX = 'contact_detail_state'

const selectionKey = computed(() => {
  if (!props.selection) return ''
  return String(
    props.selection.id
      || props.selection.userId
      || props.selection.groupId
      || props.selection.username
      || props.selection.groupName
      || '',
  )
})

const resolveContactDetailCacheScope = () => {
  try {
    const stored = uni.getStorageSync('userInfo') || {}
    const storedId = String(stored?.id || '')
    if (storedId) {
      return `contact-detail:${storedId}`
    }
  } catch {}

  return 'contact-detail:anonymous'
}

const resolveContactDetailCacheKey = (selectionType, key) => {
  const normalizedType = String(selectionType || '').trim()
  const normalizedKey = String(key || '').trim()
  if (!normalizedType || !normalizedKey) {
    return ''
  }
  return `${CONTACT_DETAIL_CACHE_KEY_PREFIX}:${normalizedType}:${normalizedKey}`
}

const normalizeContactDetailSnapshot = (value = {}) => ({
  starred: Boolean(value?.starred),
  localRemark: String(value?.localRemark || ''),
  localPermissionScope: Number.isFinite(Number(value?.localPermissionScope))
    ? Number(value.localPermissionScope)
    : 0,
})

const readContactDetailSnapshot = (selectionType, key) => {
  const cacheKey = resolveContactDetailCacheKey(selectionType, key)
  if (!cacheKey) return null
  return LocalStateCache.getValue(resolveContactDetailCacheScope(), cacheKey)
}

const writeContactDetailSnapshot = (selectionType, key, value) => {
  const cacheKey = resolveContactDetailCacheKey(selectionType, key)
  if (!cacheKey) return
  LocalStateCache.set(
    resolveContactDetailCacheScope(),
    cacheKey,
    normalizeContactDetailSnapshot(value),
  )
}

const removeContactDetailSnapshot = (selectionType, key) => {
  const cacheKey = resolveContactDetailCacheKey(selectionType, key)
  if (!cacheKey) return
  LocalStateCache.remove(resolveContactDetailCacheScope(), cacheKey)
}

watch(
  [() => props.selectionType, selectionKey],
  () => {
    menuOpen.value = false
    const fallback = normalizeContactDetailSnapshot({
      starred: props.selection?.starred,
      localRemark: props.selection?.remarkName || '',
      localPermissionScope: props.selection?.permissionScope ?? 0,
    })
    const cached = readContactDetailSnapshot(props.selectionType, selectionKey.value)
    const next = cached ? { ...fallback, ...normalizeContactDetailSnapshot(cached) } : fallback
    starred.value = next.starred
    localRemark.value = next.localRemark
    localPermissionScope.value = next.localPermissionScope
  },
  { immediate: true },
)

watch(
  [() => props.selectionType, selectionKey, starred, localRemark, localPermissionScope],
  ([selectionType, key]) => {
    if (String(selectionType || '') !== 'friend' || !String(key || '')) {
      return
    }

    writeContactDetailSnapshot(selectionType, key, {
      starred: starred.value,
      localRemark: localRemark.value,
      localPermissionScope: localPermissionScope.value,
    })
  },
)

const avatarUrl = computed(() => {
  if (!props.selection) return props.defaultAvatar
  if (props.selectionType === 'group') {
    return props.selection.groupAvatar || props.defaultAvatar
  }
  return props.selection.avatar || props.defaultAvatar
})

const title = computed(() => {
  if (!props.selection) return ''
  if (props.selectionType === 'group') {
    return props.selection.groupName || '未命名群聊'
  }
  return props.selection.nickname || props.selection.username || props.selection.userId || '联系人'
})

const subtitle = computed(() => {
  if (!props.selection) return ''
  if (props.selectionType === 'group') {
    return `${Number(props.selection.currentMemberCount || 0)} 位成员`
  }
  return `微信号：${props.selection.username || props.selection.userId || '--'}`
})

const secondaryMeta = computed(() => {
  if (!props.selection) return ''
  if (props.selectionType === 'group') {
    return `我的身份：${roleText(props.selection.myRole)}`
  }
  return props.selection.signature || '这个联系人还没有填写签名。'
})

const showMenuButton = computed(() => props.selectionType === 'friend')

const menuItems = computed(() => [
  { key: 'remark', label: '设置备注和标签' },
  { key: 'permission', label: '设置朋友权限' },
  { key: 'recommend', label: '把他推荐给朋友' },
  { key: 'star', label: starred.value ? '取消星标朋友' : '设为星标朋友' },
  { key: 'blacklist', label: props.selection?.blacklisted ? '移出黑名单' : '加入黑名单' },
  { key: 'delete', label: '删除联系人', danger: true },
])

const detailRows = computed(() => {
  if (!props.selection) return []

  if (props.selectionType === 'group') {
    return [
      {
        label: '群聊名称',
        value: props.selection.groupName || '未命名群聊',
      },
      {
        label: '群聊 ID',
        value: String(props.selection.groupId || props.selection.id || '--'),
      },
      {
        label: '成员数量',
        value: `${Number(props.selection.currentMemberCount || 0)} 人`,
      },
      {
        label: '我的身份',
        value: roleText(props.selection.myRole),
      },
    ]
  }

  return [
    {
      label: '备注',
      value: localRemark.value || props.selection.nickname || props.selection.username || props.selection.userId || '--',
    },
    {
      label: '微信号',
      value: props.selection.username || props.selection.userId || '--',
    },
    {
      label: '个性签名',
      value: props.selection.signature || '这个联系人还没有填写签名。',
    },
    {
      label: '来源',
      value: '已在通讯录中',
    },
  ]
})

const roleText = (value) => {
  if (Number(value) === 2) return '群主'
  if (Number(value) === 1) return '管理员'
  return '群成员'
}

const closeMenu = () => {
  menuOpen.value = false
}

const toggleMenu = () => {
  menuOpen.value = !menuOpen.value
}

const handleSendMessage = () => {
  closeMenu()
  emit('send-message')
}

const handleStartAudio = () => {
  closeMenu()
  emit('start-audio')
}

const handleStartVideo = () => {
  closeMenu()
  emit('start-video')
}

const showUnsupportedToast = (title) => {
  uni.showToast({
    title,
    icon: 'none',
  })
}

const currentFriendUserId = () => String(props.selection?.userId || props.selection?.applicantId || '')

const handleMenuAction = (action) => {
  closeMenu()

  if (!props.selection) return

  if (action === 'remark') {
    uni.showModal({
      title: '设置备注',
      editable: true,
      placeholderText: '输入备注名',
      content: localRemark.value || props.selection.nickname || '',
      success: (res) => {
        if (!res.confirm) return
        localRemark.value = String(res.content || '').trim()
        emit('update-friend-relation', {
          friendUserId: currentFriendUserId(),
          remarkName: localRemark.value,
        })
      },
    })
    return
  }

  if (action === 'recommend') {
    const cardText = `${title.value} (${props.selection.username || props.selection.userId || '--'})`
    uni.setClipboardData({
      data: cardText,
      success: () => {
        uni.showToast({
          title: '联系人信息已复制',
          icon: 'none',
        })
      },
      fail: () => {
        showUnsupportedToast('复制失败，请稍后重试')
      },
    })
    return
  }

  if (action === 'star') {
    starred.value = !starred.value
    emit('update-friend-relation', {
      friendUserId: currentFriendUserId(),
      starred: starred.value,
    })
    return
  }

  if (action === 'permission') {
    uni.showActionSheet({
      itemList: ['默认权限', '仅聊天', '限制朋友圈'],
      success: ({ tapIndex }) => {
        localPermissionScope.value = tapIndex
        emit('update-friend-relation', {
          friendUserId: currentFriendUserId(),
          permissionScope: tapIndex,
        })
      },
    })
    return
  }

  if (action === 'blacklist') {
    const nextBlacklisted = !Boolean(props.selection?.blacklisted)
    uni.showModal({
      title: nextBlacklisted ? '加入黑名单' : '移出黑名单',
      content: nextBlacklisted
        ? '加入黑名单后，该联系人会从当前通讯录隐藏。'
        : '移出黑名单后，该联系人会重新回到通讯录。',
      success: (res) => {
        if (!res.confirm) return
        emit('update-friend-blacklist', {
          friendUserId: currentFriendUserId(),
          blacklisted: nextBlacklisted,
        })
      },
    })
    return
  }

  if (action === 'delete') {
    uni.showModal({
      title: '删除联系人',
      content: '删除后，该联系人会从当前通讯录中移除，但聊天记录会保留。',
      success: (res) => {
        if (!res.confirm) return
        removeContactDetailSnapshot(props.selectionType, selectionKey.value)
        emit('delete-friend', currentFriendUserId())
      },
    })
  }
}
</script>

<style scoped>
.desktop-contact-detail {
  display: flex;
  height: 100%;
  min-height: 0;
  background: #f5f5f5;
}

.detail-shell,
.requests-shell,
.empty-shell {
  position: relative;
  flex: 1;
  min-height: 0;
  padding: 34px 46px 38px;
}

.detail-shell {
  display: flex;
  justify-content: center;
  overflow: hidden;
}

.menu-scrim {
  position: absolute;
  inset: 0;
  z-index: 2;
}

.detail-wrap {
  position: relative;
  z-index: 3;
  width: min(522px, 100%);
  padding-top: 2px;
}

.profile-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) 36px;
  gap: 22px;
  align-items: start;
  padding-bottom: 24px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
}

.profile-avatar {
  width: 88px;
  height: 88px;
  border-radius: 10px;
  background: #d8dee6;
}

.profile-copy {
  min-width: 0;
  padding-top: 4px;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.profile-name {
  font-size: 20px;
  line-height: 1.25;
  font-weight: 600;
  color: #111827;
}

.profile-badge {
  min-width: 66px;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: #ebfaf1;
  color: #07c160;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.profile-line {
  display: block;
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.62;
  color: #98a1ab;
}

.profile-tools {
  position: relative;
  display: flex;
  justify-content: flex-end;
}

.profile-more-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  cursor: pointer;
}

.profile-more-btn-hover {
  background: rgba(15, 23, 42, 0.05);
}

.more-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #9aa1ab;
}

.more-menu {
  position: absolute;
  top: 20px;
  right: 0;
  width: 190px;
  padding: 8px 0;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.16);
  border: 1px solid rgba(15, 23, 42, 0.05);
  overflow: hidden;
  z-index: 5;
}

.menu-item {
  height: 56px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #1f2937;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item-hover {
  background: #f6f7f8;
}

.menu-item.danger {
  color: #ef4444;
}

.detail-table {
  margin-top: 16px;
}

.detail-row {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
  min-height: 78px;
  padding: 18px 0;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
}

.detail-label {
  font-size: 13px;
  color: #a2a9b2;
}

.detail-value {
  font-size: 13px;
  line-height: 1.7;
  color: #1f2937;
  word-break: break-word;
}

.action-row {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  gap: 58px;
  padding-top: 42px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  min-width: 92px;
  padding: 4px 0;
  border-radius: 14px;
}

.action-item-hover {
  background: rgba(15, 23, 42, 0.04);
}

.action-icon {
  width: 46px;
  height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.action-svg {
  width: 100%;
  height: 100%;
  color: #5f78ad;
}

.action-label {
  font-size: 13px;
  color: #5f78ad;
  line-height: 1.2;
}

.requests-header {
  padding-bottom: 18px;
}

.requests-title {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.requests-sub {
  display: block;
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.6;
  color: #97a0aa;
}

.requests-panels {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.request-panel {
  padding: 18px 20px 14px;
  border-radius: 10px;
  background: #ffffff;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 8px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.panel-count {
  min-width: 34px;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: #ebfaf1;
  color: #07c160;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.request-row {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 12px 0;
  border-top: 1px solid rgba(15, 23, 42, 0.06);
}

.request-avatar {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: #d8dee6;
}

.request-copy {
  min-width: 0;
}

.request-name {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #111827;
}

.request-desc {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #97a0aa;
  line-height: 1.45;
}

.request-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ghost-btn,
.primary-btn {
  min-width: 64px;
  height: 34px;
  padding: 0 12px;
  border: none;
  border-radius: 8px;
  font-size: 12px;
}

.ghost-btn {
  background: #f3f4f6;
  color: #6b7280;
}

.primary-btn {
  background: #07c160;
  color: #ffffff;
}

.request-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 110px;
  color: #97a0aa;
  font-size: 13px;
}

.empty-shell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #97a0aa;
  text-align: center;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.empty-sub {
  font-size: 13px;
  line-height: 1.7;
}
</style>
