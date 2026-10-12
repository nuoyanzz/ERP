package com.erp.pm.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.wf.integration.ApprovalGate;
import com.erp.wf.integration.PMPickApprovalListener;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.fi.domain.dto.VoucherLine;
import com.erp.fi.service.FiVoucherService;
import com.erp.ic.service.IcStockService;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.domain.entity.MdWarehouse;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.md.mapper.MdWarehouseMapper;
import com.erp.pm.domain.dto.PmPickEntryRequest;
import com.erp.pm.domain.dto.PmPickSaveRequest;
import com.erp.pm.domain.entity.PmBomEntry;
import com.erp.pm.domain.entity.PmMo;
import com.erp.pm.domain.entity.PmMoEntry;
import com.erp.pm.domain.entity.PmPick;
import com.erp.pm.domain.entity.PmPickEntry;
import com.erp.pm.domain.vo.PmPickEntryVO;
import com.erp.pm.domain.vo.PmPickVO;
import com.erp.pm.mapper.PmBomEntryMapper;
import com.erp.pm.mapper.PmMoEntryMapper;
import com.erp.pm.mapper.PmMoMapper;
import com.erp.pm.mapper.PmPickEntryMapper;
import com.erp.pm.mapper.PmPickMapper;
import com.erp.pm.service.PmPickService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PmPickServiceImpl extends ServiceImpl<PmPickMapper, PmPick> implements PmPickService {

    private static final String NO_PREFIX = "PK-";
    private static final String TRAN_TYPE = "PM_PICK";
    private static final String VOUCHER_BIZ_TYPE = "PM_PICK";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final PmPickEntryMapper pickEntryMapper;
    private final PmBomEntryMapper bomEntryMapper;
    private final PmMoMapper moMapper;
    private final PmMoEntryMapper moEntryMapper;
    private final MdWarehouseMapper warehouseMapper;
    private final MdMaterialMapper materialMapper;
    private final IcStockService stockService;

    private final ApprovalGate approvalGate;
    private final FiVoucherService voucherService;

    @Override
    public PageResult<PmPickVO> pagePick(int pageNum, int pageSize, Long moId, Integer status) {
        Page<PmPick> page = this.page(new Page<>(pageNum, pageSize), Wrappers.<PmPick>lambdaQuery()
                .eq(moId != null, PmPick::getMoId, moId)
                .eq(status != null, PmPick::getStatus, status)
                .orderByDesc(PmPick::getId));
        List<PmPickVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<PmPickVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public PmPickVO getPickDetail(Long id) {
        PmPick pick = this.getById(id);
        if (pick == null) {
            throw new BizException("领料单不存在");
        }
        PmPickVO vo = toVO(pick);
        vo.setEntries(loadEntries(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPick(PmPickSaveRequest request) {
        PmMo mo = moMapper.selectById(request.getMoId());
        if (mo == null) {
            throw new BizException("生产订单不存在");
        }
        if (mo.getStatus() == null || (mo.getStatus() != PmMo.STATUS_AUDITED && mo.getStatus() != PmMo.STATUS_PROCESSING)) {
            throw new BizException("生产订单未审核，不能领料");
        }
        MdWarehouse warehouse = warehouseMapper.selectById(request.getWarehouseId());
        if (warehouse == null) {
            throw new BizException("领料仓库不存在");
        }
        Map<Long, PmMoEntry> moEntryMap = loadMoEntries(request.getMoId());
        validateEntries(request.getEntries(), moEntryMap);
        PmPick pick = new PmPick();
        pick.setPickNo(nextNo(request.getPickDate()));
        pick.setMoId(request.getMoId());
        pick.setWarehouseId(request.getWarehouseId());
        pick.setPickDate(request.getPickDate());
        pick.setStatus(PmPick.STATUS_DRAFT);
        pick.setRemark(request.getRemark());
        this.save(pick);
        saveEntries(pick.getId(), request.getEntries(), moEntryMap);
        return pick.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePick(PmPickSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        PmPick exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("领料单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != PmPick.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的领料单才能修改");
        }
        MdWarehouse warehouse = warehouseMapper.selectById(request.getWarehouseId());
        if (warehouse == null) {
            throw new BizException("领料仓库不存在");
        }
        Map<Long, PmMoEntry> moEntryMap = loadMoEntries(request.getMoId());
        validateEntries(request.getEntries(), moEntryMap);
        pickEntryMapper.delete(Wrappers.<PmPickEntry>lambdaQuery().eq(PmPickEntry::getPickId, request.getId()));
        saveEntries(request.getId(), request.getEntries(), moEntryMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePick(Long id) {
        PmPick exist = this.getById(id);
        if (exist == null) {
            throw new BizException("领料单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != PmPick.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的领料单才能删除");
        }
        this.removeById(id);
        pickEntryMapper.delete(Wrappers.<PmPickEntry>lambdaQuery().eq(PmPickEntry::getPickId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditPick(Long id) {
        PmPick pick = this.getById(id);
        if (pick == null) {
            throw new BizException("领料单不存在");
        }
        if (pick.getStatus() == null || pick.getStatus() != PmPick.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的领料单才能审核");
        }
        // 若该业务类型配置了审批流，审核动作改由审批通过后回调触发
        if (approvalGate.tryStart(PMPickApprovalListener.BIZ_TYPE, id,
                "生产领料单" + pick.getPickNo(), null, null)) {
            return;
        }
        PmMo mo = moMapper.selectById(pick.getMoId());
        if (mo == null) {
            throw new BizException("生产订单不存在");
        }
        List<PmPickEntry> entries = pickEntryMapper.selectList(Wrappers.<PmPickEntry>lambdaQuery()
                .eq(PmPickEntry::getPickId, id)
                .orderByAsc(PmPickEntry::getId));
        if (entries.isEmpty()) {
            throw new BizException("领料单明细为空");
        }
        Map<Long, PmMoEntry> moEntryMap = loadMoEntries(pick.getMoId());
        Map<Long, MdMaterial> materialMap = loadMaterials(entries);
        List<VoucherLine> voucherLines = new ArrayList<>();
        BigDecimal totalCost = BigDecimal.ZERO;
        for (PmPickEntry entry : entries) {
            PmMoEntry moEntry = moEntryMap.get(entry.getMoEntryId());
            if (moEntry == null) {
                throw new BizException("订单子项不存在：" + entry.getMoEntryId());
            }
            MdMaterial material = materialMap.get(entry.getMaterialId());
            String materialName = material == null ? String.valueOf(entry.getMaterialId()) : material.getMaterialName();
            if (material != null && material.getTrackBatch() != null && material.getTrackBatch() == 1 && !StringUtils.hasText(entry.getBatchNo())) {
                throw new BizException("批次管理物料必须填写批次号：" + material.getMaterialCode());
            }
            stockService.decreaseStock(entry.getMaterialId(), pick.getWarehouseId(), entry.getBatchNo(),
                    entry.getQty(), TRAN_TYPE, pick.getId(), pick.getPickNo(), pick.getPickDate(), "生产领料");
            moEntryMapper.addPickedQty(entry.getMoEntryId(), entry.getQty());
            voucherLines.add(VoucherLine.builder()
                    .direction(com.erp.fi.domain.entity.FiVoucherEntry.DIRECTION_DEBIT)
                    .ruleKey("COST")
                    .amount(entry.getCostAmount())
                    .summary("生产领料：" + materialName)
                    .build());
            totalCost = totalCost.add(entry.getCostAmount());
        }
        voucherLines.add(VoucherLine.builder()
                .direction(com.erp.fi.domain.entity.FiVoucherEntry.DIRECTION_CREDIT)
                .ruleKey("MATERIAL")
                .amount(totalCost)
                .summary("生产领料出库")
                .build());
        voucherService.generateBizVoucher(VOUCHER_BIZ_TYPE, pick.getPickDate(),
                "生产领料 " + pick.getPickNo(), voucherLines);
        PmPick update = new PmPick();
        update.setId(id);
        update.setStatus(PmPick.STATUS_AUDITED);
        update.setAuditBy(SecurityUtils.getCurrentUserId());
        update.setAuditTime(LocalDateTime.now());
        this.updateById(update);
        PmMo moUpdate = new PmMo();
        moUpdate.setId(mo.getId());
        moUpdate.setStatus(PmMo.STATUS_PROCESSING);
        moMapper.updateById(moUpdate);
    }

    private void validateEntries(List<PmPickEntryRequest> entryRequests, Map<Long, PmMoEntry> moEntryMap) {
        for (PmPickEntryRequest entryRequest : entryRequests) {
            PmMoEntry moEntry = moEntryMap.get(entryRequest.getMoEntryId());
            if (moEntry == null) {
                throw new BizException("订单子项不存在：" + entryRequest.getMoEntryId());
            }
            BigDecimal remaining = moEntry.getRequireQty().subtract(
                    moEntry.getPickedQty() == null ? BigDecimal.ZERO : moEntry.getPickedQty());
            if (entryRequest.getQty().compareTo(remaining) > 0) {
                throw new BizException("领料数量超过订单需求数量，物料：" + moEntry.getMaterialId());
            }
        }
    }

    private Map<Long, PmMoEntry> loadMoEntries(Long moId) {
        List<PmMoEntry> entries = moEntryMapper.selectList(Wrappers.<PmMoEntry>lambdaQuery()
                .eq(PmMoEntry::getMoId, moId));
        return entries.stream().collect(Collectors.toMap(PmMoEntry::getId, Function.identity()));
    }

    /**
     * 解析实际领用物料。
     * <p>
     * 主料缺货时允许用 BOM 中登记的替代料顶替，但必须校验：
     * <ol>
     *   <li>该订单子项对应的 BOM 明细确实配置了替代料</li>
     *   <li>传入的替代料与 BOM 登记的替代料一致</li>
     * </ol>
     * 否则会导致成本归集与 BOM 标准用量对不上。
     *
     * @return 实际领用物料 ID；未使用替代料时返回主料 ID
     */
    private Long resolveActualMaterialId(PmPickEntryRequest entryRequest, PmMoEntry moEntry,
                                         Map<Long, PmBomEntry> bomEntryMap) {
        Long actual = entryRequest.getActualMaterialId();
        if (actual == null || actual.equals(moEntry.getMaterialId())) {
            return moEntry.getMaterialId();
        }
        PmBomEntry bomEntry = bomEntryMap.get(moEntry.getBomEntryId());
        if (bomEntry == null) {
            throw new BizException("订单子项未关联 BOM 明细，不能使用替代料");
        }
        if (bomEntry.getSubstituteFlag() == null || bomEntry.getSubstituteFlag() != 1
                || bomEntry.getSubstituteMaterialId() == null) {
            throw new BizException("物料未配置替代料，不能替代领用：" + moEntry.getMaterialId());
        }
        if (!bomEntry.getSubstituteMaterialId().equals(actual)) {
            throw new BizException("替代料不匹配，只允许使用物料 " + bomEntry.getSubstituteMaterialId());
        }
        return actual;
    }

    /**
     * 载入订单子项对应的 BOM 明细（用于校验替代料）。
     */
    private Map<Long, PmBomEntry> loadBomEntriesByMoEntries(List<PmMoEntry> moEntries) {
        List<Long> bomEntryIds = moEntries.stream()
                .map(PmMoEntry::getBomEntryId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (bomEntryIds.isEmpty()) {
            return new HashMap<>();
        }
        return bomEntryMapper.selectList(Wrappers.<PmBomEntry>lambdaQuery()
                        .in(PmBomEntry::getId, bomEntryIds))
                .stream()
                .collect(Collectors.toMap(PmBomEntry::getId, Function.identity()));
    }

    private void saveEntries(Long pickId, List<PmPickEntryRequest> entryRequests, Map<Long, PmMoEntry> moEntryMap) {
        Map<Long, PmBomEntry> bomEntryMap = loadBomEntriesByMoEntries(
                new ArrayList<>(moEntryMap.values()));
        List<Long> materialIds = new ArrayList<>();
        for (PmPickEntryRequest entryRequest : entryRequests) {
            PmMoEntry moEntry = moEntryMap.get(entryRequest.getMoEntryId());
            materialIds.add(moEntry.getMaterialId());
            Long actual = resolveActualMaterialId(entryRequest, moEntry, bomEntryMap);
            materialIds.add(actual);
        }
        Map<Long, MdMaterial> materialMap = materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery()
                        .in(MdMaterial::getId, materialIds.stream().distinct().collect(Collectors.toList())))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
        for (PmPickEntryRequest entryRequest : entryRequests) {
            PmMoEntry moEntry = moEntryMap.get(entryRequest.getMoEntryId());
            Long actualMaterialId = resolveActualMaterialId(entryRequest, moEntry, bomEntryMap);
            MdMaterial material = materialMap.get(actualMaterialId);
            if (material == null) {
                throw new BizException("物料不存在：" + actualMaterialId);
            }
            boolean substituted = !actualMaterialId.equals(moEntry.getMaterialId());
            // 成本按实际领用物料的成本价计算：替代料就用替代料的成本
            BigDecimal costPrice = material.getCostPrice() == null ? BigDecimal.ZERO : material.getCostPrice();
            PmPickEntry entry = new PmPickEntry();
            entry.setPickId(pickId);
            entry.setMoEntryId(entryRequest.getMoEntryId());
            entry.setMaterialId(actualMaterialId);
            entry.setOriginalMaterialId(moEntry.getMaterialId());
            entry.setSubstituteFlag(substituted ? 1 : 0);
            entry.setSubstituteMaterialId(substituted ? actualMaterialId : null);
            entry.setQty(entryRequest.getQty());
            entry.setCostPrice(costPrice);
            entry.setCostAmount(costPrice.multiply(entryRequest.getQty()).setScale(2, RoundingMode.HALF_UP));
            entry.setBatchNo(StringUtils.hasText(entryRequest.getBatchNo()) ? entryRequest.getBatchNo() : "");
            entry.setRemark(entryRequest.getRemark());
            pickEntryMapper.insert(entry);
        }
    }

    private Map<Long, MdMaterial> loadMaterials(List<PmPickEntry> entries) {
        List<Long> materialIds = entries.stream().map(PmPickEntry::getMaterialId).distinct().collect(Collectors.toList());
        return materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery().in(MdMaterial::getId, materialIds))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
    }

    private List<PmPickEntryVO> loadEntries(Long pickId) {
        List<PmPickEntry> entries = pickEntryMapper.selectList(Wrappers.<PmPickEntry>lambdaQuery()
                .eq(PmPickEntry::getPickId, pickId)
                .orderByAsc(PmPickEntry::getId));
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, MdMaterial> materialMap = loadMaterials(entries);
        return entries.stream().map(entry -> {
            PmPickEntryVO vo = new PmPickEntryVO();
            BeanUtils.copyProperties(entry, vo);
            MdMaterial material = materialMap.get(entry.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getMaterialCode());
                vo.setMaterialName(material.getMaterialName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private PmPickVO toVO(PmPick pick) {
        PmPickVO vo = new PmPickVO();
        BeanUtils.copyProperties(pick, vo);
        PmMo mo = moMapper.selectById(pick.getMoId());
        if (mo != null) {
            vo.setMoNo(mo.getMoNo());
        }
        MdWarehouse warehouse = warehouseMapper.selectById(pick.getWarehouseId());
        if (warehouse != null) {
            vo.setWarehouseName(warehouse.getWarehouseName());
        }
        return vo;
    }

    private String nextNo(LocalDate date) {
        String prefix = NO_PREFIX + date.format(MONTH_FORMATTER) + "-";
        return prefix + nextSeq(baseMapper.selectLastPickNo(prefix));
    }

    private String nextSeq(String lastNo) {
        int next = 1;
        if (lastNo != null) {
            int idx = lastNo.lastIndexOf('-');
            if (idx >= 0 && idx < lastNo.length() - 1) {
                try {
                    next = Integer.parseInt(lastNo.substring(idx + 1)) + 1;
                } catch (NumberFormatException ignored) {
                    next = 1;
                }
            }
        }
        return String.format("%04d", next);
    }
}








