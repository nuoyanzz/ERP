package com.erp.pa.service.impl;

import com.erp.common.util.SerialNoGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.security.datascope.DataScopeHelper;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.domain.entity.MdSupplier;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.md.mapper.MdSupplierMapper;
import com.erp.pa.domain.dto.PaOrderCloseRequest;
import com.erp.pa.domain.dto.PaOrderEntryRequest;
import com.erp.pa.domain.dto.PaOrderQueryRequest;
import com.erp.pa.domain.dto.PaOrderSaveRequest;
import com.erp.pa.domain.entity.PaOrder;
import com.erp.pa.domain.entity.PaOrderEntry;
import com.erp.pa.domain.vo.PaOrderDetailVO;
import com.erp.pa.domain.vo.PaOrderEntryVO;
import com.erp.pa.domain.vo.PaOrderListVO;
import com.erp.pa.mapper.PaOrderEntryMapper;
import com.erp.pa.mapper.PaOrderMapper;
import com.erp.pa.service.PaOrderService;
import com.erp.wf.integration.ApprovalGate;
import com.erp.wf.integration.PAOrderApprovalListener;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaOrderServiceImpl extends ServiceImpl<PaOrderMapper, PaOrder> implements PaOrderService {

    private static final String ORDER_NO_PREFIX = "CG-";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PaOrderEntryMapper orderEntryMapper;

    private final DataScopeHelper dataScopeHelper;
    private final MdSupplierMapper supplierMapper;
    private final MdMaterialMapper materialMapper;

    private final com.erp.wf.integration.ApprovalGate approvalGate;
    /** 业务单号生成器（原子自增，避免并发撞号） */
    private final SerialNoGenerator serialNoGenerator;
    @Override
    public PageResult<PaOrderListVO> pageOrder(PaOrderQueryRequest request) {
        Page<PaOrder> page = this.page(request.toPage(), Wrappers.<PaOrder>lambdaQuery()
                .like(StringUtils.hasText(request.getOrderNo()), PaOrder::getOrderNo, request.getOrderNo())
                .eq(request.getSupplierId() != null, PaOrder::getSupplierId, request.getSupplierId())
                .eq(request.getStatus() != null, PaOrder::getStatus, request.getStatus())
                .ge(request.getStartDate() != null, PaOrder::getOrderDate, request.getStartDate())
                .le(request.getEndDate() != null, PaOrder::getOrderDate, request.getEndDate())
                .orderByDesc(PaOrder::getOrderDate)
                .orderByDesc(PaOrder::getId));
        Map<Long, MdSupplier> supplierMap = loadSuppliers(page.getRecords());
        List<PaOrderListVO> records = page.getRecords().stream()
                .map(order -> toListVO(order, supplierMap))
                .collect(Collectors.toList());
        Page<PaOrderListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public PaOrderDetailVO getOrderDetail(Long id) {
        PaOrder order = this.getById(id);
        if (order == null) {
            throw new BizException("采购订单不存在");
        }
        PaOrderDetailVO detail = new PaOrderDetailVO();
        BeanUtils.copyProperties(order, detail);
        List<PaOrderEntryVO> entries = loadEntries(id);
        detail.setEntries(entries);
        MdSupplier supplier = supplierMapper.selectById(order.getSupplierId());
        if (supplier != null) {
            detail.setSupplierName(supplier.getSupplierName());
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(PaOrderSaveRequest request) {
        MdSupplier supplier = supplierMapper.selectById(request.getSupplierId());
        if (supplier == null) {
            throw new BizException("供应商不存在");
        }
        if (supplier.getStatus() != null && supplier.getStatus() != 0) {
            throw new BizException("供应商已停用");
        }
        Map<Long, MdMaterial> materialMap = validateEntries(request.getEntries());
        PaOrder order = new PaOrder();
        order.setOrderNo(nextOrderNo(request.getOrderDate()));
        order.setSupplierId(request.getSupplierId());
        order.setDeptId(dataScopeHelper.resolveDeptId(request.getDeptId(), null));
        order.setOrderDate(request.getOrderDate());
        order.setDeliveryDate(request.getDeliveryDate());
        order.setStatus(PaOrder.STATUS_DRAFT);
        order.setRemark(request.getRemark());
        this.save(order);
        BigDecimal totalAmount = saveEntries(order.getId(), request.getEntries(), materialMap);
        order.setTotalAmount(totalAmount);
        this.updateById(order);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(PaOrderSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        PaOrder exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("采购订单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != PaOrder.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的订单才能修改");
        }
        MdSupplier supplier = supplierMapper.selectById(request.getSupplierId());
        if (supplier == null) {
            throw new BizException("供应商不存在");
        }
        Map<Long, MdMaterial> materialMap = validateEntries(request.getEntries());
        PaOrder order = new PaOrder();
        order.setId(request.getId());
        order.setSupplierId(request.getSupplierId());
        order.setDeptId(dataScopeHelper.resolveDeptId(request.getDeptId(), null));
        order.setOrderDate(request.getOrderDate());
        order.setDeliveryDate(request.getDeliveryDate());
        order.setRemark(request.getRemark());
        this.updateById(order);
        orderEntryMapper.delete(Wrappers.<PaOrderEntry>lambdaQuery().eq(PaOrderEntry::getOrderId, request.getId()));
        BigDecimal totalAmount = saveEntries(request.getId(), request.getEntries(), materialMap);
        PaOrder totalUpdate = new PaOrder();
        totalUpdate.setId(request.getId());
        totalUpdate.setTotalAmount(totalAmount);
        this.updateById(totalUpdate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        PaOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException("采购订单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != PaOrder.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的订单才能删除");
        }
        this.removeById(id);
        orderEntryMapper.delete(Wrappers.<PaOrderEntry>lambdaQuery().eq(PaOrderEntry::getOrderId, id));
    }

    @Override
    public void auditOrder(Long id) {
        PaOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException("采购订单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != PaOrder.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的订单才能审核");
        }
        // 若该业务类型配置了审批流，审核动作改由审批通过后回调触发
        if (approvalGate.tryStart(PAOrderApprovalListener.BIZ_TYPE, id,
                "采购订单 " + exist.getOrderNo(), exist.getTotalAmount(), null)) {
            return;
        }
        PaOrder update = new PaOrder();
        update.setId(id);
        update.setStatus(PaOrder.STATUS_AUDITED);
        update.setAuditBy(SecurityUtils.getCurrentUserId());
        update.setAuditTime(LocalDateTime.now());
        this.updateById(update);
    }

    @Override
    public void closeOrder(Long id, PaOrderCloseRequest request) {
        PaOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException("采购订单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != PaOrder.STATUS_AUDITED) {
            throw new BizException("只有已审核状态的订单才能关闭");
        }
        PaOrder update = new PaOrder();
        update.setId(id);
        update.setStatus(PaOrder.STATUS_CLOSED);
        update.setCloseReason(request == null ? null : request.getCloseReason());
        this.updateById(update);
    }

    private List<PaOrderEntryVO> loadEntries(Long orderId) {
        List<PaOrderEntry> entries = orderEntryMapper.selectList(Wrappers.<PaOrderEntry>lambdaQuery()
                .eq(PaOrderEntry::getOrderId, orderId)
                .orderByAsc(PaOrderEntry::getId));
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> materialIds = entries.stream().map(PaOrderEntry::getMaterialId).distinct().collect(Collectors.toList());
        Map<Long, MdMaterial> materialMap = materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery()
                        .in(MdMaterial::getId, materialIds))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
        return entries.stream().map(entry -> {
            PaOrderEntryVO vo = new PaOrderEntryVO();
            BeanUtils.copyProperties(entry, vo);
            MdMaterial material = materialMap.get(entry.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getMaterialCode());
                vo.setMaterialName(material.getMaterialName());
                vo.setSpec(material.getSpec());
                vo.setUnit(material.getUnit());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private Map<Long, MdMaterial> validateEntries(List<PaOrderEntryRequest> entryRequests) {
        if (entryRequests == null || entryRequests.isEmpty()) {
            throw new BizException("订单明细不能为空");
        }
        List<Long> materialIds = entryRequests.stream()
                .map(PaOrderEntryRequest::getMaterialId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, MdMaterial> materialMap = materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery()
                        .in(MdMaterial::getId, materialIds))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
        for (PaOrderEntryRequest entryRequest : entryRequests) {
            MdMaterial material = materialMap.get(entryRequest.getMaterialId());
            if (material == null) {
                throw new BizException("物料不存在：" + entryRequest.getMaterialId());
            }
            if (material.getStatus() != null && material.getStatus() != 0) {
                throw new BizException("物料已停用：" + material.getMaterialCode());
            }
            if (entryRequest.getQty() == null || entryRequest.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("数量必须大于0");
            }
            if (entryRequest.getPrice() == null || entryRequest.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("单价不能小于0");
            }
        }
        return materialMap;
    }

    private BigDecimal saveEntries(Long orderId, List<PaOrderEntryRequest> entryRequests, Map<Long, MdMaterial> materialMap) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PaOrderEntryRequest entryRequest : entryRequests) {
            PaOrderEntry entry = new PaOrderEntry();
            entry.setOrderId(orderId);
            entry.setMaterialId(entryRequest.getMaterialId());
            entry.setQty(entryRequest.getQty());
            entry.setPrice(entryRequest.getPrice());
            entry.setTaxRate(entryRequest.getTaxRate() == null ? BigDecimal.ZERO : entryRequest.getTaxRate());
            entry.setAmount(calcAmount(entryRequest.getQty(), entryRequest.getPrice(), entry.getTaxRate()));
            entry.setReceivedQty(BigDecimal.ZERO);
            entry.setRemark(entryRequest.getRemark());
            orderEntryMapper.insert(entry);
            totalAmount = totalAmount.add(entry.getAmount());
        }
        return totalAmount;
    }

    private BigDecimal calcAmount(BigDecimal qty, BigDecimal price, BigDecimal taxRate) {
        BigDecimal gross = BigDecimal.ONE.add(taxRate.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP));
        return qty.multiply(price).multiply(gross).setScale(2, RoundingMode.HALF_UP);
    }

    private String nextOrderNo(LocalDate orderDate) {
        // 序号取自流水表原子自增：并发不撞号，软删除不重用旧号
        return serialNoGenerator.next("CG", orderDate.format(MONTH_FORMATTER));
    }


    private Map<Long, MdSupplier> loadSuppliers(List<PaOrder> orders) {
        List<Long> supplierIds = orders.stream().map(PaOrder::getSupplierId).distinct().collect(Collectors.toList());
        if (supplierIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return supplierMapper.selectList(Wrappers.<MdSupplier>lambdaQuery().in(MdSupplier::getId, supplierIds))
                .stream()
                .collect(Collectors.toMap(MdSupplier::getId, Function.identity()));
    }

    private PaOrderListVO toListVO(PaOrder order, Map<Long, MdSupplier> supplierMap) {
        PaOrderListVO vo = new PaOrderListVO();
        BeanUtils.copyProperties(order, vo);
        MdSupplier supplier = supplierMap.get(order.getSupplierId());
        if (supplier != null) {
            vo.setSupplierName(supplier.getSupplierName());
        }
        return vo;
    }
}








