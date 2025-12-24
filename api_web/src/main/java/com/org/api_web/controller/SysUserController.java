package com.org.api_web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.org.api_common.constant.ErrorCodeEnum;
import com.org.api_common.entity.SysUser;
import com.org.api_common.exception.BusinessException;
import com.org.api_common.result.Result;
import com.org.api_common.util.JwtUtil;
import com.org.api_common.vo.*;
import com.org.api_service.service.AkOperateLogService;
import com.org.api_service.service.SysUserService;
import com.org.api_web.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户AK/SK校验接口（api_web模块）
 */
@RestController
@RequestMapping("/api/user")
public class SysUserController {

    @Resource
    private SysUserService sysUserService;
    @Autowired
    private AkOperateLogService akOperateLogService;

    /**
     * AK/SK校验接口（供调用方前置校验）
     */
    @PostMapping("/verifyAkSk")
    public Result<Boolean> verifyAkSk(
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce
    ) {
        boolean result = sysUserService.verifyAkSk(accessKey, sign, timestamp, nonce);
        return Result.success(result);
    }

    // 2. 获取用户信息接口（新增）
    @GetMapping("/getUserInfo")
    public Result<UserInfoVO> getUserInfo(
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce
    ) {
        // 先执行AK/SK鉴权，失败直接抛异常
        sysUserService.verifyAkSk(accessKey, sign, timestamp, nonce);
        // 鉴权通过后返回用户信息VO
        UserInfoVO userInfoVO = sysUserService.getUserInfoVO(accessKey);
        return Result.success(userInfoVO);
    }

    // 新增：生成新AK/SK接口（需要AK/SK签名验证）
    @PostMapping("/generateNewAkSk")
    public Result<Map<String, String>> generateNewAkSk(
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce
    ) {
        // 1. 先执行AK/SK鉴权
        sysUserService.verifyAkSk(accessKey, sign, timestamp, nonce);
        // 2. 生成新AK/SK
        Map<String, String> newKeys = sysUserService.generateNewAkSk(accessKey);
        
        // 3. 查询用户信息（获取更新后的AccessKey和用户ID）
        SysUser user = sysUserService.getByAccessKey(newKeys.get("newAccessKey"));
        if (user != null) {
            // 4. 生成新Token（包含新的AccessKey）
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("username", user.getUsername());
            claims.put("accessKey", user.getAccessKey());
            claims.put("permissionType", user.getPermissionType());
            String newToken = JwtUtil.generateToken(claims);
            
            // 5. 将新Token添加到返回结果中
            newKeys.put("newToken", newToken);
        }
        
        return Result.success(newKeys);
    }

    // 新增：通过Token+密码生成新AK/SK接口（适用于忘记SecretKey的情况）
    @PostMapping("/generateNewAkSkByToken")
    public Result<Map<String, String>> generateNewAkSkByToken(@RequestParam String password) {
        // 1. 从Token中获取当前用户ID和AccessKey（JwtInterceptor已验证Token）
        Long userId = JwtInterceptor.getCurrentUserId();
        String accessKey = JwtInterceptor.getCurrentAccessKey();
        
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.TOKEN_MISSING, "Token中未找到用户ID");
        }
        if (accessKey == null) {
            throw new BusinessException(ErrorCodeEnum.TOKEN_MISSING, "Token中未找到AccessKey");
        }
        
        // 2. 验证密码（通过userId查询用户并校验密码）
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "用户不存在");
        }
        
        // 3. 校验密码（与登录逻辑一致）
        if (!password.equals(user.getPassword())) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_FAILED, "密码错误");
        }
        
        // 4. 密码验证通过后，生成新AK/SK
        Map<String, String> newKeys = sysUserService.generateNewAkSk(accessKey);
        
        // 5. 重新查询用户信息（获取更新后的AccessKey）
        SysUser updatedUser = sysUserService.getById(userId);
        
        // 6. 生成新Token（包含新的AccessKey）
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", updatedUser.getId());
        claims.put("username", updatedUser.getUsername());
        claims.put("accessKey", updatedUser.getAccessKey());
        claims.put("permissionType", updatedUser.getPermissionType());
        String newToken = JwtUtil.generateToken(claims);
        
        // 7. 将新Token添加到返回结果中
        Map<String, String> result = new HashMap<>(newKeys);
        result.put("newToken", newToken);
        
        return Result.success(result, "新AK/SK生成成功，请妥善保存SecretKey");
    }

    // 新增：禁用/启用AK接口（需要AK/SK签名）
    @PutMapping("/updateAkStatus")
    public Result<Boolean> updateAkStatus(
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce,
            @RequestParam Integer status // 1=启用，0=禁用
    ) {
        // 1. 先执行AK/SK鉴权
        sysUserService.verifyAkSk(accessKey, sign, timestamp, nonce);
        // 2. 修改AK状态
        boolean result = sysUserService.updateAkStatus(accessKey, status);
        return Result.success(result);
    }


    // 新增：查询AK状态接口（需要AK/SK签名）
    @GetMapping("/getAkStatus")
    public Result<AkStatusVO> getAkStatus(
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce
    ) {
        // 1. 先执行AK/SK鉴权
        sysUserService.verifyAkSk(accessKey, sign, timestamp, nonce);
        // 2. 查询并返回AK状态
        AkStatusVO akStatusVO = sysUserService.getAkStatus(accessKey);
        return Result.success(akStatusVO);
    }

    // 新增：通过Token查询AK状态接口（适用于Web端，无需SecretKey）
    @GetMapping("/getAkStatusByToken")
    public Result<AkStatusVO> getAkStatusByToken() {
        // 1. 从Token中获取当前用户的AccessKey（JwtInterceptor已验证Token）
        String accessKey = JwtInterceptor.getCurrentAccessKey();
        if (accessKey == null) {
            throw new BusinessException(ErrorCodeEnum.TOKEN_MISSING, "Token中未找到AccessKey");
        }
        // 2. 查询并返回AK状态（无需SecretKey验证，因为Token已验证身份）
        AkStatusVO akStatusVO = sysUserService.getAkStatus(accessKey);
        return Result.success(akStatusVO);
    }

    // 新增：批量查询AK状态接口
    @PostMapping("/batchGetAkStatus")
    public Result<BatchAkStatusVO> batchGetAkStatus(
            @RequestParam String accessKey,  // 管理员AK（需启用状态，否则鉴权抛异常）
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce,
            @RequestParam String akList      // 批量查询的AK列表，逗号分隔（如：test_ak_123,test_ak_456）
    ) {
        // 1. 先执行AK/SK鉴权（管理员AK禁用则抛异常）
        sysUserService.verifyAkSk(accessKey, sign, timestamp, nonce);
        // 2. 批量查询AK状态
        BatchAkStatusVO batchVO = sysUserService.batchGetAkStatus(akList);
        // 3. 返回结果
        return Result.success(batchVO);
    }

    // 新增：AK操作日志查询接口
    @GetMapping("/getAkOperateLog")
    public Result<List<AkOperateLogVO>> getAkOperateLog(
            @RequestParam String accessKey,  // 管理员AK（需启用状态）
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce,
            @RequestParam String targetAk    // 待查日志的目标AK（如test_ak_123）
    ) {
        // 1. 先执行AK/SK鉴权（管理员AK禁用则抛异常）
        sysUserService.verifyAkSk(accessKey, sign, timestamp, nonce);
        // 2. 查询目标AK的操作日志
        List<AkOperateLogVO> logVOList = akOperateLogService.getAkOperateLog(targetAk);
        // 3. 返回结果（无日志则返回空列表，不抛异常）
        return Result.success(logVOList);
    }

    // 新增：AK过期时间管理接口
    @PostMapping("/manageAkExpireTime")
    public Result<AkExpireTimeVO> manageAkExpireTime(
            @RequestParam String accessKey,  // 管理员AK
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce,
            @RequestParam String targetAk,   // 目标AK
            @RequestParam(required = false) String expireTime  // 可选：过期时间（格式：2025-12-31T23:59:59）
    ) {
        // 1. 鉴权
        sysUserService.verifyAkSk(accessKey, sign, timestamp, nonce);

        // 2. 解析expireTime（String转LocalDateTime）
        LocalDateTime expireTimeObj = null;
        if (expireTime != null && !expireTime.isEmpty()) {
            expireTimeObj = LocalDateTime.parse(expireTime);
        }

        // 3. 处理设置/查询
        AkExpireTimeVO resultVO = sysUserService.manageAkExpireTime(targetAk, expireTimeObj);
        return Result.success(resultVO);
    }

    /**
     * AK权限管理接口（注释：管理员鉴权后调用）
     */
    @PostMapping("/manageAkPermission")
    public Result<AkPermissionVO> manageAkPermission(
            @RequestParam String accessKey,          // 管理员AK（鉴权用）
            @RequestParam String sign,               // 签名（鉴权用）
            @RequestParam Long timestamp,            // 时间戳（鉴权用）
            @RequestParam String nonce,              // 随机串（鉴权用）
            @RequestParam String targetAk,           // 目标AK
            @RequestParam(required = false) Integer permissionType // 可选：权限类型
    ) {
        // 1. 管理员鉴权（注释：复用原有鉴权逻辑，保证安全性）
        sysUserService.verifyAkSk(accessKey, sign, timestamp, nonce);

        // 2. 处理权限分配/查询（注释：调用无冗余的业务逻辑）
        AkPermissionVO resultVO = sysUserService.manageAkPermission(targetAk, permissionType);

        // 3. 返回结果
        return Result.success(resultVO);
    }

    /**
     * AK操作日志分页查询接口
     */
    @PostMapping("/queryAkOperateLog")
    public Result<IPage<AkOperateLogVO>> queryAkOperateLog(
            // 鉴权参数（必传）
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce,
            // 分页参数（必传）
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize,
            // 可选查询条件
            @RequestParam(required = false) String targetAk,
            @RequestParam(required = false) Integer operateType,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") // 匹配Postman里的“2025-12-13 18:35:45”格式
            LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime endTime
    ) {
        // 1. 管理员鉴权（复用你项目里已有的鉴权逻辑）
        sysUserService.verifyAkSk(accessKey, sign, timestamp, nonce);

        // 2. 执行日志查询
        IPage<AkOperateLogVO> logPage = akOperateLogService.queryAkOperateLog(
                pageNum, pageSize, targetAk, operateType, startTime, endTime
        );

        // 3. 返回结果
        return Result.success(logPage, "日志查询成功");
    }

    /**
     * AK批量启用/禁用接口（管理员鉴权后调用）
     */
    @PostMapping("/batchOperateAkStatus")
    public Result<Integer> batchOperateAkStatus(
            // 鉴权参数（完全保留，无变动）
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce,

            // 批量参数（仅operateType说明变动：2=启用/3=禁用，匹配枚举/数据库）
            @RequestParam Integer operateType,
            @RequestParam String targetAks,
            @RequestParam(required = false) String remark
    ) {
        // 鉴权逻辑（完全保留，无变动）
        sysUserService.verifyAkSk(accessKey, sign, timestamp, nonce);

        // 执行批量操作（无变动）
            int successCount = sysUserService.batchOperateAkStatus(operateType, targetAks, remark);

        // 返回结果（仅提示语匹配2/3，无变动）
        String msg = operateType == 3 ? "批量禁用AK成功" : "批量启用AK成功";
        return Result.success(successCount, msg + "，共操作" + successCount + "个AK");
    }

    /**
     * AK批量创建接口（POST + form-data传参，避免参数丢失）
     */
    @PostMapping("/batchCreateAk")
    public Result<Map<String, String>> batchCreateAk(
            // 1. 通用鉴权参数（必传）
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce,

            // 2. 业务参数
            @RequestParam Integer count,                // 创建数量（1-50）
            @RequestParam(required = false) Integer permissionType // 初始权限（1=只读/2=读写，默认1）
    ) {
        // 第一步：管理员鉴权（复用原有签名校验逻辑）
        sysUserService.verifyAkSk(accessKey, sign, timestamp, nonce);

        // 第二步：执行批量创建
        Map<String, String> akMap = sysUserService.batchCreateAk(count, permissionType);

        // 第三步：返回结果（适配现有Result格式）
        String msg = String.format("批量创建AK成功！共创建%d个，secretKey仅返回一次，请妥善保存", akMap.size());
        return Result.success(akMap, msg);
    }

    /**
     * AK批量查询接口（带分页，form-data传参）
     */
    @PostMapping("/batchQueryAk")
    public Result<IPage<SysUser>> batchQueryAk(
            // 1. 通用鉴权参数（必传）
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce,

            // 2. 分页参数（可选）
            @RequestParam(required = false) Integer pageNum,     // 默认1
            @RequestParam(required = false) Integer pageSize,   // 默认10，最大50

            // 3. 查询条件（可选）
            @RequestParam(required = false) String targetAkLike, // AK模糊匹配
            @RequestParam(required = false) Integer status,      // 1=启用，3=禁用
            @RequestParam(required = false) Integer permissionType // 1=只读，2=读写
    ) {
        // 管理员鉴权（复用原有签名校验）
        sysUserService.verifyAkSk(accessKey, sign, timestamp, nonce);

        // 执行分页查询
        IPage<SysUser> akPage = sysUserService.batchQueryAk(pageNum, pageSize, targetAkLike, status, permissionType);

        // 返回结果
        return Result.success(akPage, "AK列表查询成功！共" + akPage.getTotal() + "条数据");
    }

    /**
     * AK有效性校验接口（form-data传参）
     */
    @PostMapping("/checkAkValid")
    public Result<Map<String, Object>> checkAkValid(
            // 1. 通用鉴权参数（必传，管理员AK鉴权）
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce,

            // 2. 待校验的AK（必传）
            @RequestParam String checkAccessKey
    ) {
        // 管理员鉴权（复用原有签名校验）
        sysUserService.verifyAkSk(accessKey, sign, timestamp, nonce);

        // 执行AK有效性校验
        Map<String, Object> checkResult = sysUserService.checkAkValid(checkAccessKey);

        // 返回结果（200统一返回，通过msg/字段区分是否有效）
        return Result.success(checkResult, "AK校验完成");
    }

    /**
     * AK逻辑删除接口（form-data传参）
     */
    @PostMapping("/logicDeleteAk")
    public Result<Integer> logicDeleteAk(
            // 1. 通用鉴权参数（必传，仅管理员可删除）
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce,

            // 2. 业务参数
            @RequestParam String targetAk, // 待删除的AK
            @RequestParam(required = false) String remark // 删除备注（可选）
    ) {
        // 管理员鉴权（复用原有签名校验）
        sysUserService.verifyAkSk(accessKey, sign, timestamp, nonce);

        // 执行逻辑删除
        int deleteResult = sysUserService.logicDeleteAk(targetAk, remark);

        // 返回结果
        if (deleteResult == 1) {
            return Result.success(1, "AK逻辑删除成功！");
        } else {
            return Result.success(0, "AK不存在或已被删除！");
        }
    }
}
