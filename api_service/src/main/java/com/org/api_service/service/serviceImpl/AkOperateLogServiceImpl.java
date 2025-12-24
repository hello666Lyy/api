package com.org.api_service.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.org.api_common.constant.AkOperateTypeEnum;
import com.org.api_common.constant.ErrorCodeEnum;
import com.org.api_common.exception.BusinessException;
import com.org.api_common.vo.AkOperateLogVO;
import com.org.api_common.entity.AkOperateLog;
import com.org.api_service.mapper.AkOperateLogMapper;
import com.org.api_service.service.AkOperateLogService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AkOperateLogServiceImpl extends ServiceImpl<AkOperateLogMapper, AkOperateLog> implements AkOperateLogService {

    @Override
    public List<AkOperateLogVO> getAkOperateLog(String targetAk) {
        // 1. 查询指定AK的日志
        List<AkOperateLog> logList = baseMapper.selectByTargetAk(targetAk);
        // 2. 转换为VO（将操作类型code转为中文描述）
        List<AkOperateLogVO> voList = new ArrayList<>();
        for (AkOperateLog log : logList) {
            AkOperateLogVO vo = new AkOperateLogVO();
            BeanUtils.copyProperties(log, vo);
            // 替换操作类型为中文描述
            vo.setOperateTypeDesc(AkOperateTypeEnum.getDescByCode(log.getOperateType()));
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public IPage<AkOperateLogVO> queryAkOperateLog(Integer pageNum, Integer pageSize,
                                                   String targetAk, Integer operateType,
                                                   LocalDateTime startTime, LocalDateTime endTime) {
        // 1. 分页/时间参数校验
        if (pageNum <= 0 || pageSize <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "分页参数必须大于0");
        }
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "开始时间不能晚于结束时间");
        }

        // 2. 构建分页对象
        Page<AkOperateLog> page = new Page<>(pageNum, pageSize);

        // 3. 多条件查询（只拼接非空条件）
        LambdaQueryWrapper<AkOperateLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(targetAk != null && !targetAk.isEmpty(), AkOperateLog::getTargetAk, targetAk)
                .eq(operateType != null, AkOperateLog::getOperateType, operateType)
                .ge(startTime != null, AkOperateLog::getOperateTime, startTime)
                .le(endTime != null, AkOperateLog::getOperateTime, endTime)
                .orderByDesc(AkOperateLog::getOperateTime); // 按操作时间倒序

        // 4. 执行分页查询
        IPage<AkOperateLog> logPage = this.page(page, queryWrapper);

        // 5. 实体转VO（补充操作类型描述+备注脱敏）
        List<AkOperateLogVO> voList = logPage.getRecords().stream().map(log -> {
            AkOperateLogVO vo = new AkOperateLogVO();
            BeanUtils.copyProperties(log, vo);
            // 补充操作类型中文描述（用你项目里的枚举）
            vo.setOperateTypeDesc(AkOperateTypeEnum.getDescByCode(log.getOperateType()));
            // 备注脱敏（如密钥隐藏）
            if (vo.getRemark() != null && vo.getRemark().contains("新密钥：")) {
                vo.setRemark(vo.getRemark().replaceAll("新密钥：(\\w{6})\\w+", "新密钥：$1****"));
            }
            return vo;
        }).collect(Collectors.toList());

        // 6. 封装VO分页结果
        Page<AkOperateLogVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setRecords(voList);
        voPage.setTotal(logPage.getTotal());
        voPage.setPages(logPage.getPages());

        return voPage;
    }

}