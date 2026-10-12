package com.erp.fi.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class FiAccountTreeVO {

    private Long id;
    private String subjectCode;
    private String subjectName;
    private Long parentId;
    private Integer category;
    private Integer balanceDir;
    private Integer assistCustomer;
    private Integer assistSupplier;
    private Integer assistDept;
    private Integer assistProject;
    private String currency;
    private Integer status;
    private String remark;
    private List<FiAccountTreeVO> children;
}








