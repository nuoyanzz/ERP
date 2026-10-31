package com.erp.pm.service.impl;

import com.erp.common.util.SerialNoGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.security.datascope.DataScopeHelper;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.fi.domain.dto.VoucherLine;
import com.erp.fi.service.FiVoucherService;
import com.erp.ic.mapper.IcStockMapper;
import com.erp.ic.service.IcStockService;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.domain.entity.MdWarehouse;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.md.mapper.MdWarehouseMapper;
import com.erp.pm.domain.dto.PmFinishSaveRequest;
import com.erp.pm.domain.entity.PmFinish;
import com.erp.pm.domain.entity.PmMo;
import com.erp.pm.domain.entity.PmMoEntry;
import com.erp.pm.domain.entity.PmPickEntry;
import com.erp.pm.domain.model.PmFinishCostAllocation;
import com.erp.pm.domain.vo.PmFinishVO;
import com.erp.pm.mapper.PmFinishMapper;
import com.erp.pm.mapper.PmMoEntryMapper;
import com.erp.pm.mapper.PmMoMapper;
import com.erp.pm.mapper.PmPickEntryMapper;
import com.erp.pm.service.PmCostService;
import com.erp.pm.service.PmFinishService;
import com.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PmFinishServiceImpl extends ServiceImpl<PmFinishMapper, PmFinish> implements PmFinishService {

    private static final String NO_PREFIX = "FG-";
    private static final String TRAN_TYPE = "PM_FINISH";
    private static final String VOUCHER_BIZ_TYPE = "PM_FINISH";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final int SCALE = 4;

    private final PmMoMapper moMapper;

    private final DataScopeHelper dataScopeHelper;
    private final PmMoEntryMapper moEntryMapper;
    private final PmPickEntryMapper pickEntryMapper;
    private final MdWarehouseMapper warehouseMapper;
    private final MdMaterialMapper materialMapper;
    private final IcStockMapper stockMapper;
    private final IcStockService stockService;
    private final FiVoucherService voucherService;
    private final PmCostService costService;
    /** 业务单号生成器（原子自增，避免并发撞号） */
    private final SerialNoGenerator serialNoGenerator;

    @Override
    public PageResult<PmFinishVO> pageFinish(int pageNum, int pageSize, Long moId, Integer status) {
        Page<PmFinish> page = this.page(new Page<>(pageNum, pageSize), Wrappers.<PmFinish>lambdaQuery()
                .eq(moId != null, PmFinish::getMoId, moId)
                .eq(status != null, PmFinish::getStatus, status)
                .orderByDesc(PmFinish::getId));
        List<PmFinishVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<PmFinishVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public PmFinishVO getFinishDetail(Long id) {
        PmFinish finish = this.getById(id);
        if (finish == null) {
            throw new BizException("完工入库单不存在");
        }
        return toVO(finish);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFinish(PmFinishSaveRequest request) {
        PmMo mo = moMapper.selectById(request.getMoId());
        if (mo == null) {
            throw new BizException("生产订单不存在");
        }
        if (mo.getStatus() == null || (mo.getStatus() != PmMo.STATUS_AUDITED && mo.getStatus() != PmMo.STATUS_PROCESSING)) {
            throw new BizException("生产订单状态不允许完工入库");
        }
        MdWarehouse warehouse = warehouseMapper.selectById(request.getWarehouseId());
        if (warehouse == null) {
            throw new BizException("成品仓库不存在");
        }
        if (request.getQty().compareTo(mo.getQty()) > 0) {
            throw new BizException("完工数量不能超过订单数量");
        }
        PmFinish finish = new PmFinish();
        finish.setFinishNo(nextNo(request.getFinishDate()));
        finish.setMoId(request.getMoId());
        finish.setWarehouseId(request.getWarehouseId());
        finish.setFinishDate(request.getFinishDate());
        finish.setQty(request.getQty());
        finish.setCostAmount(BigDecimal.ZERO);
        finish.setMaterialCost(BigDecimal.ZERO);
        finish.setLaborCost(BigDecimal.ZERO);
        finish.setOverheadCost(BigDecimal.ZERO);
        finish.setUnitCost(BigDecimal.ZERO);
        // 归属部门供数据权限过滤：优先继承来源单据，其次取当前用户部门
        finish.setDeptId(dataScopeHelper.resolveDeptId(null, null));
        finish.setStatus(PmFinish.STATUS_DRAFT);
        finish.setRemark(request.getRemark());
        this.save(finish);
        return finish.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFinish(PmFinishSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        PmFinish exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("完工入库单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != PmFinish.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的完工入库单才能修改");
        }
        PmMo mo = moMapper.selectById(exist.getMoId());
        if (mo == null) {
            throw new BizException("生产订单不存在");
        }
        if (request.getQty().compareTo(mo.getQty()) > 0) {
            throw new BizException("完工数量不能超过订单数量");
        }
        PmFinish finish = new PmFinish();
        finish.setId(request.getId());
        finish.setMoId(request.getMoId());
        finish.setWarehouseId(request.getWarehouseId());
        finish.setFinishDate(request.getFinishDate());
        finish.setQty(request.getQty());
        finish.setRemark(request.getRemark());
        this.updateById(finish);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFinish(Long id) {
        PmFinish exist = this.getById(id);
        if (exist == null) {
            throw new BizException("完工入库单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != PmFinish.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的完工入库单才能删除");
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditFinish(Long id) {
        PmFinish finish = this.getById(id);
        if (finish == null) {
            throw new BizException("完工入库单不存在");
        }
        if (finish.getStatus() == null || finish.getStatus() != PmFinish.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的完工入库单才能审核");
        }
        PmMo mo = moMapper.selectById(finish.getMoId());
        if (mo == null) {
            throw new BizException("生产订单不存在");
        }
        if (sumPickedCost(finish.getMoId()).compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("该订单尚未领料，不能完工入库");
        }

        // 关键修复：本次完工成本按「累计已完工产量比例」结转，
        // 而不是把累计领料成本整体摊到本次产量上。
        // 否则分批完工时，第一次完工就已经把全部领料成本结转完了，第二次完工又会再结转一遍。
        PmFinishCostAllocation allocation = costService.calcFinishCostAllocation(
                finish.getMoId(), finish.getQty(), mo.getQty());
        BigDecimal materialCost = allocation.getMaterialCost();
        BigDecimal laborCost = allocation.getLaborCost();
        BigDecimal overheadCost = allocation.getOverheadCost();
        BigDecimal totalCost = allocation.getTotalCost();
        BigDecimal unitCost = allocation.getUnitCost();

        // 关键修复：写回成本价必须用加权平均，不能用单次工单成本直接覆盖全局成本价，
        // 否则一张工单的异常成本会把该物料的全局成本价冲掉，影响后续所有单据的成本计算。
        // 必须在入库之前取库存量，否则本次入库数量会被重复计入分母。
        BigDecimal newCostPrice = calcWeightedCostPrice(mo.getMaterialId(), finish.getQty(), totalCost);

        stockService.increaseStock(mo.getMaterialId(), finish.getWarehouseId(), "",
                finish.getQty(), TRAN_TYPE, finish.getId(), finish.getFinishNo(), finish.getFinishDate(),
                "生产完工入库");

        if (newCostPrice != null) {
            MdMaterial update = new MdMaterial();
            update.setId(mo.getMaterialId());
            update.setCostPrice(newCostPrice);
            materialMapper.updateById(update);
        }

        // 凭证：材料成本与人工/制造费用分两笔，便于成本分析；两笔都保证借贷平衡。
        List<VoucherLine> materialLines = new ArrayList<>();
        materialLines.add(VoucherLine.builder()
                .direction(com.erp.fi.domain.entity.FiVoucherEntry.DIRECTION_DEBIT)
                .ruleKey("STOCK")
                .amount(materialCost)
                .summary("完工入库-材料成本 " + finish.getFinishNo())
                .build());
        materialLines.add(VoucherLine.builder()
                .direction(com.erp.fi.domain.entity.FiVoucherEntry.DIRECTION_CREDIT)
                .ruleKey("COST")
                .amount(materialCost)
                .summary("结转材料生产成本 " + finish.getFinishNo())
                .build());
        Long materialVoucherId = voucherService.generateBizVoucher(VOUCHER_BIZ_TYPE, finish.getFinishDate(),
                "完工入库(材料) " + finish.getFinishNo(), materialLines);

        BigDecimal conversionCost = laborCost.add(overheadCost);
        Long conversionVoucherId = null;
        if (conversionCost.compareTo(BigDecimal.ZERO) > 0) {
            List<VoucherLine> conversionLines = new ArrayList<>();
            conversionLines.add(VoucherLine.builder()
                    .direction(com.erp.fi.domain.entity.FiVoucherEntry.DIRECTION_DEBIT)
                    .ruleKey("STOCK")
                    .amount(conversionCost)
                    .summary("完工入库-人工及制造费用 " + finish.getFinishNo())
                    .build());
            conversionLines.add(VoucherLine.builder()
                    .direction(com.erp.fi.domain.entity.FiVoucherEntry.DIRECTION_CREDIT)
                    .ruleKey("COST")
                    .amount(conversionCost)
                    .summary("结转人工及制造费用 " + finish.getFinishNo())
                    .build());
            conversionVoucherId = voucherService.generateBizVoucher(VOUCHER_BIZ_TYPE, finish.getFinishDate(),
                    "完工入库(人工制造费用) " + finish.getFinishNo(), conversionLines);
        }

        PmFinish update = new PmFinish();
        update.setId(id);
        update.setStatus(PmFinish.STATUS_AUDITED);
        update.setCostAmount(totalCost);
        update.setMaterialCost(materialCost);
        update.setLaborCost(laborCost);
        update.setOverheadCost(overheadCost);
        update.setUnitCost(unitCost);
        update.setVoucherId(materialVoucherId);
        update.setCostVoucherId(conversionVoucherId);
        update.setAuditBy(SecurityUtils.getCurrentUserId());
        update.setAuditTime(LocalDateTime.now());
        this.updateById(update);

        moMapper.addProducedQty(mo.getId(), finish.getQty());
        PmMo produced = moMapper.selectById(mo.getId());
        BigDecimal newProduced = produced.getProducedQty() == null ? BigDecimal.ZERO : produced.getProducedQty();
        // 分批完工时只有累计完工量达到订单数量，工单才算完工
        if (newProduced.compareTo(mo.getQty()) >= 0) {
            PmMo moUpdate = new PmMo();
            moUpdate.setId(mo.getId());
            moUpdate.setStatus(PmMo.STATUS_FINISHED);
            moMapper.updateById(moUpdate);
        }
        costService.refreshMoCost(produced);
    }

    /**
     * 计算加权平均后的物料成本价。
     * <p>
     * 新成本价 = (原库存金额 + 本次入库金额) ÷ (原库存数量 + 本次入库数量)
     * <p>
     * 不能直接用本次工单单位成本覆盖全局成本价：单个工单的领料成本可能与标准成本不一致，
     * 直接覆盖会让该物料后续所有单据的成本失真。加权平均能保持整体成本口径稳定。
     *
     * @return 新成本价；物料不存在或数量非法时返回 null 表示不更新
     */
    private BigDecimal calcWeightedCostPrice(Long materialId, BigDecimal finishQty, BigDecimal totalCost) {
        MdMaterial material = materialMapper.selectById(materialId);
        if (material == null) {
            return null;
        }
        BigDecimal oldCostPrice = material.getCostPrice() == null ? BigDecimal.ZERO : material.getCostPrice();
        BigDecimal stockQty = sumStockQty(materialId);
        BigDecimal newQty = stockQty.add(finishQty);
        if (newQty.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return stockQty.multiply(oldCostPrice).add(totalCost)
                .divide(newQty, SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 物料当前库存数量（用于加权平均）。
     */
    private BigDecimal sumStockQty(Long materialId) {
        List<Map<String, Object>> rows = stockMapper.sumQtyByMaterialIds(List.of(materialId));
        if (rows == null || rows.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Object value = rows.get(0).get("totalQty");
        return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
    }

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

    private List<Long> loadMoEntryIds(Long moId) {
        List<PmMoEntry> entries = moEntryMapper.selectList(Wrappers.<PmMoEntry>lambdaQuery()
                .eq(PmMoEntry::getMoId, moId));
        return entries.stream().map(PmMoEntry::getId).collect(Collectors.toList());
    }

    private PmFinishVO toVO(PmFinish finish) {
        PmFinishVO vo = new PmFinishVO();
        BeanUtils.copyProperties(finish, vo);
        PmMo mo = moMapper.selectById(finish.getMoId());
        if (mo != null) {
            vo.setMoNo(mo.getMoNo());
        }
        MdWarehouse warehouse = warehouseMapper.selectById(finish.getWarehouseId());
        if (warehouse != null) {
            vo.setWarehouseName(warehouse.getWarehouseName());
        }
        return vo;
    }

    private String nextNo(LocalDate date) {
        // 序号取自流水表原子自增：并发不撞号，软删除不重用旧号
        return serialNoGenerator.next("FG", date.format(MONTH_FORMATTER));
    }

}








