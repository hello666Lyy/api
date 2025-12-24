package com.org.api_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.org.api_common.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户Mapper（AK/SK校验核心查询）
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    /**
     * 根据AK查询有效用户（status=1）
     * @param accessKey 访问密钥AK
     * @return 有效用户信息（null=AK无效/用户禁用）
     */
    SysUser selectByAccessKey(@Param("accessKey") String accessKey);

    // 新增方法：查询所有状态的AK（供查询目标AK信息用）
    // 修复：在SQL中添加permission_type字段（必须加，否则查不到新字段）
    @Select("SELECT id, username, password, access_key, secret_key, status, create_time, update_time, expire_time, permission_type FROM sys_user WHERE access_key = #{accessKey}")
    SysUser selectByAccessKeyAllStatus(String accessKey);
}
