package com.maxxvll.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.common.LoginEventProducer;
import com.maxxvll.common.dto.UserLoginDTO;
import com.maxxvll.common.dto.UserRegisterDTO;
import com.maxxvll.common.dto.UserUpdateInfoDTO;
import com.maxxvll.common.dto.UserUpdatePasswordDTO;
import com.maxxvll.common.event.LoginEvent;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.common.vo.UserInfoVO;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.service.ChatUserService;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.utils.BeanConvertUtil;
import com.maxxvll.utils.RedissonCacheUtil;
import com.maxxvll.utils.UserContextUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
* @author 20570
* @description 针对表【chat_user(用户基础信息表)】的数据库操作Service实现
* @createDate 2026-02-19 12:02:21
*/
@Service
@Slf4j
public class ChatUserServiceImpl extends ServiceImpl<ChatUserMapper, ChatUser>
    implements ChatUserService{
    @Resource
    private ChatUserMapper chatUserMapper;
    @Resource
    private LoginEventProducer loginEventProducer;
    @Resource
    private RedissonCacheUtil redissonCacheUtils;
    @Override
    public String login(UserLoginDTO userLoginDTO) {
        String username = userLoginDTO.getUsername();
        String lockKey = redissonCacheUtils.getLoginLockKey(username);
        if(redissonCacheUtils.exists(lockKey)){
            Long remainTime = redissonCacheUtils.getRemainingTime(lockKey);
            throw new BusinessException("账号已锁定，请" + remainTime + "秒后重试");
        }
        ChatUser chatUser = chatUserMapper.selectOne(
                new LambdaQueryWrapper<ChatUser>()
                        .eq(ChatUser::getUsername, userLoginDTO.getUsername())
        );
        if (chatUser == null) {
            handleLoginFail(username);
            throw new BusinessException("用户名或密码错误");
        }
        if(chatUser.getStatus()!=1){
            throw new BusinessException("用户已被禁用");
        }
        if(!BCrypt.checkpw(userLoginDTO.getPassword(),chatUser.getPassword())){
            handleLoginFail(username);
            throw new BusinessException("用户名或密码错误");
        }
        clearLoginLimit(username);
        StpUtil.login(chatUser.getId(),userLoginDTO.getDeviceType());
        UserInfoVO userInfoVO = BeanConvertUtil.convert(chatUser, UserInfoVO.class);
        UserContextUtil.setCurrentUser(userInfoVO);
        loginEventProducer.sendLoginEvent(new LoginEvent(Long.valueOf(chatUser.getId()), chatUser.getUsername(), LocalDateTime.now(),"111"));
        return StpUtil.getTokenValue();
    }

    @Override
    public void register(UserRegisterDTO userRegisterDTO) {
        // 1. 校验确认密码
        if (!userRegisterDTO.getPassword().equals(userRegisterDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }

        // 2. 检查用户名是否已存在
        Long count = chatUserMapper.selectCount(
                new LambdaQueryWrapper<ChatUser>()
                        .eq(ChatUser::getUsername, userRegisterDTO.getUsername())
        );
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 3. 使用 Sa-Token 内置 BCrypt 加密密码
        String hashedPassword = BCrypt.hashpw(userRegisterDTO.getPassword(), BCrypt.gensalt());

        // 4. 构建用户对象并入库
        ChatUser chatUser = new ChatUser();
        chatUser.setUsername(userRegisterDTO.getUsername());
        chatUser.setPassword(hashedPassword); // 存加密后的密码
        chatUser.setNickname(userRegisterDTO.getNickname() != null ? userRegisterDTO.getNickname() : userRegisterDTO.getUsername());
        chatUser.setStatus(1);

        chatUserMapper.insert(chatUser);
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        return UserContextUtil.getCurrentUser();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(UserUpdateInfoDTO updateInfoDTO) {
        String loginId = UserContextUtil.getCurrentUserId();
        ChatUser chatUser = this.getById(loginId);
        if (chatUser == null) {
            throw new BusinessException("用户不存在");
        }

        // 核心修复：处理头像字段，过滤长URL，只保留MinIO短路径
        String avatar = updateInfoDTO.getAvatar();
        if (StrUtil.isNotBlank(avatar)) {
            // 如果是HTTP开头的长URL，截取MinIO短路径
            if (avatar.startsWith("http")) {
                // 截取avatar/开头的部分（适配MinIO路径格式）
                int avatarIndex = avatar.indexOf("avatar/");
                if (avatarIndex > 0) {
                    avatar = avatar.substring(avatarIndex);
                } else {
                    // 兜底：如果没有avatar前缀，使用原有数据库值
                    avatar = chatUser.getAvatar();
                }
            }
            // 二次兜底：限制长度（数据库字段建议VARCHAR(500)）
            if (avatar.length() > 500) {
                avatar = avatar.substring(0, 500);
            }
        } else {
            // 如果前端没传头像，沿用原有值
            avatar = chatUser.getAvatar();
        }

        // 组装更新对象
        ChatUser updateUser = new ChatUser();
        updateUser.setId(loginId);
        updateUser.setNickname(StrUtil.isBlank(updateInfoDTO.getNickname()) ? chatUser.getNickname() : updateInfoDTO.getNickname());
        updateUser.setAvatar(avatar); // 只存短路径
        updateUser.setPhone(StrUtil.isBlank(updateInfoDTO.getPhone()) ? chatUser.getPhone() : updateInfoDTO.getPhone());
        updateUser.setEmail(StrUtil.isBlank(updateInfoDTO.getEmail()) ? chatUser.getEmail() : updateInfoDTO.getEmail());
        updateUser.setExtInfo(updateInfoDTO.getExtInfo()); // 扩展信息
        updateUser.setUpdatedAt(new Date());

        // 更新数据库
        this.updateById(updateUser);
        log.info("用户信息更新成功，userId: {}, 头像路径: {}", loginId, avatar);
    }
    @Override
    public void updatePassword(UserUpdatePasswordDTO updatePasswordDTO) {
        if(!updatePasswordDTO.getNewPassword().equals(updatePasswordDTO.getOldPassword())){
            throw new BusinessException("两次输入的密码不一致");
        }
        String loginId = UserContextUtil.getCurrentUserId();
        ChatUser chatUser = chatUserMapper.selectById(loginId);
        if(!BCrypt.checkpw(updatePasswordDTO.getOldPassword(), chatUser.getPassword())){
            throw new BusinessException("原密码不正确");
        }
        chatUser.setPassword(BCrypt.hashpw(updatePasswordDTO.getNewPassword(), BCrypt.gensalt()));
        chatUserMapper.updateById(chatUser);
        StpUtil.logout(loginId);
    }

    @Override
    public SaTokenInfo refreshToken() {
        StpUtil.renewTimeout(-1);
        return StpUtil.getTokenInfo();
    }

    @Override
    public void cancelAccount() {
        String loginId = UserContextUtil.getCurrentUserId();

        // 逻辑删除
        ChatUser chatUser = new ChatUser();
        chatUser.setId(loginId);
        chatUser.setStatus(3);
        chatUserMapper.updateById(chatUser);

        // 清理会话并下线
        StpUtil.logout(loginId);
    }

    @Override
    public boolean checkUsernameExist(String username) {
        Long count = chatUserMapper.selectCount(
                new LambdaQueryWrapper<ChatUser>()
                        .eq(ChatUser::getUsername, username)
        );
        return count > 0;
    }

    @Override
    public UserInfoVO searchUser(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return null;
        }
        // 屏蔽当前用户自己
        String currentUserId = UserContextUtil.getCurrentUserId();
        ChatUser user = chatUserMapper.selectOne(
                new LambdaQueryWrapper<ChatUser>()
                        .and(w -> w.eq(ChatUser::getUsername, keyword)
                                .or()
                                .eq(ChatUser::getPhone, keyword))
                        .ne(ChatUser::getId, currentUserId)
                        .eq(ChatUser::getStatus, 1)
        );
        if (user == null) {
            return null;
        }
        return BeanConvertUtil.convert(user, UserInfoVO.class);
    }
    private void handleLoginFail(String username) {
        String failKey = redissonCacheUtils.getLoginFailKey(username);
        String lockKey = redissonCacheUtils.getLoginLockKey(username);
        Integer failCount = redissonCacheUtils.get(failKey);
        if (failCount == null) {
            failCount = 0;
        }
        failCount++;
        redissonCacheUtils.set(failKey, failCount, 1, TimeUnit.HOURS);
        if (failCount >= 3) {
            redissonCacheUtils.set(lockKey, true, 10, TimeUnit.MINUTES);
        }
        long lockTime = 0;
        if (failCount >= 15) {
            lockTime = 30 * 60; // 30分钟
        } else if (failCount >= 10) {
            lockTime = 5 * 60; // 5分钟
        } else if (failCount >= 5) {
            lockTime = 60; // 1分钟
        }
        if (lockTime > 0) {
            redissonCacheUtils.set(lockKey, "1", lockTime, TimeUnit.SECONDS);
            throw new BusinessException("账号已锁定，请" + lockTime + "秒后重试");
        }
        int remainTry=5-(failCount%5);
        if(remainTry>0 && remainTry<5){
            throw new BusinessException("密码错误，还剩 " + remainTry + " 次尝试机会");
        }
        throw new BusinessException("密码错误");

    }
    private void clearLoginLimit(String username){
        String failKey = redissonCacheUtils.getLoginFailKey(username);
        String lockKey = redissonCacheUtils.getLoginLockKey(username);
        redissonCacheUtils.delete(failKey);
        redissonCacheUtils.delete(lockKey);
    }
}




