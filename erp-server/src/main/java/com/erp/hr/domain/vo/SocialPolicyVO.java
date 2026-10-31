package com.erp.hr.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 社保个税政策展示对象。
 */
@Data
public class SocialPolicyVO {

    private Long id;
    private String policyName;
    private LocalDate effectiveDate;
    private BigDecimal socialRate;
    private BigDecimal fundRate;
    private BigDecimal taxThreshold;
    private BigDecimal taxRate1;
    private BigDecimal taxRate2;
    private BigDecimal taxRate3;
    private BigDecimal taxRate4;
    private BigDecimal taxRate5;
    private BigDecimal taxRate6;
    private BigDecimal taxRate7;
    private BigDecimal taxDeduction1;
    private BigDecimal taxDeduction2;
    private BigDecimal taxDeduction3;
    private BigDecimal taxDeduction4;
    private BigDecimal taxDeduction5;
    private BigDecimal taxDeduction6;
    private BigDecimal taxDeduction7;
    private Integer status;
    private String remark;
}
