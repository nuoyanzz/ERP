package com.erp.fi.service;

import com.erp.fi.domain.vo.FiAccountBalanceVO;

import java.util.List;

public interface FiBalanceService {

    List<FiAccountBalanceVO> getAccountBalance(String period);
}








