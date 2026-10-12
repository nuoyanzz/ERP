package com.erp.pm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MrpSuggestionVO {

    private Long id;
    private String runNo;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    private Integer suggestType;
    private BigDecimal grossQty;
    private BigDecimal availableQty;
    private BigDecimal onOrderQty;
    private BigDecimal safeStock;
    private BigDecimal netQty;

    /** 按批量/最小订购量取整后的计划下单数量 */
    private BigDecimal plannedQty;

    private Long bomId;
    private String bomNo;

    /** 建议下单日期 = 需求日 − 提前期 */
    private LocalDate suggestDate;

    /** 提前期（天） */
    private Integer leadTimeDays;

    /** 参考单价 */
    private BigDecimal unitCost;

    /** 投放状态：0 未投放，1 已投放 */
    private Integer releaseStatus;

    /** 投放生成的单据 ID */
    private Long releasedOrderId;
}








