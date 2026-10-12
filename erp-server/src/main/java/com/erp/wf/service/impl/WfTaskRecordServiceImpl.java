package com.erp.wf.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.system.domain.entity.SysUser;
import com.erp.system.mapper.SysUserMapper;
import com.erp.wf.domain.entity.WfNode;
import com.erp.wf.domain.entity.WfTaskRecord;
import com.erp.wf.domain.vo.WfTaskRecordVO;
import com.erp.wf.mapper.WfNodeMapper;
import com.erp.wf.mapper.WfTaskRecordMapper;
import com.erp.wf.service.WfTaskRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WfTaskRecordServiceImpl extends ServiceImpl<WfTaskRecordMapper, WfTaskRecord>
        implements WfTaskRecordService {

    private final WfNodeMapper nodeMapper;
    private final SysUserMapper userMapper;

    @Override
    public PageResult<WfTaskRecordVO> pageInstanceRecords(Long instanceId, int pageNum, int pageSize) {
        Page<WfTaskRecord> page = this.page(new Page<>(pageNum, pageSize), Wrappers.<WfTaskRecord>lambdaQuery()
                .eq(WfTaskRecord::getInstanceId, instanceId)
                .orderByAsc(WfTaskRecord::getOperateTime)
                .orderByAsc(WfTaskRecord::getId));
        Page<WfTaskRecordVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(toVOs(page.getRecords()));
        return PageResult.of(voPage);
    }

    private List<WfTaskRecordVO> toVOs(List<WfTaskRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> nodeIds = records.stream().map(WfTaskRecord::getNodeId).distinct().collect(Collectors.toList());
        Map<Long, WfNode> nodeMap = nodeMapper.selectList(Wrappers.<WfNode>lambdaQuery().in(WfNode::getId, nodeIds))
                .stream()
                .collect(Collectors.toMap(WfNode::getId, Function.identity()));
        List<Long> operatorIds = records.stream()
                .map(WfTaskRecord::getOperatorId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, SysUser> userMap = operatorIds.isEmpty() ? Map.of()
                : userMapper.selectList(Wrappers.<SysUser>lambdaQuery().in(SysUser::getId, operatorIds))
                .stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        return records.stream().map(record -> {
            WfTaskRecordVO vo = new WfTaskRecordVO();
            BeanUtils.copyProperties(record, vo);
            WfNode node = nodeMap.get(record.getNodeId());
            if (node != null) {
                vo.setNodeName(node.getNodeName());
            }
            SysUser operator = userMap.get(record.getOperatorId());
            if (operator != null) {
                vo.setOperatorName(operator.getNickname());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}








