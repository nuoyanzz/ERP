package com.erp.fi.service;

import com.erp.fi.domain.vo.BalanceSheetVO;
import com.erp.fi.domain.vo.CashFlowStatementVO;
import com.erp.fi.domain.vo.IncomeStatementVO;

public interface FiReportService {

    BalanceSheetVO getBalanceSheet(String period);

    IncomeStatementVO getIncomeStatement(String period);

    CashFlowStatementVO getCashFlowStatement(String period);
}








