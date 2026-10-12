package com.erp.sa.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.wf.integration.ApprovalGate;
import com.erp.wf.integration.SAOrderApprovalListener;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.md.domain.entity.MdCustomer;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.mapper.MdCustomerMapper;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.sa.domain.dto.SaOrderCloseRequest;
import com.erp.sa.domain.dto.SaOrderEntryRequest;
import com.erp.sa.domain.dto.SaOrderQueryRequest;
import com.erp.sa.domain.dto.SaOrderSaveRequest;
import com.erp.sa.domain.entity.SaOrder;
import com.erp.sa.domain.entity.SaOrderEntry;
import com.erp.sa.domain.vo.SaOrderDetailVO;
import com.erp.sa.domain.vo.SaOrderEntryVO;
import com.erp.sa.domain.vo.SaOrderListVO;
import com.erp.sa.mapper.SaOrderEntryMapper;
import com.erp.sa.mapper.SaOrderMapper;
import com.erp.sa.service.SaOrderService;
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
public class SaOrderServiceImpl extends ServiceImpl<SaOrderMapper, SaOrder> implements SaOrderService {

    private static final String ORDER_NO_PREFIX = "XS-";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final SaOrderEntryMapper orderEntryMapper;
    private final MdCustomerMapper customerMapper;
    private final MdMaterialMapper materialMapper;

    private final ApprovalGate approvalGate;

    @Override
    public PageResult<SaOrderListVO> pageOrder(SaOrderQueryRequest request) {
        Page<SaOrder> page = this.page(request.toPage(), Wrappers.<SaOrder>lambdaQuery()
                .like(StringUtils.hasText(request.getOrderNo()), SaOrder::getOrderNo, request.getOrderNo())
                .eq(request.getCustomerId() != null, SaOrder::getCustomerId, request.getCustomerId())
                .eq(request.getStatus() != null, SaOrder::getStatus, request.getStatus())
                .ge(request.getStartDate() != null, SaOrder::getOrderDate, request.getStartDate())
                .le(request.getEndDate() != null, SaOrder::getOrderDate, request.getEndDate())
                .orderByDesc(SaOrder::getOrderDate)
                .orderByDesc(SaOrder::getId));
        Map<Long, MdCustomer> customerMap = loadCustomers(page.getRecords());
        List<SaOrderListVO> records = page.getRecords().stream()
                .map(order -> toListVO(order, customerMap))
                .collect(Collectors.toList());
        Page<SaOrderListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public SaOrderDetailVO getOrderDetail(Long id) {
        SaOrder order = this.getById(id);
        if (order == null) {
            throw new BizException("销售订单不存在");
        }
        SaOrderDetailVO detail = new SaOrderDetailVO();
        BeanUtils.copyProperties(order, detail);
        detail.setEntries(loadEntries(id));
        MdCustomer customer = customerMapper.selectById(order.getCustomerId());
        if (customer != null) {
            detail.setCustomerName(customer.getCustomerName());
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(SaOrderSaveRequest request) {
        MdCustomer customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null) {
            throw new BizException("客户不存在");
        }
        if (customer.getStatus() != null && customer.getStatus() != 0) {
            throw new BizException("客户已停用");
        }
        Map<Long, MdMaterial> materialMap = validateEntries(request.getEntries());
        SaOrder order = new SaOrder();
        order.setOrderNo(nextOrderNo(request.getOrderDate()));
        order.setCustomerId(request.getCustomerId());
        order.setDeptId(request.getDeptId());
        order.setOrderDate(request.getOrderDate());
        order.setDeliveryDate(request.getDeliveryDate());
        order.setStatus(SaOrder.STATUS_DRAFT);
        order.setRemark(request.getRemark());
        this.save(order);
        BigDecimal totalAmount = saveEntries(order.getId(), request.getEntries());
        order.setTotalAmount(totalAmount);
        this.updateById(order);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(SaOrderSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        SaOrder exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("销售订单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != SaOrder.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的订单才能修改");
        }
        validateEntries(request.getEntries());
        SaOrder order = new SaOrder();
        order.setId(request.getId());
        order.setCustomerId(request.getCustomerId());
        order.setDeptId(request.getDeptId());
        order.setOrderDate(request.getOrderDate());
        order.setDeliveryDate(request.getDeliveryDate());
        order.setRemark(request.getRemark());
        this.updateById(order);
        orderEntryMapper.delete(Wrappers.<SaOrderEntry>lambdaQuery().eq(SaOrderEntry::getOrderId, request.getId()));
        BigDecimal totalAmount = saveEntries(request.getId(), request.getEntries());
        SaOrder totalUpdate = new SaOrder();
        totalUpdate.setId(request.getId());
        totalUpdate.setTotalAmount(totalAmount);
        this.updateById(totalUpdate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        SaOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException("销售订单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != SaOrder.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的订单才能删除");
        }
        this.removeById(id);
        orderEntryMapper.delete(Wrappers.<SaOrderEntry>lambdaQuery().eq(SaOrderEntry::getOrderId, id));
    }

    @Override
    public void auditOrder(Long id) {
        SaOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException("销售订单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != SaOrder.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的订单才能审核");
        }

        // 若该业务类型配置了审批流，审核动作改由审批通过后回调触发
        if (approvalGate.tryStart(SAOrderApprovalListener.BIZ_TYPE, id,
                "销售订单" + exist.getOrderNo(), exist.getTotalAmount(), null)) {
            return;
        }
        SaOrder update = new SaOrder();
        update.setId(id);
        update.setStatus(SaOrder.STATUS_AUDITED);
        update.setAuditBy(SecurityUtils.getCurrentUserId());
        update.setAuditTime(LocalDateTime.now());
        this.updateById(update);
    }

    @Override
    public void closeOrder(Long id, SaOrderCloseRequest request) {
        SaOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException("销售订单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != SaOrder.STATUS_AUDITED) {
            throw new BizException("只有已审核状态的订单才能关闭");
        }
        SaOrder update = new SaOrder();
        update.setId(id);
        update.setStatus(SaOrder.STATUS_CLOSED);
        update.setCloseReason(request == null ? null : request.getCloseReason());
        this.updateById(update);
    }

    private List<SaOrderEntryVO> loadEntries(Long orderId) {
        List<SaOrderEntry> entries = orderEntryMapper.selectList(Wrappers.<SaOrderEntry>lambdaQuery()
                .eq(SaOrderEntry::getOrderId, orderId)
                .orderByAsc(SaOrderEntry::getId));
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> materialIds = entries.stream().map(SaOrderEntry::getMaterialId).distinct().collect(Collectors.toList());
        Map<Long, MdMaterial> materialMap = materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery()
                        .in(MdMaterial::getId, materialIds))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
        return entries.stream().map(entry -> {
            SaOrderEntryVO vo = new SaOrderEntryVO();
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

    private Map<Long, MdMaterial> validateEntries(List<SaOrderEntryRequest> entryRequests) {
        if (entryRequests == null || entryRequests.isEmpty()) {
            throw new BizException("订单明细不能为空");
        }
        List<Long> materialIds = entryRequests.stream()
                .map(SaOrderEntryRequest::getMaterialId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, MdMaterial> materialMap = materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery()
                        .in(MdMaterial::getId, materialIds))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
        for (SaOrderEntryRequest entryRequest : entryRequests) {
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

    private BigDecimal saveEntries(Long orderId, List<SaOrderEntryRequest> entryRequests) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (SaOrderEntryRequest entryRequest : entryRequests) {
            SaOrderEntry entry = new SaOrderEntry();
            entry.setOrderId(orderId);
            entry.setMaterialId(entryRequest.getMaterialId());
            entry.setQty(entryRequest.getQty());
            entry.setPrice(entryRequest.getPrice());
            entry.setTaxRate(entryRequest.getTaxRate() == null ? BigDecimal.ZERO : entryRequest.getTaxRate());
            entry.setAmount(calcAmount(entryRequest.getQty(), entryRequest.getPrice(), entry.getTaxRate()));
            entry.setDeliveredQty(BigDecimal.ZERO);
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
        String prefix = ORDER_NO_PREFIX + orderDate.format(MONTH_FORMATTER) + "-";
        String lastNo = baseMapper.selectLastOrderNo(prefix);
        return prefix + nextSeq(lastNo);
    }

    private String nextSeq(String lastNo) {
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
        return String.format("%04d", next);
    }

    private Map<Long, MdCustomer> loadCustomers(List<SaOrder> orders) {
        List<Long> customerIds = orders.stream().map(SaOrder::getCustomerId).distinct().collect(Collectors.toList());
        if (customerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return customerMapper.selectList(Wrappers.<MdCustomer>lambdaQuery().in(MdCustomer::getId, customerIds))
                .stream()
                .collect(Collectors.toMap(MdCustomer::getId, Function.identity()));
    }

    private SaOrderListVO toListVO(SaOrder order, Map<Long, MdCustomer> customerMap) {
        SaOrderListVO vo = new SaOrderListVO();
        BeanUtils.copyProperties(order, vo);
        MdCustomer customer = customerMap.get(order.getCustomerId());
        if (customer != null) {
            vo.setCustomerName(customer.getCustomerName());
        }
        return vo;
    }
}








