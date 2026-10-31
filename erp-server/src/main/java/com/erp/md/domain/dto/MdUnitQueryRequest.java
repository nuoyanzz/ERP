package com.erp.md.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MdUnitQueryRequest extends PageQuery {

    private String unitCode;
    private String unitName;
    private Integer status;
}
