package com.erp.sa.service.impl;

import com.erp.common.util.SerialNoGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.security.datascope.DataScopeHelper;
import com.erp.ar.service.ArReceivableService;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.common.util.TaxAmountCalculator;
import com.erp.fi.domain.dto.VoucherLine;
import com.erp.fi.domain.entity.FiVoucherEntry;
import com.erp.fi.service.FiVoucherService;
import com.erp.ic.service.IcStockService;
import com.erp.md.domain.entity.MdCustomer;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.domain.entity.MdWarehouse;
import com.erp.md.mapper.MdCustomerMapper;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.md.mapper.MdWarehouseMapper;
import com.erp.sa.domain.dto.SaReturnEntryRequest;
import com.erp.sa.domain.dto.SaReturnQueryRequest;
import com.erp.sa.domain.dto.SaReturnSaveRequest;
import com.erp.sa.domain.entity.SaOutstock;
import com.erp.sa.domain.entity.SaOutstockEntry;
import com.erp.sa.domain.entity.SaReturn;
import com.erp.sa.domain.entity.SaReturnEntry;
import com.erp.sa.domain.vo.SaReturnDetailVO;
import com.erp.sa.domain.vo.SaReturnEntryVO;
import com.erp.sa.domain.vo.SaReturnListVO;
import com.erp.sa.mapper.SaOutstockEntryMapper;
import com.erp.sa.mapper.SaOutstockMapper;
import com.erp.sa.mapper.SaReturnEntryMapper;
import com.erp.sa.mapper.SaReturnMapper;
import com.erp.sa.service.SaReturnService;
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
public class SaReturnServiceImpl extends ServiceImpl<SaReturnMapper, SaReturn> implements SaReturnService {

    private static final String RETURN_NO_PREFIX = "XT-";
    private static final String TRAN_TYPE = "SA_RETURN";
    private static final String VOUCHER_BIZ_TYPE = "SA_RETURN";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final SaReturnEntryMapper returnEntryMapper;

    private final DataScopeHelper dataScopeHelper;
    private final SaOutstockMapper outstockMapper;
    private final SaOutstockEntryMapper outstockEntryMapper;
    private final MdCustomerMapper customerMapper;
    private final MdWarehouseMapper warehouseMapper;
    private final MdMaterialMapper materialMapper;
    private final IcStockService stockService;
    private final FiVoucherService voucherService;
    private final ArReceivableService arReceivableService;
    /** 业务单号生成器（原子自增，避免并发撞号） */
    private final SerialNoGenerator serialNoGenerator;

    @Override
    public PageResult<SaReturnListVO> pageReturn(SaReturnQueryRequest request) {
        Page<SaReturn> page = this.page(request.toPage(), Wrappers.<SaReturn>lambdaQuery()
                .like(StringUtils.hasText(request.getReturnNo()), SaReturn::getReturnNo, request.getReturnNo())
                .eq(request.getSourceOutstockId() != null, SaReturn::getSourceOutstockId, request.getSourceOutstockId())
                .eq(request.getCustomerId() != null, SaReturn::getCustomerId, request.getCustomerId())
                .eq(request.getStatus() != null, SaReturn::getStatus, request.getStatus())
                .ge(request.getStartDate() != null, SaReturn::getReturnDate, request.getStartDate())
                .le(request.getEndDate() != null, SaReturn::getReturnDate, request.getEndDate())
                .orderByDesc(SaReturn::getReturnDate)
                .orderByDesc(SaReturn::getId));
        List<SaReturnListVO> records = page.getRecords().stream().map(this::toListVO).collect(Collectors.toList());
        Page<SaReturnListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public SaReturnDetailVO getReturnDetail(Long id) {
        SaReturn saReturn = this.getById(id);
        if (saReturn == null) {
            throw new BizException("销售退货单不存在");
        }
        SaReturnDetailVO detail = new SaReturnDetailVO();
        BeanUtils.copyProperties(saReturn, detail);
        detail.setEntries(loadEntries(id));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReturn(SaReturnSaveRequest request) {
        SaOutstock source = validateSource(request.getSourceOutstockId(), request.getEntries());
        Map<Long, SaOutstockEntry> sourceEntryMap = loadSourceEntries(source.getId());
        SaReturn saReturn = new SaReturn();
        saReturn.setReturnNo(nextReturnNo(request.getReturnDate()));
        saReturn.setSourceOutstockId(source.getId());
        saReturn.setCustomerId(source.getCustomerId());
        saReturn.setWarehouseId(source.getWarehouseId());
        saReturn.setReturnDate(request.getReturnDate());
        // 归属部门供数据权限过滤：优先继承来源单据，其次取当前用户部门
        saReturn.setDeptId(dataScopeHelper.resolveDeptId(null, null));
        saReturn.setStatus(SaReturn.STATUS_DRAFT);
        saReturn.setRemark(request.getRemark());
        this.save(saReturn);
        BigDecimal[] totals = saveEntries(saReturn.getId(), request.getEntries(), sourceEntryMap);
        saReturn.setTotalAmount(totals[0]);
        saReturn.setTotalCost(totals[1]);
        this.updateById(saReturn);
        return saReturn.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReturn(SaReturnSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        SaReturn exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("销售退货单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != SaReturn.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的退货单才能修改");
        }
        SaOutstock source = validateSource(request.getSourceOutstockId(), request.getEntries());
        Map<Long, SaOutstockEntry> sourceEntryMap = loadSourceEntries(source.getId());
        returnEntryMapper.delete(Wrappers.<SaReturnEntry>lambdaQuery().eq(SaReturnEntry::getReturnId, request.getId()));
        BigDecimal[] totals = saveEntries(request.getId(), request.getEntries(), sourceEntryMap);
        SaReturn update = new SaReturn();
        update.setId(request.getId());
        update.setTotalAmount(totals[0]);
        update.setTotalCost(totals[1]);
        this.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReturn(Long id) {
        SaReturn exist = this.getById(id);
        if (exist == null) {
            throw new BizException("销售退货单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != SaReturn.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的退货单才能删除");
        }
        this.removeById(id);
        returnEntryMapper.delete(Wrappers.<SaReturnEntry>lambdaQuery().eq(SaReturnEntry::getReturnId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditReturn(Long id) {
        SaReturn saReturn = this.getById(id);
        if (saReturn == null) {
            throw new BizException("销售退货单不存在");
        }
        if (saReturn.getStatus() == null || saReturn.getStatus() != SaReturn.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的退货单才能审核");
        }
        SaOutstock source = outstockMapper.selectById(saReturn.getSourceOutstockId());
        if (source == null) {
            throw new BizException("来源出库单不存在");
        }
        List<SaReturnEntry> entries = returnEntryMapper.selectList(Wrappers.<SaReturnEntry>lambdaQuery()
                .eq(SaReturnEntry::getReturnId, id)
                .orderByAsc(SaReturnEntry::getId));
        if (entries.isEmpty()) {
            throw new BizException("退货单明细为空");
        }
        Map<Long, SaOutstockEntry> sourceEntryMap = loadSourceEntries(source.getId());
        Map<Long, MdMaterial> materialMap = loadMaterials(entries);
        List<VoucherLine> voucherLines = new ArrayList<>();
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalNetAmount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        for (SaReturnEntry entry : entries) {
            SaOutstockEntry sourceEntry = sourceEntryMap.get(entry.getSourceEntryId());
            if (sourceEntry == null) {
                throw new BizException("来源出库明细不存在：" + entry.getSourceEntryId());
            }
            MdMaterial material = materialMap.get(entry.getMaterialId());
            String materialName = material == null ? String.valueOf(entry.getMaterialId()) : material.getMaterialName();
            stockService.increaseStock(entry.getMaterialId(), source.getWarehouseId(), entry.getBatchNo(),
                    entry.getQty(), TRAN_TYPE, saReturn.getId(), saReturn.getReturnNo(), saReturn.getReturnDate(),
                    "销售退货");
            outstockEntryMapper.addReturnedQty(entry.getSourceEntryId(), entry.getQty());

            // 价税分离：退货需冲回收入与销项税额
            BigDecimal[] taxSplit = TaxAmountCalculator.split(entry.getQty(), entry.getPrice(), entry.getTaxRate());
            BigDecimal netAmount = taxSplit[0];
            BigDecimal taxAmount = taxSplit[1];

            voucherLines.add(VoucherLine.builder()
                    .direction(FiVoucherEntry.DIRECTION_DEBIT)
                    .ruleKey("REVENUE")
                    .amount(netAmount)
                    .summary("销售退货冲收入：" + materialName)
                    .build());
            if (taxAmount.compareTo(BigDecimal.ZERO) > 0) {
                voucherLines.add(VoucherLine.builder()
                        .direction(FiVoucherEntry.DIRECTION_DEBIT)
                        .ruleKey("TAX")
                        .amount(taxAmount)
                        .summary("销售退货冲销项税额：" + materialName)
                        .build());
            }
            voucherLines.add(VoucherLine.builder()
                    .direction(FiVoucherEntry.DIRECTION_CREDIT)
                    .ruleKey("STOCK")
                    .amount(entry.getCostAmount())
                    .summary("销售退货退回库存：" + materialName)
                    .build());

            totalCost = totalCost.add(entry.getCostAmount());
            totalAmount = totalAmount.add(entry.getAmount());
            totalNetAmount = totalNetAmount.add(netAmount);
            totalTax = totalTax.add(taxAmount);
        }
        // 冲减应收账款
        voucherLines.add(VoucherLine.builder()
                .direction(FiVoucherEntry.DIRECTION_CREDIT)
                .ruleKey("RECEIVABLE")
                .amount(totalAmount)
                .summary("销售退货冲应收")
                .auxType(FiVoucherEntry.AUX_CUSTOMER)
                .auxId(saReturn.getCustomerId())
                .build());
        voucherLines.add(VoucherLine.builder()
                .direction(FiVoucherEntry.DIRECTION_CREDIT)
                .ruleKey("COST")
                .amount(totalCost)
                .summary("销售退货冲成本")
                .build());
        Long voucherId = voucherService.generateBizVoucher(VOUCHER_BIZ_TYPE, saReturn.getReturnDate(),
                "销售退货 " + saReturn.getReturnNo(), voucherLines);

        // 冲减往来账：按来源出库单冲减应收余额，避免应收长期虚高
        arReceivableService.reverseBySource("SA_OUTSTOCK", source.getId(), totalAmount);

        SaReturn update = new SaReturn();
        update.setId(id);
        update.setStatus(SaReturn.STATUS_AUDITED);
        update.setAuditBy(SecurityUtils.getCurrentUserId());
        update.setAuditTime(LocalDateTime.now());
        update.setVoucherStatus(SaReturn.VOUCHER_STATUS_DONE);
        update.setVoucherId(voucherId);
        update.setTotalCost(totalCost);
        update.setTotalAmount(totalAmount);
        update.setTaxAmount(totalTax);
        this.updateById(update);
    }

    private SaOutstock validateSource(Long sourceOutstockId, List<SaReturnEntryRequest> entryRequests) {
        SaOutstock source = outstockMapper.selectById(sourceOutstockId);
        if (source == null) {
            throw new BizException("来源出库单不存在");
        }
        if (source.getStatus() == null || source.getStatus() != SaOutstock.STATUS_AUDITED) {
            throw new BizException("来源出库单未审核，不能退货");
        }
        Map<Long, SaOutstockEntry> sourceEntryMap = loadSourceEntries(source.getId());
        for (SaReturnEntryRequest entryRequest : entryRequests) {
            SaOutstockEntry sourceEntry = sourceEntryMap.get(entryRequest.getSourceEntryId());
            if (sourceEntry == null) {
                throw new BizException("来源出库明细不存在：" + entryRequest.getSourceEntryId());
            }
            BigDecimal returned = sourceEntry.getReturnedQty() == null ? BigDecimal.ZERO : sourceEntry.getReturnedQty();
            BigDecimal remaining = sourceEntry.getQty().subtract(returned);
            if (entryRequest.getQty().compareTo(remaining) > 0) {
                throw new BizException("退货数量超过可退数量，物料：" + sourceEntry.getMaterialId());
            }
        }
        return source;
    }

    private Map<Long, SaOutstockEntry> loadSourceEntries(Long outstockId) {
        List<SaOutstockEntry> entries = outstockEntryMapper.selectList(Wrappers.<SaOutstockEntry>lambdaQuery()
                .eq(SaOutstockEntry::getOutstockId, outstockId));
        return entries.stream().collect(Collectors.toMap(SaOutstockEntry::getId, Function.identity()));
    }

    private BigDecimal[] saveEntries(Long returnId, List<SaReturnEntryRequest> entryRequests, Map<Long, SaOutstockEntry> sourceEntryMap) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        for (SaReturnEntryRequest entryRequest : entryRequests) {
            SaOutstockEntry sourceEntry = sourceEntryMap.get(entryRequest.getSourceEntryId());
            if (sourceEntry == null) {
                throw new BizException("来源出库明细不存在：" + entryRequest.getSourceEntryId());
            }
            SaReturnEntry entry = new SaReturnEntry();
            entry.setReturnId(returnId);
            entry.setSourceEntryId(sourceEntry.getId());
            entry.setMaterialId(sourceEntry.getMaterialId());
            entry.setQty(entryRequest.getQty());
            entry.setPrice(sourceEntry.getPrice());
            entry.setTaxRate(sourceEntry.getTaxRate() == null ? BigDecimal.ZERO : sourceEntry.getTaxRate());
            entry.setAmount(calcAmount(entry.getQty(), entry.getPrice(), entry.getTaxRate()));
            BigDecimal costPrice = sourceEntry.getCostPrice() == null ? BigDecimal.ZERO : sourceEntry.getCostPrice();
            entry.setCostPrice(costPrice);
            entry.setCostAmount(costPrice.multiply(entry.getQty()).setScale(2, RoundingMode.HALF_UP));
            entry.setBatchNo(sourceEntry.getBatchNo() == null ? "" : sourceEntry.getBatchNo());
            entry.setRemark(entryRequest.getRemark());
            returnEntryMapper.insert(entry);
            totalAmount = totalAmount.add(entry.getAmount());
            totalCost = totalCost.add(entry.getCostAmount());
        }
        return new BigDecimal[]{totalAmount, totalCost};
    }

    private BigDecimal calcAmount(BigDecimal qty, BigDecimal price, BigDecimal taxRate) {
        BigDecimal gross = BigDecimal.ONE.add(taxRate.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP));
        return qty.multiply(price).multiply(gross).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<Long, MdMaterial> loadMaterials(List<SaReturnEntry> entries) {
        List<Long> materialIds = entries.stream().map(SaReturnEntry::getMaterialId).distinct().collect(Collectors.toList());
        return materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery().in(MdMaterial::getId, materialIds))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
    }

    private List<SaReturnEntryVO> loadEntries(Long returnId) {
        List<SaReturnEntry> entries = returnEntryMapper.selectList(Wrappers.<SaReturnEntry>lambdaQuery()
                .eq(SaReturnEntry::getReturnId, returnId)
                .orderByAsc(SaReturnEntry::getId));
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, MdMaterial> materialMap = loadMaterials(entries);
        return entries.stream().map(entry -> {
            SaReturnEntryVO vo = new SaReturnEntryVO();
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
        return serialNoGenerator.next("XT", returnDate.format(MONTH_FORMATTER));
    }

    private SaReturnListVO toListVO(SaReturn saReturn) {
        SaReturnListVO vo = new SaReturnListVO();
        BeanUtils.copyProperties(saReturn, vo);
        SaOutstock source = outstockMapper.selectById(saReturn.getSourceOutstockId());
        if (source != null) {
            vo.setSourceOutstockNo(source.getOutstockNo());
        }
        MdCustomer customer = customerMapper.selectById(saReturn.getCustomerId());
        if (customer != null) {
            vo.setCustomerName(customer.getCustomerName());
        }
        MdWarehouse warehouse = warehouseMapper.selectById(saReturn.getWarehouseId());
        if (warehouse != null) {
            vo.setWarehouseName(warehouse.getWarehouseName());
        }
        return vo;
    }
}








