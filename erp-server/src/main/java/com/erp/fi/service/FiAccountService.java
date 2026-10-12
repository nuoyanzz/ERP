package com.erp.fi.service;

import com.erp.fi.domain.dto.FiAccountSaveRequest;
import com.erp.fi.domain.vo.FiAccountTreeVO;

import java.util.List;

public interface FiAccountService {

    List<FiAccountTreeVO> treeAccounts();

    FiAccountTreeVO getAccountDetail(Long id);

    Long createAccount(FiAccountSaveRequest request);

    void updateAccount(FiAccountSaveRequest request);

    void deleteAccount(Long id);
}








