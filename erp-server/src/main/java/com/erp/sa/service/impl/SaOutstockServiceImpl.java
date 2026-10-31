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
import com.erp.sa.domain.dto.SaOutstockEntryRequest;
import com.erp.sa.domain.dto.SaOutstockQueryRequest;
import com.erp.sa.domain.dto.SaOutstockSaveRequest;
import com.erp.sa.domain.entity.SaOrder;
import com.erp.sa.domain.entity.SaOrderEntry;
import com.erp.sa.domain.entity.SaOutstock;
import com.erp.sa.domain.entity.SaOutstockEntry;
import com.erp.sa.domain.vo.SaOutstockDetailVO;
import com.erp.sa.domain.vo.SaOutstockEntryVO;
import com.erp.sa.domain.vo.SaOutstockListVO;
import com.erp.sa.mapper.SaOrderEntryMapper;
import com.erp.sa.mapper.SaOrderMapper;
import com.erp.sa.mapper.SaOutstockEntryMapper;
import com.erp.sa.mapper.SaOutstockMapper;
import com.erp.sa.service.SaOutstockService;
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
public class SaOutstockServiceImpl extends ServiceImpl<SaOutstockMapper, SaOutstock> implements SaOutstockService {

    private static final String OUTSTOCK_NO_PREFIX = "XC-";
    private static final String TRAN_TYPE = "SA_OUTSTOCK";
    private static final String VOUCHER_BIZ_TYPE = "SA_OUTSTOCK";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final SaOutstockEntryMapper outstockEntryMapper;

    private final DataScopeHelper dataScopeHelper;
    private final SaOrderMapper orderMapper;
    private final SaOrderEntryMapper orderEntryMapper;
    private final MdCustomerMapper customerMapper;
    private final MdWarehouseMapper warehouseMapper;
    private final MdMaterialMapper materialMapper;
    private final IcStockService stockService;
    private final FiVoucherService voucherService;
    private final ArReceivableService arReceivableService;
    /** 业务单号生成器（原子自增，避免并发撞号） */
    private final SerialNoGenerator serialNoGenerator;

    @Override
    public PageResult<SaOutstockListVO> pageOutstock(SaOutstockQueryRequest request) {
        Page<SaOutstock> page = this.page(request.toPage(), Wrappers.<SaOutstock>lambdaQuery()
                .like(StringUtils.hasText(request.getOutstockNo()), SaOutstock::getOutstockNo, request.getOutstockNo())
                .eq(request.getOrderId() != null, SaOutstock::getOrderId, request.getOrderId())
                .eq(request.getCustomerId() != null, SaOutstock::getCustomerId, request.getCustomerId())
                .eq(request.getWarehouseId() != null, SaOutstock::getWarehouseId, request.getWarehouseId())
                .eq(request.getStatus() != null, SaOutstock::getStatus, request.getStatus())
                .eq(request.getVoucherStatus() != null, SaOutstock::getVoucherStatus, request.getVoucherStatus())
                .ge(request.getStartDate() != null, SaOutstock::getOutstockDate, request.getStartDate())
                .le(request.getEndDate() != null, SaOutstock::getOutstockDate, request.getEndDate())
                .orderByDesc(SaOutstock::getOutstockDate)
                .orderByDesc(SaOutstock::getId));
        List<SaOutstockListVO> records = page.getRecords().stream().map(this::toListVO).collect(Collectors.toList());
        Page<SaOutstockListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public SaOutstockDetailVO getOutstockDetail(Long id) {
        SaOutstock outstock = this.getById(id);
        if (outstock == null) {
            throw new BizException("销售出库单不存在");
        }
        SaOutstockDetailVO detail = new SaOutstockDetailVO();
        BeanUtils.copyProperties(outstock, detail);
        detail.setEntries(loadEntries(id));
        if (outstock.getOrderId() != null) {
            SaOrder order = orderMapper.selectById(outstock.getOrderId());
            if (order != null) {
                detail.setOrderNo(order.getOrderNo());
            }
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOutstock(SaOutstockSaveRequest request) {
        MdCustomer customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null) {
            throw new BizException("客户不存在");
        }
        MdWarehouse warehouse = warehouseMapper.selectById(request.getWarehouseId());
        if (warehouse == null) {
            throw new BizException("仓库不存在");
        }
        SaOrder order = null;
        if (request.getOrderId() != null) {
            order = orderMapper.selectById(request.getOrderId());
            if (order == null) {
                throw new BizException("销售订单不存在");
            }
            if (order.getStatus() == null || order.getStatus() != SaOrder.STATUS_AUDITED) {
                throw new BizException("销售订单未审核，不能出库");
            }
            if (!order.getCustomerId().equals(request.getCustomerId())) {
                throw new BizException("出库客户与订单客户不一致");
            }
        }
        validateOrderEntries(request, order);
        SaOutstock outstock = new SaOutstock();
        outstock.setOutstockNo(nextOutstockNo(request.getOutstockDate()));
        outstock.setOrderId(request.getOrderId());
        outstock.setCustomerId(request.getCustomerId());
        outstock.setWarehouseId(request.getWarehouseId());
        outstock.setOutstockDate(request.getOutstockDate());
        // 归属部门供数据权限过滤：优先继承来源单据，其次取当前用户部门
        outstock.setDeptId(dataScopeHelper.resolveDeptId(null, null));
        outstock.setStatus(SaOutstock.STATUS_DRAFT);
        outstock.setVoucherStatus(SaOutstock.VOUCHER_STATUS_NONE);
        outstock.setRemark(request.getRemark());
        this.save(outstock);
        BigDecimal[] totals = saveEntries(outstock.getId(), request.getEntries());
        outstock.setTotalAmount(totals[0]);
        outstock.setTotalCost(totals[1]);
        this.updateById(outstock);
        return outstock.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOutstock(SaOutstockSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        SaOutstock exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("销售出库单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != SaOutstock.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的出库单才能修改");
        }
        MdWarehouse warehouse = warehouseMapper.selectById(request.getWarehouseId());
        if (warehouse == null) {
            throw new BizException("仓库不存在");
        }
        SaOutstock outstock = new SaOutstock();
        outstock.setId(request.getId());
        outstock.setOrderId(request.getOrderId());
        outstock.setCustomerId(request.getCustomerId());
        outstock.setWarehouseId(request.getWarehouseId());
        outstock.setOutstockDate(request.getOutstockDate());
        outstock.setRemark(request.getRemark());
        this.updateById(outstock);
        outstockEntryMapper.delete(Wrappers.<SaOutstockEntry>lambdaQuery().eq(SaOutstockEntry::getOutstockId, request.getId()));
        BigDecimal[] totals = saveEntries(request.getId(), request.getEntries());
        SaOutstock totalUpdate = new SaOutstock();
        totalUpdate.setId(request.getId());
        totalUpdate.setTotalAmount(totals[0]);
        totalUpdate.setTotalCost(totals[1]);
        this.updateById(totalUpdate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOutstock(Long id) {
        SaOutstock exist = this.getById(id);
        if (exist == null) {
            throw new BizException("销售出库单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != SaOutstock.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的出库单才能删除");
        }
        this.removeById(id);
        outstockEntryMapper.delete(Wrappers.<SaOutstockEntry>lambdaQuery().eq(SaOutstockEntry::getOutstockId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditOutstock(Long id) {
        SaOutstock outstock = this.getById(id);
        if (outstock == null) {
            throw new BizException("销售出库单不存在");
        }
        if (outstock.getStatus() == null || outstock.getStatus() != SaOutstock.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的出库单才能审核");
        }
        List<SaOutstockEntry> entries = outstockEntryMapper.selectList(Wrappers.<SaOutstockEntry>lambdaQuery()
                .eq(SaOutstockEntry::getOutstockId, id)
                .orderByAsc(SaOutstockEntry::getId));
        if (entries.isEmpty()) {
            throw new BizException("出库单明细为空");
        }

        // 审核时重新校验订单可出库数量，防止暂存后多人建单导致超发
        checkDeliverableQty(entries);

        Map<Long, MdMaterial> materialMap = loadMaterials(entries);
        List<VoucherLine> voucherLines = new ArrayList<>();
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalNetAmount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (SaOutstockEntry entry : entries) {
            MdMaterial material = materialMap.get(entry.getMaterialId());
            String materialName = material == null ? String.valueOf(entry.getMaterialId()) : material.getMaterialName();

            // 成本按审核时的最新加权平均价重算，避免用暂存时的过期成本结转
            BigDecimal costPrice = resolveCostPrice(entry, material);
            BigDecimal costAmount = costPrice.multiply(entry.getQty())
                    .setScale(2, RoundingMode.HALF_UP);
            updateEntryCost(entry.getId(), costPrice, costAmount);

            stockService.decreaseStock(entry.getMaterialId(), outstock.getWarehouseId(), entry.getBatchNo(),
                    entry.getQty(), TRAN_TYPE, outstock.getId(), outstock.getOutstockNo(), outstock.getOutstockDate(),
                    "销售出库");
            if (entry.getOrderEntryId() != null) {
                orderEntryMapper.addDeliveredQty(entry.getOrderEntryId(), entry.getQty());
            }

            // 收入端：价税分离
            BigDecimal[] taxSplit = TaxAmountCalculator.split(entry.getQty(), entry.getPrice(), entry.getTaxRate());
            BigDecimal netAmount = taxSplit[0];
            BigDecimal taxAmount = taxSplit[1];

            voucherLines.add(VoucherLine.builder()
                    .direction(FiVoucherEntry.DIRECTION_CREDIT)
                    .ruleKey("REVENUE")
                    .amount(netAmount)
                    .summary("销售收入：" + materialName)
                    .build());
            if (taxAmount.compareTo(BigDecimal.ZERO) > 0) {
                voucherLines.add(VoucherLine.builder()
                        .direction(FiVoucherEntry.DIRECTION_CREDIT)
                        .ruleKey("TAX")
                        .amount(taxAmount)
                        .summary("销项税额：" + materialName)
                        .build());
            }
            voucherLines.add(VoucherLine.builder()
                    .direction(FiVoucherEntry.DIRECTION_DEBIT)
                    .ruleKey("COST")
                    .amount(costAmount)
                    .summary("销售成本：" + materialName)
                    .build());

            totalNetAmount = totalNetAmount.add(netAmount);
            totalTax = totalTax.add(taxAmount);
            totalCost = totalCost.add(costAmount);
        }

        // 应收按价税合计确认
        BigDecimal totalAmount = totalNetAmount.add(totalTax);
        voucherLines.add(VoucherLine.builder()
                .direction(FiVoucherEntry.DIRECTION_DEBIT)
                .ruleKey("RECEIVABLE")
                .amount(totalAmount)
                .summary("应收：" + outstock.getOutstockNo())
                .auxType(FiVoucherEntry.AUX_CUSTOMER)
                .auxId(outstock.getCustomerId())
                .build());
        voucherLines.add(VoucherLine.builder()
                .direction(FiVoucherEntry.DIRECTION_CREDIT)
                .ruleKey("STOCK")
                .amount(totalCost)
                .summary("销售出库转成本")
                .build());

        Long voucherId = voucherService.generateBizVoucher(VOUCHER_BIZ_TYPE, outstock.getOutstockDate(),
                "销售出库 " + outstock.getOutstockNo(), voucherLines);
        arReceivableService.createFromOutstock(outstock.getCustomerId(), outstock.getId(),
                outstock.getOutstockNo(), totalAmount, outstock.getOutstockDate(), outstock.getDeptId());
        refreshOrderStatus(outstock.getOrderId());
        SaOutstock update = new SaOutstock();
        update.setId(id);
        update.setStatus(SaOutstock.STATUS_AUDITED);
        update.setAuditBy(SecurityUtils.getCurrentUserId());
        update.setAuditTime(LocalDateTime.now());
        update.setVoucherStatus(SaOutstock.VOUCHER_STATUS_DONE);
        update.setVoucherId(voucherId);
        update.setTotalCost(totalCost);
        update.setTotalAmount(totalAmount);
        update.setNetAmount(totalNetAmount);
        update.setTaxAmount(totalTax);
        this.updateById(update);
    }

    /**
     * 审核时校验订单可出库数量。
     * <p>
     * 暂存时的校验不足以防超发——多张出库单可以先后通过暂存校验，
     * 只有在审核环节按最新已出库数量复核才能真正拦住超发。
     */
    private void checkDeliverableQty(List<SaOutstockEntry> entries) {
        List<Long> orderEntryIds = entries.stream()
                .map(SaOutstockEntry::getOrderEntryId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (orderEntryIds.isEmpty()) {
            return;
        }
        Map<Long, SaOrderEntry> orderEntryMap = orderEntryMapper.selectBatchIds(orderEntryIds).stream()
                .collect(Collectors.toMap(SaOrderEntry::getId, Function.identity()));

        // 汇总本次出库单中同一订单行的数量
        Map<Long, BigDecimal> currentQtyMap = new HashMap<>();
        for (SaOutstockEntry entry : entries) {
            if (entry.getOrderEntryId() == null) {
                continue;
            }
            currentQtyMap.merge(entry.getOrderEntryId(), entry.getQty(), BigDecimal::add);
        }
        for (Map.Entry<Long, BigDecimal> item : currentQtyMap.entrySet()) {
            SaOrderEntry orderEntry = orderEntryMap.get(item.getKey());
            if (orderEntry == null) {
                throw new BizException("订单明细不存在：" + item.getKey());
            }
            BigDecimal delivered = orderEntry.getDeliveredQty() == null
                    ? BigDecimal.ZERO : orderEntry.getDeliveredQty();
            BigDecimal remaining = orderEntry.getQty().subtract(delivered);
            if (item.getValue().compareTo(remaining) > 0) {
                throw new BizException("出库数量超过订单可出库数量，物料：" + orderEntry.getMaterialId());
            }
        }
    }

    /**
     * 解析出库成本价。优先使用出库明细上已固化的成本，无则取物料当前加权平均价。
     */
    private BigDecimal resolveCostPrice(SaOutstockEntry entry, MdMaterial material) {
        if (material != null && material.getCostPrice() != null
                && material.getCostPrice().compareTo(BigDecimal.ZERO) > 0) {
            return material.getCostPrice();
        }
        return entry.getCostPrice() == null ? BigDecimal.ZERO : entry.getCostPrice();
    }

    private void updateEntryCost(Long entryId, BigDecimal costPrice, BigDecimal costAmount) {
        SaOutstockEntry update = new SaOutstockEntry();
        update.setId(entryId);
        update.setCostPrice(costPrice);
        update.setCostAmount(costAmount);
        outstockEntryMapper.updateById(update);
    }

    /**
     * 刷新订单状态。
     * <p>
     * 已关闭的订单不能被出库动作改回「已完成」——关闭是业务的明确终止意图，
     * 出库只是执行历史数据，不应推翻该意图。
     */
    private void refreshOrderStatus(Long orderId) {
        if (orderId == null) {
            return;
        }
        SaOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        if (order.getStatus() != null && order.getStatus() == SaOrder.STATUS_AUDITED) {
            return;
        }
        Long pendingCount = orderEntryMapper.selectCount(Wrappers.<SaOrderEntry>lambdaQuery()
                .eq(SaOrderEntry::getOrderId, orderId)
                .apply("qty > delivered_qty"));
        if (pendingCount != null && pendingCount > 0) {
            return;
        }
        SaOrder update = new SaOrder();
        update.setId(orderId);
        update.setStatus(SaOrder.STATUS_COMPLETED);
        orderMapper.updateById(update);
    }

    private void validateOrderEntries(SaOutstockSaveRequest request, SaOrder order) {
        if (order == null && request.getOrderId() == null) {
            return;
        }
        SaOrder effectiveOrder = order;
        if (effectiveOrder == null) {
            effectiveOrder = orderMapper.selectById(request.getOrderId());
            if (effectiveOrder == null) {
                throw new BizException("销售订单不存在");
            }
        }
        List<SaOrderEntry> orderEntries = orderEntryMapper.selectList(Wrappers.<SaOrderEntry>lambdaQuery()
                .eq(SaOrderEntry::getOrderId, effectiveOrder.getId()));
        Map<Long, SaOrderEntry> map = orderEntries.stream()
                .collect(Collectors.toMap(SaOrderEntry::getId, Function.identity()));
        for (SaOutstockEntryRequest entryRequest : request.getEntries()) {
            if (entryRequest.getOrderEntryId() == null) {
                continue;
            }
            SaOrderEntry orderEntry = map.get(entryRequest.getOrderEntryId());
            if (orderEntry == null) {
                throw new BizException("订单明细不存在：" + entryRequest.getOrderEntryId());
            }
            if (!orderEntry.getMaterialId().equals(entryRequest.getMaterialId())) {
                throw new BizException("出库物料与订单明细物料不一致");
            }
            BigDecimal remaining = orderEntry.getQty().subtract(
                    orderEntry.getDeliveredQty() == null ? BigDecimal.ZERO : orderEntry.getDeliveredQty());
            if (entryRequest.getQty().compareTo(remaining) > 0) {
                throw new BizException("出库数量超过订单可出库数量，物料：" + entryRequest.getMaterialId());
            }
        }
    }

    private BigDecimal[] saveEntries(Long outstockId, List<SaOutstockEntryRequest> entryRequests) {
        if (entryRequests == null || entryRequests.isEmpty()) {
            throw new BizException("出库明细不能为空");
        }
        List<Long> materialIds = entryRequests.stream()
                .map(SaOutstockEntryRequest::getMaterialId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, MdMaterial> materialMap = materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery()
                        .in(MdMaterial::getId, materialIds))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        for (SaOutstockEntryRequest entryRequest : entryRequests) {
            MdMaterial material = materialMap.get(entryRequest.getMaterialId());
            if (material == null) {
                throw new BizException("物料不存在：" + entryRequest.getMaterialId());
            }
            if (entryRequest.getQty() == null || entryRequest.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("数量必须大于0");
            }
            String batchNo = StringUtils.hasText(entryRequest.getBatchNo()) ? entryRequest.getBatchNo() : "";
            if (material.getTrackBatch() != null && material.getTrackBatch() == 1 && !StringUtils.hasText(batchNo)) {
                throw new BizException("批次管理物料必须填写批次号：" + material.getMaterialCode());
            }
            BigDecimal taxRate = entryRequest.getTaxRate() == null ? BigDecimal.ZERO : entryRequest.getTaxRate();
            BigDecimal costPrice = material.getCostPrice() == null ? BigDecimal.ZERO : material.getCostPrice();
            SaOutstockEntry entry = new SaOutstockEntry();
            entry.setOutstockId(outstockId);
            entry.setOrderEntryId(entryRequest.getOrderEntryId());
            entry.setMaterialId(entryRequest.getMaterialId());
            entry.setQty(entryRequest.getQty());
            entry.setPrice(entryRequest.getPrice() == null ? BigDecimal.ZERO : entryRequest.getPrice());
            entry.setTaxRate(taxRate);
            entry.setAmount(calcAmount(entry.getQty(), entry.getPrice(), taxRate));
            entry.setCostPrice(costPrice);
            entry.setCostAmount(costPrice.multiply(entry.getQty()).setScale(2, RoundingMode.HALF_UP));
            entry.setBatchNo(batchNo);
            entry.setRemark(entryRequest.getRemark());
            outstockEntryMapper.insert(entry);
            totalAmount = totalAmount.add(entry.getAmount());
            totalCost = totalCost.add(entry.getCostAmount());
        }
        return new BigDecimal[]{totalAmount, totalCost};
    }

    private BigDecimal calcAmount(BigDecimal qty, BigDecimal price, BigDecimal taxRate) {
        BigDecimal gross = BigDecimal.ONE.add(taxRate.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP));
        return qty.multiply(price).multiply(gross).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<Long, MdMaterial> loadMaterials(List<SaOutstockEntry> entries) {
        List<Long> materialIds = entries.stream().map(SaOutstockEntry::getMaterialId).distinct().collect(Collectors.toList());
        return materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery().in(MdMaterial::getId, materialIds))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
    }

    private List<SaOutstockEntryVO> loadEntries(Long outstockId) {
        List<SaOutstockEntry> entries = outstockEntryMapper.selectList(Wrappers.<SaOutstockEntry>lambdaQuery()
                .eq(SaOutstockEntry::getOutstockId, outstockId)
                .orderByAsc(SaOutstockEntry::getId));
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, MdMaterial> materialMap = loadMaterials(entries);
        return entries.stream().map(entry -> {
            SaOutstockEntryVO vo = new SaOutstockEntryVO();
            BeanUtils.copyProperties(entry, vo);
            MdMaterial material = materialMap.get(entry.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getMaterialCode());
                vo.setMaterialName(material.getMaterialName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private String nextOutstockNo(LocalDate outstockDate) {
        // 序号取自流水表原子自增：并发不撞号，软删除不重用旧号
        return serialNoGenerator.next("XC", outstockDate.format(MONTH_FORMATTER));
    }

    private SaOutstockListVO toListVO(SaOutstock outstock) {
        SaOutstockListVO vo = new SaOutstockListVO();
        BeanUtils.copyProperties(outstock, vo);
        MdCustomer customer = customerMapper.selectById(outstock.getCustomerId());
        if (customer != null) {
            vo.setCustomerName(customer.getCustomerName());
        }
        MdWarehouse warehouse = warehouseMapper.selectById(outstock.getWarehouseId());
        if (warehouse != null) {
            vo.setWarehouseName(warehouse.getWarehouseName());
        }
        if (outstock.getOrderId() != null) {
            SaOrder order = orderMapper.selectById(outstock.getOrderId());
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
            }
        }
        return vo;
    }
}








