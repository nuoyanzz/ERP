package com.erp.hr.service;

import com.erp.hr.domain.entity.HrSocialPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SalaryCalculatorTest {

    private HrSocialPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new HrSocialPolicy();
        policy.setPolicyName("2026年度政策");
        policy.setEffectiveDate(LocalDate.of(2026, 1, 1));
        policy.setSocialRate(new BigDecimal("10.5"));
        policy.setFundRate(new BigDecimal("7"));
        policy.setTaxThreshold(new BigDecimal("5000"));
        policy.setTaxRate1(new BigDecimal("3"));
        policy.setTaxRate2(new BigDecimal("10"));
        policy.setTaxRate3(new BigDecimal("20"));
        policy.setTaxRate4(new BigDecimal("25"));
        policy.setTaxRate5(new BigDecimal("30"));
        policy.setTaxRate6(new BigDecimal("35"));
        policy.setTaxRate7(new BigDecimal("45"));
        // 综合所得税率表对应的速算扣除数
        policy.setTaxDeduction1(BigDecimal.ZERO);
        policy.setTaxDeduction2(new BigDecimal("2520"));
        policy.setTaxDeduction3(new BigDecimal("16920"));
        policy.setTaxDeduction4(new BigDecimal("31920"));
        policy.setTaxDeduction5(new BigDecimal("52920"));
        policy.setTaxDeduction6(new BigDecimal("85920"));
        policy.setTaxDeduction7(new BigDecimal("181920"));
        policy.setQuickDeduction(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("社保公积金个人缴纳：基数 × 比例")
    void personalContribution() {
        BigDecimal result = SalaryCalculator.personalContribution(
                new BigDecimal("10000"), new BigDecimal("10000"), policy);
        // 10000 × 10.5% + 10000 × 7% = 1050 + 700 = 1750
        assertEquals(0, new BigDecimal("1750.00").compareTo(result));
    }

    @Test
    @DisplayName("社保公积金单位承担")
    void companyContribution() {
        BigDecimal result = SalaryCalculator.companyContribution(
                new BigDecimal("20000"), new BigDecimal("20000"), policy);
        // 20000 × 10.5% + 20000 × 7% = 2100 + 1400 = 3500
        assertEquals(0, new BigDecimal("3500.00").compareTo(result));
    }

    @Test
    @DisplayName("个税：应纳税所得额为负时不扣税")
    void taxBelowThreshold() {
        // 累计收入 4000 低于起征点 5000，不产生个税
        BigDecimal result = SalaryCalculator.individualTax(
                new BigDecimal("4000"), new BigDecimal("5000"), BigDecimal.ZERO, policy);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    @DisplayName("个税：应纳税所得额刚好达到起征点时按 3% 计税")
    void taxAtThreshold() {
        // 10000 - 5000 = 5000，× 3% = 150
        BigDecimal result = SalaryCalculator.individualTax(
                new BigDecimal("10000"), new BigDecimal("5000"), BigDecimal.ZERO, policy);
        assertEquals(0, new BigDecimal("150.00").compareTo(result));
    }

    @Test
    @DisplayName("个税：首档 3%，速算扣除数为 0")
    void taxFirstBracket() {
        // 应纳税所得额 = 30000 - 5000 = 25000，落在 3% 档
        BigDecimal result = SalaryCalculator.individualTax(
                new BigDecimal("30000"), new BigDecimal("5000"), BigDecimal.ZERO, policy);
        assertEquals(0, new BigDecimal("750.00").compareTo(result));
    }

    @Test
    @DisplayName("个税：专项扣除可减少应纳税所得额")
    void taxWithSpecialDeduction() {
        BigDecimal result = SalaryCalculator.individualTax(
                new BigDecimal("30000"), new BigDecimal("5000"), new BigDecimal("5000"), policy);
        // 应纳税所得额 = 30000 - 5000 - 5000 = 20000，× 3% = 600
        assertEquals(0, new BigDecimal("600.00").compareTo(result));
    }

    @Test
    @DisplayName("个税：累计预扣法随累计收入增加而增加")
    void taxAccumulative() {
        // 累计收入 30000，应纳税所得额 25000 → 750
        BigDecimal first = SalaryCalculator.individualTax(
                new BigDecimal("30000"), new BigDecimal("5000"), BigDecimal.ZERO, policy);
        // 累计收入 60000，应纳税所得额 55000 → 55000×10% - 2520 = 2980
        BigDecimal second = SalaryCalculator.individualTax(
                new BigDecimal("60000"), new BigDecimal("5000"), BigDecimal.ZERO, policy);
        assertEquals(0, new BigDecimal("750.00").compareTo(first));
        assertEquals(0, new BigDecimal("2980.00").compareTo(second));
    }

    @Test
    @DisplayName("个税：第二档使用 10% 税率与对应速算扣除数")
    void taxSecondBracket() {
        // 应纳税所得额 100000 → 100000×10% - 2520 = 7480
        BigDecimal result = SalaryCalculator.individualTax(
                new BigDecimal("105000"), new BigDecimal("5000"), BigDecimal.ZERO, policy);
        assertEquals(0, new BigDecimal("7480.00").compareTo(result));
    }

    @Test
    @DisplayName("无政策时不产生扣款")
    void nullPolicy() {
        assertEquals(0, BigDecimal.ZERO.compareTo(
                SalaryCalculator.personalContribution(new BigDecimal("10000"), new BigDecimal("10000"), null)));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                SalaryCalculator.individualTax(new BigDecimal("100000"), BigDecimal.ZERO, BigDecimal.ZERO, null)));
    }
}








