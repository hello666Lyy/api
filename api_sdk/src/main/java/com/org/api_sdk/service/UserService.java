package com.org.api_sdk.service;

import com.org.api_common.entity.SysUser;
import com.org.api_common.vo.*;
import com.org.api_sdk.ApiClient;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Map;

/**
 * 用户相关API服务
 * @author zhangzhenhui
 */
public class UserService {
    private final ApiClient client;

    public UserService(ApiClient client) {
        this.client = client;
    }

    /**
     * AK/SK校验
     */
    public Boolean verifyAkSk() {
        return client.executeRequest("POST", "/api/user/verifyAkSk", null, Boolean.class);
    }

    /**
     * 获取用户信息
     */
    public UserInfoVO getUserInfo() {
        return client.executeRequest("GET", "/api/user/getUserInfo", null, UserInfoVO.class);
    }

    /**
     * 生成新AK/SK
     */
    public Map<String, String> generateNewAkSk() {
        return client.executeRequest("POST", "/api/user/generateNewAkSk", null, Map.class);
    }

    /**
     * 更新AK状态
     * @param status 1=启用，0=禁用
     */
    public Boolean updateAkStatus(Integer status) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("status", status);
        return client.executeRequest("PUT", "/api/user/updateAkStatus", params, Boolean.class);
    }

    /**
     * 获取AK状态
     */
    public AkStatusVO getAkStatus() {
        return client.executeRequest("GET", "/api/user/getAkStatus", null, AkStatusVO.class);
    }

    /**
     * 批量获取AK状态
     * @param akList AK列表，逗号分隔
     */
    public BatchAkStatusVO batchGetAkStatus(String akList) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("akList", akList);
        return client.executeRequest("POST", "/api/user/batchGetAkStatus", params, BatchAkStatusVO.class);
    }

    /**
     * 获取AK操作日志
     * @param targetAk 目标AK
     */
    public List<AkOperateLogVO> getAkOperateLog(String targetAk) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("targetAk", targetAk);
        return client.executeRequest("GET", "/api/user/getAkOperateLog", params, List.class);
    }

    /**
     * 管理AK过期时间
     * @param targetAk 目标AK
     * @param expireTime 过期时间（格式：2025-12-31T23:59:59），null表示查询
     */
    public AkExpireTimeVO manageAkExpireTime(String targetAk, String expireTime) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("targetAk", targetAk);
        if (expireTime != null) {
            params.put("expireTime", expireTime);
        }
        return client.executeRequest("POST", "/api/user/manageAkExpireTime", params, AkExpireTimeVO.class);
    }

    /**
     * 管理AK权限
     * @param targetAk 目标AK
     * @param permissionType 权限类型，null表示查询
     */
    public AkPermissionVO manageAkPermission(String targetAk, Integer permissionType) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("targetAk", targetAk);
        if (permissionType != null) {
            params.put("permissionType", permissionType);
        }
        return client.executeRequest("POST", "/api/user/manageAkPermission", params, AkPermissionVO.class);
    }

    /**
     * 分页查询AK操作日志
     */
    public IPage<AkOperateLogVO> queryAkOperateLog(Integer pageNum, Integer pageSize,
                                                   String targetAk, Integer operateType,
                                                   String startTime, String endTime) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("pageNum", pageNum);
        params.put("pageSize", pageSize);
        if (targetAk != null) {
            params.put("targetAk", targetAk);
        }
        if (operateType != null) {
            params.put("operateType", operateType);
        }
        if (startTime != null) {
            params.put("startTime", startTime);
        }
        if (endTime != null) {
            params.put("endTime", endTime);
        }
        return client.executeRequest("POST", "/api/user/queryAkOperateLog", params, IPage.class);
    }

    /**
     * 批量操作AK状态
     * @param operateType 操作类型：2=启用，3=禁用
     * @param targetAks 目标AK列表，逗号分隔
     * @param remark 备注（可选）
     */
    public Integer batchOperateAkStatus(Integer operateType, String targetAks, String remark) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("operateType", operateType);
        params.put("targetAks", targetAks);
        if (remark != null) {
            params.put("remark", remark);
        }
        return client.executeRequest("POST", "/api/user/batchOperateAkStatus", params, Integer.class);
    }

    /**
     * 批量创建AK
     * @param count 创建数量（1-50）
     * @param permissionType 初始权限（1=只读/2=读写，默认1）
     */
    public Map<String, String> batchCreateAk(Integer count, Integer permissionType) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("count", count);
        if (permissionType != null) {
            params.put("permissionType", permissionType);
        }
        return client.executeRequest("POST", "/api/user/batchCreateAk", params, Map.class);
    }

    /**
     * 批量查询AK（分页）
     */
    public IPage<SysUser> batchQueryAk(Integer pageNum, Integer pageSize,
                                       String targetAkLike, Integer status, Integer permissionType) {
        Map<String, Object> params = new java.util.HashMap<>();
        if (pageNum != null) {
            params.put("pageNum", pageNum);
        }
        if (pageSize != null) {
            params.put("pageSize", pageSize);
        }
        if (targetAkLike != null) {
            params.put("targetAkLike", targetAkLike);
        }
        if (status != null) {
            params.put("status", status);
        }
        if (permissionType != null) {
            params.put("permissionType", permissionType);
        }
        return client.executeRequest("POST", "/api/user/batchQueryAk", params, IPage.class);
    }

    /**
     * 校验AK有效性
     * @param checkAccessKey 待校验的AK
     */
    public Map<String, Object> checkAkValid(String checkAccessKey) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("checkAccessKey", checkAccessKey);
        return client.executeRequest("POST", "/api/user/checkAkValid", params, Map.class);
    }

    /**
     * 逻辑删除AK
     * @param targetAk 待删除的AK
     * @param remark 删除备注（可选）
     */
    public Integer logicDeleteAk(String targetAk, String remark) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("targetAk", targetAk);
        if (remark != null) {
            params.put("remark", remark);
        }
        return client.executeRequest("POST", "/api/user/logicDeleteAk", params, Integer.class);
    }
}