package com.erp.sa.service;

import com.erp.common.api.PageResult;
import com.erp.sa.domain.dto.SaOrderCloseRequest;
import com.erp.sa.domain.dto.SaOrderQueryRequest;
import com.erp.sa.domain.dto.SaOrderSaveRequest;
import com.erp.sa.domain.vo.SaOrderDetailVO;
import com.erp.sa.domain.vo.SaOrderListVO;

public interface SaOrderService {

    PageResult<SaOrderListVO> pageOrder(SaOrderQueryRequest request);

    SaOrderDetailVO getOrderDetail(Long id);

    Long createOrder(SaOrderSaveRequest request);

    void updateOrder(SaOrderSaveRequest request);

    void deleteOrder(Long id);

    void auditOrder(Long id);

    void closeOrder(Long id, SaOrderCloseRequest request);
}








