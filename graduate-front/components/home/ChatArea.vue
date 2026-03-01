<template>
  <view class="chat-area">
    <!-- 顶部标题栏：会话名称 + 工具按钮 -->
    <view class="chat-header">
      <text class="chat-name">{{ currentSession.sessionName }}</text>
      <view class="header-tools">
        <text class="tool-icon">📞</text>
        <text class="tool-icon">📹</text>
        <text class="tool-icon" @click.stop="$emit('toggleChatInfo')">···</text>
      </view>
    </view>

    <!-- 聊天信息侧栏（点击「···」展开），包含成员头像和配置选项 -->
    <view class="chat-info-panel" v-if="showChatInfoPanel" @click.stop>
      <view class="chat-info-avatars">
        <view
          class="avatar-item"
          v-if="friendForSession"
          @click.stop="$emit('openProfile', $event, friendForSession, friendForSession.userId)"
        >
          <image
            :id="'profile-avatar-' + friendForSession.userId"
            :src="friendForSession.avatar || defaultAvatar"
            class="info-avatar"
            mode="aspectFill"
          />
          <text class="avatar-name">{{ friendForSession.nickname }}</text>
        </view>
        <view class="avatar-item add-avatar">
          <text class="add-plus">+</text>
          <text class="avatar-name">添加</text>
        </view>
      </view>
      <view class="chat-info-options">
        <view class="option-item" @click="$emit('findContent')">查找聊天内容</view>
        <view class="option-item toggle-row">
          <text>消息免打扰</text>
          <input type="checkbox" :checked="doNotDisturb" @change="$emit('toggleDoNotDisturb')" />
        </view>
        <view class="option-item toggle-row">
          <text>置顶聊天</text>
          <input type="checkbox" :checked="pinned" @change="$emit('togglePin')" />
        </view>
        <view class="option-item option-danger" @click="$emit('clearRecords')">清空聊天记录</view>
      </view>
    </view>

    <!-- 消息列表区域：按时间分段展示，支持图片/视频/音频/文件/文本 -->
    <scroll-view
      class="chat-messages"
      scroll-y="true"
      :scroll-into-view="scrollToView"
      :scroll-with-animation="true"
      @scroll="$emit('chatScroll', $event)"
      ref="chatMessagesRef"
    >
      <view class="messages-inner-wrapper">
        <view v-if="messagesWithTime.length === 0" class="empty-tip">
          暂无消息，发送一条消息开始聊天吧
        </view>

        <template v-for="(item, index) in messagesWithTime" :key="index">
          <!-- 时间分隔线：相邻消息超过 3 分钟则显示 -->
          <view v-if="item.type === 'time'" class="time-separator">
            {{ item.timeText }}
          </view>

          <view
            v-else-if="item.type === 'msg'"
            class="message-item"
            :class="isMyMessage(item.msg) ? 'self' : 'other'"
            :id="'msg-' + item.msg.id"
          >
            <image
              :src="item.msg.avatar || defaultAvatar"
              class="message-avatar"
              mode="aspectFill"
            />

            <view class="message-content-wrapper">
              <!-- 群聊中展示发送人名称 -->
              <view
                v-if="isGroupSession && !isMyMessage(item.msg) && item.msg.sender_name"
                class="message-sender"
              >
                {{ item.msg.sender_name }}
              </view>

              <!-- 文本消息 -->
              <view
                v-if="item.msg.message_type === MESSAGE_TYPE.TEXT && item.msg.content && item.msg.content.trim()"
                class="message-content"
              >
                {{ item.msg.content }}
              </view>

              <!-- 图片消息 -->
              <image
                v-else-if="item.msg.message_type === MESSAGE_TYPE.IMAGE && item.msg.file_url"
                :src="item.msg.local_file_url || item.msg.file_url"
                class="message-image"
                @click="$emit('imageClick', item.msg)"
                mode="aspectFill"
                :lazy-load="true"
              />

              <!-- 视频消息 -->
              <video
                v-else-if="item.msg.message_type === MESSAGE_TYPE.VIDEO && item.msg.file_url"
                :src="item.msg.local_file_url || item.msg.file_url"
                class="message-video"
                controls
                show-center-play-btn
              />

              <!-- 语音消息 -->
              <view
                v-else-if="item.msg.message_type === MESSAGE_TYPE.AUDIO"
                class="message-voice"
                @click="$emit('voiceClick', item.msg)"
              >
                <text class="voice-icon">🔊</text>
                <text class="voice-duration">{{ item.msg.duration || '1' }}''</text>
                <text v-if="item.msg.is_downloading" class="downloading-text">下载中...</text>
              </view>

              <!-- 文件消息 -->
              <view
                v-else-if="item.msg.message_type === MESSAGE_TYPE.FILE && item.msg.file_url"
                class="message-file"
                @click="$emit('fileClick', item.msg)"
              >
                <text class="file-icon">📄</text>
                <view class="file-info">
                  <text class="file-name">{{ item.msg.file_name || '未知文件' }}</text>
                  <text class="file-size">{{ formatFileSize(item.msg.file_size) }}</text>
                  <text v-if="item.msg.is_downloading" class="downloading-text">
                    下载中 {{ item.msg.download_progress || 0 }}%
                  </text>
                </view>
              </view>

              <!-- 其他消息类型兜底 -->
              <view v-else class="message-content">
                {{ item.msg.content_replaced || item.msg.content || '' }}
              </view>
            </view>
          </view>
        </template>
      </view>
    </scroll-view>

    <!-- 滚动到底部浮动按钮 -->
    <view v-if="showScrollToBottom" class="scroll-to-bottom-btn" @click="$emit('scrollToBottom')">
      <text class="scroll-icon">↓</text>
    </view>

    <!-- 语音录制全屏遮罩 -->
    <view v-if="isRecording" class="recording-mask" @click.stop>
      <view class="recording-modal">
        <text class="recording-wave">🎙️</text>
        <text class="recording-modal-text">正在录音... {{ recordingDuration }}s</text>
        <view class="recording-btns">
          <button class="cancel-voice-btn" @click="$emit('cancelVoice')">取消</button>
          <button class="send-voice-btn" @click="$emit('stopAndSendVoice')">发送</button>
        </view>
      </view>
    </view>

    <!-- 输入区：待发文件预览 + 工具栏 + 输入框 + 发送按钮 -->
    <view class="chat-input-area">
      <!-- 待发文件预览列表 -->
      <view v-if="pendingFiles.length > 0" class="pending-files">
        <view
          v-for="(file, index) in pendingFiles"
          :key="index"
          class="pending-file-item"
        >
          <image
            v-if="file.message_type === MESSAGE_TYPE.IMAGE"
            :src="file.file_url"
            class="pending-file-image"
            mode="aspectFill"
          />
          <view v-else-if="file.message_type === MESSAGE_TYPE.VIDEO" class="pending-file-video">
            <text class="video-icon">🎬</text>
            <text class="file-name">{{ file.file_name }}</text>
          </view>
          <view v-else class="pending-file-doc">
            <text class="doc-icon">📄</text>
            <text class="file-name">{{ file.file_name }}</text>
          </view>
          <text class="remove-btn" @click="$emit('removePendingFile', index)">×</text>
        </view>
      </view>

      <!-- 工具栏：录音、表情、附件 -->
      <view class="input-tools">
        <view class="tool-icon-wrapper" @click="$emit('toggleVoice')">
          <text v-if="!isRecording" class="tool-icon">🎤</text>
          <view v-else class="recording-btn-active">
            <text class="recording-pulse">🔴</text>
          </view>
        </view>

        <view class="emoji-btn-wrapper">
          <text class="tool-icon" @click="$emit('toggleEmoji')">😊</text>
          <view class="emoji-panel" v-if="showEmojiPanel" @click.stop>
            <view class="emoji-section" v-if="recentEmojis.length > 0">
              <view class="emoji-title">最近使用</view>
              <view class="emoji-grid">
                <text
                  class="emoji-item"
                  v-for="(emoji, i) in recentEmojis"
                  :key="'r' + i"
                  @click="$emit('insertEmoji', emoji)"
                >{{ emoji }}</text>
              </view>
            </view>
            <view class="emoji-section">
              <view class="emoji-title">所有表情</view>
              <scroll-view class="emoji-scroll" scroll-y="true">
                <view class="emoji-grid">
                  <text
                    class="emoji-item"
                    v-for="(emoji, i) in allEmojis"
                    :key="'a' + i"
                    @click="$emit('insertEmoji', emoji)"
                  >{{ emoji }}</text>
                </view>
              </scroll-view>
            </view>
          </view>
        </view>

        <text class="tool-icon">📦</text>
        <text class="tool-icon" @click="$emit('chooseFile')">📁</text>
        <text class="tool-icon" @click="$emit('chooseImage')">🖼️</text>
      </view>

      <!-- 输入框：支持拖拽文件、Ctrl+Enter 换行、Enter 发送 -->
      <div
        class="input-container"
        @dragenter="$emit('dragEnter', $event)"
        @dragover="$emit('dragOver', $event)"
        @dragleave="$emit('dragLeave', $event)"
        @drop="$emit('drop', $event)"
        :class="{ 'drag-over': isInputDragOver }"
      >
        <textarea
          v-model="inputMsgModel"
          placeholder="输入消息..."
          class="chat-input"
          @keydown.enter="$emit('enterKey', $event)"
          @keydown.ctrl.enter="$emit('ctrlEnter')"
          :auto-height="true"
          :maxlength="-1"
        />
        <view class="send-btn-wrapper">
          <button
            class="send-btn"
            :disabled="(!inputMsgModel.trim() && pendingFiles.length === 0) || isSending"
            @click="$emit('send')"
          >
            {{ isSending ? '发送中...' : '发送(S)' }}
          </button>
        </view>
      </div>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  currentSession:    { type: Object,  required: true },
  messagesWithTime:  { type: Array,   default: () => [] },
  showChatInfoPanel: { type: Boolean, default: false },
  friendForSession:  { type: Object,  default: null },
  doNotDisturb:      { type: Boolean, default: false },
  pinned:            { type: Boolean, default: false },
  scrollToView:      { type: String,  default: '' },
  showScrollToBottom:{ type: Boolean, default: false },
  isRecording:       { type: Boolean, default: false },
  recordingDuration: { type: Number,  default: 0 },
  pendingFiles:      { type: Array,   default: () => [] },
  inputMsg:          { type: String,  default: '' },
  isSending:         { type: Boolean, default: false },
  isInputDragOver:   { type: Boolean, default: false },
  showEmojiPanel:    { type: Boolean, default: false },
  recentEmojis:      { type: Array,   default: () => [] },
  allEmojis:         { type: Array,   default: () => [] },
  defaultAvatar:     { type: String,  default: '' },
  MESSAGE_TYPE:      { type: Object,  required: true },
  SESSION_TYPE:      { type: Object,  required: true },
  currentUserId:     { type: [String, Number], default: '' },
})

const emit = defineEmits([
  'update:inputMsg',
  'toggleChatInfo', 'openProfile', 'findContent',
  'toggleDoNotDisturb', 'togglePin', 'clearRecords',
  'chatScroll', 'scrollToBottom',
  'imageClick', 'voiceClick', 'fileClick',
  'cancelVoice', 'stopAndSendVoice', 'toggleVoice',
  'toggleEmoji', 'insertEmoji',
  'chooseImage', 'chooseFile', 'removePendingFile',
  'dragEnter', 'dragOver', 'dragLeave', 'drop',
  'enterKey', 'ctrlEnter', 'send',
])

// v-model 双向绑定输入框内容
const inputMsgModel = computed({
  get: () => props.inputMsg,
  set: (v) => emit('update:inputMsg', v),
})

// 判断是否为当前用户发出的消息
const isMyMessage = (msg) => {
  if (!msg) return false
  const senderId = String(msg.sender_id || msg.senderId || '').trim()
  return senderId === String(props.currentUserId).trim() && senderId !== ''
}

// 判断是否为群聊会话
const isGroupSession = computed(() =>
  props.currentSession?.sessionType === props.SESSION_TYPE.GROUP,
)

/** 格式化文件大小：自动选择 B/KB/MB/GB */
const formatFileSize = (bytes) => {
  if (!bytes) return '0B'
  const k     = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i     = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}
</script>

<style scoped>
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
  position: relative;
  overflow: hidden;
}
</style>
