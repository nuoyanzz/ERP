package com.erp.ar.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ArReceivableQueryRequest extends PageQuery {

    private Long customerId;
    private Integer status;
    private String sourceNo;
}








