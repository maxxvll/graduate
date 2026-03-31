<template>
  <view class="ambient-shapes">
    <view class="ambient-shape shape-1"></view>
    <view class="ambient-shape shape-2"></view>
    <view class="ambient-shape shape-3"></view>
  </view>
</template>

<script setup>
// AmbientBackground - 背景动画组件
// 提供流动的背景形状动画
</script>

<style scoped>
.ambient-shapes {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 1;
  pointer-events: none;
}

.ambient-shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.6;
  animation: float 20s ease-in-out infinite;
}

.shape-1 {
  top: -10%;
  left: -10%;
  width: 500px;
  height: 500px;
  background: var(--ambient-green);
  animation-delay: 0s;
}

.shape-2 {
  bottom: -10%;
  right: -10%;
  width: 400px;
  height: 400px;
  background: var(--ambient-blue);
  animation-delay: -7s;
}

.shape-3 {
  top: 40%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 350px;
  height: 350px;
  background: var(--ambient-purple);
  animation-delay: -14s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(30px, -30px) scale(1.1);
  }
  50% {
    transform: translate(-20px, 20px) scale(0.9);
  }
  75% {
    transform: translate(-30px, -20px) scale(1.05);
  }
}

/* 响应式设计 */
@media (max-width: 640px) {
  .shape-1 {
    width: 300px;
    height: 300px;
  }

  .shape-2 {
    width: 250px;
    height: 250px;
  }

  .shape-3 {
    width: 200px;
    height: 200px;
  }
}
</style>
