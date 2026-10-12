package com.erp.wf.engine;

import com.erp.common.exception.BizException;
import com.erp.common.expression.ExpressionEvaluator;
import com.erp.wf.domain.entity.WfDefinition;
import com.erp.wf.domain.entity.WfInstance;
import com.erp.wf.domain.entity.WfInstanceVariable;
import com.erp.wf.domain.entity.WfNode;
import com.erp.wf.domain.entity.WfTask;
import com.erp.wf.domain.entity.WfTaskRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工作流引擎核心。
 * <p>
 * 负责节点流转、条件分支求值、会签统计、驳回/退回/转办/加签/撤回等动作，
 * 并把每次动作写入 {@code wf_task_record} 留痕。
 * <p>
 * 引擎不直接依赖业务模块：流程结束时通过返回值 {@link WfFlowResult} 把结果交给上层，
 * 由上层转成业务回调，避免引擎与业务模块形成循环依赖。
 * <p>
 * 引擎只依赖 {@link WfStore} 与 {@link WfDirectory} 两个窄接口，
 * 使流转规则可以脱离数据库独立测试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfEngine {

    /** 流程变量键：并行分支当前激活的节点 ID 列表（逗号分隔） */
    private static final String VAR_PARALLEL_GROUP = "parallel_group";

    private final WfStore store;
    private final WfApproverResolver approverResolver;
    private final WfDirectory directory;

    // ------------------------------------------------------------------
    // 发起
    // ------------------------------------------------------------------

    /**
     * 发起流程实例。
     */
    public WfInstance start(WfDefinition definition, String bizType, Long bizId, String title,
                            Long starterId, Map<String, Object> vars) {
        if (definition == null) {
            throw new BizException("流程定义不存在");
        }
        if (store.countRunningInstances(bizType, bizId) > 0) {
            throw new BizException("该单据已存在运行中的审批流程");
        }

        WfInstance instance = new WfInstance();
        instance.setDefinitionId(definition.getId());
        instance.setBizType(bizType);
        instance.setBizId(bizId);
        instance.setTitle(title);
        instance.setStarterId(starterId);
        instance.setStatus(WfInstance.STATUS_RUNNING);
        instance.setRollbackCount(0);
        store.insertInstance(instance);

        Map<String, Object> context = buildContext(starterId, bizType, bizId, title, vars);
        saveVariables(instance.getId(), context);
        writeRecord(null, instance.getId(), null, WfTaskRecord.ACTION_SUBMIT, "发起流程", starterId);

        List<WfNode> nodes = store.listNodes(definition.getId());
        if (nodes.isEmpty()) {
            throw new BizException("流程未配置节点");
        }
        // 从头推进：跳过发起节点，找到第一个满足条件的审批节点
        advance(instance, nodes, null, context, null, WfTask.ACTION_CREATE);
        return instance;
    }

    // ------------------------------------------------------------------
    // 审批 / 驳回
    // ------------------------------------------------------------------

    /**
     * 同意任务。
     *
     * @return 流程结束时返回结果，否则返回 null
     */
    public WfFlowResult approve(Long taskId, Long operatorId, String comment) {
        WfTask task = requirePendingTask(taskId, operatorId);
        WfInstance instance = requireRunningInstance(task.getInstanceId());
        WfNode node = requireNode(task.getNodeId());

        markTask(task, WfTask.STATUS_APPROVED, comment, operatorId);
        writeRecord(task.getId(), instance.getId(), node.getId(), WfTaskRecord.ACTION_APPROVE, comment, operatorId);

        // 前加签任务通过后，恢复被挂起的原任务
        Long restoredTaskId = restoreSourceTask(task);

        Map<String, Object> context = loadVariables(instance.getId());
        if (!isNodeCompleted(instance, node)) {
            return null;
        }
        // 或签模式下其余待办不再需要处理
        // 刚恢复的原任务不在取消范围内——它还需由原审批人处理
        cancelRemainPendingTasks(instance, node, restoredTaskId);

        List<WfNode> nodes = store.listNodes(instance.getDefinitionId());
        WfNode parallelNode = findParallelNode(instance, node, context);
        if (parallelNode != null) {
            // 并行分支：等待同组其他节点全部完成后再继续流转
            if (!isParallelGroupCompleted(instance, context)) {
                return null;
            }
            advance(instance, nodes, parallelNode, context, null, WfTask.ACTION_CREATE);
        } else {
            advance(instance, nodes, node, context, null, WfTask.ACTION_CREATE);
        }
        // 只有流程真正走到终态才返回结果，否则交由上层按实例状态判断
        return buildResultIfFinished(instance, context);
    }

    /**
     * 驳回任务：终止整个流程。
     */
    public WfFlowResult reject(Long taskId, Long operatorId, String comment) {
        WfTask task = requirePendingTask(taskId, operatorId);
        WfInstance instance = requireRunningInstance(task.getInstanceId());
        WfNode node = requireNode(task.getNodeId());

        markTask(task, WfTask.STATUS_REJECTED, comment, operatorId);
        writeRecord(task.getId(), instance.getId(), node.getId(), WfTaskRecord.ACTION_REJECT, comment, operatorId);
        cancelPendingTasks(instance.getId());

        Map<String, Object> context = loadVariables(instance.getId());
        finishInstance(instance, WfInstance.STATUS_REJECTED, comment == null ? "审批驳回" : comment);
        return buildResult(instance, false, comment == null ? "审批驳回" : comment, context);
    }

    // ------------------------------------------------------------------
    // 退回 / 转办 / 加签 / 撤回 / 催办
    // ------------------------------------------------------------------

    /**
     * 退回到上一个已审批节点重新审批。
     * <p>
     * 退回次数超过上限时强制终止流程，避免发起人与审批人互相退回形成死循环。
     */
    public WfFlowResult rollback(Long taskId, Long operatorId, String comment) {
        WfTask task = requirePendingTask(taskId, operatorId);
        WfInstance instance = requireRunningInstance(task.getInstanceId());
        WfNode node = requireNode(task.getNodeId());
        Map<String, Object> context = loadVariables(instance.getId());

        int rollbackCount = instance.getRollbackCount() == null ? 0 : instance.getRollbackCount();
        if (rollbackCount + 1 > WfConstants.MAX_ROLLBACK) {
            markTask(task, WfTask.STATUS_CANCELLED, comment, operatorId);
            writeRecord(task.getId(), instance.getId(), node.getId(), WfTaskRecord.ACTION_ROLLBACK,
                    "退回次数超过上限，流程强制终止", operatorId);
            cancelPendingTasks(instance.getId());
            finishInstance(instance, WfInstance.STATUS_REJECTED, "退回次数超过上限，流程强制终止");
            return buildResult(instance, false, "退回次数超过上限，流程强制终止", context);
        }

        WfNode previousNode = findPreviousNode(instance, node);
        if (previousNode == null) {
            throw new BizException("没有可退回的上一个节点");
        }

        markTask(task, WfTask.STATUS_CANCELLED, comment, operatorId);
        writeRecord(task.getId(), instance.getId(), node.getId(), WfTaskRecord.ACTION_ROLLBACK,
                "退回到节点：" + previousNode.getNodeName(), operatorId);
        cancelPendingTasksOfNode(instance.getId(), node.getId());
        // 上一节点的历史任务作废并重新生成待办，让原审批人重新审批
        resetNodeTasks(instance, previousNode);

        WfInstance update = new WfInstance();
        update.setId(instance.getId());
        update.setCurrentNodeId(previousNode.getId());
        update.setRollbackCount(rollbackCount + 1);
        update.setStatus(WfInstance.STATUS_RUNNING);
        store.updateInstance(update);
        instance.setCurrentNodeId(previousNode.getId());
        instance.setRollbackCount(rollbackCount + 1);
        return null;
    }

    /**
     * 转办：把待办转给其他人，原任务置为已转办并新建目标人的任务。
     */
    public void transfer(Long taskId, Long operatorId, Long targetUserId, String comment) {
        if (targetUserId == null) {
            throw new BizException("转办目标不能为空");
        }
        WfTask task = requirePendingTask(taskId, operatorId);
        WfInstance instance = requireRunningInstance(task.getInstanceId());
        requireAvailableUser(targetUserId, "转办目标");

        markTask(task, WfTask.STATUS_TRANSFERRED, comment, operatorId);
        writeRecord(task.getId(), instance.getId(), task.getNodeId(), WfTaskRecord.ACTION_TRANSFER,
                "转办给：" + describeUser(targetUserId), operatorId);

        WfTask newTask = new WfTask();
        newTask.setInstanceId(instance.getId());
        newTask.setNodeId(task.getNodeId());
        newTask.setNodeKey(task.getNodeKey());
        newTask.setAssigneeId(targetUserId);
        newTask.setStatus(WfTask.STATUS_PENDING);
        newTask.setSourceTaskId(task.getId());
        newTask.setActionType(WfTask.ACTION_TRANSFER);
        newTask.setTimeoutAt(task.getTimeoutAt());
        newTask.setRemindCount(0);
        store.insertTask(newTask);
    }

    /**
     * 加签：在当前节点增加一个审批人。
     *
     * @param position {@link WfConstants#COUNTERSIGN_BEFORE} 前加签，{@link WfConstants#COUNTERSIGN_AFTER} 后加签
     */
    public void countersign(Long taskId, Long operatorId, Long targetUserId, Integer position, String comment) {
        if (targetUserId == null) {
            throw new BizException("加签人不能为空");
        }
        int signPosition = position == null ? WfConstants.COUNTERSIGN_AFTER : position;
        WfTask task = requirePendingTask(taskId, operatorId);
        WfInstance instance = requireRunningInstance(task.getInstanceId());
        requireAvailableUser(targetUserId, "加签人");

        WfTask newTask = new WfTask();
        newTask.setInstanceId(instance.getId());
        newTask.setNodeId(task.getNodeId());
        newTask.setNodeKey(task.getNodeKey());
        newTask.setAssigneeId(targetUserId);
        newTask.setStatus(WfTask.STATUS_PENDING);
        newTask.setSourceTaskId(task.getId());
        newTask.setActionType(WfTask.ACTION_COUNTERSIGN);
        newTask.setTimeoutAt(task.getTimeoutAt());
        newTask.setRemindCount(0);
        store.insertTask(newTask);

        writeRecord(task.getId(), instance.getId(), task.getNodeId(), WfTaskRecord.ACTION_COUNTERSIGN,
                (signPosition == WfConstants.COUNTERSIGN_BEFORE ? "前加签：" : "后加签：") + describeUser(targetUserId),
                operatorId);

        if (signPosition == WfConstants.COUNTERSIGN_BEFORE) {
            // 前加签：原任务挂起，等加签人通过后再放行
            WfTask suspend = new WfTask();
            suspend.setId(task.getId());
            suspend.setStatus(WfTask.STATUS_WAITING);
            store.updateTask(suspend);
        }
    }

    /**
     * 校验目标用户存在且启用。
     */
    private void requireAvailableUser(Long userId, String label) {
        List<Long> enabled = directory.filterEnabledUsers(List.of(userId));
        if (enabled.isEmpty()) {
            throw new BizException(label + "不存在或已停用");
        }
    }

    /**
     * 用户展示名，昵称缺失时退回用户 ID。
     */
    private String describeUser(Long userId) {
        String nickname = directory.nicknameOfUser(userId);
        return nickname == null ? String.valueOf(userId) : nickname;
    }

    /**
     * 撤回：仅发起人、且没有任何任务被处理过时可撤回。
     */
    public WfFlowResult withdraw(Long instanceId, Long operatorId, String comment) {
        WfInstance instance = store.findInstance(instanceId);
        if (instance == null) {
            throw new BizException("流程实例不存在");
        }
        if (instance.getStatus() == null || instance.getStatus() != WfInstance.STATUS_RUNNING) {
            throw new BizException("只有运行中的流程才能撤回");
        }
        if (instance.getStarterId() == null || !instance.getStarterId().equals(operatorId)) {
            throw new BizException("只有发起人才能撤回流程");
        }
        long handled = store.countTasks(instanceId, List.of(
                WfTask.STATUS_APPROVED, WfTask.STATUS_REJECTED, WfTask.STATUS_TRANSFERRED,
                WfTask.STATUS_COUNTERSIGNED));
        if (handled > 0) {
            throw new BizException("流程已被处理，无法撤回");
        }
        Map<String, Object> context = loadVariables(instanceId);
        cancelPendingTasks(instanceId);
        writeRecord(null, instanceId, instance.getCurrentNodeId(), WfTaskRecord.ACTION_WITHDRAW, comment, operatorId);
        finishInstance(instance, WfInstance.STATUS_CANCELLED, comment == null ? "发起人撤回" : comment);
        return buildResult(instance, false, comment == null ? "发起人撤回" : comment, context);
    }

    /**
     * 催办：给待办任务提醒一次。
     */
    public void urge(Long taskId, Long operatorId) {
        WfTask task = store.findTask(taskId);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        if (task.getStatus() == null || task.getStatus() != WfTask.STATUS_PENDING) {
            throw new BizException("任务不存在或已处理");
        }
        WfTask update = new WfTask();
        update.setId(taskId);
        update.setRemindCount((task.getRemindCount() == null ? 0 : task.getRemindCount()) + 1);
        store.updateTask(update);
        writeRecord(taskId, task.getInstanceId(), task.getNodeId(), WfTaskRecord.ACTION_URGE, "发起催办", operatorId);
    }

    // ------------------------------------------------------------------
    // 节点流转
    // ------------------------------------------------------------------

    /**
     * 从 {@code currentNode} 之后继续推进流程。
     *
     * @param currentNode 当前节点；为 null 表示从流程开头推进
     */
    private void advance(WfInstance instance, List<WfNode> nodes, WfNode currentNode,
                         Map<String, Object> context, Long sourceTaskId, String actionType) {
        Integer afterSortNo = currentNode == null ? null : currentNode.getSortNo();
        for (int i = 0; i < nodes.size(); i++) {
            WfNode node = nodes.get(i);
            if (afterSortNo != null && node.getSortNo() <= afterSortNo) {
                continue;
            }
            Integer nodeType = node.getNodeType() == null ? WfNode.TYPE_APPROVAL : node.getNodeType();
            switch (nodeType) {
                case WfNode.TYPE_START -> {
                    // 发起节点只是流程入口，不产生待办
                }
                case WfNode.TYPE_END -> {
                    finishInstance(instance, WfInstance.STATUS_APPROVED, "流程审批通过");
                    return;
                }
                case WfNode.TYPE_CC -> {
                    recordCc(instance, node);
                }
                case WfNode.TYPE_CONDITION -> {
                    // 条件节点是路由开关：条件不成立时跳过紧随其后的分支节点
                    boolean matched = matchCondition(node, context);
                    if (!matched && i + 1 < nodes.size()) {
                        i++;
                    }
                }
                case WfNode.TYPE_PARALLEL -> {
                    activateParallel(instance, nodes, node, context);
                    return;
                }
                default -> {
                    if (!matchCondition(node, context)) {
                        // 条件不满足的审批节点直接跳过
                        continue;
                    }
                    activateNode(instance, node, context, sourceTaskId, actionType);
                    return;
                }
            }
        }
        // 没有更多节点视为流程结束
        finishInstance(instance, WfInstance.STATUS_APPROVED, "流程审批通过");
    }

    /**
     * 激活单个审批节点。
     */
    private void activateNode(WfInstance instance, WfNode node, Map<String, Object> context,
                              Long sourceTaskId, String actionType) {
        WfApprover approver = approverResolver.resolve(node, instance.getStarterId(),
                directory.deptIdOfUser(instance.getStarterId()));
        if (approver.isEmpty()) {
            suspendInstance(instance, node, approver.getFailReason());
            return;
        }
        for (Long userId : approver.getUserIds()) {
            createTask(instance, node, userId, sourceTaskId, actionType);
        }
        updateCurrentNode(instance, node.getId());
    }

    /**
     * 激活并行分支：分支内的审批节点同时激活，全部完成后流程继续。
     */
    private void activateParallel(WfInstance instance, List<WfNode> nodes, WfNode parallelNode,
                                  Map<String, Object> context) {
        List<Long> activated = new ArrayList<>();
        for (WfNode node : nodes) {
            if (node.getSortNo() <= parallelNode.getSortNo()) {
                continue;
            }
            Integer nodeType = node.getNodeType() == null ? WfNode.TYPE_APPROVAL : node.getNodeType();
            if (nodeType == WfNode.TYPE_PARALLEL || nodeType == WfNode.TYPE_END) {
                break;
            }
            if (nodeType != WfNode.TYPE_APPROVAL || !matchCondition(node, context)) {
                continue;
            }
            WfApprover approver = approverResolver.resolve(node, instance.getStarterId(),
                    directory.deptIdOfUser(instance.getStarterId()));
            if (approver.isEmpty()) {
                suspendInstance(instance, node, approver.getFailReason());
                return;
            }
            for (Long userId : approver.getUserIds()) {
                createTask(instance, node, userId, null, WfTask.ACTION_CREATE);
            }
            activated.add(node.getId());
        }
        if (activated.isEmpty()) {
            finishInstance(instance, WfInstance.STATUS_APPROVED, "流程审批通过");
            return;
        }
        context.put(VAR_PARALLEL_GROUP, joinIds(activated));
        saveVariables(instance.getId(), Map.of(VAR_PARALLEL_GROUP, joinIds(activated)));
        updateCurrentNode(instance, parallelNode.getId());
    }

    /**
     * 创建待办任务。
     */
    private void createTask(WfInstance instance, WfNode node, Long assigneeId, Long sourceTaskId, String actionType) {
        WfTask task = new WfTask();
        task.setInstanceId(instance.getId());
        task.setNodeId(node.getId());
        task.setNodeKey(node.getNodeKey());
        task.setAssigneeId(assigneeId);
        task.setStatus(WfTask.STATUS_PENDING);
        task.setSourceTaskId(sourceTaskId);
        task.setActionType(actionType == null ? WfTask.ACTION_CREATE : actionType);
        task.setRemindCount(0);
        if (node.getTimeoutHours() != null && node.getTimeoutHours() > 0) {
            task.setTimeoutAt(LocalDateTime.now().plusHours(node.getTimeoutHours()));
        }
        store.insertTask(task);
    }

    /**
     * 判断节点是否已完成。
     * <p>
     * 规则分两层：
     * <ul>
     *   <li>加签任务是额外的必审项：只要还有待办的加签任务，节点就不算完成。
     *       前加签时原任务被挂起（WAITING），加签人通过后原任务恢复待办，节点仍需原审批人处理。</li>
     *   <li>原始任务按节点的会签模式判定：会签需全部通过，或签任一通过即可。</li>
     * </ul>
     */
    private boolean isNodeCompleted(WfInstance instance, WfNode node) {
        List<WfTask> tasks = store.listTasksByNode(instance.getId(), node.getId());
        int originalPending = 0;
        int originalApproved = 0;
        int countersignPending = 0;
        int waiting = 0;
        for (WfTask task : tasks) {
            Integer status = task.getStatus();
            if (status == null) {
                continue;
            }
            boolean isCountersign = WfTask.ACTION_COUNTERSIGN.equals(task.getActionType());
            switch (status) {
                case WfTask.STATUS_PENDING -> {
                    if (isCountersign) {
                        countersignPending++;
                    } else {
                        originalPending++;
                    }
                }
                case WfTask.STATUS_WAITING -> waiting++;
                case WfTask.STATUS_APPROVED -> {
                    if (!isCountersign) {
                        originalApproved++;
                    }
                }
                default -> {
                    // 已取消/驳回/转办的任务不参与统计
                }
            }
        }
        // 前加签场景：原任务被挂起，等加签人通过后恢复
        if (waiting > 0) {
            return false;
        }
        // 加签任务是额外必审项，必须单独通过
        if (countersignPending > 0) {
            return false;
        }
        boolean all = node.getMultiApprove() != null && node.getMultiApprove() == WfNode.MULTI_ALL;
        if (all) {
            return originalPending == 0 && originalApproved > 0;
        }
        return originalApproved > 0;
    }

    /**
     * 并行分组是否已全部完成。
     */
    private boolean isParallelGroupCompleted(WfInstance instance, Map<String, Object> context) {
        Set<Long> nodeIds = parseParallelGroup(context.get(VAR_PARALLEL_GROUP));
        if (nodeIds.isEmpty()) {
            return false;
        }
        for (Long nodeId : nodeIds) {
            WfNode node = store.findNode(nodeId);
            if (node == null) {
                continue;
            }
            if (!isNodeCompleted(instance, node)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断节点是否属于某个并行分组。
     *
     * @return 所属的并行分支节点；不属于并行分组时返回 null
     */
    private WfNode findParallelNode(WfInstance instance, WfNode node, Map<String, Object> context) {
        Set<Long> group = parseParallelGroup(context.get(VAR_PARALLEL_GROUP));
        if (!group.contains(node.getId())) {
            return null;
        }
        // 并行分支节点是分组内排序最靠前的节点
        WfNode parallelNode = null;
        for (WfNode candidate : store.listNodes(instance.getDefinitionId())) {
            Integer nodeType = candidate.getNodeType() == null ? WfNode.TYPE_APPROVAL : candidate.getNodeType();
            if (nodeType != WfNode.TYPE_PARALLEL) {
                continue;
            }
            if (candidate.getSortNo() >= node.getSortNo()) {
                continue;
            }
            if (parallelNode == null || candidate.getSortNo() > parallelNode.getSortNo()) {
                parallelNode = candidate;
            }
        }
        return parallelNode;
    }

    private Set<Long> parseParallelGroup(Object group) {
        Set<Long> nodeIds = new LinkedHashSet<>();
        if (group == null) {
            return nodeIds;
        }
        for (String token : String.valueOf(group).split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                nodeIds.add(Long.valueOf(trimmed));
            } catch (NumberFormatException ignored) {
                // 忽略脏数据
            }
        }
        return nodeIds;
    }

    /**
     * 或签模式下取消同节点剩余待办。
     *
     * @param excludeTaskId 需要排除的任务 ID（前加签恢复的原任务）
     */
    private void cancelRemainPendingTasks(WfInstance instance, WfNode node, Long excludeTaskId) {
        for (WfTask task : store.listTasksByNode(instance.getId(), node.getId())) {
            if (excludeTaskId != null && excludeTaskId.equals(task.getId())) {
                continue;
            }
            if (task.getStatus() != null && task.getStatus() == WfTask.STATUS_PENDING) {
                WfTask update = new WfTask();
                update.setId(task.getId());
                update.setStatus(WfTask.STATUS_CANCELLED);
                store.updateTask(update);
            }
        }
    }

    /**
     * 挂起实例：解析不到审批人时不能静默失败，必须记录原因等待人工介入。
     */
    private void suspendInstance(WfInstance instance, WfNode node, String reason) {
        WfInstance update = new WfInstance();
        update.setId(instance.getId());
        update.setStatus(WfInstance.STATUS_SUSPENDED);
        update.setSuspendReason(reason);
        update.setCurrentNodeId(node == null ? instance.getCurrentNodeId() : node.getId());
        store.updateInstance(update);
        instance.setStatus(WfInstance.STATUS_SUSPENDED);
        instance.setSuspendReason(reason);
        log.warn("流程实例 {} 挂起，节点 {}：{}", instance.getId(),
                node == null ? "未知" : node.getNodeName(), reason);
    }

    /**
     * 结束实例。
     */
    private void finishInstance(WfInstance instance, int status, String reason) {
        WfInstance update = new WfInstance();
        update.setId(instance.getId());
        update.setStatus(status);
        update.setFinishTime(LocalDateTime.now());
        update.setRemark(reason);
        store.updateInstance(update);
        instance.setStatus(status);
        instance.setFinishTime(LocalDateTime.now());
        writeRecord(null, instance.getId(), instance.getCurrentNodeId(), WfTaskRecord.ACTION_FINISH, reason, null);
    }

    /**
     * 抄送节点：只留痕，不阻塞流转。
     */
    private void recordCc(WfInstance instance, WfNode node) {
        WfApprover approver = approverResolver.resolve(node, instance.getStarterId(),
                directory.deptIdOfUser(instance.getStarterId()));
        if (approver.isEmpty()) {
            return;
        }
        for (Long userId : approver.getUserIds()) {
            writeRecord(null, instance.getId(), node.getId(), WfTaskRecord.ACTION_CC,
                    "抄送：" + node.getNodeName(), userId);
        }
    }

    // ------------------------------------------------------------------
    // 条件求值
    // ------------------------------------------------------------------

    /**
     * 节点条件求值：表达式为空视为无条件通过。
     */
    private boolean matchCondition(WfNode node, Map<String, Object> context) {
        String expr = node.getConditionExpr();
        if (!StringUtils.hasText(expr)) {
            return true;
        }
        try {
            return ExpressionEvaluator.evaluate(expr, context);
        } catch (Exception e) {
            // 表达式配置错误不应让流程凭空通过，按不满足处理并留下日志
            log.error("节点 {} 条件表达式求值失败：{}", node.getNodeName(), e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------
    // 变量
    // ------------------------------------------------------------------

    /**
     * 构造流程上下文：内置 amount/type/dept/user 等变量，再叠加业务传入的扩展变量。
     */
    private Map<String, Object> buildContext(Long starterId, String bizType, Long bizId, String title,
                                             Map<String, Object> vars) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put(WfConstants.VAR_USER, directory.nicknameOfUser(starterId));
        context.put(WfConstants.VAR_DEPT, directory.deptIdOfUser(starterId));
        context.put(WfConstants.VAR_TYPE, bizType);
        context.put(WfConstants.VAR_BIZ_ID, bizId);
        context.put(WfConstants.VAR_TITLE, title);
        if (vars != null) {
            vars.forEach((key, value) -> {
                if (key != null && !context.containsKey(key)) {
                    context.put(key, value);
                }
            });
        }
        return context;
    }

    /**
     * 持久化流程变量，供后续节点求值使用。
     */
    private void saveVariables(Long instanceId, Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            WfInstanceVariable variable = new WfInstanceVariable();
            variable.setInstanceId(instanceId);
            variable.setVarKey(entry.getKey());
            variable.setVarValue(String.valueOf(value));
            store.insertVariable(variable);
        }
    }

    /**
     * 读取流程变量。
     */
    public Map<String, Object> loadVariables(Long instanceId) {
        Map<String, Object> context = new LinkedHashMap<>();
        for (WfInstanceVariable variable : store.listVariables(instanceId)) {
            context.put(variable.getVarKey(), castVariable(variable.getVarKey(), variable.getVarValue()));
        }
        return context;
    }

    /**
     * 变量还原：金额保持数值类型，避免条件比较退化成字符串比较。
     */
    private Object castVariable(String key, String value) {
        if (value == null) {
            return null;
        }
        if (WfConstants.VAR_AMOUNT.equals(key)) {
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException e) {
                return value;
            }
        }
        if (WfConstants.VAR_BIZ_ID.equals(key)) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException e) {
                return value;
            }
        }
        return value;
    }

    // ------------------------------------------------------------------
    // 辅助
    // ------------------------------------------------------------------

    /**
     * 更新任务状态与操作信息。
     */
    private void markTask(WfTask task, int status, String comment, Long operateBy) {
        WfTask update = new WfTask();
        update.setId(task.getId());
        update.setStatus(status);
        update.setComment(comment);
        update.setOperateBy(operateBy);
        update.setOperateTime(LocalDateTime.now());
        store.updateTask(update);
        task.setStatus(status);
        task.setOperateBy(operateBy);
        task.setOperateTime(update.getOperateTime());
    }

    /**
     * 校验待办任务并返回。
     */
    private WfTask requirePendingTask(Long taskId, Long operatorId) {
        WfTask task = store.findTask(taskId);
        if (task == null || task.getStatus() == null || task.getStatus() != WfTask.STATUS_PENDING) {
            throw new BizException("任务不存在或已处理");
        }
        if (operatorId == null || !operatorId.equals(task.getAssigneeId())) {
            throw new BizException("无权处理该任务");
        }
        return task;
    }

    /**
     * 校验运行中的实例并返回。
     */
    private WfInstance requireRunningInstance(Long instanceId) {
        WfInstance instance = store.findInstance(instanceId);
        if (instance == null) {
            throw new BizException("流程实例不存在");
        }
        if (instance.getStatus() == null || instance.getStatus() != WfInstance.STATUS_RUNNING) {
            throw new BizException("流程已结束，不能审批");
        }
        return instance;
    }

    /**
     * 校验节点并返回。
     */
    private WfNode requireNode(Long nodeId) {
        WfNode node = store.findNode(nodeId);
        if (node == null) {
            throw new BizException("流程节点不存在");
        }
        return node;
    }

    /**
     * 查找上一个已激活过的审批节点，作为退回目标。
     * <p>
     * 用「节点被激活过（存在任务）」而不是「节点有已通过任务」来判断，
     * 因为退回会把原节点的任务作废重建，此时原节点的通过记录已经不在了，
     * 但它仍然是合法的退回目标。
     */
    private WfNode findPreviousNode(WfInstance instance, WfNode currentNode) {
        int currentSort = currentNode.getSortNo() == null ? Integer.MAX_VALUE : currentNode.getSortNo();
        WfNode candidate = null;
        for (WfNode node : store.listNodes(instance.getDefinitionId())) {
            Integer nodeType = node.getNodeType() == null ? WfNode.TYPE_APPROVAL : node.getNodeType();
            if (nodeType != WfNode.TYPE_APPROVAL) {
                continue;
            }
            int nodeSort = node.getSortNo() == null ? Integer.MAX_VALUE : node.getSortNo();
            if (nodeSort >= currentSort) {
                continue;
            }
            // 条件不满足而被跳过的节点没有任务，不能作为退回目标
            if (store.listTasksByNode(instance.getId(), node.getId()).isEmpty()) {
                continue;
            }
            if (candidate == null || nodeSort > candidate.getSortNo()) {
                candidate = node;
            }
        }
        return candidate;
    }

    /**
     * 重新生成节点待办：作废历史任务并按原审批人重新创建。
     */
    private void resetNodeTasks(WfInstance instance, WfNode node) {
        List<WfTask> tasks = store.listTasksByNode(instance.getId(), node.getId());
        if (tasks.isEmpty()) {
            return;
        }
        List<Long> assignees = new ArrayList<>();
        for (WfTask task : tasks) {
            if (task.getAssigneeId() != null && !assignees.contains(task.getAssigneeId())) {
                assignees.add(task.getAssigneeId());
            }
            WfTask update = new WfTask();
            update.setId(task.getId());
            update.setStatus(WfTask.STATUS_CANCELLED);
            store.updateTask(update);
        }
        for (Long assigneeId : assignees) {
            createTask(instance, node, assigneeId, null, WfTask.ACTION_ROLLBACK);
        }
    }

    /**
     * 恢复被前加签挂起的原任务。
     */
    private Long restoreSourceTask(WfTask countersignTask) {
        if (!WfTask.ACTION_COUNTERSIGN.equals(countersignTask.getActionType())
                || countersignTask.getSourceTaskId() == null) {
            return null;
        }
        WfTask source = store.findTask(countersignTask.getSourceTaskId());
        if (source == null || source.getStatus() == null || source.getStatus() != WfTask.STATUS_WAITING) {
            return null;
        }
        WfTask update = new WfTask();
        update.setId(source.getId());
        update.setStatus(WfTask.STATUS_PENDING);
        store.updateTask(update);
        return source.getId();
    }

    /**
     * 取消实例下所有待办任务。
     */
    public void cancelPendingTasks(Long instanceId) {
        for (WfTask task : store.listTasks(instanceId)) {
            if (task.getStatus() != null && task.getStatus() == WfTask.STATUS_PENDING) {
                WfTask update = new WfTask();
                update.setId(task.getId());
                update.setStatus(WfTask.STATUS_CANCELLED);
                store.updateTask(update);
            }
        }
    }

    /**
     * 取消指定节点下所有待办任务。
     */
    private void cancelPendingTasksOfNode(Long instanceId, Long nodeId) {
        for (WfTask task : store.listTasksByNode(instanceId, nodeId)) {
            if (task.getStatus() != null && task.getStatus() == WfTask.STATUS_PENDING) {
                WfTask update = new WfTask();
                update.setId(task.getId());
                update.setStatus(WfTask.STATUS_CANCELLED);
                store.updateTask(update);
            }
        }
    }

    /**
     * 更新实例当前节点。
     */
    private void updateCurrentNode(WfInstance instance, Long nodeId) {
        WfInstance update = new WfInstance();
        update.setId(instance.getId());
        update.setCurrentNodeId(nodeId);
        store.updateInstance(update);
        instance.setCurrentNodeId(nodeId);
    }

    /**
     * 记录审批动作流水。
     */
    public void writeRecord(Long taskId, Long instanceId, Long nodeId, String action, String comment, Long operatorId) {
        WfTaskRecord record = new WfTaskRecord();
        record.setTaskId(taskId == null ? 0L : taskId);
        record.setInstanceId(instanceId);
        record.setNodeId(nodeId == null ? 0L : nodeId);
        record.setAction(action);
        record.setComment(comment);
        record.setOperatorId(operatorId);
        record.setOperateTime(LocalDateTime.now());
        store.insertRecord(record);
    }

    /**
     * 构造流程结果。
     */
    private WfFlowResult buildResult(WfInstance instance, boolean approved, String reason, Map<String, Object> context) {
        if (approved) {
            return WfFlowResult.approved(instance.getId(), instance.getBizType(), instance.getBizId(), reason, context);
        }
        return WfFlowResult.rejected(instance.getId(), instance.getBizType(), instance.getBizId(), reason, context);
    }

    /**
     * 仅在流程进入终态时构造结果，否则返回 null。
     * <p>
     * 审批动作本身不代表流程结束——节点通过后可能还有后续节点待审批。
     * 只有实例状态是终态时，才应该通知业务模块「审核完成」。
     */
    private WfFlowResult buildResultIfFinished(WfInstance instance, Map<String, Object> context) {
        Integer status = instance.getStatus();
        if (status == null || status == WfInstance.STATUS_RUNNING || status == WfInstance.STATUS_SUSPENDED) {
            return null;
        }
        if (status == WfInstance.STATUS_APPROVED) {
            return buildResult(instance, true, "流程审批通过", context);
        }
        return buildResult(instance, false, instance.getRemark() == null ? "流程已终止" : instance.getRemark(), context);
    }

    private String joinIds(List<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id);
        }
        return sb.toString();
    }
}








