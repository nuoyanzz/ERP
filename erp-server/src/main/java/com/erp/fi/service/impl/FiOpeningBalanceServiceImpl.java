package com.erp.fi.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.exception.BizException;
import com.erp.fi.domain.dto.OpeningBalanceRequest;
import com.erp.fi.domain.entity.FiAccount;
import com.erp.fi.domain.entity.FiOpeningBalance;
import com.erp.fi.domain.entity.FiTrialBalance;
import com.erp.fi.domain.vo.OpeningBalanceVO;
import com.erp.fi.domain.vo.TrialBalanceLineVO;
import com.erp.fi.domain.vo.TrialBalanceVO;
import com.erp.fi.mapper.FiAccountMapper;
import com.erp.fi.mapper.FiOpeningBalanceMapper;
import com.erp.fi.mapper.FiTrialBalanceMapper;
import com.erp.fi.service.AccountBalanceCalculator;
import com.erp.fi.service.FiOpeningBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FiOpeningBalanceServiceImpl
        extends ServiceImpl<FiOpeningBalanceMapper, FiOpeningBalance> implements FiOpeningBalanceService {

    private final FiAccountMapper accountMapper;
    private final FiTrialBalanceMapper trialBalanceMapper;
    private final AccountBalanceCalculator balanceCalculator;

    @Override
    public List<OpeningBalanceVO> listOpeningBalances(String period) {
        validatePeriod(period);
        List<FiOpeningBalance> balances = this.list(Wrappers.<FiOpeningBalance>lambdaQuery()
                .le(FiOpeningBalance::getPeriod, period)
                .orderByAsc(FiOpeningBalance::getPeriod));
        if (balances.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> accountIds = balances.stream()
                .map(FiOpeningBalance::getAccountId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, FiAccount> accountMap = accountMapper.selectList(Wrappers.<FiAccount>lambdaQuery()
                        .in(FiAccount::getId, accountIds))
                .stream()
                .collect(Collectors.toMap(FiAccount::getId, account -> account, (a, b) -> a));
        List<OpeningBalanceVO> result = new ArrayList<>(balances.size());
        for (FiOpeningBalance balance : balances) {
            OpeningBalanceVO vo = new OpeningBalanceVO();
            BeanUtils.copyProperties(balance, vo);
            FiAccount account = accountMap.get(balance.getAccountId());
            if (account != null) {
                vo.setAccountCode(account.getSubjectCode());
                vo.setAccountName(account.getSubjectName());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOpeningBalance(OpeningBalanceRequest request) {
        validateRequest(request);
        // 同科目同期间同辅助项视为同一条，覆盖式更新
        FiOpeningBalance exist = this.getOne(Wrappers.<FiOpeningBalance>lambdaQuery()
                .eq(FiOpeningBalance::getAccountId, request.getAccountId())
                .eq(FiOpeningBalance::getPeriod, request.getPeriod())
                .eq(FiOpeningBalance::getAuxType, request.getAuxType() == null ? "" : request.getAuxType())
                .eq(request.getAuxId() != null, FiOpeningBalance::getAuxId, request.getAuxId())
                .last("limit 1"));
        FiOpeningBalance balance = new FiOpeningBalance();
        BeanUtils.copyProperties(request, balance);
        balance.setSobId(1L);
        if (balance.getDebitAmount() == null) {
            balance.setDebitAmount(BigDecimal.ZERO);
        }
        if (balance.getCreditAmount() == null) {
            balance.setCreditAmount(BigDecimal.ZERO);
        }
        if (exist != null) {
            balance.setId(exist.getId());
            this.updateById(balance);
            return balance.getId();
        }
        this.save(balance);
        return balance.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importOpeningBalances(List<OpeningBalanceRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BizException("导入数据不能为空");
        }
        int count = 0;
        for (OpeningBalanceRequest request : requests) {
            saveOpeningBalance(request);
            count++;
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOpeningBalance(Long id) {
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrialBalanceVO generateTrialBalance(String period) {
        validatePeriod(period);
        List<AccountBalanceCalculator.AccountBalance> balances = balanceCalculator.calculate(period);

        TrialBalanceVO vo = new TrialBalanceVO();
        vo.setPeriod(period);
        List<TrialBalanceLineVO> lines = new ArrayList<>(balances.size());
        BigDecimal openingDebit = BigDecimal.ZERO;
        BigDecimal openingCredit = BigDecimal.ZERO;
        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        BigDecimal closingDebit = BigDecimal.ZERO;
        BigDecimal closingCredit = BigDecimal.ZERO;

        for (AccountBalanceCalculator.AccountBalance balance : balances) {
            TrialBalanceLineVO line = new TrialBalanceLineVO();
            line.setAccountId(balance.getAccountId());
            line.setAccountCode(balance.getSubjectCode());
            line.setAccountName(balance.getSubjectName());
            line.setOpeningDebit(balance.getOpeningDebit());
            line.setOpeningCredit(balance.getOpeningCredit());
            line.setDebitAmount(balance.getDebitAmount());
            line.setCreditAmount(balance.getCreditAmount());
            line.setClosingDebit(balance.closingNet().max(BigDecimal.ZERO));
            line.setClosingCredit(balance.closingNet().negate().max(BigDecimal.ZERO));
            lines.add(line);

            openingDebit = openingDebit.add(balance.getOpeningDebit());
            openingCredit = openingCredit.add(balance.getOpeningCredit());
            debitTotal = debitTotal.add(balance.getDebitAmount());
            creditTotal = creditTotal.add(balance.getCreditAmount());
            closingDebit = closingDebit.add(line.getClosingDebit());
            closingCredit = closingCredit.add(line.getClosingCredit());
        }

        vo.setLines(lines);
        vo.setOpeningDebitTotal(openingDebit);
        vo.setOpeningCreditTotal(openingCredit);
        vo.setDebitTotal(debitTotal);
        vo.setCreditTotal(creditTotal);
        vo.setClosingDebitTotal(closingDebit);
        vo.setClosingCreditTotal(closingCredit);
        vo.setBalanced(debitTotal.compareTo(creditTotal) == 0
                && openingDebit.compareTo(openingCredit) == 0
                && closingDebit.compareTo(closingCredit) == 0);
        vo.setDifference(debitTotal.subtract(creditTotal));

        // 保存快照，作为该期间的记账基线
        trialBalanceMapper.delete(Wrappers.<FiTrialBalance>lambdaQuery()
                .eq(FiTrialBalance::getPeriod, period));
        for (TrialBalanceLineVO line : lines) {
            FiTrialBalance snapshot = new FiTrialBalance();
            snapshot.setPeriod(period);
            snapshot.setAccountId(line.getAccountId());
            snapshot.setDebitAmount(line.getDebitAmount());
            snapshot.setCreditAmount(line.getCreditAmount());
            trialBalanceMapper.insert(snapshot);
        }
        return vo;
    }

    private void validateRequest(OpeningBalanceRequest request) {
        if (request == null || request.getAccountId() == null) {
            throw new BizException("会计科目不能为空");
        }
        validatePeriod(request.getPeriod());
        FiAccount account = accountMapper.selectById(request.getAccountId());
        if (account == null) {
            throw new BizException("会计科目不存在：" + request.getAccountId());
        }
        BigDecimal debit = request.getDebitAmount() == null ? BigDecimal.ZERO : request.getDebitAmount();
        BigDecimal credit = request.getCreditAmount() == null ? BigDecimal.ZERO : request.getCreditAmount();
        if (debit.compareTo(BigDecimal.ZERO) < 0 || credit.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("期初余额不能为负数");
        }
        if (debit.compareTo(BigDecimal.ZERO) > 0 && credit.compareTo(BigDecimal.ZERO) > 0) {
            throw new BizException("期初余额不能同时存在借方和贷方金额");
        }
    }

    private void validatePeriod(String period) {
        if (period == null || period.trim().isEmpty()) {
            throw new BizException("会计期间不能为空");
        }
        if (!period.matches("^\\d{4}-\\d{2}$")) {
            throw new BizException("会计期间格式应为 YYYY-MM");
        }
    }
}








