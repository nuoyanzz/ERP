package com.erp.sa.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class SaOutstockQueryRequest extends PageQuery {

    private String outstockNo;
    private Long orderId;
    private Long customerId;
    private Long warehouseId;
    private Integer status;
    private Integer voucherStatus;
    private LocalDate startDate;
    private LocalDate endDate;
}








