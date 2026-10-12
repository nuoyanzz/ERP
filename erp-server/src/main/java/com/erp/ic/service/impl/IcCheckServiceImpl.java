package com.erp.ic.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.wf.integration.ApprovalGate;
import com.erp.wf.integration.ICCheckApprovalListener;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.fi.domain.dto.VoucherLine;
import com.erp.fi.domain.entity.FiVoucherEntry;
import com.erp.fi.service.FiVoucherService;
import com.erp.ic.domain.dto.IcCheckEntryRequest;
import com.erp.ic.domain.dto.IcCheckSaveRequest;
import com.erp.ic.domain.entity.IcCheck;
import com.erp.ic.domain.entity.IcCheckEntry;
import com.erp.ic.domain.entity.IcStock;
import com.erp.ic.domain.vo.IcCheckDetailVO;
import com.erp.ic.domain.vo.IcCheckEntryVO;
import com.erp.ic.domain.vo.IcCheckListVO;
import com.erp.ic.mapper.IcCheckEntryMapper;
import com.erp.ic.mapper.IcCheckMapper;
import com.erp.ic.mapper.IcStockMapper;
import com.erp.ic.service.IcCheckService;
import com.erp.ic.service.IcStockService;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.domain.entity.MdWarehouse;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.md.mapper.MdWarehouseMapper;
import com.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IcCheckServiceImpl implements IcCheckService {

    private static final String NO_PREFIX = "PD-";
    private static final String VOUCHER_BIZ_TYPE = "IC_CHECK";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final IcCheckMapper checkMapper;
    private final IcCheckEntryMapper checkEntryMapper;
    private final IcStockMapper stockMapper;
    private final MdWarehouseMapper warehouseMapper;
    private final MdMaterialMapper materialMapper;
    private final IcStockService stockService;
    private final FiVoucherService voucherService;

    private final ApprovalGate approvalGate;

    @Override
    public PageResult<IcCheckListVO> pageCheck(int pageNum, int pageSize, Long warehouseId, Integer status) {
        Page<IcCheck> page = checkMapper.selectPage(new Page<>(pageNum, pageSize), Wrappers.<IcCheck>lambdaQuery()
                .eq(warehouseId != null, IcCheck::getWarehouseId, warehouseId)
                .eq(status != null, IcCheck::getStatus, status)
                .orderByDesc(IcCheck::getCheckDate)
                .orderByDesc(IcCheck::getId));
        List<IcCheckListVO> records = page.getRecords().stream().map(this::toListVO).collect(Collectors.toList());
        Page<IcCheckListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public IcCheckDetailVO getCheckDetail(Long id) {
        IcCheck check = checkMapper.selectById(id);
        if (check == null) {
            throw new BizException("盘点单不存在");
        }
        IcCheckDetailVO detail = new IcCheckDetailVO();
        BeanUtils.copyProperties(check, detail);
        detail.setEntries(loadEntries(id));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCheck(IcCheckSaveRequest request) {
        MdWarehouse warehouse = warehouseMapper.selectById(request.getWarehouseId());
        if (warehouse == null) {
            throw new BizException("仓库不存在");
        }
        validateEntries(request.getEntries());
        IcCheck check = new IcCheck();
        check.setCheckNo(nextNo(request.getCheckDate()));
        check.setWarehouseId(request.getWarehouseId());
        check.setCheckDate(request.getCheckDate());
        check.setStatus(IcCheck.STATUS_DRAFT);
        check.setGainQty(BigDecimal.ZERO);
        check.setLossQty(BigDecimal.ZERO);
        check.setRemark(request.getRemark());
        checkMapper.insert(check);
        saveEntries(check.getId(), request.getEntries());
        return check.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCheck(IcCheckSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        IcCheck exist = checkMapper.selectById(request.getId());
        if (exist == null) {
            throw new BizException("盘点单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != IcCheck.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的盘点单才能修改");
        }
        validateEntries(request.getEntries());
        IcCheck check = new IcCheck();
        check.setId(request.getId());
        check.setWarehouseId(request.getWarehouseId());
        check.setCheckDate(request.getCheckDate());
        check.setRemark(request.getRemark());
        checkMapper.updateById(check);
        checkEntryMapper.delete(Wrappers.<IcCheckEntry>lambdaQuery().eq(IcCheckEntry::getCheckId, request.getId()));
        saveEntries(request.getId(), request.getEntries());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCheck(Long id) {
        IcCheck exist = checkMapper.selectById(id);
        if (exist == null) {
            throw new BizException("盘点单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != IcCheck.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的盘点单才能删除");
        }
        checkMapper.deleteById(id);
        checkEntryMapper.delete(Wrappers.<IcCheckEntry>lambdaQuery().eq(IcCheckEntry::getCheckId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditCheck(Long id) {
        IcCheck check = checkMapper.selectById(id);
        if (check == null) {
            throw new BizException("盘点单不存在");
        }
        if (check.getStatus() == null || check.getStatus() != IcCheck.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的盘点单才能审核");
        }

        // 若该业务类型配置了审批流，审核动作改由审批通过后回调触发
        if (approvalGate.tryStart(ICCheckApprovalListener.BIZ_TYPE, id,
                "库存盘点" + check.getCheckNo(), null, null)) {
            return;
        }
        List<IcCheckEntry> entries = checkEntryMapper.selectList(Wrappers.<IcCheckEntry>lambdaQuery()
                .eq(IcCheckEntry::getCheckId, id)
                .orderByAsc(IcCheckEntry::getId));
        if (entries.isEmpty()) {
            throw new BizException("盘点单明细为空");
        }
        Map<Long, MdMaterial> materialMap = loadMaterials(entries);
        BigDecimal totalGain = BigDecimal.ZERO;
        BigDecimal totalLoss = BigDecimal.ZERO;
        BigDecimal totalGainAmount = BigDecimal.ZERO;
        BigDecimal totalLossAmount = BigDecimal.ZERO;
        for (IcCheckEntry entry : entries) {
            MdMaterial material = materialMap.get(entry.getMaterialId());
            String batchNo = normalizeBatchNo(entry.getBatchNo());
            if (material != null && material.getTrackBatch() != null && material.getTrackBatch() == 1 && !StringUtils.hasText(batchNo)) {
                throw new BizException("批次管理物料必须填写批次号：" + material.getMaterialCode());
            }
            IcStock stock = stockMapper.selectForUpdate(entry.getMaterialId(), check.getWarehouseId(), batchNo);
            BigDecimal bookQty = stock == null ? BigDecimal.ZERO : stock.getQty();
            BigDecimal variance = entry.getCountQty().subtract(bookQty);
            BigDecimal gainQty = BigDecimal.ZERO;
            BigDecimal lossQty = BigDecimal.ZERO;
            // 盘盈盘亏的金额口径：数量 × 单位成本
            BigDecimal unitCost = resolveUnitCost(stock, material);
            BigDecimal gainAmount = BigDecimal.ZERO;
            BigDecimal lossAmount = BigDecimal.ZERO;
            if (variance.compareTo(BigDecimal.ZERO) > 0) {
                gainQty = variance;
                gainAmount = unitCost.multiply(gainQty).setScale(2, RoundingMode.HALF_UP);
                stockService.increaseStock(entry.getMaterialId(), check.getWarehouseId(), batchNo,
                        gainQty, "IC_CHECK_GAIN", check.getId(), check.getCheckNo(), check.getCheckDate(), "盘点盘盈");
                totalGain = totalGain.add(gainQty);
                totalGainAmount = totalGainAmount.add(gainAmount);
            } else if (variance.compareTo(BigDecimal.ZERO) < 0) {
                lossQty = variance.abs();
                lossAmount = unitCost.multiply(lossQty).setScale(2, RoundingMode.HALF_UP);
                stockService.decreaseStock(entry.getMaterialId(), check.getWarehouseId(), batchNo,
                        lossQty, "IC_CHECK_LOSS", check.getId(), check.getCheckNo(), check.getCheckDate(), "盘点盘亏");
                totalLoss = totalLoss.add(lossQty);
                totalLossAmount = totalLossAmount.add(lossAmount);
            }
            IcCheckEntry update = new IcCheckEntry();
            update.setId(entry.getId());
            update.setBookQty(bookQty);
            update.setGainQty(gainQty);
            update.setLossQty(lossQty);
            update.setGainAmount(gainAmount);
            update.setLossAmount(lossAmount);
            checkEntryMapper.updateById(update);
        }

        Long voucherId = null;
        // 盘盈与盘亏分别生成凭证，不做净额相抵——两者的账务含义不同
        if (totalGainAmount.compareTo(BigDecimal.ZERO) != 0 || totalLossAmount.compareTo(BigDecimal.ZERO) != 0) {
            List<VoucherLine> lines = new ArrayList<>();
            if (totalGainAmount.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(VoucherLine.builder()
                        .direction(FiVoucherEntry.DIRECTION_DEBIT)
                        .ruleKey("STOCK")
                        .amount(totalGainAmount)
                        .summary("盘点盘盈")
                        .build());
                lines.add(VoucherLine.builder()
                        .direction(FiVoucherEntry.DIRECTION_CREDIT)
                        .ruleKey("SUSPENSE")
                        .amount(totalGainAmount)
                        .summary("盘点盘盈")
                        .build());
            }
            if (totalLossAmount.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(VoucherLine.builder()
                        .direction(FiVoucherEntry.DIRECTION_DEBIT)
                        .ruleKey("SUSPENSE")
                        .amount(totalLossAmount)
                        .summary("盘点盘亏")
                        .build());
                lines.add(VoucherLine.builder()
                        .direction(FiVoucherEntry.DIRECTION_CREDIT)
                        .ruleKey("STOCK")
                        .amount(totalLossAmount)
                        .summary("盘点盘亏")
                        .build());
            }
            voucherId = voucherService.generateBizVoucher(VOUCHER_BIZ_TYPE, check.getCheckDate(),
                    "库存盘点 " + check.getCheckNo(), lines);
        }
        IcCheck update = new IcCheck();
        update.setId(id);
        update.setStatus(IcCheck.STATUS_AUDITED);
        update.setGainQty(totalGain);
        update.setLossQty(totalLoss);
        update.setGainAmount(totalGainAmount);
        update.setLossAmount(totalLossAmount);
        update.setVoucherId(voucherId);
        update.setAuditBy(SecurityUtils.getCurrentUserId());
        update.setAuditTime(LocalDateTime.now());
        checkMapper.updateById(update);
    }

    /**
     * 解析盘点的单位成本。
     * <p>
     * 优先用库存行的加权平均价（ic_stock.avg_price），退回物料主数据的成本价。
     * 盘盈没有库存行，只能用物料成本价。
     */
    private BigDecimal resolveUnitCost(IcStock stock, MdMaterial material) {
        if (stock != null && stock.getAvgPrice() != null
                && stock.getAvgPrice().compareTo(BigDecimal.ZERO) > 0) {
            return stock.getAvgPrice();
        }
        if (stock != null && stock.getAmount() != null && stock.getQty() != null
                && stock.getQty().compareTo(BigDecimal.ZERO) > 0) {
            return stock.getAmount().divide(stock.getQty(), 4, RoundingMode.HALF_UP);
        }
        if (material != null && material.getCostPrice() != null) {
            return material.getCostPrice();
        }
        return BigDecimal.ZERO;
    }

    private void validateEntries(List<IcCheckEntryRequest> entryRequests) {
        if (entryRequests == null || entryRequests.isEmpty()) {
            throw new BizException("盘点明细不能为空");
        }
        List<Long> materialIds = entryRequests.stream()
                .map(IcCheckEntryRequest::getMaterialId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, MdMaterial> materialMap = materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery()
                        .in(MdMaterial::getId, materialIds))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
        for (IcCheckEntryRequest entryRequest : entryRequests) {
            MdMaterial material = materialMap.get(entryRequest.getMaterialId());
            if (material == null) {
                throw new BizException("物料不存在：" + entryRequest.getMaterialId());
            }
            if (material.getStatus() != null && material.getStatus() != 0) {
                throw new BizException("物料已停用：" + material.getMaterialCode());
            }
        }
    }

    private void saveEntries(Long checkId, List<IcCheckEntryRequest> entryRequests) {
        for (IcCheckEntryRequest entryRequest : entryRequests) {
            IcCheckEntry entry = new IcCheckEntry();
            entry.setCheckId(checkId);
            entry.setMaterialId(entryRequest.getMaterialId());
            entry.setCountQty(entryRequest.getCountQty());
            entry.setBatchNo(StringUtils.hasText(entryRequest.getBatchNo()) ? entryRequest.getBatchNo() : "");
            entry.setBookQty(BigDecimal.ZERO);
            entry.setGainQty(BigDecimal.ZERO);
            entry.setLossQty(BigDecimal.ZERO);
            entry.setRemark(entryRequest.getRemark());
            checkEntryMapper.insert(entry);
        }
    }

    private Map<Long, MdMaterial> loadMaterials(List<IcCheckEntry> entries) {
        List<Long> materialIds = entries.stream().map(IcCheckEntry::getMaterialId).distinct().collect(Collectors.toList());
        return materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery().in(MdMaterial::getId, materialIds))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
    }

    private List<IcCheckEntryVO> loadEntries(Long checkId) {
        List<IcCheckEntry> entries = checkEntryMapper.selectList(Wrappers.<IcCheckEntry>lambdaQuery()
                .eq(IcCheckEntry::getCheckId, checkId)
                .orderByAsc(IcCheckEntry::getId));
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, MdMaterial> materialMap = loadMaterials(entries);
        return entries.stream().map(entry -> {
            IcCheckEntryVO vo = new IcCheckEntryVO();
            BeanUtils.copyProperties(entry, vo);
            MdMaterial material = materialMap.get(entry.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getMaterialCode());
                vo.setMaterialName(material.getMaterialName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private String nextNo(LocalDate checkDate) {
        String prefix = NO_PREFIX + checkDate.format(MONTH_FORMATTER) + "-";
        String lastNo = checkMapper.selectLastCheckNo(prefix);
        int next = 1;
        if (StringUtils.hasText(lastNo)) {
            int idx = lastNo.lastIndexOf('-');
            if (idx >= 0 && idx < lastNo.length() - 1) {
                try {
                    next = Integer.parseInt(lastNo.substring(idx + 1)) + 1;
                } catch (NumberFormatException ignored) {
                    next = 1;
                }
            }
        }
        return prefix + String.format("%04d", next);
    }

    private String normalizeBatchNo(String batchNo) {
        return StringUtils.hasText(batchNo) ? batchNo : "";
    }

    private IcCheckListVO toListVO(IcCheck check) {
        IcCheckListVO vo = new IcCheckListVO();
        BeanUtils.copyProperties(check, vo);
        MdWarehouse warehouse = warehouseMapper.selectById(check.getWarehouseId());
        if (warehouse != null) {
            vo.setWarehouseName(warehouse.getWarehouseName());
        }
        return vo;
    }
}








