package com.erp.ar.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ArVerifyVO {

    private Long id;
    private Long receivableId;
    private String receivableNo;
    private BigDecimal amount;
}








