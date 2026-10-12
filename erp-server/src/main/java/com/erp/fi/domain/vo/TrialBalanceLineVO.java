package com.erp.fi.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 试算平衡表明细行。
 */
@Data
public class TrialBalanceLineVO {

    private Long accountId;
    private String accountCode;
    private String accountName;

    private BigDecimal openingDebit;
    private BigDecimal openingCredit;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private BigDecimal closingDebit;
    private BigDecimal closingCredit;
}








