package com.erp.md.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SupplierQueryRequest extends PageQuery {

    private String supplierCode;
    private String supplierName;
    private Integer status;
}








