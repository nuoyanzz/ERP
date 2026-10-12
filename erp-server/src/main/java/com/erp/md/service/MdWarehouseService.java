package com.erp.md.service;

import com.erp.common.api.PageResult;
import com.erp.md.domain.dto.WarehouseQueryRequest;
import com.erp.md.domain.dto.WarehouseSaveRequest;
import com.erp.md.domain.vo.WarehouseVO;

public interface MdWarehouseService {

    PageResult<WarehouseVO> pageWarehouse(WarehouseQueryRequest request);

    WarehouseVO getWarehouseDetail(Long id);

    Long createWarehouse(WarehouseSaveRequest request);

    void updateWarehouse(WarehouseSaveRequest request);

    void deleteWarehouse(Long id);
}








