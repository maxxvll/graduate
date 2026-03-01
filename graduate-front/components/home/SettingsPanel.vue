<template>
  <!-- PC 端设置页：左侧分类导航 + 右侧内容区 双栏布局 -->
  <view class="settings-root">
    <!-- ── 左侧导航 ── -->
    <view class="settings-nav">
      <!-- 用户简介卡片，点击直接跳转个人信息页 -->
      <view class="settings-user-brief" @click="activeTab = 'info'">
        <image :src="userInfo.avatar || defaultAvatar" class="settings-brief-avatar" mode="aspectFill" />
        <view class="settings-brief-info">
          <text class="settings-brief-name">{{ userInfo.nickname }}</text>
          <text class="settings-brief-id">ID: {{ userInfo.wechatId }}</text>
        </view>
      </view>

      <view class="settings-nav-group">
        <view
          v-for="item in navItems"
          :key="item.key"
          class="settings-nav-item"
          :class="{ active: activeTab === item.key }"
          @click="activeTab = item.key"
        >
          <text class="settings-nav-icon">{{ item.icon }}</text>
          <text class="settings-nav-label">{{ item.label }}</text>
        </view>
      </view>
    </view>

    <!-- ── 右侧内容区 ── -->
    <view class="settings-content">

      <!-- ① 个人信息 -->
      <template v-if="activeTab === 'info'">
        <view class="settings-panel-header">
          <text class="settings-panel-title">个人信息</text>
          <text class="settings-panel-subtitle">管理你的个人资料和展示信息</text>
        </view>
        <view class="settings-body">
          <!-- 头像 -->
          <view class="settings-section">
            <text class="section-label">头像</text>
            <view class="avatar-edit-block">
              <image :src="userInfo.avatar || defaultAvatar" class="settings-avatar-preview" mode="aspectFill" />
              <view class="avatar-edit-meta">
                <text class="avatar-tip">建议使用清晰的正面照，支持 JPG / PNG 格式</text>
                <button class="settings-action-btn" @click="$emit('openEditProfile')">更换头像</button>
              </view>
            </view>
          </view>

          <!-- 基本资料表单 -->
          <view class="settings-section">
            <text class="section-label">基本资料</text>
            <view class="settings-form">
              <view class="form-row">
                <text class="form-field-label">昵称</text>
                <text class="form-field-value">{{ userInfo.nickname }}</text>
                <text class="form-field-action" @click="$emit('openEditProfile')">编辑</text>
              </view>
              <view class="form-row">
                <text class="form-field-label">账号 ID</text>
                <text class="form-field-value mono">{{ userInfo.wechatId }}</text>
              </view>
              <view class="form-row">
                <text class="form-field-label">个性签名</text>
                <text class="form-field-value muted">{{ userInfo.signature || '未填写' }}</text>
                <text class="form-field-action" @click="$emit('openEditProfile')">编辑</text>
              </view>
              <view class="form-row no-border">
                <text class="form-field-label">所在地区</text>
                <text class="form-field-value muted">{{ userInfo.region || '未设置' }}</text>
                <text class="form-field-action" @click="$emit('openEditProfile')">编辑</text>
              </view>
            </view>
          </view>

          <!-- 二维码卡片，点击按钮弹出二维码 Modal -->
          <view class="settings-section">
            <text class="section-label">我的二维码</text>
            <view class="qrcode-card">
              <view class="qrcode-placeholder-box">
                <text class="qrcode-placeholder-icon">📱</text>
              </view>
              <view class="qrcode-card-info">
                <text class="qrcode-card-title">扫码添加好友</text>
                <text class="qrcode-card-desc">将此二维码分享给朋友，让他们扫码添加你</text>
                <button class="settings-action-btn" @click="$emit('openQrCode')">查看二维码</button>
              </view>
            </view>
          </view>
        </view>
      </template>

      <!-- ② 账号与安全 -->
      <template v-else-if="activeTab === 'security'">
        <view class="settings-panel-header">
          <text class="settings-panel-title">账号与安全</text>
          <text class="settings-panel-subtitle">管理你的登录凭证和绑定信息</text>
        </view>
        <view class="settings-body">
          <view class="settings-section">
            <text class="section-label">登录凭证</text>
            <view class="settings-option-list">
              <view class="settings-option-row">
                <view class="option-row-left">
                  <text class="option-row-title">登录密码</text>
                  <text class="option-row-desc">定期更换密码有助于保护账号安全</text>
                </view>
                <button class="settings-action-btn">修改密码</button>
              </view>
              <view class="settings-option-row no-border">
                <view class="option-row-left">
                  <text class="option-row-title">手机号码</text>
                  <text class="option-row-desc">{{ userInfo.phone || '暂未绑定' }}</text>
                </view>
                <button class="settings-action-btn">{{ userInfo.phone ? '更换' : '绑定' }}</button>
              </view>
            </view>
          </view>
        </view>
      </template>

      <!-- ③ 消息通知 -->
      <template v-else-if="activeTab === 'notify'">
        <view class="settings-panel-header">
          <text class="settings-panel-title">消息通知</text>
          <text class="settings-panel-subtitle">设置你希望接收通知的方式</text>
        </view>
        <view class="settings-body">
          <view class="settings-section">
            <text class="section-label">通知偏好</text>
            <view class="settings-option-list">
              <view class="settings-option-row">
                <view class="option-row-left">
                  <text class="option-row-title">新消息提醒</text>
                  <text class="option-row-desc">收到新消息时在桌面显示通知</text>
                </view>
                <!-- 自定义 Toggle 开关，比 checkbox 视觉效果更现代 -->
                <view class="toggle-switch" :class="{ on: notifySettings.newMessage }" @click="notifySettings.newMessage = !notifySettings.newMessage">
                  <view class="toggle-thumb"></view>
                </view>
              </view>
              <view class="settings-option-row no-border">
                <view class="option-row-left">
                  <text class="option-row-title">消息提示音</text>
                  <text class="option-row-desc">收到消息时播放提示音效</text>
                </view>
                <view class="toggle-switch" :class="{ on: notifySettings.sound }" @click="notifySettings.sound = !notifySettings.sound">
                  <view class="toggle-thumb"></view>
                </view>
              </view>
            </view>
          </view>
        </view>
      </template>

      <!-- ④ 隐私设置 -->
      <template v-else-if="activeTab === 'privacy'">
        <view class="settings-panel-header">
          <text class="settings-panel-title">隐私设置</text>
          <text class="settings-panel-subtitle">控制谁可以联系你和查看你的信息</text>
        </view>
        <view class="settings-body">
          <view class="settings-section">
            <text class="section-label">好友管理</text>
            <view class="settings-option-list">
              <view class="settings-option-row no-border">
                <view class="option-row-left">
                  <text class="option-row-title">添加好友需要验证</text>
                  <text class="option-row-desc">开启后，他人添加你时需通过验证</text>
                </view>
                <view class="toggle-switch" :class="{ on: privacySettings.friendVerify }" @click="privacySettings.friendVerify = !privacySettings.friendVerify">
                  <view class="toggle-thumb"></view>
                </view>
              </view>
            </view>
          </view>
        </view>
      </template>

      <!-- ⑤ 通用设置 -->
      <template v-else-if="activeTab === 'general'">
        <view class="settings-panel-header">
          <text class="settings-panel-title">通用设置</text>
          <text class="settings-panel-subtitle">应用全局功能配置</text>
        </view>
        <view class="settings-body">
          <view class="settings-section">
            <text class="section-label">账号操作</text>
            <view class="settings-option-list">
              <!-- 危险操作行：退出登录，使用红色高亮与普通选项区分 -->
              <view class="settings-option-row danger-row no-border" @click="$emit('logout')">
                <view class="option-row-left">
                  <text class="option-row-title danger">退出登录</text>
                  <text class="option-row-desc">退出后需重新登录才能继续使用</text>
                </view>
                <text class="option-row-arrow">›</text>
              </view>
            </view>
          </view>
        </view>
      </template>

    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  userInfo:      { type: Object, required: true },
  defaultAvatar: { type: String, default: '' },
})

defineEmits(['openEditProfile', 'openQrCode', 'logout'])

// 左侧导航分类配置，数组驱动避免硬编码重复 v-if 链
const navItems = [
  { key: 'info',     icon: '👤', label: '个人信息' },
  { key: 'security', icon: '🔒', label: '账号与安全' },
  { key: 'notify',   icon: '🔔', label: '消息通知' },
  { key: 'privacy',  icon: '🛡️', label: '隐私设置' },
  { key: 'general',  icon: '⚙️', label: '通用设置' },
]

const activeTab = ref('info')

// 通知与隐私设置为组件内本地状态，可后续通过接口持久化
const notifySettings  = ref({ newMessage: true, sound: true })
const privacySettings = ref({ friendVerify: true })
</script>

<style scoped>
/* 整体容器：flex 行，占满父容器剩余高度 */
.settings-root {
  flex: 1;
  display: flex;
  flex-direction: row;
  background: #f7f8fa;
  height: 100%;
  min-width: 0;
  overflow: hidden;
}

/* ── 左侧导航 ── */
.settings-nav {
  width: 220px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid #ebebeb;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
}
.settings-user-brief {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.15s;
}
.settings-user-brief:hover { background: #fafafa; }
.settings-brief-avatar {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  flex-shrink: 0;
  background: #e6e6e6;
}
.settings-brief-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.settings-brief-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.settings-brief-id {
  font-size: 12px;
  color: #bbb;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.settings-nav-group { padding: 10px 0; flex: 1; }
.settings-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 20px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  color: #555;
  font-size: 14px;
  user-select: none;
}
.settings-nav-item:hover  { background: #f5f5f5; color: #333; }
.settings-nav-item.active { background: #edf4ff; color: #1677ff; font-weight: 500; }
.settings-nav-icon  { font-size: 16px; width: 20px; text-align: center; flex-shrink: 0; }
.settings-nav-label { font-size: 14px; }

/* ── 右侧内容区 ── */
.settings-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
  overflow-y: auto;
  background: #f7f8fa;
}
.settings-panel-header {
  padding: 28px 40px 22px;
  background: #fff;
  border-bottom: 1px solid #ebebeb;
  flex-shrink: 0;
}
.settings-panel-title    { font-size: 20px; font-weight: 600; color: #1a1a1a; display: block; }
.settings-panel-subtitle { font-size: 13px; color: #bbb; margin-top: 5px; display: block; }
.settings-body           { padding: 28px 40px; display: flex; flex-direction: column; gap: 28px; }
.settings-section        { display: flex; flex-direction: column; gap: 10px; }
.section-label           { font-size: 11px; color: #aaa; font-weight: 600; text-transform: uppercase; letter-spacing: 0.8px; padding-left: 4px; }

/* 头像编辑 */
.avatar-edit-block {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 20px 24px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #ebebeb;
}
.settings-avatar-preview {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  flex-shrink: 0;
  background: #e6e6e6;
}
.avatar-edit-meta { display: flex; flex-direction: column; gap: 10px; }
.avatar-tip       { font-size: 13px; color: #bbb; }

/* 表单行 */
.settings-form { background: #fff; border-radius: 10px; border: 1px solid #ebebeb; overflow: hidden; }
.form-row {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #f5f5f5;
  gap: 12px;
}
.form-row.no-border   { border-bottom: none; }
.form-field-label     { font-size: 14px; color: #666; width: 88px; flex-shrink: 0; }
.form-field-value     { flex: 1; font-size: 14px; color: #1a1a1a; }
.form-field-value.mono  { font-family: 'SF Mono', 'Consolas', monospace; color: #555; font-size: 13px; }
.form-field-value.muted { color: #bbb; }
.form-field-action {
  font-size: 13px;
  color: #1677ff;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 5px;
  transition: background 0.15s;
  flex-shrink: 0;
  user-select: none;
}
.form-field-action:hover { background: #edf4ff; }

/* 通用操作按钮 */
.settings-action-btn {
  display: inline-flex;
  align-items: center;
  padding: 6px 16px;
  font-size: 13px;
  color: #1677ff;
  background: #edf4ff;
  border: 1px solid #c7e0ff;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
  white-space: nowrap;
  line-height: 1.5;
}
.settings-action-btn:hover { background: #dceeff; border-color: #a8cefc; }

/* 二维码卡片 */
.qrcode-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px 24px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #ebebeb;
}
.qrcode-placeholder-box {
  width: 68px;
  height: 68px;
  background: #f5f5f5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1px dashed #ddd;
}
.qrcode-placeholder-icon { font-size: 26px; }
.qrcode-card-info { flex: 1; display: flex; flex-direction: column; gap: 6px; }
.qrcode-card-title { font-size: 15px; font-weight: 500; color: #1a1a1a; }
.qrcode-card-desc  { font-size: 13px; color: #bbb; }

/* 选项行列表（账号安全/通知/隐私/通用） */
.settings-option-list { background: #fff; border-radius: 10px; border: 1px solid #ebebeb; overflow: hidden; }
.settings-option-row {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #f5f5f5;
  gap: 16px;
}
.settings-option-row.no-border  { border-bottom: none; }
.settings-option-row.danger-row { cursor: pointer; transition: background 0.15s; }
.settings-option-row.danger-row:hover { background: #fff5f5; }
.option-row-left {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.option-row-title        { font-size: 14px; color: #1a1a1a; }
.option-row-title.danger { color: #e53935; }
.option-row-desc         { font-size: 12px; color: #bbb; }
.option-row-arrow        { font-size: 22px; color: #ccc; line-height: 1; }

/* Toggle 开关 */
.toggle-switch {
  width: 44px;
  height: 24px;
  background: #d9d9d9;
  border-radius: 12px;
  position: relative;
  cursor: pointer;
  transition: background 0.25s;
  flex-shrink: 0;
}
.toggle-switch.on     { background: #1677ff; }
.toggle-thumb {
  position: absolute;
  width: 20px;
  height: 20px;
  background: #fff;
  border-radius: 50%;
  top: 2px;
  left: 2px;
  transition: left 0.25s;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.18);
}
.toggle-switch.on .toggle-thumb { left: 22px; }
</style>
