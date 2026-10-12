package com.erp.wf.integration;

import java.util.Map;

/**
 * 业务单据审批监听器。
 * <p>
 * 业务模块实现本接口并声明 {@link #bizType()}，工作流引擎在流程结束（通过/驳回）时回调，
 * 业务模块在这里执行真正的审核动作（如单据状态改为已审核、生成凭证等）。
 */
public interface BizApprovalListener {

    /**
     * 业务类型，需与 {@code wf_definition.biz_type} 一致。
     */
    String bizType();

    /**
     * 审批通过回调。
     *
     * @param bizId 业务单据主键
     * @param vars  流程变量快照（含 amount 等条件变量）
     */
    void onApproved(Long bizId, Map<String, Object> vars);

    /**
     * 审批驳回回调。
     *
     * @param bizId  业务单据主键
     * @param reason 驳回原因
     * @param vars   流程变量快照
     */
    void onRejected(Long bizId, String reason, Map<String, Object> vars);
}








