<template>
  <DesktopSideShell>
    <template #toolbar>
      <view class="toolbar-row">
        <view class="search-shell">
          <text class="search-icon">⌕</text>
          <input
            v-model="keyword"
            class="search-input"
            placeholder="搜索"
            confirm-type="search"
          />
        </view>

        <view class="toolbar-manage" @click="openRequests">
          <view class="manage-mark compact">
            <view class="manage-mark-head"></view>
            <view class="manage-mark-body"></view>
          </view>
        </view>
      </view>
    </template>

    <view class="contact-body">
      <view class="manage-entry" @click="openRequests">
        <view class="manage-mark">
          <view class="manage-mark-head"></view>
          <view class="manage-mark-body"></view>
        </view>
        <text class="manage-label">通讯录管理</text>
        <view class="manage-arrow"></view>
      </view>

      <view class="section">
        <view class="section-head" @click="toggleRequestsSection">
          <view class="section-left">
            <view class="chevron" :class="{ open: activeSection === 'requests' }"></view>
            <text class="section-title">新的朋友</text>
          </view>
          <text class="section-count">{{ pendingCount }}</text>
        </view>

        <view v-if="activeSection === 'requests'" class="section-list">
          <view
            v-for="item in requestPreviewItems"
            :key="item.key"
            class="request-item"
            @click="openRequests"
          >
            <image class="request-avatar" :src="item.avatar || defaultAvatar" mode="aspectFill" />
            <view class="request-copy">
              <text class="request-title">{{ item.title }}</text>
              <text class="request-sub">{{ item.sub }}</text>
            </view>
          </view>

          <view v-if="!requestPreviewItems.length" class="section-empty">
            <text>暂无待处理申请</text>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-head" @click="toggleGroupsSection">
          <view class="section-left">
            <view class="chevron" :class="{ open: activeSection === 'groups' }"></view>
            <text class="section-title">群聊</text>
          </view>
          <text class="section-count">{{ groups.length }}</text>
        </view>

        <view v-if="activeSection === 'groups'" class="section-list flat-list">
          <view
            v-for="group in filteredGroups"
            :key="group.groupId || group.id"
            class="contact-item"
            :class="{ selected: isSelectedGroup(group) }"
            @click="previewGroup(group)"
          >
            <image class="contact-avatar" :src="group.groupAvatar || defaultAvatar" mode="aspectFill" />
            <view class="contact-copy">
              <text class="contact-name">{{ group.groupName || '未命名群聊' }}</text>
              <text class="contact-sub">{{ Number(group.currentMemberCount || 0) }} 位成员</text>
            </view>
          </view>

          <view v-if="!filteredGroups.length" class="section-empty">
            <text>暂无群聊</text>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-head" @click="toggleFriendsSection">
          <view class="section-left">
            <view class="chevron" :class="{ open: activeSection === 'friends' }"></view>
            <text class="section-title">联系人</text>
          </view>
          <text class="section-count">{{ friends.length }}</text>
        </view>

        <view v-if="activeSection === 'friends'" class="section-list flat-list">
          <view
            v-for="bucket in groupedFriends"
            :key="bucket.letter"
            class="letter-block"
          >
            <text class="letter-tag">{{ bucket.letter }}</text>

            <view
              v-for="friend in bucket.items"
              :key="friend.userId"
              class="contact-item"
              :class="{ selected: isSelectedFriend(friend) }"
              @click="previewFriend(friend)"
            >
              <image class="contact-avatar" :src="friend.avatar || defaultAvatar" mode="aspectFill" />
              <view class="contact-copy">
                <text class="contact-name">{{ friend.nickname || friend.username || friend.userId }}</text>
                <text class="contact-sub">{{ friend.username || friend.userId || '--' }}</text>
              </view>
            </view>
          </view>

          <view v-if="!groupedFriends.length" class="section-empty">
            <text>暂无联系人</text>
          </view>
        </view>
      </view>
    </view>
  </DesktopSideShell>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import DesktopSideShell from './DesktopSideShell.vue'
import { DEFAULT_AVATAR as defaultAvatar } from '@/utils/common'

const props = defineProps({
  friends: { type: Array, default: () => [] },
  groups: { type: Array, default: () => [] },
  notifications: {
    type: Object,
    default: () => ({
      friendApplies: [],
      groupApplies: [],
    }),
  },
  pendingCount: { type: Number, default: 0 },
  selectedKind: { type: String, default: 'requests' },
  selectedId: { type: String, default: '' },
})

const emit = defineEmits(['preview-friend', 'preview-group', 'preview-requests'])

const keyword = ref('')
const activeSection = ref('friends')

watch(
  () => props.selectedKind,
  (value) => {
    if (value === 'group') {
      activeSection.value = 'groups'
      return
    }
    if (value === 'requests') {
      activeSection.value = 'requests'
      return
    }
    activeSection.value = 'friends'
  },
  { immediate: true },
)

const query = computed(() => keyword.value.trim().toLowerCase())

const filteredGroups = computed(() => {
  if (!query.value) return props.groups
  return props.groups.filter((group) =>
    `${group.groupName || ''}${group.groupId || group.id || ''}`.toLowerCase().includes(query.value),
  )
})

const filteredFriends = computed(() => {
  if (!query.value) return props.friends
  return props.friends.filter((friend) =>
    `${friend.nickname || ''}${friend.username || ''}${friend.userId || ''}`
      .toLowerCase()
      .includes(query.value),
  )
})

const groupedFriends = computed(() => {
  const buckets = new Map()
  filteredFriends.value.forEach((friend) => {
    const seed = String(friend.nickname || friend.username || friend.userId || '#').trim()
    const firstChar = seed ? seed[0].toUpperCase() : '#'
    const key = /[A-Z]/.test(firstChar) ? firstChar : '#'
    if (!buckets.has(key)) {
      buckets.set(key, [])
    }
    buckets.get(key).push(friend)
  })

  return [...buckets.entries()]
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([letter, items]) => ({ letter, items }))
})

const requestPreviewItems = computed(() => {
  const friendItems = (props.notifications.friendApplies || []).slice(0, 2).map((item) => ({
    key: `friend-${item.id}`,
    avatar: item.applicantAvatar,
    title: item.applicantNickname || item.applicantId || '好友申请',
    sub: item.remark || '申请添加你为好友',
  }))

  const groupItems = (props.notifications.groupApplies || []).slice(0, 2).map((item) => ({
    key: `group-${item.id}`,
    avatar: item.applicantAvatar,
    title: item.applicantNickname || item.applicantId || '群聊申请',
    sub: item.groupName || item.remark || '申请加入群聊',
  }))

  return [...friendItems, ...groupItems]
})

const previewFriend = (friend) => {
  emit('preview-friend', friend)
}

const previewGroup = (group) => {
  emit('preview-group', group)
}

const openRequests = () => {
  activeSection.value = 'requests'
  emit('preview-requests')
}

const toggleRequestsSection = () => {
  if (activeSection.value === 'requests') {
    activeSection.value = ''
    return
  }
  openRequests()
}

const openGroups = () => {
  activeSection.value = 'groups'
  if (filteredGroups.value[0]) {
    emit('preview-group', filteredGroups.value[0])
  }
}

const toggleGroupsSection = () => {
  if (activeSection.value === 'groups') {
    activeSection.value = ''
    return
  }
  openGroups()
}

const openFriends = () => {
  activeSection.value = 'friends'
  const firstFriend = groupedFriends.value[0]?.items?.[0]
  if (firstFriend) {
    emit('preview-friend', firstFriend)
  }
}

const toggleFriendsSection = () => {
  if (activeSection.value === 'friends') {
    activeSection.value = ''
    return
  }
  openFriends()
}

const isSelectedFriend = (friend) =>
  props.selectedKind === 'friend' && String(props.selectedId || '') === String(friend.userId || '')

const isSelectedGroup = (group) =>
  props.selectedKind === 'group' &&
  String(props.selectedId || '') === String(group.groupId || group.id || '')
</script>

<style scoped>
.toolbar-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.search-shell {
  flex: 1;
  min-width: 0;
  height: 64rpx;
  padding: 0 18rpx;
  border-radius: 14rpx;
  background: #ebebeb;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.search-icon {
  flex-shrink: 0;
  font-size: 24rpx;
  color: #9ca3af;
}

.search-input {
  flex: 1;
  min-width: 0;
  font-size: 24rpx;
  color: #111827;
}

.toolbar-manage {
  width: 64rpx;
  height: 64rpx;
  border-radius: 14rpx;
  background: #ebebeb;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.contact-body {
  padding: 0;
  box-sizing: border-box;
}

.manage-entry {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) 12px;
  align-items: center;
  gap: 14px;
  min-height: 104rpx;
  padding: 16rpx 18rpx;
  background: #ffffff;
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.05);
}

.manage-mark {
  position: relative;
  width: 18px;
  height: 18px;
}

.manage-mark.compact {
  transform: scale(0.95);
}

.manage-mark-head {
  position: absolute;
  top: 0;
  left: 3px;
  width: 7px;
  height: 7px;
  border: 1.5px solid #4b5563;
  border-radius: 50%;
}

.manage-mark-body {
  position: absolute;
  left: 0;
  bottom: 0;
  width: 14px;
  height: 9px;
  border: 1.5px solid #4b5563;
  border-top-left-radius: 10px;
  border-top-right-radius: 10px;
  border-bottom: none;
}

.manage-label {
  font-size: 24rpx;
  font-weight: 500;
  color: #111827;
}

.manage-arrow {
  width: 8px;
  height: 8px;
  border-top: 1.5px solid #9aa0a6;
  border-right: 1.5px solid #9aa0a6;
  transform: rotate(45deg);
  justify-self: end;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 84rpx;
  padding: 0 18rpx;
}

.section-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.chevron {
  width: 8px;
  height: 8px;
  border-top: 1.5px solid #8f98a3;
  border-right: 1.5px solid #8f98a3;
  transform: rotate(45deg);
  transition: transform 0.16s ease;
}

.chevron.open {
  transform: rotate(135deg);
}

.section-title {
  font-size: 24rpx;
  font-weight: 600;
  color: #111827;
}

.section-count {
  font-size: 20rpx;
  color: #94a3b8;
}

.section-list {
  margin-top: 0;
}

.flat-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.request-item,
.contact-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  min-height: 104rpx;
  padding: 16rpx 18rpx;
  background: #ffffff;
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.05);
}

.contact-item.selected {
  background: #e9e9e9;
}

.request-avatar,
.contact-avatar {
  width: 66rpx;
  height: 66rpx;
  border-radius: 16rpx;
  background: #d1d5db;
  flex-shrink: 0;
}

.request-copy,
.contact-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.request-title,
.contact-name {
  font-size: 24rpx;
  font-weight: 600;
  color: #111827;
}

.request-sub,
.contact-sub {
  font-size: 21rpx;
  color: #94a3b8;
}

.letter-tag {
  display: block;
  padding: 16rpx 18rpx 10rpx;
  font-size: 20rpx;
  color: #94a3b8;
}

.section-empty {
  padding: 18px;
  font-size: 21rpx;
  color: #9aa0a6;
}
</style>
