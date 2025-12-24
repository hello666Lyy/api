package com.org.api_common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口调用日志表
 * @author zhangzhenhui
 */
@Data
@TableName("api_call_log")
public class ApiCallLog {
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 调用方AK */
    private String accessKey;
    /** 接口ID */
    private Long apiId;
    /** 调用IP */
    private String ip;
    /** 请求参数 */
    private String requestParams;
    /** 响应结果 */
    private String responseResult;
    /** 调用状态：0失败 1成功 */
    private Integer status;
    /** 错误信息 */
    private String errorMsg;
    /** 调用耗时（毫秒） */
    private Long costTime;
    /** 调用时间 */
    private LocalDateTime callTime;

    /** 以下为非持久化字段，用于返回给前端展示 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String apiPath;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String method;
}