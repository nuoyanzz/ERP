package com.erp.hr.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 社保与个税政策。定义社保缴纳比例、公积金比例与个税预扣预缴税率表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_social_policy")
public class HrSocialPolicy extends BaseEntity {

    private String policyName;
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
    /** 速算扣除数（首档，与 quickDeduction 同义，保留兼容） */
    private BigDecimal quickDeduction;
    /** 各档速算扣除数，与 taxRate1~7 一一对应 */
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








