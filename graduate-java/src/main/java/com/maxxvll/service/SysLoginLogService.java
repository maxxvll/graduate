package com.maxxvll.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxxvll.domain.SysLoginLog;

/**
 * 登录日志服务接口
 *
 * @author Claude Code
 * @since 2026-03-31
 */
public interface SysLoginLogService extends IService<SysLoginLog> {

    /**
     * 异步保存登录日志
     *
     * @param loginLog 登录日志
     */
    void saveAsync(SysLoginLog loginLog);

    /**
     * 同步保存登录日志
     *
     * @param loginLog 登录日志
     * @return 是否保存成功
     */
    boolean saveSync(SysLoginLog loginLog);

    /**
     * 记录登录成功
     *
     * @param username 用户名
     * @param userId 用户ID
     * @param loginType 登录方式
     * @param clientIp 客户端IP
     * @param deviceType 设备类型
     * @param userAgent 用户代理
     */
    void recordLoginSuccess(String username, String userId, String loginType,
                           String clientIp, String deviceType, String userAgent);

    /**
     * 记录登录失败
     *
     * @param username 用户名
     * @param loginType 登录方式
     * @param clientIp 客户端IP
     * @param failReason 失败原因
     * @param deviceType 设备类型
     * @param userAgent 用户代理
     */
    void recordLoginFail(String username, String loginType, String clientIp,
                        String failReason, String deviceType, String userAgent);

    /**
     * 检查是否异地登录
     *
     * @param userId 用户ID
     * @param clientIp 当前IP
     * @return 是否为异地登录
     */
    boolean isCrossLocationLogin(String userId, String clientIp);
}
