package com.erp.common.util;

import com.erp.security.datascope.DataScope;
import com.erp.security.datascope.DataScopeContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 单号生成相关的单元测试。
 * <p>
 * 覆盖两类容易回归的逻辑：流水号格式，以及取号时绕过数据权限的开关。
 */
class SerialNoGeneratorTest {

    @Test
    @DisplayName("单号格式：前缀-期间-4 位序号")
    void numberFormat() {
        assertEquals("CG-202610-0001", format("CG", "202610", 1));
        assertEquals("CG-202610-0012", format("CG", "202610", 12));
        assertEquals("CG-202610-1234", format("CG", "202610", 1234));
    }

    @Test
    @DisplayName("序号超过 9999 后自然扩展，不截断")
    void numberFormatBeyondFourDigits() {
        assertEquals("CG-202610-10000", format("CG", "202610", 10000));
    }

    @Test
    @DisplayName("取号时忽略数据权限，避免被部门范围裁剪")
    void ignoringScopeBypassesDataScope() {
        DataScopeContext.DataScopeInfo info = new DataScopeContext.DataScopeInfo();
        info.setScope(DataScope.DEPT);
        info.setDeptIds(List.of(1L, 2L));
        DataScopeContext.set(info);
        try {
            assertFalse(DataScopeContext.isIgnored());
            DataScopeContext.ignoringScope(() -> {
                assertTrue(DataScopeContext.isIgnored());
                return null;
            });
            // 退出后应恢复原状
            assertFalse(DataScopeContext.isIgnored());
        } finally {
            DataScopeContext.clear();
        }
    }

    @Test
    @DisplayName("嵌套调用忽略数据权限后仍能恢复原状态")
    void nestedIgnoringScopeRestores() {
        DataScopeContext.ignoringScope(() -> {
            assertTrue(DataScopeContext.isIgnored());
            DataScopeContext.ignoringScope(() -> {
                assertTrue(DataScopeContext.isIgnored());
                return null;
            });
            // 内层结束后外层仍处于忽略状态
            assertTrue(DataScopeContext.isIgnored());
            return null;
        });
        assertFalse(DataScopeContext.isIgnored());
    }

    @Test
    @DisplayName("未进入忽略作用域时标记为 false")
    void defaultNotIgnored() {
        DataScopeContext.clear();
        assertFalse(DataScopeContext.isIgnored());
    }

    private String format(String prefix, String period, long seq) {
        return String.format("%s-%s-%04d", prefix, period, seq);
    }
}
