package com.erp.hr.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 社保个税政策录入请求。
 */
@Data
public class SocialPolicySaveRequest {

    private Long id;

    @NotBlank(message = "政策名称不能为空")
    private String policyName;

    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveDate;

    /** 社保个人缴纳比例（百分数） */
    private BigDecimal socialRate;

    /** 公积金个人缴纳比例（百分数） */
    private BigDecimal fundRate;

    /** 个税起征点 */
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
