package com.erp.pm.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_mrp_result")
public class PmMrpResult extends BaseEntity {

    public static final int SUGGEST_PURCHASE = 1;
    public static final int SUGGEST_PRODUCE = 2;

    public static final int RELEASE_NONE = 0;
    public static final int RELEASE_RELEASED = 1;

    private String runNo;
    private Long materialId;
    private Integer suggestType;
    private BigDecimal grossQty;
    private BigDecimal availableQty;
    private BigDecimal onOrderQty;
    private BigDecimal safeStock;
    private BigDecimal netQty;
    private Long bomId;

    /** 按批量/最小订购量取整后的计划下单数量 */
    private BigDecimal plannedQty;

    /** 建议下单日期 = 需求日 - 提前期 */
    private LocalDate suggestDate;

    /** 投放状态：0 未投放，1 已投放 */
    private Integer releaseStatus;

    /** 投放后生成的单据 ID（采购订单或生产订单） */
    private Long releasedOrderId;

    /** 采购单价（采购建议时按物料成本价/参考价填充） */
    private BigDecimal unitCost;

    /** 提前期（天） */
    private Integer leadTimeDays;
}








