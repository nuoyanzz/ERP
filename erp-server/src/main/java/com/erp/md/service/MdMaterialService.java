package com.erp.md.service;

import com.erp.common.api.PageResult;
import com.erp.md.domain.dto.MaterialQueryRequest;
import com.erp.md.domain.dto.MaterialSaveRequest;
import com.erp.md.domain.vo.MaterialVO;

public interface MdMaterialService {

    PageResult<MaterialVO> pageMaterial(MaterialQueryRequest request);

    MaterialVO getMaterialDetail(Long id);

    Long createMaterial(MaterialSaveRequest request);

    void updateMaterial(MaterialSaveRequest request);

    void deleteMaterial(Long id);
}








