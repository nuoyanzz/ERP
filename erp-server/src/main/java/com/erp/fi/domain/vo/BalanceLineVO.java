package com.erp.fi.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BalanceLineVO {

    private String accountCode;
    private String accountName;

    /** 期初余额（借方为正） */
    private BigDecimal openingDebit;
    /** 期初余额（贷方为正） */
    private BigDecimal openingCredit;

    /** 本期借方发生额 */
    private BigDecimal periodDebit;
    /** 本期贷方发生额 */
    private BigDecimal periodCredit;

    /** 期末余额（借方为正） */
    private BigDecimal closingDebit;
    /** 期末余额（贷方为正） */
    private BigDecimal closingCredit;

    /** 期末净额，按科目类别取符号后为正 */
    private BigDecimal balance;
}








