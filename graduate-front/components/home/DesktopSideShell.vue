<template>
  <view class="desktop-side-shell">
    <view class="desktop-side-toolbar">
      <slot name="toolbar" />
    </view>

    <scroll-view class="desktop-side-scroll" scroll-y>
      <slot />
    </scroll-view>
  </view>
</template>

<style scoped>
.desktop-side-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #f5f5f5;
}

.desktop-side-toolbar {
  padding: 18rpx 18rpx 16rpx;
  background: #f5f5f5;
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.05);
}

.desktop-side-scroll {
  flex: 1;
  min-height: 0;
  background: #ffffff;
}
</style>
