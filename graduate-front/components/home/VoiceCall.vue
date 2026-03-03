<!-- 语音通话组件 - 兼容 PC 端和移动端 -->
<template>
  <view v-if="visible" class="voice-call-container">
    <!-- 遮罩层 -->
    <view class="voice-call-mask" @click="handleMaskClick"></view>
    
    <!-- 通话界面 -->
    <view class="voice-call-panel" :class="{ 'mobile': isMobile }">
      <!-- 通话信息 -->
      <view class="call-info">
        <image 
          v-if="callerInfo.avatar" 
          :src="callerInfo.avatar" 
          mode="aspectFill"
          class="caller-avatar"
        />
        <view v-else class="caller-avatar default-avatar">
          <text>{{ callerInfo.nickname?.charAt(0) || '用' }}</text>
        </view>
        
        <view class="caller-name">{{ callerInfo.nickname || '未知用户' }}</view>
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
      sessionId: '',
      callDuration: 0,
      durationTimer: null,
      isMuted: false,
      isSpeakerOn: false,
      localStream: null,
      peerConnection: null,
      wsMessageListener: null
    }
  },
  computed: {
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
          sessionId: this.sessionId,
          callType: '1',
          extraInfo: '语音通话邀请'
        })
        console.log('语音呼叫请求已发送')
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
      
      // 生成会话 ID
      this.sessionId = `voice_${Date.now()}`
      console.log('📞 生成的会话 ID:', this.sessionId)
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
      const { callType } = message
      
      switch (callType) {
        case '1': // 收到呼叫
          if (!this.visible) {
            // 显示来电界面
            this.$emit('state-change', { type: 'incoming', data: message })
          }
          break
          
        case '2': // 对方已接听
          this.callState = 2
          this.startDurationTimer()
          break
          
        case '3': // 对方拒绝
          uni.showToast({ title: '对方拒绝通话', icon: 'none' })
          this.close()
          break
          
        case '4': // 对方挂断
          uni.showToast({ title: '对方已挂断', icon: 'none' })
          this.close()
          break
          
        case '5': // SDP 交换
          this.handleSDP(message)
          break
          
        case '6': // ICE 交换
          this.handleICE(message)
          break
      }
    },
    
    async initWebRTC() {
      try {
        // 获取音频流
        this.localStream = await navigator.mediaDevices.getUserMedia({
          audio: true,
          video: false
        })
        
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
        
        // 监听远程流
        this.peerConnection.ontrack = (event) => {
          if (event.streams && event.streams[0]) {
            const remoteAudio = document.createElement('audio')
            remoteAudio.srcObject = event.streams[0]
            remoteAudio.autoplay = true
            remoteAudio.play()
          }
        }
        
        // 监听 ICE candidate
        this.peerConnection.onicecandidate = (event) => {
          if (event.candidate) {
            this.sendICE(event.candidate)
          }
        }
        
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
        // 获取音频流
        this.localStream = await navigator.mediaDevices.getUserMedia({
          audio: true,
          video: false
        })
        
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
        
        // 监听远程流
        this.peerConnection.ontrack = (event) => {
          if (event.streams && event.streams[0]) {
            const remoteAudio = document.createElement('audio')
            remoteAudio.srcObject = event.streams[0]
            remoteAudio.autoplay = true
            remoteAudio.play()
          }
        }
        
        // 监听 ICE candidate
        this.peerConnection.onicecandidate = (event) => {
          if (event.candidate) {
            this.sendICE(event.candidate)
          }
        }
        
        // 更新状态
        this.callState = 2
        this.startDurationTimer()
        
        // 通知后端已接听
        await service.post('/voice-call/answer', {
          targetId: this.peerInfo.id,
          sessionId: this.sessionId,
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
          sessionId: this.sessionId,
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
          sessionId: this.sessionId,
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
          sessionId: this.sessionId,
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
          sessionId: this.sessionId,
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
        }
      } catch (error) {
        console.error('处理 ICE 失败:', error)
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
  width: 400px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  padding: 40px 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  text-align: center;
  color: #fff;
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

.call-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  flex-wrap: wrap;
}

.action-btn {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.action-btn:hover {
  transform: scale(1.1);
  background: rgba(255, 255, 255, 0.3);
}

.action-btn .btn-icon {
  font-size: 32px;
  margin-bottom: 5px;
}

.action-btn .btn-text {
  font-size: 12px;
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
