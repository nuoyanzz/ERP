package com.erp.wf.support;

import com.erp.wf.domain.entity.WfInstance;
import com.erp.wf.domain.entity.WfInstanceVariable;
import com.erp.wf.domain.entity.WfNode;
import com.erp.wf.domain.entity.WfTask;
import com.erp.wf.domain.entity.WfTaskRecord;
import com.erp.wf.engine.WfStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存版工作流存储，用于引擎单元测试。
 * <p>
 * 只实现 {@link WfStore} 声明的查询语义，让引擎的流转规则完全脱离数据库运行。
 */
public class InMemoryWfStore implements WfStore {

    private final Map<Long, WfInstance> instances = new ConcurrentHashMap<>();
    private final Map<Long, WfNode> nodes = new ConcurrentHashMap<>();
    private final Map<Long, WfTask> tasks = new ConcurrentHashMap<>();
    private final Map<Long, WfTaskRecord> records = new ConcurrentHashMap<>();
    private final Map<Long, WfInstanceVariable> variables = new ConcurrentHashMap<>();

    private final AtomicLong taskIdSeq = new AtomicLong(1000);
    private final AtomicLong recordIdSeq = new AtomicLong(2000);
    private final AtomicLong variableIdSeq = new AtomicLong(3000);

    public void addNode(WfNode node) {
        nodes.put(node.getId(), node);
    }

    @Override
    public WfNode findNode(Long nodeId) {
        return nodes.get(nodeId);
    }

    @Override
    public List<WfNode> listNodes(Long definitionId) {
        List<WfNode> result = new ArrayList<>();
        for (WfNode node : nodes.values()) {
            if (node.getDefinitionId().equals(definitionId)) {
                result.add(node);
            }
        }
        result.sort(Comparator.comparing(WfNode::getSortNo));
        return result;
    }

    @Override
    public WfInstance findInstance(Long instanceId) {
        return instances.get(instanceId);
    }

    @Override
    public void insertInstance(WfInstance instance) {
        if (instance.getId() == null) {
            instance.setId(500L);
        }
        instances.put(instance.getId(), instance);
    }

    @Override
    public void updateInstance(WfInstance instance) {
        WfInstance existing = instances.get(instance.getId());
        if (existing == null) {
            instances.put(instance.getId(), instance);
            return;
        }
        // 模拟 MyBatis-Plus 的按需更新：只覆盖非 null 字段
        if (instance.getStatus() != null) {
            existing.setStatus(instance.getStatus());
        }
        if (instance.getCurrentNodeId() != null) {
            existing.setCurrentNodeId(instance.getCurrentNodeId());
        }
        if (instance.getSuspendReason() != null) {
            existing.setSuspendReason(instance.getSuspendReason());
        }
        if (instance.getRollbackCount() != null) {
            existing.setRollbackCount(instance.getRollbackCount());
        }
        if (instance.getFinishTime() != null) {
            existing.setFinishTime(instance.getFinishTime());
        }
        if (instance.getRemark() != null) {
            existing.setRemark(instance.getRemark());
        }
    }

    @Override
    public long countRunningInstances(String bizType, Long bizId) {
        return instances.values().stream()
                .filter(instance -> instance.getBizType().equals(bizType)
                        && instance.getBizId().equals(bizId)
                        && instance.getStatus() != null
                        && instance.getStatus() == WfInstance.STATUS_RUNNING)
                .count();
    }

    @Override
    public WfTask findTask(Long taskId) {
        return tasks.get(taskId);
    }

    @Override
    public void insertTask(WfTask task) {
        task.setId(taskIdSeq.incrementAndGet());
        tasks.put(task.getId(), task);
    }

    @Override
    public void updateTask(WfTask task) {
        WfTask existing = tasks.get(task.getId());
        if (existing == null) {
            tasks.put(task.getId(), task);
            return;
        }
        if (task.getStatus() != null) {
            existing.setStatus(task.getStatus());
        }
        if (task.getComment() != null) {
            existing.setComment(task.getComment());
        }
        if (task.getOperateBy() != null) {
            existing.setOperateBy(task.getOperateBy());
        }
        if (task.getOperateTime() != null) {
            existing.setOperateTime(task.getOperateTime());
        }
        if (task.getRemindCount() != null) {
            existing.setRemindCount(task.getRemindCount());
        }
    }

    @Override
    public List<WfTask> listTasks(Long instanceId) {
        List<WfTask> result = new ArrayList<>();
        for (WfTask task : tasks.values()) {
            if (task.getInstanceId().equals(instanceId)) {
                result.add(task);
            }
        }
        result.sort(Comparator.comparing(WfTask::getId));
        return result;
    }

    @Override
    public List<WfTask> listTasksByNode(Long instanceId, Long nodeId) {
        List<WfTask> result = new ArrayList<>();
        for (WfTask task : tasks.values()) {
            if (task.getInstanceId().equals(instanceId) && task.getNodeId().equals(nodeId)) {
                result.add(task);
            }
        }
        result.sort(Comparator.comparing(WfTask::getId));
        return result;
    }

    @Override
    public long countTasks(Long instanceId, Collection<Integer> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return 0;
        }
        return listTasks(instanceId).stream()
                .filter(task -> task.getStatus() != null && statuses.contains(task.getStatus()))
                .count();
    }

    @Override
    public void insertRecord(WfTaskRecord record) {
        record.setId(recordIdSeq.incrementAndGet());
        records.put(record.getId(), record);
    }

    @Override
    public void insertVariable(WfInstanceVariable variable) {
        variable.setId(variableIdSeq.incrementAndGet());
        variables.put(variable.getId(), variable);
    }

    @Override
    public List<WfInstanceVariable> listVariables(Long instanceId) {
        List<WfInstanceVariable> result = new ArrayList<>();
        for (WfInstanceVariable variable : variables.values()) {
            if (variable.getInstanceId().equals(instanceId)) {
                result.add(variable);
            }
        }
        return result;
    }

    /**
     * 查询实例下的审批流水，用于断言动作轨迹。
     */
    public List<WfTaskRecord> listRecords(Long instanceId) {
        List<WfTaskRecord> result = new ArrayList<>();
        for (WfTaskRecord record : records.values()) {
            if (record.getInstanceId().equals(instanceId)) {
                result.add(record);
            }
        }
        result.sort(Comparator.comparing(WfTaskRecord::getId));
        return result;
    }
}








