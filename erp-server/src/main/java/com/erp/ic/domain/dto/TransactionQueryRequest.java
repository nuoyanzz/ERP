package com.erp.ic.domain.dto;

import com.erp.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class TransactionQueryRequest extends PageQuery {

    private Long materialId;
    private String tranType;
    private String bizNo;
    private Integer direction;
    private LocalDate startDate;
    private LocalDate endDate;
}








