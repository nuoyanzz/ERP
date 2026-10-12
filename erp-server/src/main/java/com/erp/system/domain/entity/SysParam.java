package com.erp.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_param")
public class SysParam extends BaseEntity {

    private String paramKey;
    private String paramValue;
    private String paramName;
    /** 参数类型：STRING / NUMBER / BOOLEAN / JSON */
    private String paramType;
    private Integer status;
    private String remark;
}








