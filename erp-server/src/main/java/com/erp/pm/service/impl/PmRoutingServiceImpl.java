package com.erp.pm.service.impl;

import com.erp.common.util.SerialNoGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.pm.domain.dto.PmRoutingOperationRequest;
import com.erp.pm.domain.dto.PmRoutingSaveRequest;
import com.erp.pm.domain.entity.PmRouting;
import com.erp.pm.domain.entity.PmRoutingOperation;
import com.erp.pm.domain.entity.PmWorkCenter;
import com.erp.pm.domain.vo.PmRoutingOperationVO;
import com.erp.pm.domain.vo.PmRoutingVO;
import com.erp.pm.domain.vo.PmWorkCenterVO;
import com.erp.pm.mapper.PmRoutingMapper;
import com.erp.pm.mapper.PmRoutingOperationMapper;
import com.erp.pm.mapper.PmWorkCenterMapper;
import com.erp.pm.service.PmRoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PmRoutingServiceImpl extends ServiceImpl<PmRoutingMapper, PmRouting> implements PmRoutingService {

    private static final String NO_PREFIX = "RT-";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final PmRoutingOperationMapper operationMapper;
    private final PmWorkCenterMapper workCenterMapper;
    private final MdMaterialMapper materialMapper;
    /** 业务单号生成器（原子自增，避免并发撞号） */
    private final SerialNoGenerator serialNoGenerator;

    @Override
    public PageResult<PmRoutingVO> pageRouting(int pageNum, int pageSize, Long materialId, Integer status) {
        Page<PmRouting> page = this.page(new Page<>(pageNum, pageSize), Wrappers.<PmRouting>lambdaQuery()
                .eq(materialId != null, PmRouting::getMaterialId, materialId)
                .eq(status != null, PmRouting::getStatus, status)
                .orderByDesc(PmRouting::getId));
        List<PmRoutingVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<PmRoutingVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public PmRoutingVO getRoutingDetail(Long id) {
        PmRouting routing = this.getById(id);
        if (routing == null) {
            throw new BizException("工艺路线不存在");
        }
        PmRoutingVO vo = toVO(routing);
        vo.setOperations(loadOperations(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRouting(PmRoutingSaveRequest request) {
        MdMaterial material = materialMapper.selectById(request.getMaterialId());
        if (material == null) {
            throw new BizException("生产物料不存在");
        }
        validateOperations(request.getOperations());
        PmRouting routing = new PmRouting();
        routing.setRoutingNo(nextNo());
        routing.setMaterialId(request.getMaterialId());
        routing.setVersion(StringUtils.hasText(request.getVersion()) ? request.getVersion() : "V1");
        routing.setStatus(PmRouting.STATUS_ENABLED);
        routing.setEffectiveDate(request.getEffectiveDate());
        routing.setRemark(request.getRemark());
        this.save(routing);
        saveOperations(routing.getId(), request.getOperations());
        return routing.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRouting(PmRoutingSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        PmRouting exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("工艺路线不存在");
        }
        MdMaterial material = materialMapper.selectById(request.getMaterialId());
        if (material == null) {
            throw new BizException("生产物料不存在");
        }
        validateOperations(request.getOperations());
        PmRouting routing = new PmRouting();
        routing.setId(request.getId());
        routing.setMaterialId(request.getMaterialId());
        routing.setVersion(StringUtils.hasText(request.getVersion()) ? request.getVersion() : exist.getVersion());
        routing.setEffectiveDate(request.getEffectiveDate());
        routing.setRemark(request.getRemark());
        this.updateById(routing);
        // 工序整体替换，保持与前端提交的一致
        operationMapper.delete(Wrappers.<PmRoutingOperation>lambdaQuery()
                .eq(PmRoutingOperation::getRoutingId, request.getId()));
        saveOperations(request.getId(), request.getOperations());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRouting(Long id) {
        PmRouting exist = this.getById(id);
        if (exist == null) {
            throw new BizException("工艺路线不存在");
        }
        this.removeById(id);
        operationMapper.delete(Wrappers.<PmRoutingOperation>lambdaQuery()
                .eq(PmRoutingOperation::getRoutingId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addOperation(Long routingId, PmRoutingOperationRequest request) {
        assertRoutingExists(routingId);
        validateOperations(Collections.singletonList(request));
        PmRoutingOperation operation = toOperation(request);
        operation.setRoutingId(routingId);
        if (operation.getSortNo() == null || operation.getSortNo() <= 0) {
            operation.setSortNo(nextSortNo(routingId));
        }
        operationMapper.insert(operation);
        return operation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOperation(Long routingId, Long operationId, PmRoutingOperationRequest request) {
        assertRoutingExists(routingId);
        validateOperations(Collections.singletonList(request));
        PmRoutingOperation exist = operationMapper.selectById(operationId);
        if (exist == null || !exist.getRoutingId().equals(routingId)) {
            throw new BizException("工序不存在");
        }
        PmRoutingOperation operation = toOperation(request);
        operation.setId(operationId);
        operation.setRoutingId(routingId);
        operationMapper.updateById(operation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOperation(Long routingId, Long operationId) {
        assertRoutingExists(routingId);
        PmRoutingOperation exist = operationMapper.selectById(operationId);
        if (exist == null || !exist.getRoutingId().equals(routingId)) {
            throw new BizException("工序不存在");
        }
        operationMapper.deleteById(operationId);
    }

    @Override
    public List<PmWorkCenterVO> listWorkCenters() {
        return workCenterMapper.selectList(Wrappers.<PmWorkCenter>lambdaQuery()
                        .orderByAsc(PmWorkCenter::getId))
                .stream()
                .map(center -> {
                    PmWorkCenterVO vo = new PmWorkCenterVO();
                    BeanUtils.copyProperties(center, vo);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private void assertRoutingExists(Long routingId) {
        PmRouting routing = this.getById(routingId);
        if (routing == null) {
            throw new BizException("工艺路线不存在");
        }
    }

    private int nextSortNo(Long routingId) {
        List<PmRoutingOperation> operations = operationMapper.selectList(
                Wrappers.<PmRoutingOperation>lambdaQuery().eq(PmRoutingOperation::getRoutingId, routingId));
        int max = 0;
        for (PmRoutingOperation operation : operations) {
            if (operation.getSortNo() != null) {
                max = Math.max(max, operation.getSortNo());
            }
        }
        return max + 1;
    }

    /**
     * 校验工序：工序号不可重复，工作中心必须存在。
     */
    private void validateOperations(List<PmRoutingOperationRequest> operations) {
        if (operations == null || operations.isEmpty()) {
            throw new BizException("工序明细不能为空");
        }
        List<String> operationNos = operations.stream()
                .map(PmRoutingOperationRequest::getOperationNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (operationNos.size() != operations.size()) {
            throw new BizException("工序号不能为空");
        }
        if (operationNos.stream().distinct().count() != operationNos.size()) {
            throw new BizException("工序号不能重复");
        }
        List<Long> workCenterIds = operations.stream()
                .map(PmRoutingOperationRequest::getWorkCenterId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!workCenterIds.isEmpty()) {
            Long count = workCenterMapper.selectCount(Wrappers.<PmWorkCenter>lambdaQuery()
                    .in(PmWorkCenter::getId, workCenterIds));
            if (count == null || count.intValue() != workCenterIds.size()) {
                throw new BizException("存在无效的工作中心");
            }
        }
    }

    private PmRoutingOperation toOperation(PmRoutingOperationRequest request) {
        PmRoutingOperation operation = new PmRoutingOperation();
        BeanUtils.copyProperties(request, operation);
        operation.setStandardHours(nullSafe(request.getStandardHours()));
        operation.setStandardQty(nullSafe(request.getStandardQty()));
        operation.setLaborRate(nullSafe(request.getLaborRate()));
        operation.setOverheadRate(nullSafe(request.getOverheadRate()));
        return operation;
    }

    private void saveOperations(Long routingId, List<PmRoutingOperationRequest> operations) {
        int sort = 0;
        for (PmRoutingOperationRequest request : operations) {
            sort++;
            PmRoutingOperation operation = toOperation(request);
            operation.setRoutingId(routingId);
            if (operation.getSortNo() == null || operation.getSortNo() <= 0) {
                operation.setSortNo(sort);
            }
            operationMapper.insert(operation);
        }
    }

    private List<PmRoutingOperationVO> loadOperations(Long routingId) {
        List<PmRoutingOperation> operations = operationMapper.selectList(
                Wrappers.<PmRoutingOperation>lambdaQuery()
                        .eq(PmRoutingOperation::getRoutingId, routingId)
                        .orderByAsc(PmRoutingOperation::getSortNo)
                        .orderByAsc(PmRoutingOperation::getId));
        if (operations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> workCenterIds = operations.stream()
                .map(PmRoutingOperation::getWorkCenterId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, PmWorkCenter> centerMap = workCenterIds.isEmpty()
                ? Collections.emptyMap()
                : workCenterMapper.selectList(Wrappers.<PmWorkCenter>lambdaQuery()
                        .in(PmWorkCenter::getId, workCenterIds))
                .stream()
                .collect(Collectors.toMap(PmWorkCenter::getId, Function.identity()));
        return operations.stream().map(operation -> {
            PmRoutingOperationVO vo = new PmRoutingOperationVO();
            BeanUtils.copyProperties(operation, vo);
            PmWorkCenter center = centerMap.get(operation.getWorkCenterId());
            if (center != null) {
                vo.setWorkCenterCode(center.getCenterCode());
                vo.setWorkCenterName(center.getCenterName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private PmRoutingVO toVO(PmRouting routing) {
        PmRoutingVO vo = new PmRoutingVO();
        BeanUtils.copyProperties(routing, vo);
        MdMaterial material = materialMapper.selectById(routing.getMaterialId());
        if (material != null) {
            vo.setMaterialCode(material.getMaterialCode());
            vo.setMaterialName(material.getMaterialName());
        }
        List<PmRoutingOperationVO> operations = loadOperations(routing.getId());
        vo.setOperations(operations);
        BigDecimal totalHours = BigDecimal.ZERO;
        BigDecimal labor = BigDecimal.ZERO;
        BigDecimal overhead = BigDecimal.ZERO;
        for (PmRoutingOperationVO operation : operations) {
            BigDecimal hours = nullSafe(operation.getStandardHours());
            totalHours = totalHours.add(hours);
            labor = labor.add(hours.multiply(nullSafe(operation.getLaborRate())));
            overhead = overhead.add(hours.multiply(nullSafe(operation.getOverheadRate())));
        }
        vo.setTotalStandardHours(totalHours);
        vo.setTotalLaborCost(labor);
        vo.setTotalOverheadCost(overhead);
        return vo;
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String nextNo() {
        // 序号取自流水表原子自增：并发不撞号，软删除不重用旧号
        return serialNoGenerator.next("RT", LocalDate.now().format(MONTH_FORMATTER));
    }

}








