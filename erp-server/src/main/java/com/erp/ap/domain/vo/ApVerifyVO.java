package com.erp.ap.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApVerifyVO {

    private Long id;
    private Long payableId;
    private String payableNo;
    private BigDecimal amount;
}








