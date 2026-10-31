package com.erp.md.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MdUnitConvertQueryRequest extends PageQuery {

    private Long materialId;
    private String fromUnit;
    private String toUnit;
}
