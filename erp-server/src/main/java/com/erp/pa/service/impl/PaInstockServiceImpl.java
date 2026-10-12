package com.erp.pa.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.ap.service.ApPayableService;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.common.util.TaxAmountCalculator;
import com.erp.fi.domain.dto.VoucherLine;
import com.erp.fi.domain.entity.FiVoucherEntry;
import com.erp.fi.service.FiVoucherService;
import com.erp.ic.mapper.IcStockMapper;
import com.erp.ic.service.IcStockService;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.domain.entity.MdSupplier;
import com.erp.md.domain.entity.MdWarehouse;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.md.mapper.MdSupplierMapper;
import com.erp.md.mapper.MdWarehouseMapper;
import com.erp.pa.domain.dto.PaInstockEntryRequest;
import com.erp.pa.domain.dto.PaInstockQueryRequest;
import com.erp.pa.domain.dto.PaInstockSaveRequest;
import com.erp.pa.domain.entity.PaInstock;
import com.erp.pa.domain.entity.PaInstockEntry;
import com.erp.pa.domain.entity.PaOrder;
import com.erp.pa.domain.entity.PaOrderEntry;
import com.erp.pa.domain.vo.PaInstockDetailVO;
import com.erp.pa.domain.vo.PaInstockEntryVO;
import com.erp.pa.domain.vo.PaInstockListVO;
import com.erp.pa.mapper.PaInstockEntryMapper;
import com.erp.pa.mapper.PaInstockMapper;
import com.erp.pa.mapper.PaOrderEntryMapper;
import com.erp.pa.mapper.PaOrderMapper;
import com.erp.pa.service.PaInstockService;
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
public class PaInstockServiceImpl extends ServiceImpl<PaInstockMapper, PaInstock> implements PaInstockService {

    private static final String INSTOCK_NO_PREFIX = "CR-";
    private static final String TRAN_TYPE = "PA_INSTOCK";
    private static final String VOUCHER_BIZ_TYPE = "PA_INSTOCK";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PaInstockEntryMapper instockEntryMapper;
    private final PaOrderMapper orderMapper;
    private final PaOrderEntryMapper orderEntryMapper;
    private final MdSupplierMapper supplierMapper;
    private final MdWarehouseMapper warehouseMapper;
    private final MdMaterialMapper materialMapper;
    private final IcStockMapper stockMapper;
    private final IcStockService stockService;
    private final FiVoucherService voucherService;
    private final ApPayableService apPayableService;

    @Override
    public PageResult<PaInstockListVO> pageInstock(PaInstockQueryRequest request) {
        Page<PaInstock> page = this.page(request.toPage(), Wrappers.<PaInstock>lambdaQuery()
                .like(StringUtils.hasText(request.getInstockNo()), PaInstock::getInstockNo, request.getInstockNo())
                .eq(request.getOrderId() != null, PaInstock::getOrderId, request.getOrderId())
                .eq(request.getSupplierId() != null, PaInstock::getSupplierId, request.getSupplierId())
                .eq(request.getWarehouseId() != null, PaInstock::getWarehouseId, request.getWarehouseId())
                .eq(request.getStatus() != null, PaInstock::getStatus, request.getStatus())
                .eq(request.getVoucherStatus() != null, PaInstock::getVoucherStatus, request.getVoucherStatus())
                .ge(request.getStartDate() != null, PaInstock::getInstockDate, request.getStartDate())
                .le(request.getEndDate() != null, PaInstock::getInstockDate, request.getEndDate())
                .orderByDesc(PaInstock::getInstockDate)
                .orderByDesc(PaInstock::getId));
        List<PaInstockListVO> records = page.getRecords().stream().map(this::toListVO).collect(Collectors.toList());
        Page<PaInstockListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public PaInstockDetailVO getInstockDetail(Long id) {
        PaInstock instock = this.getById(id);
        if (instock == null) {
            throw new BizException("采购入库单不存在");
        }
        PaInstockDetailVO detail = new PaInstockDetailVO();
        BeanUtils.copyProperties(instock, detail);
        detail.setEntries(loadEntries(id));
        if (instock.getOrderId() != null) {
            PaOrder order = orderMapper.selectById(instock.getOrderId());
            if (order != null) {
                detail.setOrderNo(order.getOrderNo());
            }
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInstock(PaInstockSaveRequest request) {
        MdSupplier supplier = supplierMapper.selectById(request.getSupplierId());
        if (supplier == null) {
            throw new BizException("供应商不存在");
        }
        MdWarehouse warehouse = warehouseMapper.selectById(request.getWarehouseId());
        if (warehouse == null) {
            throw new BizException("仓库不存在");
        }
        PaOrder order = null;
        if (request.getOrderId() != null) {
            order = orderMapper.selectById(request.getOrderId());
            if (order == null) {
                throw new BizException("采购订单不存在");
            }
            if (order.getStatus() == null || order.getStatus() != PaOrder.STATUS_AUDITED) {
                throw new BizException("采购订单未审核，不能入库");
            }
            if (!order.getSupplierId().equals(request.getSupplierId())) {
                throw new BizException("入库供应商与订单供应商不一致");
            }
        }
        Map<Long, PaOrderEntry> orderEntryMap = loadOrderEntries(request, order);
        PaInstock instock = new PaInstock();
        instock.setInstockNo(nextInstockNo(request.getInstockDate()));
        instock.setOrderId(request.getOrderId());
        instock.setSupplierId(request.getSupplierId());
        instock.setWarehouseId(request.getWarehouseId());
        instock.setInstockDate(request.getInstockDate());
        instock.setStatus(PaInstock.STATUS_DRAFT);
        instock.setVoucherStatus(PaInstock.VOUCHER_STATUS_NONE);
        instock.setRemark(request.getRemark());
        this.save(instock);
        BigDecimal totalAmount = saveEntries(instock.getId(), request.getEntries(), orderEntryMap);
        instock.setTotalAmount(totalAmount);
        this.updateById(instock);
        return instock.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInstock(PaInstockSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        PaInstock exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("采购入库单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != PaInstock.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的入库单才能修改");
        }
        MdWarehouse warehouse = warehouseMapper.selectById(request.getWarehouseId());
        if (warehouse == null) {
            throw new BizException("仓库不存在");
        }
        PaInstock instock = new PaInstock();
        instock.setId(request.getId());
        instock.setOrderId(request.getOrderId());
        instock.setSupplierId(request.getSupplierId());
        instock.setWarehouseId(request.getWarehouseId());
        instock.setInstockDate(request.getInstockDate());
        instock.setRemark(request.getRemark());
        this.updateById(instock);
        instockEntryMapper.delete(Wrappers.<PaInstockEntry>lambdaQuery().eq(PaInstockEntry::getInstockId, request.getId()));
        BigDecimal totalAmount = saveEntries(request.getId(), request.getEntries(), loadOrderEntries(request, null));
        PaInstock totalUpdate = new PaInstock();
        totalUpdate.setId(request.getId());
        totalUpdate.setTotalAmount(totalAmount);
        this.updateById(totalUpdate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInstock(Long id) {
        PaInstock exist = this.getById(id);
        if (exist == null) {
            throw new BizException("采购入库单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != PaInstock.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的入库单才能删除");
        }
        this.removeById(id);
        instockEntryMapper.delete(Wrappers.<PaInstockEntry>lambdaQuery().eq(PaInstockEntry::getInstockId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditInstock(Long id) {
        PaInstock instock = this.getById(id);
        if (instock == null) {
            throw new BizException("采购入库单不存在");
        }
        if (instock.getStatus() == null || instock.getStatus() != PaInstock.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的入库单才能审核");
        }
        List<PaInstockEntry> entries = instockEntryMapper.selectList(Wrappers.<PaInstockEntry>lambdaQuery()
                .eq(PaInstockEntry::getInstockId, id)
                .orderByAsc(PaInstockEntry::getId));
        if (entries.isEmpty()) {
            throw new BizException("入库单明细为空");
        }
        Map<Long, MdMaterial> materialMap = loadMaterials(entries);
        Map<Long, BigDecimal> beforeQtyMap = loadBeforeQtyMap(entries);

        // 审核时按最新已收数量复核，防止多张入库单先后通过暂存校验造成超收
        checkReceivableQty(entries);

        List<VoucherLine> voucherLines = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalNetAmount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        for (PaInstockEntry entry : entries) {
            MdMaterial material = materialMap.get(entry.getMaterialId());
            String materialName = material == null ? String.valueOf(entry.getMaterialId()) : material.getMaterialName();
            stockService.increaseStock(entry.getMaterialId(), instock.getWarehouseId(), entry.getBatchNo(),
                    entry.getQty(), TRAN_TYPE, instock.getId(), instock.getInstockNo(), instock.getInstockDate(),
                    "采购入库");
            if (entry.getOrderEntryId() != null) {
                orderEntryMapper.addReceivedQty(entry.getOrderEntryId(), entry.getQty());
            }

            // 价税分离：存货成本按不含税金额入账，进项税额单独列示
            BigDecimal[] taxSplit = TaxAmountCalculator.split(entry.getQty(), entry.getPrice(), entry.getTaxRate());
            BigDecimal netAmount = taxSplit[0];
            BigDecimal taxAmount = taxSplit[1];

            // 移动加权成本按不含税单价计算，与总账存货科目口径一致
            // netAmount 是「数量 × 单价」的结果，需还原为单价参与加权
            BigDecimal netUnitPrice = netAmount.divide(entry.getQty(), 4, RoundingMode.HALF_UP);
            updateMovingAverageCost(material, entry.getQty(), netUnitPrice, beforeQtyMap);

            voucherLines.add(VoucherLine.builder()
                    .direction(FiVoucherEntry.DIRECTION_DEBIT)
                    .ruleKey("MATERIAL")
                    .amount(netAmount)
                    .summary("采购入库：" + materialName)
                    .build());
            if (taxAmount.compareTo(BigDecimal.ZERO) > 0) {
                voucherLines.add(VoucherLine.builder()
                        .direction(FiVoucherEntry.DIRECTION_DEBIT)
                        .ruleKey("TAX")
                        .amount(taxAmount)
                        .summary("进项税额：" + materialName)
                        .build());
            }
            totalNetAmount = totalNetAmount.add(netAmount);
            totalTax = totalTax.add(taxAmount);
            totalAmount = totalAmount.add(entry.getAmount());
        }
        voucherLines.add(VoucherLine.builder()
                .direction(FiVoucherEntry.DIRECTION_CREDIT)
                .ruleKey("PAYABLE")
                .amount(totalAmount)
                .summary("采购入库暂估")
                .auxType(FiVoucherEntry.AUX_SUPPLIER)
                .auxId(instock.getSupplierId())
                .build());
        Long voucherId = voucherService.generateBizVoucher(VOUCHER_BIZ_TYPE, instock.getInstockDate(),
                "采购入库 " + instock.getInstockNo(), voucherLines);
        apPayableService.createFromInstock(instock.getSupplierId(), instock.getId(),
                instock.getInstockNo(), totalAmount, instock.getInstockDate());
        refreshOrderStatus(instock.getOrderId());
        PaInstock update = new PaInstock();
        update.setId(id);
        update.setStatus(PaInstock.STATUS_AUDITED);
        update.setAuditBy(SecurityUtils.getCurrentUserId());
        update.setAuditTime(LocalDateTime.now());
        update.setVoucherStatus(PaInstock.VOUCHER_STATUS_DONE);
        update.setVoucherId(voucherId);
        update.setTotalAmount(totalAmount);
        update.setTotalCost(totalNetAmount);
        update.setTaxAmount(totalTax);
        this.updateById(update);
    }

    /**
     * 审核时校验订单可入库数量。
     * <p>
     * 暂存时校验不足以防超收——多张入库单可以先后通过暂存校验，
     * 只有在审核环节按最新已收数量复核才能真正拦住超收。
     */
    private void checkReceivableQty(List<PaInstockEntry> entries) {
        List<Long> orderEntryIds = entries.stream()
                .map(PaInstockEntry::getOrderEntryId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (orderEntryIds.isEmpty()) {
            return;
        }
        Map<Long, PaOrderEntry> orderEntryMap = orderEntryMapper.selectBatchIds(orderEntryIds).stream()
                .collect(Collectors.toMap(PaOrderEntry::getId, Function.identity()));

        Map<Long, BigDecimal> currentQtyMap = new HashMap<>();
        for (PaInstockEntry entry : entries) {
            if (entry.getOrderEntryId() == null) {
                continue;
            }
            currentQtyMap.merge(entry.getOrderEntryId(), entry.getQty(), BigDecimal::add);
        }
        for (Map.Entry<Long, BigDecimal> item : currentQtyMap.entrySet()) {
            PaOrderEntry orderEntry = orderEntryMap.get(item.getKey());
            if (orderEntry == null) {
                throw new BizException("订单明细不存在：" + item.getKey());
            }
            BigDecimal received = orderEntry.getReceivedQty() == null
                    ? BigDecimal.ZERO : orderEntry.getReceivedQty();
            BigDecimal remaining = orderEntry.getQty().subtract(received);
            if (item.getValue().compareTo(remaining) > 0) {
                throw new BizException("入库数量超过订单可入库数量，物料：" + orderEntry.getMaterialId());
            }
        }
    }

    private Map<Long, BigDecimal> loadBeforeQtyMap(List<PaInstockEntry> entries) {
        List<Long> materialIds = entries.stream()
                .map(PaInstockEntry::getMaterialId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, BigDecimal> beforeQtyMap = new java.util.HashMap<>();
        for (Map<String, Object> row : stockMapper.sumQtyByMaterialIds(materialIds)) {
            Object materialId = row.get("materialId");
            Object totalQty = row.get("totalQty");
            if (materialId != null && totalQty != null) {
                beforeQtyMap.put(((Number) materialId).longValue(), new BigDecimal(String.valueOf(totalQty)));
            }
        }
        return beforeQtyMap;
    }

    private void updateMovingAverageCost(MdMaterial material, BigDecimal instockQty, BigDecimal price,
                                         Map<Long, BigDecimal> beforeQtyMap) {
        if (material == null || price == null) {
            return;
        }
        BigDecimal beforeQty = beforeQtyMap.getOrDefault(material.getId(), BigDecimal.ZERO);
        BigDecimal newQty = beforeQty.add(instockQty);
        if (newQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal oldCost = material.getCostPrice() == null ? BigDecimal.ZERO : material.getCostPrice();
        BigDecimal inCost = price;
        BigDecimal newCost = oldCost.multiply(beforeQty).add(inCost.multiply(instockQty))
                .divide(newQty, 4, RoundingMode.HALF_UP);
        MdMaterial update = new MdMaterial();
        update.setId(material.getId());
        update.setCostPrice(newCost);
        materialMapper.updateById(update);
        material.setCostPrice(newCost);
        beforeQtyMap.put(material.getId(), newQty);
    }

    /**
     * 刷新订单状态。
     * <p>
     * 已关闭的订单不能被入库动作改回「已完成」——关闭是业务的明确终止意图。
     */
    private void refreshOrderStatus(Long orderId) {
        if (orderId == null) {
            return;
        }
        PaOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        if (order.getStatus() != null && order.getStatus() == PaOrder.STATUS_AUDITED) {
            return;
        }
        Long pendingCount = orderEntryMapper.selectCount(Wrappers.<PaOrderEntry>lambdaQuery()
                .eq(PaOrderEntry::getOrderId, orderId)
                .apply("qty > received_qty"));
        if (pendingCount != null && pendingCount > 0) {
            return;
        }
        PaOrder update = new PaOrder();
        update.setId(orderId);
        update.setStatus(PaOrder.STATUS_COMPLETED);
        orderMapper.updateById(update);
    }

    private Map<Long, PaOrderEntry> loadOrderEntries(PaInstockSaveRequest request, PaOrder order) {
        if (order == null && request.getOrderId() == null) {
            return Collections.emptyMap();
        }
        PaOrder effectiveOrder = order;
        if (effectiveOrder == null) {
            effectiveOrder = orderMapper.selectById(request.getOrderId());
            if (effectiveOrder == null) {
                throw new BizException("采购订单不存在");
            }
        }
        List<PaOrderEntry> orderEntries = orderEntryMapper.selectList(Wrappers.<PaOrderEntry>lambdaQuery()
                .eq(PaOrderEntry::getOrderId, effectiveOrder.getId()));
        Map<Long, PaOrderEntry> map = orderEntries.stream()
                .collect(Collectors.toMap(PaOrderEntry::getId, Function.identity()));
        for (PaInstockEntryRequest entryRequest : request.getEntries()) {
            if (entryRequest.getOrderEntryId() == null) {
                continue;
            }
            PaOrderEntry orderEntry = map.get(entryRequest.getOrderEntryId());
            if (orderEntry == null) {
                throw new BizException("订单明细不存在：" + entryRequest.getOrderEntryId());
            }
            if (!orderEntry.getMaterialId().equals(entryRequest.getMaterialId())) {
                throw new BizException("入库物料与订单明细物料不一致");
            }
            BigDecimal remaining = orderEntry.getQty().subtract(
                    orderEntry.getReceivedQty() == null ? BigDecimal.ZERO : orderEntry.getReceivedQty());
            if (entryRequest.getQty().compareTo(remaining) > 0) {
                throw new BizException("入库数量超过订单可入库数量，物料：" + entryRequest.getMaterialId());
            }
        }
        return map;
    }

    private BigDecimal saveEntries(Long instockId, List<PaInstockEntryRequest> entryRequests, Map<Long, PaOrderEntry> orderEntryMap) {
        if (entryRequests == null || entryRequests.isEmpty()) {
            throw new BizException("入库明细不能为空");
        }
        List<Long> materialIds = entryRequests.stream()
                .map(PaInstockEntryRequest::getMaterialId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, MdMaterial> materialMap = materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery()
                        .in(MdMaterial::getId, materialIds))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PaInstockEntryRequest entryRequest : entryRequests) {
            MdMaterial material = materialMap.get(entryRequest.getMaterialId());
            if (material == null) {
                throw new BizException("物料不存在：" + entryRequest.getMaterialId());
            }
            if (entryRequest.getQty() == null || entryRequest.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("数量必须大于0");
            }
            BigDecimal taxRate = entryRequest.getTaxRate() == null ? BigDecimal.ZERO : entryRequest.getTaxRate();
            PaInstockEntry entry = new PaInstockEntry();
            entry.setInstockId(instockId);
            entry.setOrderEntryId(entryRequest.getOrderEntryId());
            entry.setMaterialId(entryRequest.getMaterialId());
            entry.setQty(entryRequest.getQty());
            entry.setPrice(entryRequest.getPrice() == null ? BigDecimal.ZERO : entryRequest.getPrice());
            entry.setTaxRate(taxRate);
            entry.setAmount(calcAmount(entry.getQty(), entry.getPrice(), taxRate));
            entry.setBatchNo(StringUtils.hasText(entryRequest.getBatchNo()) ? entryRequest.getBatchNo() : "");
            entry.setRemark(entryRequest.getRemark());
            instockEntryMapper.insert(entry);
            totalAmount = totalAmount.add(entry.getAmount());
        }
        return totalAmount;
    }

    private BigDecimal calcAmount(BigDecimal qty, BigDecimal price, BigDecimal taxRate) {
        BigDecimal gross = BigDecimal.ONE.add(taxRate.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP));
        return qty.multiply(price).multiply(gross).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<Long, MdMaterial> loadMaterials(List<PaInstockEntry> entries) {
        List<Long> materialIds = entries.stream().map(PaInstockEntry::getMaterialId).distinct().collect(Collectors.toList());
        return materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery().in(MdMaterial::getId, materialIds))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
    }

    private List<PaInstockEntryVO> loadEntries(Long instockId) {
        List<PaInstockEntry> entries = instockEntryMapper.selectList(Wrappers.<PaInstockEntry>lambdaQuery()
                .eq(PaInstockEntry::getInstockId, instockId)
                .orderByAsc(PaInstockEntry::getId));
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, MdMaterial> materialMap = loadMaterials(entries);
        return entries.stream().map(entry -> {
            PaInstockEntryVO vo = new PaInstockEntryVO();
            BeanUtils.copyProperties(entry, vo);
            MdMaterial material = materialMap.get(entry.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getMaterialCode());
                vo.setMaterialName(material.getMaterialName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private String nextInstockNo(LocalDate instockDate) {
        String prefix = INSTOCK_NO_PREFIX + instockDate.format(MONTH_FORMATTER) + "-";
        String lastNo = baseMapper.selectLastInstockNo(prefix);
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

    private PaInstockListVO toListVO(PaInstock instock) {
        PaInstockListVO vo = new PaInstockListVO();
        BeanUtils.copyProperties(instock, vo);
        MdSupplier supplier = supplierMapper.selectById(instock.getSupplierId());
        if (supplier != null) {
            vo.setSupplierName(supplier.getSupplierName());
        }
        MdWarehouse warehouse = warehouseMapper.selectById(instock.getWarehouseId());
        if (warehouse != null) {
            vo.setWarehouseName(warehouse.getWarehouseName());
        }
        if (instock.getOrderId() != null) {
            PaOrder order = orderMapper.selectById(instock.getOrderId());
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
            }
        }
        return vo;
    }
}








