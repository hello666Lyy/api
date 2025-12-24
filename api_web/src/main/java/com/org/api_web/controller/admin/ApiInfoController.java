package com.org.api_web.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.org.api_common.result.Result;
import com.org.api_common.dto.ApiInfoDTO;
import com.org.api_common.vo.ApiInfoVO;
import com.org.api_admin_service.service.ApiInfoService;
import com.org.api_service.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 接口信息管理控制器（管理员专用）
 */
@RestController
@RequestMapping("/api/admin/api-info")
@Validated
public class ApiInfoController {

    @Resource
    private ApiInfoService apiInfoService;

    @Autowired
    private SysUserService sysUserService;

    /**
     * 分页查询接口列表
     */
    @GetMapping("/list")
    public Result<IPage<ApiInfoVO>> listApis(
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String apiName,
            @RequestParam(required = false) String apiPath,
            @RequestParam(required = false) Integer status) {

        // 管理员权限校验
        sysUserService.verifyAdminPermission(accessKey, sign, timestamp, nonce);

        IPage<ApiInfoVO> page = apiInfoService.pageApiList(pageNum, pageSize,
                apiName, apiPath, status);
        return Result.success(page, "接口列表查询成功");
    }

    /**
     * 添加接口
     */
    @PostMapping("/add")
    public Result<ApiInfoVO> addApi(
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce,
            @RequestParam String apiName,
            @RequestParam String apiPath,
            @RequestParam String method,
            @RequestParam(required = false) String apiDesc,
            @RequestParam Integer status) {

        ApiInfoDTO apiInfoDTO = new ApiInfoDTO();
        apiInfoDTO.setApiName(apiName);
        apiInfoDTO.setApiPath(apiPath);
        apiInfoDTO.setMethod(method);
        apiInfoDTO.setApiDesc(apiDesc);
        apiInfoDTO.setStatus(status);
        // 管理员权限校验
        sysUserService.verifyAdminPermission(accessKey, sign, timestamp, nonce);

        ApiInfoVO apiInfoVO = apiInfoService.addApi(apiInfoDTO);
        return Result.success(apiInfoVO, "接口添加成功");
    }

    /**
     * 修改接口
     */
    @PutMapping("/update/{id}")
    public Result<ApiInfoVO> updateApi(
            @PathVariable Long id,
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce,
            @RequestParam String apiName,
            @RequestParam String apiPath,
            @RequestParam String method,
            @RequestParam(required = false) String apiDesc,
            @RequestParam Integer status) {

        ApiInfoDTO apiInfoDTO = new ApiInfoDTO();
        apiInfoDTO.setApiName(apiName);
        apiInfoDTO.setApiPath(apiPath);
        apiInfoDTO.setMethod(method);
        apiInfoDTO.setApiDesc(apiDesc);
        apiInfoDTO.setStatus(status);
        // 管理员权限校验
        sysUserService.verifyAdminPermission(accessKey, sign, timestamp, nonce);

        ApiInfoVO apiInfoVO = apiInfoService.updateApi(id, apiInfoDTO);
        return Result.success(apiInfoVO, "接口修改成功");
    }

    /**
     * 删除接口
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteApi(
            @PathVariable Long id,
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce) {

        // 管理员权限校验
        sysUserService.verifyAdminPermission(accessKey, sign, timestamp, nonce);

        apiInfoService.deleteApi(id);
        return Result.success(null, "接口删除成功");
    }

    /**
     * 获取接口详情
     */
    @GetMapping("/detail/{id}")
    public Result<ApiInfoVO> getApiDetail(
            @PathVariable Long id,
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce) {

        // 管理员权限校验
        sysUserService.verifyAdminPermission(accessKey, sign, timestamp, nonce);

        ApiInfoVO apiInfoVO = apiInfoService.getApiById(id);
        return Result.success(apiInfoVO, "接口详情查询成功");
    }

    /**
     * 启用/禁用接口
     */
    @PutMapping("/status/{id}")
    public Result<ApiInfoVO> updateApiStatus(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce) {

        // 管理员权限校验
        sysUserService.verifyAdminPermission(accessKey, sign, timestamp, nonce);

        ApiInfoVO apiInfoVO = apiInfoService.updateApiStatus(id, status);
        String msg = status == 1 ? "接口启用成功" : "接口禁用成功";
        return Result.success(apiInfoVO, msg);
    }
}