package com.erp.wf.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 审批回调注册表单元测试。
 */
class ApprovalCallbackRegistryTest {

    @Test
    @DisplayName("按 bizType 建立索引并正确分发")
    void registryIndexesByBizType() {
        BizApprovalListener paListener = new RecordingListener("PA_ORDER");
        BizApprovalListener saListener = new RecordingListener("SA_ORDER");

        ApprovalCallbackRegistry registry = new ApprovalCallbackRegistry(List.of(paListener, saListener));

        assertSame(paListener, registry.find("PA_ORDER"));
        assertSame(saListener, registry.find("SA_ORDER"));
        assertTrue(registry.supports("PA_ORDER"));
        assertFalse(registry.supports("IC_CHECK"));
    }

    @Test
    @DisplayName("未注册的业务类型返回 null")
    void registryReturnsNullForUnknownBizType() {
        ApprovalCallbackRegistry registry = new ApprovalCallbackRegistry(List.of());

        assertNull(registry.find("PM_MO"));
        assertNull(registry.find(null));
        assertFalse(registry.supports("PM_MO"));
    }

    @Test
    @DisplayName("bizType 为空的监听器被忽略")
    void registryIgnoresBlankBizType() {
        ApprovalCallbackRegistry registry = new ApprovalCallbackRegistry(List.of(new RecordingListener("  ")));

        assertNull(registry.find("  "));
    }

    /**
     * 记录型监听器，仅用于测试分发。
     */
    private static final class RecordingListener implements BizApprovalListener {

        private final String bizType;

        private RecordingListener(String bizType) {
            this.bizType = bizType;
        }

        @Override
        public String bizType() {
            return bizType;
        }

        @Override
        public void onApproved(Long bizId, Map<String, Object> vars) {
            // 仅记录分发，不做断言
        }

        @Override
        public void onRejected(Long bizId, String reason, Map<String, Object> vars) {
            // 仅记录分发，不做断言
        }
    }

    @Test
    @DisplayName("监听器实现可被正常构造")
    void listenerCanBeConstructed() {
        assertNotNull(new RecordingListener("PA_ORDER"));
    }
}








