package com.org.api_common.vo;

import lombok.Data;

/**
 * 全局调用统计VO（管理员视角）
 */
@Data
public class GlobalStatisticsVO {

    /** 总调用次数 */
    private Long totalCalls;

    /** 今日调用次数 */
    private Long todayCalls;

    /** 总用户数 */
    private Long totalUsers;

    /** 总接口数 */
    private Long totalApis;

    /** 平均响应时间（毫秒），可选字段 */
    private Double avgResponseTime;
}






























