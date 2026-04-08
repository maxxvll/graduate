package com.maxxvll.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.annotation.RateLimit;
import com.maxxvll.common.annotation.RequirePermission;
import com.maxxvll.common.config.KafkaFeatureProperties;
import com.maxxvll.common.dto.*;
import com.maxxvll.common.enums.QrCodeStatus;
import com.maxxvll.common.event.EmailEvent;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.common.producer.EmailEventProducer;
import com.maxxvll.common.vo.*;
import com.maxxvll.service.ChatUserService;
import com.maxxvll.utils.FileSecurityUtil;
import com.maxxvll.utils.MinioUtil;
import com.maxxvll.utils.RedissonCacheUtil;
import com.maxxvll.utils.UserContextUtil;
import com.wf.captcha.SpecCaptcha;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/user")
@Tag(name = "用户", description = "用户认证与信息相关接口")
public class UserController extends BaseController {
    @Resource
    private ChatUserService chatUserService;
    @Resource
    private RedissonCacheUtil redissonCacheUtils;
    @Resource
    private MinioUtil minioUtil;
    @Resource
    private JavaMailSender mailSender;
    @Resource
    private EmailEventProducer emailEventProducer;
    @Resource
    private KafkaFeatureProperties kafkaFeatureProperties;
    @Resource
    private FileSecurityUtil fileSecurityUtil;
    @Value("${spring.mail.username}")
    private String mailFrom;

    // ==================== 登录相关 ====================
    /**
     * 用户登录
     * 限流：每IP每分钟最多10次
     */
    @RateLimit(limit = 10, period = 60, limitType = RateLimit.LimitType.IP, message = "登录操作过于频繁，请稍后再试")
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名密码登录，返回JWT令牌")
    public Result<String> login(@Valid @RequestBody UserLoginDTO userLoginDTO) {
        String token = chatUserService.login(userLoginDTO);
        return Result.success("登录成功", token);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "注销当前登录状态")
    public Result<Void> logout() {
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        Object loginId = tokenInfo.getLoginId();
        String loginDeviceType = tokenInfo.getLoginDeviceType();

        StpUtil.logout(loginId, loginDeviceType);

        log.info("[用户登出] userId={}, deviceType={}, result=SUCCESS",
            loginId, loginDeviceType == null ? "默认" : loginDeviceType);

        return Result.success("登出成功：已下线" + (loginDeviceType == null ? "默认" : loginDeviceType) + "端");
    }

    // ==================== 注册相关 ====================
    /**
     * 发送邮箱验证码，60s内不允许重复发送，验证码有效期5分钟
     * 限流：每IP每分钟最多3次
     */
    @RateLimit(limit = 3, period = 60, limitType = RateLimit.LimitType.IP, message = "验证码发送过于频繁，请稍后再试")
    @PostMapping("/sendEmailCode")
    @Operation(summary = "发送邮箱验证码", description = "发送注册用的邮箱验证码，60秒内不能重复发送")
    public Result<Void> sendEmailCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (StrUtil.isBlank(email)) {
            throw new BusinessException("邮箱不能为空");
        }
        // 防止60s内重复发送：检查是否已存在未过期的Key
        String redisKey = redissonCacheUtils.getEmailCodeKey(email);
        if (redissonCacheUtils.exists(redisKey)) {
            Long remainTime = redissonCacheUtils.getRemainingTime(redisKey);
            if (remainTime != null && remainTime > 240) { // 5min TTL > 4min: sent within 60s
                throw new BusinessException("发送过于频繁，请60秒后重试");
            }
        }
        // 生成6位随机数字验证码
        String code = String.format("%06d", new Random().nextInt(1000000));
        // 存入Redis，5分钟过期
        redissonCacheUtils.set(redisKey, code, 5, TimeUnit.MINUTES);

        // 检查是否启用 Kafka 异步发送
        if (kafkaFeatureProperties.isEmailAsyncEnabled()) {
            // 使用 Kafka 异步发送邮件
            try {
                EmailEvent event = EmailEvent.builder()
                    .to(email)
                    .subject("注册验证码")
                    .content("您好！\n\n您的注册验证码为：" + code + "\n\n验证码有效期5分钟，请勿泄露给他人。\n\n如非本人操作，请忽略此邮件。")
                    .emailType(EmailEvent.EmailType.REGISTER_CODE)
                    .verificationCode(code)
                    .from(mailFrom)
                    .build();

                emailEventProducer.sendEmailEvent(event);

                log.info("邮箱验证码已提交异步发送（Kafka），email: {}, eventId: {}", email, event.getEventId());

            } catch (Exception e) {
                // Kafka 发送失败，删除 Redis Key，避免用户无法重试
                redissonCacheUtils.delete(redisKey);
                log.error("Kafka 邮件事件发送失败，email: {}", email, e);
                throw new BusinessException("验证码发送失败，请稍后重试");
            }
        } else {
            // 使用原有同步逻辑（回滚方案）
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(email);
                message.setSubject("注册验证码");
                message.setText("您好！\n\n您的注册验证码为：" + code + "\n\n验证码有效期5分钟，请勿泄露给他人。\n\n如非本人操作，请忽略此邮件。");
                mailSender.send(message);
                log.info("邮箱验证码已发送（同步），email: {}", email);
            } catch (Exception e) {
                // 删除Redis Key，避免发送失败后用户无法重试
                redissonCacheUtils.delete(redisKey);
                log.error("邮箱验证码发送失败（同步），email: {}", email, e);
                throw new BusinessException("验证码发送失败，请检查邮箱地址是否正确");
            }
        }

        return Result.success("验证码已发送，请查收邮箱");
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "使用邮箱验证码注册新用户")
    public Result<Void> register(@Valid @RequestBody UserRegisterDTO userRegisterDTO) {
        // 校验邮箱验证码
        String email = userRegisterDTO.getEmail();
        if (StrUtil.isBlank(email)) {
            throw new BusinessException("邮箱不能为空");
        }
        String emailCode = userRegisterDTO.getEmailCode();
        if (StrUtil.isBlank(emailCode)) {
            throw new BusinessException("请输入邮箱验证码");
        }
        String redisKey = redissonCacheUtils.getEmailCodeKey(email);
        String storedCode = redissonCacheUtils.get(redisKey);
        if (storedCode == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!storedCode.equals(emailCode)) {
            throw new BusinessException("验证码错误");
        }
        // 校验通过，删除Redis中的验证码
        redissonCacheUtils.delete(redisKey);
        chatUserService.register(userRegisterDTO);
        return Result.success("注册成功");
    }

    @GetMapping("/captcha/generate")
    @Operation(summary = "生成图形验证码", description = "生成图形验证码，返回Base64编码图片")
    public Result<CaptchaVO> generateCaptcha() {
        String captchaKey = UUID.randomUUID().toString().replace("-", "");
        SpecCaptcha captcha = new SpecCaptcha(130, 48, 4);
        captcha.setCharType(SpecCaptcha.TYPE_DEFAULT);
        String captchaCode = captcha.text().toLowerCase();

        String redisKey = redissonCacheUtils.getCaptchaKey(captchaKey);
        redissonCacheUtils.set(redisKey, captchaCode, 2, TimeUnit.MINUTES);

        return Result.success("生成成功", CaptchaVO.builder()
                .captchaKey(captchaKey)
                .captchaBase64(captcha.toBase64())
                .build());
    }

    // ==================== 二维码相关 ====================
    /**
     * 生成二维码
     * 返回二维码Base64编码和二维码ID，前端使用qrCodeId轮询查询状态
     */
    @GetMapping(value = "/qrcode/generate")
    @Operation(summary = "生成二维码", description = "生成用于扫码登录的二维码")
    public Result<QrCodeGenerateVO> generateQrCode() {
        String qrCodeId = UUID.randomUUID().toString().replace("-", "");
        String redisKey = redissonCacheUtils.getQrCodeKey(qrCodeId);
        QrConfig qrConfig = new QrConfig(300, 300);
        String base64Data = QrCodeUtil.generateAsBase64(qrCodeId, qrConfig, "png");

        QrCodeStatusVO statusVO = QrCodeStatusVO.builder()
                .qrCodeId(qrCodeId)
                .status(QrCodeStatus.WAITING.getCode())
                .build();
        redissonCacheUtils.set(redisKey, statusVO, 5, TimeUnit.MINUTES);

        return Result.success("二维码生成成功", QrCodeGenerateVO.builder()
                .qrCodeId(qrCodeId)
                .qrCodeBase64(base64Data)
                .build());
    }

    /**
     * 查询二维码状态（前端轮询用）
     * 返回状态和登录token
     */
    @GetMapping("/qrcode/status")
    @Operation(summary = "查询二维码状态", description = "轮询查询二维码扫描状态")
    public Result<QrCodeStatusVO> checkQrCodeStatus(@Parameter(description = "二维码ID") @RequestParam String qrCodeId) {
        String redisKey = redissonCacheUtils.getQrCodeKey(qrCodeId);
        QrCodeStatusVO statusVO = (QrCodeStatusVO) redissonCacheUtils.get(redisKey);

        if (statusVO == null) {
            return Result.fail("二维码不存在或已过期");
        }

        // 检查是否到期
        long ttl = redissonCacheUtils.getRemainingTime(redisKey) != null ? 
                   redissonCacheUtils.getRemainingTime(redisKey) : 0;
        if (ttl <= 0) {
            statusVO.setStatus(QrCodeStatus.EXPIRED.getCode());
            redissonCacheUtils.delete(redisKey);
            return Result.fail("二维码已过期");
        }

        return Result.success(statusVO);
    }

    /**
     * 扫码确认（已登录用户点击确认登录）
     * 将二维码状态更新为已确认，并生成登录token
     */
    @PostMapping("/qrcode/confirm")
    @Operation(summary = "确认扫码登录", description = "已登录用户扫码确认登录")
    public Result<QrCodeStatusVO> confirmQrCodeLogin(@Parameter(description = "二维码ID") @RequestParam String qrCodeId) {
        String redisKey = redissonCacheUtils.getQrCodeKey(qrCodeId);
        QrCodeStatusVO statusVO = (QrCodeStatusVO) redissonCacheUtils.get(redisKey);

        if (statusVO == null) {
            throw new BusinessException("二维码不存在或已过期");
        }

        // 获取当前登录用户信息
        String userId = UserContextUtil.getCurrentUserId();
        if (StrUtil.isBlank(userId)) {
            throw new BusinessException("请先登录");
        }

        // 获取用户详细信息
        UserInfoVO currentUser = UserContextUtil.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException("用户信息获取失败");
        }

        // 更新状态为已确认
        statusVO.setStatus(QrCodeStatus.CONFIRMED.getCode());

        // 为新设备生成 Token
        // 使用固定设备类型 "DESKTOP"，实现同端互斥登录
        // 先踢出该用户在 DESKTOP 端的旧设备，确保只有一个桌面端在线
        String deviceType = "DESKTOP";
        StpUtil.kickout(Long.parseLong(userId), deviceType);
        StpUtil.login(Long.parseLong(userId), deviceType);
        String token = StpUtil.getTokenValue();
        statusVO.setToken(token);

        // 设置用户信息
        statusVO.setUserId(Long.parseLong(currentUser.getId()));
        statusVO.setUsername(currentUser.getUsername());
        statusVO.setNickname(currentUser.getNickname());

        // 重新设置到Redis（保持5分钟过期时间，给客户端时间拉取）
        redissonCacheUtils.set(redisKey, statusVO, 5, TimeUnit.MINUTES);

        log.info("用户[{}]通过二维码[{}]确认登录", userId, qrCodeId);
        return Result.success("确认登录成功", statusVO);
    }

    // ==================== 用户信息相关 ====================
    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息", description = "获取登录用户的基本信息")
    public Result<UserInfoVO> getCurrentUserInfo() {
        UserInfoVO userInfo = chatUserService.getCurrentUserInfo();

        // 把数据库的短路径转换成临时URL返回给前端
        if (StrUtil.isNotBlank(userInfo.getAvatar()) && !userInfo.getAvatar().startsWith("http")) {
            String avatarUrl = minioUtil.getAvatarUrl(userInfo.getAvatar());
            userInfo.setAvatar(avatarUrl);
        }

        return Result.success("获取成功", userInfo);
    }

    @PostMapping("/update")
    @Operation(summary = "更新用户信息", description = "更新当前用户的昵称、头像等个人信息")
    @RequirePermission("user:update")
    public Result<Void> updateUserInfo(@Valid @RequestBody UserUpdateInfoDTO updateInfoDTO) {
        chatUserService.updateUserInfo(updateInfoDTO);
        return Result.success("修改成功");
    }

    @PostMapping("/update-password")
    @Operation(summary = "修改密码", description = "修改当前用户的登录密码")
    @RequirePermission("user:update:password")
    public Result<Void> updatePassword(@Valid @RequestBody UserUpdatePasswordDTO updatePasswordDTO) {
        chatUserService.updatePassword(updatePasswordDTO);
        return Result.success("密码修改成功，请重新登录");
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "刷新Token", description = "刷新JWT访问令牌")
    public Result<SaTokenInfo> refreshToken() {
        SaTokenInfo tokenInfo = chatUserService.refreshToken();
        return Result.success("刷新成功", tokenInfo);
    }

    @PostMapping("/cancel")
    @Operation(summary = "注销账号", description = "注销当前用户的账号")
    @RequirePermission("user:cancel")
    public Result<Void> cancelAccount() {
        chatUserService.cancelAccount();
        return Result.success("账号注销成功");
    }

    @GetMapping("/check-username")
    @Operation(summary = "检查用户名", description = "检查用户名是否已被注册")
    public Result<Boolean> checkUsernameExist(@Parameter(description = "用户名") @RequestParam String username) {
        boolean exist = chatUserService.checkUsernameExist(username);
        return Result.success(exist ? "用户名已存在" : "用户名可用", exist);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索用户", description = "根据关键词搜索用户")
    public Result<UserInfoVO> searchUser(@Parameter(description = "搜索关键词") @RequestParam String keyword) {
        UserInfoVO user = chatUserService.searchUser(keyword);
        if (user != null && StrUtil.isNotBlank(user.getAvatar()) && !user.getAvatar().startsWith("http")) {
            user.setAvatar(minioUtil.getAvatarUrl(user.getAvatar()));
        }
        return Result.success("搜索成功", user);
    }

    // ==================== 头像上传（核心修改：返回短路径+预览URL） ====================
    /**
     * 上传用户头像
     * 限流：每用户每分钟最多5次
     */
    @RateLimit(limit = 5, period = 60, limitType = RateLimit.LimitType.USER, message = "头像上传过于频繁，请稍后再试")
    @PostMapping("/avatar/upload")
    @Operation(summary = "上传头像", description = "上传用户头像图片")
    @lombok.SneakyThrows
    public Result<Map<String, String>> uploadAvatar(@Parameter(description = "头像文件") @RequestParam MultipartFile file) {
        // 1. 安全校验
        if (file.isEmpty()) {
            throw new BusinessException("上传失败：文件为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("上传失败：文件名为空");
        }

        // 2. 使用 FileSecurityUtil 进行安全验证
        fileSecurityUtil.validateAvatarExtension(originalFilename);
        fileSecurityUtil.validateFileSize(file.getSize(), FileSecurityUtil.MAX_AVATAR_SIZE);

        // 3. 验证内容类型
        String contentType = file.getContentType();
        if (contentType != null) {
            fileSecurityUtil.validateContentType(contentType,
                java.util.Set.of("image/jpeg", "image/png", "image/gif", "image/webp"));
        }

        // 4. 获取当前用户ID
        String userId = UserContextUtil.getCurrentUserId();

        // 5. 调用工具类上传，获取短路径
        String filePath = minioUtil.uploadAvatar(file, userId);

        // 6. 生成预览URL
        String previewUrl = minioUtil.getAvatarUrl(filePath);

        // 7. 更新数据库（只存短路径）
        UserUpdateInfoDTO userUpdateInfoDTO = new UserUpdateInfoDTO();
        userUpdateInfoDTO.setAvatar(filePath);
        chatUserService.updateUserInfo(userUpdateInfoDTO);

        // 核心修改：返回对象包含短路径和预览URL
        Map<String, String> result = new HashMap<>();
        result.put("filePath", filePath);    // 短路径（用于后续更新）
        result.put("previewUrl", previewUrl); // 预览URL（用于前端显示）
        return Result.success("上传成功", result);
    }
}
