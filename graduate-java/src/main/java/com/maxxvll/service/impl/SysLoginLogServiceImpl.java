package com.maxxvll.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.domain.SysLoginLog;
import com.maxxvll.mapper.SysLoginLogMapper;
import com.maxxvll.service.SysLoginLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录日志服务实现
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Slf4j
@Service
public class SysLoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog>
        implements SysLoginLogService {

    @Resource
    private SysLoginLogMapper sysLoginLogMapper;

    /**
     * 异地登录检测时间窗口（小时）
     */
    private static final int CROSS_LOCATION_WINDOW_HOURS = 24;

    @Override
    @Async
    public void saveAsync(SysLoginLog loginLog) {
        try {
            sysLoginLogMapper.insert(loginLog);
            log.debug("异步保存登录日志成功: username={}, loginStatus={}",
                    loginLog.getUsername(), loginLog.getLoginStatus());
        } catch (Exception e) {
            log.error("异步保存登录日志失败: username={}, error={}",
                    loginLog.getUsername(), e.getMessage(), e);
        }
    }

    @Override
    public boolean saveSync(SysLoginLog loginLog) {
        try {
            return sysLoginLogMapper.insert(loginLog) > 0;
        } catch (Exception e) {
            log.error("同步保存登录日志失败: username={}, error={}",
                    loginLog.getUsername(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void recordLoginSuccess(String username, String userId, String loginType,
                                   String clientIp, String deviceType, String userAgent) {
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setUserId(userId);
        loginLog.setUsername(username);
        loginLog.setLoginType(loginType);
        loginLog.setClientIp(clientIp);
        loginLog.setDeviceType(deviceType);
        loginLog.setUserAgent(userAgent);
        loginLog.setLoginStatus(true);
        loginLog.setLoginTime(LocalDateTime.now());

        // 解析浏览器和操作系统
        parseUserAgent(userAgent, loginLog);

        // 检查异地登录
        if (isCrossLocationLogin(userId, clientIp)) {
            loginLog.setIsSuspicious(true);
            loginLog.setSuspiciousReason("检测到异地登录");
            log.warn("[安全告警] 用户 {} 从新IP {} 登录，可能存在账号风险", username, clientIp);
        } else {
            loginLog.setIsSuspicious(false);
        }

        saveAsync(loginLog);
    }

    @Override
    public void recordLoginFail(String username, String loginType, String clientIp,
                                String failReason, String deviceType, String userAgent) {
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setUsername(username);
        loginLog.setLoginType(loginType);
        loginLog.setClientIp(clientIp);
        loginLog.setFailReason(failReason);
        loginLog.setDeviceType(deviceType);
        loginLog.setUserAgent(userAgent);
        loginLog.setLoginStatus(false);
        loginLog.setLoginTime(LocalDateTime.now());
        loginLog.setIsSuspicious(false);

        // 解析浏览器和操作系统
        parseUserAgent(userAgent, loginLog);

        // 连续登录失败检测
        checkLoginFailures(username, clientIp, loginLog);

        saveAsync(loginLog);
    }

    @Override
    public boolean isCrossLocationLogin(String userId, String clientIp) {
        // 查询最近一次登录的IP
        QueryWrapper<SysLoginLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("login_status", true)
                .orderByDesc("login_time")
                .last("LIMIT 1");

        SysLoginLog lastLogin = sysLoginLogMapper.selectOne(queryWrapper);

        if (lastLogin == null) {
            // 首次登录，不算异地
            return false;
        }

        // 检查时间窗口（24小时内）
        LocalDateTime windowStart = LocalDateTime.now().minusHours(CROSS_LOCATION_WINDOW_HOURS);
        if (lastLogin.getLoginTime().isBefore(windowStart)) {
            // 超过时间窗口，重新登录记录不算异地
            return false;
        }

        // 比较IP
        String lastIp = lastLogin.getClientIp();
        if (lastIp != null && clientIp != null && !lastIp.equals(clientIp)) {
            // 检查IP段是否相同（简单判断）
            String[] lastIpParts = lastIp.split("\\.");
            String[] currentIpParts = clientIp.split("\\.");

            if (lastIpParts.length >= 2 && currentIpParts.length >= 2) {
                // 如果前两段相同，认为是同一地区
                if (lastIpParts[0].equals(currentIpParts[0]) &&
                    lastIpParts[1].equals(currentIpParts[1])) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    /**
     * 解析UserAgent，提取浏览器和操作系统信息
     */
    private void parseUserAgent(String userAgent, SysLoginLog loginLog) {
        if (userAgent == null) {
            return;
        }

        // 简单解析浏览器
        if (userAgent.contains("Chrome")) {
            loginLog.setBrowser("Chrome");
        } else if (userAgent.contains("Firefox")) {
            loginLog.setBrowser("Firefox");
        } else if (userAgent.contains("Safari")) {
            loginLog.setBrowser("Safari");
        } else if (userAgent.contains("Edge")) {
            loginLog.setBrowser("Edge");
        } else {
            loginLog.setBrowser("Unknown");
        }

        // 简单解析操作系统
        if (userAgent.contains("Windows")) {
            loginLog.setOs("Windows");
        } else if (userAgent.contains("Mac OS")) {
            loginLog.setOs("macOS");
        } else if (userAgent.contains("Linux")) {
            loginLog.setOs("Linux");
        } else if (userAgent.contains("Android")) {
            loginLog.setOs("Android");
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            loginLog.setOs("iOS");
        } else {
            loginLog.setOs("Unknown");
        }
    }

    /**
     * 检查登录失败情况
     */
    private void checkLoginFailures(String username, String clientIp, SysLoginLog loginLog) {
        // 查询最近5分钟内同一IP的登录失败次数
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(5);

        QueryWrapper<SysLoginLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username)
                .eq("client_ip", clientIp)
                .eq("login_status", false)
                .ge("login_time", windowStart);

        long failCount = sysLoginLogMapper.selectCount(queryWrapper);

        if (failCount >= 3) {
            loginLog.setIsSuspicious(true);
            loginLog.setSuspiciousReason(String.format("5分钟内连续登录失败%d次", failCount + 1));
            log.warn("[安全告警] 用户 {} 从IP {} 连续登录失败{}次，可能存在暴力破解",
                    username, clientIp, failCount + 1);
        }
    }
}
