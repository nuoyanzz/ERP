package com.erp.ar.service.impl;

import com.erp.common.util.SerialNoGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.ar.domain.dto.ArReceivableQueryRequest;
import com.erp.ar.domain.entity.ArReceivable;
import com.erp.ar.domain.vo.ArAgingVO;
import com.erp.ar.domain.vo.ArReceivableVO;
import com.erp.ar.mapper.ArReceivableMapper;
import com.erp.ar.service.ArReceivableService;
import com.erp.md.domain.entity.MdCustomer;
import com.erp.md.mapper.MdCustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArReceivableServiceImpl extends ServiceImpl<ArReceivableMapper, ArReceivable> implements ArReceivableService {

    private static final String NO_PREFIX = "AR-";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String SOURCE_TYPE_OUTSTOCK = "SA_OUTSTOCK";
    private static final int CREDIT_DAYS = 30;

    private final MdCustomerMapper customerMapper;
    /** 业务单号生成器（原子自增，避免并发撞号） */
    private final SerialNoGenerator serialNoGenerator;

    @Override
    public PageResult<ArReceivableVO> pageReceivable(ArReceivableQueryRequest request) {
        Page<ArReceivable> page = this.page(request.toPage(), Wrappers.<ArReceivable>lambdaQuery()
                .eq(request.getCustomerId() != null, ArReceivable::getCustomerId, request.getCustomerId())
                .eq(request.getStatus() != null, ArReceivable::getStatus, request.getStatus())
                .like(StringUtils.hasText(request.getSourceNo()), ArReceivable::getSourceNo, request.getSourceNo())
                .orderByDesc(ArReceivable::getId));
        Map<Long, MdCustomer> customerMap = loadCustomers(page.getRecords());
        List<ArReceivableVO> records = page.getRecords().stream()
                .map(receivable -> toVO(receivable, customerMap))
                .collect(Collectors.toList());
        Page<ArReceivableVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public ArReceivableVO getReceivableDetail(Long id) {
        ArReceivable receivable = this.getById(id);
        if (receivable == null) {
            throw new BizException("应收单不存在");
        }
        return toVO(receivable, loadCustomers(Collections.singletonList(receivable)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFromOutstock(Long customerId, Long sourceId, String sourceNo, BigDecimal amount,
                                    LocalDate bizDate, Long deptId) {
        ArReceivable receivable = new ArReceivable();
        receivable.setReceivableNo(nextNo(bizDate));
        receivable.setCustomerId(customerId);
        receivable.setSourceType(SOURCE_TYPE_OUTSTOCK);
        receivable.setSourceId(sourceId);
        receivable.setSourceNo(sourceNo);
        receivable.setAmount(amount);
        receivable.setReceivedAmount(BigDecimal.ZERO);
        receivable.setStatus(ArReceivable.STATUS_OPEN);
        receivable.setDueDate(bizDate.plusDays(CREDIT_DAYS));
        receivable.setDeptId(deptId);
        this.save(receivable);
    }

    /**
     * 冲销应收。
     * <p>
     * 销售退货时按来源出库单找到对应应收单，冲减其已收金额。
     * 若应收已被部分收款，只冲减剩余部分；若已全额收清则不再冲减——
     * 已收的钱需要通过退款单退回，而不是凭空减少应收。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal reverseBySource(String sourceType, Long sourceId, BigDecimal amount) {
        if (sourceId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        List<ArReceivable> receivables = this.list(Wrappers.<ArReceivable>lambdaQuery()
                .eq(ArReceivable::getSourceType, sourceType)
                .eq(ArReceivable::getSourceId, sourceId)
                .eq(ArReceivable::getStatus, ArReceivable.STATUS_OPEN));
        BigDecimal reversed = BigDecimal.ZERO;
        for (ArReceivable receivable : receivables) {
            BigDecimal received = receivable.getReceivedAmount() == null
                    ? BigDecimal.ZERO : receivable.getReceivedAmount();
            BigDecimal remaining = receivable.getAmount().subtract(received);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal current = amount.subtract(reversed);
            if (current.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal reverseAmount = current.min(remaining);
            BigDecimal newReceived = received.add(reverseAmount);
            ArReceivable update = new ArReceivable();
            update.setId(receivable.getId());
            update.setReceivedAmount(newReceived);
            if (newReceived.compareTo(receivable.getAmount()) >= 0) {
                update.setStatus(ArReceivable.STATUS_CLEARED);
            }
            this.updateById(update);
            reversed = reversed.add(reverseAmount);
        }
        return reversed;
    }

    @Override
    public List<ArAgingVO> aging(LocalDate asOfDate) {
        LocalDate base = asOfDate == null ? LocalDate.now() : asOfDate;
        List<ArReceivable> openList = this.list(Wrappers.<ArReceivable>lambdaQuery()
                .eq(ArReceivable::getStatus, ArReceivable.STATUS_OPEN));
        if (openList.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, MdCustomer> customerMap = loadCustomers(openList);
        Map<Long, ArAgingVO> result = new java.util.LinkedHashMap<>();
        for (ArReceivable receivable : openList) {
            BigDecimal remaining = receivable.getAmount()
                    .subtract(receivable.getReceivedAmount() == null ? BigDecimal.ZERO : receivable.getReceivedAmount());
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            ArAgingVO vo = result.computeIfAbsent(receivable.getCustomerId(), key -> {
                ArAgingVO created = new ArAgingVO();
                created.setCustomerId(key);
                created.setTotal(BigDecimal.ZERO);
                created.setNotDue(BigDecimal.ZERO);
                created.setWithin30(BigDecimal.ZERO);
                created.setWithin60(BigDecimal.ZERO);
                created.setWithin90(BigDecimal.ZERO);
                created.setOver90(BigDecimal.ZERO);
                return created;
            });
            MdCustomer customer = customerMap.get(receivable.getCustomerId());
            if (customer != null && vo.getCustomerName() == null) {
                vo.setCustomerName(customer.getCustomerName());
            }
            vo.setTotal(vo.getTotal().add(remaining));
            long overdueDays = receivable.getDueDate() == null ? 0 : ChronoUnit.DAYS.between(receivable.getDueDate(), base);
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

    private Map<Long, MdCustomer> loadCustomers(List<ArReceivable> receivables) {
        List<Long> customerIds = receivables.stream()
                .map(ArReceivable::getCustomerId)
                .distinct()
                .collect(Collectors.toList());
        if (customerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return customerMapper.selectList(Wrappers.<MdCustomer>lambdaQuery().in(MdCustomer::getId, customerIds))
                .stream()
                .collect(Collectors.toMap(MdCustomer::getId, Function.identity()));
    }

    private ArReceivableVO toVO(ArReceivable receivable, Map<Long, MdCustomer> customerMap) {
        ArReceivableVO vo = new ArReceivableVO();
        BeanUtils.copyProperties(receivable, vo);
        MdCustomer customer = customerMap.get(receivable.getCustomerId());
        if (customer != null) {
            vo.setCustomerName(customer.getCustomerName());
        }
        vo.setUnreceivedAmount(receivable.getAmount().subtract(
                receivable.getReceivedAmount() == null ? BigDecimal.ZERO : receivable.getReceivedAmount()));
        return vo;
    }

    private String nextNo(LocalDate date) {
        // 序号取自流水表原子自增：并发不撞号，软删除不重用旧号
        return serialNoGenerator.next("AR", date.format(MONTH_FORMATTER));
    }

}








