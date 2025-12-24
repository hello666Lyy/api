package com.org.api_common.vo;

import lombok.Data;

/**
 * AK权限返回VO
 */
@Data
public class AkPermissionVO {
    private String targetAk;          // 目标AK
    private Integer permissionType;   // 权限编码（1/2/3）
    private String permissionDesc;    // 权限中文描述（只读/读写/管理员）
}
