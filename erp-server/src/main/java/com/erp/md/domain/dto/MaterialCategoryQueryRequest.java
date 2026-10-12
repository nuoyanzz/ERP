package com.erp.md.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialCategoryQueryRequest extends PageQuery {

    private String categoryCode;
    private String categoryName;
    private Integer status;
}








