package com.org.api_common.vo;

import lombok.Data;

import java.util.List;

/**
 * 批量AK状态返回VO
 */
@Data
public class BatchAkStatusVO {
    /** 批量查询的AK总数 */
    private Integer total;
    /** 存在的AK数量 */
    private Integer existCount;
    /** 单个AK的状态列表 */
    private List<SingleAkStatusVO> akStatusList;
}