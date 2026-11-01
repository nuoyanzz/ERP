package com.erp.md.service;

import java.util.List;
import com.erp.common.api.PageResult;
import com.erp.md.domain.dto.SupplierQueryRequest;
import com.erp.md.domain.dto.SupplierSaveRequest;
import com.erp.md.domain.vo.SupplierVO;

public interface MdSupplierService {
    /**
     * 下拉选项用的全量列表。
     * <p>
     * 主数据条目数量有限，单据表单的下拉选择直接返回全量，
     * 不做分页包装，避免前端逐页拼接。
     */
    List<SupplierVO> listMdSupplier();


    PageResult<SupplierVO> pageSupplier(SupplierQueryRequest request);

    SupplierVO getSupplierDetail(Long id);

    Long createSupplier(SupplierSaveRequest request);

    void updateSupplier(SupplierSaveRequest request);

    void deleteSupplier(Long id);
}








