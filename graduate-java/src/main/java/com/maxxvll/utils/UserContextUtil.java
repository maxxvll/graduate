package com.maxxvll.utils;

import cn.dev33.satoken.stp.StpUtil;
import com.maxxvll.common.vo.UserInfoVO;

public class UserContextUtil {
    private static final String USER_INFO_KEY="userInfo";
    public static String getCurrentUserId(){
        return StpUtil.getLoginId().toString();
    }
    public static UserInfoVO getCurrentUser(){
        Object loginId = StpUtil.getLoginId();
        return (UserInfoVO) StpUtil.getSessionByLoginId(loginId).get(USER_INFO_KEY);
    }
    public static void setCurrentUser(UserInfoVO userInfoVO){
        StpUtil.getSessionByLoginId(userInfoVO.getId()).set(USER_INFO_KEY,userInfoVO);
    }
    public static void refreshCurrentUser(UserInfoVO userInfoVO){
        setCurrentUser(userInfoVO);
    }
}
