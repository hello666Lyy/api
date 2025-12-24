package com.org.api_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.org.api_common.entity.ApiCallLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * API调用日志Mapper
 */
@Mapper
public interface ApiCallLogMapper extends BaseMapper<ApiCallLog> {
    // 基础CRUD由BaseMapper提供
}