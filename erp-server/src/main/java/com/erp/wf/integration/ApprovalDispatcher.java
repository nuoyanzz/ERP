package com.erp.wf.integration;

import com.erp.wf.domain.entity.WfInstance;
import com.erp.wf.engine.WfEngine;
import com.erp.wf.mapper.WfInstanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

/**
 * 审批结果分发器。
 * <p>
 * 把「流程结束」这一事实翻译成「业务回调」。放在独立组件里是为了保证：
 * 无论流程是从审批接口、集成接口还是撤回接口结束的，业务侧都只收到一次回调。
 * <p>
 * 回调在事务提交后执行：业务侧的审核动作（改状态、动库存、生成凭证）与流程状态变更
 * 属于两个独立的业务事实，若在同一事务里执行且业务侧抛异常，审批记录会跟着回滚而丢失。
 * 放到提交后执行可以保证流程状态先落库，业务失败时至少留下可追溯的审批记录供人工补偿。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalDispatcher {

    /**
     * 回调上下文标记。
     * <p>
     * 业务监听器在回调里会再次调用业务 Service 的审核方法，而该审核方法开头又会调用
     * {@code startApproval}。若不加拦截，就会为同一张单据重复发起流程。
     */
    private static final ThreadLocal<Boolean> IN_CALLBACK = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final WfInstanceMapper instanceMapper;
    private final WfEngine engine;
    private final ApprovalCallbackRegistry registry;

    /**
     * 当前线程是否处于业务回调中。
     */
    public static boolean inCallback() {
        return IN_CALLBACK.get();
    }

    /**
     * 按实例当前状态分发回调：仅当实例已结束时才回调。
     */
    public void dispatchByInstanceState(Long instanceId) {
        WfInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            return;
        }
        Integer status = instance.getStatus();
        if (status == null || status == WfInstance.STATUS_RUNNING) {
            // 流程尚未结束，等待后续节点
            return;
        }
        if (status == WfInstance.STATUS_SUSPENDED) {
            // 挂起不是终态，流程仍可能恢复，此时不能通知业务模块「审核未通过」
            return;
        }
        dispatchAfterCommit(instance, status == WfInstance.STATUS_APPROVED, instance.getRemark());
    }

    /**
     * 分发审批通过回调。
     */
    public void dispatchApproved(Long instanceId, String reason) {
        WfInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            return;
        }
        dispatchAfterCommit(instance, true, reason);
    }

    /**
     * 分发审批驳回回调。
     */
    public void dispatchRejected(Long instanceId, String reason) {
        WfInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            return;
        }
        dispatchAfterCommit(instance, false, reason);
    }

    /**
     * 在事务提交后执行回调；无事务时直接执行。
     */
    private void dispatchAfterCommit(WfInstance instance, boolean approved, String reason) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            dispatch(instance, approved, reason);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                dispatch(instance, approved, reason);
            }
        });
    }

    /**
     * 执行回调。
     */
    private void dispatch(WfInstance instance, boolean approved, String reason) {
        BizApprovalListener listener = registry.find(instance.getBizType());
        if (listener == null) {
            log.info("业务类型 {} 未注册审批监听器，跳过回调", instance.getBizType());
            return;
        }
        Map<String, Object> vars = engine.loadVariables(instance.getId());
        // 记录进入回调前的状态，嵌套回调结束后恢复，避免提前解除屏蔽导致重复发起流程
        boolean outerInCallback = IN_CALLBACK.get();
        try {
            IN_CALLBACK.set(Boolean.TRUE);
            if (approved) {
                listener.onApproved(instance.getBizId(), vars);
            } else {
                listener.onRejected(instance.getBizId(), reason, vars);
            }
        } catch (Exception e) {
            // 业务回调失败不应让审批记录丢失，记录日志由人工补偿
            log.error("审批回调失败 bizType={} bizId={}：{}", instance.getBizType(), instance.getBizId(),
                    e.getMessage(), e);
        } finally {
            IN_CALLBACK.set(outerInCallback);
        }
    }
}








