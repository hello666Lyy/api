package com.org.api_common.vo;

import lombok.Data;
import java.util.List;

/**
 * 我的调用统计VO
 */
@Data
public class MyStatisticsVO {
    /** 总调用次数 */
    private Long totalCalls;
    
    /** 成功调用次数 */
    private Long successCalls;
    
    /** 失败调用次数 */
    private Long failedCalls;
    
    /** 今日调用次数 */
    private Long todayCalls;
    
    /** 各接口调用统计 */
    private List<ApiCallStat> apiCallStats;
    
    /**
     * 接口调用统计
     */
    @Data
    public static class ApiCallStat {
        /** 接口ID */
        private Long apiId;
        
        /** 接口名称 */
        private String apiName;
        
        /** 调用次数 */
        private Long callCount;
    }
}

