package com.erp.ap.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApPayableQueryRequest extends PageQuery {

    private Long supplierId;
    private Integer status;
    private String sourceNo;
}








