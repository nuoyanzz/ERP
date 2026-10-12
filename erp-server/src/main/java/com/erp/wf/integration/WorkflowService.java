package com.erp.wf.integration;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 工作流对业务模块暴露的门面接口。
 * <p>
 * 业务模块只需要在「提交审核」处调用 {@link #startApproval}：
 * 返回非 null 表示单据已进入审批流，业务侧应直接返回；返回 null 表示该业务类型没有配置审批流程，
 * 业务侧按原有逻辑继续审核即可。
 */
public interface WorkflowService {

    /**
     * 发起审批流程。
     *
     * @param bizType   业务类型，需与 {@code wf_definition.biz_type} 一致
     * @param bizId     业务单据主键
     * @param title     审批标题
     * @param amount    单据金额，用于条件分支求值
     * @param extraVars 额外的流程变量（可为 null）
     * @return 流程实例 ID；该业务类型未配置已发布流程时返回 null
     */
    Long startApproval(String bizType, Long bizId, String title, BigDecimal amount, Map<String, Object> extraVars);

    /**
     * 完成审批。
     *
     * @param bizType  业务类型
     * @param bizId    业务单据主键
     * @param taskId   待办任务 ID
     * @param approved 是否通过
     * @param comment  审批意见
     */
    void completeApproval(String bizType, Long bizId, Long taskId, boolean approved, String comment);

    /**
     * 单据是否存在运行中的审批流程。
     */
    boolean isPending(String bizType, Long bizId);

    /**
     * 查询单据当前待办的审批任务 ID，便于业务侧展示「待审批人」。
     *
     * @return 待办任务 ID；无待办时返回 null
     */
    Long getPendingTaskId(String bizType, Long bizId);
}








