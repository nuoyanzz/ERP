package com.erp.fi.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class VoucherQueryRequest extends PageQuery {

    private String voucherNo;
    private String period;
    private Integer status;
    private LocalDate startDate;
    private LocalDate endDate;
}








