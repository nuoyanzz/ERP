package com.erp.hr.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HrOrgVO {

    private Long id;
    private String orgCode;
    private String orgName;
    private Integer orgType;
    private Long parentId;
    private String remark;
    private LocalDateTime createTime;
}








