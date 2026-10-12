package com.erp.fi.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.fi.domain.entity.FiAccount;
import com.erp.fi.mapper.FiAccountMapper;
import com.erp.fi.mapper.FiOpeningBalanceMapper;
import com.erp.fi.mapper.FiVoucherEntryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 科目余额计算器。
 * <p>
 * 统一「期初 + 本期发生 = 期末」的计算口径，供资产负债表、科目余额表、
 * 试算平衡表共用，避免各报表口径不一致。
 * <p>
 * 余额方向约定：借方为正、贷方为负（净额表示）。资产类科目净额即为资产金额，
 * 负债与权益类科目取负后为正数。
 */
@Component
@RequiredArgsConstructor
public class AccountBalanceCalculator {

    private final FiVoucherEntryMapper voucherEntryMapper;
    private final FiOpeningBalanceMapper openingBalanceMapper;
    private final FiAccountMapper accountMapper;

    /**
     * 余额明细。
     */
    public static class AccountBalance {

        private Long accountId;
        private String subjectCode;
        private String subjectName;
        private Integer category;
        /** 期初借方发生额 */
        private BigDecimal openingDebit = BigDecimal.ZERO;
        /** 期初贷方发生额 */
        private BigDecimal openingCredit = BigDecimal.ZERO;
        /** 本期借方发生额 */
        private BigDecimal debitAmount = BigDecimal.ZERO;
        /** 本期贷方发生额 */
        private BigDecimal creditAmount = BigDecimal.ZERO;

        /** 期初余额（借正贷负） */
        public BigDecimal openingNet() {
            return openingDebit.subtract(openingCredit);
        }

        /** 期末余额（借正贷负） */
        public BigDecimal closingNet() {
            return openingNet().add(debitAmount).subtract(creditAmount);
        }

        /** 期末余额，按科目类别取符号：资产为正，负债与权益为正 */
        public BigDecimal closingBalance(int assetCategory) {
            BigDecimal net = closingNet();
            return category != null && category == assetCategory ? net : net.negate();
        }

        /** 本期借贷发生额差额（借正贷负） */
        public BigDecimal periodNet() {
            return debitAmount.subtract(creditAmount);
        }

        public Long getAccountId() {
            return accountId;
        }

        public void setAccountId(Long accountId) {
            this.accountId = accountId;
        }

        public String getSubjectCode() {
            return subjectCode;
        }

        public void setSubjectCode(String subjectCode) {
            this.subjectCode = subjectCode;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public void setSubjectName(String subjectName) {
            this.subjectName = subjectName;
        }

        public Integer getCategory() {
            return category;
        }

        public void setCategory(Integer category) {
            this.category = category;
        }

        public BigDecimal getOpeningDebit() {
            return openingDebit;
        }

        public void setOpeningDebit(BigDecimal openingDebit) {
            this.openingDebit = openingDebit;
        }

        public BigDecimal getOpeningCredit() {
            return openingCredit;
        }

        public void setOpeningCredit(BigDecimal openingCredit) {
            this.openingCredit = openingCredit;
        }

        public BigDecimal getDebitAmount() {
            return debitAmount;
        }

        public void setDebitAmount(BigDecimal debitAmount) {
            this.debitAmount = debitAmount;
        }

        public BigDecimal getCreditAmount() {
            return creditAmount;
        }

        public void setCreditAmount(BigDecimal creditAmount) {
            this.creditAmount = creditAmount;
        }
    }

    /**
     * 计算指定期间的科目余额。
     * <p>
     * 期初 = 系统期初余额表 + 该期间之前已记账凭证的发生额；
     * 本期 = 该期间已记账凭证的发生额；
     * 期末 = 期初 + 本期。
     *
     * @param period 期间，格式 YYYY-MM
     */
    public List<AccountBalance> calculate(String period) {
        Map<Long, BigDecimal[]> openingMap = toPairMap(
                openingBalanceMapper.sumOpeningBeforePeriod(period));
        Map<Long, BigDecimal[]> beforeMap = toPairMap(
                voucherEntryMapper.sumPostedBeforePeriod(period));
        Map<Long, BigDecimal[]> periodMap = toPairMap(
                voucherEntryMapper.sumPostedByPeriod(period));

        List<Long> accountIds = new ArrayList<>();
        accountIds.addAll(openingMap.keySet());
        accountIds.addAll(beforeMap.keySet());
        accountIds.addAll(periodMap.keySet());
        List<Long> distinctIds = accountIds.stream().distinct().collect(Collectors.toList());

        Map<Long, FiAccount> accountMap = distinctIds.isEmpty()
                ? Map.of()
                : accountMapper.selectList(Wrappers.<FiAccount>lambdaQuery()
                        .in(FiAccount::getId, distinctIds))
                .stream()
                .collect(Collectors.toMap(FiAccount::getId, account -> account, (a, b) -> a));

        List<AccountBalance> result = new ArrayList<>();
        for (Long accountId : distinctIds) {
            FiAccount account = accountMap.get(accountId);
            if (account == null) {
                continue;
            }
            BigDecimal[] opening = openingMap.get(accountId);
            BigDecimal[] before = beforeMap.get(accountId);
            BigDecimal[] current = periodMap.get(accountId);

            AccountBalance balance = new AccountBalance();
            balance.setAccountId(accountId);
            balance.setSubjectCode(account.getSubjectCode());
            balance.setSubjectName(account.getSubjectName());
            balance.setCategory(account.getCategory());
            balance.setOpeningDebit(add(opening, 0).add(add(before, 0)));
            balance.setOpeningCredit(add(opening, 1).add(add(before, 1)));
            balance.setDebitAmount(add(current, 0));
            balance.setCreditAmount(add(current, 1));
            result.add(balance);
        }
        result.sort((a, b) -> a.getSubjectCode().compareTo(b.getSubjectCode()));
        return result;
    }

    /**
     * 计算全局试算平衡：全部科目借方合计与贷方合计。
     */
    public TrialBalanceResult trialBalance(String period) {
        List<AccountBalance> balances = calculate(period);
        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        BigDecimal openingDebitTotal = BigDecimal.ZERO;
        BigDecimal openingCreditTotal = BigDecimal.ZERO;
        for (AccountBalance balance : balances) {
            debitTotal = debitTotal.add(balance.getDebitAmount());
            creditTotal = creditTotal.add(balance.getCreditAmount());
            openingDebitTotal = openingDebitTotal.add(balance.getOpeningDebit());
            openingCreditTotal = openingCreditTotal.add(balance.getOpeningCredit());
        }
        return new TrialBalanceResult(debitTotal, creditTotal, openingDebitTotal, openingCreditTotal);
    }

    /**
     * 试算平衡结果。
     */
    public static class TrialBalanceResult {

        private final BigDecimal debitTotal;
        private final BigDecimal creditTotal;
        private final BigDecimal openingDebitTotal;
        private final BigDecimal openingCreditTotal;

        TrialBalanceResult(BigDecimal debitTotal, BigDecimal creditTotal,
                           BigDecimal openingDebitTotal, BigDecimal openingCreditTotal) {
            this.debitTotal = debitTotal;
            this.creditTotal = creditTotal;
            this.openingDebitTotal = openingDebitTotal;
            this.openingCreditTotal = openingCreditTotal;
        }

        public BigDecimal getDebitTotal() {
            return debitTotal;
        }

        public BigDecimal getCreditTotal() {
            return creditTotal;
        }

        public BigDecimal getOpeningDebitTotal() {
            return openingDebitTotal;
        }

        public BigDecimal getOpeningCreditTotal() {
            return openingCreditTotal;
        }

        public boolean isBalanced() {
            return debitTotal.compareTo(creditTotal) == 0
                    && openingDebitTotal.compareTo(openingCreditTotal) == 0;
        }

        public BigDecimal getDifference() {
            return debitTotal.subtract(creditTotal);
        }
    }

    private BigDecimal add(BigDecimal[] pair, int index) {
        return pair == null ? BigDecimal.ZERO : pair[index];
    }

    private Map<Long, BigDecimal[]> toPairMap(List<Map<String, Object>> rows) {
        Map<Long, BigDecimal[]> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object accountId = row.get("accountId");
            if (accountId == null) {
                continue;
            }
            map.put(((Number) accountId).longValue(), new BigDecimal[]{
                    toDecimal(row.get("debitAmount")), toDecimal(row.get("creditAmount"))});
        }
        return map;
    }

    private BigDecimal toDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
    }
}








