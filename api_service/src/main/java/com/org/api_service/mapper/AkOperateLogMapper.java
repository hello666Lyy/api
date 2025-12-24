package com.org.api_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.org.api_common.entity.AkOperateLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AkOperateLogMapper extends BaseMapper<AkOperateLog> {
    /** 查询指定AK的所有操作日志 */
    @Select("SELECT id, target_ak, operate_type, operate_time, operator, remark FROM ak_operate_log WHERE target_ak = #{targetAk} ORDER BY operate_time DESC")
    List<AkOperateLog> selectByTargetAk(String targetAk);
}
