package com.org.api_common.constant;

import lombok.Getter;

/**
 * AK操作类型枚举
 */
@Getter
public enum AkOperateTypeEnum {
    // 以下枚举完全保留，无任何变动（匹配数据库operate_type=1/2/3）
    GENERATE_NEW_KEY(1, "生成新AK/SK"),
    ENABLE_AK(2, "启用AK"),
    DISABLE_AK(3, "禁用AK"),

    // 核心保留：code=4和名称完全不变（已有接口依赖，禁止修改）
    RESET_SECRET_KEY(4, "重置AK密钥"),

    // 最小变动：仅改code从4→6（解决重复问题，不改动名称）
    QUERY_AK_STATUS(6, "查询AK状态"),

    // 完全保留：code=5和名称不变（数据库中5是分配权限，此处注释说明冲突即可）
    BATCH_QUERY_AK(5, "批量查询AK"),

    // 新增：仅新增该枚举（不影响原有接口），用于分配权限接口，code=8避免冲突
    ASSIGN_PERMISSION(8, "分配AK权限");
//    RESET_SECRET_KEY(4, "重置AK密钥"),
//    GENERATE_NEW_KEY(1, "生成新AK/SK"),
//    ENABLE_AK(2, "启用AK"),
//    DISABLE_AK(3, "禁用AK"),
//    QUERY_AK_STATUS(4, "查询AK状态"),
//    BATCH_QUERY_AK(5, "批量查询AK");

    private final Integer code;
    private final String desc;

    AkOperateTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 根据code获取描述
    public static String getDescByCode(Integer code) {
        for (AkOperateTypeEnum e : AkOperateTypeEnum.values()) { // 显式写枚举类名，更稳妥
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return "未知操作";
    }

    // 手动写getter（必须有！）
    public Integer getCode() {
        return this.code;
    }

    public String getDesc() {
        return this.desc;
    }
}