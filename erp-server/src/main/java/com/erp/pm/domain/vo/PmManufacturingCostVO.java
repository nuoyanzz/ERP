package com.erp.pm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 成本归集明细。
 */
@Data
public class PmManufacturingCostVO {

    private Long id;
    private Long moId;
    private String moNo;
    private String materialName;

    /** 成本类型：1 人工，2 制造费用，3 其他 */
    private Integer costType;
    private String costTypeName;

    private LocalDate costDate;
    private BigDecimal laborAmount;
    private BigDecimal overheadAmount;
    private BigDecimal otherAmount;
    private BigDecimal totalAmount;

    private String sourceType;
    private Long sourceId;
    private String remark;
    private LocalDateTime createTime;
}








