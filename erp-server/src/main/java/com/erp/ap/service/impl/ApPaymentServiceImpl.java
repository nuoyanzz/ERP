package com.erp.ap.service.impl;

import com.erp.common.util.SerialNoGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.security.datascope.DataScopeHelper;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.ap.domain.dto.ApPaymentSaveRequest;
import com.erp.ap.domain.entity.ApPayment;
import com.erp.ap.domain.entity.ApPayable;
import com.erp.ap.domain.entity.ApVerify;
import com.erp.ap.domain.vo.ApPaymentVO;
import com.erp.ap.domain.vo.ApVerifyVO;
import com.erp.ap.mapper.ApPayableMapper;
import com.erp.ap.mapper.ApPaymentMapper;
import com.erp.ap.mapper.ApVerifyMapper;
import com.erp.ap.service.ApPaymentService;
import com.erp.fi.domain.dto.VoucherLine;
import com.erp.fi.service.FiVoucherService;
import com.erp.md.domain.entity.MdSupplier;
import com.erp.md.mapper.MdSupplierMapper;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApPaymentServiceImpl extends ServiceImpl<ApPaymentMapper, ApPayment> implements ApPaymentService {

    private static final String NO_PREFIX = "FK-";
    private static final String VOUCHER_BIZ_TYPE = "AP_PAYMENT";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final ApPayableMapper payableMapper;

    private final DataScopeHelper dataScopeHelper;
    private final ApVerifyMapper verifyMapper;
    private final MdSupplierMapper supplierMapper;
    private final FiVoucherService voucherService;
    /** 业务单号生成器（原子自增，避免并发撞号） */
    private final SerialNoGenerator serialNoGenerator;

    @Override
    public PageResult<ApPaymentVO> pagePayment(int pageNum, int pageSize, Long supplierId, Integer status) {
        Page<ApPayment> page = this.page(new Page<>(pageNum, pageSize), Wrappers.<ApPayment>lambdaQuery()
                .eq(supplierId != null, ApPayment::getSupplierId, supplierId)
                .eq(status != null, ApPayment::getStatus, status)
                .orderByDesc(ApPayment::getId));
        Map<Long, MdSupplier> supplierMap = loadSuppliers(page.getRecords());
        List<ApPaymentVO> records = page.getRecords().stream()
                .map(payment -> toVO(payment, supplierMap))
                .collect(Collectors.toList());
        Page<ApPaymentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public ApPaymentVO getPaymentDetail(Long id) {
        ApPayment payment = this.getById(id);
        if (payment == null) {
            throw new BizException("付款单不存在");
        }
        ApPaymentVO vo = toVO(payment, loadSuppliers(Collections.singletonList(payment)));
        vo.setVerifies(loadVerifies(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPayment(ApPaymentSaveRequest request) {
        MdSupplier supplier = supplierMapper.selectById(request.getSupplierId());
        if (supplier == null) {
            throw new BizException("供应商不存在");
        }
        ApPayment payment = new ApPayment();
        payment.setPaymentNo(nextNo(request.getPaymentDate()));
        payment.setSupplierId(request.getSupplierId());
        payment.setPaymentDate(request.getPaymentDate());
        payment.setAmount(request.getAmount());
        // 归属部门供数据权限过滤：优先继承来源单据，其次取当前用户部门
        payment.setDeptId(dataScopeHelper.resolveDeptId(null, null));
        payment.setStatus(ApPayment.STATUS_DRAFT);
        payment.setRemark(request.getRemark());
        this.save(payment);
        return payment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePayment(ApPaymentSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        ApPayment exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("付款单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != ApPayment.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的付款单才能修改");
        }
        ApPayment payment = new ApPayment();
        payment.setId(request.getId());
        payment.setSupplierId(request.getSupplierId());
        payment.setPaymentDate(request.getPaymentDate());
        payment.setAmount(request.getAmount());
        payment.setRemark(request.getRemark());
        this.updateById(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePayment(Long id) {
        ApPayment exist = this.getById(id);
        if (exist == null) {
            throw new BizException("付款单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != ApPayment.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的付款单才能删除");
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditPayment(Long id) {
        ApPayment payment = this.getById(id);
        if (payment == null) {
            throw new BizException("付款单不存在");
        }
        if (payment.getStatus() == null || payment.getStatus() != ApPayment.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的付款单才能审核");
        }
        List<ApPayable> payables = payableMapper.selectList(Wrappers.<ApPayable>lambdaQuery()
                .eq(ApPayable::getSupplierId, payment.getSupplierId())
                .eq(ApPayable::getStatus, ApPayable.STATUS_OPEN)
                .orderByAsc(ApPayable::getDueDate)
                .orderByAsc(ApPayable::getId));
        BigDecimal remaining = payment.getAmount();
        List<ApVerify> verifies = new ArrayList<>();
        for (ApPayable payable : payables) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal payableRemaining = payable.getAmount().subtract(
                    payable.getPaidAmount() == null ? BigDecimal.ZERO : payable.getPaidAmount());
            if (payableRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal allocate = remaining.min(payableRemaining);
            ApVerify verify = new ApVerify();
            verify.setPaymentId(payment.getId());
            verify.setPayableId(payable.getId());
            verify.setAmount(allocate);
            verifies.add(verify);
            ApPayable update = new ApPayable();
            update.setId(payable.getId());
            update.setPaidAmount(payable.getPaidAmount().add(allocate));
            if (update.getPaidAmount().compareTo(payable.getAmount()) >= 0) {
                update.setStatus(ApPayable.STATUS_CLEARED);
            }
            payableMapper.updateById(update);
            remaining = remaining.subtract(allocate);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new BizException("付款金额超过该供应商应付总额，无法核销");
        }
        for (ApVerify verify : verifies) {
            verifyMapper.insert(verify);
        }
        Long voucherId = voucherService.generateBizVoucher(VOUCHER_BIZ_TYPE, payment.getPaymentDate(),
                "付款 " + payment.getPaymentNo(), Arrays.asList(
                        VoucherLine.builder()
                                .direction(com.erp.fi.domain.entity.FiVoucherEntry.DIRECTION_DEBIT)
                                .ruleKey("PAYABLE")
                                .amount(payment.getAmount())
                                .summary("支付货款")
                                .build(),
                        VoucherLine.builder()
                                .direction(com.erp.fi.domain.entity.FiVoucherEntry.DIRECTION_CREDIT)
                                .ruleKey("BANK")
                                .amount(payment.getAmount())
                                .summary("银行支付")
                                .build()));
        ApPayment update = new ApPayment();
        update.setId(id);
        update.setStatus(ApPayment.STATUS_AUDITED);
        update.setAuditBy(SecurityUtils.getCurrentUserId());
        update.setAuditTime(LocalDateTime.now());
        update.setVoucherId(voucherId);
        this.updateById(update);
    }

    private List<ApVerifyVO> loadVerifies(Long paymentId) {
        List<ApVerify> verifies = verifyMapper.selectList(Wrappers.<ApVerify>lambdaQuery()
                .eq(ApVerify::getPaymentId, paymentId)
                .orderByAsc(ApVerify::getId));
        if (verifies.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> payableIds = verifies.stream().map(ApVerify::getPayableId).distinct().collect(Collectors.toList());
        Map<Long, ApPayable> payableMap = payableMapper.selectList(Wrappers.<ApPayable>lambdaQuery()
                        .in(ApPayable::getId, payableIds))
                .stream()
                .collect(Collectors.toMap(ApPayable::getId, Function.identity()));
        return verifies.stream().map(verify -> {
            ApVerifyVO vo = new ApVerifyVO();
            BeanUtils.copyProperties(verify, vo);
            ApPayable payable = payableMap.get(verify.getPayableId());
            if (payable != null) {
                vo.setPayableNo(payable.getPayableNo());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private Map<Long, MdSupplier> loadSuppliers(List<ApPayment> payments) {
        List<Long> supplierIds = payments.stream().map(ApPayment::getSupplierId).distinct().collect(Collectors.toList());
        if (supplierIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return supplierMapper.selectList(Wrappers.<MdSupplier>lambdaQuery().in(MdSupplier::getId, supplierIds))
                .stream()
                .collect(Collectors.toMap(MdSupplier::getId, Function.identity()));
    }

    private ApPaymentVO toVO(ApPayment payment, Map<Long, MdSupplier> supplierMap) {
        ApPaymentVO vo = new ApPaymentVO();
        BeanUtils.copyProperties(payment, vo);
        MdSupplier supplier = supplierMap.get(payment.getSupplierId());
        if (supplier != null) {
            vo.setSupplierName(supplier.getSupplierName());
        }
        return vo;
    }

    private String nextNo(LocalDate date) {
        // 序号取自流水表原子自增：并发不撞号，软删除不重用旧号
        return serialNoGenerator.next("FK", date.format(MONTH_FORMATTER));
    }
}








