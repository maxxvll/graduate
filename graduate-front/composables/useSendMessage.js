import { ref } from 'vue'
import service from '@/utils/request'
import ChatStorage from '@/utils/chat-storage'
import { uploadFile, uploadFileWithJSON, uploadChunk, isH5, isNativeApp, sendJsonMessage } from '@/utils/file-upload'
import { debounce, throttle, safeAsync } from '@/utils/performance'

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
  const inputMsg = ref('')
  const pendingFiles = ref([])
  const isSending = ref(false)
  const isInputDragOver = ref(false)
  const showEmojiPanel = ref(false)
  const recentEmojis = ref([])
  const allEmojis = ref([
    '😀',
    '😅',
    '😍',
    '😭',
    '😊',
    '😎',
    '😢',
    '😡',
    '🥳',
    '😱',
    '🤔',
    '👍',
    '👎',
    '🙏',
    '🎉',
    '❤️',
    '😂',
    '🤣',
    '😇',
    '🤩',
    '🥺',
    '😤',
    '🤯',
    '😴',
  ])

  // ─── 语音录制状态 ───────────────────────────────────────────────────────────
  const isRecording = ref(false)
  const recordingDuration = ref(0)
  let mediaRecorder = null
  let audioStream = null
  let recordingTimer = null
  let msgIdCounter = 0

  // ─── Blob URL 管理 ─────────────────────────────────────────────────────────────
  const pendingBlobUrls = new Set()

  const createFileUrl = (file) => {
    // #ifdef H5
    const url = URL.createObjectURL(file)
    pendingBlobUrls.add(url)
    return url
    // #endif
    // #ifndef H5
    // 非 H5 环境不需要管理 Blob URL
    return null
    // #endif
  }

  const revokeFileUrl = (url) => {
    // #ifdef H5
    if (pendingBlobUrls.has(url)) {
      URL.revokeObjectURL(url)
      pendingBlobUrls.delete(url)
    }
    // #endif
  }

  const cleanupFileUrls = () => {
    // #ifdef H5
    pendingBlobUrls.forEach(url => URL.revokeObjectURL(url))
    pendingBlobUrls.clear()
    // #endif
  }

  // ─── 消息重试状态 ────────────────────────────────────────────────────────────
  const MAX_RETRY_TIMES = 3 // 最大重试次数
  const RETRY_DELAY = 2000 // 重试延迟（ms）
  const messageRetryMap = new Map() // 消息重试计数器

  // ─── 工具函数 ────────────────────────────────────────────────────────────────

  const generateMsgId = () => Date.now() + ++msgIdCounter

  /**
   * 解包 axios 响应中的嵌套 data 字段，兼容后端返回 {data: {data: ...}} 的情况。
   * 最多解包 3 层，避免无限循环
   */
  const unwrapData = (res) => {
    let data = res
    let depth = 0
    const maxDepth = 3

    while (data && data.data && depth < maxDepth) {
      data = data.data
      depth++
    }

    return data
  }

  /**
   * 数值验证和边界检查
   * @param {number} value - 要验证的数值
   * @param {number} min - 最小值
   * @param {number} max - 最大值
   * @param {number} defaultValue - 默认值
   * @returns {number} 验证后的数值
   */
  const validateNumber = (value, min = 0, max = Infinity, defaultValue = 0) => {
    if (typeof value !== 'number' || isNaN(value)) {
      console.warn(`[validateNumber] 数值无效: ${value}, 使用默认值: ${defaultValue}`)
      return defaultValue
    }
    return Math.max(min, Math.min(max, value))
  }

  /**
   * 计算文件 MD5，用于大文件秒传和断点续传的唯一标识。
   * H5 使用 SparkMD5 逐片读取文件，使用 setTimeout 分片计算避免阻塞 UI
   * 非 H5 环境返回临时 mock 值。
   */
  const calculateFileMD5 = (file) => {
    return new Promise((resolve, reject) => {
      // #ifdef H5
      if (!file) return reject(new Error('No file provided'))

      const reader = new FileReader()
      const spark = new SparkMD5.ArrayBuffer()
      const chunkSize = 2 * 1024 * 1024 // 2MB 每片
      const chunks = Math.ceil(file.size / chunkSize)
      let currentChunk = 0

      const loadNext = () => {
        const start = currentChunk * chunkSize
        const end = Math.min(start + chunkSize, file.size)

        reader.onload = (e) => {
          spark.append(e.target.result)
          currentChunk++

          if (currentChunk < chunks) {
            // 使用 setTimeout 让出主线程，避免阻塞 UI
            setTimeout(() => loadNext(), 0)
          } else {
            resolve(spark.end())
          }
        }

        reader.onerror = () => reject(new Error('MD5 计算失败'))

        reader.readAsArrayBuffer(file.slice(start, end))
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
   *  5. 任意步骤异常：自动重试（最多3次），将失败消息标记为 FAILED，展示错误提示
   */
  const sendSingleMessage = async (msgData, retryCount = 0) => {
    const sessionId = currentSession.value.sessionId
    const newId = generateMsgId()
    const nowTime = new Date().toISOString()

    // 立即创建占位消息，确保用户看到即时反馈
    const localMsg = cleanMessage({
      id: newId,
      session_id: sessionId,
      message_type: msgData.message_type,
      content: msgData.content,
      local_file_url: msgData.file_url,
      file_url: msgData.file_url,
      file_name: msgData.file_name,
      file_size: msgData.file_size,
      duration: msgData.duration,
      send_time: nowTime,
      send_status: SEND_STATUS.PENDING,
      sender_id: CURRENT_USER_ID.value,
    })

    messages.value.push(localMsg)
    updateSessionLastMsg(sessionId, msgData.content, nowTime)
    scrollToBottom()

    try {
      let finalFileUrl = null
      let finalFileName = msgData.file_name
      let finalFileSize = msgData.file_size
      // 小文件通过 multipart 一次性完成「上传 + 发送」，无需再调 send 接口
      let alreadySent = false
      // 服务端返回的消息对象（含真实 id、messageNo）
      let serverData = null

      if (msgData.originalFile) {
        const file = msgData.originalFile
        if (file.size < 10 * 1024 * 1024) {
          // 小文件：文件与消息元数据同一请求，服务端处理后直接投递
          const sendDTO = {
            messageNo: localMsg.messageNo,
            sessionId,
            sessionType: currentSession.value.sessionType,
            senderId: CURRENT_USER_ID.value,
            receiverId:
              currentSession.value.sessionType === SESSION_TYPE.SINGLE
                ? currentSession.value.targetId
                : null,
            messageType: msgData.message_type,
            content: msgData.content,
            duration: msgData.duration,
            fileUrl: null,
            fileName: file.name,
            fileSize: file.size,
          }
          
          // 使用新的跨平台上传工具
          // H5 使用 FormData，App 使用 uni.uploadFile
          try {
            const uploadRes = await uploadFileWithJSON(
              '/chat/message/send',
              file,
              sendDTO,
              'sendDTO'
            )

            if (uploadRes && uploadRes.code === 200) {
              const data = uploadRes.data || uploadRes
              serverData = data
              finalFileUrl = data?.fileUrl || data?.file_url || null
              finalFileName = file.name
              finalFileSize = file.size
              alreadySent = true
              console.log('[sendMessage] 小文件上传成功:', finalFileUrl)
            } else {
              throw new Error(uploadRes?.message || '文件上传失败')
            }
          } catch (uploadErr) {
            console.error('[sendMessage] 文件上传失败:', uploadErr)
            throw uploadErr
          }
        } else {
          // 大文件处理
          // H5：支持分片上传；App：降级为普通上传
          
          if (isNativeApp()) {
            // App 环境：不支持 MD5 计算和分片，降级为普通 uni.uploadFile 上传
            console.warn('[sendMessage] App 环境，降级为普通大文件上传')
            
            try {
              const uploadRes = await uploadFileWithJSON(
                '/chat/message/send',
                file,
                sendDTO,
                'sendDTO'
              )

              if (uploadRes && uploadRes.code === 200) {
                const data = uploadRes.data || uploadRes
                finalFileUrl = data?.fileUrl || data?.file_url || null
              } else {
                throw new Error(uploadRes?.message || '文件上传失败')
              }
            } catch (err) {
              console.error('[sendMessage] 大文件上传失败:', err)
              throw err
            }
          }
          // #ifdef H5
          else {
            // H5 环境：使用分片上传（秒传检测 → 上传缺失分片 → 服务端合并 → 返回最终 URL）
            const fileMD5 = await calculateFileMD5(file)
            const totalChunks = Math.ceil(file.size / CHUNK_SIZE)

            const checkRes = await service.post(
              '/chat/file/check',
              {},
              { params: { md5: fileMD5, fileName: file.name } },
            )
            const checkData = unwrapData(checkRes)
            if (!checkData) throw new Error('文件检查接口返回数据异常')

            if (!checkData.shouldUpload) {
              // 服务端已有同 MD5 文件，直接复用（秒传）
              finalFileUrl = checkData.fileUrl
            } else {
              const uploadedChunks = checkData.uploadedChunks || []
              
              // 关键：在 H5 中使用 uploadChunk 工具上传分片
              // 该工具自动使用 FormData，避免 App 环境错误
              for (let i = 0; i < totalChunks; i++) {
                if (uploadedChunks.includes(i)) continue
                const start = i * CHUNK_SIZE
                
                try {
                  const chunkBlob = file.slice(start, Math.min(start + CHUNK_SIZE, file.size))
                  await uploadChunk('/chat/file/upload-chunk', chunkBlob, {
                    md5: fileMD5,
                    chunkIndex: i,
                    fileName: file.name,
                    totalChunks: totalChunks,
                    fileSize: file.size
                  })
                } catch (chunkErr) {
                  console.error(`分片 ${i} 上传失败:`, chunkErr)
                  throw new Error(`分片 ${i} 上传失败: ${chunkErr.message}`)
                }
              }

              // 通知服务端合并所有分片，返回最终可访问的文件 URL
              const mergeRes = await service.post(
                '/chat/file/merge',
                {},
                {
                  params: {
                    md5: fileMD5,
                    fileName: file.name,
                    totalChunks,
                    isPublic:
                      currentSession.value.sessionType === SESSION_TYPE.GROUP,
                  },
                },
              )
              const mergedData = unwrapData(mergeRes)
              finalFileUrl =
                typeof mergedData === 'object'
                  ? mergedData?.url || mergedData?.fileUrl || null
                  : mergedData
            }
          }
          // #endif
          finalFileName = file.name
          finalFileSize = file.size
        }
      }

      // 纯文本消息或大文件上传完毕后，单独调 send 接口完成消息投递
      if (!alreadySent) {
        const sendDTO = {
          messageNo: localMsg.messageNo,
          sessionId,
          sessionType: currentSession.value.sessionType,
          senderId: CURRENT_USER_ID.value,
          receiverId:
            currentSession.value.sessionType === SESSION_TYPE.SINGLE
              ? currentSession.value.targetId
              : null,
          messageType: msgData.message_type,
          content: msgData.content,
          duration: msgData.duration,
          fileUrl: finalFileUrl,
          fileName: finalFileName,
          fileSize: finalFileSize,
        }
        
        // 使用跨平台消息发送方法
        // H5: FormData + Fetch, App: uni.uploadFile
        const res = await sendJsonMessage('/chat/message/send', sendDTO, 'sendDTO')
        const data = unwrapData(res)
        serverData = data
        
        // 以服务端返回的 URL 为准（如有转码处理）
        if (data?.fileUrl) finalFileUrl = data.fileUrl
      }

      // 将发送成功的最终消息写入本地缓存，并同步更新 UI 状态
      // 关键：用服务端返回的真实 id 替换临时前端 id，确保撤回等后续操作能找到正确的 DB 记录
      const serverMsg = {
        ...localMsg,
        id: serverData?.id ?? localMsg.id,
        messageNo: serverData?.messageNo ?? localMsg.messageNo,
        file_url: finalFileUrl,
        file_name: finalFileName,
        file_size: finalFileSize,
        send_status: SEND_STATUS.SUCCESS,
      }
      serverMsg.local_file_url = localMsg.local_file_url
      // 通过 messageNo 定位占位消息（客户端 messageNo 与服务端已对齐）
      const idx = messages.value.findIndex(
        (m) => m.messageNo === localMsg.messageNo,
      )
      if (idx !== -1) {
        messages.value[idx] = cleanMessage(serverMsg)
        if (ChatStorage.insertMessage)
          await ChatStorage.insertMessage(messages.value[idx])
      }
    } catch (err) {
      console.error('消息发送异常:', err)

      // 检查是否需要重试
      const shouldRetry =
        retryCount < MAX_RETRY_TIMES &&
        !err.message?.includes('已取消') &&
        !err.message?.includes('用户取消')

      if (shouldRetry) {
        // 更新重试计数
        messageRetryMap.set(newId, retryCount + 1)

        console.log(`[SendMessage] 消息发送失败，正在进行第 ${retryCount + 1} 次重试...`)

        // 更新消息状态为重试中
        const failIdx = messages.value.findIndex((m) => m.id === newId)
        if (failIdx !== -1) {
          messages.value[failIdx].send_status = SEND_STATUS.PENDING
          messages.value[failIdx].content = `[重试中 ${retryCount + 1}/${MAX_RETRY_TIMES}] ${msgData.content}`
          messages.value = [...messages.value]
        }

        // 延迟后重试
        await new Promise((resolve) => setTimeout(resolve, RETRY_DELAY))
        return sendSingleMessage(msgData, retryCount + 1)
      }

      // 重试次数用尽或不可重试的错误，标记为失败
      messageRetryMap.delete(newId)

      const failIdx = messages.value.findIndex((m) => m.id === newId)
      if (failIdx !== -1) {
        messages.value[failIdx].send_status = SEND_STATUS.FAILED
        messages.value[failIdx].content = msgData.content // 恢复原始内容
        messages.value = [...messages.value]
      }

      // 显示错误提示
      const errorMsg = err.message || '发送失败'
      if (retryCount >= MAX_RETRY_TIMES) {
        errorMsg = `发送失败，已重试 ${MAX_RETRY_TIMES} 次`
      }
      uni.showToast({ title: errorMsg, icon: 'none' })
    }
  }

  /**
   * 发送按钮触发：先发文本再按序发每个附件。
   * 文本和文件分开发送，确保消息顺序正确。
   */
  const sendMessageWithFiles = async () => {
    if (isSending.value || !currentSession.value) return
    const hasText = inputMsg.value.trim()
    const hasFiles = pendingFiles.value.length > 0
    if (!hasText && !hasFiles) return
    isSending.value = true
    try {
      if (hasText) {
        await sendSingleMessage({
          message_type: MESSAGE_TYPE.TEXT,
          content: inputMsg.value,
          originalFile: null,
        })
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

  // 防抖的输入处理（用于实时字数统计、自动保存等）
  const handleInputDebounced = debounce((value) => {
    // 这里可以添加实时保存草稿等逻辑
    console.log('[Input] Debounced input:', value.length, 'chars')
  }, 500)

  // 监听输入变化
  const onInputChange = (value) => {
    inputMsg.value = value
    handleInputDebounced(value)
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
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = 'image/*'
    input.multiple = 'multiple'
    input.onchange = (e) => {
      const files = Array.from(e.target.files || [])
      if (!files.length) return
      files.slice(0, 9).forEach((file) => {
        const fileUrl = createFileUrl(file)
        pendingFiles.value.push({
          message_type: MESSAGE_TYPE.IMAGE,
          content: '[图片]',
          file_url: fileUrl,
          file_name: file.name,
          file_size: file.size,
          originalFile: file,
        })
      })
    }
    input.click()
    // #endif
    // #ifndef H5
    uni.chooseImage({
      count: 9, // 小程序最大9张，App也统一限制
      sizeType: ['compressed'], // 压缩图片，节省流量
      sourceType: ['album', 'camera'], // 相册和相机
      success: (res) => {
        const tempFiles = res.tempFiles || []
        tempFiles.forEach((tempFile) => {
          if (tempFile) {
            pendingFiles.value.push({
              message_type: MESSAGE_TYPE.IMAGE,
              content: '[图片]',
              file_url: tempFile.path,
              file_name: tempFile.name || 'image.jpg',
              file_size: tempFile.size,
              originalFile: tempFile,
            })
          }
        })
      },
      fail: () => uni.showToast({ title: '选择图片失败', icon: 'none' }),
    })
    // #endif
  }

  const chooseFile = () => {
    // #ifdef H5
    const input = document.createElement('input')
    input.type = 'file'
    input.multiple = 'multiple'
    input.onchange = (e) => {
      const files = Array.from(e.target.files || [])
      if (!files.length) return
      files.forEach((file) => {
        const fileUrl = createFileUrl(file)
        pendingFiles.value.push({
          message_type: MESSAGE_TYPE.FILE,
          content: '[文件]',
          file_url: fileUrl,
          file_name: file.name,
          file_size: file.size,
          originalFile: file,
        })
      })
    }
    input.click()
    // #endif
    // #ifndef H5
    uni.chooseMessageFile({
      count: 9, // 允许选择多个文件
      type: 'file',
      success: (res) => {
        const tempFiles = res.tempFiles || []
        tempFiles.forEach((tempFile) => {
          if (tempFile) {
            pendingFiles.value.push({
              message_type: MESSAGE_TYPE.FILE,
              content: '[文件]',
              file_url: tempFile.path,
              file_name: tempFile.name,
              file_size: tempFile.size,
              originalFile: tempFile,
            })
          }
        })
      },
      fail: () => uni.showToast({ title: '选择文件失败', icon: 'none' }),
    })
    // #endif
  }

  const removePendingFile = (index) => {
    const removedFile = pendingFiles.value[index]
    if (removedFile?.file_url) {
      revokeFileUrl(removedFile.file_url)
    }
    pendingFiles.value.splice(index, 1)
  }

  // ─── 拖拽上传 ────────────────────────────────────────────────────────────────

  const onInputDragEnter = (e) => {
    e.preventDefault()
    isInputDragOver.value = true
  }
  const onInputDragOver = (e) => {
    e.preventDefault()
    isInputDragOver.value = true
  }
  const onInputDragLeave = (e) => {
    e.preventDefault()
    isInputDragOver.value = false
  }

  /** 拖拽文件到输入区自动识别类型并加入待发队列 */
  const onInputDrop = (e) => {
    e.preventDefault()
    isInputDragOver.value = false
    // #ifdef H5
    const files = e.dataTransfer?.files
    if (!files) return
    Array.from(files).forEach((file) => {
      let msgType = MESSAGE_TYPE.FILE
      if (file.type.startsWith('image/')) msgType = MESSAGE_TYPE.IMAGE
      else if (file.type.startsWith('video/')) msgType = MESSAGE_TYPE.VIDEO
      const label =
        msgType === MESSAGE_TYPE.IMAGE
          ? '图片'
          : msgType === MESSAGE_TYPE.VIDEO
            ? '视频'
            : '文件'
      const fileUrl = createFileUrl(file)
      pendingFiles.value.push({
        message_type: msgType,
        content: `[${label}]`,
        file_url: fileUrl,
        file_name: file.name,
        file_size: file.size,
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
    // 验证录音时长（至少 1 秒）
    const validDuration = validateNumber(recordingDuration.value, 0, 300, 0)
    if (validDuration < 1) {
      uni.showToast({ title: '录音时间太短', icon: 'none' })
      return
    }

    // 验证文件大小
    const validFileSize = validateNumber(fileSize, 0, 100 * 1024 * 1024, 0)
    if (validFileSize === 0) {
      uni.showToast({ title: '录音文件无效', icon: 'none' })
      return
    }

    const voiceFile = new File([blob], `voice_${Date.now()}.mp3`, {
      type: 'audio/mpeg',
    })

    isSending.value = true
    try {
      await sendSingleMessage({
        message_type: MESSAGE_TYPE.AUDIO,
        content: '[语音]',
        file_url: tempUrl,
        file_name: voiceFile.name,
        file_size: validFileSize,
        duration: Math.ceil(validDuration), // 使用验证后的时长
        originalFile: voiceFile,
      })
    } finally {
      isSending.value = false
    }
  }

  const startVoiceRecording = async () => {
    // #ifdef H5
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      audioStream = stream
      const chunks = []
      mediaRecorder = new MediaRecorder(stream)
      mediaRecorder.ondataavailable = (e) =>
        e.data.size > 0 && chunks.push(e.data)
      mediaRecorder.onstop = () => {
        const blob = new Blob(chunks, { type: 'audio/mpeg' })
        const blobUrl = createFileUrl(blob)
        processVoiceData(
          blobUrl || URL.createObjectURL(blob),
          recordingDuration.value * 1000,
          blob.size,
          blob,
        )
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
    if (mediaRecorder && mediaRecorder.state !== 'inactive')
      mediaRecorder.stop()
    if (audioStream) audioStream.getTracks().forEach((t) => t.stop())
    // #endif
  }

  const cancelVoice = () => {
    isRecording.value = false
    stopRecordingTimer()

    // 停止媒体录制器
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      try {
        mediaRecorder.stop()
      } catch (e) {
        console.warn('[Voice] 停止录制器失败:', e)
      }
    }

    // 停止音频流的所有轨道
    if (audioStream) {
      try {
        audioStream.getTracks().forEach((track) => {
          track.stop()
          // 移除轨道引用
          audioStream.removeTrack(track)
        })
      } catch (e) {
        console.warn('[Voice] 停止音频轨道失败:', e)
      }
    }

    // 清空引用
    mediaRecorder = null
    audioStream = null
  }

  /** 点击录音按钮：未录制则开始，录制中则取消 */
  const toggleVoiceRecording = () => {
    isRecording.value ? cancelVoice() : startVoiceRecording()
  }

  /**
   * 组件卸载时清理：停止录音流、计时器和清理引用，防止内存泄漏。
   * 调用方需在 onUnmounted 中执行此函数。
   */
  const cleanup = () => {
    // 停止录音计时器
    if (recordingTimer) {
      clearInterval(recordingTimer)
      recordingTimer = null
    }

    // 停止媒体录制器
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      try {
        mediaRecorder.stop()
        mediaRecorder = null
      } catch (e) {
        console.warn('[Cleanup] 停止录制器失败:', e)
      }
    }

    // 停止并清理音频流
    if (audioStream) {
      try {
        audioStream.getTracks().forEach((track) => {
          track.stop()
          audioStream.removeTrack(track)
        })
        audioStream = null
      } catch (e) {
        console.warn('[Cleanup] 清理音频流失败:', e)
      }
    }

    // 清理消息重试计数器
    messageRetryMap.clear()

    // 清理所有 Blob URLs
    cleanupFileUrls()

    console.log('[Cleanup] 所有资源已清理')
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
    onInputChange,
    // 表情
    toggleEmojiPanel,
    insertEmoji,
    // 文件
    chooseImage,
    chooseFile,
    removePendingFile,
    // Blob URL 管理
    createFileUrl,
    revokeFileUrl,
    cleanupFileUrls,
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
    // 性能优化
    validateNumber,
  }
}
