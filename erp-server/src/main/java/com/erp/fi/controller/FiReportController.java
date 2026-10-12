package com.erp.fi.controller;

import com.erp.common.api.Result;
import com.erp.fi.domain.vo.BalanceSheetVO;
import com.erp.fi.domain.vo.CashFlowStatementVO;
import com.erp.fi.domain.vo.IncomeStatementVO;
import com.erp.fi.service.FiReportService;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fi/report")
@RequiredArgsConstructor
public class FiReportController {

    private final FiReportService reportService;

    @GetMapping("/balance-sheet")
    @RequiresPermission("fi:report:query")
    @OperLog(module = "fi")
    public Result<BalanceSheetVO> balanceSheet(@RequestParam String period) {
        return Result.ok(reportService.getBalanceSheet(period));
    }

    @GetMapping("/income-statement")
    @RequiresPermission("fi:report:query")
    @OperLog(module = "fi")
    public Result<IncomeStatementVO> incomeStatement(@RequestParam String period) {
        return Result.ok(reportService.getIncomeStatement(period));
    }

    @GetMapping("/cash-flow-statement")
    @RequiresPermission("fi:report:query")
    @OperLog(module = "fi")
    public Result<CashFlowStatementVO> cashFlowStatement(@RequestParam String period) {
        return Result.ok(reportService.getCashFlowStatement(period));
    }
}








