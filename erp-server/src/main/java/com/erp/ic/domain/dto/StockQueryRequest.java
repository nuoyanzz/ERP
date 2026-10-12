package com.erp.ic.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StockQueryRequest extends PageQuery {

    private String materialCode;
    private String materialName;
    private Long warehouseId;
}








