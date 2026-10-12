package com.erp.pm.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.pm.domain.entity.PmManufacturingCost;
import com.erp.pm.domain.entity.PmMo;
import com.erp.pm.domain.model.PmMoCostSummary;
import com.erp.pm.domain.vo.PmManufacturingCostVO;
import com.erp.pm.domain.vo.PmMoCostVO;
import com.erp.pm.mapper.PmManufacturingCostMapper;
import com.erp.pm.mapper.PmMoMapper;
import com.erp.pm.service.PmCostService;
import com.erp.pm.service.PmManufacturingCostService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PmManufacturingCostServiceImpl implements PmManufacturingCostService {

    private static final int SCALE = 4;

    private final PmMoMapper moMapper;
    private final PmManufacturingCostMapper costMapper;
    private final MdMaterialMapper materialMapper;
    private final PmCostService costService;

    @Override
    public PageResult<PmMoCostVO> pageMoCost(int pageNum, int pageSize, Long moId, Long materialId) {
        Page<PmMo> page = moMapper.selectPage(new Page<>(pageNum, pageSize), Wrappers.<PmMo>lambdaQuery()
                .eq(moId != null, PmMo::getId, moId)
                .eq(materialId != null, PmMo::getMaterialId, materialId)
                .orderByDesc(PmMo::getId));
        List<PmMoCostVO> records = page.getRecords().stream()
                .map(this::toMoCostVO)
                .collect(Collectors.toList());
        Page<PmMoCostVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public PmMoCostVO getMoCostDetail(Long moId) {
        PmMo mo = moMapper.selectById(moId);
        if (mo == null) {
            throw new BizException("生产订单不存在");
        }
        PmMoCostVO vo = toMoCostVO(mo);
        vo.setDetails(loadCostDetails(moId));
        return vo;
    }

    @Override
    public PageResult<PmManufacturingCostVO> pageCostDetail(int pageNum, int pageSize, Long moId, Integer costType) {
        Page<PmManufacturingCost> page = costMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<PmManufacturingCost>lambdaQuery()
                        .eq(moId != null, PmManufacturingCost::getMoId, moId)
                        .eq(costType != null, PmManufacturingCost::getCostType, costType)
                        .orderByDesc(PmManufacturingCost::getId));
        Map<Long, PmMo> moMap = loadMoMap(page.getRecords());
        Map<Long, MdMaterial> materialMap = loadMaterialMap(moMap);
        List<PmManufacturingCostVO> records = page.getRecords().stream()
                .map(cost -> toCostVO(cost, moMap, materialMap))
                .collect(Collectors.toList());
        Page<PmManufacturingCostVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public List<PmMoCostVO> listMoCost(Long materialId) {
        List<PmMo> mos = moMapper.selectList(Wrappers.<PmMo>lambdaQuery()
                .eq(materialId != null, PmMo::getMaterialId, materialId)
                .orderByDesc(PmMo::getId));
        return mos.stream().map(this::toMoCostVO).collect(Collectors.toList());
    }

    private PmMoCostVO toMoCostVO(PmMo mo) {
        PmMoCostSummary summary = costService.summarizeMoCost(mo.getId());
        PmMoCostVO vo = new PmMoCostVO();
        vo.setMoId(mo.getId());
        vo.setMoNo(mo.getMoNo());
        vo.setMaterialId(mo.getMaterialId());
        MdMaterial material = materialMapper.selectById(mo.getMaterialId());
        if (material != null) {
            vo.setMaterialCode(material.getMaterialCode());
            vo.setMaterialName(material.getMaterialName());
        }
        vo.setOrderQty(summary.getOrderQty());
        vo.setProducedQty(summary.getProducedQty());
        vo.setMaterialCost(summary.getMaterialCost());
        vo.setLaborCost(summary.getLaborCost());
        vo.setOverheadCost(summary.getOverheadCost());
        vo.setOtherCost(summary.getOtherCost());
        vo.setTotalCost(summary.getTotalCost());
        vo.setUnitCost(summary.getUnitCost());
        return vo;
    }

    private List<PmManufacturingCostVO> loadCostDetails(Long moId) {
        List<PmManufacturingCost> costs = costMapper.selectList(Wrappers.<PmManufacturingCost>lambdaQuery()
                .eq(PmManufacturingCost::getMoId, moId)
                .orderByDesc(PmManufacturingCost::getId));
        Map<Long, PmMo> moMap = loadMoMap(costs);
        Map<Long, MdMaterial> materialMap = loadMaterialMap(moMap);
        return costs.stream().map(cost -> toCostVO(cost, moMap, materialMap)).collect(Collectors.toList());
    }

    private PmManufacturingCostVO toCostVO(PmManufacturingCost cost, Map<Long, PmMo> moMap,
                                          Map<Long, MdMaterial> materialMap) {
        PmManufacturingCostVO vo = new PmManufacturingCostVO();
        BeanUtils.copyProperties(cost, vo);
        vo.setCostTypeName(costTypeName(cost.getCostType()));
        PmMo mo = moMap.get(cost.getMoId());
        if (mo != null) {
            vo.setMoNo(mo.getMoNo());
            MdMaterial material = materialMap.get(mo.getMaterialId());
            if (material != null) {
                vo.setMaterialName(material.getMaterialName());
            }
        }
        return vo;
    }

    private String costTypeName(Integer costType) {
        if (costType == null) {
            return null;
        }
        return switch (costType) {
            case PmManufacturingCost.TYPE_LABOR -> "人工成本";
            case PmManufacturingCost.TYPE_OVERHEAD -> "制造费用";
            case PmManufacturingCost.TYPE_OTHER -> "其他成本";
            default -> "未知";
        };
    }

    private Map<Long, PmMo> loadMoMap(List<PmManufacturingCost> costs) {
        List<Long> moIds = costs.stream()
                .map(PmManufacturingCost::getMoId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (moIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return moMapper.selectList(Wrappers.<PmMo>lambdaQuery().in(PmMo::getId, moIds))
                .stream()
                .collect(Collectors.toMap(PmMo::getId, Function.identity()));
    }

    private Map<Long, MdMaterial> loadMaterialMap(Map<Long, PmMo> moMap) {
        List<Long> materialIds = moMap.values().stream()
                .map(PmMo::getMaterialId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (materialIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery().in(MdMaterial::getId, materialIds))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
    }
}








