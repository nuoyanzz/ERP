package com.erp.fi.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FiAccountBalanceVO {

    private Long accountId;
    private String subjectCode;
    private String subjectName;
    private Integer category;
    private Integer balanceDir;
    private BigDecimal openingDebit;
    private BigDecimal openingCredit;
    private BigDecimal periodDebit;
    private BigDecimal periodCredit;
    private BigDecimal closingDebit;
    private BigDecimal closingCredit;
}








