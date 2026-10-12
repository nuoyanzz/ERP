package com.erp.pa.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaInstockQueryRequest extends PageQuery {

    private String instockNo;
    private Long orderId;
    private Long supplierId;
    private Long warehouseId;
    private Integer status;
    private Integer voucherStatus;
    private LocalDate startDate;
    private LocalDate endDate;
}








