package com.org.api_service.service.serviceImpl;

import com.org.api_common.constant.ErrorCodeEnum;
import com.org.api_common.constant.SignConstants;
import com.org.api_common.entity.SysUser;
import com.org.api_common.exception.BusinessException;
import com.org.api_common.util.JwtUtil;
import com.org.api_common.vo.LoginResultVO;
import com.org.api_service.service.AuthService;
import com.org.api_service.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 认证服务实现
 * @author zhangzhenhui
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SysUserService sysUserService;

    // 随机字符池（生成AK/SK用）
    private static final String RANDOM_CHAR_POOL = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final Random random = new Random();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResultVO register(String username, String password) {
        // 1. 校验用户名是否已存在
        long count = sysUserService.lambdaQuery()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleteStatus, 0)
                .count();
        if (count > 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "用户名已存在");
        }

        // 2. 生成唯一AK（16位）
        String accessKey;
        do {
            accessKey = generateRandomStr(SignConstants.AK_LENGTH);
        } while (sysUserService.lambdaQuery()
                .eq(SysUser::getAccessKey, accessKey)
                .count() > 0);

        // 3. 生成SK（32位）
        String secretKey = generateRandomStr(SignConstants.SK_LENGTH);

        // 4. 创建用户
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(password); // 不加密，明文存储
        user.setAccessKey(accessKey);
        user.setSecretKey(secretKey);
        user.setStatus(1); // 默认启用
        user.setPermissionType(1); // 默认只读权限
        user.setDeleteStatus(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        
        sysUserService.save(user);

        // 5. 生成JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("accessKey", user.getAccessKey());
        claims.put("permissionType", user.getPermissionType());
        String token = JwtUtil.generateToken(claims);

        // 6. 构建返回结果（包含AK/SK，SK仅返回一次）
        LoginResultVO result = new LoginResultVO();
        result.setToken(token);
        result.setUserId(user.getId());
        result.setUsername(user.getUsername());
        result.setAccessKey(user.getAccessKey());
        result.setSecretKey(secretKey); // 注册时返回SK，仅此一次
        result.setPermissionType(user.getPermissionType());
        result.setExpireTime(System.currentTimeMillis() + 2 * 60 * 60 * 1000L);

        return result;
    }

    /**
     * 辅助方法：生成指定长度的随机字符串
     */
    private String generateRandomStr(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM_CHAR_POOL.charAt(random.nextInt(RANDOM_CHAR_POOL.length())));
        }
        return sb.toString();
    }

    @Override
    public LoginResultVO login(String username, String password) {
        // 1. 根据用户名查询用户
        SysUser user = sysUserService.lambdaQuery()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleteStatus, 0)
                .one();

        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_FAILED);
        }

        // 2. 校验密码（生产环境应使用 BCrypt）
        if (!password.equals(user.getPassword())) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_FAILED);
        }

        // 3. 检查用户状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN, "账号已被禁用");
        }

        // 4. 检查必要字段
        if (user.getAccessKey() == null || user.getAccessKey().isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR, "用户AccessKey不存在，请联系管理员");
        }

        // 5. 生成 JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("accessKey", user.getAccessKey());
        claims.put("permissionType", user.getPermissionType() != null ? user.getPermissionType() : 1); // 默认权限类型为1（只读）
        String token = JwtUtil.generateToken(claims);

        // 6. 构建返回结果
        LoginResultVO result = new LoginResultVO();
        result.setToken(token);
        result.setUserId(user.getId());
        result.setUsername(user.getUsername());
        result.setAccessKey(user.getAccessKey());
        result.setPermissionType(user.getPermissionType() != null ? user.getPermissionType() : 1); // 默认权限类型为1（只读）
        result.setExpireTime(System.currentTimeMillis() + 2 * 60 * 60 * 1000L);

        return result;
    }

    @Override
    public LoginResultVO getCurrentUserInfo(Long userId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "用户不存在");
        }

        LoginResultVO result = new LoginResultVO();
        result.setUserId(user.getId());
        result.setUsername(user.getUsername());
        result.setAccessKey(user.getAccessKey());
        result.setPermissionType(user.getPermissionType());
        return result;
    }

    @Override
    public String refreshToken(String oldToken) {
        // 解析旧Token，重新生成新Token
        Long userId = JwtUtil.getUserIdFromToken(oldToken);
        SysUser user = sysUserService.getById(userId);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("accessKey", user.getAccessKey());
        claims.put("permissionType", user.getPermissionType());

        return JwtUtil.generateToken(claims);
    }

    @Override
    public void logout(String token) {
        // 可选：将Token加入Redis黑名单
        // redisUtil.set("token:blacklist:" + token, "1", 2 * 60 * 60);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        // 1. 查询用户
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "用户不存在");
        }

        // 2. 校验原密码
        if (!oldPassword.equals(user.getPassword())) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_FAILED, "原密码错误");
        }

        // 3. 更新密码
        user.setPassword(newPassword);
        sysUserService.updateById(user);
    }
}