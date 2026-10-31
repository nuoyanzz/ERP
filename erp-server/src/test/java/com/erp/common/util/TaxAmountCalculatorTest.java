package com.erp.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaxAmountCalculatorTest {

    @Test
    @DisplayName("价税分离：数量 × 单价 × 税率")
    void splitWithTax() {
        BigDecimal[] result = TaxAmountCalculator.split(
                new BigDecimal("10"), new BigDecimal("100"), new BigDecimal("13"));
        assertEquals(0, new BigDecimal("1000.00").compareTo(result[0]));
        assertEquals(0, new BigDecimal("130.00").compareTo(result[1]));
        assertEquals(0, new BigDecimal("1130.00").compareTo(result[2]));
    }

    @Test
    @DisplayName("零税率：税额为 0")
    void splitZeroTax() {
        BigDecimal[] result = TaxAmountCalculator.split(
                new BigDecimal("5"), new BigDecimal("20"), BigDecimal.ZERO);
        assertEquals(0, BigDecimal.ZERO.compareTo(result[1]));
        assertEquals(0, new BigDecimal("100.00").compareTo(result[2]));
    }

    @Test
    @DisplayName("从价税合计反推：100 元含 13% 税")
    void splitFromTotal() {
        BigDecimal[] result = TaxAmountCalculator.splitFromTotal(
                new BigDecimal("113"), new BigDecimal("13"));
        assertEquals(0, new BigDecimal("100.00").compareTo(result[0]));
        assertEquals(0, new BigDecimal("13.00").compareTo(result[1]));
    }

    @Test
    @DisplayName("反推后税额与合计严格相等")
    void splitFromTotalConsistency() {
        BigDecimal total = new BigDecimal("5678.90");
        BigDecimal[] result = TaxAmountCalculator.splitFromTotal(total, new BigDecimal("13"));
        assertEquals(0, total.compareTo(result[0].add(result[1])));
    }

    @Test
    @DisplayName("空值按零处理")
    void nullSafe() {
        BigDecimal[] result = TaxAmountCalculator.split(null, null, null);
        assertEquals(0, BigDecimal.ZERO.compareTo(result[0]));
        assertEquals(0, BigDecimal.ZERO.compareTo(result[1]));
        assertEquals(0, BigDecimal.ZERO.compareTo(result[2]));
    }

    @Test
    @DisplayName("不含税金额计算")
    void netAmount() {
        BigDecimal result = TaxAmountCalculator.netAmount(new BigDecimal("3"), new BigDecimal("12.34"));
        assertEquals(0, new BigDecimal("37.02").compareTo(result));
    }
}








