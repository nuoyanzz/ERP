package com.erp.md.service;

import java.util.List;
import com.erp.common.api.PageResult;
import com.erp.md.domain.dto.CustomerQueryRequest;
import com.erp.md.domain.dto.CustomerSaveRequest;
import com.erp.md.domain.vo.CustomerVO;

public interface MdCustomerService {
    /**
     * 下拉选项用的全量列表。
     * <p>
     * 主数据条目数量有限，单据表单的下拉选择直接返回全量，
     * 不做分页包装，避免前端逐页拼接。
     */
    List<CustomerVO> listMdCustomer();


    PageResult<CustomerVO> pageCustomer(CustomerQueryRequest request);

    CustomerVO getCustomerDetail(Long id);

    Long createCustomer(CustomerSaveRequest request);

    void updateCustomer(CustomerSaveRequest request);

    void deleteCustomer(Long id);
}








