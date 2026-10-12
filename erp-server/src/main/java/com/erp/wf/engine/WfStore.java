package com.erp.wf.engine;

import com.erp.wf.domain.entity.WfInstance;
import com.erp.wf.domain.entity.WfInstanceVariable;
import com.erp.wf.domain.entity.WfNode;
import com.erp.wf.domain.entity.WfTask;
import com.erp.wf.domain.entity.WfTaskRecord;

import java.util.Collection;
import java.util.List;

/**
 * 引擎持久化接口。
 * <p>
 * 抽成窄接口的目的：引擎的流转规则是本项目最复杂的部分，必须能脱离数据库做单元测试。
 * 同时把「查询条件怎么写」收敛在一处，避免各处 lambda 条件写法不一致导致状态统计出错。
 */
public interface WfStore {

    // ---------------- 节点 ----------------

    WfNode findNode(Long nodeId);

    /**
     * 查询流程定义的全部节点，按 sort_no 升序。
     */
    List<WfNode> listNodes(Long definitionId);

    // ---------------- 实例 ----------------

    WfInstance findInstance(Long instanceId);

    void insertInstance(WfInstance instance);

    void updateInstance(WfInstance instance);

    /**
     * 统计指定业务单据下运行中的流程数。
     */
    long countRunningInstances(String bizType, Long bizId);

    // ---------------- 任务 ----------------

    WfTask findTask(Long taskId);

    void insertTask(WfTask task);

    void updateTask(WfTask task);

    /**
     * 查询实例下的全部任务，按 ID 升序。
     */
    List<WfTask> listTasks(Long instanceId);

    /**
     * 查询实例下指定节点的任务，按 ID 升序。
     */
    List<WfTask> listTasksByNode(Long instanceId, Long nodeId);

    /**
     * 统计实例下处于指定状态的任务数。
     */
    long countTasks(Long instanceId, Collection<Integer> statuses);

    // ---------------- 流水 ----------------

    void insertRecord(WfTaskRecord record);

    // ---------------- 变量 ----------------

    void insertVariable(WfInstanceVariable variable);

    List<WfInstanceVariable> listVariables(Long instanceId);
}








