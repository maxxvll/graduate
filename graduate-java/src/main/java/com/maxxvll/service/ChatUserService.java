package com.maxxvll.service;

import cn.dev33.satoken.stp.SaTokenInfo;
import com.maxxvll.common.dto.UserLoginDTO;
import com.maxxvll.common.dto.UserRegisterDTO;
import com.maxxvll.common.dto.UserUpdateInfoDTO;
import com.maxxvll.common.dto.UserUpdatePasswordDTO;
import com.maxxvll.common.vo.UserInfoVO;
import com.maxxvll.domain.ChatUser;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.bind.annotation.RequestBody;

/**
* @author 20570
* @description 针对表【chat_user(用户基础信息表)】的数据库操作Service
* @createDate 2026-02-19 12:02:21
*/
public interface ChatUserService extends IService<ChatUser> {
    String login(UserLoginDTO userLoginDTO);
    void register(UserRegisterDTO userRegisterDTO);
    UserInfoVO getCurrentUserInfo();
    void updateUserInfo(UserUpdateInfoDTO updateInfoDTO);
    void updatePassword(UserUpdatePasswordDTO updatePasswordDTO);
    SaTokenInfo refreshToken();
    void cancelAccount();
    boolean checkUsernameExist(String username);

    /**
     * 根据用户名或手机号搜索用户（用于添加好友）
     * @param keyword 关键字（用户名或手机号）
     * @return 用户信息，未找到返回null
     */
    UserInfoVO searchUser(String keyword);
}
