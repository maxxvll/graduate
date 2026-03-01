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
        <view class="avatar-item add-avatar" @click="$emit('addMember')">
          <view class="add-plus-wrap"><text class="add-plus">+</text></view>
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

          <!-- 系统通知消息（type=7）：居中灰色文字，不显示头像气泡 -->
          <view
            v-else-if="item.type === 'msg' && item.msg.message_type === MESSAGE_TYPE.SYSTEM"
            class="system-notify-msg"
          >
            <text class="system-notify-text">{{ item.msg.content }}</text>
          </view>

          <view
            v-else-if="item.type === 'msg'"
            class="message-item"
            :class="isMyMessage(item.msg) ? 'self' : 'other'"
            :id="'msg-' + item.msg.id"
            @contextmenu.prevent="showCtxMenu($event, item.msg)"
          >
            <image
              v-show="item.msg.status !== 3"
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

              <!-- 已撤回消息 -->
              <view
                v-if="item.msg.status === 3"
                class="message-revoked"
              >
                {{ item.msg.content_replaced || '[消息已撤回]' }}
              </view>

              <!-- 文本消息 -->
              <view
                v-else-if="item.msg.message_type === MESSAGE_TYPE.TEXT && item.msg.content && item.msg.content.trim()"
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

    <!-- 滑动到底部浮动按鈕 -->
    <view v-if="showScrollToBottom" class="scroll-to-bottom-btn" @click="$emit('scrollToBottom')">
      <text class="scroll-icon">↓</text>
    </view>
    
    <!-- PC 右键菜单 -->
    <teleport to="body">
      <div
        v-if="pcCtxMenu.visible"
        class="pc-ctx-overlay"
        @click="closePcCtxMenu"
        @contextmenu.prevent
      >
        <div
          class="pc-ctx-menu"
          :style="{ top: pcCtxMenu.y + 'px', left: pcCtxMenu.x + 'px' }"
          @click.stop
        >
          <div
            v-if="pcCtxMenu.msg && pcCtxMenu.msg.message_type === MESSAGE_TYPE.TEXT"
            class="pc-ctx-item"
            @click="doPcCopy"
          >复制</div>
          <div
            v-if="pcCtxMenu.msg && isMyMessage(pcCtxMenu.msg)"
            class="pc-ctx-item pc-ctx-revoke"
            @click="doPcRevoke"
          >撤回</div>
        </div>
      </div>
    </teleport>

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
import { computed, ref } from 'vue'

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
  'revokeMsg', 'copyMsg',
  'addMember',
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

// ========== PC 右键菜单 ==========
const pcCtxMenu = ref({ visible: false, x: 0, y: 0, msg: null })

/** 展示右键菜单 */
const showCtxMenu = (event, msg) => {
  if (msg.status === 3) return // 已撤回不展示菜单
  // 计算菜单位置，防止超出屏幕边界
  const x = Math.min(event.clientX, window.innerWidth - 140)
  const y = Math.min(event.clientY, window.innerHeight - 100)
  pcCtxMenu.value = { visible: true, x, y, msg }
}

/** 关闭右键菜单 */
const closePcCtxMenu = () => {
  pcCtxMenu.value.visible = false
}

/** PC 复制消息 */
const doPcCopy = () => {
  const text = pcCtxMenu.value.msg?.content || ''
  if (navigator.clipboard) {
    navigator.clipboard.writeText(text).then(() => {
      emit('copyMsg', text)
    })
  }
  closePcCtxMenu()
}

/** PC 撤回消息 */
const doPcRevoke = () => {
  const msg = pcCtxMenu.value.msg
  closePcCtxMenu()
  if (msg) emit('revokeMsg', msg)
}

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

/* 顶部标题栏 */
.chat-header {
  height: 52px;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
  background: #f0f0f0;
}
.chat-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}
.header-tools {
  display: flex;
  gap: 20px;
}
.header-tools .tool-icon {
  font-size: 18px;
  color: #666;
  cursor: pointer;
}

/* 聊天信息侧栏 */
.chat-info-panel {
  position: absolute;
  right: 20px;
  top: 56px;
  width: 320px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.12);
  z-index: 2000;
  overflow: hidden;
  padding: 12px;
}
.chat-info-avatars {
  display: flex;
  gap: 12px;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid #f5f5f5;
}
.avatar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  position: relative;
}
.info-avatar {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  background: #eee;
}
.avatar-name {
  font-size: 12px;
  color: #333;
  margin-top: 6px;
}
.add-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
}
.add-plus-wrap {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  border: 2px dashed #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.add-avatar .add-plus {
  font-size: 24px;
  color: #ccc;
  line-height: 1;
}
.add-avatar .avatar-name {
  margin-top: 6px;
}
.chat-info-options {
  margin-top: 8px;
}
.option-item {
  padding: 10px 4px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
}
.option-item:hover {
  color: #07c160;
}
.toggle-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.option-danger {
  color: #ff3b30;
}
.option-danger:hover {
  color: #d93025;
}

/* 消息列表 */
.chat-messages {
  flex: 1;
  background: #f0f0f0;
  display: flex;
  flex-direction: column;
  height: 0;
}
.messages-inner-wrapper {
  padding: 20px;
  width: 100%;
  box-sizing: border-box;
}
.empty-tip {
  text-align: center;
  color: #999;
  font-size: 14px;
  margin-top: 100px;
}

/* 时间分隔线 */
.time-separator {
  text-align: center;
  color: #999;
  font-size: 12px;
  margin: 16px 0;
  line-height: 1;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.time-separator::before,
.time-separator::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e6e6e6;
  max-width: 60px;
  margin: 0 12px;
}

/* 消息气泡 */
.message-item {
  display: flex;
  margin-bottom: 16px;
  width: 100%;
  align-items: flex-start;
}
.message-item.other {
  justify-content: flex-start;
  flex-direction: row;
}
.message-item.self {
  justify-content: flex-end;
  flex-direction: row;
}
.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  flex-shrink: 0;
  background: #cccccc;
}
.message-sender {
  font-size: 12px;
  color: #666;
  margin-bottom: 2px;
}
.message-item.other .message-avatar {
  order: 1;
  margin-right: 10px;
}
.message-item.self .message-avatar {
  order: 2;
  margin-left: 10px;
}
.message-content-wrapper {
  display: flex;
  flex-direction: column;
  max-width: 60%;
}
.message-item.other .message-content-wrapper {
  order: 2;
  align-items: flex-start;
}
.message-item.self .message-content-wrapper {
  order: 1;
  align-items: flex-end;
}
.message-content {
  padding: 9px 12px;
  border-radius: 4px;
  word-break: break-all;
  font-size: 14px;
  line-height: 1.5;
  max-width: 500px;
}
.message-voice {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 16px;
  border-radius: 4px;
  min-width: 60px;
  cursor: pointer;
  position: relative;
}
.message-item.other .message-voice {
  background: #fff;
  border-top-left-radius: 0;
}
.message-item.self .message-voice {
  background: #95ec69;
  border-top-right-radius: 0;
  flex-direction: row-reverse;
}
.voice-icon {
  font-size: 16px;
}
.voice-duration {
  font-size: 12px;
  color: #666;
}
.downloading-text {
  font-size: 10px;
  color: #999;
  position: absolute;
  bottom: -18px;
  right: 0;
}
.message-image {
  max-width: 300px;
  max-height: 300px;
  border-radius: 4px;
  cursor: pointer;
}
.message-video {
  max-width: 300px;
  max-height: 300px;
  border-radius: 4px;
}
.message-file {
  display: flex;
  align-items: center;
  width: 280px;
  padding: 12px 16px;
  border-radius: 4px;
  position: relative;
}
.file-icon {
  font-size: 32px;
  margin-right: 12px;
}
.file-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}
.file-name {
  font-size: 14px;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.file-size {
  font-size: 12px;
  color: #999;
}
/* 对方消息：白底；自己消息：微信绿 */
.message-item.other .message-content,
.message-item.other .message-file,
.message-item.other .message-voice {
  background: #fff;
  color: #333;
  border-top-left-radius: 0;
}
.message-item.self .message-content,
.message-item.self .message-file,
.message-item.self .message-voice {
  background: #95ec69;
  color: #333;
  border-top-right-radius: 0;
}

/* 滚动到底部按钮 */
.scroll-to-bottom-btn {
  position: absolute;
  right: 20px;
  bottom: 220px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 50;
}

/* 录音弹窗 */
.recording-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.recording-modal {
  background: #fff;
  padding: 40px 60px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}
.recording-wave {
  font-size: 48px;
}
.recording-modal-text {
  font-size: 16px;
  color: #333;
  font-weight: bold;
}
.recording-btns {
  display: flex;
  gap: 20px;
}
.cancel-voice-btn {
  padding: 8px 24px;
  background: #f5f5f5;
  color: #333;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.send-voice-btn {
  padding: 8px 24px;
  background: #07c160;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

/* 底部输入区 */
.chat-input-area {
  border-top: 1px solid #e6e6e6;
  display: flex;
  flex-direction: column;
  padding: 12px 20px;
  flex-shrink: 0;
  background: #f0f0f0;
  min-height: 180px;
  max-height: 300px;
  gap: 8px;
}
.pending-files {
  display: flex;
  gap: 10px;
  padding: 0 0 4px 0;
  flex-wrap: wrap;
}
.pending-file-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e6e6e6;
}
.pending-file-image {
  width: 100%;
  height: 100%;
}
.pending-file-video,
.pending-file-doc {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.remove-btn {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  cursor: pointer;
}
.input-tools {
  display: flex;
  flex-direction: row;
  gap: 24px;
  flex-shrink: 0;
  padding: 0 4px;
  align-items: center;
}
.tool-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 30px;
  height: 30px;
  user-select: none;
}
.recording-btn-active {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #ff3b30;
  border-radius: 4px;
}
.recording-pulse {
  font-size: 12px;
  animation: pulse 1s infinite;
}
@keyframes pulse {
  0%   { opacity: 1; transform: scale(1); }
  50%  { opacity: 0.5; transform: scale(1.2); }
  100% { opacity: 1; transform: scale(1); }
}
.emoji-btn-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
}
.input-tools .tool-icon {
  font-size: 20px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}
.input-tools .tool-icon:hover {
  color: #07c160;
}

/* 拖拽输入容器 */
.input-container {
  position: relative;
  flex: 1;
  width: 100%;
  border: 2px dashed transparent;
  border-radius: 4px;
  transition: all 0.2s;
}
.input-container.drag-over {
  border-color: #07c160;
  background: rgba(7, 193, 96, 0.05);
}
.chat-input {
  width: 100%;
  height: 100%;
  border: none;
  border-radius: 4px;
  padding: 10px 12px 45px 12px;
  font-size: 14px;
  background: #fff;
  outline: none;
  resize: none;
  line-height: 1.5;
  min-height: 80px;
  max-height: 180px;
  box-sizing: border-box;
}
.send-btn-wrapper {
  position: absolute;
  right: 12px;
  bottom: 12px;
  flex-shrink: 0;
}
.send-btn {
  padding: 6px 20px;
  background: #07c160;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
}
.send-btn:disabled {
  background: #cccccc;
  cursor: not-allowed;
}

/* 表情面板 */
.emoji-panel {
  position: absolute;
  bottom: 100%;
  left: 0;
  width: 500px;
  height: 280px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  margin-bottom: 8px;
  overflow: hidden;
  z-index: 100;
}
.emoji-section {
  padding: 0 12px;
}
.emoji-title {
  font-size: 12px;
  color: #999;
  margin: 12px 0 8px 0;
}
.emoji-grid {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 8px;
}
.emoji-scroll {
  flex: 1;
  overflow-y: auto;
}
.emoji-item {
  width: 100%;
  aspect-ratio: 1/1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  cursor: pointer;
  border-radius: 4px;
}
.emoji-item:hover {
  background: #f5f5f5;
}

/* 已撤回消息显示 */
.message-revoked {
  font-size: 13px;
  color: #aaa;
  font-style: italic;
  padding: 8px 12px;
  user-select: none;
}

/* 系统通知消息：居中显示的灰色提示文字 */
.system-notify-msg {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 6px 16px;
  margin: 4px 0;
}
.system-notify-text {
  font-size: 12px;
  color: #999;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 20px;
  padding: 3px 12px;
  user-select: none;
}

/* PC 右键菜单過粖层 */
.pc-ctx-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: transparent;
}
/* PC 右键菜单本体 */
.pc-ctx-menu {
  position: fixed;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.18);
  min-width: 120px;
  overflow: hidden;
  border: 1px solid #e8e8e8;
  z-index: 10000;
}
.pc-ctx-item {
  padding: 10px 18px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: background 0.1s;
  user-select: none;
  white-space: nowrap;
}
.pc-ctx-item:hover {
  background: #f5f5f5;
}
.pc-ctx-revoke {
  color: #e74c3c;
}
.pc-ctx-revoke:hover {
  background: #fff0ee;
}
</style>
