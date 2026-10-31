package com.erp.common.expression;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionEvaluatorTest {

    private Map<String, Object> context() {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("amount", 60000);
        ctx.put("quantity", 10);
        ctx.put("type", "采购");
        ctx.put("level", "VIP");
        ctx.put("deptId", 96014L);
        ctx.put("flag", true);
        ctx.put("ratio", 0.15);
        return ctx;
    }

    @Test
    @DisplayName("数值比较：大于、小于、大于等于")
    void numericComparison() {
        Map<String, Object> ctx = context();
        assertTrue(ExpressionEvaluator.evaluate("amount > 50000", ctx));
        assertFalse(ExpressionEvaluator.evaluate("amount > 100000", ctx));
        assertTrue(ExpressionEvaluator.evaluate("amount >= 60000", ctx));
        assertTrue(ExpressionEvaluator.evaluate("quantity < 100", ctx));
        assertTrue(ExpressionEvaluator.evaluate("amount != 1", ctx));
    }

    @Test
    @DisplayName("字符串比较")
    void stringComparison() {
        Map<String, Object> ctx = context();
        assertTrue(ExpressionEvaluator.evaluate("type == '采购'", ctx));
        assertFalse(ExpressionEvaluator.evaluate("type == '销售'", ctx));
        assertTrue(ExpressionEvaluator.evaluate("level != '普通'", ctx));
    }

    @Test
    @DisplayName("逻辑与：两个条件都成立")
    void logicalAnd() {
        Map<String, Object> ctx = context();
        assertTrue(ExpressionEvaluator.evaluate("amount > 50000 && type == '采购'", ctx));
        assertFalse(ExpressionEvaluator.evaluate("amount > 50000 && type == '销售'", ctx));
        assertTrue(ExpressionEvaluator.evaluate("amount > 50000 and type == '采购'", ctx));
    }

    @Test
    @DisplayName("逻辑或：任一条件成立")
    void logicalOr() {
        Map<String, Object> ctx = context();
        assertTrue(ExpressionEvaluator.evaluate("amount > 100000 || type == '采购'", ctx));
        assertTrue(ExpressionEvaluator.evaluate("amount > 100000 or type == '采购'", ctx));
        assertFalse(ExpressionEvaluator.evaluate("amount > 100000 || type == '销售'", ctx));
    }

    @Test
    @DisplayName("逻辑非与括号分组")
    void negationAndGrouping() {
        Map<String, Object> ctx = context();
        assertTrue(ExpressionEvaluator.evaluate("!(amount > 100000)", ctx));
        assertTrue(ExpressionEvaluator.evaluate("not amount > 100000", ctx));
        assertTrue(ExpressionEvaluator.evaluate("(amount > 50000 || type == '销售') && level == 'VIP'", ctx));
    }

    @Test
    @DisplayName("缺失变量按 null 处理，条件不成立")
    void missingVariable() {
        assertFalse(ExpressionEvaluator.evaluate("unknownField > 100", context()));
        assertFalse(ExpressionEvaluator.evaluate("unknownField == 'x'", context()));
    }

    @Test
    @DisplayName("取值：返回变量原始值")
    void evaluateValue() {
        Map<String, Object> ctx = context();
        assertEquals(60000, ExpressionEvaluator.evaluateValue("amount", ctx));
        assertEquals(60000, ExpressionEvaluator.evaluateValue("${amount}", ctx));
        assertEquals("采购", ExpressionEvaluator.evaluateValue("type", ctx));
        assertEquals(96014L, ExpressionEvaluator.evaluateValue("deptId", ctx));
    }

    @Test
    @DisplayName("小数比较")
    void decimalComparison() {
        Map<String, Object> ctx = context();
        assertTrue(ExpressionEvaluator.evaluate("ratio > 0.1", ctx));
        assertFalse(ExpressionEvaluator.evaluate("ratio > 0.2", ctx));
    }

    @Test
    @DisplayName("布尔变量")
    void booleanValue() {
        Map<String, Object> ctx = context();
        assertTrue(ExpressionEvaluator.evaluate("flag", ctx));
    }

    @Test
    @DisplayName("空表达式视为条件成立")
    void blankExpression() {
        assertTrue(ExpressionEvaluator.evaluate(null, context()));
        assertTrue(ExpressionEvaluator.evaluate("  ", context()));
    }
}








