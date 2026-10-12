package com.erp.hr.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class HrDeptTreeVO {

    private Long id;
    private String deptCode;
    private String deptName;
    private Long orgId;
    private Long parentId;
    private String remark;
    private List<HrDeptTreeVO> children;
}








