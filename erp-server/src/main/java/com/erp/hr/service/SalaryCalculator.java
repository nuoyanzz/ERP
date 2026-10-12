package com.erp.hr.service;

import com.erp.hr.domain.entity.HrSocialPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 薪资计算器。
 * <p>
 * 负责个人所得税预扣预缴与社保公积金的个人/单位分担计算。
 * 个税采用综合所得累计预扣法：应纳税所得额 = 累计收入 - 累计免税减除 - 累计扣除。
 */
public final class SalaryCalculator {

    /** 金额保留 2 位小数 */
    public static final int SCALE = 2;

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private SalaryCalculator() {
    }

    /**
     * 社保公积金个人缴纳额。
     *
     * @param socialBase 社保基数
     * @param fundBase   公积金基数
     * @param policy     政策
     * @return 个人应缴社保与公积金合计
     */
    public static BigDecimal personalContribution(BigDecimal socialBase, BigDecimal fundBase,
                                                  HrSocialPolicy policy) {
        if (policy == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal social = socialBase == null ? BigDecimal.ZERO : socialBase;
        BigDecimal fund = fundBase == null ? BigDecimal.ZERO : fundBase;
        BigDecimal socialAmount = social.multiply(policy.getSocialRate())
                .divide(ONE_HUNDRED, SCALE, RoundingMode.HALF_UP);
        BigDecimal fundAmount = fund.multiply(policy.getFundRate())
                .divide(ONE_HUNDRED, SCALE, RoundingMode.HALF_UP);
        return socialAmount.add(fundAmount).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 社保公积金单位承担部分。
     * <p>
     * 单位缴纳比例通常高于个人比例，差额部分由单位承担，形成企业成本。
     */
    public static BigDecimal companyContribution(BigDecimal socialBase, BigDecimal fundBase,
                                                 HrSocialPolicy policy) {
        if (policy == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal social = socialBase == null ? BigDecimal.ZERO : socialBase;
        BigDecimal fund = fundBase == null ? BigDecimal.ZERO : fundBase;
        BigDecimal socialAmount = social.multiply(policy.getSocialRate())
                .divide(ONE_HUNDRED, SCALE, RoundingMode.HALF_UP);
        BigDecimal fundAmount = fund.multiply(policy.getFundRate())
                .divide(ONE_HUNDRED, SCALE, RoundingMode.HALF_UP);
        return socialAmount.add(fundAmount).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 计算个人所得税（累计预扣法）。
     *
     * @param cumulativeIncome     累计收入
     * @param cumulativeDeduction 累计减除费用（起征点按月累计，即 5000 × 已发月份）
     * @param cumulativeSpecial    累计专项扣除（社保公积金个人部分等）
     * @param policy               政策
     * @return 本期应预扣个税
     */
    public static BigDecimal individualTax(BigDecimal cumulativeIncome, BigDecimal cumulativeDeduction,
                                           BigDecimal cumulativeSpecial, HrSocialPolicy policy) {
        if (policy == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal income = cumulativeIncome == null ? BigDecimal.ZERO : cumulativeIncome;
        BigDecimal deduction = cumulativeDeduction == null ? BigDecimal.ZERO : cumulativeDeduction;
        BigDecimal special = cumulativeSpecial == null ? BigDecimal.ZERO : cumulativeSpecial;

        BigDecimal taxable = income.subtract(deduction).subtract(special);
        // 起征点内的部分不纳税，只有超过起征点的部分才计税
        if (taxable.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal rate;
        BigDecimal quickDeduction;
        if (taxable.compareTo(new BigDecimal("36000")) <= 0) {
            rate = policy.getTaxRate1();
            // 首档兼容旧的 quickDeduction 字段
            quickDeduction = policy.getTaxDeduction1() != null
                    ? policy.getTaxDeduction1() : zeroIfNull(policy.getQuickDeduction());
        } else if (taxable.compareTo(new BigDecimal("144000")) <= 0) {
            rate = policy.getTaxRate2();
            quickDeduction = zeroIfNull(policy.getTaxDeduction2());
        } else if (taxable.compareTo(new BigDecimal("300000")) <= 0) {
            rate = policy.getTaxRate3();
            quickDeduction = zeroIfNull(policy.getTaxDeduction3());
        } else if (taxable.compareTo(new BigDecimal("420000")) <= 0) {
            rate = policy.getTaxRate4();
            quickDeduction = zeroIfNull(policy.getTaxDeduction4());
        } else if (taxable.compareTo(new BigDecimal("660000")) <= 0) {
            rate = policy.getTaxRate5();
            quickDeduction = zeroIfNull(policy.getTaxDeduction5());
        } else if (taxable.compareTo(new BigDecimal("960000")) <= 0) {
            rate = policy.getTaxRate6();
            quickDeduction = zeroIfNull(policy.getTaxDeduction6());
        } else {
            rate = policy.getTaxRate7();
            quickDeduction = zeroIfNull(policy.getTaxDeduction7());
        }

        BigDecimal tax = taxable.multiply(rate).divide(ONE_HUNDRED, SCALE, RoundingMode.HALF_UP)
                .subtract(quickDeduction);
        return tax.max(BigDecimal.ZERO).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}








