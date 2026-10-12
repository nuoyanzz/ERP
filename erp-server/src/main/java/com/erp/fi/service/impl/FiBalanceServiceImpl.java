package com.erp.fi.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.exception.BizException;
import com.erp.fi.domain.entity.FiAccount;
import com.erp.fi.domain.vo.FiAccountBalanceVO;
import com.erp.fi.mapper.FiAccountMapper;
import com.erp.fi.mapper.FiVoucherEntryMapper;
import com.erp.fi.service.FiBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FiBalanceServiceImpl implements FiBalanceService {

    private final FiVoucherEntryMapper voucherEntryMapper;
    private final FiAccountMapper accountMapper;

    @Override
    public List<FiAccountBalanceVO> getAccountBalance(String period) {
        if (period == null || period.trim().isEmpty()) {
            throw new BizException("会计期间不能为空");
        }
        Map<Long, BigDecimal[]> currentMap = aggregate(voucherEntryMapper.sumPostedByPeriod(period));
        Map<Long, BigDecimal[]> openingMap = aggregate(voucherEntryMapper.sumPostedBeforePeriod(period));
        Map<Long, FiAccount> accountMap = loadAccounts(currentMap, openingMap);
        Map<String, FiAccountBalanceVO> result = new TreeMap<>();
        for (Map.Entry<Long, FiAccount> entry : accountMap.entrySet()) {
            Long accountId = entry.getKey();
            FiAccount account = entry.getValue();
            BigDecimal[] current = currentMap.getOrDefault(accountId, zeroPair());
            BigDecimal[] opening = openingMap.getOrDefault(accountId, zeroPair());
            BigDecimal openingNet = opening[0].subtract(opening[1]);
            BigDecimal currentNet = current[0].subtract(current[1]);
            BigDecimal closingNet = openingNet.add(currentNet);
            FiAccountBalanceVO vo = new FiAccountBalanceVO();
            vo.setAccountId(accountId);
            vo.setSubjectCode(account.getSubjectCode());
            vo.setSubjectName(account.getSubjectName());
            vo.setCategory(account.getCategory());
            vo.setBalanceDir(account.getBalanceDir());
            vo.setOpeningDebit(openingNet.max(BigDecimal.ZERO));
            vo.setOpeningCredit(openingNet.negate().max(BigDecimal.ZERO));
            vo.setPeriodDebit(current[0]);
            vo.setPeriodCredit(current[1]);
            vo.setClosingDebit(closingNet.max(BigDecimal.ZERO));
            vo.setClosingCredit(closingNet.negate().max(BigDecimal.ZERO));
            result.put(account.getSubjectCode(), vo);
        }
        return new ArrayList<>(result.values());
    }

    private Map<Long, BigDecimal[]> aggregate(List<Map<String, Object>> rows) {
        Map<Long, BigDecimal[]> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object accountId = row.get("accountId");
            if (accountId == null) {
                continue;
            }
            BigDecimal debit = toDecimal(row.get("debitAmount"));
            BigDecimal credit = toDecimal(row.get("creditAmount"));
            map.put(((Number) accountId).longValue(), new BigDecimal[]{debit, credit});
        }
        return map;
    }

    private BigDecimal toDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
    }

    private BigDecimal[] zeroPair() {
        return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
    }

    private Map<Long, FiAccount> loadAccounts(Map<Long, BigDecimal[]> currentMap, Map<Long, BigDecimal[]> openingMap) {
        List<Long> accountIds = new ArrayList<>();
        accountIds.addAll(currentMap.keySet());
        accountIds.addAll(openingMap.keySet());
        List<Long> distinctIds = accountIds.stream().distinct().collect(Collectors.toList());
        if (distinctIds.isEmpty()) {
            return new HashMap<>();
        }
        return accountMapper.selectList(Wrappers.<FiAccount>lambdaQuery().in(FiAccount::getId, distinctIds))
                .stream()
                .collect(Collectors.toMap(FiAccount::getId, account -> account, (a, b) -> a));
    }
}








