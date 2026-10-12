package com.erp.wf.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.wf.domain.dto.WfDefinitionSaveRequest;
import com.erp.wf.domain.dto.WfNodeRequest;
import com.erp.wf.domain.entity.WfDefinition;
import com.erp.wf.domain.entity.WfNode;
import com.erp.wf.domain.vo.WfDefinitionVO;
import com.erp.wf.domain.vo.WfNodeBranchVO;
import com.erp.wf.domain.vo.WfNodeVO;
import com.erp.wf.mapper.WfDefinitionMapper;
import com.erp.wf.mapper.WfNodeMapper;
import com.erp.wf.service.WfDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WfDefinitionServiceImpl extends ServiceImpl<WfDefinitionMapper, WfDefinition> implements WfDefinitionService {

    private final WfNodeMapper nodeMapper;

    @Override
    public PageResult<WfDefinitionVO> pageDefinition(int pageNum, int pageSize, String definitionKey, String definitionName, Integer status) {
        Page<WfDefinition> page = this.page(new Page<>(pageNum, pageSize), Wrappers.<WfDefinition>lambdaQuery()
                .like(StringUtils.hasText(definitionKey), WfDefinition::getDefinitionKey, definitionKey)
                .like(StringUtils.hasText(definitionName), WfDefinition::getDefinitionName, definitionName)
                .eq(status != null, WfDefinition::getStatus, status)
                .orderByDesc(WfDefinition::getId));
        List<WfDefinitionVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<WfDefinitionVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public WfDefinitionVO getDefinitionDetail(Long id) {
        WfDefinition definition = this.getById(id);
        if (definition == null) {
            throw new BizException("流程定义不存在");
        }
        WfDefinitionVO vo = toVO(definition);
        vo.setNodes(loadNodes(id));
        vo.setBranchTree(buildBranchTree(nodeMapper.selectList(Wrappers.<WfNode>lambdaQuery()
                .eq(WfNode::getDefinitionId, id)
                .orderByAsc(WfNode::getSortNo))));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDefinition(WfDefinitionSaveRequest request) {
        Long count = this.count(Wrappers.<WfDefinition>lambdaQuery().eq(WfDefinition::getDefinitionKey, request.getDefinitionKey()));
        if (count != null && count > 0) {
            throw new BizException("流程编码已存在");
        }
        WfDefinition definition = new WfDefinition();
        BeanUtils.copyProperties(request, definition);
        definition.setStatus(WfDefinition.STATUS_DISABLED);
        definition.setVersion(1);
        this.save(definition);
        saveNodes(definition.getId(), request.getNodes());
        return definition.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDefinition(WfDefinitionSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        WfDefinition exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("流程定义不存在");
        }
        if (!exist.getDefinitionKey().equals(request.getDefinitionKey())) {
            Long count = this.count(Wrappers.<WfDefinition>lambdaQuery()
                    .eq(WfDefinition::getDefinitionKey, request.getDefinitionKey())
                    .ne(WfDefinition::getId, request.getId()));
            if (count != null && count > 0) {
                throw new BizException("流程编码已存在");
            }
        }
        WfDefinition definition = new WfDefinition();
        definition.setId(request.getId());
        definition.setDefinitionKey(request.getDefinitionKey());
        definition.setDefinitionName(request.getDefinitionName());
        definition.setBizType(request.getBizType());
        definition.setDescription(request.getDescription());
        definition.setRemark(request.getRemark());
        this.updateById(definition);
        nodeMapper.delete(Wrappers.<WfNode>lambdaQuery().eq(WfNode::getDefinitionId, request.getId()));
        saveNodes(request.getId(), request.getNodes());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDefinition(Long id) {
        WfDefinition exist = this.getById(id);
        if (exist == null) {
            throw new BizException("流程定义不存在");
        }
        this.removeById(id);
        nodeMapper.delete(Wrappers.<WfNode>lambdaQuery().eq(WfNode::getDefinitionId, id));
    }

    @Override
    public void publishDefinition(Long id) {
        updateStatus(id, WfDefinition.STATUS_ENABLED, "流程定义不存在");
    }

    @Override
    public void disableDefinition(Long id) {
        updateStatus(id, WfDefinition.STATUS_DISABLED, "流程定义不存在");
    }

    @Override
    public List<WfDefinitionVO> listByBizType(String bizType, boolean onlyEnabled) {
        if (!StringUtils.hasText(bizType)) {
            return Collections.emptyList();
        }
        List<WfDefinition> definitions = this.list(Wrappers.<WfDefinition>lambdaQuery()
                .eq(WfDefinition::getBizType, bizType)
                .eq(onlyEnabled, WfDefinition::getStatus, WfDefinition.STATUS_ENABLED)
                .orderByDesc(WfDefinition::getVersion)
                .orderByDesc(WfDefinition::getId));
        if (definitions == null || definitions.isEmpty()) {
            return Collections.emptyList();
        }
        return definitions.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public WfDefinitionVO getPublishedByBizType(String bizType) {
        if (!StringUtils.hasText(bizType)) {
            return null;
        }
        WfDefinition definition = this.getOne(Wrappers.<WfDefinition>lambdaQuery()
                .eq(WfDefinition::getBizType, bizType)
                .eq(WfDefinition::getStatus, WfDefinition.STATUS_ENABLED)
                .orderByDesc(WfDefinition::getVersion)
                .orderByDesc(WfDefinition::getId)
                .last("limit 1"), false);
        return definition == null ? null : toVO(definition);
    }

    private void updateStatus(Long id, int status, String notFoundMessage) {
        WfDefinition exist = this.getById(id);
        if (exist == null) {
            throw new BizException(notFoundMessage);
        }
        WfDefinition update = new WfDefinition();
        update.setId(id);
        update.setStatus(status);
        this.updateById(update);
    }

    private void saveNodes(Long definitionId, List<WfNodeRequest> nodeRequests) {
        for (WfNodeRequest nodeRequest : nodeRequests) {
            WfNode node = new WfNode();
            BeanUtils.copyProperties(nodeRequest, node);
            node.setDefinitionId(definitionId);
            node.setNodeType(nodeRequest.getNodeType() == null ? WfNode.TYPE_APPROVAL : nodeRequest.getNodeType());
            node.setSortNo(nodeRequest.getSortNo() == null ? 0 : nodeRequest.getSortNo());
            node.setApproverType(nodeRequest.getApproverType() == null ? WfNode.APPROVER_TYPE_ROLE : nodeRequest.getApproverType());
            node.setMultiApprove(nodeRequest.getMultiApprove() == null ? WfNode.MULTI_ANY : nodeRequest.getMultiApprove());
            node.setCcFlag(nodeRequest.getCcFlag() == null ? 0 : nodeRequest.getCcFlag());
            node.setTimeoutHours(nodeRequest.getTimeoutHours() == null ? 0 : nodeRequest.getTimeoutHours());
            nodeMapper.insert(node);
        }
    }

    private List<WfNodeVO> loadNodes(Long definitionId) {
        List<WfNode> nodes = nodeMapper.selectList(Wrappers.<WfNode>lambdaQuery()
                .eq(WfNode::getDefinitionId, definitionId)
                .orderByAsc(WfNode::getSortNo));
        if (nodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<WfNodeVO> vos = new ArrayList<>(nodes.size());
        for (WfNode node : nodes) {
            WfNodeVO vo = new WfNodeVO();
            BeanUtils.copyProperties(node, vo);
            vos.add(vo);
        }
        return vos;
    }

    /**
     * 构建条件分支树。
     * <p>
     * 条件节点的每个分支由「条件节点 + 其后第一个审批节点」构成，
     * 因此这里把条件节点作为父节点，把紧随其后的分支节点挂到它的 children 下。
     */
    private List<WfNodeBranchVO> buildBranchTree(List<WfNode> nodes) {
        List<WfNodeBranchVO> roots = new ArrayList<>();
        if (nodes == null || nodes.isEmpty()) {
            return roots;
        }
        WfNodeBranchVO pendingCondition = null;
        for (WfNode node : nodes) {
            Integer nodeType = node.getNodeType() == null ? WfNode.TYPE_APPROVAL : node.getNodeType();
            if (nodeType == WfNode.TYPE_CONDITION) {
                pendingCondition = toBranchVO(node);
                roots.add(pendingCondition);
                continue;
            }
            WfNodeBranchVO branch = toBranchVO(node);
            if (pendingCondition != null) {
                pendingCondition.getChildren().add(branch);
                // 一个条件节点只承载一个直接子分支，后续节点回到顶层
                pendingCondition = null;
            } else {
                roots.add(branch);
            }
        }
        return roots;
    }

    private WfNodeBranchVO toBranchVO(WfNode node) {
        WfNodeBranchVO vo = new WfNodeBranchVO();
        vo.setId(node.getId());
        vo.setNodeKey(node.getNodeKey());
        vo.setNodeName(node.getNodeName());
        vo.setNodeType(node.getNodeType());
        vo.setSortNo(node.getSortNo());
        vo.setConditionExpr(node.getConditionExpr());
        vo.setApproverType(node.getApproverType());
        vo.setApproverValue(node.getApproverValue());
        vo.setMultiApprove(node.getMultiApprove());
        return vo;
    }

    private WfDefinitionVO toVO(WfDefinition definition) {
        WfDefinitionVO vo = new WfDefinitionVO();
        BeanUtils.copyProperties(definition, vo);
        return vo;
    }
}








