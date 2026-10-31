package com.erp.wf.integration;

import com.erp.wf.domain.entity.WfInstance;
import com.erp.wf.domain.entity.WfTaskRecord;
import com.erp.wf.engine.WfEngine;
import com.erp.wf.mapper.WfInstanceMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 审批回调分发单元测试。
 * <p>
 * 重点验证回调重入保护：业务监听器在回调里再次调用业务 Service 的审核方法，
 * 而该方法开头又会发起流程，若无保护会对同一张单据重复发起流程。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApprovalDispatcherTest {

    @Mock
    private WfInstanceMapper instanceMapper;
    @Mock
    private WfEngine engine;

    @Test
    @DisplayName("回调期间禁止再次发起流程，退出后恢复")
    void inCallbackFlagIsRestoredAfterDispatch() {
        BizApprovalListener listener = new BizApprovalListener() {
            @Override
            public String bizType() {
                return "PA_ORDER";
            }

            @Override
            public void onApproved(Long bizId, Map<String, Object> vars) {
                // 回调内部应处于「回调中」状态
                assertTrue(ApprovalDispatcher.inCallback());
            }

            @Override
            public void onRejected(Long bizId, String reason, Map<String, Object> vars) {
                assertTrue(ApprovalDispatcher.inCallback());
            }
        };

        ApprovalDispatcher dispatcher = buildDispatcher(listener);
        WfInstance instance = instance(1L, WfInstance.STATUS_APPROVED);

        assertFalse(ApprovalDispatcher.inCallback());
        dispatcher.dispatchApproved(instance.getId(), "通过");
        // 回调结束后应恢复为「非回调中」
        assertFalse(ApprovalDispatcher.inCallback());
    }

    @Test
    @DisplayName("实例仍在运行时不分发回调")
    void runningInstanceIsNotDispatched() {
        List<String> calls = new ArrayList<>();
        BizApprovalListener listener = recordingListener(calls);

        ApprovalDispatcher dispatcher = buildDispatcher(listener);
        dispatcher.dispatchByInstanceState(instance(1L, WfInstance.STATUS_RUNNING).getId());

        assertEquals(0, calls.size());
    }

    @Test
    @DisplayName("挂起状态不分发回调——流程仍可能恢复")
    void suspendedInstanceIsNotDispatched() {
        List<String> calls = new ArrayList<>();
        BizApprovalListener listener = recordingListener(calls);

        ApprovalDispatcher dispatcher = buildDispatcher(listener);
        dispatcher.dispatchByInstanceState(instance(1L, WfInstance.STATUS_SUSPENDED).getId());

        assertEquals(0, calls.size());
    }

    @Test
    @DisplayName("实例已通过时回调 onApproved")
    void approvedInstanceCallsOnApproved() {
        List<String> calls = new ArrayList<>();
        BizApprovalListener listener = recordingListener(calls);

        ApprovalDispatcher dispatcher = buildDispatcher(listener);
        dispatcher.dispatchByInstanceState(instance(1L, WfInstance.STATUS_APPROVED).getId());

        assertEquals(List.of("approved"), calls);
    }

    @Test
    @DisplayName("实例已驳回时回调 onRejected")
    void rejectedInstanceCallsOnRejected() {
        List<String> calls = new ArrayList<>();
        BizApprovalListener listener = recordingListener(calls);

        ApprovalDispatcher dispatcher = buildDispatcher(listener);
        dispatcher.dispatchByInstanceState(instance(1L, WfInstance.STATUS_REJECTED).getId());

        assertEquals(List.of("rejected"), calls);
    }

    @Test
    @DisplayName("未注册监听器的业务类型不抛异常")
    void unknownBizTypeIsIgnored() {
        ApprovalDispatcher dispatcher = buildDispatcher(new BizApprovalListener() {
            @Override
            public String bizType() {
                return "SA_ORDER";
            }

            @Override
            public void onApproved(Long bizId, Map<String, Object> vars) {
                throw new AssertionError("不应被调用");
            }

            @Override
            public void onRejected(Long bizId, String reason, Map<String, Object> vars) {
                throw new AssertionError("不应被调用");
            }
        });

        dispatcher.dispatchApproved(instance(1L, WfInstance.STATUS_APPROVED).getId(), "通过");
    }

    private ApprovalDispatcher buildDispatcher(BizApprovalListener listener) {
        ApprovalCallbackRegistry registry = new ApprovalCallbackRegistry(List.of(listener));
        when(instanceMapper.selectById(any())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            WfInstance instance = new WfInstance();
            instance.setId(id);
            instance.setBizType("PA_ORDER");
            instance.setBizId(100L);
            instance.setStatus(WfInstance.STATUS_APPROVED);
            return instance;
        });
        when(engine.loadVariables(any())).thenReturn(Map.of());
        return new ApprovalDispatcher(instanceMapper, engine, registry);
    }

    private WfInstance instance(Long id, int status) {
        WfInstance instance = new WfInstance();
        instance.setId(id);
        instance.setBizType("PA_ORDER");
        instance.setBizId(100L);
        instance.setStatus(status);
        when(instanceMapper.selectById(id)).thenReturn(instance);
        return instance;
    }

    private BizApprovalListener recordingListener(List<String> calls) {
        return new BizApprovalListener() {
            @Override
            public String bizType() {
                return "PA_ORDER";
            }

            @Override
            public void onApproved(Long bizId, Map<String, Object> vars) {
                calls.add("approved");
            }

            @Override
            public void onRejected(Long bizId, String reason, Map<String, Object> vars) {
                calls.add("rejected");
            }
        };
    }

    /**
     * 保证动作常量与流水实体保持一致，避免枚举漂移。
     */
    @Test
    @DisplayName("审批动作常量与流水实体一致")
    void actionConstantsAreStable() {
        assertEquals("SUBMIT", WfTaskRecord.ACTION_SUBMIT);
        assertEquals("APPROVE", WfTaskRecord.ACTION_APPROVE);
        assertEquals("REJECT", WfTaskRecord.ACTION_REJECT);
        assertEquals("ROLLBACK", WfTaskRecord.ACTION_ROLLBACK);
        assertEquals("TRANSFER", WfTaskRecord.ACTION_TRANSFER);
        assertEquals("COUNTERSIGN", WfTaskRecord.ACTION_COUNTERSIGN);
        assertEquals("WITHDRAW", WfTaskRecord.ACTION_WITHDRAW);
    }
}








