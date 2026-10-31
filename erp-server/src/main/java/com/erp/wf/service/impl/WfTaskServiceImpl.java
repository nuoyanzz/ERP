package com.erp.wf.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.security.SecurityUtils;
import com.erp.system.domain.entity.SysUser;
import com.erp.system.mapper.SysUserMapper;
import com.erp.wf.domain.dto.WfApproveRequest;
import com.erp.wf.domain.entity.WfInstance;
import com.erp.wf.domain.entity.WfNode;
import com.erp.wf.domain.entity.WfTask;
import com.erp.wf.domain.vo.WfTaskVO;
import com.erp.wf.engine.WfEngine;
import com.erp.wf.integration.ApprovalDispatcher;
import com.erp.wf.mapper.WfInstanceMapper;
import com.erp.wf.mapper.WfNodeMapper;
import com.erp.wf.mapper.WfTaskMapper;
import com.erp.wf.service.WfTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WfTaskServiceImpl extends ServiceImpl<WfTaskMapper, WfTask> implements WfTaskService {

    private final WfInstanceMapper instanceMapper;
    private final WfNodeMapper nodeMapper;
    private final SysUserMapper userMapper;
    private final WfEngine engine;
    private final ApprovalDispatcher dispatcher;

    @Override
    public PageResult<WfTaskVO> todoPage(int pageNum, int pageSize, String bizType, Long bizId) {
        Long currentUserId = requireCurrentUser();
        var query = Wrappers.<WfTask>lambdaQuery()
                .eq(WfTask::getAssigneeId, currentUserId)
                .eq(WfTask::getStatus, WfTask.STATUS_PENDING)
                .orderByAsc(WfTask::getId);
        List<Long> instanceIds = filterInstanceIds(bizType, bizId);
        if (instanceIds != null) {
            if (instanceIds.isEmpty()) {
                return emptyPage(pageNum, pageSize);
            }
            query.in(WfTask::getInstanceId, instanceIds);
        }
        return toPageResult(this.page(new Page<>(pageNum, pageSize), query));
    }

    @Override
    public PageResult<WfTaskVO> donePage(int pageNum, int pageSize, String bizType, Long bizId) {
        Long currentUserId = requireCurrentUser();
        var query = Wrappers.<WfTask>lambdaQuery()
                .eq(WfTask::getOperateBy, currentUserId)
                .in(WfTask::getStatus, WfTask.STATUS_APPROVED, WfTask.STATUS_REJECTED)
                .orderByDesc(WfTask::getOperateTime);
        List<Long> instanceIds = filterInstanceIds(bizType, bizId);
        if (instanceIds != null) {
            if (instanceIds.isEmpty()) {
                return emptyPage(pageNum, pageSize);
            }
            query.in(WfTask::getInstanceId, instanceIds);
        }
        return toPageResult(this.page(new Page<>(pageNum, pageSize), query));
    }

    /**
     * 按业务类型/单据解析流程实例 ID 集合。
     *
     * @return null 表示不需要过滤；空集合表示过滤后无匹配实例
     */
    private List<Long> filterInstanceIds(String bizType, Long bizId) {
        boolean hasType = bizType != null && !bizType.isBlank();
        boolean hasId = bizId != null;
        if (!hasType && !hasId) {
            return null;
        }
        var query = Wrappers.<WfInstance>lambdaQuery();
        if (hasType) {
            query.eq(WfInstance::getBizType, bizType.trim());
        }
        if (hasId) {
            query.eq(WfInstance::getBizId, bizId);
        }
        return instanceMapper.selectList(query).stream().map(WfInstance::getId).collect(Collectors.toList());
    }

    private PageResult<WfTaskVO> emptyPage(int pageNum, int pageSize) {
        Page<WfTaskVO> voPage = new Page<>(pageNum, pageSize, 0);
        voPage.setRecords(Collections.emptyList());
        return PageResult.of(voPage);
    }

    @Override
    public PageResult<WfTaskVO> pageInstanceTasks(Long instanceId, int pageNum, int pageSize) {
        Page<WfTask> page = this.page(new Page<>(pageNum, pageSize), Wrappers.<WfTask>lambdaQuery()
                .eq(WfTask::getInstanceId, instanceId)
                .orderByAsc(WfTask::getId));
        return toPageResult(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveTask(Long taskId, WfApproveRequest request) {
        Long currentUserId = requireCurrentUser();
        engine.approve(taskId, currentUserId, request == null ? null : request.getComment());
        dispatcher.dispatchByInstanceState(instanceIdOf(taskId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectTask(Long taskId, WfApproveRequest request) {
        Long currentUserId = requireCurrentUser();
        engine.reject(taskId, currentUserId, request == null ? null : request.getComment());
        dispatcher.dispatchByInstanceState(instanceIdOf(taskId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackTask(Long taskId, WfApproveRequest request) {
        Long currentUserId = requireCurrentUser();
        engine.rollback(taskId, currentUserId, request == null ? null : request.getComment());
        dispatcher.dispatchByInstanceState(instanceIdOf(taskId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferTask(Long taskId, Long targetUserId, String comment) {
        Long currentUserId = requireCurrentUser();
        engine.transfer(taskId, currentUserId, targetUserId, comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void countersignTask(Long taskId, Long targetUserId, Integer position, String comment) {
        Long currentUserId = requireCurrentUser();
        engine.countersign(taskId, currentUserId, targetUserId, position, comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void urgeTask(Long taskId) {
        Long currentUserId = requireCurrentUser();
        engine.urge(taskId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPendingTasks(Long instanceId) {
        engine.cancelPendingTasks(instanceId);
    }

    /**
     * 查询任务所属的流程实例 ID。
     */
    private Long instanceIdOf(Long taskId) {
        WfTask task = this.getById(taskId);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        return task.getInstanceId();
    }

    private PageResult<WfTaskVO> toPageResult(Page<WfTask> page) {
        Page<WfTaskVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(toTaskVOs(page.getRecords()));
        return PageResult.of(voPage);
    }

    private List<WfTaskVO> toTaskVOs(List<WfTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> instanceIds = tasks.stream().map(WfTask::getInstanceId).distinct().collect(Collectors.toList());
        Map<Long, WfInstance> instanceMap = instanceMapper.selectList(Wrappers.<WfInstance>lambdaQuery()
                        .in(WfInstance::getId, instanceIds))
                .stream()
                .collect(Collectors.toMap(WfInstance::getId, Function.identity()));
        List<Long> userIds = tasks.stream().map(WfTask::getAssigneeId).distinct().collect(Collectors.toList());
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
            SysUser assignee = userMap.get(task.getAssigneeId());
            if (assignee != null) {
                vo.setAssigneeName(assignee.getNickname());
            }
            WfInstance instance = instanceMap.get(task.getInstanceId());
            if (instance != null) {
                vo.setInstanceTitle(instance.getTitle());
                vo.setBizType(instance.getBizType());
                vo.setBizId(instance.getBizId());
            }
            if (task.getOperateBy() != null) {
                SysUser operator = userMap.get(task.getOperateBy());
                if (operator == null) {
                    operator = userMapper.selectById(task.getOperateBy());
                }
                if (operator != null) {
                    vo.setOperateByName(operator.getNickname());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private Long requireCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BizException("登录已失效，请重新登录");
        }
        return userId;
    }
}








