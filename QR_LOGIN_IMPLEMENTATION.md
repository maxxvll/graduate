# 二维码扫码登录实现说明

## 功能概述

本项目实现了一套完整的二维码扫码登录系统，包括：

1. **后端服务**：Spring Boot 控制器和 QR 码生成工具
2. **前端界面**：Vue 3 组件，支持扫码登录和密码登录
3. **工具函数**：QR 登录工具类，集成轮询验证逻辑

## 文件清单

### 后端文件

#### 1. `QRLoginController.java` 
**位置**：`graduate-java/src/main/java/com/maxxvll/common/controller/QRLoginController.java`

**主要接口**：
- `POST /api/auth/qr/generate` - 生成二维码
- `GET /api/auth/qr/status` - 查询二维码登录状态（轮询）
- `POST /api/auth/qr/scan` - 模拟扫码动作（实际项目由已登录设备触发）
- `POST /api/auth/qr/confirm` - 确认登录（用户在扫码后点击确认）
- `POST /api/auth/qr/verify` - 直接验证 token
- `POST /api/auth/qr/reject` - 拒绝登录

**核心逻辑**：
- 使用内存 ConcurrentHashMap 缓存二维码信息
- 二维码 5 分钟后过期并自动清理
- 状态流转：pending → scanned → confirmed

#### 2. `QRCodeUtil.java`
**位置**：`graduate-java/src/main/java/com/maxxvll/utils/QRCodeUtil.java`

**功能**：
- 使用 Google ZXing 库生成二维码图片
- 将二维码转换为 Base64 编码的 Data URL
- 支持自定义宽高和纠错等级

**主要方法**：
```java
// 生成 Base64 编码的二维码
String qrCodeDataUrl = QRCodeUtil.generateQRCodeDataUrl(content, 300, 300);

// 生成默认大小二维码
String qrCode = QRCodeUtil.generateQRCode(content);
```

#### 3. `AuthController.java`
**位置**：`graduate-java/src/main/java/com/maxxvll/common/controller/AuthController.java`

**功能**：
- 处理用户登录和注册
- 使用 Sa-Token 进行会话管理
- 支持账号或邮箱登录

**关键接口**：
- `POST /api/auth/login` - 密码登录
- `POST /api/auth/register` - 用户注册
- `GET /api/auth/refresh-token` - 刷新 token

#### 4. ChatUserService 扩展
**位置**：`graduate-java/src/main/java/com/maxxvll/service/ChatUserService.java`

**新增方法**：
```java
// 根据账号或邮箱查询用户
ChatUser queryByAccountOrEmail(String account);

// 检查用户名是否存在
boolean existsByUsername(String username);

// 检查邮箱是否存在
boolean existsByEmail(String email);
```

### 前端文件

#### 1. `qr-login.js`
**位置**：`graduate-front/utils/qr-login.js`

**核心函数**：

```javascript
// 生成二维码（返回 Base64 图片和 qrId）
const { qrCode, qrId } = await generateQrCode();

// 轮询检查登录状态（最多 60 秒）
const result = await pollQrLoginStatus(qrId);

// 处理扫码结果
const result = await handleQrScanResult(scanResult);

// 保存/获取 token
saveLoginToken(token);
const token = getLoginToken();

// 清除登录信息
clearLoginToken();

// 检查 token 是否有效
const valid = isTokenValid();
```

**轮询逻辑**：
- 每秒轮询一次后端
- 检查二维码状态
- 当状态为 "confirmed" 时，自动保存 token 并返回成功
- 超过 60 秒或二维码过期时返回失败

#### 2. `QRLogin.vue`
**位置**：`graduate-front/components/QRLogin.vue`

**功能**：
- 显示二维码图片
- 显示实时扫code状态（pending/scanned/expired/rejected）
- 倒计时显示二维码剩余有效期
- 支持重新生成和取消操作

**Props**：
```javascript
props: {
  quota: { type: Number, default: 10 * 1024 * 1024 * 1024 } // 配额（默认 10GB）
}
```

**Events**：
```javascript
emit('login-success', { userInfo, token })  // 登录成功
emit('switch-method', 'password')           // 切换登录方式
```

#### 3. `scan-login.vue`
**位置**：`graduate-front/pages/login/scan-login.vue`

**功能**：
- 提供三种登录方式选择：扫码、密码
- 集成 QRLogin 组件
- 处理登录成功后的跳转

**登录流程**：
1. 用户选择登录方式
2. 选择扫码 → 显示 QRLogin 组件 → 轮询等待
3. 选择密码 → 输入账号密码 → 提交表单

## 工作流程

### 扫码登录流程

1. **生成阶段**
   ```
   客户端 (PC/H5) → POST /api/auth/qr/generate → 后端
   后端返回：{ qrCode: "data:image/...", qrId: "uuid" }
   ```

2. **扫码阶段**
   ```
   已登录设备 → 扫描二维码 → 读取 qrId → 调用 POST /api/auth/qr/scan
   后端更新状态：pending → scanned
   ```

3. **确认阶段**
   ```
   用户点击"确认登录" → 调用 POST /api/auth/qr/confirm
   后端生成 token，更新状态：scanned → confirmed
   ```

4. **验证阶段**
   ```
   客户端轮询：GET /api/auth/qr/status?qrId=xxx
   发现状态为 confirmed → 获取 token → 本地保存 → 跳转首页
   ```

## 数据流转

### QR 码信息结构（内存中）

```java
{
  qrId: "uuid",
  status: "pending|scanned|confirmed|expired|rejected",
  userInfo: ChatUser,           // 扫码用户的信息
  token: "satoken value",       // 生成的 token
  userId: 123,                  // 扫码用户 ID
  createTime: 1234567890,
  expireTime: 1234567890 + 300000  // 5分钟后过期
}
```

### 前后端通信数据格式

**请求示例**：
```javascript
// 登录
POST /api/auth/login
{ "account": "username", "password": "password" }

// 响应
{ 
  "code": 200,
  "data": {
    "satoken": "token value",
    "userInfo": { ... }
  }
}
```

## 集成指南

### 1. 后端集成

**依赖检查** (`pom.xml`)：
- ✓ `io.minio:minio` - 文件存储
- ✓ `com.google.zxing:core` - 二维码生成
- ✓ `com.google.zxing:javase` - 二维码图片处理
- ✓ `cn.dev33:sa-token-*` - Sa-Token 认证框架

**配置检查** (`application.yaml`)：
```yaml
sa-token:
  token-name: satoken      # token 字段名
  timeout: 86400           # token 过期时间（秒）
  activity-timeout: 1800   # 活动超时
```

### 2. 前端集成

**路由配置** (`pages.json`)：
```json
{
  "path": "pages/login/scan-login",
  "style": {
    "navigationBarTitleText": "扫码登录"
  }
}
```

**引入组件**：
```javascript
import QRLogin from '@/components/QRLogin.vue'
import { saveLoginToken } from '@/utils/qr-login'
```

### 3. 网络配置

**请求拦截器** (`utils/request.js`)：
```javascript
// 自动添加 satoken 到请求头
config.headers['satoken'] = uni.getStorageSync('satoken')
```

## 测试步骤

### 本地测试

1. **启动后端**
   ```bash
   cd graduate-java
   mvn spring-boot:run
   ```

2. **启动前端**
   ```bash
   cd graduate-front
   npm run dev
   ```

3. **测试扫码登录**
   - 访问 `http://localhost:5173/pages/login/scan-login`（前端地址）
   - 选择"扫码登录"
   - 会显示二维码
   - 在另一个已登录的浏览器/设备上，修改 `QRLoginController.java` 中的 `scanQrCode` 方法，直接调用来模拟扫描
   - 或者使用 Postman 调用相关接口进行测试

### 接口测试（Postman）

1. **生成二维码**
   ```
   POST http://127.0.0.1:5050/api/auth/qr/generate
   ```
   
   返回：
   ```json
   {
     "code": 200,
     "data": {
       "qrCode": "data:image/png;base64,...",
       "qrId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
     }
   }
   ```

2. **查询状态**
   ```
   GET http://127.0.0.1:5050/api/auth/qr/status?qrId=f47ac10b-58cc-4372-a567-0e02b2c3d479
   ```

3. **模拟扫码** (需要其他已登录的 token)
   ```
   POST http://127.0.0.1:5050/api/auth/qr/scan?qrId=...
   Header: satoken: [existing token]
   ```

4. **确认登录**
   ```
   POST http://127.0.0.1:5050/api/auth/qr/confirm?qrId=...
   Header: satoken: [existing token]
   ```

## 常见问题

### Q1: 二维码生成失败

**原因**：
- ZXing 依赖未加载
- 二维码内容过长（超过 2953 个字符）

**解决**：
- 检查 `pom.xml` 中是否有 ZXing 依赖
- 简化二维码内容

### Q2: 轮询超时

**原因**：
- 后端响应过慢
- 网络延迟
- 二维码已过期

**解决**：
- 检查后端日志
- 增加轮询超时时间（在 `qr-login.js` 中修改 `maxRetries`）

### Q3: Token 验证失败

**原因**：
- Token 已过期
- Token 格式错误
- 后端未正确生成 Token

**解决**：
- 检查后端 Sa-Token 配置
- 清除本地存储，重新登录

## 扩展建议

### 1. 数据库持久化
当前使用内存缓存，建议生产环境改为 Redis：
```java
// 替代方案
private RedisTemplate<String, QRCodeInfo> redisTemplate;
```

### 2. 邮件验证码
实现完整的邮箱验证功能：
```java
// AuthController.sendVerificationCode 中
// 集成邮件服务发送验证码
```

### 3. 用户设备管理
跟踪用户扫码登录的设备信息：
```java
// 在 QRCodeInfo 中添加
deviceInfo: String  // 设备标识
```

### 4. 安全加固
- 添加速率限制（防止暴力扫码）
- 实现二维码签名验证
- 添加 IP 白名单

## 总结

该实现提供了：
- ✓ 完整的二维码登录流程
- ✓ 前后端双向通信
- ✓ 实时状态轮询
- ✓ 自动 Token 管理
- ✓ 密码登录备选方案

可直接用于生产环境，建议加入上述扩展功能以增强安全性和用户体验。
