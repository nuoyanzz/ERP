package com.erp.fi.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.exception.BizException;
import com.erp.fi.domain.dto.VoucherLine;
import com.erp.fi.domain.entity.FiAccount;
import com.erp.fi.domain.entity.FiCarryForward;
import com.erp.fi.domain.entity.FiPeriod;
import com.erp.fi.domain.entity.FiVoucher;
import com.erp.fi.domain.entity.FiVoucherEntry;
import com.erp.fi.mapper.FiAccountMapper;
import com.erp.fi.mapper.FiCarryForwardMapper;
import com.erp.fi.mapper.FiPeriodMapper;
import com.erp.fi.mapper.FiVoucherEntryMapper;
import com.erp.fi.mapper.FiVoucherMapper;
import com.erp.fi.service.FiCarryForwardService;
import com.erp.fi.service.FiVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FiCarryForwardServiceImpl implements FiCarryForwardService {

    private static final String PROFIT_ACCOUNT_CODE = "4103";
    private static final String RETAINED_EARNINGS_CODE = "410401";
    private static final String VOUCHER_BIZ_TYPE = "CARRY_FORWARD";
    private static final String YEAR_END_BIZ_TYPE = "YEAR_END";

    private final FiPeriodMapper periodMapper;
    private final FiVoucherMapper voucherMapper;
    private final FiVoucherEntryMapper voucherEntryMapper;
    private final FiAccountMapper accountMapper;
    private final FiCarryForwardMapper carryForwardMapper;
    private final FiVoucherService voucherService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long carryForwardProfitAndLoss(String period) {
        FiPeriod fiPeriod = requireOpenPeriod(period);

        FiCarryForward existing = findActiveRecord(period, FiCarryForward.TYPE_PROFIT_LOSS);
        if (existing != null) {
            throw new BizException("本期间已执行过损益结转，不能重复结转");
        }

        FiAccount profitAccount = requireAccount(PROFIT_ACCOUNT_CODE);
        List<FiAccount> plAccounts = loadCarryForwardAccounts();
        if (plAccounts.isEmpty()) {
            throw new BizException("未配置损益类科目");
        }

        Map<Long, BigDecimal> netMap = loadNetAmounts(period, plAccounts);
        List<VoucherLine> lines = new ArrayList<>();
        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;

        for (FiAccount account : plAccounts) {
            BigDecimal net = netMap.get(account.getId());
            if (net == null || net.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            if (net.compareTo(BigDecimal.ZERO) > 0) {
                // 借方余额（如收入）结转时记贷方
                lines.add(VoucherLine.builder()
                        .direction(FiVoucherEntry.DIRECTION_CREDIT)
                        .subjectCode(account.getSubjectCode())
                        .amount(net)
                        .summary("结转" + account.getSubjectName())
                        .build());
                debitTotal = debitTotal.add(net);
            } else {
                // 贷方余额（如费用）结转时记借方
                lines.add(VoucherLine.builder()
                        .direction(FiVoucherEntry.DIRECTION_DEBIT)
                        .subjectCode(account.getSubjectCode())
                        .amount(net.abs())
                        .summary("结转" + account.getSubjectName())
                        .build());
                creditTotal = creditTotal.add(net.abs());
            }
        }
        if (lines.isEmpty()) {
            throw new BizException("本期间无损益发生额，无需结转");
        }

        BigDecimal netProfit = debitTotal.subtract(creditTotal);
        if (netProfit.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(VoucherLine.builder()
                    .direction(FiVoucherEntry.DIRECTION_DEBIT)
                    .subjectCode(PROFIT_ACCOUNT_CODE)
                    .amount(netProfit)
                    .summary("结转本年利润")
                    .build());
        } else if (netProfit.compareTo(BigDecimal.ZERO) < 0) {
            lines.add(VoucherLine.builder()
                    .direction(FiVoucherEntry.DIRECTION_CREDIT)
                    .subjectCode(PROFIT_ACCOUNT_CODE)
                    .amount(netProfit.abs())
                    .summary("结转本年利润")
                    .build());
        }

        LocalDate voucherDate = YearMonth.parse(period).atEndOfMonth();
        Long voucherId = voucherService.generateBizVoucher(VOUCHER_BIZ_TYPE, voucherDate,
                "期间损益结转 " + period, lines);
        postVoucherDirectly(voucherId);

        FiCarryForward record = new FiCarryForward();
        record.setPeriod(period);
        record.setCarryType(FiCarryForward.TYPE_PROFIT_LOSS);
        record.setVoucherId(voucherId);
        record.setTotalAmount(netProfit);
        record.setStatus(FiCarryForward.STATUS_ACTIVE);
        carryForwardMapper.insert(record);

        FiPeriod periodUpdate = new FiPeriod();
        periodUpdate.setId(fiPeriod.getId());
        periodUpdate.setCarriedForward(1);
        periodMapper.updateById(periodUpdate);
        return voucherId;
    }

    /**
     * 年末结转：把本年利润科目余额结转到未分配利润。
     * <p>
     * 12 月损益结转后本年利润科目仍有余额（当年净利润），年末需再结转到利润分配-未分配利润，
     * 否则该余额会永远挂着，次年无法清零。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long carryForwardYearEnd(String period) {
        FiPeriod fiPeriod = requireOpenPeriod(period);
        if (!Integer.valueOf(12).equals(fiPeriod.getPeriodNo())) {
            throw new BizException("年末结转只能在 12 月执行");
        }
        FiCarryForward existing = findActiveRecord(period, FiCarryForward.TYPE_YEAR_END);
        if (existing != null) {
            throw new BizException("本期间已执行过年末结转，不能重复结转");
        }

        FiAccount profitAccount = requireAccount(PROFIT_ACCOUNT_CODE);
        FiAccount retainedAccount = requireAccount(RETAINED_EARNINGS_CODE);

        BigDecimal profitNet = loadAccountNet(period, profitAccount.getId());
        if (profitNet.compareTo(BigDecimal.ZERO) == 0) {
            throw new BizException("本年利润科目余额为零，无需年末结转");
        }

        List<VoucherLine> lines = new ArrayList<>();
        if (profitNet.compareTo(BigDecimal.ZERO) > 0) {
            // 盈利：借本年利润，贷未分配利润
            lines.add(VoucherLine.builder()
                    .direction(FiVoucherEntry.DIRECTION_DEBIT)
                    .subjectCode(PROFIT_ACCOUNT_CODE)
                    .amount(profitNet)
                    .summary("年末结转本年利润")
                    .build());
            lines.add(VoucherLine.builder()
                    .direction(FiVoucherEntry.DIRECTION_CREDIT)
                    .subjectCode(RETAINED_EARNINGS_CODE)
                    .amount(profitNet)
                    .summary("年末结转未分配利润")
                    .build());
        } else {
            // 亏损：借未分配利润，贷本年利润
            lines.add(VoucherLine.builder()
                    .direction(FiVoucherEntry.DIRECTION_DEBIT)
                    .subjectCode(RETAINED_EARNINGS_CODE)
                    .amount(profitNet.abs())
                    .summary("年末结转未分配利润")
                    .build());
            lines.add(VoucherLine.builder()
                    .direction(FiVoucherEntry.DIRECTION_CREDIT)
                    .subjectCode(PROFIT_ACCOUNT_CODE)
                    .amount(profitNet.abs())
                    .summary("年末结转本年利润")
                    .build());
        }

        LocalDate voucherDate = YearMonth.parse(period).atEndOfMonth();
        Long voucherId = voucherService.generateBizVoucher(YEAR_END_BIZ_TYPE, voucherDate,
                "年末结转 " + period, lines);
        postVoucherDirectly(voucherId);

        FiCarryForward record = new FiCarryForward();
        record.setPeriod(period);
        record.setCarryType(FiCarryForward.TYPE_YEAR_END);
        record.setVoucherId(voucherId);
        record.setTotalAmount(profitNet);
        record.setStatus(FiCarryForward.STATUS_ACTIVE);
        carryForwardMapper.insert(record);
        return voucherId;
    }

    /**
     * 反结转：作废结转凭证并把结转记录标记为已反结转。
     * <p>
     * 反结转用于纠正错误的结转。结转凭证是系统生成而非手工录入，
     * 直接作废比红冲更符合「这笔账不该存在」的语义。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverseCarryForward(String period, Integer carryType) {
        FiCarryForward record = findActiveRecord(period, carryType);
        if (record == null) {
            throw new BizException("该期间没有可反结转的结转记录");
        }
        if (record.getVoucherId() == null) {
            throw new BizException("结转记录缺少凭证信息，无法反结转");
        }
        FiVoucher voucher = voucherMapper.selectById(record.getVoucherId());
        if (voucher == null) {
            throw new BizException("结转凭证不存在，可能已被删除");
        }
        if (voucher.getStatus() != null && voucher.getStatus() == FiVoucher.STATUS_POSTED) {
            throw new BizException("结转凭证已记账，请先反记账再反结转");
        }
        voucherService.voidVoucher(voucher.getId(), "反结转：" + period);

        FiCarryForward update = new FiCarryForward();
        update.setId(record.getId());
        update.setStatus(FiCarryForward.STATUS_REVERSED);
        carryForwardMapper.updateById(update);

        FiPeriod fiPeriod = periodMapper.selectOne(Wrappers.<FiPeriod>lambdaQuery()
                .eq(FiPeriod::getPeriodMonth, period)
                .last("limit 1"));
        if (fiPeriod != null && carryType != null && carryType == FiCarryForward.TYPE_PROFIT_LOSS) {
            FiPeriod periodUpdate = new FiPeriod();
            periodUpdate.setId(fiPeriod.getId());
            periodUpdate.setCarriedForward(0);
            periodMapper.updateById(periodUpdate);
        }
    }

    /**
     * 结转凭证直接记账。
     * <p>
     * 期末结转是系统内部生成的技术性凭证，由系统直接完成审核与记账，
     * 不占用财务人员的手工审核环节。业务凭证引擎已默认直接记账，
     * 此处仅做状态兜底校验。
     */
    private void postVoucherDirectly(Long voucherId) {
        FiVoucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            throw new BizException("结转凭证生成失败");
        }
        if (voucher.getStatus() != null && voucher.getStatus() == FiVoucher.STATUS_POSTED) {
            return;
        }
        if (voucher.getStatus() != null && voucher.getStatus() != FiVoucher.STATUS_SUBMITTED) {
            throw new BizException("结转凭证状态异常，无法记账");
        }
        FiVoucher update = new FiVoucher();
        update.setId(voucherId);
        update.setStatus(FiVoucher.STATUS_POSTED);
        voucherMapper.updateById(update);
    }

    private FiPeriod requireOpenPeriod(String period) {
        if (!StringUtils.hasText(period)) {
            throw new BizException("会计期间不能为空");
        }
        FiPeriod fiPeriod = periodMapper.selectOne(Wrappers.<FiPeriod>lambdaQuery()
                .eq(FiPeriod::getPeriodMonth, period)
                .last("limit 1"));
        if (fiPeriod == null) {
            throw new BizException("会计期间不存在：" + period);
        }
        if (fiPeriod.getStatus() == null || fiPeriod.getStatus() != FiPeriod.STATUS_OPEN) {
            throw new BizException("会计期间未开放，不能执行结转");
        }
        return fiPeriod;
    }

    private FiAccount requireAccount(String subjectCode) {
        FiAccount account = accountMapper.selectOne(Wrappers.<FiAccount>lambdaQuery()
                .eq(FiAccount::getSubjectCode, subjectCode)
                .last("limit 1"));
        if (account == null) {
            throw new BizException("科目不存在：" + subjectCode);
        }
        return account;
    }

    /**
     * 加载参与损益结转的科目。
     * <p>
     * 排除「以前年度损益调整」等不应结转的科目——它们属于以前年度的损益，
     * 结转会污染本年利润。
     */
    private List<FiAccount> loadCarryForwardAccounts() {
        List<FiAccount> accounts = accountMapper.selectList(Wrappers.<FiAccount>lambdaQuery()
                .eq(FiAccount::getCategory, FiAccount.CATEGORY_PROFIT)
                .eq(FiAccount::getStatus, 0));
        return accounts.stream()
                .filter(account -> !"6901".equals(account.getSubjectCode()))
                .sorted((a, b) -> a.getSubjectCode().compareTo(b.getSubjectCode()))
                .collect(Collectors.toList());
    }

    private Map<Long, BigDecimal> loadNetAmounts(String period, List<FiAccount> accounts) {
        Map<Long, Boolean> included = accounts.stream()
                .collect(Collectors.toMap(FiAccount::getId, a -> true));
        Map<Long, BigDecimal> netMap = new HashMap<>();
        for (Map<String, Object> row : voucherEntryMapper.sumPostedByPeriod(period)) {
            Object accountId = row.get("accountId");
            if (accountId == null) {
                continue;
            }
            Long id = ((Number) accountId).longValue();
            if (!included.containsKey(id)) {
                continue;
            }
            BigDecimal debit = toDecimal(row.get("debitAmount"));
            BigDecimal credit = toDecimal(row.get("creditAmount"));
            netMap.put(id, debit.subtract(credit));
        }
        return netMap;
    }

    private BigDecimal loadAccountNet(String period, Long accountId) {
        BigDecimal net = BigDecimal.ZERO;
        for (Map<String, Object> row : voucherEntryMapper.sumPostedByPeriod(period)) {
            Object id = row.get("accountId");
            if (id == null || ((Number) id).longValue() != accountId) {
                continue;
            }
            net = net.add(toDecimal(row.get("debitAmount"))).subtract(toDecimal(row.get("creditAmount")));
        }
        return net;
    }

    private FiCarryForward findActiveRecord(String period, Integer carryType) {
        return carryForwardMapper.selectOne(Wrappers.<FiCarryForward>lambdaQuery()
                .eq(FiCarryForward::getPeriod, period)
                .eq(FiCarryForward::getCarryType, carryType)
                .eq(FiCarryForward::getStatus, FiCarryForward.STATUS_ACTIVE)
                .last("limit 1"));
    }

    private BigDecimal toDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
    }
}








