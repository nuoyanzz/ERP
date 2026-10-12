package com.erp.wf.engine;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.wf.domain.entity.WfInstance;
import com.erp.wf.domain.entity.WfInstanceVariable;
import com.erp.wf.domain.entity.WfNode;
import com.erp.wf.domain.entity.WfTask;
import com.erp.wf.domain.entity.WfTaskRecord;
import com.erp.wf.mapper.WfInstanceMapper;
import com.erp.wf.mapper.WfInstanceVariableMapper;
import com.erp.wf.mapper.WfNodeMapper;
import com.erp.wf.mapper.WfTaskMapper;
import com.erp.wf.mapper.WfTaskRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * 引擎持久化的 MyBatis-Plus 实现。
 */
@Component
@RequiredArgsConstructor
public class WfStoreImpl implements WfStore {

    private final WfInstanceMapper instanceMapper;
    private final WfNodeMapper nodeMapper;
    private final WfTaskMapper taskMapper;
    private final WfTaskRecordMapper recordMapper;
    private final WfInstanceVariableMapper variableMapper;

    @Override
    public WfNode findNode(Long nodeId) {
        return nodeMapper.selectById(nodeId);
    }

    @Override
    public List<WfNode> listNodes(Long definitionId) {
        List<WfNode> nodes = nodeMapper.selectList(Wrappers.<WfNode>lambdaQuery()
                .eq(WfNode::getDefinitionId, definitionId)
                .orderByAsc(WfNode::getSortNo)
                .orderByAsc(WfNode::getId));
        return nodes == null ? List.of() : nodes;
    }

    @Override
    public WfInstance findInstance(Long instanceId) {
        return instanceMapper.selectById(instanceId);
    }

    @Override
    public void insertInstance(WfInstance instance) {
        instanceMapper.insert(instance);
    }

    @Override
    public void updateInstance(WfInstance instance) {
        instanceMapper.updateById(instance);
    }

    @Override
    public long countRunningInstances(String bizType, Long bizId) {
        Long count = instanceMapper.selectCount(Wrappers.<WfInstance>lambdaQuery()
                .eq(WfInstance::getBizType, bizType)
                .eq(WfInstance::getBizId, bizId)
                .eq(WfInstance::getStatus, WfInstance.STATUS_RUNNING));
        return count == null ? 0 : count;
    }

    @Override
    public WfTask findTask(Long taskId) {
        return taskMapper.selectById(taskId);
    }

    @Override
    public void insertTask(WfTask task) {
        taskMapper.insert(task);
    }

    @Override
    public void updateTask(WfTask task) {
        taskMapper.updateById(task);
    }

    @Override
    public List<WfTask> listTasks(Long instanceId) {
        List<WfTask> tasks = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
                .eq(WfTask::getInstanceId, instanceId)
                .orderByAsc(WfTask::getId));
        return tasks == null ? List.of() : tasks;
    }

    @Override
    public List<WfTask> listTasksByNode(Long instanceId, Long nodeId) {
        List<WfTask> tasks = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
                .eq(WfTask::getInstanceId, instanceId)
                .eq(WfTask::getNodeId, nodeId)
                .orderByAsc(WfTask::getId));
        return tasks == null ? List.of() : tasks;
    }

    @Override
    public long countTasks(Long instanceId, Collection<Integer> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return 0;
        }
        Long count = taskMapper.selectCount(Wrappers.<WfTask>lambdaQuery()
                .eq(WfTask::getInstanceId, instanceId)
                .in(WfTask::getStatus, statuses));
        return count == null ? 0 : count;
    }

    @Override
    public void insertRecord(WfTaskRecord record) {
        recordMapper.insert(record);
    }

    @Override
    public void insertVariable(WfInstanceVariable variable) {
        variableMapper.insert(variable);
    }

    @Override
    public List<WfInstanceVariable> listVariables(Long instanceId) {
        List<WfInstanceVariable> variables = variableMapper.selectList(
                Wrappers.<WfInstanceVariable>lambdaQuery()
                        .eq(WfInstanceVariable::getInstanceId, instanceId));
        return variables == null ? List.of() : variables;
    }
}








