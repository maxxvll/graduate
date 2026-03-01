<template>
  <!-- 遮罩层点击关闭，内容区阻止冒泡 -->
  <view v-if="show" class="qr-mask" @click.self="$emit('close')">
    <view class="qr-modal" @click.stop>
      <!-- 标题栏 -->
      <view class="qr-header">
        <text class="qr-title">我的二维码</text>
        <text class="qr-close-btn" @click="$emit('close')">✕</text>
      </view>

      <!-- 主体：头像 + 昵称 + 二维码 canvas -->
      <view class="qr-body">
        <image
          :src="avatar"
          class="qr-avatar"
          mode="aspectFill"
        />
        <text class="qr-nickname">{{ nickname }}</text>
        <text class="qr-subtitle">扫描二维码，添加为好友</text>

        <!-- 二维码渲染区域：使用 toDataURL 生成 base64，用 <img> 显示，避免 uni-app 的 canvas 组件代理问题 -->
        <view class="qr-canvas-wrapper">
          <image
            v-if="qrDataUrl && !generating"
            :src="qrDataUrl"
            class="qr-canvas"
            mode="aspectFill"
            :style="{ width: canvasSize + 'px', height: canvasSize + 'px' }"
          />
          <view v-if="generating" class="qr-loading">
            <text>生成中...</text>
          </view>
          <view v-if="!generating && !qrDataUrl" class="qr-loading">
            <text>二维码生成失败</text>
          </view>
        </view>

        <text class="qr-id-text">账号 ID：{{ wechatId }}</text>
      </view>

      <!-- 底部操作 -->
      <view class="qr-footer">
        <button class="qr-save-btn" @click="saveQrCode">保存图片</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'
// 使用 toDataURL 生成 base64 字符串，避免 uni-app 中 canvas 组件代理无法调用 getContext 的问题
import QRCode from 'qrcode'

const props = defineProps({
  show:     { type: Boolean, default: false },
  wechatId: { type: String,  default: '' },
  nickname: { type: String,  default: '' },
  avatar:   { type: String,  default: '' },
})

defineEmits(['close'])

const qrDataUrl  = ref('')
const generating = ref(false)
const canvasSize = 220

/**
 * 生成二维码为 base64 data URL。
 * 二维码内容为应用专属协议：chatapp://add?id=<wechatId>
 * 使用 toDataURL 代替 toCanvas，彻底避免 uni-app canvas 组件代理问题。
 */
const renderQrCode = async () => {
  if (!props.wechatId) return
  generating.value = true
  qrDataUrl.value  = ''
  try {
    qrDataUrl.value = await QRCode.toDataURL(`chatapp://add?id=${props.wechatId}`, {
      width:         canvasSize,
      margin:        2,
      color:         { dark: '#1a1a1a', light: '#ffffff' },
      errorCorrectionLevel: 'M',
    })
  } catch (err) {
    console.error('QrCodeModal: 二维码生成失败', err)
    qrDataUrl.value = ''
  } finally {
    generating.value = false
  }
}

/**
 * 将 base64 data URL 触发浏览器下载为 PNG。
 */
const saveQrCode = () => {
  // #ifdef H5
  if (!qrDataUrl.value) {
    uni.showToast({ title: '二维码尚未生成', icon: 'none' })
    return
  }
  const link = document.createElement('a')
  link.href = qrDataUrl.value
  link.download = `qrcode_${props.wechatId}.png`
  link.click()
  // #endif

  // #ifndef H5
  uni.showToast({ title: '请截图保存', icon: 'none' })
  // #endif
}

// 每次弹窗打开时重新生成二维码（wechatId 可能还未就绪）
watch(
  () => props.show,
  (visible) => { if (visible) renderQrCode() },
)
</script>

<style scoped>
.qr-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}
.qr-modal {
  background: #fff;
  border-radius: 16px;
  width: 360px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.18);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 标题栏 */
.qr-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 14px;
  border-bottom: 1px solid #f0f0f0;
}
.qr-title { font-size: 16px; font-weight: 600; color: #1a1a1a; }
.qr-close-btn {
  font-size: 16px;
  color: #aaa;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: color 0.15s;
}
.qr-close-btn:hover { color: #333; }

/* 主体 */
.qr-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 28px 20px 20px;
  gap: 10px;
}
.qr-avatar {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  background: #e6e6e6;
}
.qr-nickname {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}
.qr-subtitle {
  font-size: 12px;
  color: #bbb;
  margin-bottom: 4px;
}

/* 二维码 canvas 容器 */
.qr-canvas-wrapper {
  position: relative;
  width: 220px;
  height: 220px;
  background: #f9f9f9;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #ebebeb;
  display: flex;
  align-items: center;
  justify-content: center;
}
.qr-canvas {
  display: block;
  border-radius: 8px;
}
.qr-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9f9f9;
  font-size: 13px;
  color: #bbb;
}
.qr-id-text {
  font-size: 12px;
  color: #aaa;
  font-family: 'SF Mono', 'Consolas', monospace;
}

/* 底部 */
.qr-footer {
  padding: 12px 20px 20px;
  display: flex;
  justify-content: center;
}
.qr-save-btn {
  padding: 8px 28px;
  background: #1677ff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s;
  line-height: 1.5;
}
.qr-save-btn:hover { background: #0f63d9; }
</style>
