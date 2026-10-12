package com.erp.ar.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.fi.domain.dto.VoucherLine;
import com.erp.fi.service.FiVoucherService;
import com.erp.ar.domain.dto.ArReceiptSaveRequest;
import com.erp.ar.domain.entity.ArReceipt;
import com.erp.ar.domain.entity.ArReceivable;
import com.erp.ar.domain.entity.ArVerify;
import com.erp.ar.domain.vo.ArReceiptVO;
import com.erp.ar.domain.vo.ArVerifyVO;
import com.erp.ar.mapper.ArReceiptMapper;
import com.erp.ar.mapper.ArReceivableMapper;
import com.erp.ar.mapper.ArVerifyMapper;
import com.erp.ar.service.ArReceiptService;
import com.erp.md.domain.entity.MdCustomer;
import com.erp.md.mapper.MdCustomerMapper;
import com.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
public class ArReceiptServiceImpl extends ServiceImpl<ArReceiptMapper, ArReceipt> implements ArReceiptService {

    private static final String NO_PREFIX = "SK-";
    private static final String VOUCHER_BIZ_TYPE = "AR_RECEIPT";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final ArReceivableMapper receivableMapper;
    private final ArVerifyMapper verifyMapper;
    private final MdCustomerMapper customerMapper;
    private final FiVoucherService voucherService;

    @Override
    public PageResult<ArReceiptVO> pageReceipt(int pageNum, int pageSize, Long customerId, Integer status) {
        Page<ArReceipt> page = this.page(new Page<>(pageNum, pageSize), Wrappers.<ArReceipt>lambdaQuery()
                .eq(customerId != null, ArReceipt::getCustomerId, customerId)
                .eq(status != null, ArReceipt::getStatus, status)
                .orderByDesc(ArReceipt::getId));
        Map<Long, MdCustomer> customerMap = loadCustomers(page.getRecords());
        List<ArReceiptVO> records = page.getRecords().stream()
                .map(receipt -> toVO(receipt, customerMap))
                .collect(Collectors.toList());
        Page<ArReceiptVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public ArReceiptVO getReceiptDetail(Long id) {
        ArReceipt receipt = this.getById(id);
        if (receipt == null) {
            throw new BizException("收款单不存在");
        }
        ArReceiptVO vo = toVO(receipt, loadCustomers(Collections.singletonList(receipt)));
        vo.setVerifies(loadVerifies(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReceipt(ArReceiptSaveRequest request) {
        MdCustomer customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null) {
            throw new BizException("客户不存在");
        }
        ArReceipt receipt = new ArReceipt();
        receipt.setReceiptNo(nextNo(request.getReceiptDate()));
        receipt.setCustomerId(request.getCustomerId());
        receipt.setReceiptDate(request.getReceiptDate());
        receipt.setAmount(request.getAmount());
        receipt.setStatus(ArReceipt.STATUS_DRAFT);
        receipt.setRemark(request.getRemark());
        this.save(receipt);
        return receipt.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReceipt(ArReceiptSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        ArReceipt exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("收款单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != ArReceipt.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的收款单才能修改");
        }
        ArReceipt receipt = new ArReceipt();
        receipt.setId(request.getId());
        receipt.setCustomerId(request.getCustomerId());
        receipt.setReceiptDate(request.getReceiptDate());
        receipt.setAmount(request.getAmount());
        receipt.setRemark(request.getRemark());
        this.updateById(receipt);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReceipt(Long id) {
        ArReceipt exist = this.getById(id);
        if (exist == null) {
            throw new BizException("收款单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != ArReceipt.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的收款单才能删除");
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditReceipt(Long id) {
        ArReceipt receipt = this.getById(id);
        if (receipt == null) {
            throw new BizException("收款单不存在");
        }
        if (receipt.getStatus() == null || receipt.getStatus() != ArReceipt.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的收款单才能审核");
        }
        List<ArReceivable> receivables = receivableMapper.selectList(Wrappers.<ArReceivable>lambdaQuery()
                .eq(ArReceivable::getCustomerId, receipt.getCustomerId())
                .eq(ArReceivable::getStatus, ArReceivable.STATUS_OPEN)
                .orderByAsc(ArReceivable::getDueDate)
                .orderByAsc(ArReceivable::getId));
        BigDecimal remaining = receipt.getAmount();
        List<ArVerify> verifies = new ArrayList<>();
        for (ArReceivable receivable : receivables) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal receivableRemaining = receivable.getAmount()
                    .subtract(receivable.getReceivedAmount() == null ? BigDecimal.ZERO : receivable.getReceivedAmount());
            if (receivableRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal allocate = remaining.min(receivableRemaining);
            ArVerify verify = new ArVerify();
            verify.setReceiptId(receipt.getId());
            verify.setReceivableId(receivable.getId());
            verify.setAmount(allocate);
            verifies.add(verify);
            ArReceivable update = new ArReceivable();
            update.setId(receivable.getId());
            update.setReceivedAmount(receivable.getReceivedAmount().add(allocate));
            if (update.getReceivedAmount().compareTo(receivable.getAmount()) >= 0) {
                update.setStatus(ArReceivable.STATUS_CLEARED);
            }
            receivableMapper.updateById(update);
            remaining = remaining.subtract(allocate);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new BizException("收款金额超过该客户应收总额，无法核销");
        }
        for (ArVerify verify : verifies) {
            verifyMapper.insert(verify);
        }
        Long voucherId = voucherService.generateBizVoucher(VOUCHER_BIZ_TYPE, receipt.getReceiptDate(),
                "收款 " + receipt.getReceiptNo(), java.util.Arrays.asList(
                        VoucherLine.builder()
                                .direction(com.erp.fi.domain.entity.FiVoucherEntry.DIRECTION_DEBIT)
                                .ruleKey("BANK")
                                .amount(receipt.getAmount())
                                .summary("收到货款")
                                .build(),
                        VoucherLine.builder()
                                .direction(com.erp.fi.domain.entity.FiVoucherEntry.DIRECTION_CREDIT)
                                .ruleKey("RECEIVABLE")
                                .amount(receipt.getAmount())
                                .summary("核销应收账款")
                                .build()));
        ArReceipt update = new ArReceipt();
        update.setId(id);
        update.setStatus(ArReceipt.STATUS_AUDITED);
        update.setAuditBy(SecurityUtils.getCurrentUserId());
        update.setAuditTime(LocalDateTime.now());
        update.setVoucherId(voucherId);
        this.updateById(update);
    }

    private List<ArVerifyVO> loadVerifies(Long receiptId) {
        List<ArVerify> verifies = verifyMapper.selectList(Wrappers.<ArVerify>lambdaQuery()
                .eq(ArVerify::getReceiptId, receiptId)
                .orderByAsc(ArVerify::getId));
        if (verifies.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> receivableIds = verifies.stream().map(ArVerify::getReceivableId).distinct().collect(Collectors.toList());
        Map<Long, ArReceivable> receivableMap = receivableMapper.selectList(Wrappers.<ArReceivable>lambdaQuery()
                        .in(ArReceivable::getId, receivableIds))
                .stream()
                .collect(Collectors.toMap(ArReceivable::getId, Function.identity()));
        return verifies.stream().map(verify -> {
            ArVerifyVO vo = new ArVerifyVO();
            BeanUtils.copyProperties(verify, vo);
            ArReceivable receivable = receivableMap.get(verify.getReceivableId());
            if (receivable != null) {
                vo.setReceivableNo(receivable.getReceivableNo());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private Map<Long, MdCustomer> loadCustomers(List<ArReceipt> receipts) {
        List<Long> customerIds = receipts.stream().map(ArReceipt::getCustomerId).distinct().collect(Collectors.toList());
        if (customerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return customerMapper.selectList(Wrappers.<MdCustomer>lambdaQuery().in(MdCustomer::getId, customerIds))
                .stream()
                .collect(Collectors.toMap(MdCustomer::getId, Function.identity()));
    }

    private ArReceiptVO toVO(ArReceipt receipt, Map<Long, MdCustomer> customerMap) {
        ArReceiptVO vo = new ArReceiptVO();
        BeanUtils.copyProperties(receipt, vo);
        MdCustomer customer = customerMap.get(receipt.getCustomerId());
        if (customer != null) {
            vo.setCustomerName(customer.getCustomerName());
        }
        return vo;
    }

    private String nextNo(LocalDate date) {
        String prefix = NO_PREFIX + date.format(MONTH_FORMATTER) + "-";
        String lastNo = baseMapper.selectLastReceiptNo(prefix);
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
        return prefix + String.format("%04d", next);
    }
}








