package com.erp.ap.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.ap.domain.dto.ApPayableQueryRequest;
import com.erp.ap.domain.entity.ApPayable;
import com.erp.ap.domain.vo.ApPayableVO;
import com.erp.ap.mapper.ApPayableMapper;
import com.erp.ap.service.ApPayableService;
import com.erp.md.domain.entity.MdSupplier;
import com.erp.md.mapper.MdSupplierMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApPayableServiceImpl extends ServiceImpl<ApPayableMapper, ApPayable> implements ApPayableService {

    private static final String NO_PREFIX = "AP-";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String SOURCE_TYPE_INSTOCK = "PA_INSTOCK";
    private static final int CREDIT_DAYS = 30;

    private final MdSupplierMapper supplierMapper;

    @Override
    public PageResult<ApPayableVO> pagePayable(ApPayableQueryRequest request) {
        Page<ApPayable> page = this.page(request.toPage(), Wrappers.<ApPayable>lambdaQuery()
                .eq(request.getSupplierId() != null, ApPayable::getSupplierId, request.getSupplierId())
                .eq(request.getStatus() != null, ApPayable::getStatus, request.getStatus())
                .like(StringUtils.hasText(request.getSourceNo()), ApPayable::getSourceNo, request.getSourceNo())
                .orderByDesc(ApPayable::getId));
        Map<Long, MdSupplier> supplierMap = loadSuppliers(page.getRecords());
        List<ApPayableVO> records = page.getRecords().stream()
                .map(payable -> toVO(payable, supplierMap))
                .collect(Collectors.toList());
        Page<ApPayableVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public ApPayableVO getPayableDetail(Long id) {
        ApPayable payable = this.getById(id);
        if (payable == null) {
            throw new BizException("应付单不存在");
        }
        return toVO(payable, loadSuppliers(Collections.singletonList(payable)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFromInstock(Long supplierId, Long sourceId, String sourceNo, BigDecimal amount, LocalDate bizDate) {
        ApPayable payable = new ApPayable();
        payable.setPayableNo(nextNo(bizDate));
        payable.setSupplierId(supplierId);
        payable.setSourceType(SOURCE_TYPE_INSTOCK);
        payable.setSourceId(sourceId);
        payable.setSourceNo(sourceNo);
        payable.setAmount(amount);
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setStatus(ApPayable.STATUS_OPEN);
        payable.setDueDate(bizDate.plusDays(CREDIT_DAYS));
        this.save(payable);
    }

    /**
     * 冲销应付。
     * <p>
     * 采购退货时按来源入库单找到对应应付单，冲减其已付金额。
     * 若应付已被部分付款，只冲减剩余部分；若已全额付清则不再冲减——
     * 已付的钱需要通过退款单退回，而不是凭空减少应付。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal reverseBySource(String sourceType, Long sourceId, BigDecimal amount) {
        if (sourceId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        List<ApPayable> payables = this.list(Wrappers.<ApPayable>lambdaQuery()
                .eq(ApPayable::getSourceType, sourceType)
                .eq(ApPayable::getSourceId, sourceId)
                .eq(ApPayable::getStatus, ApPayable.STATUS_OPEN));
        BigDecimal reversed = BigDecimal.ZERO;
        for (ApPayable payable : payables) {
            BigDecimal paid = payable.getPaidAmount() == null
                    ? BigDecimal.ZERO : payable.getPaidAmount();
            BigDecimal remaining = payable.getAmount().subtract(paid);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal current = amount.subtract(reversed);
            if (current.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal reverseAmount = current.min(remaining);
            BigDecimal newPaid = paid.add(reverseAmount);
            ApPayable update = new ApPayable();
            update.setId(payable.getId());
            update.setPaidAmount(newPaid);
            if (newPaid.compareTo(payable.getAmount()) >= 0) {
                update.setStatus(ApPayable.STATUS_CLEARED);
            }
            this.updateById(update);
            reversed = reversed.add(reverseAmount);
        }
        return reversed;
    }

    private Map<Long, MdSupplier> loadSuppliers(List<ApPayable> payables) {
        List<Long> supplierIds = payables.stream().map(ApPayable::getSupplierId).distinct().collect(Collectors.toList());
        if (supplierIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return supplierMapper.selectList(Wrappers.<MdSupplier>lambdaQuery().in(MdSupplier::getId, supplierIds))
                .stream()
                .collect(Collectors.toMap(MdSupplier::getId, Function.identity()));
    }

    private ApPayableVO toVO(ApPayable payable, Map<Long, MdSupplier> supplierMap) {
        ApPayableVO vo = new ApPayableVO();
        BeanUtils.copyProperties(payable, vo);
        MdSupplier supplier = supplierMap.get(payable.getSupplierId());
        if (supplier != null) {
            vo.setSupplierName(supplier.getSupplierName());
        }
        vo.setUnpaidAmount(payable.getAmount().subtract(
                payable.getPaidAmount() == null ? BigDecimal.ZERO : payable.getPaidAmount()));
        return vo;
    }

    private String nextNo(LocalDate date) {
        String prefix = NO_PREFIX + date.format(MONTH_FORMATTER) + "-";
        return prefix + nextSeq(baseMapper.selectLastPayableNo(prefix));
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
}








