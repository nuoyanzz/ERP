package com.erp.md.service;

import com.erp.common.api.PageResult;
import com.erp.md.domain.dto.CustomerQueryRequest;
import com.erp.md.domain.dto.CustomerSaveRequest;
import com.erp.md.domain.vo.CustomerVO;

public interface MdCustomerService {

    PageResult<CustomerVO> pageCustomer(CustomerQueryRequest request);

    CustomerVO getCustomerDetail(Long id);

    Long createCustomer(CustomerSaveRequest request);

    void updateCustomer(CustomerSaveRequest request);

    void deleteCustomer(Long id);
}








