package com.erp.wf.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.security.SecurityUtils;
import com.erp.system.domain.entity.SysUser;
import com.erp.system.mapper.SysUserMapper;
import com.erp.wf.domain.dto.WfInstanceQueryRequest;
import com.erp.wf.domain.dto.WfStartRequest;
import com.erp.wf.domain.entity.WfDefinition;
import com.erp.wf.domain.entity.WfInstance;
import com.erp.wf.domain.entity.WfNode;
import com.erp.wf.domain.entity.WfTask;
import com.erp.wf.domain.vo.WfInstanceDetailVO;
import com.erp.wf.domain.vo.WfInstanceVO;
import com.erp.wf.domain.vo.WfNodeBranchVO;
import com.erp.wf.domain.vo.WfTaskRecordVO;
import com.erp.wf.domain.vo.WfTaskVO;
import com.erp.wf.engine.WfEngine;
import com.erp.wf.engine.WfFlowResult;
import com.erp.wf.integration.ApprovalDispatcher;
import com.erp.wf.mapper.WfDefinitionMapper;
import com.erp.wf.mapper.WfInstanceMapper;
import com.erp.wf.mapper.WfNodeMapper;
import com.erp.wf.mapper.WfTaskMapper;
import com.erp.wf.service.WfInstanceService;
import com.erp.wf.service.WfTaskRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WfInstanceServiceImpl extends ServiceImpl<WfInstanceMapper, WfInstance> implements WfInstanceService {

    private final WfDefinitionMapper definitionMapper;
    private final WfNodeMapper nodeMapper;
    private final WfTaskMapper taskMapper;
    private final SysUserMapper userMapper;
    private final WfEngine engine;
    private final ApprovalDispatcher dispatcher;
    private final WfTaskRecordService recordService;

    @Override
    public PageResult<WfInstanceVO> pageInstance(WfInstanceQueryRequest request) {
        Page<WfInstance> page = this.page(request.toPage(), Wrappers.<WfInstance>lambdaQuery()
                .like(StringUtils.hasText(request.getTitle()), WfInstance::getTitle, request.getTitle())
                .eq(StringUtils.hasText(request.getBizType()), WfInstance::getBizType, request.getBizType())
                .eq(request.getStatus() != null, WfInstance::getStatus, request.getStatus())
                .orderByDesc(WfInstance::getId));
        List<WfInstanceVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<WfInstanceVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public WfInstanceDetailVO getInstanceDetail(Long id) {
        WfInstance instance = this.getById(id);
        if (instance == null) {
            throw new BizException("流程实例不存在");
        }
        WfInstanceDetailVO detail = new WfInstanceDetailVO();
        BeanUtils.copyProperties(toVO(instance), detail);
        List<WfTask> tasks = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
                .eq(WfTask::getInstanceId, id)
                .orderByAsc(WfTask::getId));
        detail.setTasks(toTaskVOs(tasks));
        detail.setRecords(recordService.pageInstanceRecords(id, 1, 500).getRecords());
        detail.setBranchTree(buildBranchTree(instance.getDefinitionId()));
        return detail;
    }

    /**
     * 构建流程的条件分支树。
     */
    private List<WfNodeBranchVO> buildBranchTree(Long definitionId) {
        List<WfNode> nodes = nodeMapper.selectList(Wrappers.<WfNode>lambdaQuery()
                .eq(WfNode::getDefinitionId, definitionId)
                .orderByAsc(WfNode::getSortNo));
        List<WfNodeBranchVO> roots = new ArrayList<>();
        WfNodeBranchVO pendingCondition = null;
        for (WfNode node : nodes) {
            Integer nodeType = node.getNodeType() == null ? WfNode.TYPE_APPROVAL : node.getNodeType();
            WfNodeBranchVO branch = new WfNodeBranchVO();
            branch.setId(node.getId());
            branch.setNodeKey(node.getNodeKey());
            branch.setNodeName(node.getNodeName());
            branch.setNodeType(node.getNodeType());
            branch.setSortNo(node.getSortNo());
            branch.setConditionExpr(node.getConditionExpr());
            branch.setApproverType(node.getApproverType());
            branch.setApproverValue(node.getApproverValue());
            branch.setMultiApprove(node.getMultiApprove());
            if (nodeType == WfNode.TYPE_CONDITION) {
                pendingCondition = branch;
                roots.add(branch);
            } else if (pendingCondition != null) {
                pendingCondition.getChildren().add(branch);
                pendingCondition = null;
            } else {
                roots.add(branch);
            }
        }
        return roots;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long startInstance(WfStartRequest request) {
        WfDefinition definition = definitionMapper.selectOne(Wrappers.<WfDefinition>lambdaQuery()
                .eq(WfDefinition::getDefinitionKey, request.getDefinitionKey())
                .last("limit 1"));
        if (definition == null || definition.getStatus() == null || definition.getStatus() != WfDefinition.STATUS_ENABLED) {
            throw new BizException("流程不存在或未发布");
        }
        WfInstance instance = engine.start(definition, request.getBizType(), request.getBizId(), request.getTitle(),
                SecurityUtils.getCurrentUserId(), null);
        if (instance.getStatus() != null && instance.getStatus() == WfInstance.STATUS_APPROVED) {
            dispatcher.dispatchApproved(instance.getId(), "流程审批通过");
        }
        return instance.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelInstance(Long id) {
        WfInstance instance = this.getById(id);
        if (instance == null) {
            throw new BizException("流程实例不存在");
        }
        if (instance.getStatus() == null || instance.getStatus() != WfInstance.STATUS_RUNNING) {
            throw new BizException("只有运行中的流程才能撤销");
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!instance.getStarterId().equals(currentUserId)) {
            throw new BizException("只有发起人才能撤销流程");
        }
        engine.cancelPendingTasks(id);
        WfInstance update = new WfInstance();
        update.setId(id);
        update.setStatus(WfInstance.STATUS_CANCELLED);
        update.setFinishTime(java.time.LocalDateTime.now());
        this.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawInstance(Long id, String comment) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        WfFlowResult result = engine.withdraw(id, currentUserId, comment);
        if (result != null) {
            dispatcher.dispatchRejected(result.getInstanceId(), result.getReason());
        }
    }

    private WfInstanceVO toVO(WfInstance instance) {
        WfInstanceVO vo = new WfInstanceVO();
        BeanUtils.copyProperties(instance, vo);
        WfDefinition definition = definitionMapper.selectById(instance.getDefinitionId());
        if (definition != null) {
            vo.setDefinitionKey(definition.getDefinitionKey());
            vo.setDefinitionName(definition.getDefinitionName());
        }
        SysUser starter = userMapper.selectById(instance.getStarterId());
        if (starter != null) {
            vo.setStarterName(starter.getNickname());
        }
        if (instance.getCurrentNodeId() != null) {
            WfNode node = nodeMapper.selectById(instance.getCurrentNodeId());
            if (node != null) {
                vo.setCurrentNodeName(node.getNodeName());
            }
        }
        return vo;
    }

    private List<WfTaskVO> toTaskVOs(List<WfTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = tasks.stream()
                .map(WfTask::getAssigneeId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, SysUser> userMap = userMapper.selectList(Wrappers.<SysUser>lambdaQuery().in(SysUser::getId, userIds))
                .stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        List<Long> nodeIds = tasks.stream().map(WfTask::getNodeId).distinct().collect(Collectors.toList());
        Map<Long, WfNode> nodeMap = nodeMapper.selectList(Wrappers.<WfNode>lambdaQuery().in(WfNode::getId, nodeIds))
                .stream()
                .collect(Collectors.toMap(WfNode::getId, Function.identity()));
        return tasks.stream().map(task -> {
            WfTaskVO vo = new WfTaskVO();
            BeanUtils.copyProperties(task, vo);
            WfNode node = nodeMap.get(task.getNodeId());
            if (node != null) {
                vo.setNodeName(node.getNodeName());
            }
            SysUser user = userMap.get(task.getAssigneeId());
            if (user != null) {
                vo.setAssigneeName(user.getNickname());
                vo.setOperateByName(user.getNickname());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}








