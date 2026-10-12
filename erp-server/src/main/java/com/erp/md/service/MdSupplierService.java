package com.erp.md.service;

import com.erp.common.api.PageResult;
import com.erp.md.domain.dto.SupplierQueryRequest;
import com.erp.md.domain.dto.SupplierSaveRequest;
import com.erp.md.domain.vo.SupplierVO;

public interface MdSupplierService {

    PageResult<SupplierVO> pageSupplier(SupplierQueryRequest request);

    SupplierVO getSupplierDetail(Long id);

    Long createSupplier(SupplierSaveRequest request);

    void updateSupplier(SupplierSaveRequest request);

    void deleteSupplier(Long id);
}








