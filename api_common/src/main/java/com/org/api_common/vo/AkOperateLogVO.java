package com.org.api_common.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * AK操作日志返回VO
 */
@Data
public class AkOperateLogVO {
    /** 日志ID */
    private Long id;
    /** 操作的目标AK */
    private String targetAk;
    /** 操作类型（中文描述） */
    private String operateTypeDesc;
    /** 操作时间 */
    private LocalDateTime operateTime;
    /** 操作人 */
    private String operator;
    /** 备注 */
    private String remark;
}