<template>
  <view class="contact-hub" :class="`is-${variant}`">
    <view class="hub-header">
      <view class="hub-tabs">
        <view
          v-for="tab in tabs"
          :key="tab.key"
          class="hub-tab"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          <text>{{ tab.label }}</text>
          <text v-if="tab.key === 'requests' && pendingCount" class="tab-badge">
            {{ pendingCount > 99 ? '99+' : pendingCount }}
          </text>
        </view>
      </view>

      <view class="hub-hero">
        <view class="hero-copy">
          <text class="hero-title">{{ heroTitle }}</text>
          <text class="hero-subtitle">{{ heroSubtitle }}</text>
        </view>
        <view class="hero-stats">
          <view class="hero-pill">
            <text class="pill-value">{{ friends.length }}</text>
            <text class="pill-label">好友</text>
          </view>
          <view class="hero-pill">
            <text class="pill-value">{{ groups.length }}</text>
            <text class="pill-label">群聊</text>
          </view>
          <view class="hero-pill">
            <text class="pill-value">{{ pendingCount }}</text>
            <text class="pill-label">待处理</text>
          </view>
        </view>
      </view>
    </view>

    <view v-if="activeTab === 'friends'" class="tab-panel">
      <view class="panel-toolbar">
        <input
          v-model="friendKeyword"
          class="panel-input"
          placeholder="搜索昵称、用户名或 ID"
          confirm-type="search"
        />
        <button class="toolbar-btn" @click="activeTab = 'discover'">添加</button>
      </view>

      <scroll-view class="panel-scroll" scroll-y>
        <view
          v-for="friend in filteredFriends"
          :key="friend.userId"
          class="list-card"
          @click="$emit('select-friend', friend)"
        >
          <image class="item-avatar" :src="friend.avatar || defaultAvatar" mode="aspectFill" />
          <view class="item-main">
            <text class="item-title">{{ friend.nickname || friend.username || friend.userId }}</text>
            <text class="item-sub">{{ friend.signature || '点击后直接进入会话' }}</text>
          </view>
          <text class="item-action">发消息</text>
        </view>

        <view v-if="!filteredFriends.length" class="empty-card">
          <text class="empty-title">还没有好友</text>
          <text class="empty-subtitle">从发现页搜索用户，或者把你的二维码发给朋友。</text>
        </view>
      </scroll-view>
    </view>

    <view v-else-if="activeTab === 'groups'" class="tab-panel">
      <view class="panel-toolbar">
        <input
          v-model="groupKeyword"
          class="panel-input"
          placeholder="搜索我的群聊"
          confirm-type="search"
        />
        <button class="toolbar-btn" @click="showCreateGroup = !showCreateGroup">
          {{ showCreateGroup ? '收起' : '建群' }}
        </button>
      </view>

      <view v-if="showCreateGroup" class="create-card">
        <input
          v-model="createGroupName"
          class="panel-input"
          maxlength="32"
          placeholder="输入群名称"
        />

        <view class="field-stack">
          <text class="field-label">入群方式</text>
          <view class="chip-group">
            <view
              v-for="item in joinTypeOptions"
              :key="item.value"
              class="chip"
              :class="{ active: createJoinType === item.value }"
              @click="createJoinType = item.value"
            >
              {{ item.label }}
            </view>
          </view>
        </view>

        <view class="field-stack">
          <text class="field-label">初始成员</text>
          <view class="chip-group">
            <view
              v-for="friend in friends"
              :key="friend.userId"
              class="chip"
              :class="{ active: selectedCreateMemberIds.includes(friend.userId) }"
              @click="toggleCreateMember(friend.userId)"
            >
              {{ friend.nickname || friend.username || friend.userId }}
            </view>
          </view>
        </view>

        <button class="primary-btn" :disabled="!canSubmitCreateGroup" @click="submitCreateGroup">
          {{ creatingGroup ? '创建中…' : '创建群聊' }}
        </button>
      </view>

      <scroll-view class="panel-scroll" scroll-y>
        <view
          v-for="group in filteredGroups"
          :key="group.groupId || group.id"
          class="list-card"
          @click="$emit('select-group', group)"
        >
          <image class="item-avatar" :src="group.groupAvatar || defaultAvatar" mode="aspectFill" />
          <view class="item-main">
            <text class="item-title">{{ group.groupName }}</text>
            <text class="item-sub">
              {{ group.currentMemberCount || group.memberCount || 0 }} 人 · {{ roleText(group.myRole) }}
            </text>
          </view>
          <text class="item-action">进入</text>
        </view>

        <view v-if="!filteredGroups.length" class="empty-card">
          <text class="empty-title">你还没有群聊</text>
          <text class="empty-subtitle">可以建一个新群，或者在发现页申请加入。</text>
        </view>
      </scroll-view>
    </view>

    <view v-else-if="activeTab === 'requests'" class="tab-panel">
      <scroll-view class="panel-scroll" scroll-y>
        <view class="request-section">
          <view class="section-head">
            <text class="section-title">好友申请</text>
            <text class="section-count">{{ notifications.friendApplies.length }}</text>
          </view>

          <view
            v-for="item in notifications.friendApplies"
            :key="`friend-${item.id}`"
            class="request-card"
          >
            <view class="request-main">
              <image class="item-avatar" :src="item.applicantAvatar || defaultAvatar" mode="aspectFill" />
              <view class="item-main">
                <text class="item-title">{{ item.applicantNickname || item.applicantId }}</text>
                <text class="item-sub">{{ item.remark || '想把你添加为好友' }}</text>
              </view>
            </view>
            <view class="request-actions">
              <button class="ghost-btn" @click="$emit('reject-friend', item)">拒绝</button>
              <button class="primary-btn compact" @click="$emit('approve-friend', item)">通过</button>
            </view>
          </view>
        </view>

        <view class="request-section">
          <view class="section-head">
            <text class="section-title">群聊申请</text>
            <text class="section-count">{{ notifications.groupApplies.length }}</text>
          </view>

          <view
            v-for="item in notifications.groupApplies"
            :key="`group-${item.id}`"
            class="request-card"
          >
            <view class="request-main">
              <image class="item-avatar" :src="item.applicantAvatar || defaultAvatar" mode="aspectFill" />
              <view class="item-main">
                <text class="item-title">{{ item.applicantNickname || item.applicantId }}</text>
                <text class="item-sub">{{ item.groupName }} · {{ item.remark || '申请加入群聊' }}</text>
              </view>
            </view>
            <view class="request-actions">
              <button class="ghost-btn" @click="$emit('reject-group', item)">拒绝</button>
              <button class="primary-btn compact" @click="$emit('approve-group', item)">通过</button>
            </view>
          </view>
        </view>

        <view v-if="notifyLoading" class="empty-card">
          <text class="empty-title">通知加载中</text>
          <text class="empty-subtitle">马上就好。</text>
        </view>

        <view
          v-else-if="!notifications.friendApplies.length && !notifications.groupApplies.length"
          class="empty-card"
        >
          <text class="empty-title">没有待处理申请</text>
          <text class="empty-subtitle">新申请会在这里集中显示。</text>
        </view>
      </scroll-view>
    </view>

    <view v-else class="tab-panel">
      <scroll-view class="panel-scroll" scroll-y>
        <view class="discover-card">
          <view class="discover-head">
            <view>
              <text class="discover-title">找人和建联</text>
              <text class="discover-subtitle">输入用户名、昵称或手机号搜索。</text>
            </view>
            <button class="ghost-btn compact" @click="$emit('open-qr-code')">我的二维码</button>
          </view>

          <input
            v-model="userSearchKeyword"
            class="panel-input"
            placeholder="搜索用户"
            confirm-type="search"
          />
          <input
            v-model="friendRemark"
            class="panel-input"
            maxlength="100"
            placeholder="验证信息（可选）"
          />
          <button class="primary-btn" :disabled="!userSearchKeyword.trim() || searchingUser" @click="searchUser">
            {{ searchingUser ? '搜索中…' : '搜索用户' }}
          </button>

          <view v-if="searchedUser" class="result-card">
            <view class="request-main">
              <image class="item-avatar" :src="searchedUser.avatar || defaultAvatar" mode="aspectFill" />
              <view class="item-main">
                <text class="item-title">{{ searchedUser.nickname || searchedUser.username }}</text>
                <text class="item-sub">{{ searchedUser.signature || searchedUser.username }}</text>
              </view>
            </view>
            <button
              class="primary-btn compact"
              :disabled="isSearchedUserFriend || sameAsCurrentUser"
              @click="applyFriend"
            >
              {{ sameAsCurrentUser ? '当前账号' : isSearchedUserFriend ? '已是好友' : '发送申请' }}
            </button>
          </view>
        </view>

        <view class="discover-card">
          <view class="discover-head">
            <view>
              <text class="discover-title">发现群聊</text>
              <text class="discover-subtitle">支持分页接口，结果会更稳。</text>
            </view>
          </view>

          <input
            v-model="groupSearchKeyword"
            class="panel-input"
            placeholder="搜索群名称"
            confirm-type="search"
          />
          <button class="primary-btn" :disabled="!groupSearchKeyword.trim() || searchingGroups" @click="searchGroups">
            {{ searchingGroups ? '搜索中…' : '搜索群聊' }}
          </button>

          <view v-for="group in groupSearchResults" :key="group.id" class="result-card">
            <view class="request-main">
              <image class="item-avatar" :src="group.groupAvatar || defaultAvatar" mode="aspectFill" />
              <view class="item-main">
                <text class="item-title">{{ group.groupName }}</text>
                <text class="item-sub">
                  {{ group.currentMemberCount || 0 }} 人 · {{ joinTypeText(group.joinType) }}
                </text>
              </view>
            </view>
            <button
              class="primary-btn compact"
              :disabled="group.applyStatus === 'member' || group.applyStatus === 'pending'"
              @click="$emit('apply-group', group)"
            >
              {{
                group.applyStatus === 'member'
                  ? '已加入'
                  : group.applyStatus === 'pending'
                    ? '审核中'
                    : '申请加入'
              }}
            </button>
          </view>

          <view v-if="!groupSearchResults.length && searchedGroups" class="empty-card compact-empty">
            <text class="empty-title">没有找到匹配的群聊</text>
            <text class="empty-subtitle">可以换一个关键词再试一次。</text>
          </view>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { DEFAULT_AVATAR as defaultAvatar } from '@/utils/common'

const props = defineProps({
  variant: { type: String, default: 'desktop' },
  userInfo: { type: Object, default: () => ({}) },
  currentUserId: { type: String, default: '' },
  friends: { type: Array, default: () => [] },
  groups: { type: Array, default: () => [] },
  notifications: {
    type: Object,
    default: () => ({ friendApplies: [], groupApplies: [] }),
  },
  notifyLoading: { type: Boolean, default: false },
  pendingCount: { type: Number, default: 0 },
  searchedUser: { type: Object, default: null },
  searchingUser: { type: Boolean, default: false },
  groupSearchResults: { type: Array, default: () => [] },
  searchingGroups: { type: Boolean, default: false },
  creatingGroup: { type: Boolean, default: false },
})

const emit = defineEmits([
  'select-friend',
  'select-group',
  'approve-friend',
  'reject-friend',
  'approve-group',
  'reject-group',
  'search-user',
  'apply-friend',
  'search-groups',
  'apply-group',
  'create-group',
  'open-qr-code',
])

const tabs = [
  { key: 'friends', label: '好友' },
  { key: 'groups', label: '群聊' },
  { key: 'requests', label: '通知' },
  { key: 'discover', label: '发现' },
]

const joinTypeOptions = [
  { label: '需审核', value: 1 },
  { label: '免审核', value: 2 },
  { label: '仅邀请', value: 3 },
]

const activeTab = ref('friends')
const friendKeyword = ref('')
const groupKeyword = ref('')
const userSearchKeyword = ref('')
const groupSearchKeyword = ref('')
const friendRemark = ref('')
const createGroupName = ref('')
const createJoinType = ref(1)
const showCreateGroup = ref(false)
const selectedCreateMemberIds = ref([])
const searchedGroups = ref(false)

const heroTitle = computed(() => {
  if (activeTab.value === 'friends') return '联系人'
  if (activeTab.value === 'groups') return '群组'
  if (activeTab.value === 'requests') return '通知中心'
  return '发现更多连接'
})

const heroSubtitle = computed(() => {
  if (activeTab.value === 'friends') {
    return `你好，${props.userInfo?.nickname || props.userInfo?.username || '欢迎回来'}。`
  }
  if (activeTab.value === 'groups') {
    return '在这里管理已有群聊，也可以快速创建新群。'
  }
  if (activeTab.value === 'requests') {
    return '好友申请和群申请都集中在这里处理。'
  }
  return '搜索用户、搜索群聊，或者把二维码发出去。'
})

const filteredFriends = computed(() => {
  const query = friendKeyword.value.trim().toLowerCase()
  if (!query) return props.friends
  return props.friends.filter((item) =>
    `${item.nickname || ''}${item.username || ''}${item.userId || ''}`.toLowerCase().includes(query),
  )
})

const filteredGroups = computed(() => {
  const query = groupKeyword.value.trim().toLowerCase()
  if (!query) return props.groups
  return props.groups.filter((item) =>
    `${item.groupName || ''}${item.groupId || ''}`.toLowerCase().includes(query),
  )
})

const isSearchedUserFriend = computed(() =>
  props.friends.some((item) => String(item.userId) === String(props.searchedUser?.id)),
)

const sameAsCurrentUser = computed(
  () => String(props.currentUserId || '') === String(props.searchedUser?.id || ''),
)

const canSubmitCreateGroup = computed(
  () => createGroupName.value.trim() && selectedCreateMemberIds.value.length > 0 && !props.creatingGroup,
)

const roleText = (role) => {
  if (Number(role) === 1) return '群主'
  if (Number(role) === 2) return '管理员'
  return '成员'
}

const joinTypeText = (value) => {
  if (Number(value) === 1) return '需审核'
  if (Number(value) === 2) return '免审核'
  return '仅邀请'
}

const toggleCreateMember = (userId) => {
  const exists = selectedCreateMemberIds.value.includes(userId)
  selectedCreateMemberIds.value = exists
    ? selectedCreateMemberIds.value.filter((item) => item !== userId)
    : [...selectedCreateMemberIds.value, userId]
}

const submitCreateGroup = () => {
  emit('create-group', {
    groupName: createGroupName.value.trim(),
    joinType: createJoinType.value,
    memberIds: [...selectedCreateMemberIds.value],
  })
}

const searchUser = () => {
  emit('search-user', userSearchKeyword.value.trim())
}

const applyFriend = () => {
  if (!props.searchedUser?.id) return
  emit('apply-friend', {
    targetId: String(props.searchedUser.id),
    remark: friendRemark.value.trim(),
  })
}

const searchGroups = () => {
  searchedGroups.value = true
  emit('search-groups', groupSearchKeyword.value.trim())
}

watch(
  () => props.creatingGroup,
  (busy) => {
    if (!busy && createGroupName.value && selectedCreateMemberIds.value.length) {
      createGroupName.value = ''
      createJoinType.value = 1
      selectedCreateMemberIds.value = []
      showCreateGroup.value = false
      activeTab.value = 'groups'
    }
  },
)
</script>

<style scoped>
.contact-hub {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.hub-header {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.hub-tabs {
  display: flex;
  gap: 12rpx;
  padding: 8rpx;
  border-radius: 24rpx;
  background: rgba(15, 23, 42, 0.04);
}

.hub-tab {
  position: relative;
  flex: 1;
  height: 72rpx;
  border-radius: 18rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  font-size: 24rpx;
  color: #6b7280;
}

.hub-tab.active {
  background: #ffffff;
  color: #111827;
  box-shadow: 0 10rpx 24rpx rgba(15, 23, 42, 0.08);
}

.tab-badge {
  min-width: 34rpx;
  height: 34rpx;
  padding: 0 10rpx;
  border-radius: 999rpx;
  background: #fa5151;
  color: #ffffff;
  font-size: 20rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.hub-hero {
  padding: 24rpx;
  border-radius: 30rpx;
  background:
    radial-gradient(circle at top right, rgba(7, 193, 96, 0.18), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 249, 250, 0.96));
  box-shadow: 0 16rpx 36rpx rgba(15, 23, 42, 0.08);
}

.hero-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #111827;
}

.hero-subtitle,
.pill-label,
.item-sub,
.empty-subtitle,
.discover-subtitle,
.field-label,
.section-count {
  font-size: 23rpx;
  color: #6b7280;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
  margin-top: 18rpx;
}

.hero-pill {
  padding: 18rpx;
  border-radius: 22rpx;
  background: rgba(15, 23, 42, 0.04);
}

.pill-value {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #07c160;
  margin-bottom: 8rpx;
}

.tab-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1;
  padding-top: 18rpx;
}

.panel-toolbar {
  display: flex;
  gap: 14rpx;
  margin-bottom: 18rpx;
}

.panel-input {
  width: 100%;
  min-height: 78rpx;
  padding: 0 24rpx;
  border-radius: 22rpx;
  background: rgba(243, 244, 246, 0.92);
  font-size: 25rpx;
  color: #111827;
}

.toolbar-btn,
.ghost-btn,
.primary-btn {
  height: 78rpx;
  padding: 0 26rpx;
  border-radius: 22rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 25rpx;
  font-weight: 600;
}

.toolbar-btn,
.ghost-btn {
  background: rgba(15, 23, 42, 0.06);
  color: #111827;
}

.primary-btn {
  background: linear-gradient(135deg, #07c160 0%, #29d17c 100%);
  color: #ffffff;
}

.primary-btn.compact,
.ghost-btn.compact {
  height: 64rpx;
  font-size: 23rpx;
}

.panel-scroll {
  flex: 1;
  min-height: 0;
}

.list-card,
.create-card,
.request-card,
.discover-card,
.result-card,
.empty-card {
  margin-bottom: 16rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14rpx 34rpx rgba(15, 23, 42, 0.06);
}

.list-card {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 18rpx;
}

.item-avatar {
  width: 82rpx;
  height: 82rpx;
  border-radius: 24rpx;
  background: #e5e7eb;
  flex-shrink: 0;
}

.item-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.item-title,
.section-title,
.discover-title,
.empty-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #111827;
}

.item-action {
  font-size: 23rpx;
  color: #07c160;
}

.create-card,
.discover-card,
.request-card,
.result-card,
.empty-card {
  padding: 22rpx;
}

.field-stack {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.chip {
  padding: 12rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(15, 23, 42, 0.06);
  color: #4b5563;
  font-size: 23rpx;
}

.chip.active {
  background: rgba(7, 193, 96, 0.14);
  color: #07c160;
}

.request-section + .request-section {
  margin-top: 20rpx;
}

.section-head,
.discover-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14rpx;
  margin-bottom: 16rpx;
}

.request-main {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.request-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12rpx;
  margin-top: 16rpx;
}

.compact-empty {
  text-align: center;
}

.empty-card {
  text-align: center;
}

.empty-subtitle {
  display: block;
  margin-top: 10rpx;
}

.is-mobile .hero-stats {
  grid-template-columns: repeat(3, 1fr);
}
</style>
