package com.erp.pm.domain.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 本次完工应结转的成本构成。
 */
@Data
public class PmFinishCostAllocation {

    /** 材料成本（按累计完工产量比例从累计领料成本中结转） */
    private BigDecimal materialCost = BigDecimal.ZERO;

    /** 人工成本 */
    private BigDecimal laborCost = BigDecimal.ZERO;

    /** 制造费用 */
    private BigDecimal overheadCost = BigDecimal.ZERO;

    /** 本次完工总成本 */
    private BigDecimal totalCost = BigDecimal.ZERO;

    /** 单位成本 = 总成本 ÷ 本次完工数量 */
    private BigDecimal unitCost = BigDecimal.ZERO;
}








