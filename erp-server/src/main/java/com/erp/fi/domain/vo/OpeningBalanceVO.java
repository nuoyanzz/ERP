package com.erp.fi.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 期初余额展示对象。
 */
@Data
public class OpeningBalanceVO {

    private Long id;
    private Long accountId;
    private String accountCode;
    private String accountName;
    private String period;
    private String currency;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private String auxType;
    private Long auxId;
}








