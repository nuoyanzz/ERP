package com.erp.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统参数返回对象。
 */
@Data
public class SysParamVO {

    private Long id;
    private String paramKey;
    private String paramValue;
    private String paramName;
    /** 参数类型：STRING / NUMBER / BOOLEAN / JSON */
    private String paramType;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}













