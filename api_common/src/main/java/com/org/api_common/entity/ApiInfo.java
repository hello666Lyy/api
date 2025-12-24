package com.org.api_common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口信息表
 * @author zhangzhenhui
 */
@Data
@TableName("api_info")
public class ApiInfo {
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 接口名称 */
    private String apiName;
    /** 接口路径 */
    private String apiPath;
    /** 请求方式：GET/POST/PUT/DELETE */
    private String method;
    /** 接口描述 */
    private String apiDesc;
    /** 状态：0禁用 1启用 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}