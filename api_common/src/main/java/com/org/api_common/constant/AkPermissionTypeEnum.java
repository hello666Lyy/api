package com.org.api_common.constant;

import lombok.Getter;

/**
 * AK权限类型枚举
 */
@Getter
public enum AkPermissionTypeEnum {
    READ_ONLY(1, "只读"),       // 仅能查询自身AK信息
    READ_WRITE(2, "读写"),      // 可查询+修改自身AK状态/过期时间
    ADMIN(3, "管理员");         // 可管理所有AK

    private final Integer code;   // 权限编码
    private final String desc;    // 权限中文描述

    AkPermissionTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 根据编码获取中文描述（注释：供VO转换使用）
    public static String getDescByCode(Integer code) {
        for (AkPermissionTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return "未知权限";
    }

    // 手动写getter（必须有！）
    public Integer getCode() {
        return this.code;
    }

    public String getDesc() {
        return this.desc;
    }
}
