package com.erp.ar.service.impl;

import com.erp.common.util.SerialNoGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.ar.domain.dto.ArBadDebtQueryRequest;
import com.erp.ar.domain.dto.ArBadDebtSaveRequest;
import com.erp.ar.domain.entity.ArBadDebt;
import com.erp.ar.domain.entity.ArReceivable;
import com.erp.ar.domain.vo.ArBadDebtVO;
import com.erp.ar.mapper.ArBadDebtMapper;
import com.erp.ar.mapper.ArReceivableMapper;
import com.erp.ar.service.ArBadDebtService;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.fi.domain.dto.VoucherLine;
import com.erp.fi.domain.entity.FiVoucherEntry;
import com.erp.fi.service.FiVoucherService;
import com.erp.md.domain.entity.MdCustomer;
import com.erp.md.mapper.MdCustomerMapper;
import com.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 应收账款坏账计提服务实现。
 * <p>
 * 计提单审核时做两件事：
 * <ol>
 *   <li>把计提金额按到期日顺序分摊到该客户未结清的应收单上（bad_debt_amount）；</li>
 *   <li>生成记账凭证：借「信用减值损失」贷「坏账准备」，科目由
 *       {@code fi_biz_account_rule} 表中 biz_type=AR_BAD_DEBT 的规则给出。</li>
 * </ol>
 * 同一客户同一期间只允许存在一条计提记录，避免重复计提。
 */
@Service
@RequiredArgsConstructor
public class ArBadDebtServiceImpl extends ServiceImpl<ArBadDebtMapper, ArBadDebt> implements ArBadDebtService {

    private static final String NO_PREFIX = "BD-";
    /** 坏账计提对应的业务类型，用于匹配会计科目映射规则 */
    private static final String VOUCHER_BIZ_TYPE = "AR_BAD_DEBT";
    /** 借方科目规则键：信用减值损失 */
    private static final String RULE_KEY_LOSS = "LOSS";
    /** 贷方科目规则键：坏账准备 */
    private static final String RULE_KEY_PROVISION = "PROVISION";

    private final ArReceivableMapper receivableMapper;
    private final MdCustomerMapper customerMapper;
    private final FiVoucherService voucherService;
    /** 业务单号生成器（原子自增，避免并发撞号） */
    private final SerialNoGenerator serialNoGenerator;

    @Override
    public PageResult<ArBadDebtVO> pageBadDebt(ArBadDebtQueryRequest request) {
        Page<ArBadDebt> page = this.page(request.toPage(), Wrappers.<ArBadDebt>lambdaQuery()
                .eq(request.getCustomerId() != null, ArBadDebt::getCustomerId, request.getCustomerId())
                .eq(StringUtils.hasText(request.getPeriod()), ArBadDebt::getPeriod, request.getPeriod())
                .eq(request.getStatus() != null, ArBadDebt::getStatus, request.getStatus())
                .like(StringUtils.hasText(request.getRecordNo()), ArBadDebt::getRecordNo, request.getRecordNo())
                .orderByDesc(ArBadDebt::getId));
        Map<Long, MdCustomer> customerMap = loadCustomers(page.getRecords());
        List<ArBadDebtVO> records = page.getRecords().stream()
                .map(badDebt -> toVO(badDebt, customerMap))
                .collect(Collectors.toList());
        Page<ArBadDebtVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public ArBadDebtVO getBadDebtDetail(Long id) {
        ArBadDebt badDebt = this.getById(id);
        if (badDebt == null) {
            throw new BizException("坏账计提单不存在");
        }
        return toVO(badDebt, loadCustomers(Collections.singletonList(badDebt)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBadDebt(ArBadDebtSaveRequest request) {
        checkCustomerExists(request.getCustomerId());
        checkPeriodUnique(request.getCustomerId(), request.getPeriod(), null);
        ArBadDebt badDebt = new ArBadDebt();
        BeanUtils.copyProperties(request, badDebt);
        badDebt.setRecordNo(nextNo(request.getPeriod()));
        badDebt.setStatus(ArBadDebt.STATUS_DRAFT);
        this.save(badDebt);
        return badDebt.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBadDebt(ArBadDebtSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        ArBadDebt exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("坏账计提单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != ArBadDebt.STATUS_DRAFT) {
            throw new BizException("只有草稿状态的坏账计提单才能修改");
        }
        checkCustomerExists(request.getCustomerId());
        checkPeriodUnique(request.getCustomerId(), request.getPeriod(), request.getId());
        ArBadDebt badDebt = new ArBadDebt();
        badDebt.setId(request.getId());
        badDebt.setCustomerId(request.getCustomerId());
        badDebt.setPeriod(request.getPeriod());
        badDebt.setAmount(request.getAmount());
        badDebt.setReason(request.getReason());
        badDebt.setRemark(request.getRemark());
        this.updateById(badDebt);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBadDebt(Long id) {
        ArBadDebt exist = this.getById(id);
        if (exist == null) {
            throw new BizException("坏账计提单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != ArBadDebt.STATUS_DRAFT) {
            throw new BizException("只有草稿状态的坏账计提单才能删除");
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditBadDebt(Long id) {
        ArBadDebt badDebt = this.getById(id);
        if (badDebt == null) {
            throw new BizException("坏账计提单不存在");
        }
        if (badDebt.getStatus() == null || badDebt.getStatus() != ArBadDebt.STATUS_DRAFT) {
            throw new BizException("只有草稿状态的坏账计提单才能审核");
        }
        MdCustomer customer = customerMapper.selectById(badDebt.getCustomerId());
        if (customer == null) {
            throw new BizException("客户不存在");
        }
        BigDecimal amount = badDebt.getAmount() == null ? BigDecimal.ZERO : badDebt.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("计提金额必须大于0");
        }
        // 计提金额落到具体应收单上：按到期日从早到晚依次分摊
        applyToReceivables(badDebt.getCustomerId(), amount);
        // 记账凭证：借 信用减值损失 / 贷 坏账准备
        Long voucherId = voucherService.generateBizVoucher(VOUCHER_BIZ_TYPE, voucherDate(badDebt.getPeriod()),
                "坏账计提 " + badDebt.getRecordNo(),
                List.of(
                        VoucherLine.builder()
                                .direction(FiVoucherEntry.DIRECTION_DEBIT)
                                .ruleKey(RULE_KEY_LOSS)
                                .amount(amount)
                                .summary("计提坏账准备：" + customer.getCustomerName())
                                .build(),
                        VoucherLine.builder()
                                .direction(FiVoucherEntry.DIRECTION_CREDIT)
                                .ruleKey(RULE_KEY_PROVISION)
                                .amount(amount)
                                .summary("计提坏账准备：" + customer.getCustomerName())
                                .auxType(FiVoucherEntry.AUX_CUSTOMER)
                                .auxId(badDebt.getCustomerId())
                                .auxName(customer.getCustomerName())
                                .build()));
        ArBadDebt update = new ArBadDebt();
        update.setId(id);
        update.setStatus(ArBadDebt.STATUS_AUDITED);
        update.setAuditBy(SecurityUtils.getCurrentUserId());
        update.setAuditTime(LocalDateTime.now());
        update.setVoucherId(voucherId);
        this.updateById(update);
    }

    /**
     * 把计提金额分摊到该客户未结清的应收单上。
     * <p>
     * 按到期日从早到晚分摊：到期日越早、越可能收不回，优先计提；
     * 到期日为空视为未到期，排在最后。分摊上限为各应收单的未收金额，
     * 计提总额不得超过该客户的未结清应收余额，否则拒绝审核。
     */
    private void applyToReceivables(Long customerId, BigDecimal amount) {
        List<ArReceivable> receivables = receivableMapper.selectList(Wrappers.<ArReceivable>lambdaQuery()
                .eq(ArReceivable::getCustomerId, customerId)
                .eq(ArReceivable::getStatus, ArReceivable.STATUS_OPEN));
        receivables.sort(Comparator
                .comparing(ArReceivable::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ArReceivable::getId));
        BigDecimal remaining = amount;
        for (ArReceivable receivable : receivables) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal received = receivable.getReceivedAmount() == null
                    ? BigDecimal.ZERO : receivable.getReceivedAmount();
            BigDecimal unpaid = receivable.getAmount().subtract(received);
            if (unpaid.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal allocated = remaining.min(unpaid);
            ArReceivable update = new ArReceivable();
            update.setId(receivable.getId());
            update.setBadDebtAmount(nz(receivable.getBadDebtAmount()).add(allocated));
            receivableMapper.updateById(update);
            remaining = remaining.subtract(allocated);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new BizException("计提金额超出该客户未结清应收余额，无法计提");
        }
    }

    /**
     * 凭证日期取所属会计期间的最后一天。
     */
    private LocalDate voucherDate(String period) {
        String[] parts = period.split("-");
        return YearMonth.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])).atEndOfMonth();
    }

    /**
     * 同一客户同一期间只能有一条计提记录。
     */
    private void checkPeriodUnique(Long customerId, String period, Long excludeId) {
        Long count = this.count(Wrappers.<ArBadDebt>lambdaQuery()
                .eq(ArBadDebt::getCustomerId, customerId)
                .eq(ArBadDebt::getPeriod, period)
                .ne(excludeId != null, ArBadDebt::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("该客户在本期间已有坏账计提记录，不能重复计提");
        }
    }

    private void checkCustomerExists(Long customerId) {
        if (customerMapper.selectById(customerId) == null) {
            throw new BizException("客户不存在");
        }
    }

    private Map<Long, MdCustomer> loadCustomers(List<ArBadDebt> badDebts) {
        List<Long> customerIds = badDebts.stream()
                .map(ArBadDebt::getCustomerId)
                .distinct()
                .collect(Collectors.toList());
        if (customerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return customerMapper.selectList(Wrappers.<MdCustomer>lambdaQuery().in(MdCustomer::getId, customerIds))
                .stream()
                .collect(Collectors.toMap(MdCustomer::getId, Function.identity()));
    }

    private ArBadDebtVO toVO(ArBadDebt badDebt, Map<Long, MdCustomer> customerMap) {
        ArBadDebtVO vo = new ArBadDebtVO();
        BeanUtils.copyProperties(badDebt, vo);
        MdCustomer customer = customerMap.get(badDebt.getCustomerId());
        if (customer != null) {
            vo.setCustomerName(customer.getCustomerName());
        }
        return vo;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String nextNo(String period) {
        // 序号取自流水表原子自增：并发不撞号，软删除不重用旧号
        return serialNoGenerator.next("BD", period.replace("-", ""));
    }

}













