package com.erp.ap.service;

import com.erp.common.api.PageResult;
import com.erp.ap.domain.dto.ApPaymentSaveRequest;
import com.erp.ap.domain.vo.ApPaymentVO;

public interface ApPaymentService {

    PageResult<ApPaymentVO> pagePayment(int pageNum, int pageSize, Long supplierId, Integer status);

    ApPaymentVO getPaymentDetail(Long id);

    Long createPayment(ApPaymentSaveRequest request);

    void updatePayment(ApPaymentSaveRequest request);

    void deletePayment(Long id);

    void auditPayment(Long id);
}








