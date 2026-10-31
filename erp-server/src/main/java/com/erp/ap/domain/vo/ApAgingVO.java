package com.erp.ap.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 应付账龄分析结果。按供应商汇总未结清应付余额，并按到期日分桶。
 */
@Data
public class ApAgingVO {

    private Long supplierId;
    private String supplierName;
    /** 未到期 */
    private BigDecimal notDue;
    /** 逾期 30 天内（含 30 天） */
    private BigDecimal within30;
    /** 逾期 31-60 天 */
    private BigDecimal within60;
    /** 逾期 61-90 天 */
    private BigDecimal within90;
    /** 逾期 90 天以上 */
    private BigDecimal over90;
}











