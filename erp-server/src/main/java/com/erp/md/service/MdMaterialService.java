package com.erp.md.service;

import java.util.List;
import com.erp.common.api.PageResult;
import com.erp.md.domain.dto.MaterialQueryRequest;
import com.erp.md.domain.dto.MaterialSaveRequest;
import com.erp.md.domain.vo.MaterialVO;

public interface MdMaterialService {
    /**
     * 下拉选项用的全量列表。
     * <p>
     * 主数据条目数量有限，单据表单的下拉选择直接返回全量，
     * 不做分页包装，避免前端逐页拼接。
     */
    List<MaterialVO> listMdMaterial();


    PageResult<MaterialVO> pageMaterial(MaterialQueryRequest request);

    MaterialVO getMaterialDetail(Long id);

    Long createMaterial(MaterialSaveRequest request);

    void updateMaterial(MaterialSaveRequest request);

    void deleteMaterial(Long id);
}








