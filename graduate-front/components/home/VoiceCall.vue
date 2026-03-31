<template>
  <view v-if="visible" class="voice-layer">
    <view class="voice-backdrop" @click="handleBackdropClick"></view>
    <view class="voice-card" :class="{ mobile }">
      <view class="voice-badge">{{ badgeText }}</view>

      <view v-if="usesStreamTransport && isVideoMode" class="app-media-stage stream-media-stage">
        <view class="stream-remote-stage">
          <video
            v-if="appRemotePlayUrl"
            :key="`${appRemotePlayUrl}_${appPlayerKey}`"
            class="app-remote-player"
            :src="appRemotePlayUrl"
            :autoplay="true"
            :controls="false"
            :is-live="true"
            :muted="false"
            :show-center-play-btn="false"
            :show-play-btn="false"
            :show-fullscreen-btn="false"
            :enable-progress-gesture="false"
            object-fit="cover"
            @play="handleAppRemotePlay"
            @error="handleAppRemoteError"
          />
          <view v-else class="app-remote-placeholder">
            {{ phase === 'incoming' ? '等待你接听后建立画面' : '等待对方画面接入...' }}
          </view>
        </view>

        <view class="stream-local-stage">
          <text class="stream-stage-label">我的画面</text>
          <live-pusher
            v-if="appLocalPushUrl"
            :id="appPusherId"
            class="stream-local-pusher"
            :url="appLocalPushUrl"
            mode="SD"
            :autopush="false"
            :muted="false"
            :enable-camera="true"
            :enable-mic="!isMuted"
            aspect="9:16"
            @statechange="handleAppPusherState"
            @error="handleAppPusherError"
          />
        </view>
      </view>

      <view v-else-if="showsWebRtcVideoStage" class="app-media-stage web-video-stage">
        <video
          ref="remoteVideoElement"
          class="app-remote-player"
          autoplay
          playsinline
          object-fit="cover"
        />
        <view v-if="!webRtcRemoteReady" class="app-remote-placeholder">
          {{ phase === 'incoming' ? '等待你接听后建立画面' : '等待对方画面接入...' }}
        </view>

        <video
          ref="localVideoElement"
          class="app-local-pusher web-local-preview"
          autoplay
          playsinline
          muted
          object-fit="cover"
        />
      </view>

      <view v-else-if="usesStreamTransport" class="app-audio-stage">
        <live-pusher
          v-if="appLocalPushUrl"
          :id="appPusherId"
          class="app-hidden-media"
          :url="appLocalPushUrl"
          mode="SD"
          :autopush="false"
          :muted="false"
          :enable-camera="false"
          :enable-mic="!isMuted"
          aspect="9:16"
          @statechange="handleAppPusherState"
          @error="handleAppPusherError"
        />
        <video
          v-if="appRemotePlayUrl"
          :key="`${appRemotePlayUrl}_${appPlayerKey}`"
          class="app-hidden-media"
          :src="appRemotePlayUrl"
          :autoplay="true"
          :controls="false"
          :is-live="true"
          :muted="false"
          :show-center-play-btn="false"
          :show-play-btn="false"
          :show-fullscreen-btn="false"
          :enable-progress-gesture="false"
          @play="handleAppRemotePlay"
          @error="handleAppRemoteError"
        />
      </view>

      <view class="voice-hero" :class="{ compact: usesStreamTransport && isVideoMode }">
        <image class="voice-avatar" :src="peerAvatar" mode="aspectFill" />
        <view class="voice-copy">
          <text class="voice-name">{{ peerName }}</text>
          <text class="voice-sub">{{ subtitleText }}</text>
        </view>

        <view class="voice-rings">
          <view class="ring ring-one"></view>
          <view class="ring ring-two"></view>
          <view class="ring ring-three"></view>
        </view>
      </view>

      <view class="voice-panel">
        <view class="meta-item">
          <text class="meta-label">模式</text>
          <text class="meta-value">{{ modeLabel }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-label">链路</text>
          <text class="meta-value">{{ transportLabel }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-label">状态</text>
          <text class="meta-value">{{ statusText }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-label">时长</text>
          <text class="meta-value">{{ durationText }}</text>
        </view>
      </view>

      <view v-if="unsupportedReason" class="voice-tip danger">{{ unsupportedReason }}</view>
      <view v-else-if="phase === 'incoming'" class="voice-tip">
        对方正在发起{{ isVideoMode ? '视频' : '语音' }}通话，你可以接听或拒绝。
      </view>
      <view v-else-if="phase === 'calling'" class="voice-tip">
        呼叫请求已发出，等待对方应答。
      </view>
      <view v-else-if="phase === 'connecting'" class="voice-tip">
        正在建立{{ usesStreamTransport ? '推流/拉流' : '音视频' }}连接，请稍等。
      </view>
      <view v-else-if="phase === 'connected'" class="voice-tip success">
        通话已建立，保持网络稳定会更顺畅。
      </view>
      <view v-else-if="phase === 'ended'" class="voice-tip">
        {{ endReason || '通话已结束' }}
      </view>

      <view class="voice-actions">
        <template v-if="phase === 'incoming'">
          <button class="call-btn reject" @click="rejectCall">拒绝</button>
          <button class="call-btn answer" :disabled="busy || Boolean(unsupportedReason)" @click="answerCall">
            接听
          </button>
        </template>

        <template v-else-if="phase === 'calling' || phase === 'connecting' || phase === 'connected'">
          <button class="call-btn subtle" :disabled="busy || !hasLocalAudio" @click="toggleMute">
            {{ isMuted ? '取消静音' : '静音' }}
          </button>
          <button
            v-if="usesStreamTransport && isVideoMode"
            class="call-btn subtle"
            :disabled="busy || !appPublishing"
            @click="switchCamera"
          >
            切换镜头
          </button>
          <button class="call-btn subtle" :disabled="busy" @click="toggleSpeaker">
            {{ speakerText }}
          </button>
          <button class="call-btn reject" :disabled="busy" @click="hangupCall">
            挂断
          </button>
        </template>

        <button v-else class="call-btn subtle full" @click="closePanel">关闭</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import {
  APP_PERMISSION_SCOPE,
  ensureAppPermissionAccess,
  formatAppPermissionLabels,
} from '@/utils/app-permission'
import service from '@/utils/request'
import {
  buildCallStreamKey,
  buildRtcPlayUrl,
  buildRtcPushUrl,
  hasConfiguredNativeWebRtcPlugin,
  hasRtcStreamConfig,
  trimTrailingSlash,
} from '@/utils/config'
import {
  canUseUniversalWebRtcRuntime,
  createRtcIceCandidate,
  createRtcSessionDescription,
  getWebRtcMediaDevices,
  getWebRtcPeerConnectionConstructor,
  hasNativeWebRtcPlugin,
} from '@/utils/native-webrtc'
import { isAppPlusRuntime, supportsBrowserDom, waitForPlusReady } from '@/utils/runtime'
import { DEFAULT_AVATAR_LARGE as defaultAvatar } from '@/utils/common'

const CALL_TYPE = {
  CALL: '1',
  ANSWER: '2',
  REJECT: '3',
  HANGUP: '4',
  SDP: '5',
  ICE: '6',
}

const props = defineProps({
  visible: { type: Boolean, default: false },
  incoming: { type: Boolean, default: false },
  peer: { type: Object, default: () => ({}) },
  sessionId: { type: String, default: '' },
  mode: { type: String, default: 'audio' },
  transport: { type: String, default: 'webrtc' },
  signal: { type: Object, default: null },
  currentUserId: { type: String, default: '' },
  mobile: { type: Boolean, default: false },
})

const emit = defineEmits(['close', 'ended', 'state-change'])

const instance = getCurrentInstance()
const appPusherId = `voice-call-pusher-${Math.random().toString(36).slice(2, 8)}`

const phase = ref('idle')
const busy = ref(false)
const isMuted = ref(false)
const isSpeakerOn = ref(true)
const elapsedSeconds = ref(0)
const endReason = ref('')
const unsupportedReason = ref('')
const appLocalPushUrl = ref('')
const appRemotePlayUrl = ref('')
const appPlayerKey = ref(0)
const appPublishing = ref(false)
const appPlayerReady = ref(false)
const serverPushBaseUrl = ref('')
const serverPlayBaseUrl = ref('')
const webrtcIceServers = ref([{ urls: 'stun:stun.l.google.com:19302' }])
const supportedTransports = ref(['webrtc', 'stream'])
const localVideoElement = ref(null)
const remoteVideoElement = ref(null)
const webRtcRemoteReady = ref(false)

let localStream = null
let peerConnection = null
let remoteAudio = null
let startTimer = null
let pendingOffer = null
let pendingCandidates = []
let bootstrapped = false
let appPusherContext = null
let appRemoteRetryTimer = null
let voiceConfigPromise = null

const peerName = computed(
  () => props.peer?.nickname || props.peer?.username || props.peer?.id || '未命名联系人',
)
const peerAvatar = computed(() => props.peer?.avatar || defaultAvatar)
const isVideoMode = computed(() => props.mode === 'video')
const usesStreamTransport = computed(() => props.transport === 'stream')
const localUserId = computed(() => {
  if (props.currentUserId) {
    return String(props.currentUserId)
  }
  try {
    return String(uni.getStorageSync('userInfo')?.id || '')
  } catch {
    return ''
  }
})
const modeLabel = computed(() => (isVideoMode.value ? '视频通话' : '语音通话'))
const transportLabel = computed(() => {
  if (usesStreamTransport.value) {
    return 'App 推拉流'
  }
  if (isAppPlusRuntime()) {
    return '原生 WebRTC'
  }
  return 'WebRTC'
})
const hasLocalAudio = computed(() =>
  usesStreamTransport.value ? appPublishing.value : Boolean(localStream?.getAudioTracks?.().length),
)
const supportsRequestedTransport = computed(() => {
  if (!supportedTransports.value.length) {
    return true
  }
  return supportedTransports.value.includes(props.transport || 'webrtc')
})
const showsWebRtcVideoStage = computed(
  () => !usesStreamTransport.value && isVideoMode.value && supportsBrowserDom(),
)
const badgeText = computed(() => {
  if (phase.value === 'incoming') return '来电'
  if (phase.value === 'calling') return '呼叫中'
  if (phase.value === 'connecting') return '连接中'
  if (phase.value === 'connected') return '通话中'
  return '已结束'
})
const subtitleText = computed(() => {
  if (phase.value === 'incoming') return `邀请你开始${isVideoMode.value ? '视频' : '语音'}通话`
  if (phase.value === 'calling') return '等待对方接听'
  if (phase.value === 'connecting') return isVideoMode.value ? '正在同步视频链路' : '正在同步音频链路'
  if (phase.value === 'connected') return isVideoMode.value ? '视频连接已建立' : '语音连接已建立'
  return endReason.value || '这次通话已经结束'
})
const statusText = computed(() => {
  if (unsupportedReason.value) return '当前环境不支持'
  if (phase.value === 'incoming') return '等待你确认'
  if (phase.value === 'calling') return '已发起邀请'
  if (phase.value === 'connecting') return '正在建立连接'
  if (phase.value === 'connected') return '连接正常'
  return '已结束'
})
const durationText = computed(() => {
  const total = Number(elapsedSeconds.value || 0)
  const minutes = String(Math.floor(total / 60)).padStart(2, '0')
  const seconds = String(total % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
})
const speakerText = computed(() => (isSpeakerOn.value ? '扬声器' : '听筒'))

const canUseWebRtc = () => Boolean(getWebRtcPeerConnectionConstructor() && canUseUniversalWebRtcRuntime())

const resolveMediaElement = (target) => target?.$el || target || null

const clearMediaElementStream = (target) => {
  const element = resolveMediaElement(target)
  if (!element) {
    return
  }

  if ('srcObject' in element) {
    element.srcObject = null
  }
  if ('src' in element) {
    element.src = ''
  }
  element.load?.()
}

const attachMediaStreamToElement = async (targetRef, stream, { muted = false } = {}) => {
  if (!showsWebRtcVideoStage.value || !stream) {
    return false
  }

  await nextTick()
  const element = resolveMediaElement(targetRef?.value)
  if (!element) {
    return false
  }

  element.autoplay = true
  element.playsInline = true
  element.muted = muted

  if ('srcObject' in element) {
    element.srcObject = stream
  } else if (typeof URL !== 'undefined' && typeof URL.createObjectURL === 'function') {
    element.src = URL.createObjectURL(stream)
  } else {
    return false
  }

  await element.play?.().catch(() => {})
  return true
}

const syncLocalVideoPreview = async () => {
  if (!localStream) {
    return
  }
  await attachMediaStreamToElement(localVideoElement, localStream, { muted: true })
}

const syncRemoteVideoPlayback = async (stream) => {
  const attached = await attachMediaStreamToElement(remoteVideoElement, stream)
  if (attached) {
    webRtcRemoteReady.value = true
  }
  return attached
}

const ensureCallPermissions = async () => {
  if (!isAppPlusRuntime()) {
    return
  }

  const scopes = [APP_PERMISSION_SCOPE.MICROPHONE]
  if (isVideoMode.value) {
    scopes.push(APP_PERMISSION_SCOPE.CAMERA)
  }

  const result = await ensureAppPermissionAccess(scopes, {
    title: isVideoMode.value ? '需要相机和麦克风权限' : '需要麦克风权限',
    content: isVideoMode.value
      ? '开始视频通话前，请先在系统设置中开启相机和麦克风权限。'
      : '开始语音通话前，请先在系统设置中开启麦克风权限。',
  })

  if (!result.ok) {
    throw new Error(`请先开启${formatAppPermissionLabels(result.blockedScopes)}权限`)
  }
}

const buildSignalPayload = (extra = {}) => ({
  targetId: String(props.peer?.id || ''),
  sessionId: props.sessionId || '',
  mode: props.mode || 'audio',
  transport: props.transport || 'webrtc',
  ...extra,
})

const parseRtcPayload = (value) => {
  if (!value) return null
  if (typeof value === 'object') return value
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

const normalizeIceServers = (servers = []) =>
  (Array.isArray(servers) ? servers : [])
    .map((item) => ({
      urls: Array.isArray(item?.urls) ? item.urls.filter(Boolean) : [item?.urls].filter(Boolean),
      username: item?.username || '',
      credential: item?.credential || '',
    }))
    .filter((item) => item.urls.length)

const loadVoiceCallConfig = async () => {
  if (voiceConfigPromise) {
    return voiceConfigPromise
  }

  voiceConfigPromise = service
    .get('/voice-call/config')
    .then((response) => {
      const nextSupportedTransports = Array.isArray(response?.data?.supportedTransports)
        ? response.data.supportedTransports
            .map((item) => String(item || '').trim().toLowerCase())
            .filter(Boolean)
        : []
      if (nextSupportedTransports.length) {
        supportedTransports.value = nextSupportedTransports
      }

      const nextIceServers = normalizeIceServers(response?.data?.iceServers || [])
      if (nextIceServers.length) {
        webrtcIceServers.value = nextIceServers
      }

      const serverPushUrl = String(response?.data?.pushBaseUrl || '').trim()
      if (serverPushUrl) {
        serverPushBaseUrl.value = serverPushUrl
      }

      const serverPlayUrl = String(response?.data?.playBaseUrl || '').trim()
      if (serverPlayUrl) {
        serverPlayBaseUrl.value = serverPlayUrl
      }
    })
    .catch((error) => {
      console.warn('[voice-call] load config failed', error)
    })

  return voiceConfigPromise
}

const startDuration = () => {
  if (startTimer) return
  startTimer = setInterval(() => {
    elapsedSeconds.value += 1
  }, 1000)
}

const stopDuration = () => {
  if (!startTimer) return
  clearInterval(startTimer)
  startTimer = null
}

const clearAppRemoteRetry = () => {
  if (!appRemoteRetryTimer) return
  clearTimeout(appRemoteRetryTimer)
  appRemoteRetryTimer = null
}

const scheduleAppRemoteRetry = () => {
  if (!usesStreamTransport.value || !appRemotePlayUrl.value || appPlayerReady.value || phase.value === 'ended') {
    return
  }
  if (appRemoteRetryTimer) {
    return
  }
  appRemoteRetryTimer = setTimeout(() => {
    appRemoteRetryTimer = null
    if (!appRemotePlayUrl.value || appPlayerReady.value || phase.value === 'ended') {
      return
    }
    appPlayerKey.value += 1
    scheduleAppRemoteRetry()
  }, 1800)
}

const setAppRemotePlayUrl = (url) => {
  clearAppRemoteRetry()
  appRemotePlayUrl.value = url || ''
  appPlayerReady.value = false
  if (url) {
    appPlayerKey.value += 1
    scheduleAppRemoteRetry()
  }
}

const ensureAppCallContext = () => {
  if (!hasRtcStreamConfig()) {
    throw new Error('当前 App 端未配置推流/拉流地址')
  }
  if (!props.sessionId || !props.peer?.id || !localUserId.value) {
    throw new Error('通话上下文不完整')
  }
}

const buildAppLocalPushUrl = () => {
  // 优先使用服务器返回的推流地址
  if (serverPushBaseUrl.value) {
    const key = buildCallStreamKey({
      sessionId: props.sessionId,
      userId: localUserId.value,
      mode: props.mode,
    })
    return `${trimTrailingSlash(serverPushBaseUrl.value)}/${key}`
  }
  // 回退到本地配置
  return buildRtcPushUrl({
    sessionId: props.sessionId,
    userId: localUserId.value,
    mode: props.mode,
  })
}

const buildAppRemotePlayUrl = () => {
  // 优先使用服务器返回的拉流地址
  if (serverPlayBaseUrl.value) {
    const key = buildCallStreamKey({
      sessionId: props.sessionId,
      userId: String(props.peer?.id || ''),
      mode: props.mode,
    })
    return `${trimTrailingSlash(serverPlayBaseUrl.value)}/${key}`
  }
  // 回退到本地配置
  return buildRtcPlayUrl({
    sessionId: props.sessionId,
    userId: String(props.peer?.id || ''),
    mode: props.mode,
  })
}

const prepareAppMediaSession = ({ withRemote = false } = {}) => {
  ensureAppCallContext()
  appLocalPushUrl.value = buildAppLocalPushUrl()
  if (withRemote) {
    setAppRemotePlayUrl(buildAppRemotePlayUrl())
  }
}

const ensureAppPusherContext = async () => {
  await waitForPlusReady()
  await nextTick()
  if (!appPusherContext && typeof uni.createLivePusherContext === 'function') {
    appPusherContext = uni.createLivePusherContext(appPusherId, instance?.proxy)
  }
  if (!appPusherContext) {
    throw new Error('当前环境未提供直播推流能力')
  }
  return appPusherContext
}

const invokeAppPusher = async (method) => {
  const context = await ensureAppPusherContext()
  if (typeof context[method] !== 'function') {
    return
  }
  return new Promise((resolve, reject) => {
    try {
      context[method]({
        success: () => resolve(),
        fail: (error) => reject(new Error(error?.errMsg || `${method} failed`)),
      })
    } catch (error) {
      reject(error)
    }
  })
}

const startAppPublishing = async () => {
  if (appPublishing.value) {
    return
  }

  await ensureCallPermissions()
  prepareAppMediaSession()
  try {
    await invokeAppPusher('startPreview')
  } catch (error) {
    console.warn('[voice-call] start preview failed', error)
  }
  await invokeAppPusher('start')
  appPublishing.value = true
}

const stopAppPublishing = async () => {
  if (!appPusherContext) {
    appPublishing.value = false
    return
  }

  try {
    await invokeAppPusher('stop')
  } catch (error) {
    console.warn('[voice-call] stop pusher failed', error)
  }

  try {
    await invokeAppPusher('stopPreview')
  } catch {}

  appPublishing.value = false
}

const closeAppResources = async () => {
  clearAppRemoteRetry()
  await stopAppPublishing()
  appPusherContext = null
  appLocalPushUrl.value = ''
  setAppRemotePlayUrl('')
  appPlayerReady.value = false
}

const handleAppPusherState = (event) => {
  const code = Number(event?.detail?.code || event?.code || 0)
  if (code < 0) {
    console.error('[voice-call] pusher state error', event)
    finalizeCall('本地推流失败')
  }
}

const handleAppPusherError = (event) => {
  console.error('[voice-call] pusher error', event)
  finalizeCall('本地推流失败')
}

const handleAppRemotePlay = () => {
  appPlayerReady.value = true
  clearAppRemoteRetry()
  if (phase.value !== 'connected') {
    phase.value = 'connected'
    startDuration()
    emit('state-change', 'connected')
  }
}

const handleAppRemoteError = (event) => {
  console.warn('[voice-call] remote play error', event)
  scheduleAppRemoteRetry()
}

const ensurePeerConnection = async () => {
  if (peerConnection) return peerConnection

  const PeerConnection = getWebRtcPeerConnectionConstructor()
  if (typeof PeerConnection !== 'function') {
    throw new Error('当前环境未提供 RTCPeerConnection')
  }

  peerConnection = new PeerConnection({
    iceServers: webrtcIceServers.value,
  })

  peerConnection.onicecandidate = async (event) => {
    if (!event.candidate || !props.peer?.id) return
    try {
      await service.post(
        '/voice-call/ice',
        buildSignalPayload({
          candidate: JSON.stringify(event.candidate.toJSON?.() || event.candidate),
        }),
      )
    } catch (error) {
      console.warn('[voice-call] send ICE failed', error)
    }
  }

  peerConnection.ontrack = (event) => {
    const stream = event.streams?.[0]
    if (!stream) return
    if (isVideoMode.value) {
      void syncRemoteVideoPlayback(stream)
      return
    }
    if (!remoteAudio && typeof Audio !== 'undefined') {
      remoteAudio = new Audio()
      remoteAudio.autoplay = true
      remoteAudio.playsInline = true
      remoteAudio.style.display = 'none'
    }
    if (!remoteAudio) return
    remoteAudio.srcObject = stream
    remoteAudio.play?.().catch(() => {})
  }

  peerConnection.onconnectionstatechange = () => {
    const state = peerConnection?.connectionState
    if (state === 'connected') {
      phase.value = 'connected'
      startDuration()
      emit('state-change', 'connected')
      return
    }

    if (['failed', 'closed', 'disconnected'].includes(state)) {
      finalizeCall('连接已断开')
    }
  }

  if (localStream) {
    localStream.getTracks().forEach((track) => peerConnection.addTrack(track, localStream))
  }

  if (pendingCandidates.length) {
    const queue = [...pendingCandidates]
    pendingCandidates = []
    for (const candidate of queue) {
      await addIceCandidate(candidate)
    }
  }

  return peerConnection
}

const ensureLocalStream = async () => {
  if (localStream) return localStream
  await ensureCallPermissions()
  const mediaDevices = getWebRtcMediaDevices()
  if (!mediaDevices?.getUserMedia) {
    throw new Error('当前环境无法访问麦克风')
  }

  localStream = await mediaDevices.getUserMedia({
    audio: true,
    video: isVideoMode.value,
  })
  await syncLocalVideoPreview()
  return localStream
}

const addIceCandidate = async (candidatePayload) => {
  const candidate = createRtcIceCandidate(parseRtcPayload(candidatePayload))
  if (!candidate) return

  if (!peerConnection || !peerConnection.remoteDescription) {
    pendingCandidates.push(candidate)
    return
  }

  try {
    await peerConnection.addIceCandidate(candidate)
  } catch (error) {
    console.warn('[voice-call] add ICE failed', error)
  }
}

const applyRemoteOffer = async (offerPayload) => {
  const offer = createRtcSessionDescription(parseRtcPayload(offerPayload))
  if (!offer) return

  if (phase.value === 'incoming') {
    pendingOffer = offer
    return
  }

  await ensurePeerConnection()
  await peerConnection.setRemoteDescription(offer)

  if (pendingCandidates.length) {
    const queue = [...pendingCandidates]
    pendingCandidates = []
    for (const candidate of queue) {
      await addIceCandidate(candidate)
    }
  }

  const answer = await peerConnection.createAnswer()
  await peerConnection.setLocalDescription(answer)
  await service.post(
    '/voice-call/sdp',
    buildSignalPayload({
      sdp: JSON.stringify(answer.toJSON?.() || answer),
    }),
  )
  phase.value = 'connecting'
  emit('state-change', 'connecting')
}

const applyRemoteAnswer = async (answerPayload) => {
  const answer = createRtcSessionDescription(parseRtcPayload(answerPayload))
  if (!answer) return
  await ensurePeerConnection()
  await peerConnection.setRemoteDescription(answer)
  phase.value = 'connecting'
  emit('state-change', 'connecting')
}

const prepareStreamRemotePlayback = () => {
  prepareAppMediaSession({ withRemote: true })
  phase.value = 'connecting'
  emit('state-change', 'connecting')
}

const handleSignal = async (signal) => {
  if (!signal?.callType) return

  const callType = String(signal.callType)

  if (callType === CALL_TYPE.CALL) {
    phase.value = 'incoming'
    emit('state-change', 'incoming')
    return
  }

  if (callType === CALL_TYPE.ANSWER) {
    if (usesStreamTransport.value) {
      prepareStreamRemotePlayback()
      return
    }
    phase.value = 'connecting'
    emit('state-change', 'connecting')
    return
  }

  if (callType === CALL_TYPE.REJECT) {
    finalizeCall('对方已拒绝')
    return
  }

  if (callType === CALL_TYPE.HANGUP) {
    finalizeCall('通话已结束')
    return
  }

  if (usesStreamTransport.value) {
    return
  }

  if (callType === CALL_TYPE.SDP) {
    const sdp = parseRtcPayload(signal.sdp)
    if (!sdp?.type) return
    if (sdp.type === 'offer') {
      await applyRemoteOffer(sdp)
    } else {
      await applyRemoteAnswer(sdp)
    }
    return
  }

  if (callType === CALL_TYPE.ICE) {
    await addIceCandidate(signal.candidate)
  }
}

const resetRuntime = () => {
  stopDuration()
  elapsedSeconds.value = 0
  endReason.value = ''
  unsupportedReason.value = ''
  isMuted.value = false
  isSpeakerOn.value = true
  pendingOffer = null
  pendingCandidates = []
  clearAppRemoteRetry()
  appPlayerReady.value = false
  webRtcRemoteReady.value = false
}

const closeRtcResources = () => {
  try {
    peerConnection?.close?.()
  } catch {}
  peerConnection = null

  if (localStream) {
    localStream.getTracks().forEach((track) => track.stop())
    localStream = null
  }

  if (remoteAudio) {
    remoteAudio.pause?.()
    remoteAudio.srcObject = null
  }
  remoteAudio = null

  clearMediaElementStream(localVideoElement.value)
  clearMediaElementStream(remoteVideoElement.value)
}

const finalizeCall = (reason) => {
  phase.value = 'ended'
  endReason.value = reason
  stopDuration()
  closeRtcResources()
  void closeAppResources()
  bootstrapped = false
  emit('state-change', 'ended')
  emit('ended', reason)
}

const closePanel = () => {
  finalizeCall(endReason.value || '通话已结束')
  emit('close')
}

const startOutgoingWebRtcCall = async () => {
  try {
    busy.value = true
    await ensureLocalStream()
    await ensurePeerConnection()
    await service.post('/voice-call/call', buildSignalPayload({ extraInfo: 'voice-call' }))
    const offer = await peerConnection.createOffer({
      offerToReceiveAudio: true,
      offerToReceiveVideo: isVideoMode.value,
    })
    await peerConnection.setLocalDescription(offer)
    await service.post(
      '/voice-call/sdp',
      buildSignalPayload({
        sdp: JSON.stringify(offer.toJSON?.() || offer),
      }),
    )
    phase.value = 'calling'
    emit('state-change', 'calling')
  } finally {
    busy.value = false
  }
}

const startOutgoingStreamCall = async () => {
  try {
    busy.value = true
    prepareAppMediaSession()
    await startAppPublishing()
    await service.post('/voice-call/call', buildSignalPayload({ extraInfo: 'stream-call' }))
    phase.value = 'calling'
    emit('state-change', 'calling')
  } finally {
    busy.value = false
  }
}

const bootstrap = async () => {
  if (!props.visible || bootstrapped) return

  resetRuntime()
  bootstrapped = true
  await loadVoiceCallConfig()

  if (!supportsRequestedTransport.value) {
    phase.value = props.incoming ? 'incoming' : 'ended'
    unsupportedReason.value = usesStreamTransport.value
      ? '服务端暂未启用移动端流媒体通话链路'
      : '服务端暂未启用 WebRTC 通话链路'
    emit('state-change', phase.value)
    return
  }

  if (usesStreamTransport.value) {
    if (!isAppPlusRuntime()) {
      phase.value = props.incoming ? 'incoming' : 'ended'
      unsupportedReason.value = '当前端不支持 App 推流/拉流通话，请切换到 App 端接听'
      emit('state-change', phase.value)
      return
    }

    if (!hasRtcStreamConfig()) {
      phase.value = props.incoming ? 'incoming' : 'ended'
      unsupportedReason.value =
        '当前 App 端未配置推流/拉流地址，请补充 VITE_RTC_PUSH_BASE_URL 和 VITE_RTC_PLAY_BASE_URL'
      emit('state-change', phase.value)
      return
    }

    if (!props.incoming) {
      try {
        await startOutgoingStreamCall()
      } catch (error) {
        console.error('[voice-call] start outgoing stream call failed', error)
        finalizeCall(error?.message || '发起通话失败')
      }
      return
    }

    phase.value = 'incoming'
    emit('state-change', 'incoming')
    return
  }

  if (isAppPlusRuntime() && isVideoMode.value && !usesStreamTransport.value && !supportsBrowserDom()) {
    phase.value = props.incoming ? 'incoming' : 'ended'
    unsupportedReason.value = hasRtcStreamConfig()
      ? '当前 App 视频通话仅支持流媒体链路，请重新发起视频通话'
      : '当前 App 视频通话缺少流媒体地址配置，请补充推流和拉流地址'
    emit('state-change', phase.value)
    return
  }

  if (!canUseWebRtc()) {
    phase.value = props.incoming ? 'incoming' : 'ended'
    unsupportedReason.value = isAppPlusRuntime()
      ? hasConfiguredNativeWebRtcPlugin() || hasNativeWebRtcPlugin()
        ? '已检测到原生 WebRTC 插件配置，但当前插件没有暴露 RTCPeerConnection / getUserMedia 能力'
        : '当前 App 端未安装原生 WebRTC 插件，请先安装并配置 VITE_NATIVE_WEBRTC_PLUGIN_ID'
      : '当前浏览器缺少 WebRTC 或麦克风能力，暂时无法发起语音通话'
    emit('state-change', phase.value)
    return
  }

  phase.value = props.incoming ? 'incoming' : 'calling'
  emit('state-change', phase.value)

  if (!props.incoming) {
    try {
      await startOutgoingWebRtcCall()
    } catch (error) {
      console.error('[voice-call] start outgoing failed', error)
      finalizeCall(error?.message || '发起通话失败')
    }
  }
}

const answerCall = async () => {
  try {
    busy.value = true

    if (usesStreamTransport.value) {
      prepareAppMediaSession({ withRemote: true })
      await startAppPublishing()
      await service.post('/voice-call/answer', buildSignalPayload())
      phase.value = 'connecting'
      emit('state-change', 'connecting')
      return
    }

    await ensureLocalStream()
    await ensurePeerConnection()
    await service.post('/voice-call/answer', buildSignalPayload())
    phase.value = 'connecting'
    emit('state-change', 'connecting')
    if (pendingOffer) {
      const offer = pendingOffer
      pendingOffer = null
      await applyRemoteOffer(offer)
    }
  } catch (error) {
    console.error('[voice-call] answer failed', error)
    finalizeCall(error?.message || '接听失败')
  } finally {
    busy.value = false
  }
}

const rejectCall = async () => {
  try {
    busy.value = true
    await service.post('/voice-call/reject', buildSignalPayload())
  } catch (error) {
    console.warn('[voice-call] reject failed', error)
  } finally {
    busy.value = false
    finalizeCall('你已拒绝来电')
    emit('close')
  }
}

const hangupCall = async () => {
  try {
    busy.value = true
    await service.post('/voice-call/hangup', buildSignalPayload())
  } catch (error) {
    console.warn('[voice-call] hangup failed', error)
  } finally {
    busy.value = false
    finalizeCall('通话已结束')
    emit('close')
  }
}

const toggleMute = () => {
  const next = !isMuted.value
  isMuted.value = next

  if (usesStreamTransport.value) {
    return
  }

  if (!localStream) return
  localStream.getAudioTracks().forEach((track) => {
    track.enabled = !next
  })
}

const switchCamera = async () => {
  if (!usesStreamTransport.value || !isVideoMode.value) return
  try {
    await invokeAppPusher('switchCamera')
  } catch (error) {
    console.warn('[voice-call] switch camera failed', error)
    uni.showToast({
      title: '切换镜头失败',
      icon: 'none',
    })
  }
}

const toggleSpeaker = async () => {
  isSpeakerOn.value = !isSpeakerOn.value

  if (usesStreamTransport.value) {
    uni.showToast({
      title: 'App 端默认跟随系统音频路由，请使用系统外放/听筒切换',
      icon: 'none',
    })
    return
  }

  if (typeof remoteAudio?.setSinkId === 'function') {
    try {
      await remoteAudio.setSinkId('default')
    } catch (error) {
      console.warn('[voice-call] set sink id failed', error)
    }
    return
  }

  uni.showToast({
    title: '当前浏览器不支持切换输出设备',
    icon: 'none',
  })
}

const handleBackdropClick = () => {
  if (phase.value === 'ended' || unsupportedReason.value) {
    emit('close')
  }
}

watch(
  () => props.visible,
  async (visible) => {
    if (visible) {
      await bootstrap()
      return
    }

    bootstrapped = false
    phase.value = 'idle'
    resetRuntime()
    closeRtcResources()
    void closeAppResources()
  },
  { immediate: true },
)

watch(
  () => props.signal?._signalId,
  async () => {
    if (!props.signal || !props.visible) return
    try {
      await handleSignal(props.signal)
    } catch (error) {
      console.error('[voice-call] handle signal failed', error)
      finalizeCall('通话信令异常')
    }
  },
)

onBeforeUnmount(() => {
  resetRuntime()
  closeRtcResources()
  void closeAppResources()
})
</script>

<style scoped>
.voice-layer {
  position: fixed;
  inset: 0;
  z-index: 70;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28rpx;
}

.voice-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.58);
  backdrop-filter: blur(22rpx);
}

.voice-card {
  position: relative;
  width: min(760rpx, 100%);
  padding: 32rpx;
  border-radius: 40rpx;
  background:
    radial-gradient(circle at top, rgba(7, 193, 96, 0.18), transparent 36%),
    linear-gradient(180deg, rgba(252, 253, 253, 0.98), rgba(244, 246, 247, 0.98));
  box-shadow: 0 40rpx 100rpx rgba(15, 23, 42, 0.22);
  overflow: hidden;
}

.voice-card.mobile {
  width: 100%;
  border-radius: 36rpx;
  padding: 28rpx;
}

.voice-badge {
  display: inline-flex;
  min-width: 120rpx;
  height: 56rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  align-items: center;
  justify-content: center;
  background: rgba(7, 193, 96, 0.12);
  color: #0f9f53;
  font-size: 22rpx;
  font-weight: 600;
}

.app-media-stage {
  position: relative;
  margin-top: 24rpx;
  border-radius: 30rpx;
  overflow: hidden;
  background: #0f172a;
  height: 420rpx;
}

.stream-media-stage {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  height: auto;
  padding: 16rpx;
}

.stream-remote-stage {
  position: relative;
  height: 360rpx;
  border-radius: 24rpx;
  overflow: hidden;
  background: #0f172a;
}

.stream-local-stage {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.stream-stage-label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.76);
}

.stream-local-pusher {
  width: 100%;
  height: 220rpx;
  border-radius: 24rpx;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.08);
}

.web-video-stage .app-remote-player,
.web-video-stage .app-remote-placeholder {
  position: absolute;
  inset: 0;
}

.app-remote-player {
  width: 100%;
  height: 100%;
  background: #0f172a;
}

.app-remote-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.82);
  font-size: 26rpx;
}

.app-local-pusher {
  position: absolute;
  right: 22rpx;
  bottom: 22rpx;
  width: 176rpx;
  height: 236rpx;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 18rpx 40rpx rgba(15, 23, 42, 0.3);
  background: rgba(255, 255, 255, 0.1);
}

.web-local-preview {
  border: 2rpx solid rgba(255, 255, 255, 0.18);
}

.app-audio-stage {
  width: 2rpx;
  height: 2rpx;
  opacity: 0.01;
  overflow: hidden;
}

.app-hidden-media {
  width: 2rpx;
  height: 2rpx;
}

.voice-hero {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18rpx;
  padding: 36rpx 0 24rpx;
}

.voice-hero.compact {
  padding-top: 26rpx;
}

.voice-avatar {
  width: 176rpx;
  height: 176rpx;
  border-radius: 56rpx;
  background: #d1d5db;
  position: relative;
  z-index: 2;
}

.voice-copy {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
}

.voice-name {
  font-size: 40rpx;
  font-weight: 700;
  color: #111827;
}

.voice-sub {
  font-size: 24rpx;
  color: #6b7280;
}

.voice-rings {
  position: absolute;
  inset: 12rpx 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ring {
  position: absolute;
  border-radius: 999rpx;
  border: 1rpx solid rgba(7, 193, 96, 0.14);
}

.ring-one {
  width: 230rpx;
  height: 230rpx;
}

.ring-two {
  width: 280rpx;
  height: 280rpx;
}

.ring-three {
  width: 330rpx;
  height: 330rpx;
}

.voice-panel {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
}

.meta-item {
  padding: 20rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(15, 23, 42, 0.05);
}

.meta-label {
  display: block;
  font-size: 21rpx;
  color: #94a3b8;
  margin-bottom: 10rpx;
}

.meta-value {
  font-size: 25rpx;
  color: #111827;
  font-weight: 600;
}

.voice-tip {
  margin-top: 22rpx;
  padding: 18rpx 20rpx;
  border-radius: 22rpx;
  background: rgba(15, 23, 42, 0.04);
  font-size: 23rpx;
  color: #475569;
}

.voice-tip.success {
  background: rgba(7, 193, 96, 0.1);
  color: #0f9f53;
}

.voice-tip.danger {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}

.voice-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 28rpx;
}

.call-btn {
  flex: 1;
  min-width: 140rpx;
  height: 86rpx;
  border-radius: 24rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 27rpx;
  font-weight: 600;
}

.call-btn.answer {
  background: linear-gradient(135deg, #07c160 0%, #29d17c 100%);
  color: #ffffff;
}

.call-btn.reject {
  background: linear-gradient(135deg, #ff5d5b 0%, #f04d4a 100%);
  color: #ffffff;
}

.call-btn.subtle {
  background: rgba(15, 23, 42, 0.06);
  color: #111827;
}

.call-btn.full {
  flex-basis: 100%;
}

@media (max-width: 959px) {
  .voice-panel {
    grid-template-columns: repeat(2, 1fr);
  }

  .app-media-stage {
    height: 360rpx;
  }

  .app-local-pusher {
    width: 150rpx;
    height: 210rpx;
  }
}
</style>
