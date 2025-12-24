package com.org.api_service.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.org.api_common.entity.SysUser;
import com.org.api_common.vo.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户业务接口（AK/SK校验核心）
 */
public interface SysUserService extends IService<SysUser> {
    /**
     * 根据AK查询有效用户
     * @param accessKey 访问密钥AK
     * @return 有效用户信息
     */
    SysUser getByAccessKey(String accessKey);

    /**
     * 完整校验AK/SK+签名+时间戳+Nonce
     * @param accessKey 访问密钥AK
     * @param sign 调用方传入的签名
     * @param timestamp 调用方传入的时间戳（秒级）
     * @param nonce 调用方传入的随机串
     * @return 校验通过返回true，失败抛业务异常
     */
    boolean verifyAkSk(String accessKey, String sign, Long timestamp, String nonce);

    // 新增：获取用户非敏感信息（转VO）
    UserInfoVO getUserInfoVO(String accessKey);

    /** 生成新AK/SK并更新用户 */
    Map<String, String> generateNewAkSk(String accessKey);

    /** 修改AK的启用/禁用状态 */
    boolean updateAkStatus(String accessKey, Integer status);

    /** 查询AK状态信息 */
    AkStatusVO getAkStatus(String accessKey);

    // 新增：批量查询AK状态
    BatchAkStatusVO batchGetAkStatus(String akListStr);

    /** 设置/查询AK过期时间 */
    AkExpireTimeVO manageAkExpireTime(String targetAk, LocalDateTime expireTime);

    /**
     * 分配/查询AK权限
     * @param targetAk 目标AK
     * @param permissionType 权限类型（传则分配，不传则查询）
     * @return 权限信息VO
     */
    AkPermissionVO manageAkPermission(String targetAk, Integer permissionType);

    /**
     * 重置目标AK的secret_key
     * @param targetAk 目标AK
     * @return 新的secret_key（仅返回一次，后续无法再获取）
     */
    String resetAkSecretKey(String targetAk);

    /**
     * 批量操作AK状态（启用/禁用）
     * @param operateType 2=启用AK（枚举ENABLE_AK）/3=禁用AK（枚举DISABLE_AK）（匹配数据库operate_type=2/3）
     * @param targetAks 批量AK，逗号分隔
     * @param remark 操作备注
     * @return 成功操作数量
     */
    int batchOperateAkStatus(Integer operateType, String targetAks, String remark);

    // ========== 新增批量创建AK核心方法 ==========
    /**
     * 批量创建AK（accessKey + secretKey）
     * @param count 创建数量（限制1-50个，避免批量生成过多）
     * @param permissionType 初始权限：1=只读（默认）、2=读写
     * @return Map<accessKey, secretKey> - 仅返回一次，需妥善保存secretKey
     */
    Map<String, String> batchCreateAk(Integer count, Integer permissionType);

    /**
     * 分页查询AK列表
     * @param pageNum 页码（默认1）
     * @param pageSize 页大小（默认10，最大50）
     * @param targetAkLike AK模糊匹配（可选，如test_ak%）
     * @param status 状态筛选（可选：1=启用，3=禁用）
     * @param permissionType 权限筛选（可选：1=只读，2=读写）
     * @return 分页结果（含总数+当前页数据）
     */
    IPage<SysUser> batchQueryAk(Integer pageNum, Integer pageSize, String targetAkLike, Integer status, Integer permissionType);

    /**
     * 校验AK是否有效
     * @param accessKey 待校验的AK
     * @return 校验结果：key=校验维度，value=是否通过（true/false）；额外返回msg=结果说明
     */
    Map<String, Object> checkAkValid(String accessKey);

    /**
     * AK逻辑删除（标记delete_status=1）
     * @param targetAk 待删除的AK
     * @param remark 删除备注（可选，如“清理过期AK”）
     * @return 1=删除成功，0=AK不存在/已删除
     */
    int logicDeleteAk(String targetAk, String remark);

    /**
     * 校验管理员权限
     * @param accessKey 管理员AK
     * @param sign 签名
     * @param timestamp 时间戳
     * @param nonce 随机串
     */
    void verifyAdminPermission(String accessKey, String sign, Long timestamp, String nonce);
}