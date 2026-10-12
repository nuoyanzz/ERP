package com.erp.sa.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class SaOrderQueryRequest extends PageQuery {

    private String orderNo;
    private Long customerId;
    private Integer status;
    private LocalDate startDate;
    private LocalDate endDate;
}








