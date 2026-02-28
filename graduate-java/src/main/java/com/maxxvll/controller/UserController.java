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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
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
    @PostMapping("/register")
    public Result<Void> register(@RequestBody UserRegisterDTO userRegisterDTO) {
        String captchaKey = redissonCacheUtils.getCaptchaKey(userRegisterDTO.getCaptchaKey());
        String captchaCode = redissonCacheUtils.get(captchaKey);

        if (captchaCode == null) {
            throw new BusinessException("验证码已过期");
        }
        if (!captchaCode.equalsIgnoreCase(userRegisterDTO.getCaptchaCode())) {
            throw new BusinessException("验证码错误");
        }

        redissonCacheUtils.delete(captchaKey);
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