package com.org.api_admin_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.org.api_common.entity.ApiPermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 接口权限Mapper
 */
@Mapper
public interface ApiPermissionMapper extends BaseMapper<ApiPermission> {
    // 基础CRUD由BaseMapper提供
}

