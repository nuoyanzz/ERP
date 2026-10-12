package com.erp.fi.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.exception.BizException;
import com.erp.fi.domain.entity.FiAccount;
import com.erp.fi.domain.vo.BalanceLineVO;
import com.erp.fi.domain.vo.BalanceSectionVO;
import com.erp.fi.domain.vo.BalanceSheetVO;
import com.erp.fi.domain.vo.CashFlowItemVO;
import com.erp.fi.domain.vo.CashFlowSectionVO;
import com.erp.fi.domain.vo.CashFlowStatementVO;
import com.erp.fi.domain.vo.IncomeItemVO;
import com.erp.fi.domain.vo.IncomeStatementVO;
import com.erp.fi.mapper.FiAccountMapper;
import com.erp.fi.mapper.FiVoucherEntryMapper;
import com.erp.fi.service.AccountBalanceCalculator;
import com.erp.fi.service.FiReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FiReportServiceImpl implements FiReportService {

    private static final List<String> CURRENT_ASSET_PREFIXES = Arrays.asList("10", "11", "12", "13", "14");
    private static final List<String> NON_CURRENT_ASSET_PREFIXES = Arrays.asList("15", "16", "17", "18", "19");
    private static final List<String> CURRENT_LIABILITY_PREFIXES = Arrays.asList("20", "21", "22", "23", "24");
    private static final List<String> NON_CURRENT_LIABILITY_PREFIXES = Arrays.asList("25", "26", "27", "28", "29");

    private static final int TYPE_INCOME = 1;
    private static final int TYPE_EXPENSE = 2;

    /** 利润表项目分类 */
    private static final List<Bucket> BUCKETS = Arrays.asList(
            new Bucket("一、营业收入", TYPE_INCOME, new String[]{"4001", "6001"}),
            new Bucket("减：营业成本", TYPE_EXPENSE, new String[]{"4002", "6401", "6402"}),
            new Bucket("减：税金及附加", TYPE_EXPENSE, new String[]{"4003", "6403", "6404"}),
            new Bucket("减：销售费用", TYPE_EXPENSE, new String[]{"6601"}),
            new Bucket("减：管理费用", TYPE_EXPENSE, new String[]{"6602"}),
            new Bucket("减：财务费用", TYPE_EXPENSE, new String[]{"6603"}),
            new Bucket("加：营业外收入", TYPE_INCOME, new String[]{"5301", "6301"}),
            new Bucket("减：营业外支出", TYPE_EXPENSE, new String[]{"5311", "6711"}),
            new Bucket("减：所得税费用", TYPE_EXPENSE, new String[]{"6801", "6806"})
    );

    private static final int IDX_REVENUE = 0;
    private static final int IDX_COST = 1;
    private static final int IDX_TAX = 2;
    private static final int IDX_SELLING = 3;
    private static final int IDX_ADMIN = 4;
    private static final int IDX_FINANCE = 5;
    private static final int IDX_NON_OP_INCOME = 6;
    private static final int IDX_NON_OP_EXPENSE = 7;
    private static final int IDX_INCOME_TAX = 8;

    private final FiAccountMapper accountMapper;
    private final FiVoucherEntryMapper voucherEntryMapper;
    private final AccountBalanceCalculator balanceCalculator;

    @Override
    public BalanceSheetVO getBalanceSheet(String period) {
        validatePeriod(period);
        List<AccountBalanceCalculator.AccountBalance> balances = balanceCalculator.calculate(period);

        BalanceSheetVO sheet = new BalanceSheetVO();
        sheet.setPeriod(period);
        List<BalanceSectionVO> sections = new ArrayList<>();
        sections.add(buildSection("流动资产", balances, CURRENT_ASSET_PREFIXES, FiAccount.CATEGORY_ASSET));
        sections.add(buildSection("非流动资产", balances, NON_CURRENT_ASSET_PREFIXES, FiAccount.CATEGORY_ASSET));
        sections.add(buildSection("流动负债", balances, CURRENT_LIABILITY_PREFIXES, FiAccount.CATEGORY_LIABILITY));
        sections.add(buildSection("非流动负债", balances, NON_CURRENT_LIABILITY_PREFIXES, FiAccount.CATEGORY_LIABILITY));
        sections.add(buildEquitySection(balances));
        sheet.setSections(sections);

        BigDecimal assetTotal = subtotalOf(sections.get(0)).add(subtotalOf(sections.get(1)));
        BigDecimal liabilityTotal = subtotalOf(sections.get(2)).add(subtotalOf(sections.get(3)));
        // 权益 = 已结转到本年利润的金额 + 当期未结转损益
        BigDecimal uncarriedProfit = currentPeriodProfit(period);
        BigDecimal equityTotal = subtotalOf(sections.get(4)).add(uncarriedProfit);
        if (uncarriedProfit.compareTo(BigDecimal.ZERO) != 0) {
            BalanceLineVO profitLine = new BalanceLineVO();
            profitLine.setAccountCode("4103");
            profitLine.setAccountName("本年利润（含未结转损益）");
            profitLine.setClosingDebit(uncarriedProfit.max(BigDecimal.ZERO));
            profitLine.setClosingCredit(uncarriedProfit.negate().max(BigDecimal.ZERO));
            profitLine.setBalance(uncarriedProfit);
            sections.get(4).getLines().add(profitLine);
        }
        sheet.setAssetTotal(assetTotal);
        sheet.setLiabilityTotal(liabilityTotal);
        sheet.setEquityTotal(equityTotal);
        sheet.setBalanced(assetTotal.compareTo(liabilityTotal.add(equityTotal)) == 0);
        return sheet;
    }

    /**
     * 计算当期未结转损益。
     * <p>
     * 期末未执行损益结转时，本期利润仍留在损益类科目里，尚未进入本年利润。
     * 若权益只统计已结转金额，资产与权益就会相差一个当期净利润。
     * 这里把未结转损益计入权益，与「未分配利润含当期损益」的口径一致。
     *
     * @return 净利润（收入类为正，费用类为负）
     */
    private BigDecimal currentPeriodProfit(String period) {
        Map<Long, BigDecimal[]> periodMap = toMap(voucherEntryMapper.sumPostedByPeriod(period));
        if (periodMap.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<FiAccount> profitAccounts = accountMapper.selectList(Wrappers.<FiAccount>lambdaQuery()
                .eq(FiAccount::getCategory, FiAccount.CATEGORY_PROFIT)
                .eq(FiAccount::getStatus, 0));
        BigDecimal total = BigDecimal.ZERO;
        for (FiAccount account : profitAccounts) {
            BigDecimal[] pair = periodMap.get(account.getId());
            if (pair == null) {
                continue;
            }
            BigDecimal net = pair[0].subtract(pair[1]);
            total = total.add(net.negate());
        }
        return total;
    }

    @Override
    public IncomeStatementVO getIncomeStatement(String period) {
        validatePeriod(period);
        String yearStart = period.substring(0, 4) + "-01";

        // 本期发生额：直接取本期已记账凭证的发生额，与结转状态无关
        Map<Long, BigDecimal[]> currentMap = toMap(voucherEntryMapper.sumPostedByPeriod(period));
        // 本年累计：取年初至本期的发生额累计，结转后依然正确
        Map<Long, BigDecimal[]> ytdMap = toMap(voucherEntryMapper.sumPostedBetweenPeriods(yearStart, period));

        Map<Long, FiAccount> accountMap = accountMapper.selectList(Wrappers.<FiAccount>lambdaQuery()
                        .eq(FiAccount::getCategory, FiAccount.CATEGORY_PROFIT))
                .stream()
                .collect(Collectors.toMap(FiAccount::getId, account -> account, (a, b) -> a));

        BigDecimal[] current = new BigDecimal[BUCKETS.size()];
        BigDecimal[] ytd = new BigDecimal[BUCKETS.size()];
        Arrays.fill(current, BigDecimal.ZERO);
        Arrays.fill(ytd, BigDecimal.ZERO);
        for (Map.Entry<Long, FiAccount> entry : accountMap.entrySet()) {
            int bucketIndex = bucketIndexOf(entry.getValue().getSubjectCode());
            if (bucketIndex < 0) {
                continue;
            }
            Bucket bucket = BUCKETS.get(bucketIndex);
            Long accountId = entry.getKey();
            current[bucketIndex] = current[bucketIndex].add(netOf(currentMap.get(accountId), bucket.getType()));
            ytd[bucketIndex] = ytd[bucketIndex].add(netOf(ytdMap.get(accountId), bucket.getType()));
        }

        IncomeStatementVO statement = new IncomeStatementVO();
        statement.setPeriod(period);
        List<IncomeItemVO> items = new ArrayList<>();
        for (int i = 0; i < BUCKETS.size(); i++) {
            IncomeItemVO item = new IncomeItemVO();
            item.setName(BUCKETS.get(i).getName());
            item.setCurrentAmount(current[i]);
            item.setYtdAmount(ytd[i]);
            items.add(item);
        }
        statement.setItems(items);

        statement.setOperatingProfit(operatingProfit(current));
        statement.setTotalProfit(totalProfit(current));
        statement.setNetProfit(netProfit(current));
        statement.setYtdOperatingProfit(operatingProfit(ytd));
        statement.setYtdTotalProfit(totalProfit(ytd));
        statement.setYtdNetProfit(netProfit(ytd));
        return statement;
    }

    @Override
    public CashFlowStatementVO getCashFlowStatement(String period) {
        validatePeriod(period);
        String yearStart = period.substring(0, 4) + "-01";

        List<Map<String, Object>> currentRows = voucherEntryMapper.sumCashFlowByPeriod(period);
        List<Map<String, Object>> ytdRows = voucherEntryMapper.sumCashFlowBetweenPeriods(yearStart, period);

        Map<String, Map<String, Object>> ytdIndex = new HashMap<>();
        for (Map<String, Object> row : ytdRows) {
            ytdIndex.put(row.get("subjectCode") + "|" + row.get("cashFlowType"), row);
        }

        List<CashFlowItemVO> items = new ArrayList<>();
        BigDecimal[] operating = new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
        BigDecimal[] investing = new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
        BigDecimal[] financing = new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};

        for (Map<String, Object> row : currentRows) {
            int type = toInt(row.get("cashFlowType"));
            String subjectCode = String.valueOf(row.get("subjectCode"));
            BigDecimal inflow = toDecimal(row.get("inflowAmount"));
            BigDecimal outflow = toDecimal(row.get("outflowAmount"));

            CashFlowItemVO item = new CashFlowItemVO();
            item.setCashFlowType(type);
            item.setSubjectCode(subjectCode);
            item.setSubjectName(String.valueOf(row.get("subjectName")));
            item.setInflow(inflow);
            item.setOutflow(outflow);
            item.setNet(inflow.subtract(outflow));

            Map<String, Object> ytdRow = ytdIndex.get(subjectCode + "|" + type);
            if (ytdRow != null) {
                item.setYtdInflow(toDecimal(ytdRow.get("inflowAmount")));
                item.setYtdOutflow(toDecimal(ytdRow.get("outflowAmount")));
            } else {
                item.setYtdInflow(BigDecimal.ZERO);
                item.setYtdOutflow(BigDecimal.ZERO);
            }
            items.add(item);

            BigDecimal[] target = switch (type) {
                case FiAccount.CASH_FLOW_OPERATING -> operating;
                case FiAccount.CASH_FLOW_INVESTING -> investing;
                case FiAccount.CASH_FLOW_FINANCING -> financing;
                default -> null;
            };
            if (target != null) {
                target[0] = target[0].add(inflow);
                target[1] = target[1].add(outflow);
            }
        }

        CashFlowStatementVO statement = new CashFlowStatementVO();
        statement.setPeriod(period);
        statement.setItems(items);

        statement.setOperatingInflow(operating[0]);
        statement.setOperatingOutflow(operating[1]);
        statement.setOperatingNet(operating[0].subtract(operating[1]));
        statement.setInvestingInflow(investing[0]);
        statement.setInvestingOutflow(investing[1]);
        statement.setInvestingNet(investing[0].subtract(investing[1]));
        statement.setFinancingInflow(financing[0]);
        statement.setFinancingOutflow(financing[1]);
        statement.setFinancingNet(financing[0].subtract(financing[1]));

        statement.setOperating(buildSection("经营活动", operating));
        statement.setInvesting(buildSection("投资活动", investing));
        statement.setFinancing(buildSection("筹资活动", financing));

        BigDecimal netIncrease = statement.getOperatingNet()
                .add(statement.getInvestingNet())
                .add(statement.getFinancingNet());
        statement.setNetIncrease(netIncrease);

        // 期初/期末现金余额取现金类科目的期末净额合计
        BigDecimal openingCash = cashBalanceBefore(period);
        statement.setOpeningBalance(openingCash);
        statement.setClosingBalance(openingCash.add(netIncrease));
        return statement;
    }

    private CashFlowSectionVO buildSection(String title, BigDecimal[] pair) {
        CashFlowSectionVO section = new CashFlowSectionVO();
        section.setTitle(title);
        section.setInflow(pair[0]);
        section.setOutflow(pair[1]);
        section.setNet(pair[0].subtract(pair[1]));
        return section;
    }

    /**
     * 现金及现金等价物期初余额：现金类科目在指定期间之前的净额合计。
     */
    private BigDecimal cashBalanceBefore(String period) {
        List<Map<String, Object>> rows = voucherEntryMapper.sumCashFlowBetweenPeriods("0000-01", period);
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            int type = toInt(row.get("cashFlowType"));
            if (type != FiAccount.CASH_FLOW_OPERATING) {
                continue;
            }
            total = total.add(toDecimal(row.get("inflowAmount")).subtract(toDecimal(row.get("outflowAmount"))));
        }
        return total;
    }

    private BigDecimal operatingProfit(BigDecimal[] amounts) {
        return amounts[IDX_REVENUE]
                .subtract(amounts[IDX_COST]).subtract(amounts[IDX_TAX])
                .subtract(amounts[IDX_SELLING]).subtract(amounts[IDX_ADMIN]).subtract(amounts[IDX_FINANCE]);
    }

    private BigDecimal totalProfit(BigDecimal[] amounts) {
        return operatingProfit(amounts)
                .add(amounts[IDX_NON_OP_INCOME]).subtract(amounts[IDX_NON_OP_EXPENSE]);
    }

    private BigDecimal netProfit(BigDecimal[] amounts) {
        return totalProfit(amounts).subtract(amounts[IDX_INCOME_TAX]);
    }

    private int bucketIndexOf(String code) {
        for (int i = 0; i < BUCKETS.size(); i++) {
            for (String prefix : BUCKETS.get(i).getCodes()) {
                if (code.startsWith(prefix)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private BigDecimal netOf(BigDecimal[] pair, int type) {
        if (pair == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal net = pair[0].subtract(pair[1]);
        return type == TYPE_INCOME ? net.negate() : net;
    }

    /**
     * 构建资产负债表的分类段。
     */
    private BalanceSectionVO buildSection(String title, List<AccountBalanceCalculator.AccountBalance> balances,
                                         List<String> prefixList, int category) {
        BalanceSectionVO section = new BalanceSectionVO();
        section.setTitle(title);
        List<BalanceLineVO> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (AccountBalanceCalculator.AccountBalance balance : balances) {
            if (balance.getCategory() == null || balance.getCategory() != category) {
                continue;
            }
            if (!prefixList.stream().anyMatch(balance.getSubjectCode()::startsWith)) {
                continue;
            }
            BigDecimal closing = balance.closingBalance(FiAccount.CATEGORY_ASSET);
            BalanceLineVO line = new BalanceLineVO();
            line.setAccountCode(balance.getSubjectCode());
            line.setAccountName(balance.getSubjectName());
            line.setOpeningDebit(balance.getOpeningDebit().max(BigDecimal.ZERO));
            line.setOpeningCredit(balance.getOpeningCredit().max(BigDecimal.ZERO));
            line.setPeriodDebit(balance.getDebitAmount());
            line.setPeriodCredit(balance.getCreditAmount());
            line.setClosingDebit(balance.closingNet().max(BigDecimal.ZERO));
            line.setClosingCredit(balance.closingNet().negate().max(BigDecimal.ZERO));
            line.setBalance(closing);
            lines.add(line);
            subtotal = subtotal.add(closing);
        }
        section.setLines(lines);
        section.setSubtotal(subtotal);
        return section;
    }

    /**
     * 所有者权益段。权益类科目按类别取全部，不做流动/非流动划分。
     */
    private BalanceSectionVO buildEquitySection(List<AccountBalanceCalculator.AccountBalance> balances) {
        BalanceSectionVO section = new BalanceSectionVO();
        section.setTitle("所有者权益");
        List<BalanceLineVO> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (AccountBalanceCalculator.AccountBalance balance : balances) {
            if (balance.getCategory() == null || balance.getCategory() != FiAccount.CATEGORY_EQUITY) {
                continue;
            }
            BigDecimal closing = balance.closingBalance(FiAccount.CATEGORY_ASSET);
            BalanceLineVO line = new BalanceLineVO();
            line.setAccountCode(balance.getSubjectCode());
            line.setAccountName(balance.getSubjectName());
            line.setOpeningDebit(balance.getOpeningDebit().max(BigDecimal.ZERO));
            line.setOpeningCredit(balance.getOpeningCredit().max(BigDecimal.ZERO));
            line.setPeriodDebit(balance.getDebitAmount());
            line.setPeriodCredit(balance.getCreditAmount());
            line.setClosingDebit(balance.closingNet().max(BigDecimal.ZERO));
            line.setClosingCredit(balance.closingNet().negate().max(BigDecimal.ZERO));
            line.setBalance(closing);
            lines.add(line);
            subtotal = subtotal.add(closing);
        }
        section.setLines(lines);
        section.setSubtotal(subtotal);
        return section;
    }

    private BigDecimal subtotalOf(BalanceSectionVO section) {
        return section.getSubtotal() == null ? BigDecimal.ZERO : section.getSubtotal();
    }

    private Map<Long, BigDecimal[]> toMap(List<Map<String, Object>> rows) {
        Map<Long, BigDecimal[]> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object accountId = row.get("accountId");
            if (accountId == null) {
                continue;
            }
            map.put(((Number) accountId).longValue(),
                    new BigDecimal[]{toDecimal(row.get("debitAmount")), toDecimal(row.get("creditAmount"))});
        }
        return map;
    }

    private BigDecimal toDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }

    private void validatePeriod(String period) {
        if (period == null || period.trim().isEmpty()) {
            throw new BizException("会计期间不能为空");
        }
    }

    private static class Bucket {
        private final String name;
        private final int type;
        private final String[] codes;

        private Bucket(String name, int type, String[] codes) {
            this.name = name;
            this.type = type;
            this.codes = codes;
        }

        private String getName() {
            return name;
        }

        private int getType() {
            return type;
        }

        private String[] getCodes() {
            return codes;
        }
    }
}








