package com.erp.ar.service;

import com.erp.ar.domain.dto.ArBadDebtQueryRequest;
import com.erp.ar.domain.dto.ArBadDebtSaveRequest;
import com.erp.ar.domain.vo.ArBadDebtVO;
import com.erp.common.api.PageResult;

public interface ArBadDebtService {

    PageResult<ArBadDebtVO> pageBadDebt(ArBadDebtQueryRequest request);

    ArBadDebtVO getBadDebtDetail(Long id);

    Long createBadDebt(ArBadDebtSaveRequest request);

    void updateBadDebt(ArBadDebtSaveRequest request);

    void deleteBadDebt(Long id);

    /**
     * 审核坏账计提单。
     * <p>
     * 审核后：把计提金额分摊到该客户未结清的应收单上（bad_debt_amount），
     * 并生成「借：信用减值损失 / 贷：坏账准备」的记账凭证。
     */
    void auditBadDebt(Long id);
}













