package com.erp.md.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseQueryRequest extends PageQuery {

    private String warehouseCode;
    private String warehouseName;
    private Integer whType;
    private Integer status;
}








