package com.erp.md.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class MaterialCategoryTreeVO {

    private Long id;
    private String categoryCode;
    private String categoryName;
    private Long parentId;
    private Integer sortNo;
    private Integer status;
    private String remark;
    private List<MaterialCategoryTreeVO> children;
}








