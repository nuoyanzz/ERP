package com.erp.pa.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaReturnQueryRequest extends PageQuery {

    private String returnNo;
    private Long sourceInstockId;
    private Long supplierId;
    private Integer status;
    private LocalDate startDate;
    private LocalDate endDate;
}








