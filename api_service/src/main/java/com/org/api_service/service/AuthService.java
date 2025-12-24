package com.org.api_service.service;

import com.org.api_common.vo.LoginResultVO;

/**
 * 认证服务接口（登录/登出/Token管理）
 * @author zhangzhenhui
 */
public interface AuthService {

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录结果（含Token）
     */
    LoginResultVO login(String username, String password);

    /**
     * 获取当前登录用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    LoginResultVO getCurrentUserInfo(Long userId);

    /**
     * 刷新Token（可选）
     * @param oldToken 旧Token
     * @return 新Token
     */
    String refreshToken(String oldToken);

    /**
     * 退出登录（可选：Token加入黑名单）
     * @param token 当前Token
     */
    void logout(String token);

    /**
     * 修改密码
     * @param userId 用户ID
     * @param oldPassword 原密码
     * @param newPassword 新密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码
     * @return 注册结果（含Token和AK/SK）
     */
    LoginResultVO register(String username, String password);
}