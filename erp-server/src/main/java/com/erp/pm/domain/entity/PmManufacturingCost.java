package com.erp.pm.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 生产成本归集明细：一行代表一笔归集到某个工单的成本。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_manufacturing_cost")
public class PmManufacturingCost extends BaseEntity {

    public static final int TYPE_LABOR = 1;
    public static final int TYPE_OVERHEAD = 2;
    public static final int TYPE_OTHER = 3;

    private Long moId;

    /** 成本类型：1 人工，2 制造费用，3 其他 */
    private Integer costType;

    private LocalDate costDate;

    private BigDecimal laborAmount;
    private BigDecimal overheadAmount;
    private BigDecimal otherAmount;

    /** 合计金额 */
    private BigDecimal totalAmount;

    /** 来源类型（如 REPORT_WORK / MANUAL） */
    private String sourceType;

    /** 来源单据 ID */
    private Long sourceId;

    private String remark;
}








