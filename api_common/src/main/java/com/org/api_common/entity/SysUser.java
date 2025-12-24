package com.org.api_common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户表
 * @author zhangzhenhui
 */
@Data
@TableName("sys_user")
public class SysUser {
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户名 */
    private String username;
    /** 密码（加密） */
    private String password;
    /** 访问密钥AK */
    private String accessKey;
    /** 秘钥SK */
    private String secretKey;
    /** 状态：0禁用 1启用 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;

    /** AK过期时间（null表示永不过期） */
    @TableField("expire_time")
    private LocalDateTime expireTime;
    /** AK权限类型：1=只读，2=读写，3=管理员 */
    @TableField("permission_type")
    private Integer permissionType;
    @TableField("delete_status") // 明确映射数据库字段（驼峰转下划线的话，也可以不写，但加上更保险）
    private Integer deleteStatus; // 0=未删除，1=已删除
}
