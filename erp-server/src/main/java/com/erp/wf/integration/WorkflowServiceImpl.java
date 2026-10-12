package com.erp.wf.integration;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.security.SecurityUtils;
import com.erp.wf.domain.entity.WfDefinition;
import com.erp.wf.domain.entity.WfInstance;
import com.erp.wf.domain.entity.WfTask;
import com.erp.wf.engine.WfEngine;
import com.erp.wf.engine.WfFlowResult;
import com.erp.wf.mapper.WfDefinitionMapper;
import com.erp.wf.mapper.WfInstanceMapper;
import com.erp.wf.mapper.WfTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流集成服务实现。
 * <p>
 * 负责把「业务单据」翻译成「流程实例」，并在流程结束时把结果回调给业务模块。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final WfDefinitionMapper definitionMapper;
    private final WfInstanceMapper instanceMapper;
    private final WfTaskMapper taskMapper;
    private final WfEngine engine;
    private final ApprovalCallbackRegistry callbackRegistry;
    private final ApprovalDispatcher dispatcher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long startApproval(String bizType, Long bizId, String title, BigDecimal amount,
                              Map<String, Object> extraVars) {
        if (!StringUtils.hasText(bizType)) {
            return null;
        }
        if (ApprovalDispatcher.inCallback()) {
            // 回调期间不再发起新流程，交由业务原有审核逻辑处理
            return null;
        }
        WfDefinition definition = findPublishedDefinition(bizType);
        if (definition == null) {
            // 该业务类型未配置审批流程，业务侧按原逻辑审核即可
            return null;
        }
        Map<String, Object> vars = new LinkedHashMap<>();
        if (extraVars != null) {
            vars.putAll(extraVars);
        }
        if (amount != null) {
            vars.put("amount", amount);
        }
        Long starterId = SecurityUtils.getCurrentUserId();
        WfInstance instance = engine.start(definition, bizType, bizId, title, starterId, vars);

        // 流程在首个节点即结束（如无审批节点）时，引擎已写入终态，需要立即回调业务
        if (instance.getStatus() != null && instance.getStatus() == WfInstance.STATUS_APPROVED) {
            dispatcher.dispatchApproved(instance.getId(), "流程审批通过");
        }
        return instance.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeApproval(String bizType, Long bizId, Long taskId, boolean approved, String comment) {
        if (taskId == null) {
            throw new IllegalArgumentException("审批任务不能为空");
        }
        Long operatorId = SecurityUtils.getCurrentUserId();
        WfFlowResult result = approved
                ? engine.approve(taskId, operatorId, comment)
                : engine.reject(taskId, operatorId, comment);
        if (result == null) {
            // 流程尚未结束（会签未完成 / 后续节点待审批），无需回调业务
            return;
        }
        if (result.isApproved()) {
            dispatcher.dispatchApproved(result.getInstanceId(), result.getReason());
        } else {
            dispatcher.dispatchRejected(result.getInstanceId(), result.getReason());
        }
    }

    @Override
    public boolean isPending(String bizType, Long bizId) {
        if (!StringUtils.hasText(bizType)) {
            return false;
        }
        Long count = instanceMapper.selectCount(Wrappers.<WfInstance>lambdaQuery()
                .eq(WfInstance::getBizType, bizType)
                .eq(WfInstance::getBizId, bizId)
                .eq(WfInstance::getStatus, WfInstance.STATUS_RUNNING));
        return count != null && count > 0;
    }

    @Override
    public Long getPendingTaskId(String bizType, Long bizId) {
        if (!StringUtils.hasText(bizType)) {
            return null;
        }
        List<WfInstance> instances = instanceMapper.selectList(Wrappers.<WfInstance>lambdaQuery()
                .eq(WfInstance::getBizType, bizType)
                .eq(WfInstance::getBizId, bizId)
                .eq(WfInstance::getStatus, WfInstance.STATUS_RUNNING)
                .orderByDesc(WfInstance::getId)
                .last("limit 1"));
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        List<WfTask> tasks = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
                .eq(WfTask::getInstanceId, instances.get(0).getId())
                .eq(WfTask::getStatus, WfTask.STATUS_PENDING)
                .orderByAsc(WfTask::getId)
                .last("limit 1"));
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }
        return tasks.get(0).getId();
    }

    /**
     * 按业务类型查找已发布的流程定义。
     */
    public WfDefinition findPublishedDefinition(String bizType) {
        List<WfDefinition> definitions = definitionMapper.selectList(Wrappers.<WfDefinition>lambdaQuery()
                .eq(WfDefinition::getBizType, bizType)
                .eq(WfDefinition::getStatus, WfDefinition.STATUS_ENABLED)
                .orderByDesc(WfDefinition::getVersion)
                .orderByDesc(WfDefinition::getId)
                .last("limit 1"));
        if (definitions == null || definitions.isEmpty()) {
            return null;
        }
        return definitions.get(0);
    }

    /**
     * 是否已注册该业务类型的审批监听器。
     */
    public boolean hasListener(String bizType) {
        return callbackRegistry.supports(bizType);
    }
}








