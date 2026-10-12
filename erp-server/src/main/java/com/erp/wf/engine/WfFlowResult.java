package com.erp.wf.engine;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 流程流转结果。
 * <p>
 * 引擎方法在流程结束时返回该对象，由集成层转成业务回调；流程未结束时返回 {@code null}。
 */
@Data
public class WfFlowResult {

    private Long instanceId;
    private String bizType;
    private Long bizId;
    /** true=审批通过，false=驳回/撤回/强制终止 */
    private boolean approved;
    /** 结束原因 */
    private String reason;
    /** 流程变量快照 */
    private Map<String, Object> vars = new HashMap<>();

    public static WfFlowResult approved(Long instanceId, String bizType, Long bizId, String reason, Map<String, Object> vars) {
        WfFlowResult result = new WfFlowResult();
        result.setInstanceId(instanceId);
        result.setBizType(bizType);
        result.setBizId(bizId);
        result.setApproved(true);
        result.setReason(reason);
        if (vars != null) {
            result.setVars(vars);
        }
        return result;
    }

    public static WfFlowResult rejected(Long instanceId, String bizType, Long bizId, String reason, Map<String, Object> vars) {
        WfFlowResult result = approved(instanceId, bizType, bizId, reason, vars);
        result.setApproved(false);
        return result;
    }
}








