package com.erp.pa.service;

import com.erp.common.api.PageResult;
import com.erp.pa.domain.dto.PaOrderCloseRequest;
import com.erp.pa.domain.dto.PaOrderQueryRequest;
import com.erp.pa.domain.dto.PaOrderSaveRequest;
import com.erp.pa.domain.vo.PaOrderDetailVO;
import com.erp.pa.domain.vo.PaOrderListVO;

public interface PaOrderService {

    PageResult<PaOrderListVO> pageOrder(PaOrderQueryRequest request);

    PaOrderDetailVO getOrderDetail(Long id);

    Long createOrder(PaOrderSaveRequest request);

    void updateOrder(PaOrderSaveRequest request);

    void deleteOrder(Long id);

    void auditOrder(Long id);

    void closeOrder(Long id, PaOrderCloseRequest request);
}








