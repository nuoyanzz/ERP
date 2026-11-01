package com.erp.md.service;

import java.util.List;
import com.erp.common.api.PageResult;
import com.erp.md.domain.dto.WarehouseQueryRequest;
import com.erp.md.domain.dto.WarehouseSaveRequest;
import com.erp.md.domain.vo.WarehouseVO;

public interface MdWarehouseService {
    /**
     * 下拉选项用的全量列表。
     * <p>
     * 主数据条目数量有限，单据表单的下拉选择直接返回全量，
     * 不做分页包装，避免前端逐页拼接。
     */
    List<WarehouseVO> listMdWarehouse();


    PageResult<WarehouseVO> pageWarehouse(WarehouseQueryRequest request);

    WarehouseVO getWarehouseDetail(Long id);

    Long createWarehouse(WarehouseSaveRequest request);

    void updateWarehouse(WarehouseSaveRequest request);

    void deleteWarehouse(Long id);
}








