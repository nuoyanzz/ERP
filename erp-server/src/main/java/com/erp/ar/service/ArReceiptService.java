package com.erp.ar.service;

import com.erp.common.api.PageResult;
import com.erp.ar.domain.dto.ArReceiptSaveRequest;
import com.erp.ar.domain.vo.ArReceiptVO;

public interface ArReceiptService {

    PageResult<ArReceiptVO> pageReceipt(int pageNum, int pageSize, Long customerId, Integer status);

    ArReceiptVO getReceiptDetail(Long id);

    Long createReceipt(ArReceiptSaveRequest request);

    void updateReceipt(ArReceiptSaveRequest request);

    void deleteReceipt(Long id);

    void auditReceipt(Long id);
}








