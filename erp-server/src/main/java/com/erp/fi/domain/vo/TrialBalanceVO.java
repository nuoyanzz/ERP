package com.erp.fi.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 试算平衡表。
 */
@Data
public class TrialBalanceVO {

    private String period;
    /** 明细行 */
    private List<TrialBalanceLineVO> lines;

    /** 期初借方合计 */
    private BigDecimal openingDebitTotal;
    /** 期初贷方合计 */
    private BigDecimal openingCreditTotal;
    /** 本期借方合计 */
    private BigDecimal debitTotal;
    /** 本期贷方合计 */
    private BigDecimal creditTotal;
    /** 期末借方合计 */
    private BigDecimal closingDebitTotal;
    /** 期末贷方合计 */
    private BigDecimal closingCreditTotal;

    /** 是否平衡 */
    private boolean balanced;
    /** 差额 */
    private BigDecimal difference;
}








