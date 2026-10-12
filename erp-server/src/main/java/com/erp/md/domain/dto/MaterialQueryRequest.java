package com.erp.md.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialQueryRequest extends PageQuery {

    private String materialCode;
    private String materialName;
    private Long categoryId;
    private Integer status;
}








