package com.erp.pm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工单成本汇总（成本归集页的主表行）。
 */
@Data
public class PmMoCostVO {

    private Long moId;
    private String moNo;
    private Long materialId;
    private String materialCode;
    private String materialName;

    /** 订单数量 */
    private BigDecimal orderQty;

    /** 累计完工量 */
    private BigDecimal producedQty;

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

    /** 成本明细 */
    private List<PmManufacturingCostVO> details;
}








