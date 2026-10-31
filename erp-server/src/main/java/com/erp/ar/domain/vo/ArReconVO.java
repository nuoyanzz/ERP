package com.erp.ar.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 应收往来对账结果。
 * <p>
 * 口径：应收单余额合计（未结清应收的未收金额）与总账 1122「应收账款」科目的
 * 客户辅助核算余额对比，差额即对账差异。
 */
@Data
public class ArReconVO {

    private Long customerId;
    private String customerName;
    /** 应收单余额合计 */
    private BigDecimal receivableBalance;
    /** 总账 1122 科目该客户的辅助核算余额 */
    private BigDecimal ledgerBalance;
    /** 差异 = 应收单余额 - 总账余额 */
    private BigDecimal difference;
    /** 是否账实相符 */
    private boolean checked;
}













