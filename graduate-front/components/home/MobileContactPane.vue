<template>
  <view class="mobile-contact-pane">
    <ContactHub
      variant="mobile"
      :userInfo="userInfo"
      :currentUserId="currentUserId"
      :friends="friends"
      :groups="groups"
      :notifications="notifications"
      :notifyLoading="notifyLoading"
      :pendingCount="pendingCount"
      :searchedUser="searchedUser"
      :searchingUser="searchingUser"
      :groupSearchResults="groupSearchResults"
      :searchingGroups="searchingGroups"
      :creatingGroup="creatingGroup"
      @select-friend="$emit('select-friend', $event)"
      @select-group="$emit('select-group', $event)"
      @approve-friend="$emit('approve-friend', $event)"
      @reject-friend="$emit('reject-friend', $event)"
      @approve-group="$emit('approve-group', $event)"
      @reject-group="$emit('reject-group', $event)"
      @search-user="$emit('search-user', $event)"
      @apply-friend="$emit('apply-friend', $event)"
      @search-groups="$emit('search-groups', $event)"
      @apply-group="$emit('apply-group', $event)"
      @create-group="$emit('create-group', $event)"
      @open-qr-code="$emit('open-qr-code')"
    />
  </view>
</template>

<script setup>
import ContactHub from './ContactHub.vue'

defineProps({
  userInfo: { type: Object, default: () => ({}) },
  currentUserId: { type: String, default: '' },
  friends: { type: Array, default: () => [] },
  groups: { type: Array, default: () => [] },
  notifications: { type: Object, default: () => ({ friendApplies: [], groupApplies: [] }) },
  notifyLoading: { type: Boolean, default: false },
  pendingCount: { type: Number, default: 0 },
  searchedUser: { type: Object, default: null },
  searchingUser: { type: Boolean, default: false },
  groupSearchResults: { type: Array, default: () => [] },
  searchingGroups: { type: Boolean, default: false },
  creatingGroup: { type: Boolean, default: false },
})

defineEmits([
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
</script>

<style scoped>
/* Disable text selection on mobile */
text {
  user-select: none;
  -webkit-user-select: none;
}

.mobile-contact-pane {
  height: 100%;
  padding: 18rpx 18rpx calc(24rpx + env(safe-area-inset-bottom, 0px));
  background: #f7f7f7;
}
</style>
