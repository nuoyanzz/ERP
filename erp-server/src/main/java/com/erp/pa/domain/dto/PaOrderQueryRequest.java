package com.erp.pa.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaOrderQueryRequest extends PageQuery {

    private String orderNo;
    private Long supplierId;
    private Integer status;
    private LocalDate startDate;
    private LocalDate endDate;
}








