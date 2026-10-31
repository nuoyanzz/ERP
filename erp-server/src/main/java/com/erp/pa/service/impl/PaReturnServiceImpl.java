package com.erp.pa.service.impl;

import com.erp.common.util.SerialNoGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.security.datascope.DataScopeHelper;
import com.erp.ap.service.ApPayableService;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.common.util.TaxAmountCalculator;
import com.erp.fi.domain.dto.VoucherLine;
import com.erp.fi.domain.entity.FiVoucherEntry;
import com.erp.fi.service.FiVoucherService;
import com.erp.ic.service.IcStockService;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.domain.entity.MdSupplier;
import com.erp.md.domain.entity.MdWarehouse;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.md.mapper.MdSupplierMapper;
import com.erp.md.mapper.MdWarehouseMapper;
import com.erp.pa.domain.dto.PaReturnEntryRequest;
import com.erp.pa.domain.dto.PaReturnQueryRequest;
import com.erp.pa.domain.dto.PaReturnSaveRequest;
import com.erp.pa.domain.entity.PaInstock;
import com.erp.pa.domain.entity.PaInstockEntry;
import com.erp.pa.domain.entity.PaReturn;
import com.erp.pa.domain.entity.PaReturnEntry;
import com.erp.pa.domain.vo.PaReturnDetailVO;
import com.erp.pa.domain.vo.PaReturnEntryVO;
import com.erp.pa.domain.vo.PaReturnListVO;
import com.erp.pa.mapper.PaInstockEntryMapper;
import com.erp.pa.mapper.PaInstockMapper;
import com.erp.pa.mapper.PaReturnEntryMapper;
import com.erp.pa.mapper.PaReturnMapper;
import com.erp.pa.service.PaReturnService;
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
public class PaReturnServiceImpl extends ServiceImpl<PaReturnMapper, PaReturn> implements PaReturnService {

    private static final String RETURN_NO_PREFIX = "CT-";
    private static final String TRAN_TYPE = "PA_RETURN";
    private static final String VOUCHER_BIZ_TYPE = "PA_RETURN";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PaReturnEntryMapper returnEntryMapper;

    private final DataScopeHelper dataScopeHelper;
    private final PaInstockMapper instockMapper;
    private final PaInstockEntryMapper instockEntryMapper;
    private final MdSupplierMapper supplierMapper;
    private final MdWarehouseMapper warehouseMapper;
    private final MdMaterialMapper materialMapper;
    private final IcStockService stockService;
    private final FiVoucherService voucherService;
    private final ApPayableService apPayableService;
    /** 业务单号生成器（原子自增，避免并发撞号） */
    private final SerialNoGenerator serialNoGenerator;

    @Override
    public PageResult<PaReturnListVO> pageReturn(PaReturnQueryRequest request) {
        Page<PaReturn> page = this.page(request.toPage(), Wrappers.<PaReturn>lambdaQuery()
                .like(StringUtils.hasText(request.getReturnNo()), PaReturn::getReturnNo, request.getReturnNo())
                .eq(request.getSourceInstockId() != null, PaReturn::getSourceInstockId, request.getSourceInstockId())
                .eq(request.getSupplierId() != null, PaReturn::getSupplierId, request.getSupplierId())
                .eq(request.getStatus() != null, PaReturn::getStatus, request.getStatus())
                .ge(request.getStartDate() != null, PaReturn::getReturnDate, request.getStartDate())
                .le(request.getEndDate() != null, PaReturn::getReturnDate, request.getEndDate())
                .orderByDesc(PaReturn::getReturnDate)
                .orderByDesc(PaReturn::getId));
        List<PaReturnListVO> records = page.getRecords().stream().map(this::toListVO).collect(Collectors.toList());
        Page<PaReturnListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public PaReturnDetailVO getReturnDetail(Long id) {
        PaReturn paReturn = this.getById(id);
        if (paReturn == null) {
            throw new BizException("采购退货单不存在");
        }
        PaReturnDetailVO detail = new PaReturnDetailVO();
        BeanUtils.copyProperties(paReturn, detail);
        detail.setEntries(loadEntries(id));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReturn(PaReturnSaveRequest request) {
        PaInstock source = validateSource(request.getSourceInstockId(), request.getEntries());
        Map<Long, PaInstockEntry> sourceEntryMap = loadSourceEntries(source.getId());
        PaReturn paReturn = new PaReturn();
        paReturn.setReturnNo(nextReturnNo(request.getReturnDate()));
        paReturn.setSourceInstockId(source.getId());
        paReturn.setSupplierId(source.getSupplierId());
        paReturn.setWarehouseId(source.getWarehouseId());
        paReturn.setReturnDate(request.getReturnDate());
        // 归属部门供数据权限过滤：优先继承来源单据，其次取当前用户部门
        paReturn.setDeptId(dataScopeHelper.resolveDeptId(null, null));
        paReturn.setStatus(PaReturn.STATUS_DRAFT);
        paReturn.setRemark(request.getRemark());
        this.save(paReturn);
        BigDecimal totalAmount = saveEntries(paReturn.getId(), request.getEntries(), sourceEntryMap);
        paReturn.setTotalAmount(totalAmount);
        this.updateById(paReturn);
        return paReturn.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReturn(PaReturnSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        PaReturn exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("采购退货单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != PaReturn.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的退货单才能修改");
        }
        PaInstock source = validateSource(request.getSourceInstockId(), request.getEntries());
        Map<Long, PaInstockEntry> sourceEntryMap = loadSourceEntries(source.getId());
        returnEntryMapper.delete(Wrappers.<PaReturnEntry>lambdaQuery().eq(PaReturnEntry::getReturnId, request.getId()));
        BigDecimal totalAmount = saveEntries(request.getId(), request.getEntries(), sourceEntryMap);
        PaReturn update = new PaReturn();
        update.setId(request.getId());
        update.setTotalAmount(totalAmount);
        this.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReturn(Long id) {
        PaReturn exist = this.getById(id);
        if (exist == null) {
            throw new BizException("采购退货单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != PaReturn.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的退货单才能删除");
        }
        this.removeById(id);
        returnEntryMapper.delete(Wrappers.<PaReturnEntry>lambdaQuery().eq(PaReturnEntry::getReturnId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditReturn(Long id) {
        PaReturn paReturn = this.getById(id);
        if (paReturn == null) {
            throw new BizException("采购退货单不存在");
        }
        if (paReturn.getStatus() == null || paReturn.getStatus() != PaReturn.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的退货单才能审核");
        }
        PaInstock source = instockMapper.selectById(paReturn.getSourceInstockId());
        if (source == null) {
            throw new BizException("来源入库单不存在");
        }
        List<PaReturnEntry> entries = returnEntryMapper.selectList(Wrappers.<PaReturnEntry>lambdaQuery()
                .eq(PaReturnEntry::getReturnId, id)
                .orderByAsc(PaReturnEntry::getId));
        if (entries.isEmpty()) {
            throw new BizException("退货单明细为空");
        }
        Map<Long, PaInstockEntry> sourceEntryMap = loadSourceEntries(source.getId());
        Map<Long, MdMaterial> materialMap = loadMaterials(entries);
        List<VoucherLine> voucherLines = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalNetAmount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        for (PaReturnEntry entry : entries) {
            PaInstockEntry sourceEntry = sourceEntryMap.get(entry.getSourceEntryId());
            if (sourceEntry == null) {
                throw new BizException("来源入库明细不存在：" + entry.getSourceEntryId());
            }
            MdMaterial material = materialMap.get(entry.getMaterialId());
            String materialName = material == null ? String.valueOf(entry.getMaterialId()) : material.getMaterialName();
            stockService.decreaseStock(entry.getMaterialId(), source.getWarehouseId(), entry.getBatchNo(),
                    entry.getQty(), TRAN_TYPE, paReturn.getId(), paReturn.getReturnNo(), paReturn.getReturnDate(),
                    "采购退货");
            instockEntryMapper.addReturnedQty(entry.getSourceEntryId(), entry.getQty());

            // 价税分离：退货需冲回存货成本与进项税额
            BigDecimal[] taxSplit = TaxAmountCalculator.split(entry.getQty(), entry.getPrice(), entry.getTaxRate());
            BigDecimal netAmount = taxSplit[0];
            BigDecimal taxAmount = taxSplit[1];

            voucherLines.add(VoucherLine.builder()
                    .direction(FiVoucherEntry.DIRECTION_CREDIT)
                    .ruleKey("MATERIAL")
                    .amount(netAmount)
                    .summary("采购退货冲存货：" + materialName)
                    .build());
            if (taxAmount.compareTo(BigDecimal.ZERO) > 0) {
                voucherLines.add(VoucherLine.builder()
                        .direction(FiVoucherEntry.DIRECTION_CREDIT)
                        .ruleKey("TAX")
                        .amount(taxAmount)
                        .summary("采购退货冲进项税额：" + materialName)
                        .build());
            }
            totalNetAmount = totalNetAmount.add(netAmount);
            totalTax = totalTax.add(taxAmount);
            totalAmount = totalAmount.add(entry.getAmount());
        }
        voucherLines.add(VoucherLine.builder()
                .direction(FiVoucherEntry.DIRECTION_DEBIT)
                .ruleKey("PAYABLE")
                .amount(totalAmount)
                .summary("采购退货冲暂估")
                .auxType(FiVoucherEntry.AUX_SUPPLIER)
                .auxId(paReturn.getSupplierId())
                .build());
        Long voucherId = voucherService.generateBizVoucher(VOUCHER_BIZ_TYPE, paReturn.getReturnDate(),
                "采购退货 " + paReturn.getReturnNo(), voucherLines);

        // 冲减往来账：按来源入库单冲减应付余额，避免应付长期虚高
        apPayableService.reverseBySource("PA_INSTOCK", source.getId(), totalAmount);

        PaReturn update = new PaReturn();
        update.setId(id);
        update.setStatus(PaReturn.STATUS_AUDITED);
        update.setAuditBy(SecurityUtils.getCurrentUserId());
        update.setAuditTime(LocalDateTime.now());
        update.setVoucherStatus(PaReturn.VOUCHER_STATUS_DONE);
        update.setTotalAmount(totalAmount);
        update.setTotalCost(totalNetAmount);
        update.setTaxAmount(totalTax);
        update.setVoucherId(voucherId);
        this.updateById(update);
    }

    private PaInstock validateSource(Long sourceInstockId, List<PaReturnEntryRequest> entryRequests) {
        PaInstock source = instockMapper.selectById(sourceInstockId);
        if (source == null) {
            throw new BizException("来源入库单不存在");
        }
        if (source.getStatus() == null || source.getStatus() != PaInstock.STATUS_AUDITED) {
            throw new BizException("来源入库单未审核，不能退货");
        }
        Map<Long, PaInstockEntry> sourceEntryMap = loadSourceEntries(source.getId());
        for (PaReturnEntryRequest entryRequest : entryRequests) {
            PaInstockEntry sourceEntry = sourceEntryMap.get(entryRequest.getSourceEntryId());
            if (sourceEntry == null) {
                throw new BizException("来源入库明细不存在：" + entryRequest.getSourceEntryId());
            }
            BigDecimal returned = sourceEntry.getReturnedQty() == null ? BigDecimal.ZERO : sourceEntry.getReturnedQty();
            BigDecimal remaining = sourceEntry.getQty().subtract(returned);
            if (entryRequest.getQty().compareTo(remaining) > 0) {
                throw new BizException("退货数量超过可退数量，物料：" + sourceEntry.getMaterialId());
            }
        }
        return source;
    }

    private Map<Long, PaInstockEntry> loadSourceEntries(Long instockId) {
        List<PaInstockEntry> entries = instockEntryMapper.selectList(Wrappers.<PaInstockEntry>lambdaQuery()
                .eq(PaInstockEntry::getInstockId, instockId));
        return entries.stream().collect(Collectors.toMap(PaInstockEntry::getId, Function.identity()));
    }

    private BigDecimal saveEntries(Long returnId, List<PaReturnEntryRequest> entryRequests, Map<Long, PaInstockEntry> sourceEntryMap) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PaReturnEntryRequest entryRequest : entryRequests) {
            PaInstockEntry sourceEntry = sourceEntryMap.get(entryRequest.getSourceEntryId());
            if (sourceEntry == null) {
                throw new BizException("来源入库明细不存在：" + entryRequest.getSourceEntryId());
            }
            PaReturnEntry entry = new PaReturnEntry();
            entry.setReturnId(returnId);
            entry.setSourceEntryId(sourceEntry.getId());
            entry.setMaterialId(sourceEntry.getMaterialId());
            entry.setQty(entryRequest.getQty());
            entry.setPrice(sourceEntry.getPrice());
            entry.setTaxRate(sourceEntry.getTaxRate() == null ? BigDecimal.ZERO : sourceEntry.getTaxRate());
            entry.setAmount(calcAmount(entry.getQty(), entry.getPrice(), entry.getTaxRate()));
            entry.setBatchNo(sourceEntry.getBatchNo() == null ? "" : sourceEntry.getBatchNo());
            entry.setRemark(entryRequest.getRemark());
            returnEntryMapper.insert(entry);
            totalAmount = totalAmount.add(entry.getAmount());
        }
        return totalAmount;
    }

    private BigDecimal calcAmount(BigDecimal qty, BigDecimal price, BigDecimal taxRate) {
        BigDecimal gross = BigDecimal.ONE.add(taxRate.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP));
        return qty.multiply(price).multiply(gross).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<Long, MdMaterial> loadMaterials(List<PaReturnEntry> entries) {
        List<Long> materialIds = entries.stream().map(PaReturnEntry::getMaterialId).distinct().collect(Collectors.toList());
        return materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery().in(MdMaterial::getId, materialIds))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
    }

    private List<PaReturnEntryVO> loadEntries(Long returnId) {
        List<PaReturnEntry> entries = returnEntryMapper.selectList(Wrappers.<PaReturnEntry>lambdaQuery()
                .eq(PaReturnEntry::getReturnId, returnId)
                .orderByAsc(PaReturnEntry::getId));
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, MdMaterial> materialMap = loadMaterials(entries);
        return entries.stream().map(entry -> {
            PaReturnEntryVO vo = new PaReturnEntryVO();
            BeanUtils.copyProperties(entry, vo);
            MdMaterial material = materialMap.get(entry.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getMaterialCode());
                vo.setMaterialName(material.getMaterialName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private String nextReturnNo(LocalDate returnDate) {
        // 序号取自流水表原子自增：并发不撞号，软删除不重用旧号
        return serialNoGenerator.next("CT", returnDate.format(MONTH_FORMATTER));
    }

    private PaReturnListVO toListVO(PaReturn paReturn) {
        PaReturnListVO vo = new PaReturnListVO();
        BeanUtils.copyProperties(paReturn, vo);
        PaInstock source = instockMapper.selectById(paReturn.getSourceInstockId());
        if (source != null) {
            vo.setSourceInstockNo(source.getInstockNo());
        }
        MdSupplier supplier = supplierMapper.selectById(paReturn.getSupplierId());
        if (supplier != null) {
            vo.setSupplierName(supplier.getSupplierName());
        }
        MdWarehouse warehouse = warehouseMapper.selectById(paReturn.getWarehouseId());
        if (warehouse != null) {
            vo.setWarehouseName(warehouse.getWarehouseName());
        }
        return vo;
    }
}








