<!-- 语音通话组件 - 兼容 PC 端和移动端 -->
<template>
  <view v-if="visible" class="voice-call-container">
    <!-- 遮罩层 -->
    <view class="voice-call-mask" @click="handleMaskClick"></view>
    
    <!-- 通话界面 -->
    <view class="voice-call-panel" :class="{ 'mobile': isMobile }">
      <!-- 通话信息 -->
      <view class="call-info">
        <!-- 视频模式时在上方占位（真实 video 节点由脚本插入以兼容 uni-app） -->
        <view v-if="mode==='video'" class="video-placeholder"></view>
        <image 
          v-if="displayInfo.avatar" 
          :src="displayInfo.avatar" 
          mode="aspectFill"
          class="caller-avatar"
        />
        <view v-else class="caller-avatar default-avatar">
          <text>{{ displayInfo.nickname?.charAt(0) || '用' }}</text>
        </view>
        
        <view class="caller-name">{{ displayInfo.nickname || '未知用户' }}</view>
        <view class="call-status">{{ statusText }}</view>
        <view v-if="callDuration > 0" class="call-timer">{{ formatDuration(callDuration) }}</view>
      </view>
      
      <!-- 操作按钮 -->
      <view class="call-actions">
        <!-- 正在呼叫时显示拒绝按钮 -->
        <button 
          v-if="callState === 1" 
          class="action-btn reject-btn"
          @click="rejectCall"
        >
          <text class="btn-icon">✕</text>
          <text class="btn-text">拒绝</text>
        </button>
        
        <!-- 来电时显示接听和拒绝按钮 -->
        <template v-if="isIncomingCall && callState === 1">
          <button class="action-btn answer-btn" @click="answerCall">
            <text class="btn-icon">✓</text>
            <text class="btn-text">接听</text>
          </button>
        </template>
        
        <!-- 通话中显示挂断按钮 -->
        <button 
          v-if="callState === 2" 
          class="action-btn hangup-btn"
          @click="hangupCall"
        >
          <text class="btn-icon">📞</text>
          <text class="btn-text">挂断</text>
        </button>
        
        <!-- 静音按钮（通话中） -->
        <button 
          v-if="callState === 2" 
          class="action-btn mute-btn"
          :class="{ 'active': isMuted }"
          @click="toggleMute"
        >
          <text class="btn-icon">{{ isMuted ? '🔇' : '🎤' }}</text>
          <text class="btn-text">{{ isMuted ? '取消静音' : '静音' }}</text>
        </button>
        
        <!-- 扬声器按钮（通话中） -->
        <button 
          v-if="callState === 2" 
          class="action-btn speaker-btn"
          :class="{ 'active': isSpeakerOn }"
          @click="toggleSpeaker"
        >
          <text class="btn-icon">{{ isSpeakerOn ? '🔊' : '🔈' }}</text>
          <text class="btn-text">{{ isSpeakerOn ? '免提' : '听筒' }}</text>
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import service from '@/utils/request'
import { wsClient } from '@/utils/websocket'

export default {
  name: 'VoiceCall',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    // 呼叫方向：'outgoing' - 呼出，'incoming' - 呼入
    direction: {
      type: String,
      default: 'outgoing'
    },
    // 对方用户信息
    peerInfo: {
      type: Object,
      default: () => ({})
    },
    // 会话 ID（可选，如果不传则自动生成）
    sessionId: {
      type: String,
      default: ''
    },
    // 通话模式：'audio' 或 'video'
    mode: {
      type: String,
      default: 'audio'
    }
  },
  emits: ['close', 'state-change'],
  data() {
    return {
      isMobile: false,
      callState: 1, // 1-呼叫中，2-通话中，3-已结束
      isIncomingCall: this.direction === 'incoming',
      callerInfo: {}, // 主叫方信息
      calleeInfo: {}, // 被叫方信息
      internalSessionId: '', // 内部使用的 sessionId
      callDuration: 0,
      durationTimer: null,
      isMuted: false,
      isSpeakerOn: false,
      localStream: null,
      peerConnection: null,
      // 当信令在 peerConnection 创建之前到达时，先缓存起来，等创建完成后再处理
      pendingRemoteSDP: null,
      pendingICE: [],
      // 复用远端 audio 元素，避免生成过多元素
      remoteAudioEl: null,
      // video 元素（video 通话时使用）
      remoteVideoEl: null,
      localVideoEl: null,
      wsMessageListener: null
    }
  },
  computed: {
    // 用于模板显示的用户信息：对呼入显示 callerInfo，呼出显示 calleeInfo（呼叫方界面应展示被叫信息）
    displayInfo() {
      // 如果呼出但 calleeInfo 尚未被初始化（可能在 mounted 前 prop 更新），回退到 peerInfo
      if (this.isIncomingCall) {
        return this.callerInfo || {}
      }
      if (this.calleeInfo && this.calleeInfo.id) {
        return this.calleeInfo
      }
      return this.peerInfo || {}
    },
    statusText() {
      if (this.callState === 1) {
        return this.isIncomingCall ? '来电...' : '呼叫中...'
      } else if (this.callState === 2) {
        return '通话中'
      } else {
        return '通话已结束'
      }
    }
  },
  watch: {
    // 当 peerInfo 更新时，及时刷新 calleeInfo/callerInfo
    peerInfo: {
      deep: true,
      immediate: true,
      handler(newVal) {
        if (this.isIncomingCall) {
          this.callerInfo = { ...newVal }
        } else {
          this.calleeInfo = { ...newVal }
        }
      }
    }
  },
  async mounted() {
    // 检测设备类型
    // #ifdef H5
    this.isMobile = /Android|iPhone/i.test(navigator.userAgent)
    // #endif
    
    // 初始化通话信息
    this.initCallInfo()
    
    // 监听 WebSocket 消息
    this.setupWebSocketListener()
    
    // 如果是呼出，先发送呼叫请求，再初始化 WebRTC
    if (!this.isIncomingCall) {
      await this.makeCall()
      // 延迟一点初始化 WebRTC，等待信令发送
      setTimeout(() => {
        this.initWebRTC()
      }, 300)
    }
  },
  beforeUnmount() {
    this.cleanup()
  },
  methods: {
    async makeCall() {
      try {
        // 发送呼叫请求到后端
        await service.post('/voice-call/call', {
          targetId: this.peerInfo.id,
          sessionId: this.internalSessionId,
          callType: '1',
          extraInfo: this.mode === 'video' ? '视频通话邀请' : '语音通话邀请',
          mode: this.mode
        })
        console.log((this.mode==='video'?'视频':'语音') + '呼叫请求已发送')
      } catch (error) {
        console.error('发起语音呼叫失败:', error)
        uni.showToast({ title: '发起呼叫失败', icon: 'none' })
      }
    },
    
    initCallInfo() {
      const currentUserId = uni.getStorageSync('currentUserId')
      const currentUserAvatar = uni.getStorageSync('currentUserAvatar')
      const currentUserNickname = uni.getStorageSync('currentUserNickname')
      
      console.log('📞 VoiceCall 组件初始化 - 方向:', this.isIncomingCall ? '呼入' : '呼出')
      console.log('📞 VoiceCall 组件 - peerInfo:', this.peerInfo)
      console.log('📞 VoiceCall 组件 - props.sessionId:', this.sessionId)
      
      if (this.isIncomingCall) {
        // 呼入：显示主叫方信息（peerInfo 就是主叫方）
        this.callerInfo = { ...this.peerInfo }
        this.calleeInfo = {
          id: currentUserId,
          avatar: currentUserAvatar,
          nickname: currentUserNickname
        }
        console.log('✅ 呼入模式 - 主叫方信息:', this.callerInfo)
      } else {
        // 呼出：显示被叫方信息（peerInfo 就是被叫方）
        this.callerInfo = {
          id: currentUserId,
          avatar: currentUserAvatar,
          nickname: currentUserNickname
        }
        this.calleeInfo = { ...this.peerInfo }
        console.log('✅ 呼出模式 - 被叫方信息:', this.calleeInfo)
      }
      
      // 使用传入的 sessionId 或自动生成
      if (this.sessionId) {
        this.internalSessionId = this.sessionId
        console.log('📞 使用传入的 sessionId:', this.internalSessionId)
      } else {
        // 生成会话 ID
        this.internalSessionId = `voice_${Date.now()}`
        console.log('📞 自动生成的 sessionId:', this.internalSessionId)
      }
    },
    
    setupWebSocketListener() {
      this.wsMessageListener = (data) => {
        try {
          const message = typeof data === 'string' ? JSON.parse(data) : data
          
          // 判断是否是语音通话信令
          if (message.callType && ['1', '2', '3', '4', '5', '6'].includes(message.callType)) {
            this.handleVoiceCallMessage(message)
          }
        } catch (error) {
          console.error('处理语音通话消息失败:', error)
        }
      }
      uni.$on('wsMessage', this.wsMessageListener)
    },
    
    handleVoiceCallMessage(message) {
      const { callType, sessionId } = message
      
      console.log('📞 VoiceCall 收到信令:', callType, 'sessionId:', sessionId, '当前 internalSessionId:', this.internalSessionId)
      
      // 对于呼入场景，第一次收到的信令（callType='1'）用于初始化 sessionId
      if (this.isIncomingCall && callType === '1' && !this.internalSessionId) {
        // 使用信令中的 sessionId
        this.internalSessionId = sessionId
        console.log('✅ 呼入模式，使用信令中的 sessionId:', this.internalSessionId)
      }
      
      // 验证 sessionId 是否匹配（确保是当前通话的信令）
      if (sessionId && sessionId !== this.internalSessionId) {
        console.warn('⚠️ SessionId 不匹配，忽略此次信令')
        return
      }
      
      switch (callType) {
        case '1': // 收到呼叫（呼入）
          if (!this.visible) {
            // 显示来电界面
            this.$emit('state-change', { type: 'incoming', data: message })
          }
          break
          
        case '2': // 对方已接听（主叫方收到此消息）
          console.log('✅ 对方已接听，开始通话')
          this.callState = 2
          this.startDurationTimer()
          uni.showToast({ title: '对方已接听', icon: 'success' })
          break
          
        case '3': // 对方拒绝（主叫方收到此消息）
          console.log('❌ 对方拒绝通话')
          uni.showToast({ title: '对方拒绝通话', icon: 'none' })
          this.close()
          break
          
        case '4': // 对方挂断（双方都可能收到）
          console.log('📴 对方挂断通话')
          uni.showToast({ title: '通话已结束', icon: 'none' })
          this.close()
          break
          
        case '5': // SDP 交换
          this.handleSDP(message)
          break
          
        case '6': // ICE 交换
          this.handleICE(message)
          break
          
        default:
          console.warn('未知信令类型:', callType)
      }
    },
    
    async initWebRTC() {
      try {
        // 获取本地媒体流（音频或音视频，基于 mode）
        const _constraints = this.mode === 'video' ? { audio: true, video: { width: 640, height: 480 } } : { audio: true, video: false }
        this.localStream = await navigator.mediaDevices.getUserMedia(_constraints)
        // 如果是视频模式，创建并插入本地预览 video 节点（静音以避免回声）
        if (this.mode === 'video') {
          try {
            this.localVideoEl = document.createElement('video')
            this.localVideoEl.autoplay = true
            this.localVideoEl.muted = true
            this.localVideoEl.playsInline = true
            this.localVideoEl.className = 'local-preview'
            this.localVideoEl.srcObject = this.localStream
            const panel = document.querySelector('.voice-call-panel')
            if (panel) panel.appendChild(this.localVideoEl)
          } catch (e) {
            console.warn('创建本地预览失败:', e)
          }
        }
        
        // 创建 RTCPeerConnection
        const config = {
          iceServers: [
            { urls: 'stun:stun.l.google.com:19302' }
          ]
        }
        
        this.peerConnection = new RTCPeerConnection(config)
        
        // 添加本地流
        this.localStream.getTracks().forEach(track => {
          this.peerConnection.addTrack(track, this.localStream)
        })
        
        // 监听远端流（支持 audio / video）
        this.peerConnection.ontrack = (event) => {
          if (event.streams && event.streams[0]) {
            try {
              const remoteStream = event.streams[0]
              if (this.mode === 'video') {
                if (!this.remoteVideoEl) {
                  this.remoteVideoEl = document.createElement('video')
                  this.remoteVideoEl.autoplay = true
                  this.remoteVideoEl.playsInline = true
                  this.remoteVideoEl.className = 'remote-video'
                }
                this.remoteVideoEl.srcObject = remoteStream
                const p = this.remoteVideoEl.play()
                if (p && p.catch) p.catch(err => console.warn('远端视频播放被阻止:', err))

                // 移除占位元素
                const placeholder = document.querySelector('.video-placeholder')
                if (placeholder && placeholder.parentNode) {
                  placeholder.parentNode.removeChild(placeholder)
                }

                if (!document.body.contains(this.remoteVideoEl)) {
                  const panel = document.querySelector('.voice-call-panel')
                  if (panel) panel.insertBefore(this.remoteVideoEl, panel.firstChild)
                }
              } else {
                if (!this.remoteAudioEl) {
                  this.remoteAudioEl = document.createElement('audio')
                  this.remoteAudioEl.autoplay = true
                }
                this.remoteAudioEl.srcObject = remoteStream
                const p = this.remoteAudioEl.play()
                if (p && p.catch) p.catch(err => console.warn('远端音频播放被阻止:', err))
              }
            } catch (err) {
              console.error('处理远端流失败:', err)
            }
          }
        }
        
        // 监听 ICE candidate
        this.peerConnection.onicecandidate = (event) => {
          if (event.candidate) {
            this.sendICE(event.candidate)
          }
        }

        // 如果有接收到但尚未处理的远端 SDP/ICE，按序处理
        this._drainPendingSignaling()
        
        // 创建 Offer
        const offer = await this.peerConnection.createOffer()
        await this.peerConnection.setLocalDescription(offer)
        
        // 发送 Offer
        await this.sendSDP(offer)
        
      } catch (error) {
        console.error('初始化 WebRTC 失败:', error)
        uni.showToast({ title: '无法获取麦克风权限', icon: 'none' })
      }
    },
    
    async answerCall() {
      try {
        // 获取本地媒体流（音频或音视频，基于 mode）
        const _constraints = this.mode === 'video' ? { audio: true, video: { width: 640, height: 480 } } : { audio: true, video: false }
        this.localStream = await navigator.mediaDevices.getUserMedia(_constraints)
        // 如果是视频模式，创建本地预览节点
        if (this.mode === 'video') {
          try {
            this.localVideoEl = document.createElement('video')
            this.localVideoEl.autoplay = true
            this.localVideoEl.muted = true
            this.localVideoEl.playsInline = true
            this.localVideoEl.className = 'local-preview'
            this.localVideoEl.srcObject = this.localStream
            const panel = document.querySelector('.voice-call-panel')
            if (panel) panel.appendChild(this.localVideoEl)
          } catch (e) {
            console.warn('创建本地预览失败:', e)
          }
        }
        
        // 创建 RTCPeerConnection
        const config = {
          iceServers: [
            { urls: 'stun:stun.l.google.com:19302' }
          ]
        }
        
        this.peerConnection = new RTCPeerConnection(config)
        
        // 添加本地流
        this.localStream.getTracks().forEach(track => {
          this.peerConnection.addTrack(track, this.localStream)
        })
        
        // 监听远端流（支持 audio / video）
        this.peerConnection.ontrack = (event) => {
          if (event.streams && event.streams[0]) {
            try {
              const remoteStream = event.streams[0]
              if (this.mode === 'video') {
                if (!this.remoteVideoEl) {
                  this.remoteVideoEl = document.createElement('video')
                  this.remoteVideoEl.autoplay = true
                  this.remoteVideoEl.playsInline = true
                  this.remoteVideoEl.className = 'remote-video'
                }
                this.remoteVideoEl.srcObject = remoteStream
                const p = this.remoteVideoEl.play()
                if (p && p.catch) p.catch(err => console.warn('远端视频播放被阻止:', err))

                // 移除占位元素
                const placeholder = document.querySelector('.video-placeholder')
                if (placeholder && placeholder.parentNode) {
                  placeholder.parentNode.removeChild(placeholder)
                }

                if (!document.body.contains(this.remoteVideoEl)) {
                  const panel = document.querySelector('.voice-call-panel')
                  if (panel) panel.insertBefore(this.remoteVideoEl, panel.firstChild)
                }
              } else {
                if (!this.remoteAudioEl) {
                  this.remoteAudioEl = document.createElement('audio')
                  this.remoteAudioEl.autoplay = true
                }
                this.remoteAudioEl.srcObject = remoteStream
                const p = this.remoteAudioEl.play()
                if (p && p.catch) p.catch(err => console.warn('远端音频播放被阻止:', err))
              }
            } catch (err) {
              console.error('处理远端流失败:', err)
            }
          }
        }
        
        // 监听 ICE candidate
        this.peerConnection.onicecandidate = (event) => {
          if (event.candidate) {
            this.sendICE(event.candidate)
          }
        }
        // 处理可能已到达的远端 SDP/ICE（通常是 Offer）。
        // 如果之前收到过 Offer，会在这里设置远端描述并创建 Answer。
        if (this.pendingRemoteSDP) {
          try {
            await this.peerConnection.setRemoteDescription(new RTCSessionDescription(this.pendingRemoteSDP))
            const answer = await this.peerConnection.createAnswer()
            await this.peerConnection.setLocalDescription(answer)
            await this.sendSDP(answer)
            // 清空缓存
            this.pendingRemoteSDP = null
          } catch (err) {
            console.error('在接听时处理挂起的 SDP 失败:', err)
          }
        }

        // 添加之前到达但尚未处理的 ICE 候选
        if (this.pendingICE && this.pendingICE.length) {
          for (const c of this.pendingICE) {
            try {
              await this.peerConnection.addIceCandidate(new RTCIceCandidate(c))
            } catch (err) {
              console.warn('添加挂起 ICE 失败:', err)
            }
          }
          this.pendingICE = []
        }

        // 更新状态
        this.callState = 2
        this.startDurationTimer()

        // 通知后端已接听
        await service.post('/voice-call/answer', {
          targetId: this.peerInfo.id,
          sessionId: this.internalSessionId,
          callType: '2',
          extraInfo: '已接听'
        })
        
      } catch (error) {
        console.error('接听失败:', error)
        uni.showToast({ title: '接听失败', icon: 'none' })
      }
    },
    
    async rejectCall() {
      try {
        await service.post('/voice-call/reject', {
          targetId: this.peerInfo.id,
          sessionId: this.internalSessionId,
          callType: '3',
          extraInfo: '拒绝通话'
        })
        this.close()
      } catch (error) {
        console.error('拒绝通话失败:', error)
      }
    },
    
    async hangupCall() {
      try {
        await service.post('/voice-call/hangup', {
          targetId: this.peerInfo.id,
          sessionId: this.internalSessionId,
          callType: '4',
          extraInfo: '挂断通话'
        })
        this.close()
      } catch (error) {
        console.error('挂断通话失败:', error)
      }
    },
    
    async sendSDP(sdp) {
      try {
        await service.post('/voice-call/sdp', {
          targetId: this.isIncomingCall ? this.callerInfo.id : this.calleeInfo.id,
          sessionId: this.internalSessionId,
          sdp: JSON.stringify(sdp)
        })
      } catch (error) {
        console.error('发送 SDP 失败:', error)
      }
    },
    
    async sendICE(candidate) {
      try {
        await service.post('/voice-call/ice', {
          targetId: this.isIncomingCall ? this.callerInfo.id : this.calleeInfo.id,
          sessionId: this.internalSessionId,
          candidate: JSON.stringify(candidate)
        })
      } catch (error) {
        console.error('发送 ICE 失败:', error)
      }
    },
    
    async handleSDP(message) {
      try {
        const sdp = JSON.parse(message.sdp)
        if (this.peerConnection) {
          await this.peerConnection.setRemoteDescription(new RTCSessionDescription(sdp))
          
          // 如果是 Offer，创建 Answer
          if (sdp.type === 'offer') {
            const answer = await this.peerConnection.createAnswer()
            await this.peerConnection.setLocalDescription(answer)
            await this.sendSDP(answer)
          }
        } else {
          // peerConnection 尚未创建（例如来电界面尚未接听），缓存 SDP，等待接听后处理
          console.log('🔶 收到 SDP 但 peerConnection 未创建，缓存 SDP')
          this.pendingRemoteSDP = sdp
        }
      } catch (error) {
        console.error('处理 SDP 失败:', error)
      }
    },
    
    async handleICE(message) {
      try {
        const candidate = JSON.parse(message.candidate)
        if (this.peerConnection) {
          await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate))
        } else {
          // peerConnection 尚未创建，缓存 ICE，等创建后再 add
          console.log('🔶 收到 ICE 但 peerConnection 未创建，缓存 candidate')
          this.pendingICE.push(candidate)
        }
      } catch (error) {
        console.error('处理 ICE 失败:', error)
      }
    },

    // 处理之前缓存的 SDP/ICE（在 peerConnection 创建后调用）
    async _drainPendingSignaling() {
      if (!this.peerConnection) return

      if (this.pendingRemoteSDP) {
        try {
          await this.peerConnection.setRemoteDescription(new RTCSessionDescription(this.pendingRemoteSDP))
          if (this.pendingRemoteSDP.type === 'offer') {
            const answer = await this.peerConnection.createAnswer()
            await this.peerConnection.setLocalDescription(answer)
            await this.sendSDP(answer)
          }
        } catch (err) {
          console.error('处理挂起 SDP 失败:', err)
        }
        this.pendingRemoteSDP = null
      }

      if (this.pendingICE && this.pendingICE.length) {
        for (const c of this.pendingICE) {
          try {
            await this.peerConnection.addIceCandidate(new RTCIceCandidate(c))
          } catch (err) {
            console.warn('添加挂起 ICE 失败:', err)
          }
        }
        this.pendingICE = []
      }
    },
    
    startDurationTimer() {
      this.callDuration = 0
      this.durationTimer = setInterval(() => {
        this.callDuration++
      }, 1000)
    },
    
    formatDuration(seconds) {
      const mins = Math.floor(seconds / 60)
      const secs = seconds % 60
      return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
    },
    
    toggleMute() {
      if (this.localStream) {
        const audioTrack = this.localStream.getAudioTracks()[0]
        if (audioTrack) {
          audioTrack.enabled = !audioTrack.enabled
          this.isMuted = !audioTrack.enabled
        }
      }
    },
    
    toggleSpeaker() {
      // 切换扬声器/听筒模式
      this.isSpeakerOn = !this.isSpeakerOn
      // 实际项目中可能需要调用设备特定 API
    },
    
    handleMaskClick() {
      // 通话中不允许点击遮罩关闭
      if (this.callState !== 2) {
        this.close()
      }
    },
    
    close() {
      this.cleanup()
      this.$emit('close')
    },
    
    cleanup() {
      // 停止计时器
      if (this.durationTimer) {
        clearInterval(this.durationTimer)
        this.durationTimer = null
      }
      
      // 关闭 WebRTC 连接
      if (this.peerConnection) {
        this.peerConnection.close()
        this.peerConnection = null
      }
      
      // 停止音频流
      if (this.localStream) {
        this.localStream.getTracks().forEach(track => track.stop())
        this.localStream = null
      }

      // 清理远端 audio/video、本地预览元素
      if (this.remoteAudioEl) {
        try { this.remoteAudioEl.pause(); this.remoteAudioEl.srcObject = null } catch (e) {}
        try { if (this.remoteAudioEl.parentNode) this.remoteAudioEl.parentNode.removeChild(this.remoteAudioEl) } catch (e) {}
        this.remoteAudioEl = null
      }
      if (this.remoteVideoEl) {
        try { this.remoteVideoEl.pause(); this.remoteVideoEl.srcObject = null } catch (e) {}
        try { if (this.remoteVideoEl.parentNode) this.remoteVideoEl.parentNode.removeChild(this.remoteVideoEl) } catch (e) {}
        this.remoteVideoEl = null
      }
      if (this.localVideoEl) {
        try { this.localVideoEl.pause(); this.localVideoEl.srcObject = null } catch (e) {}
        try { if (this.localVideoEl.parentNode) this.localVideoEl.parentNode.removeChild(this.localVideoEl) } catch (e) {}
        this.localVideoEl = null
      }
      
      // 移除 WebSocket 监听
      if (this.wsMessageListener) {
        uni.$off('wsMessage', this.wsMessageListener)
        this.wsMessageListener = null
      }
    }
  }
}
</script>

<style scoped>
.voice-call-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
}

.voice-call-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
}

.voice-call-panel {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 420px;
  min-width: 320px;
  min-height: 360px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  padding: 40px 20px 56px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  text-align: center;
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.video-placeholder {
  width: 100%;
  height: 180px;
  background: rgba(0,0,0,0.15);
  border-radius: 12px;
  margin-bottom: 8px;
  overflow: hidden;
}

.remote-video {
  width: 100%;
  height: 180px;
  border-radius: 12px;
  object-fit: cover;
  margin-bottom: 8px;
}

.local-preview {
  position: absolute;
  right: 24px;
  top: 24px;
  width: 120px;
  height: 90px;
  border-radius: 8px;
  object-fit: cover;
  box-shadow: 0 6px 18px rgba(0,0,0,0.25);
  z-index: 10;
}


.voice-call-panel.mobile {
  width: 90%;
  max-width: 400px;
}

.call-info {
  margin-bottom: 40px;
}

.caller-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  margin: 0 auto 20px;
  object-fit: cover;
  border: 4px solid rgba(255, 255, 255, 0.3);
}

.default-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  margin: 0 auto 20px;
  background: rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: bold;
  border: 4px solid rgba(255, 255, 255, 0.3);
}

.caller-name {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 10px;
}

.call-status {
  font-size: 16px;
  opacity: 0.8;
  margin-bottom: 5px;
}

.call-timer {
  font-size: 18px;
  font-weight: bold;
  margin-top: 10px;
}

/* container for action buttons; flex-wrap allows multiple rows on small panels */
.call-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 20px;
}

/* individual circular buttons */
.action-btn {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  cursor: pointer;
  transition: all 0.3s;
}

.action-btn:hover {
  transform: scale(1.1);
  background: rgba(255, 255, 255, 0.3);
}

.action-btn .btn-icon {
  font-size: 24px;
  margin-bottom: 3px;
}

.action-btn .btn-text {
  font-size: 10px;
}

.reject-btn {
  background: #ff3b30;
}

.answer-btn {
  background: #34c759;
}

.hangup-btn {
  background: #ff3b30;
}

.mute-btn.active,
.speaker-btn.active {
  background: rgba(255, 255, 255, 0.5);
}
</style>
