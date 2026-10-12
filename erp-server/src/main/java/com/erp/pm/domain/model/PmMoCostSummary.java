package com.erp.pm.domain.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 工单成本构成汇总。
 */
@Data
public class PmMoCostSummary {

    private Long moId;

    /** 材料成本（累计领料成本） */
    private BigDecimal materialCost;

    /** 人工成本 */
    private BigDecimal laborCost;

    /** 制造费用 */
    private BigDecimal overheadCost;

    /** 其他成本 */
    private BigDecimal otherCost;

    /** 合计成本 */
    private BigDecimal totalCost;

    /** 单位成本 = 合计成本 ÷ 累计完工量 */
    private BigDecimal unitCost;

    /** 累计完工量 */
    private BigDecimal producedQty;

    /** 订单数量 */
    private BigDecimal orderQty;
}








