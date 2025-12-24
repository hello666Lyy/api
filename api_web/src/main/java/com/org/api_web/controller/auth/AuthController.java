package com.org.api_web.controller.auth;

import com.org.api_common.constant.ErrorCodeEnum;
import com.org.api_common.dto.LoginDTO;
import com.org.api_common.dto.RegisterDTO;
import com.org.api_common.exception.BusinessException;
import com.org.api_common.result.Result;
import com.org.api_common.vo.LoginResultVO;
import com.org.api_service.service.AuthService;
import com.org.api_web.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器（登录/登出/用户信息）
 * @author zhangzhenhui
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<LoginResultVO> register(@RequestBody RegisterDTO registerDTO) {
        LoginResultVO result = authService.register(registerDTO.getUsername(), registerDTO.getPassword());
        return Result.success(result, "注册成功！请妥善保存您的AccessKey和SecretKey");
    }

    /**
     * 用户登录（用户名+密码）
     */
    @PostMapping("/login")
    public Result<LoginResultVO> login(@RequestBody LoginDTO loginDTO) {
        LoginResultVO result = authService.login(loginDTO.getUsername(), loginDTO.getPassword());
        return Result.success(result, "登录成功");
    }

    /**
     * 获取当前登录用户信息（需要Token）
     */
    @GetMapping("/userInfo")
    public Result<LoginResultVO> getUserInfo() {
        Long userId = JwtInterceptor.getCurrentUserId();
        LoginResultVO result = authService.getCurrentUserInfo(userId);
        return Result.success(result);
    }

    /**
     * 退出登录（前端清除Token即可，后端可做Token黑名单）
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        // 可选：将当前Token加入Redis黑名单
        return Result.success(null, "退出成功");
    }

    /**
     * 修改密码（需要Token）
     */
    @PostMapping("/changePassword")
    public Result<Void> changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.TOKEN_MISSING, "Token中未找到用户ID");
        }
        
        authService.changePassword(userId, oldPassword, newPassword);
        return Result.success(null, "密码修改成功");
    }
}