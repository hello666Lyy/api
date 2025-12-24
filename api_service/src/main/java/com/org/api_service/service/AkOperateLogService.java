package com.org.api_service.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.org.api_common.vo.AkOperateLogVO;
import com.org.api_common.entity.AkOperateLog;

import java.time.LocalDateTime;
import java.util.List;

public interface AkOperateLogService extends IService<AkOperateLog> {
    /** 查询指定AK的操作日志（转换为VO） */
    List<AkOperateLogVO> getAkOperateLog(String targetAk);

    /**
     * 分页查询AK操作日志（多条件过滤）
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param targetAk 目标AK（可选）
     * @param operateType 操作类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 分页日志VO
     */
    IPage<AkOperateLogVO> queryAkOperateLog(Integer pageNum, Integer pageSize,
                                            String targetAk, Integer operateType,
                                            LocalDateTime startTime, LocalDateTime endTime);
}