package com.maxxvll.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.dto.*;
import com.maxxvll.common.enums.QrCodeStatus;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.common.vo.*;
import com.maxxvll.service.ChatUserService;
import com.maxxvll.utils.MinioUtil;
import com.maxxvll.utils.RedissonCacheUtil;
import com.maxxvll.utils.UserContextUtil;
import com.wf.captcha.SpecCaptcha;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
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
public class UserController extends BaseController {
    @Resource
    private ChatUserService chatUserService;
    @Resource
    private RedissonCacheUtil redissonCacheUtils;
    @Resource
    private MinioUtil minioUtil;
    @Resource
    private JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String mailFrom;

    // ==================== 登录相关 ====================
    @PostMapping("/login")
    public Result<String> login(@RequestBody UserLoginDTO userLoginDTO) {
        String token = chatUserService.login(userLoginDTO);
        return Result.success("登录成功", token);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        Object loginId = tokenInfo.getLoginId();
        String loginDeviceType = tokenInfo.getLoginDeviceType();
        StpUtil.logout(loginId, loginDeviceType);

        log.info("用户[{}]的[{}]端已登出", loginId, loginDeviceType == null ? "默认" : loginDeviceType);
        return Result.success("登出成功：已下线" + (loginDeviceType == null ? "默认" : loginDeviceType) + "端");
    }

    // ==================== 注册相关 ====================
    /**
     * 发送邮箱验证码，60s内不允许重复发送，验证码有效期5分钟
     */
    @PostMapping("/sendEmailCode")
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
        // 发送邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject("注册验证码");
            message.setText("您好！\n\n您的注册验证码为：" + code + "\n\n验证码有效期5分钟，请勿泄露给他人。\n\n如非本人操作，请忽略此邮件。");
            mailSender.send(message);
            log.info("邮箱验证码已发送，email: {}", email);
        } catch (Exception e) {
            // 删除Redis Key，避免发送失败后用户无法重试
            redissonCacheUtils.delete(redisKey);
            log.error("邮箱验证码发送失败，email: {}", email, e);
            throw new BusinessException("验证码发送失败，请检查邮箱地址是否正确");
        }
        return Result.success("验证码已发送，请查收邮箱");
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody UserRegisterDTO userRegisterDTO) {
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
    public Result<QrCodeStatusVO> checkQrCodeStatus(@RequestParam String qrCodeId) {
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
    public Result<QrCodeStatusVO> confirmQrCodeLogin(@RequestParam String qrCodeId) {
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

        // 更新状态为已确认
        statusVO.setStatus(QrCodeStatus.CONFIRMED.getCode());
        
        // 生成登录token（使用Sa-Token框架的logout后再login）
        String token = StpUtil.getTokenValue();
        statusVO.setToken(token);

        // 重新设置到Redis（保持5分钟过期时间，给客户端时间拉取）
        redissonCacheUtils.set(redisKey, statusVO, 5, TimeUnit.MINUTES);

        log.info("用户[{}]通过二维码[{}]确认登录", userId, qrCodeId);
        return Result.success("确认登录成功", statusVO);
    }

    // ==================== 用户信息相关 ====================
    @GetMapping("/info")
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
    public Result<Void> updateUserInfo(@RequestBody UserUpdateInfoDTO updateInfoDTO) {
        chatUserService.updateUserInfo(updateInfoDTO);
        return Result.success("修改成功");
    }

    @PostMapping("/update-password")
    public Result<Void> updatePassword(@RequestBody UserUpdatePasswordDTO updatePasswordDTO) {
        chatUserService.updatePassword(updatePasswordDTO);
        return Result.success("密码修改成功，请重新登录");
    }

    @PostMapping("/refresh-token")
    public Result<SaTokenInfo> refreshToken() {
        SaTokenInfo tokenInfo = chatUserService.refreshToken();
        return Result.success("刷新成功", tokenInfo);
    }

    @PostMapping("/cancel")
    public Result<Void> cancelAccount() {
        chatUserService.cancelAccount();
        return Result.success("账号注销成功");
    }

    @GetMapping("/check-username")
    public Result<Boolean> checkUsernameExist(@RequestParam String username) {
        boolean exist = chatUserService.checkUsernameExist(username);
        return Result.success(exist ? "用户名已存在" : "用户名可用", exist);
    }

    @GetMapping("/search")
    public Result<UserInfoVO> searchUser(@RequestParam String keyword) {
        UserInfoVO user = chatUserService.searchUser(keyword);
        if (user != null && StrUtil.isNotBlank(user.getAvatar()) && !user.getAvatar().startsWith("http")) {
            user.setAvatar(minioUtil.getAvatarUrl(user.getAvatar()));
        }
        return Result.success("搜索成功", user);
    }

    // ==================== 头像上传（核心修改：返回短路径+预览URL） ====================
    @SneakyThrows
    @PostMapping("/avatar/upload")
    public Result<Map<String, String>> uploadAvatar(@RequestParam MultipartFile file) {
        // 1. 校验
        if (file.isEmpty()) throw new BusinessException("上传失败：文件为空");
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) throw new BusinessException("上传失败：文件名为空");
        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!suffix.matches("\\.(jpg|jpeg|png|gif)$")) throw new BusinessException("上传失败：文件格式不支持");

        // 2. 获取当前用户ID
        String userId = UserContextUtil.getCurrentUserId();

        // 3. 调用工具类上传，获取短路径
        String filePath = minioUtil.uploadAvatar(file, userId);

        // 4. 生成预览URL
        String previewUrl = minioUtil.getAvatarUrl(filePath);

        // 5. 更新数据库（只存短路径）
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