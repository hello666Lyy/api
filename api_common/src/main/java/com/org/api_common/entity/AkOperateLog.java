package com.org.api_common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AK操作日志实体（对应数据库表ak_operate_log）
 */
@Data
@TableName("ak_operate_log")
public class AkOperateLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String targetAk;
    private Integer operateType;
    private LocalDateTime operateTime;
    private String operator;
    private String remark;
}
