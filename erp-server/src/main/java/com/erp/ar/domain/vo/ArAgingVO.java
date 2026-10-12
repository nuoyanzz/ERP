package com.erp.ar.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ArAgingVO {

    private Long customerId;
    private String customerName;
    private BigDecimal total;
    private BigDecimal notDue;
    private BigDecimal within30;
    private BigDecimal within60;
    private BigDecimal within90;
    private BigDecimal over90;
}








