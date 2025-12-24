package com.org.api_common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口权限表
 * @author zhangzhenhui
 */
@Data
@TableName("api_permission")
public class ApiPermission {
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 接口ID */
    private Long apiId;
    /** 权限状态：0未授权 1已授权 */
    private Integer status;
    /** 授权过期时间 */
    private LocalDateTime expireTime;
    /** 创建时间 */
    private LocalDateTime createTime;
}