import { ref } from 'vue'
import service from '@/utils/request'
import ChatStorage from '@/utils/chat-storage'

// #ifdef H5
import SparkMD5 from 'spark-md5'
// #endif

/**
 * useSendMessage — 消息发送、文件上传、语音录制、拖拽输入的完整逻辑。
 *
 * 该 composable 封装了聊天输入区域的所有行为，暴露给 ChatArea 或 home.vue 使用。
 *
 * 使用方式：
 *   const { inputMsg, pendingFiles, isSending, isRecording, recordingDuration,
 *           isInputDragOver, showEmojiPanel, recentEmojis, allEmojis,
 *           sendMessageWithFiles, toggleVoiceRecording, stopAndSendVoice,
 *           cancelVoice, chooseImage, chooseFile, removePendingFile,
 *           onInputDragEnter, onInputDragOver, onInputDragLeave, onInputDrop,
 *           handleEnterKey, handleCtrlEnter, toggleEmojiPanel, insertEmoji,
 *           cleanup } = useSendMessage({ currentSession, CURRENT_USER_ID, messages,
 *                                        cleanMessage, scrollToBottom, updateSessionLastMsg,
 *                                        updateMsgInList, SESSION_TYPE, MESSAGE_TYPE,
 *                                        SEND_STATUS, CHUNK_SIZE })
 */
export function useSendMessage({
  currentSession,
  CURRENT_USER_ID,
  messages,
  cleanMessage,
  scrollToBottom,
  updateSessionLastMsg,
  SESSION_TYPE,
  MESSAGE_TYPE,
  SEND_STATUS,
  CHUNK_SIZE,
}) {
  // ─── 输入区状态 ────────────────────────────────────────────────────────────
  const inputMsg        = ref('')
  const pendingFiles    = ref([])
  const isSending       = ref(false)
  const isInputDragOver = ref(false)
  const showEmojiPanel  = ref(false)
  const recentEmojis    = ref([])
  const allEmojis       = ref([
    '😀', '😅', '😍', '😭', '😊', '😎', '😢', '😡',
    '🥳', '😱', '🤔', '👍', '👎', '🙏', '🎉', '❤️',
    '😂', '🤣', '😇', '🤩', '🥺', '😤', '🤯', '😴',
  ])

  // ─── 语音录制状态 ───────────────────────────────────────────────────────────
  const isRecording       = ref(false)
  const recordingDuration = ref(0)
  let mediaRecorder  = null
  let audioStream    = null
  let recordingTimer = null
  let msgIdCounter   = 0

  // ─── 工具函数 ────────────────────────────────────────────────────────────────

  const generateMsgId = () => Date.now() + ++msgIdCounter

  /**
   * 解包 axios 响应中的嵌套 data 字段，兼容后端返回 {data: {data: ...}} 的情况。
   */
  const unwrapData = (res) => {
    let data = res
    if (data && data.data) data = data.data
    if (data && data.data) data = data.data
    return data
  }

  /**
   * 计算文件 MD5，用于大文件秒传和断点续传的唯一标识。
   * H5 使用 SparkMD5 逐片读取文件，非 H5 环境返回临时 mock 值。
   */
  const calculateFileMD5 = (file) => {
    return new Promise((resolve, reject) => {
      // #ifdef H5
      if (!file) return reject('No file')
      const reader    = new FileReader()
      const spark     = new SparkMD5.ArrayBuffer()
      const chunkSize = 2 * 1024 * 1024
      const chunks    = Math.ceil(file.size / chunkSize)
      let currentChunk = 0
      reader.onload  = (e) => {
        spark.append(e.target.result)
        currentChunk++
        if (currentChunk < chunks) loadNext()
        else resolve(spark.end())
      }
      reader.onerror = () => reject(new Error('MD5 计算失败'))
      const loadNext = () => {
        const start = currentChunk * chunkSize
        reader.readAsArrayBuffer(file.slice(start, Math.min(start + chunkSize, file.size)))
      }
      loadNext()
      // #endif
      // #ifndef H5
      resolve(`md5_${Date.now()}`)
      // #endif
    })
  }

  // ─── 核心发送逻辑 ─────────────────────────────────────────────────────────────

  /**
   * 消息发送核心方法，统一处理文本、图片、视频、文件、语音等所有类型。
   *
   * 流程：
   *  1. 立即创建本地占位消息（PENDING 状态）推入列表，让用户即时看到发送中状态
   *  2. 如有附件：
   *     - 小文件（< 10MB）：文件与消息元数据合并为一次 multipart 请求，直接完成发送
   *     - 大文件（>= 10MB）：分片上传（支持秒传和断点续传）→ 服务端合并 → 获得最终 URL
   *  3. 纯文本消息或大文件上传完毕后，单独调 /chat/message/send 完成消息投递
   *  4. 将服务端确认后的消息状态写入本地缓存，更新 UI 为 SUCCESS
   *  5. 任意步骤异常：将消息标记为 FAILED，展示错误提示
   */
  const sendSingleMessage = async (msgData) => {
    const sessionId = currentSession.value.sessionId
    const newId     = generateMsgId()
    const nowTime   = new Date().toISOString()

    // 立即创建占位消息，确保用户看到即时反馈
    const localMsg = cleanMessage({
      id:            newId,
      session_id:    sessionId,
      message_type:  msgData.message_type,
      content:       msgData.content,
      local_file_url: msgData.file_url,
      file_url:      msgData.file_url,
      file_name:     msgData.file_name,
      file_size:     msgData.file_size,
      duration:      msgData.duration,
      send_time:     nowTime,
      send_status:   SEND_STATUS.PENDING,
      sender_id:     CURRENT_USER_ID.value,
    })

    messages.value.push(localMsg)
    updateSessionLastMsg(sessionId, msgData.content, nowTime)
    scrollToBottom()

    try {
      let finalFileUrl  = null
      let finalFileName = msgData.file_name
      let finalFileSize = msgData.file_size
      // 小文件通过 multipart 一次性完成「上传 + 发送」，无需再调 send 接口
      let alreadySent = false

      if (msgData.originalFile) {
        const file = msgData.originalFile
        if (file.size < 10 * 1024 * 1024) {
          // 小文件：文件与消息元数据同一请求，服务端处理后直接投递
          const sendDTO = {
            messageNo:   localMsg.messageNo,
            sessionId,
            sessionType: currentSession.value.sessionType,
            senderId:    CURRENT_USER_ID.value,
            receiverId:  currentSession.value.sessionType === SESSION_TYPE.SINGLE
              ? currentSession.value.targetId : null,
            messageType: msgData.message_type,
            content:     msgData.content,
            duration:    msgData.duration,
            fileUrl:     null,
            fileName:    file.name,
            fileSize:    file.size,
          }
          const formData = new FormData()
          formData.append('sendDTO', JSON.stringify(sendDTO))
          formData.append('files', file)
          const res  = await service({ url: '/chat/message/send', method: 'post', data: formData })
          const data = unwrapData(res)
          finalFileUrl  = data?.fileUrl || data?.file_url || null
          finalFileName = file.name
          finalFileSize = file.size
          alreadySent   = true
        } else {
          // 大文件分片上传：秒传检测 → 上传缺失分片 → 服务端合并 → 返回最终 URL
          const fileMD5     = await calculateFileMD5(file)
          const totalChunks = Math.ceil(file.size / CHUNK_SIZE)

          const checkRes  = await service({ url: '/chat/file/check', method: 'post', params: { md5: fileMD5, fileName: file.name } })
          const checkData = unwrapData(checkRes)
          if (!checkData) throw new Error('文件检查接口返回数据异常')

          if (!checkData.shouldUpload) {
            // 服务端已有同 MD5 文件，直接复用（秒传）
            finalFileUrl = checkData.fileUrl
          } else {
            const uploadedChunks = checkData.uploadedChunks || []
            for (let i = 0; i < totalChunks; i++) {
              if (uploadedChunks.includes(i)) continue
              const start     = i * CHUNK_SIZE
              const chunkForm = new FormData()
              chunkForm.append('md5', fileMD5)
              chunkForm.append('chunkIndex', i)
              chunkForm.append('fileName', file.name)
              chunkForm.append('totalChunks', totalChunks)
              chunkForm.append('fileSize', file.size)
              chunkForm.append('file', file.slice(start, Math.min(start + CHUNK_SIZE, file.size)))
              await service({ url: '/chat/file/upload-chunk', method: 'post', data: chunkForm })
            }
            // 通知服务端合并所有分片，返回最终可访问的文件 URL
            const mergeRes  = await service({
              url: '/chat/file/merge', method: 'post',
              params: { md5: fileMD5, fileName: file.name, totalChunks, isPublic: currentSession.value.sessionType === SESSION_TYPE.GROUP },
            })
            const mergedData = unwrapData(mergeRes)
            finalFileUrl = typeof mergedData === 'object'
              ? (mergedData?.url || mergedData?.fileUrl || null)
              : mergedData
          }
          finalFileName = file.name
          finalFileSize = file.size
        }
      }

      // 纯文本消息或大文件上传完毕后，单独调 send 接口完成消息投递
      if (!alreadySent) {
        const sendDTO = {
          messageNo:   localMsg.messageNo,
          sessionId,
          sessionType: currentSession.value.sessionType,
          senderId:    CURRENT_USER_ID.value,
          receiverId:  currentSession.value.sessionType === SESSION_TYPE.SINGLE
            ? currentSession.value.targetId : null,
          messageType: msgData.message_type,
          content:     msgData.content,
          duration:    msgData.duration,
          fileUrl:     finalFileUrl,
          fileName:    finalFileName,
          fileSize:    finalFileSize,
        }
        const formData = new FormData()
        formData.append('sendDTO', JSON.stringify(sendDTO))
        const res  = await service({ url: '/chat/message/send', method: 'post', data: formData })
        const data = unwrapData(res)
        // 以服务端返回的 URL 为准（如有转码处理）
        if (data?.fileUrl) finalFileUrl = data.fileUrl
      }

      // 将发送成功的最终消息写入本地缓存，并同步更新 UI 状态
      const serverMsg = {
        ...localMsg,
        file_url:     finalFileUrl,
        file_name:    finalFileName,
        file_size:    finalFileSize,
        send_status:  SEND_STATUS.SUCCESS,
      }
      serverMsg.local_file_url = localMsg.local_file_url
      const idx = messages.value.findIndex((m) => m.messageNo === localMsg.messageNo)
      if (idx !== -1) {
        messages.value[idx] = cleanMessage(serverMsg)
        if (ChatStorage.insertMessage) await ChatStorage.insertMessage(messages.value[idx])
      }
    } catch (err) {
      console.error('消息发送异常:', err)
      const failIdx = messages.value.findIndex((m) => m.id === newId)
      if (failIdx !== -1) {
        messages.value[failIdx].send_status = SEND_STATUS.FAILED
        messages.value = [...messages.value]
      }
      uni.showToast({ title: err.message || '发送失败', icon: 'none' })
    }
  }

  /**
   * 发送按钮触发：先发文本再按序发每个附件。
   * 文本和文件分开发送，确保消息顺序正确。
   */
  const sendMessageWithFiles = async () => {
    if (isSending.value || !currentSession.value) return
    const hasText  = inputMsg.value.trim()
    const hasFiles = pendingFiles.value.length > 0
    if (!hasText && !hasFiles) return
    isSending.value = true
    try {
      if (hasText) {
        await sendSingleMessage({ message_type: MESSAGE_TYPE.TEXT, content: inputMsg.value, originalFile: null })
        inputMsg.value = ''
      }
      for (const file of pendingFiles.value) {
        await sendSingleMessage(file)
      }
      pendingFiles.value = []
    } catch (err) {
      console.error('批量发送失败', err)
      uni.showToast({ title: '发送失败', icon: 'none' })
    } finally {
      isSending.value = false
    }
  }

  // ─── 键盘事件 ────────────────────────────────────────────────────────────────

  /** Enter 发送，Ctrl+Enter 换行 */
  const handleEnterKey = (e) => {
    if (!e.ctrlKey) {
      e.preventDefault()
      sendMessageWithFiles()
    }
  }
  const handleCtrlEnter = () => {
    inputMsg.value += '\n'
  }

  // ─── 表情面板 ────────────────────────────────────────────────────────────────

  const toggleEmojiPanel = () => {
    showEmojiPanel.value = !showEmojiPanel.value
  }

  /** 插入表情到输入框，同时更新最近使用列表（最多保留 8 个） */
  const insertEmoji = (emoji) => {
    inputMsg.value += emoji
    let list = recentEmojis.value.filter((e) => e !== emoji)
    list.unshift(emoji)
    recentEmojis.value = list.slice(0, 8)
  }

  // ─── 文件选择 ────────────────────────────────────────────────────────────────

  const chooseImage = () => {
    // #ifdef H5
    const input    = document.createElement('input')
    input.type     = 'file'
    input.accept   = 'image/*'
    input.onchange = (e) => {
      const file = e.target.files[0]
      if (!file) return
      pendingFiles.value.push({
        message_type: MESSAGE_TYPE.IMAGE,
        content:      '[图片]',
        file_url:     URL.createObjectURL(file),
        file_name:    file.name,
        file_size:    file.size,
        originalFile: file,
      })
    }
    input.click()
    // #endif
  }

  const chooseFile = () => {
    // #ifdef H5
    const input    = document.createElement('input')
    input.type     = 'file'
    input.onchange = (e) => {
      const file = e.target.files[0]
      if (!file) return
      pendingFiles.value.push({
        message_type: MESSAGE_TYPE.FILE,
        content:      '[文件]',
        file_url:     URL.createObjectURL(file),
        file_name:    file.name,
        file_size:    file.size,
        originalFile: file,
      })
    }
    input.click()
    // #endif
  }

  const removePendingFile = (index) => {
    pendingFiles.value.splice(index, 1)
  }

  // ─── 拖拽上传 ────────────────────────────────────────────────────────────────

  const onInputDragEnter = (e) => { e.preventDefault(); isInputDragOver.value = true }
  const onInputDragOver  = (e) => { e.preventDefault(); isInputDragOver.value = true }
  const onInputDragLeave = (e) => { e.preventDefault(); isInputDragOver.value = false }

  /** 拖拽文件到输入区自动识别类型并加入待发队列 */
  const onInputDrop = (e) => {
    e.preventDefault()
    isInputDragOver.value = false
    // #ifdef H5
    const files = e.dataTransfer?.files
    if (!files) return
    Array.from(files).forEach((file) => {
      let msgType = MESSAGE_TYPE.FILE
      if (file.type.startsWith('image/'))      msgType = MESSAGE_TYPE.IMAGE
      else if (file.type.startsWith('video/')) msgType = MESSAGE_TYPE.VIDEO
      const label = msgType === MESSAGE_TYPE.IMAGE ? '图片' : msgType === MESSAGE_TYPE.VIDEO ? '视频' : '文件'
      pendingFiles.value.push({
        message_type: msgType,
        content:      `[${label}]`,
        file_url:     URL.createObjectURL(file),
        file_name:    file.name,
        file_size:    file.size,
        originalFile: file,
      })
    })
    // #endif
  }

  // ─── 语音录制 ────────────────────────────────────────────────────────────────

  const startRecordingTimer = () => {
    recordingDuration.value = 0
    if (recordingTimer) clearInterval(recordingTimer)
    recordingTimer = setInterval(() => recordingDuration.value++, 1000)
  }

  const stopRecordingTimer = () => {
    if (recordingTimer) clearInterval(recordingTimer)
    recordingTimer = null
  }

  /**
   * 语音录制完成后处理：最短 1 秒，构造语音文件对象加入发送流程。
   */
  const processVoiceData = async (tempUrl, duration, fileSize, blob) => {
    if (recordingDuration.value < 1) {
      uni.showToast({ title: '录音时间太短', icon: 'none' })
      return
    }
    const voiceFile = new File([blob], `voice_${Date.now()}.mp3`, { type: 'audio/mpeg' })
    isSending.value = true
    try {
      await sendSingleMessage({
        message_type: MESSAGE_TYPE.AUDIO,
        content:      '[语音]',
        file_url:     tempUrl,
        file_name:    voiceFile.name,
        file_size:    fileSize,
        duration:     Math.ceil(duration / 1000),
        originalFile: voiceFile,
      })
    } finally {
      isSending.value = false
    }
  }

  const startVoiceRecording = async () => {
    // #ifdef H5
    try {
      const stream   = await navigator.mediaDevices.getUserMedia({ audio: true })
      audioStream    = stream
      const chunks   = []
      mediaRecorder  = new MediaRecorder(stream)
      mediaRecorder.ondataavailable = (e) => e.data.size > 0 && chunks.push(e.data)
      mediaRecorder.onstop = () => {
        const blob     = new Blob(chunks, { type: 'audio/mpeg' })
        processVoiceData(URL.createObjectURL(blob), recordingDuration.value * 1000, blob.size, blob)
      }
      mediaRecorder.start()
      startRecordingTimer()
      isRecording.value = true
    } catch (e) {
      uni.showToast({ title: '请允许麦克风权限', icon: 'none' })
    }
    // #endif
  }

  const stopAndSendVoice = () => {
    if (!isRecording.value) return
    stopRecordingTimer()
    isRecording.value = false
    // #ifdef H5
    if (mediaRecorder && mediaRecorder.state !== 'inactive') mediaRecorder.stop()
    if (audioStream) audioStream.getTracks().forEach((t) => t.stop())
    // #endif
  }

  const cancelVoice = () => {
    isRecording.value = false
    stopRecordingTimer()
    if (audioStream) audioStream.getTracks().forEach((t) => t.stop())
    if (mediaRecorder && mediaRecorder.state !== 'inactive') mediaRecorder.stop()
  }

  /** 点击录音按钮：未录制则开始，录制中则取消 */
  const toggleVoiceRecording = () => {
    isRecording.value ? cancelVoice() : startVoiceRecording()
  }

  /**
   * 组件卸载时清理：停止录音流和计时器，防止内存泄漏。
   * 调用方需在 onUnmounted 中执行此函数。
   */
  const cleanup = () => {
    if (audioStream) audioStream.getTracks().forEach((t) => t.stop())
    if (recordingTimer) clearInterval(recordingTimer)
  }

  return {
    // 状态
    inputMsg,
    pendingFiles,
    isSending,
    isRecording,
    recordingDuration,
    isInputDragOver,
    showEmojiPanel,
    recentEmojis,
    allEmojis,
    // 发送
    sendMessageWithFiles,
    sendSingleMessage,
    unwrapData,
    calculateFileMD5,
    // 键盘
    handleEnterKey,
    handleCtrlEnter,
    // 表情
    toggleEmojiPanel,
    insertEmoji,
    // 文件
    chooseImage,
    chooseFile,
    removePendingFile,
    // 拖拽
    onInputDragEnter,
    onInputDragOver,
    onInputDragLeave,
    onInputDrop,
    // 语音
    toggleVoiceRecording,
    stopAndSendVoice,
    cancelVoice,
    // 清理
    cleanup,
  }
}
