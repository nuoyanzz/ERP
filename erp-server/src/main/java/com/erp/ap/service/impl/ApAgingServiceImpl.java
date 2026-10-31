package com.erp.ap.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.ap.domain.entity.ApPayable;
import com.erp.ap.domain.vo.ApAgingVO;
import com.erp.ap.mapper.ApPayableMapper;
import com.erp.ap.service.ApAgingService;
import com.erp.md.domain.entity.MdSupplier;
import com.erp.md.mapper.MdSupplierMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 应付账龄分析实现。
 * <p>
 * 与应收账龄口径一致：只统计未结清且仍有余额的应付单，逾期天数按「基准日 - 到期日」计算，
 * 到期日为空视为未到期。
 */
@Service
@RequiredArgsConstructor
public class ApAgingServiceImpl implements ApAgingService {

    private final ApPayableMapper payableMapper;
    private final MdSupplierMapper supplierMapper;

    @Override
    public List<ApAgingVO> aging(LocalDate asOfDate) {
        LocalDate base = asOfDate == null ? LocalDate.now() : asOfDate;
        List<ApPayable> openList = payableMapper.selectList(Wrappers.<ApPayable>lambdaQuery()
                .eq(ApPayable::getStatus, ApPayable.STATUS_OPEN));
        if (openList.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, MdSupplier> supplierMap = loadSuppliers(openList);
        Map<Long, ApAgingVO> result = new LinkedHashMap<>();
        for (ApPayable payable : openList) {
            BigDecimal paid = payable.getPaidAmount() == null ? BigDecimal.ZERO : payable.getPaidAmount();
            BigDecimal remaining = payable.getAmount().subtract(paid);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            ApAgingVO vo = result.computeIfAbsent(payable.getSupplierId(), key -> {
                ApAgingVO created = new ApAgingVO();
                created.setSupplierId(key);
                created.setNotDue(BigDecimal.ZERO);
                created.setWithin30(BigDecimal.ZERO);
                created.setWithin60(BigDecimal.ZERO);
                created.setWithin90(BigDecimal.ZERO);
                created.setOver90(BigDecimal.ZERO);
                return created;
            });
            MdSupplier supplier = supplierMap.get(payable.getSupplierId());
            if (supplier != null && vo.getSupplierName() == null) {
                vo.setSupplierName(supplier.getSupplierName());
            }
            long overdueDays = payable.getDueDate() == null ? 0 : ChronoUnit.DAYS.between(payable.getDueDate(), base);
            if (overdueDays <= 0) {
                vo.setNotDue(vo.getNotDue().add(remaining));
            } else if (overdueDays <= 30) {
                vo.setWithin30(vo.getWithin30().add(remaining));
            } else if (overdueDays <= 60) {
                vo.setWithin60(vo.getWithin60().add(remaining));
            } else if (overdueDays <= 90) {
                vo.setWithin90(vo.getWithin90().add(remaining));
            } else {
                vo.setOver90(vo.getOver90().add(remaining));
            }
        }
        return new ArrayList<>(result.values());
    }

    private Map<Long, MdSupplier> loadSuppliers(List<ApPayable> payables) {
        List<Long> supplierIds = payables.stream()
                .map(ApPayable::getSupplierId)
                .distinct()
                .collect(Collectors.toList());
        if (supplierIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return supplierMapper.selectList(Wrappers.<MdSupplier>lambdaQuery().in(MdSupplier::getId, supplierIds))
                .stream()
                .collect(Collectors.toMap(MdSupplier::getId, Function.identity()));
    }
}











