package com.erp.wf.engine;

import com.erp.common.exception.BizException;
import com.erp.wf.domain.entity.WfDefinition;
import com.erp.wf.domain.entity.WfInstance;
import com.erp.wf.domain.entity.WfNode;
import com.erp.wf.domain.entity.WfTask;
import com.erp.wf.support.InMemoryWfStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工作流引擎单元测试。
 * <p>
 * 使用内存存储与内存组织数据，完全不依赖数据库，覆盖条件分支、会签、驳回、退回、转办、加签、撤回。
 */
class WfEngineTest {

    private static final Long DEFINITION_ID = 99101L;
    private static final Long STARTER_ID = 7L;
    private static final Long DEPT_ID = 100L;

    private InMemoryWfStore store;
    private TestDirectory directory;
    private WfEngine engine;

    @BeforeEach
    void setUp() {
        store = new InMemoryWfStore();
        directory = new TestDirectory();
        WfApproverResolver resolver = new WfApproverResolver(directory);
        engine = new WfEngine(store, resolver, directory);
        // 登记常用用户，默认均为启用状态
        for (Long userId : List.of(7L, 11L, 12L, 21L, 99L, 88L)) {
            directory.registerUser(userId, "用户" + userId, DEPT_ID);
        }
    }

    // ------------------------------------------------------------------
    // 条件分支
    // ------------------------------------------------------------------

    @Test
    @DisplayName("金额大于 5 万时走财务总监节点")
    void conditionBranchActivatesFinanceDirectorForLargeAmount() {
        // 采购主管 -> 财务总监(条件) -> 结束
        WfNode purchaseManager = approvalNode(99202L, "采购主管审批", 2, List.of(11L), null, WfNode.MULTI_ANY);
        WfNode financeDirector = approvalNode(99203L, "财务总监审批", 3, List.of(21L), "amount > 50000", WfNode.MULTI_ANY);
        WfNode end = endNode(99204L, 4);
        store.addNode(purchaseManager);
        store.addNode(financeDirector);
        store.addNode(end);

        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("amount", new BigDecimal("80000"));
        WfInstance instance = engine.start(definition(), "PA_ORDER", 1L, "采购订单", STARTER_ID, vars);

        // 当前停在采购主管节点
        assertEquals(purchaseManager.getId(), instance.getCurrentNodeId());
        assertEquals(1, store.listTasks(instance.getId()).size());

        // 采购主管通过后，条件成立，激活财务总监
        engine.approve(store.listTasks(instance.getId()).get(0).getId(), 11L, "同意");
        List<WfTask> tasks = store.listTasks(instance.getId());
        assertEquals(2, tasks.size());
        assertEquals(financeDirector.getId(), tasks.get(1).getNodeId());
        assertEquals(WfInstance.STATUS_RUNNING, store.findInstance(instance.getId()).getStatus());
    }

    @Test
    @DisplayName("金额不大于 5 万时跳过财务总监节点直接结束")
    void conditionBranchSkipsFinanceDirectorForSmallAmount() {
        WfNode purchaseManager = approvalNode(99202L, "采购主管审批", 2, List.of(11L), null, WfNode.MULTI_ANY);
        WfNode financeDirector = approvalNode(99203L, "财务总监审批", 3, List.of(21L), "amount > 50000", WfNode.MULTI_ANY);
        WfNode end = endNode(99204L, 4);
        store.addNode(purchaseManager);
        store.addNode(financeDirector);
        store.addNode(end);

        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("amount", new BigDecimal("3000"));
        WfInstance instance = engine.start(definition(), "PA_ORDER", 1L, "采购订单", STARTER_ID, vars);

        WfFlowResult result = engine.approve(store.listTasks(instance.getId()).get(0).getId(), 11L, "同意");

        assertNotNull(result);
        assertTrue(result.isApproved());
        // 只有采购主管一个任务，财务总监被跳过
        assertEquals(1, store.listTasks(instance.getId()).size());
        assertEquals(WfInstance.STATUS_APPROVED, store.findInstance(instance.getId()).getStatus());
    }

    @Test
    @DisplayName("条件为 null 时视为不满足，跳过该节点")
    void conditionWithNullValueSkipsNode() {
        WfNode first = approvalNode(99202L, "主管审批", 2, List.of(11L), null, WfNode.MULTI_ANY);
        WfNode conditional = approvalNode(99203L, "条件审批", 3, List.of(21L), "amount > 100", WfNode.MULTI_ANY);
        WfNode end = endNode(99204L, 4);
        store.addNode(first);
        store.addNode(conditional);
        store.addNode(end);

        // 不传 amount，条件求值为 false
        WfInstance instance = engine.start(definition(), "PA_ORDER", 1L, "采购订单", STARTER_ID, Map.of());

        engine.approve(store.listTasks(instance.getId()).get(0).getId(), 11L, "同意");

        assertEquals(1, store.listTasks(instance.getId()).size());
        assertEquals(WfInstance.STATUS_APPROVED, store.findInstance(instance.getId()).getStatus());
    }

    // ------------------------------------------------------------------
    // 会签
    // ------------------------------------------------------------------

    @Test
    @DisplayName("会签：两人全部通过才流转到下一节点")
    void multiApproveAllRequiresEveryApprover() {
        WfNode counterSign = approvalNode(99202L, "财务会签", 2, List.of(11L, 12L), null, WfNode.MULTI_ALL);
        WfNode next = approvalNode(99203L, "总经理审批", 3, List.of(21L), null, WfNode.MULTI_ANY);
        WfNode end = endNode(99204L, 4);
        store.addNode(counterSign);
        store.addNode(next);
        store.addNode(end);

        WfInstance instance = engine.start(definition(), "PA_ORDER", 1L, "采购订单", STARTER_ID, Map.of());
        assertEquals(2, store.listTasks(instance.getId()).size());

        // 第一个人通过，流程不流转
        WfFlowResult result = engine.approve(store.listTasks(instance.getId()).get(0).getId(), 11L, "同意");
        assertNull(result);
        assertEquals(WfInstance.STATUS_RUNNING, store.findInstance(instance.getId()).getStatus());
        assertEquals(2, store.listTasks(instance.getId()).size());

        // 第二个人通过，流转到下一节点
        WfTask second = store.listTasks(instance.getId()).stream()
                .filter(task -> task.getAssigneeId().equals(12L))
                .findFirst().orElseThrow();
        engine.approve(second.getId(), 12L, "同意");
        assertEquals(next.getId(), store.findInstance(instance.getId()).getCurrentNodeId());
    }

    @Test
    @DisplayName("或签：任一通过即可流转")
    void multiApproveAnyAdvancesOnFirstApproval() {
        WfNode anyApprove = approvalNode(99202L, "经理或签", 2, List.of(11L, 12L), null, WfNode.MULTI_ANY);
        WfNode next = approvalNode(99203L, "总经理审批", 3, List.of(21L), null, WfNode.MULTI_ANY);
        WfNode end = endNode(99204L, 4);
        store.addNode(anyApprove);
        store.addNode(next);
        store.addNode(end);

        WfInstance instance = engine.start(definition(), "PA_ORDER", 1L, "采购订单", STARTER_ID, Map.of());

        WfFlowResult result = engine.approve(store.listTasks(instance.getId()).get(0).getId(), 11L, "同意");

        // 流程流转到下一个节点但尚未结束，因此不返回结果，避免业务侧被误判为审核完成
        assertNull(result);
        assertEquals(next.getId(), store.findInstance(instance.getId()).getCurrentNodeId());
        assertEquals(WfInstance.STATUS_RUNNING, store.findInstance(instance.getId()).getStatus());
        // 另一个待办被取消
        long pending = store.countTasks(instance.getId(), List.of(WfTask.STATUS_PENDING));
        assertEquals(1, pending);
    }

    @Test
    @DisplayName("最后一个节点通过后流程结束并返回结果")
    void lastNodeApprovalEndsFlow() {
        WfNode only = approvalNode(99202L, "主管审批", 2, List.of(11L), null, WfNode.MULTI_ANY);
        WfNode end = endNode(99204L, 4);
        store.addNode(only);
        store.addNode(end);

        WfInstance instance = engine.start(definition(), "PA_ORDER", 1L, "采购订单", STARTER_ID, Map.of());

        WfFlowResult result = engine.approve(store.listTasks(instance.getId()).get(0).getId(), 11L, "同意");

        assertNotNull(result);
        assertTrue(result.isApproved());
        assertEquals(WfInstance.STATUS_APPROVED, store.findInstance(instance.getId()).getStatus());
    }

    // ------------------------------------------------------------------
    // 驳回 / 退回
    // ------------------------------------------------------------------

    @Test
    @DisplayName("驳回：流程终止，实例状态置为已驳回")
    void rejectTerminatesInstance() {
        WfNode approval = approvalNode(99202L, "主管审批", 2, List.of(11L), null, WfNode.MULTI_ANY);
        WfNode end = endNode(99204L, 4);
        store.addNode(approval);
        store.addNode(end);

        WfInstance instance = engine.start(definition(), "PA_ORDER", 1L, "采购订单", STARTER_ID, Map.of());

        WfFlowResult result = engine.reject(store.listTasks(instance.getId()).get(0).getId(), 11L, "预算不足");

        assertNotNull(result);
        assertFalse(result.isApproved());
        assertEquals("预算不足", result.getReason());
        assertEquals(WfInstance.STATUS_REJECTED, store.findInstance(instance.getId()).getStatus());
        assertEquals(0, store.countTasks(instance.getId(), List.of(WfTask.STATUS_PENDING)));
    }

    @Test
    @DisplayName("退回：回到上一个已审批节点并重新生成待办")
    void rollbackReturnsToPreviousNode() {
        WfNode first = approvalNode(99202L, "主管审批", 2, List.of(11L), null, WfNode.MULTI_ANY);
        WfNode second = approvalNode(99203L, "财务审批", 3, List.of(21L), null, WfNode.MULTI_ANY);
        WfNode end = endNode(99204L, 4);
        store.addNode(first);
        store.addNode(second);
        store.addNode(end);

        WfInstance instance = engine.start(definition(), "PA_ORDER", 1L, "采购订单", STARTER_ID, Map.of());
        engine.approve(store.listTasks(instance.getId()).get(0).getId(), 11L, "同意");

        WfFlowResult result = engine.rollback(store.listTasks(instance.getId()).get(1).getId(), 21L, "请重新确认");

        assertNull(result);
        WfInstance updated = store.findInstance(instance.getId());
        assertEquals(first.getId(), updated.getCurrentNodeId());
        assertEquals(1, updated.getRollbackCount());
        // 第一个节点重新生成了待办，原任务被作废
        List<WfTask> tasks = store.listTasks(instance.getId());
        assertEquals(3, tasks.size());
        WfTask regenerated = tasks.get(2);
        assertEquals(WfTask.STATUS_PENDING, regenerated.getStatus());
        assertEquals(first.getId(), regenerated.getNodeId());
        assertEquals(11L, regenerated.getAssigneeId());
        assertEquals(WfTask.ACTION_ROLLBACK, regenerated.getActionType());
    }

    @Test
    @DisplayName("退回次数超过上限时强制终止流程")
    void rollbackExceedingLimitTerminatesInstance() {
        WfNode first = approvalNode(99202L, "主管审批", 2, List.of(11L), null, WfNode.MULTI_ANY);
        WfNode second = approvalNode(99203L, "财务审批", 3, List.of(21L), null, WfNode.MULTI_ANY);
        WfNode end = endNode(99204L, 4);
        store.addNode(first);
        store.addNode(second);
        store.addNode(end);

        WfInstance instance = engine.start(definition(), "PA_ORDER", 1L, "采购订单", STARTER_ID, Map.of());
        engine.approve(store.listTasks(instance.getId()).get(0).getId(), 11L, "同意");

        // 在两个节点之间反复退回，直到超过上限被强制终止
        WfFlowResult last = null;
        int rollbacks = 0;
        while (rollbacks <= WfConstants.MAX_ROLLBACK + 1) {
            List<WfTask> pending = store.listTasks(instance.getId()).stream()
                    .filter(task -> task.getStatus() == WfTask.STATUS_PENDING)
                    .toList();
            if (pending.isEmpty()) {
                break;
            }
            WfTask task = pending.get(0);
            if (task.getNodeId().equals(first.getId())) {
                // 退回后回到第一个节点，重新通过后再次到达第二个节点
                engine.approve(task.getId(), 11L, "重新通过");
            } else {
                last = engine.rollback(task.getId(), 21L, "再退");
                rollbacks++;
                if (last != null && !last.isApproved()) {
                    break;
                }
            }
        }

        assertNotNull(last);
        assertFalse(last.isApproved());
        assertEquals(WfInstance.STATUS_REJECTED, store.findInstance(instance.getId()).getStatus());
    }

    // ------------------------------------------------------------------
    // 转办 / 加签
    // ------------------------------------------------------------------

    @Test
    @DisplayName("转办：原任务置为已转办，新建目标人任务")
    void transferMovesTaskToTargetUser() {
        WfNode approval = approvalNode(99202L, "主管审批", 2, List.of(11L), null, WfNode.MULTI_ANY);
        WfNode end = endNode(99204L, 4);
        store.addNode(approval);
        store.addNode(end);

        WfInstance instance = engine.start(definition(), "PA_ORDER", 1L, "采购订单", STARTER_ID, Map.of());
        WfTask origin = store.listTasks(instance.getId()).get(0);

        engine.transfer(origin.getId(), 11L, 99L, "我出差了");

        List<WfTask> tasks = store.listTasks(instance.getId());
        assertEquals(2, tasks.size());
        assertEquals(WfTask.STATUS_TRANSFERRED, tasks.get(0).getStatus());
        assertEquals(WfTask.STATUS_PENDING, tasks.get(1).getStatus());
        assertEquals(99L, tasks.get(1).getAssigneeId());
        assertEquals(origin.getId(), tasks.get(1).getSourceTaskId());
        assertEquals(WfTask.ACTION_TRANSFER, tasks.get(1).getActionType());
    }

    @Test
    @DisplayName("加签：新增审批人任务，原任务保持待办")
    void countersignAddsNewTask() {
        WfNode approval = approvalNode(99202L, "主管审批", 2, List.of(11L), null, WfNode.MULTI_ANY);
        WfNode end = endNode(99204L, 4);
        store.addNode(approval);
        store.addNode(end);

        WfInstance instance = engine.start(definition(), "PA_ORDER", 1L, "采购订单", STARTER_ID, Map.of());
        WfTask origin = store.listTasks(instance.getId()).get(0);

        engine.countersign(origin.getId(), 11L, 99L, WfConstants.COUNTERSIGN_AFTER, "请财务一并确认");

        List<WfTask> tasks = store.listTasks(instance.getId());
        assertEquals(2, tasks.size());
        WfTask countersignTask = tasks.get(1);
        assertEquals(99L, countersignTask.getAssigneeId());
        assertEquals(WfTask.STATUS_PENDING, countersignTask.getStatus());
        assertEquals(WfTask.ACTION_COUNTERSIGN, countersignTask.getActionType());
        assertEquals(origin.getId(), countersignTask.getSourceTaskId());
    }

    @Test
    @DisplayName("前加签：原任务被挂起，加签人通过后恢复")
    void countersignBeforeSuspendsOriginalTask() {
        WfNode approval = approvalNode(99202L, "主管审批", 2, List.of(11L), null, WfNode.MULTI_ANY);
        WfNode end = endNode(99204L, 4);
        store.addNode(approval);
        store.addNode(end);

        WfInstance instance = engine.start(definition(), "PA_ORDER", 1L, "采购订单", STARTER_ID, Map.of());
        WfTask origin = store.listTasks(instance.getId()).get(0);

        engine.countersign(origin.getId(), 11L, 99L, WfConstants.COUNTERSIGN_BEFORE, "先审");

        List<WfTask> tasks = store.listTasks(instance.getId());
        assertEquals(WfTask.STATUS_WAITING, tasks.get(0).getStatus());
        assertEquals(WfTask.STATUS_PENDING, tasks.get(1).getStatus());

        // 加签人通过后，原任务恢复待办
        engine.approve(tasks.get(1).getId(), 99L, "同意");
        assertEquals(WfTask.STATUS_PENDING, store.findTask(origin.getId()).getStatus());
    }

    // ------------------------------------------------------------------
    // 撤回 / 挂起
    // ------------------------------------------------------------------

    @Test
    @DisplayName("撤回：发起人可撤回无人处理的流程")
    void withdrawCancelsInstance() {
        WfNode approval = approvalNode(99202L, "主管审批", 2, List.of(11L), null, WfNode.MULTI_ANY);
        WfNode end = endNode(99204L, 4);
        store.addNode(approval);
        store.addNode(end);

        WfInstance instance = engine.start(definition(), "PA_ORDER", 1L, "采购订单", STARTER_ID, Map.of());

        WfFlowResult result = engine.withdraw(instance.getId(), STARTER_ID, "填错了");

        assertNotNull(result);
        assertFalse(result.isApproved());
        assertEquals(WfInstance.STATUS_CANCELLED, store.findInstance(instance.getId()).getStatus());
        assertEquals(0, store.countTasks(instance.getId(), List.of(WfTask.STATUS_PENDING)));
    }

    @Test
    @DisplayName("撤回：非发起人不能撤回")
    void withdrawRejectedForNonStarter() {
        WfNode approval = approvalNode(99202L, "主管审批", 2, List.of(11L), null, WfNode.MULTI_ANY);
        WfNode end = endNode(99204L, 4);
        store.addNode(approval);
        store.addNode(end);

        WfInstance instance = engine.start(definition(), "PA_ORDER", 1L, "采购订单", STARTER_ID, Map.of());

        assertThrows(BizException.class, () -> engine.withdraw(instance.getId(), 11L, "我要撤回"));
    }

    @Test
    @DisplayName("撤回：流程已被处理后不能撤回")
    void withdrawRejectedAfterHandled() {
        WfNode approval = approvalNode(99202L, "主管审批", 2, List.of(11L), null, WfNode.MULTI_ANY);
        WfNode end = endNode(99204L, 4);
        store.addNode(approval);
        store.addNode(end);

        WfInstance instance = engine.start(definition(), "PA_ORDER", 1L, "采购订单", STARTER_ID, Map.of());
        engine.approve(store.listTasks(instance.getId()).get(0).getId(), 11L, "同意");

        assertThrows(BizException.class, () -> engine.withdraw(instance.getId(), STARTER_ID, "撤回"));
    }

    @Test
    @DisplayName("解析不到审批人时实例挂起并记录原因")
    void instanceSuspendedWhenNoApproverFound() {
        WfNode approval = approvalNode(99202L, "主管审批", 2, List.of(), null, WfNode.MULTI_ANY);
        approval.setApproverType(WfNode.APPROVER_TYPE_ROLE);
        approval.setApproverValue("999");
        WfNode end = endNode(99204L, 4);
        store.addNode(approval);
        store.addNode(end);

        WfInstance instance = engine.start(definition(), "PA_ORDER", 1L, "采购订单", STARTER_ID, Map.of());

        WfInstance updated = store.findInstance(instance.getId());
        assertEquals(WfInstance.STATUS_SUSPENDED, updated.getStatus());
        assertNotNull(updated.getSuspendReason());
        assertTrue(updated.getSuspendReason().contains("999"));
        assertEquals(0, store.countTasks(instance.getId(), List.of(WfTask.STATUS_PENDING)));
    }

    @Test
    @DisplayName("审批流水记录了每个动作")
    void recordsEveryAction() {
        WfNode approval = approvalNode(99202L, "主管审批", 2, List.of(11L), null, WfNode.MULTI_ANY);
        WfNode end = endNode(99204L, 4);
        store.addNode(approval);
        store.addNode(end);

        WfInstance instance = engine.start(definition(), "PA_ORDER", 1L, "采购订单", STARTER_ID, Map.of());
        engine.approve(store.listTasks(instance.getId()).get(0).getId(), 11L, "同意");

        List<String> actions = store.listRecords(instance.getId()).stream()
                .map(record -> record.getAction())
                .toList();
        assertTrue(actions.contains(com.erp.wf.domain.entity.WfTaskRecord.ACTION_SUBMIT));
        assertTrue(actions.contains(com.erp.wf.domain.entity.WfTaskRecord.ACTION_APPROVE));
        assertTrue(actions.contains(com.erp.wf.domain.entity.WfTaskRecord.ACTION_FINISH));
    }

    @Test
    @DisplayName("无权处理他人任务时抛出业务异常")
    void approveOtherUserTaskFails() {
        WfNode approval = approvalNode(99202L, "主管审批", 2, List.of(11L), null, WfNode.MULTI_ANY);
        WfNode end = endNode(99204L, 4);
        store.addNode(approval);
        store.addNode(end);

        WfInstance instance = engine.start(definition(), "PA_ORDER", 1L, "采购订单", STARTER_ID, Map.of());
        WfTask task = store.listTasks(instance.getId()).get(0);

        assertThrows(BizException.class, () -> engine.approve(task.getId(), 999L, "越权审批"));
    }

    // ------------------------------------------------------------------
    // 辅助
    // ------------------------------------------------------------------

    private WfDefinition definition() {
        WfDefinition definition = new WfDefinition();
        definition.setId(DEFINITION_ID);
        definition.setDefinitionKey("PA_ORDER_APPROVE");
        definition.setBizType("PA_ORDER");
        definition.setStatus(WfDefinition.STATUS_ENABLED);
        return definition;
    }

    private WfNode approvalNode(Long id, String name, int sortNo, List<Long> assigneeIds,
                                String conditionExpr, int multiApprove) {
        WfNode node = new WfNode();
        node.setId(id);
        node.setDefinitionId(DEFINITION_ID);
        node.setNodeKey(name);
        node.setNodeName(name);
        node.setNodeType(WfNode.TYPE_APPROVAL);
        node.setSortNo(sortNo);
        node.setMultiApprove(multiApprove);
        node.setConditionExpr(conditionExpr);
        node.setApproverType(WfNode.APPROVER_TYPE_USER);
        node.setApproverValue(assigneeIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(""));
        return node;
    }

    private WfNode endNode(Long id, int sortNo) {
        WfNode node = new WfNode();
        node.setId(id);
        node.setDefinitionId(DEFINITION_ID);
        node.setNodeKey("END");
        node.setNodeName("结束");
        node.setNodeType(WfNode.TYPE_END);
        node.setSortNo(sortNo);
        return node;
    }

    /**
     * 测试用的组织数据实现。
     * <p>
     * 默认所有用户都是启用状态，需要停用的用户显式登记到 disabledUsers。
     */
    private static final class TestDirectory implements WfDirectory {

        private final Map<Long, Long> userDepts = new LinkedHashMap<>();
        private final Map<Long, String> nicknames = new LinkedHashMap<>();
        private final Map<Long, List<Long>> roleUsers = new LinkedHashMap<>();
        private final java.util.Set<Long> disabledUsers = new java.util.LinkedHashSet<>();

        void registerUser(Long userId, String nickname, Long deptId) {
            nicknames.put(userId, nickname);
            userDepts.put(userId, deptId);
        }

        @Override
        public List<Long> userIdsByRoleIds(java.util.Collection<Long> roleIds) {
            List<Long> userIds = new java.util.ArrayList<>();
            for (Long roleId : roleIds) {
                userIds.addAll(roleUsers.getOrDefault(roleId, List.of()));
            }
            return userIds;
        }

        @Override
        public List<Long> filterEnabledUsers(java.util.Collection<Long> userIds) {
            List<Long> result = new java.util.ArrayList<>();
            for (Long userId : userIds) {
                if (!disabledUsers.contains(userId)) {
                    result.add(userId);
                }
            }
            return result;
        }

        @Override
        public Long deptIdOfUser(Long userId) {
            return userDepts.get(userId);
        }

        @Override
        public String nicknameOfUser(Long userId) {
            return nicknames.get(userId);
        }

        @Override
        public List<Long> deptLeaderUserIds(Long deptId) {
            return List.of();
        }

        @Override
        public List<Long> enabledUserIdsOfDept(Long deptId) {
            return List.of();
        }
    }
}








