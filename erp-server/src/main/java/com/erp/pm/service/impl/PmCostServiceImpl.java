package com.erp.pm.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.exception.BizException;
import com.erp.pm.domain.entity.PmFinish;
import com.erp.pm.domain.entity.PmManufacturingCost;
import com.erp.pm.domain.entity.PmMo;
import com.erp.pm.domain.entity.PmMoEntry;
import com.erp.pm.domain.entity.PmPickEntry;
import com.erp.pm.domain.entity.PmReportWork;
import com.erp.pm.domain.model.PmFinishCostAllocation;
import com.erp.pm.domain.model.PmMoCostSummary;
import com.erp.pm.mapper.PmFinishMapper;
import com.erp.pm.mapper.PmManufacturingCostMapper;
import com.erp.pm.mapper.PmMoEntryMapper;
import com.erp.pm.mapper.PmMoMapper;
import com.erp.pm.mapper.PmPickEntryMapper;
import com.erp.pm.service.PmCostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PmCostServiceImpl implements PmCostService {

    private static final int SCALE = 4;

    private final PmMoMapper moMapper;
    private final PmMoEntryMapper moEntryMapper;
    private final PmPickEntryMapper pickEntryMapper;
    private final PmFinishMapper finishMapper;
    private final PmManufacturingCostMapper costMapper;

    @Override
    public BigDecimal calcFinishMaterialCost(Long moId, BigDecimal finishQty, BigDecimal orderQty) {
        BigDecimal pickedCost = sumPickedCost(moId);
        if (pickedCost.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal cumulativeProduced = sumAuditedFinishQty(moId).add(finishQty);
        if (cumulativeProduced.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal denominator = orderQty == null || orderQty.compareTo(BigDecimal.ZERO) <= 0
                ? cumulativeProduced : orderQty;
        // 累计应结转总额 = 累计领料成本 × (本次完工后累计完工量 ÷ 订单数量)
        // 超产时比例封顶为 1，避免结转超过实际领料成本
        BigDecimal ratio = cumulativeProduced.divide(denominator, 10, RoundingMode.HALF_UP);
        if (ratio.compareTo(BigDecimal.ONE) > 0) {
            ratio = BigDecimal.ONE;
        }
        BigDecimal cumulativeTarget = pickedCost.multiply(ratio).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal current = cumulativeTarget.subtract(sumCarriedMaterialCost(moId));
        if (current.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return current.setScale(SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public PmFinishCostAllocation calcFinishCostAllocation(Long moId, BigDecimal finishQty, BigDecimal orderQty) {
        PmFinishCostAllocation allocation = new PmFinishCostAllocation();
        allocation.setMaterialCost(calcFinishMaterialCost(moId, finishQty, orderQty));

        PmMoCostSummary summary = summarizeMoCost(moId);
        BigDecimal denominator = orderQty == null || orderQty.compareTo(BigDecimal.ZERO) <= 0
                ? finishQty : orderQty;
        BigDecimal cumulativeProduced = sumAuditedFinishQty(moId).add(finishQty);
        BigDecimal ratio = cumulativeProduced.divide(denominator, 10, RoundingMode.HALF_UP);
        if (ratio.compareTo(BigDecimal.ONE) > 0) {
            ratio = BigDecimal.ONE;
        }
        // 人工与制造费用按与材料相同的累计完工比例分配，保证口径一致
        BigDecimal laborTarget = summary.getLaborCost().multiply(ratio).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal overheadTarget = summary.getOverheadCost().multiply(ratio).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal laborBefore = sumCarriedLaborCost(moId);
        BigDecimal overheadBefore = sumCarriedOverheadCost(moId);
        BigDecimal labor = laborTarget.subtract(laborBefore);
        BigDecimal overhead = overheadTarget.subtract(overheadBefore);
        allocation.setLaborCost(labor.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : labor);
        allocation.setOverheadCost(overhead.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : overhead);

        BigDecimal total = allocation.getMaterialCost()
                .add(allocation.getLaborCost())
                .add(allocation.getOverheadCost());
        allocation.setTotalCost(total);
        if (finishQty != null && finishQty.compareTo(BigDecimal.ZERO) > 0) {
            allocation.setUnitCost(total.divide(finishQty, SCALE, RoundingMode.HALF_UP));
        }
        return allocation;
    }

    @Override
    public PmMoCostSummary summarizeMoCost(Long moId) {
        PmMo mo = moMapper.selectById(moId);
        if (mo == null) {
            throw new BizException("生产订单不存在");
        }
        PmMoCostSummary summary = new PmMoCostSummary();
        summary.setMoId(moId);
        summary.setMaterialCost(sumPickedCost(moId));
        summary.setOrderQty(mo.getQty());
        summary.setProducedQty(mo.getProducedQty() == null ? BigDecimal.ZERO : mo.getProducedQty());

        BigDecimal labor = BigDecimal.ZERO;
        BigDecimal overhead = BigDecimal.ZERO;
        BigDecimal other = BigDecimal.ZERO;
        List<PmManufacturingCost> costs = costMapper.selectList(Wrappers.<PmManufacturingCost>lambdaQuery()
                .eq(PmManufacturingCost::getMoId, moId));
        for (PmManufacturingCost cost : costs) {
            labor = labor.add(nullSafe(cost.getLaborAmount()));
            overhead = overhead.add(nullSafe(cost.getOverheadAmount()));
            other = other.add(nullSafe(cost.getOtherAmount()));
        }
        summary.setLaborCost(labor);
        summary.setOverheadCost(overhead);
        summary.setOtherCost(other);
        BigDecimal total = summary.getMaterialCost().add(labor).add(overhead).add(other);
        summary.setTotalCost(total);
        if (summary.getProducedQty().compareTo(BigDecimal.ZERO) > 0) {
            summary.setUnitCost(total.divide(summary.getProducedQty(), SCALE, RoundingMode.HALF_UP));
        } else {
            summary.setUnitCost(BigDecimal.ZERO);
        }
        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectReportCost(PmReportWork report, BigDecimal laborRate, BigDecimal overheadRate) {
        BigDecimal workHours = report.getWorkHours() == null ? BigDecimal.ZERO : report.getWorkHours();
        BigDecimal laborAmount = workHours.multiply(nullSafe(laborRate)).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal overheadAmount = workHours.multiply(nullSafe(overheadRate)).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal total = laborAmount.add(overheadAmount);

        PmManufacturingCost cost = new PmManufacturingCost();
        cost.setMoId(report.getMoId());
        cost.setCostType(PmManufacturingCost.TYPE_LABOR);
        cost.setCostDate(report.getReportDate());
        cost.setLaborAmount(laborAmount);
        cost.setOverheadAmount(overheadAmount);
        cost.setOtherAmount(BigDecimal.ZERO);
        cost.setTotalAmount(total);
        cost.setSourceType("REPORT_WORK");
        cost.setSourceId(report.getId());
        cost.setRemark("生产报工归集：" + report.getReportNo());
        costMapper.insert(cost);

        PmMo mo = moMapper.selectById(report.getMoId());
        if (mo != null) {
            refreshMoCost(mo);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshMoCost(PmMo mo) {
        PmMoCostSummary summary = summarizeMoCost(mo.getId());
        PmMo update = new PmMo();
        update.setId(mo.getId());
        update.setLaborCost(summary.getLaborCost());
        update.setOverheadCost(summary.getOverheadCost());
        update.setTotalCost(summary.getTotalCost());
        moMapper.updateById(update);
    }

    @Override
    public List<PmManufacturingCost> listReportCosts(Long moId, Long sourceId) {
        return costMapper.selectList(Wrappers.<PmManufacturingCost>lambdaQuery()
                .eq(PmManufacturingCost::getMoId, moId)
                .eq(PmManufacturingCost::getSourceType, "REPORT_WORK")
                .eq(PmManufacturingCost::getSourceId, sourceId));
    }

    /**
     * 累计领料成本：工单下所有领料单明细的成本合计。
     * <p>
     * 注意这里是「累计发生额」，只能用于计算本次完工应结转多少，
     * 不能直接当作本次完工成本，否则分批完工会重复摊销。
     */
    private BigDecimal sumPickedCost(Long moId) {
        List<Long> moEntryIds = loadMoEntryIds(moId);
        if (moEntryIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<PmPickEntry> pickEntries = pickEntryMapper.selectList(Wrappers.<PmPickEntry>lambdaQuery()
                .in(PmPickEntry::getMoEntryId, moEntryIds));
        return pickEntries.stream()
                .map(PmPickEntry::getCostAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 之前已完工的累计数量（不含本次）。
     */
    private BigDecimal sumAuditedFinishQty(Long moId) {
        return loadAuditedFinishes(moId).stream()
                .map(PmFinish::getQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 之前已结转的材料成本。
     */
    private BigDecimal sumCarriedMaterialCost(Long moId) {
        return loadAuditedFinishes(moId).stream()
                .map(PmFinish::getMaterialCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 之前已结转的人工成本。
     */
    private BigDecimal sumCarriedLaborCost(Long moId) {
        return loadAuditedFinishes(moId).stream()
                .map(PmFinish::getLaborCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 之前已结转的制造费用。
     */
    private BigDecimal sumCarriedOverheadCost(Long moId) {
        return loadAuditedFinishes(moId).stream()
                .map(PmFinish::getOverheadCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<PmFinish> loadAuditedFinishes(Long moId) {
        return finishMapper.selectList(Wrappers.<PmFinish>lambdaQuery()
                .eq(PmFinish::getMoId, moId)
                .eq(PmFinish::getStatus, PmFinish.STATUS_AUDITED));
    }

    private List<Long> loadMoEntryIds(Long moId) {
        List<PmMoEntry> entries = moEntryMapper.selectList(Wrappers.<PmMoEntry>lambdaQuery()
                .eq(PmMoEntry::getMoId, moId));
        return entries.stream().map(PmMoEntry::getId).collect(Collectors.toList());
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}








