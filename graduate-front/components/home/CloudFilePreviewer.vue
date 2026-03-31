<template>
  <view v-if="visible" class="preview-mask" @click.self="emitClose">
    <view class="preview-dialog">
      <view class="preview-header">
        <view class="preview-copy">
          <view class="preview-kicker">{{ previewKindLabel }}</view>
          <view class="preview-title">{{ previewTitle }}</view>
          <view class="preview-subtitle">{{ previewSubtitle }}</view>
        </view>

        <view class="preview-actions">
          <view class="preview-btn ghost" @click="emitOpenSystem">系统打开</view>
          <view class="preview-btn primary" @click="emitDownload">下载</view>
          <view class="preview-btn ghost" @click="emitClose">关闭</view>
        </view>
      </view>

      <view class="preview-body">
        <view v-if="loading" class="preview-state">
          <view class="state-title">正在准备预览</view>
          <view class="state-text">较大的文件需要一点时间，请稍等。</view>
        </view>

        <view v-else-if="mode === 'image' && state.assetUrl" class="image-wrap">
          <image class="preview-image" :src="state.assetUrl" mode="aspectFit" />
        </view>

        <view v-else-if="mode === 'video' && state.assetUrl" class="video-wrap">
          <video
            class="preview-video"
            :src="state.assetUrl"
            controls
            autoplay
            preload="metadata"
            object-fit="contain"
            show-center-play-btn
          />
        </view>

        <view v-else-if="mode === 'audio' && state.assetUrl" class="preview-state compact">
          <view class="state-title">音频预览</view>
          <view class="state-text">当前文件支持在线播放，也可以切换到系统打开。</view>
          <video
            class="audio-player"
            :src="state.assetUrl"
            controls
            autoplay
            object-fit="contain"
            :show-center-play-btn="false"
            :show-fullscreen-btn="false"
          />
        </view>

        <!-- #ifdef H5 -->
        <iframe v-else-if="mode === 'pdf' && state.assetUrl" class="preview-frame" :src="state.assetUrl"></iframe>
        <!-- #endif -->

        <scroll-view v-else-if="mode === 'text'" class="preview-scroll" scroll-y>
          <view class="text-content">{{ state.textContent || '' }}</view>
          <view v-if="state.truncated" class="preview-tip">当前只展示了部分内容，完整文件请下载查看。</view>
        </scroll-view>

        <scroll-view v-else-if="mode === 'html'" class="preview-scroll" scroll-y>
          <rich-text class="html-content" :nodes="state.htmlContent || ''" />
          <view v-if="state.truncated" class="preview-tip">当前只展示了部分内容，完整文件请下载查看。</view>
        </scroll-view>

        <view v-else class="preview-state">
          <view class="state-title">{{ state.message || '当前文件暂不支持内嵌预览' }}</view>
          <view class="state-text">你可以直接下载，或者交给系统默认程序打开。</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  file: {
    type: Object,
    default: null,
  },
  state: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['close', 'download', 'open-system'])

const mode = computed(() => props.state?.mode || props.file?.previewMode || '')
const previewTitle = computed(() => props.file?.name || props.state?.title || '文件预览')
const previewSubtitle = computed(() => {
  const parts = []
  if (props.file?.extension) {
    parts.push(String(props.file.extension || '').toUpperCase())
  }
  if (props.file?.contentType) {
    parts.push(props.file.contentType)
  }
  return parts.join(' / ') || '云盘文件预览'
})

const previewKindLabel = computed(() => {
  const labels = {
    image: 'IMAGE',
    video: 'VIDEO',
    audio: 'AUDIO',
    pdf: 'PDF',
    text: 'TEXT',
    html: 'DOC',
    document: 'DOC',
    unsupported: 'FILE',
  }
  return labels[mode.value] || 'FILE'
})

const emitClose = () => emit('close')
const emitDownload = () => emit('download')
const emitOpenSystem = () => emit('open-system')
</script>

<style scoped>
.preview-mask,
.preview-mask view {
  box-sizing: border-box;
}

.preview-mask {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24rpx;
  background:
    radial-gradient(circle at top right, rgba(88, 145, 245, 0.22), transparent 26%),
    radial-gradient(circle at bottom left, rgba(84, 205, 255, 0.16), transparent 22%),
    rgba(15, 23, 42, 0.58);
  backdrop-filter: blur(16rpx);
}

.preview-dialog {
  width: min(1420rpx, calc(100vw - 48rpx));
  height: min(1080rpx, calc(100vh - 48rpx));
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 24rpx;
  border: 1rpx solid rgba(223, 232, 245, 0.8);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 251, 255, 0.98) 100%);
  box-shadow: 0 32rpx 86rpx rgba(15, 23, 42, 0.24);
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 22rpx 24rpx;
  background: linear-gradient(180deg, rgba(253, 254, 255, 0.98) 0%, rgba(245, 249, 255, 0.96) 100%);
  border-bottom: 1rpx solid #edf2f8;
}

.preview-copy {
  min-width: 0;
  flex: 1;
}

.preview-kicker {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34rpx;
  padding: 0 12rpx;
  border-radius: 999rpx;
  background: #edf3ff;
  font-size: 18rpx;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: #5e7fd6;
}

.preview-title {
  margin-top: 8rpx;
  font-size: 32rpx;
  font-weight: 700;
  color: #1f2d3d;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.preview-subtitle {
  margin-top: 8rpx;
  font-size: 20rpx;
  color: #8c9bb0;
}

.preview-actions {
  display: flex;
  align-items: center;
  gap: 10rpx;
  flex-wrap: wrap;
}

.preview-btn {
  height: 60rpx;
  padding: 0 18rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  font-weight: 600;
  white-space: nowrap;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.preview-btn.primary {
  color: #ffffff;
  background: linear-gradient(135deg, #2f7df6 0%, #4b95ff 100%);
  box-shadow: 0 14rpx 26rpx rgba(47, 125, 246, 0.22);
}

.preview-btn.ghost {
  color: #36516f;
  background: #f2f6fd;
  border: 1rpx solid #e3ecf8;
}

.preview-body {
  flex: 1;
  min-height: 0;
  background:
    radial-gradient(circle at top right, rgba(97, 153, 247, 0.08), transparent 24%),
    linear-gradient(180deg, #f7fbff 0%, #f3f7fd 100%);
}

.preview-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32rpx;
  text-align: center;
}

.preview-state.compact {
  justify-content: flex-start;
  gap: 16rpx;
}

.state-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2d3d;
}

.state-text {
  margin-top: 12rpx;
  max-width: 720rpx;
  font-size: 22rpx;
  line-height: 1.7;
  color: #7f90a5;
}

.image-wrap,
.video-wrap {
  width: 100%;
  height: 100%;
  padding: 26rpx;
}

.preview-image,
.preview-video {
  width: 100%;
  height: 100%;
  border-radius: 20rpx;
  background: #0f172a;
  box-shadow: 0 22rpx 46rpx rgba(15, 23, 42, 0.16);
}

.audio-player {
  width: min(760rpx, 100%);
  height: 120rpx;
  margin-top: 10rpx;
}

.preview-frame {
  width: 100%;
  height: 100%;
  border: none;
  background: #ffffff;
}

.preview-scroll {
  height: 100%;
  padding: 24rpx 28rpx 32rpx;
}

.text-content,
.html-content {
  max-width: 1080rpx;
  margin: 0 auto;
  padding: 26rpx 28rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.94);
  border: 1rpx solid #e8eef8;
  box-shadow: 0 20rpx 42rpx rgba(31, 76, 152, 0.08);
  font-size: 22rpx;
  line-height: 1.8;
  color: #1f2d3d;
  white-space: pre-wrap;
  word-break: break-word;
}

.preview-tip {
  margin-top: 20rpx;
  padding: 16rpx 18rpx;
  border-radius: 16rpx;
  font-size: 20rpx;
  color: #7a6b24;
  background: #fff9e7;
}

@media (hover: hover) {
  .preview-btn:hover {
    transform: translateY(-2rpx);
  }
}

@media (max-width: 768px) {
  .preview-mask {
    padding: 12rpx;
  }

  .preview-dialog {
    width: calc(100vw - 24rpx);
    height: calc(100vh - 24rpx);
  }

  .preview-header {
    flex-wrap: wrap;
  }

  .preview-actions {
    width: 100%;
  }
}
</style>
