package com.org.api_admin_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.org.api_common.entity.ApiInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApiInfoMapper extends BaseMapper<ApiInfo> {
    // 基础CRUD由BaseMapper提供
    // 如需自定义查询，可在此添加方法
}